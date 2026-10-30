package com.example.silentsos.utils

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/** Qué pasó con el último SMS enviado a un número. */
sealed interface EstadoSms {
    data object Enviando : EstadoSms
    /** El operador lo aceptó; aún no confirma que llegó al teléfono del contacto. */
    data object Enviado : EstadoSms
    /** El operador confirmó que llegó al teléfono del contacto. */
    data object Entregado : EstadoSms
    data class Fallido(val motivo: String) : EstadoSms
}

/**
 * Envía el SMS de alerta al número configurado.
 * El envío es silencioso: no abre ninguna app de mensajes ni muestra UI.
 *
 * Pide al operador confirmación de envío y de entrega, y publica el resultado
 * por número en [estados]. Si el envío falla (sin señal, radio apagada…) se
 * reintenta una vez pasados unos segundos.
 */
@Singleton
class SmsAlertManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val _estados = MutableStateFlow<Map<String, EstadoSms>>(emptyMap())
    val estados: StateFlow<Map<String, EstadoSms>> = _estados.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private val siguienteId = AtomicInteger(0)

    /** Envíos en curso: id → (número, texto, partes que faltan por confirmar, ¿es reintento?). */
    private val envios = mutableMapOf<Int, Envio>()

    private class Envio(val numero: String, val texto: String, var partesPendientes: Int, val reintento: Boolean)

    private val receptor = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val id = intent.getIntExtra(EXTRA_ID, -1)
            val envio = synchronized(envios) { envios[id] } ?: return
            when (intent.action) {
                ACCION_ENVIADO -> alEnviarParte(id, envio, resultCode)
                ACCION_ENTREGADO -> alEntregar(envio, intent)
            }
        }
    }

    init {
        val filtro = IntentFilter().apply {
            addAction(ACCION_ENVIADO)
            addAction(ACCION_ENTREGADO)
        }
        ContextCompat.registerReceiver(context, receptor, filtro, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    fun tienePermiso(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED

    /** Borra los estados de la alerta anterior. */
    fun reiniciarEstados() {
        _estados.value = emptyMap()
    }

    /**
     * Envía [mensaje] a [numero]. Devuelve true si el SMS se entregó
     * al sistema para envío, false si faltó número, permiso o hubo un error.
     * El resultado real llega después a [estados].
     */
    fun enviarAlerta(numero: String, mensaje: String): Boolean =
        enviar(numero, TextoSms.aGsm(mensaje), reintento = false)

    private fun enviar(numero: String, texto: String, reintento: Boolean): Boolean {
        if (numero.isBlank()) {
            Log.w(TAG, "No hay número configurado para el SMS de alerta")
            return false
        }

        if (!tienePermiso()) {
            Log.w(TAG, "Permiso SEND_SMS no concedido")
            marcar(numero, EstadoSms.Fallido("no se envió: falta el permiso de SMS"))
            return false
        }

        return try {
            val smsManager: SmsManager? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            if (smsManager == null) {
                marcar(numero, EstadoSms.Fallido("no se envió: el teléfono no puede enviar SMS"))
                return false
            }

            // divideMessage puede lanzar SecurityException en Android 16+ (consulta datos
            // de la SIM sin permiso); en ese caso se parte a mano.
            val partes = try {
                smsManager.divideMessage(texto)
            } catch (e: SecurityException) {
                ArrayList(texto.chunked(LARGO_PARTE))
            }

            val id = siguienteId.incrementAndGet()
            synchronized(envios) { envios[id] = Envio(numero, texto, partes.size, reintento) }
            if (!reintento) marcar(numero, EstadoSms.Enviando)

            val enviados = ArrayList(partes.indices.map { pendiente(ACCION_ENVIADO, id, it) })
            // Basta con la confirmación de entrega de la última parte
            val entregados = ArrayList(partes.indices.map { if (it == partes.lastIndex) pendiente(ACCION_ENTREGADO, id, it) else null })
            smsManager.sendMultipartTextMessage(numero, null, partes, enviados, entregados)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando SMS de alerta", e)
            marcar(numero, EstadoSms.Fallido("no se envió: error del sistema"))
            false
        }
    }

    private fun pendiente(accion: String, id: Int, parte: Int): PendingIntent {
        val intent = Intent(accion).setPackage(context.packageName).putExtra(EXTRA_ID, id)
        // requestCode único por envío y parte para que no se reutilicen entre sí
        val codigo = id * 100 + parte * 2 + if (accion == ACCION_ENTREGADO) 1 else 0
        // Mutable: el sistema añade el PDU del informe de entrega al intent
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        return PendingIntent.getBroadcast(context, codigo, intent, flags)
    }

    private fun alEnviarParte(id: Int, envio: Envio, resultado: Int) {
        if (resultado != Activity.RESULT_OK) {
            val motivo = motivoError(resultado)
            Log.w(TAG, "SMS a ${envio.numero} falló: $motivo ($resultado)")
            synchronized(envios) { envios.remove(id) }
            if (!envio.reintento) {
                marcar(envio.numero, EstadoSms.Fallido("no se envió: $motivo, reintentando…"))
                handler.postDelayed({ enviar(envio.numero, envio.texto, reintento = true) }, ESPERA_REINTENTO_MS)
            } else {
                marcar(envio.numero, EstadoSms.Fallido("no se envió: $motivo"))
            }
            return
        }
        envio.partesPendientes--
        if (envio.partesPendientes == 0 && _estados.value[envio.numero] !is EstadoSms.Entregado) {
            marcar(envio.numero, EstadoSms.Enviado)
        }
    }

    private fun alEntregar(envio: Envio, intent: Intent) {
        val pdu = intent.getByteArrayExtra("pdu")
        val formato = intent.getStringExtra("format")
        val estado = pdu?.let { runCatching { SmsMessage.createFromPdu(it, formato).status }.getOrNull() }
        // GSM TP-Status: 0x00–0x1F entregado, 0x20–0x3F aún intentando, 0x40+ fallo definitivo
        when {
            estado == null || estado < 0x20 -> marcar(envio.numero, EstadoSms.Entregado)
            estado >= 0x40 -> marcar(envio.numero, EstadoSms.Fallido("salió del teléfono pero el operador no lo entregó (código $estado)"))
            else -> return // el operador sigue intentando: llegará otro informe
        }
        synchronized(envios) { envios.values.remove(envio) }
        Log.d(TAG, "Informe de entrega para ${envio.numero}: status=$estado")
    }

    private fun marcar(numero: String, estado: EstadoSms) {
        _estados.update { it + (numero to estado) }
    }

    private fun motivoError(codigo: Int): String = when (codigo) {
        SmsManager.RESULT_ERROR_NO_SERVICE -> "sin señal"
        SmsManager.RESULT_ERROR_RADIO_OFF -> "modo avión o radio apagada"
        SmsManager.RESULT_ERROR_NULL_PDU -> "mensaje inválido"
        SmsManager.RESULT_ERROR_LIMIT_EXCEEDED -> "límite de SMS del sistema"
        SmsManager.RESULT_ERROR_SHORT_CODE_NOT_ALLOWED,
        SmsManager.RESULT_ERROR_SHORT_CODE_NEVER_ALLOWED -> "número corto no permitido"
        SmsManager.RESULT_ERROR_FDN_CHECK_FAILURE -> "bloqueado por marcación fija"
        SmsManager.RESULT_NETWORK_REJECT -> "rechazado por la red (¿sin saldo?)"
        SmsManager.RESULT_INVALID_SMSC_ADDRESS -> "centro de mensajes de la SIM inválido"
        else -> "error del operador ($codigo)"
    }

    companion object {
        private const val TAG = "SmsAlertManager"
        private const val LARGO_PARTE = 153
        private const val ESPERA_REINTENTO_MS = 15_000L
        private const val ACCION_ENVIADO = "com.example.silentsos.SMS_ENVIADO"
        private const val ACCION_ENTREGADO = "com.example.silentsos.SMS_ENTREGADO"
        private const val EXTRA_ID = "id_envio"
    }
}
