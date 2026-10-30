package com.example.silentsos.domain.model

import org.json.JSONArray
import org.json.JSONObject

enum class OrigenAlerta(val descripcion: String) {
    BOTON_SOS("Botón SOS"),
    DOBLE_APERTURA("Doble apertura"),
    ACCESO_RAPIDO("Acceso rápido"),
    PRUEBA("Mensaje de prueba")
}

/** Una entrada del historial de alertas (se guarda solo en el teléfono). */
data class RegistroAlerta(
    val hora: Long,
    val origen: OrigenAlerta,
    val latitud: Double?,
    val longitud: Double?,
    val smsEnviados: Int,
    val whatsAppEnviados: Int,
    val contactos: Int,
    val llamada: Boolean
) {
    val enlaceMapa: String?
        get() = if (latitud != null && longitud != null) {
            String.format(java.util.Locale.US, "https://maps.google.com/?q=%.6f,%.6f", latitud, longitud)
        } else null

    fun aJson(): JSONObject = JSONObject()
        .put("hora", hora)
        .put("origen", origen.name)
        .put("lat", latitud ?: JSONObject.NULL)
        .put("lng", longitud ?: JSONObject.NULL)
        .put("sms", smsEnviados)
        .put("wa", whatsAppEnviados)
        .put("contactos", contactos)
        .put("llamada", llamada)

    companion object {
        const val MAXIMO_GUARDADOS = 50

        fun desdeJson(o: JSONObject) = RegistroAlerta(
            hora = o.getLong("hora"),
            origen = runCatching { OrigenAlerta.valueOf(o.getString("origen")) }.getOrDefault(OrigenAlerta.BOTON_SOS),
            latitud = if (o.isNull("lat")) null else o.getDouble("lat"),
            longitud = if (o.isNull("lng")) null else o.getDouble("lng"),
            smsEnviados = o.optInt("sms"),
            whatsAppEnviados = o.optInt("wa"),
            contactos = o.optInt("contactos"),
            llamada = o.optBoolean("llamada")
        )

        fun listaAJson(registros: List<RegistroAlerta>): String =
            JSONArray(registros.map { it.aJson() }).toString()

        fun listaDesdeJson(json: String): List<RegistroAlerta> = try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { runCatching { desdeJson(array.getJSONObject(it)) }.getOrNull() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
