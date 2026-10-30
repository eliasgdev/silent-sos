package com.example.silentsos.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextoSmsTest {

    @Test
    fun `quita tildes y enies`() {
        assertEquals("Ubicacion: Bogota, Espana", TextoSms.aGsm("Ubicación: Bogotá, España"))
    }

    @Test
    fun `reemplaza simbolos fuera de GSM`() {
        assertEquals("(+/-5 m) - 80%", TextoSms.aGsm("(±5 m) · 80%"))
    }

    @Test
    fun `elimina emojis y conserva saltos de linea`() {
        assertEquals(" ALERTA\nok", TextoSms.aGsm("🚨 ALERTA\nok"))
    }

    @Test
    fun `el resultado es ASCII imprimible`() {
        val texto = TextoSms.aGsm("¡Ayúdame! Ñandú — “comillas” ü")
        assertTrue(texto.all { it == '\n' || it.code in 32..126 })
    }

    @Test
    fun `los enlaces de Maps no cambian`() {
        val enlace = "https://maps.google.com/?q=4.711000,-74.072100"
        assertEquals(enlace, TextoSms.aGsm(enlace))
    }
}
