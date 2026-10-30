package com.example.silentsos.domain

import com.example.silentsos.domain.model.ContactoEmergencia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactoEmergenciaTest {

    @Test
    fun `limpiarNumero conserva el + inicial y quita espacios y guiones`() {
        assertEquals("+573001234567", ContactoEmergencia.limpiarNumero(" +57 300-123 4567 "))
    }

    @Test
    fun `limpiarNumero sin indicativo deja solo digitos`() {
        assertEquals("3001234567", ContactoEmergencia.limpiarNumero("(300) 123-4567"))
    }

    @Test
    fun `limpiarNumero ignora un + que no esta al inicio`() {
        assertEquals("57300", ContactoEmergencia.limpiarNumero("57+300"))
    }

    @Test
    fun `la lista sobrevive ida y vuelta por JSON`() {
        val contactos = listOf(
            ContactoEmergencia("Mamá", "+573001112233"),
            ContactoEmergencia("", "+573004445566")
        )
        val json = ContactoEmergencia.listaAJson(contactos)
        assertEquals(contactos, ContactoEmergencia.listaDesdeJson(json))
    }

    @Test
    fun `JSON corrupto devuelve lista vacia en vez de fallar`() {
        assertTrue(ContactoEmergencia.listaDesdeJson("{no es json").isEmpty())
    }

    @Test
    fun `contactos sin numero se descartan`() {
        val json = """[{"nombre":"Sin número","numero":""},{"nombre":"Ana","numero":"+57300"}]"""
        assertEquals(listOf(ContactoEmergencia("Ana", "+57300")), ContactoEmergencia.listaDesdeJson(json))
    }

    @Test
    fun `etiqueta usa el numero si no hay nombre`() {
        assertEquals("+57300", ContactoEmergencia("", "+57300").etiqueta)
        assertEquals("Ana", ContactoEmergencia("Ana", "+57300").etiqueta)
    }
}
