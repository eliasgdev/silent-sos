package com.example.silentsos.utils

import java.security.MessageDigest
import java.security.SecureRandom

/** Hash del PIN local: SHA-256 con salt aleatorio, nunca se guarda el PIN en claro. */
object PinHasher {

    fun nuevoSalt(): String = ByteArray(16).also { SecureRandom().nextBytes(it) }.toHex()

    fun hash(pin: String, salt: String): String =
        MessageDigest.getInstance("SHA-256").digest("$salt:$pin".toByteArray()).toHex()

    fun esPinValido(pin: String): Boolean = pin.length in 4..6 && pin.all { it.isDigit() }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
