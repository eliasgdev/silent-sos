package com.example.silentsos.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import com.example.silentsos.service.WhatsAppAutoSendService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class ResultadoWhatsApp {
    /** El chat se abrió y el servicio de accesibilidad pulsará "Enviar". */
    ENVIO_AUTOMATICO,
    /** El chat se abrió con el mensaje escrito; falta tocar "Enviar". */
    ABIERTO_PARA_ENVIAR,
    NO_INSTALADO,
    SIN_NUMERO,
    DESACTIVADO,
    ERROR
}

/**
 * Abre el chat de WhatsApp del contacto de emergencia con el mensaje ya escrito.
 *
 * WhatsApp no permite que otras apps envíen mensajes por su cuenta; si el usuario
 * activó [WhatsAppAutoSendService] en Accesibilidad, ese servicio pulsa "Enviar"
 * automáticamente y el envío no requiere tocar la pantalla.
 */
@Singleton
class WhatsAppManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun paqueteInstalado(): String? = PAQUETES.firstOrNull { paquete ->
        try {
            context.packageManager.getPackageInfo(paquete, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun estaInstalado(): Boolean = paqueteInstalado() != null

    fun autoEnvioDisponible(): Boolean = WhatsAppAutoSendService.estaHabilitado(context)

    fun enviar(numero: String, mensaje: String): ResultadoWhatsApp {
        val paquete = paqueteInstalado() ?: return ResultadoWhatsApp.NO_INSTALADO
        // WhatsApp espera el número internacional solo con dígitos (ej. 573001234567)
        val telefono = numero.filter { it.isDigit() }
        if (telefono.isEmpty()) return ResultadoWhatsApp.SIN_NUMERO

        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$telefono&text=${Uri.encode(mensaje)}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
            .setPackage(paquete)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val automatico = autoEnvioDisponible()
        if (automatico) WhatsAppAutoSendService.programarEnvio()

        return try {
            context.startActivity(intent)
            if (automatico) ResultadoWhatsApp.ENVIO_AUTOMATICO else ResultadoWhatsApp.ABIERTO_PARA_ENVIAR
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo abrir WhatsApp", e)
            WhatsAppAutoSendService.cancelarEnvio()
            ResultadoWhatsApp.ERROR
        }
    }

    companion object {
        private const val TAG = "WhatsAppManager"
        val PAQUETES = listOf("com.whatsapp", "com.whatsapp.w4b")
    }
}
