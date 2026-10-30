package com.example.silentsos.domain.model

import org.json.JSONArray
import org.json.JSONObject

data class ContactoEmergencia(
    val nombre: String,
    /** Solo dígitos y un "+" inicial, ej. +573001234567. */
    val numero: String
) {
    val etiqueta: String get() = nombre.ifBlank { numero }

    companion object {
        const val MAXIMO = 3

        fun listaAJson(contactos: List<ContactoEmergencia>): String =
            JSONArray(contactos.map { JSONObject().put("nombre", it.nombre).put("numero", it.numero) }).toString()

        fun listaDesdeJson(json: String): List<ContactoEmergencia> = try {
            val array = JSONArray(json)
            (0 until array.length()).map {
                val o = array.getJSONObject(it)
                ContactoEmergencia(o.optString("nombre"), o.optString("numero"))
            }.filter { it.numero.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }

        /** Conserva solo dígitos y un "+" al inicio. */
        fun limpiarNumero(numero: String): String {
            val recortado = numero.trim()
            val digitos = recortado.filter { it.isDigit() }
            return if (recortado.startsWith("+")) "+$digitos" else digitos
        }
    }
}
