package com.example.silentsos.domain

import com.example.silentsos.domain.model.OrigenAlerta
import com.example.silentsos.domain.model.RegistroAlerta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegistroAlertaTest {

    private val conUbicacion = RegistroAlerta(
        hora = 1_761_840_000_000L,
        origen = OrigenAlerta.DOBLE_APERTURA,
        latitud = 4.711,
        longitud = -74.0721,
        smsEnviados = 2,
        whatsAppEnviados = 1,
        contactos = 2,
        llamada = true
    )

    @Test
    fun `ida y vuelta por JSON conserva todos los campos`() {
        val sinUbicacion = conUbicacion.copy(latitud = null, longitud = null, origen = OrigenAlerta.PRUEBA)
        val json = RegistroAlerta.listaAJson(listOf(conUbicacion, sinUbicacion))
        assertEquals(listOf(conUbicacion, sinUbicacion), RegistroAlerta.listaDesdeJson(json))
    }

    @Test
    fun `enlace de mapa con 6 decimales y punto decimal`() {
        assertEquals("https://maps.google.com/?q=4.711000,-74.072100", conUbicacion.enlaceMapa)
    }

    @Test
    fun `sin ubicacion no hay enlace`() {
        assertNull(conUbicacion.copy(latitud = null).enlaceMapa)
    }

    @Test
    fun `origen desconocido no rompe la lectura`() {
        val json = """[{"hora":1,"origen":"OTRO","lat":null,"lng":null,"sms":0,"wa":0,"contactos":0,"llamada":false}]"""
        assertEquals(OrigenAlerta.BOTON_SOS, RegistroAlerta.listaDesdeJson(json).single().origen)
    }

    @Test
    fun `JSON corrupto devuelve historial vacio`() {
        assertTrue(RegistroAlerta.listaDesdeJson("basura").isEmpty())
    }
}
