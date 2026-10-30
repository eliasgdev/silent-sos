package com.example.silentsos.domain.model

import com.example.silentsos.utils.ResultadoWhatsApp

/** Estado de la última alerta, para mostrar en la pantalla principal. */
sealed interface EstadoAlerta {

    data object Inactiva : EstadoAlerta

    data object Enviando : EstadoAlerta

    /** Se pidió la alerta pero no hay ningún contacto a quien enviarla. */
    data class SinContactos(val llamadaIniciada: Boolean) : EstadoAlerta

    data class Enviada(
        val hora: Long,
        val ubicacionObtenida: Boolean,
        val contactos: Int,
        val smsEnviados: Int,
        val whatsApp: ResultadoWhatsApp,
        val seguimientoIniciado: Boolean,
        val llamadaIniciada: Boolean
    ) : EstadoAlerta
}
