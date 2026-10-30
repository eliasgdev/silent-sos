package com.example.silentsos.utils

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detecta si la app fue abierta 2 veces en un intervalo corto de tiempo,
 * para disparar la alerta automática (opción de configuración 2).
 *
 * Una "apertura" es cada vez que la app pasa a primer plano (MainActivity.onStart),
 * excepto rotaciones de pantalla y regresos desde pantallas que abrió la propia app
 * (WhatsApp, ajustes del sistema), que se omiten con [omitirSiguienteApertura].
 */
@Singleton
class AppOpenTracker @Inject constructor(
    @ApplicationContext context: Context
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var omitirSiguiente = false

    /**
     * Registra una apertura de la app y devuelve true si esta apertura
     * cuenta como "segunda apertura" dentro de la ventana de tiempo definida.
     */
    fun registrarAperturaYEsDoble(): Boolean {
        if (omitirSiguiente) {
            omitirSiguiente = false
            return false
        }

        val ahora = System.currentTimeMillis()
        val esDoble = esDobleApertura(
            ultimaApertura = prefs.getLong(KEY_ULTIMA_APERTURA, 0L),
            ahora = ahora,
            enfriamientoHasta = prefs.getLong(KEY_ENFRIAMIENTO_HASTA, 0L)
        )
        if (esDoble) {
            reiniciarContador()
        } else {
            prefs.edit().putLong(KEY_ULTIMA_APERTURA, ahora).apply()
        }
        return esDoble
    }

    /** La próxima vuelta a primer plano no cuenta como apertura. */
    fun omitirSiguienteApertura() {
        omitirSiguiente = true
    }

    /** Evita que la doble apertura se dispare de nuevo justo después de una alerta. */
    fun iniciarEnfriamiento() {
        prefs.edit()
            .putLong(KEY_ENFRIAMIENTO_HASTA, System.currentTimeMillis() + ENFRIAMIENTO_MS)
            .apply()
        reiniciarContador()
    }

    fun reiniciarContador() {
        prefs.edit().remove(KEY_ULTIMA_APERTURA).apply()
    }

    companion object {
        private const val PREFS_NAME = "silent_sos_open_tracker"
        private const val KEY_ULTIMA_APERTURA = "ultima_apertura"
        private const val KEY_ENFRIAMIENTO_HASTA = "enfriamiento_hasta"
        /**
         * Regla pura de la doble apertura (sin SharedPreferences, para poder probarla):
         * hay una apertura anterior a menos de [VENTANA_MS] y no estamos en enfriamiento.
         */
        fun esDobleApertura(ultimaApertura: Long, ahora: Long, enfriamientoHasta: Long): Boolean {
            // Tras una alerta no se vuelve a disparar otra durante un rato
            if (ahora < enfriamientoHasta) return false
            return ultimaApertura > 0L && ahora - ultimaApertura in 1..VENTANA_MS
        }

        /** Dos aperturas separadas por menos de este tiempo disparan la alerta. */
        const val VENTANA_MS = 10_000L
        private const val ENFRIAMIENTO_MS = 2 * 60_000L
    }
}
