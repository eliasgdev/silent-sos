package com.example.silentsos.utils

import java.text.Normalizer

object TextoSms {

    /**
     * Quita tildes y símbolos fuera del alfabeto GSM: así cada SMS admite 160
     * caracteres en vez de 70 y se evita la ruta Unicode de divideMessage.
     */
    fun aGsm(mensaje: String): String =
        Normalizer.normalize(mensaje.replace("±", "+/-").replace("·", "-"), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .filter { it == '\n' || it.code in 32..126 }
}
