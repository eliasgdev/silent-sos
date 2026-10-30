package com.example.silentsos.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Envía el SMS de alerta al número configurado.
 * El envío es silencioso: no abre ninguna app de mensajes ni muestra UI.
 */
@Singleton
class SmsAlertManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun tienePermiso(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Envía [mensaje] a [numero]. Devuelve true si el SMS se entregó
     * al sistema para envío, false si faltó número, permiso o hubo un error.
     */
    fun enviarAlerta(numero: String, mensaje: String): Boolean {
        if (numero.isBlank()) {
            Log.w(TAG, "No hay número configurado para el SMS de alerta")
            return false
        }

        if (!tienePermiso()) {
            Log.w(TAG, "Permiso SEND_SMS no concedido")
            return false
        }

        return try {
            val smsManager: SmsManager? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            if (smsManager == null) return false

            val texto = aTextoGsm(mensaje)
            // divideMessage puede lanzar SecurityException en Android 16+ (consulta datos
            // de la SIM sin permiso); en ese caso se parte a mano.
            val partes = try {
                smsManager.divideMessage(texto)
            } catch (e: SecurityException) {
                ArrayList(texto.chunked(LARGO_PARTE))
            }
            smsManager.sendMultipartTextMessage(numero, null, partes, null, null)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando SMS de alerta", e)
            false
        }
    }

    /**
     * Quita tildes y símbolos fuera del alfabeto GSM: así cada SMS admite 160
     * caracteres en vez de 70 y se evita la ruta Unicode de divideMessage.
     */
    private fun aTextoGsm(mensaje: String): String =
        Normalizer.normalize(mensaje.replace("±", "+/-").replace("·", "-"), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .filter { it == '\n' || it.code in 32..126 }

    companion object {
        private const val TAG = "SmsAlertManager"
        private const val LARGO_PARTE = 153
    }
}
