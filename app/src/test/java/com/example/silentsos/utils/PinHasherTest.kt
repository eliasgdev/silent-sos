package com.example.silentsos.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {

    @Test
    fun `mismo PIN y salt dan el mismo hash`() {
        assertEquals(PinHasher.hash("1234", "abc"), PinHasher.hash("1234", "abc"))
    }

    @Test
    fun `salt distinto cambia el hash`() {
        assertNotEquals(PinHasher.hash("1234", "abc"), PinHasher.hash("1234", "abd"))
    }

    @Test
    fun `el hash no contiene el PIN y mide 64 caracteres hex`() {
        val hash = PinHasher.hash("1234", PinHasher.nuevoSalt())
        assertEquals(64, hash.length)
        assertFalse(hash.contains("1234"))
        assertTrue(hash.all { it in "0123456789abcdef" })
    }

    @Test
    fun `cada salt nuevo es distinto`() {
        assertNotEquals(PinHasher.nuevoSalt(), PinHasher.nuevoSalt())
    }

    @Test
    fun `validacion de PIN`() {
        assertTrue(PinHasher.esPinValido("1234"))
        assertTrue(PinHasher.esPinValido("123456"))
        assertFalse(PinHasher.esPinValido("123"))
        assertFalse(PinHasher.esPinValido("1234567"))
        assertFalse(PinHasher.esPinValido("12a4"))
    }
}
