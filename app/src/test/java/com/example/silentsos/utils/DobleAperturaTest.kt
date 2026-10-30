package com.example.silentsos.utils

import com.example.silentsos.utils.AppOpenTracker.Companion.VENTANA_MS
import com.example.silentsos.utils.AppOpenTracker.Companion.esDobleApertura
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DobleAperturaTest {

    private val ahora = 1_000_000L

    @Test
    fun `segunda apertura dentro de la ventana dispara`() {
        assertTrue(esDobleApertura(ultimaApertura = ahora - 3_000, ahora = ahora, enfriamientoHasta = 0))
    }

    @Test
    fun `justo en el limite de la ventana todavia dispara`() {
        assertTrue(esDobleApertura(ahora - VENTANA_MS, ahora, 0))
    }

    @Test
    fun `fuera de la ventana no dispara`() {
        assertFalse(esDobleApertura(ahora - VENTANA_MS - 1, ahora, 0))
    }

    @Test
    fun `primera apertura no dispara`() {
        assertFalse(esDobleApertura(ultimaApertura = 0, ahora = ahora, enfriamientoHasta = 0))
    }

    @Test
    fun `durante el enfriamiento no dispara`() {
        assertFalse(esDobleApertura(ahora - 2_000, ahora, enfriamientoHasta = ahora + 60_000))
    }

    @Test
    fun `reloj hacia atras no dispara`() {
        assertFalse(esDobleApertura(ultimaApertura = ahora + 5_000, ahora = ahora, enfriamientoHasta = 0))
    }
}
