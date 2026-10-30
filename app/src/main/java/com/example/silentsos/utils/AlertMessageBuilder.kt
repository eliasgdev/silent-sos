package com.example.silentsos.utils

import android.content.Context
import android.location.Location
import android.os.BatteryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Arma los textos que se envían al contacto de emergencia.
 * Los de SMS van sin emojis para que no se partan en más mensajes de la cuenta.
 */
@Singleton
class AlertMessageBuilder @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun alerta(
        nombreUsuario: String,
        ubicacion: Location?,
        intervaloSeguimientoMin: Int?,
        paraWhatsApp: Boolean
    ): String = buildString {
        val quien = nombreUsuario.ifBlank { "Tu contacto" }
        if (paraWhatsApp) {
            appendLine("🚨 *ALERTA SilentSOS*")
            appendLine("$quien activó una alerta de emergencia y puede necesitar ayuda.")
            appendLine()
            appendLine("📍 ${lineaUbicacion(ubicacion)}")
            append("🕒 ${hora()}")
            bateria()?.let { append(" · 🔋 $it%") }
            appendLine()
            if (intervaloSeguimientoMin != null) {
                appendLine()
                append("Te comparto mi ubicación en tiempo real aquí en WhatsApp y te enviaré SMS con mi posición cada $intervaloSeguimientoMin min.")
            }
        } else {
            appendLine("ALERTA SilentSOS: $quien activo una alerta de emergencia y puede necesitar ayuda.")
            appendLine(lineaUbicacion(ubicacion))
            append("Hora: ${hora()}")
            bateria()?.let { append(" - Bateria: $it%") }
            if (intervaloSeguimientoMin != null) {
                appendLine()
                append("Recibiras la ubicacion cada $intervaloSeguimientoMin min.")
            }
        }
    }.trim()

    fun actualizacion(numero: Int, ubicacion: Location?, paraWhatsApp: Boolean): String {
        val prefijo = if (paraWhatsApp) "📍 *SilentSOS* · actualización #$numero" else "SilentSOS - actualizacion #$numero"
        return "$prefijo\n${lineaUbicacion(ubicacion)}\n${hora()}${bateria()?.let { " - $it%" } ?: ""}"
    }

    fun finSeguimiento(ubicacion: Location?): String =
        "SilentSOS: el seguimiento en tiempo real se detuvo.\nUltima ${lineaUbicacion(ubicacion).replaceFirstChar { it.lowercase() }}"

    fun prueba(nombreUsuario: String, ubicacion: Location?, paraWhatsApp: Boolean): String {
        val quien = nombreUsuario.ifBlank { "Tu contacto" }
        val encabezado = if (paraWhatsApp) "✅ *SilentSOS · mensaje de prueba*" else "SilentSOS - mensaje de prueba"
        return "$encabezado\n$quien te registró como contacto de emergencia. No es una emergencia real.\n${lineaUbicacion(ubicacion)}"
    }

    private fun lineaUbicacion(ubicacion: Location?): String {
        if (ubicacion == null) return "Ubicación no disponible (GPS sin señal o sin permiso)."
        val enlace = String.format(
            Locale.US, "https://maps.google.com/?q=%.6f,%.6f", ubicacion.latitude, ubicacion.longitude
        )
        val precision = if (ubicacion.hasAccuracy()) " (±${ubicacion.accuracy.toInt()} m)" else ""
        return "Ubicación: $enlace$precision"
    }

    private fun hora(): String =
        SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())

    private fun bateria(): Int? {
        val manager = context.getSystemService(BatteryManager::class.java) ?: return null
        return manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).takeIf { it in 0..100 }
    }
}
