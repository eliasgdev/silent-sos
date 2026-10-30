package com.example.silentsos.data

import android.content.Context
import android.content.SharedPreferences
import com.example.silentsos.domain.model.ContactoEmergencia
import com.example.silentsos.domain.model.RegistroAlerta
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manejo simple de preferencias locales del usuario.
 */
@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext context: Context
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun haVistoBienvenida(): Boolean = prefs.getBoolean(KEY_BIENVENIDA_VISTA, false)

    fun setBienvenidaVista(vista: Boolean) {
        prefs.edit().putBoolean(KEY_BIENVENIDA_VISTA, vista).apply()
    }

    fun isLlamadaAlAbrirActiva(): Boolean = prefs.getBoolean(KEY_LLAMADA_AL_ABRIR, false)

    fun setLlamadaAlAbrir(activo: Boolean) {
        prefs.edit().putBoolean(KEY_LLAMADA_AL_ABRIR, activo).apply()
    }

    fun isDobleAperturaAutoLlamadaActiva(): Boolean = prefs.getBoolean(KEY_DOBLE_APERTURA, false)

    fun setDobleAperturaAutoLlamada(activo: Boolean) {
        prefs.edit().putBoolean(KEY_DOBLE_APERTURA, activo).apply()
    }

    /**
     * Contactos de emergencia (máx. 3), guardados como JSON.
     * Si solo existe el contacto único de la versión 1.0, se migra a la lista.
     */
    fun getContactos(): List<ContactoEmergencia> {
        prefs.getString(KEY_CONTACTOS, null)?.let { return ContactoEmergencia.listaDesdeJson(it) }
        val numeroAntiguo = prefs.getString(KEY_NUMERO_SMS, "") ?: ""
        if (numeroAntiguo.isBlank()) return emptyList()
        return listOf(ContactoEmergencia(prefs.getString(KEY_NOMBRE_CONTACTO, "") ?: "", numeroAntiguo))
    }

    fun setContactos(contactos: List<ContactoEmergencia>) {
        prefs.edit()
            .putString(KEY_CONTACTOS, ContactoEmergencia.listaAJson(contactos))
            .remove(KEY_NUMERO_SMS)
            .remove(KEY_NOMBRE_CONTACTO)
            .apply()
    }

    fun getHistorial(): List<RegistroAlerta> =
        RegistroAlerta.listaDesdeJson(prefs.getString(KEY_HISTORIAL, "[]") ?: "[]")

    fun setHistorial(registros: List<RegistroAlerta>) {
        prefs.edit().putString(KEY_HISTORIAL, RegistroAlerta.listaAJson(registros)).apply()
    }

    fun isSmsActivo(): Boolean = prefs.getBoolean(KEY_SMS_ACTIVO, true)

    fun setSmsActivo(activo: Boolean) {
        prefs.edit().putBoolean(KEY_SMS_ACTIVO, activo).apply()
    }

    fun isWhatsAppActivo(): Boolean = prefs.getBoolean(KEY_WHATSAPP_ACTIVO, true)

    fun setWhatsAppActivo(activo: Boolean) {
        prefs.edit().putBoolean(KEY_WHATSAPP_ACTIVO, activo).apply()
    }

    fun isSeguimientoActivo(): Boolean = prefs.getBoolean(KEY_SEGUIMIENTO_ACTIVO, true)

    fun setSeguimientoActivo(activo: Boolean) {
        prefs.edit().putBoolean(KEY_SEGUIMIENTO_ACTIVO, activo).apply()
    }

    fun getIntervaloSeguimientoMin(): Int = prefs.getInt(KEY_INTERVALO_MIN, 2)

    fun setIntervaloSeguimientoMin(minutos: Int) {
        prefs.edit().putInt(KEY_INTERVALO_MIN, minutos).apply()
    }

    fun isLlamarEmergenciaActivo(): Boolean = prefs.getBoolean(KEY_LLAMAR_ACTIVO, true)

    fun setLlamarEmergenciaActivo(activo: Boolean) {
        prefs.edit().putBoolean(KEY_LLAMAR_ACTIVO, activo).apply()
    }

    /** Número al que se llama al activar la alerta (por defecto la línea 123). */
    fun getNumeroEmergencia(): String =
        prefs.getString(KEY_NUMERO_EMERGENCIA, NUMERO_EMERGENCIA_POR_DEFECTO)
            ?: NUMERO_EMERGENCIA_POR_DEFECTO

    fun setNumeroEmergencia(numero: String) {
        prefs.edit().putString(KEY_NUMERO_EMERGENCIA, numero).apply()
    }

    fun getNombreUsuario(): String = prefs.getString(KEY_NOMBRE_USUARIO, "") ?: ""

    fun setNombreUsuario(nombre: String) {
        prefs.edit().putString(KEY_NOMBRE_USUARIO, nombre).apply()
    }

    fun getPinHash(): String = prefs.getString(KEY_PIN_HASH, "") ?: ""

    fun getPinSalt(): String = prefs.getString(KEY_PIN_SALT, "") ?: ""

    fun setPin(hash: String, salt: String) {
        prefs.edit().putString(KEY_PIN_HASH, hash).putString(KEY_PIN_SALT, salt).apply()
    }

    companion object {
        private const val PREFS_NAME = "silent_sos_prefs"
        private const val KEY_BIENVENIDA_VISTA = "bienvenida_vista"
        private const val KEY_LLAMADA_AL_ABRIR = "opcion_llamada_al_abrir"
        private const val KEY_DOBLE_APERTURA = "opcion_doble_apertura"
        private const val KEY_NUMERO_SMS = "numero_sms_alerta"
        private const val KEY_NOMBRE_CONTACTO = "nombre_contacto"
        private const val KEY_CONTACTOS = "contactos_emergencia"
        private const val KEY_HISTORIAL = "historial_alertas"
        private const val KEY_SMS_ACTIVO = "canal_sms_activo"
        private const val KEY_WHATSAPP_ACTIVO = "canal_whatsapp_activo"
        private const val KEY_SEGUIMIENTO_ACTIVO = "seguimiento_activo"
        private const val KEY_INTERVALO_MIN = "seguimiento_intervalo_min"
        private const val KEY_LLAMAR_ACTIVO = "llamar_emergencia_activo"
        private const val KEY_NUMERO_EMERGENCIA = "numero_emergencia"
        private const val KEY_NOMBRE_USUARIO = "nombre_usuario"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"

        const val NUMERO_EMERGENCIA_POR_DEFECTO = "123"
    }
}
