package com.example.silentsos.data.repository

import android.content.Context
import android.location.Location
import com.example.silentsos.data.PreferencesManager
import com.example.silentsos.di.ApplicationScope
import com.example.silentsos.domain.model.EstadoAlerta
import com.example.silentsos.service.TrackingService
import com.example.silentsos.utils.AlertMessageBuilder
import com.example.silentsos.utils.AppOpenTracker
import com.example.silentsos.utils.EmergencyCallManager
import com.example.silentsos.utils.LocationHelper
import com.example.silentsos.utils.ResultadoWhatsApp
import com.example.silentsos.utils.SmsAlertManager
import com.example.silentsos.utils.WhatsAppManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Única fuente de verdad para todo lo relacionado con la config de
 * emergencia. La UI y los ViewModels no tocan PreferencesManager ni
 * los managers de utils directamente.
 */
@Singleton
class EmergencyRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
    private val preferencesManager: PreferencesManager,
    private val appOpenTracker: AppOpenTracker,
    private val emergencyCallManager: EmergencyCallManager,
    private val smsAlertManager: SmsAlertManager,
    private val whatsAppManager: WhatsAppManager,
    private val locationHelper: LocationHelper,
    private val mensajes: AlertMessageBuilder
) {

    private val _llamadaAlAbrir = MutableStateFlow(preferencesManager.isLlamadaAlAbrirActiva())
    val llamadaAlAbrir: StateFlow<Boolean> = _llamadaAlAbrir.asStateFlow()

    private val _dobleAperturaAutoLlamada =
        MutableStateFlow(preferencesManager.isDobleAperturaAutoLlamadaActiva())
    val dobleAperturaAutoLlamada: StateFlow<Boolean> = _dobleAperturaAutoLlamada.asStateFlow()

    private val _numeroSms = MutableStateFlow(preferencesManager.getNumeroSms())
    val numeroSms: StateFlow<String> = _numeroSms.asStateFlow()

    private val _nombreContacto = MutableStateFlow(preferencesManager.getNombreContacto())
    val nombreContacto: StateFlow<String> = _nombreContacto.asStateFlow()

    private val _smsActivo = MutableStateFlow(preferencesManager.isSmsActivo())
    val smsActivo: StateFlow<Boolean> = _smsActivo.asStateFlow()

    private val _whatsAppActivo = MutableStateFlow(preferencesManager.isWhatsAppActivo())
    val whatsAppActivo: StateFlow<Boolean> = _whatsAppActivo.asStateFlow()

    private val _seguimientoActivo = MutableStateFlow(preferencesManager.isSeguimientoActivo())
    val seguimientoActivo: StateFlow<Boolean> = _seguimientoActivo.asStateFlow()

    private val _intervaloSeguimientoMin = MutableStateFlow(preferencesManager.getIntervaloSeguimientoMin())
    val intervaloSeguimientoMin: StateFlow<Int> = _intervaloSeguimientoMin.asStateFlow()

    private val _llamarEmergencia = MutableStateFlow(preferencesManager.isLlamarEmergenciaActivo())
    val llamarEmergencia: StateFlow<Boolean> = _llamarEmergencia.asStateFlow()

    private val _numeroEmergencia = MutableStateFlow(preferencesManager.getNumeroEmergencia())
    val numeroEmergencia: StateFlow<String> = _numeroEmergencia.asStateFlow()

    private val _nombreUsuario = MutableStateFlow(preferencesManager.getNombreUsuario())
    val nombreUsuario: StateFlow<String> = _nombreUsuario.asStateFlow()

    private val _estadoAlerta = MutableStateFlow<EstadoAlerta>(EstadoAlerta.Inactiva)
    val estadoAlerta: StateFlow<EstadoAlerta> = _estadoAlerta.asStateFlow()

    private val _seguimientoEnCurso = MutableStateFlow(false)
    val seguimientoEnCurso: StateFlow<Boolean> = _seguimientoEnCurso.asStateFlow()

    // ---------- Configuración ----------

    fun setLlamadaAlAbrir(activo: Boolean) {
        preferencesManager.setLlamadaAlAbrir(activo)
        _llamadaAlAbrir.value = activo
    }

    fun setDobleAperturaAutoLlamada(activo: Boolean) {
        preferencesManager.setDobleAperturaAutoLlamada(activo)
        _dobleAperturaAutoLlamada.value = activo
    }

    /** Guarda el número del contacto, conservando solo dígitos y el signo +. */
    fun setNumeroSms(numero: String) {
        val limpio = limpiarNumero(numero)
        preferencesManager.setNumeroSms(limpio)
        _numeroSms.value = limpio
    }

    fun setNombreContacto(nombre: String) {
        preferencesManager.setNombreContacto(nombre.trim())
        _nombreContacto.value = nombre.trim()
    }

    fun setSmsActivo(activo: Boolean) {
        preferencesManager.setSmsActivo(activo)
        _smsActivo.value = activo
    }

    fun setWhatsAppActivo(activo: Boolean) {
        preferencesManager.setWhatsAppActivo(activo)
        _whatsAppActivo.value = activo
    }

    fun setSeguimientoActivo(activo: Boolean) {
        preferencesManager.setSeguimientoActivo(activo)
        _seguimientoActivo.value = activo
    }

    fun setIntervaloSeguimientoMin(minutos: Int) {
        preferencesManager.setIntervaloSeguimientoMin(minutos)
        _intervaloSeguimientoMin.value = minutos
    }

    fun setLlamarEmergencia(activo: Boolean) {
        preferencesManager.setLlamarEmergenciaActivo(activo)
        _llamarEmergencia.value = activo
    }

    fun setNumeroEmergencia(numero: String) {
        val limpio = limpiarNumero(numero)
        preferencesManager.setNumeroEmergencia(limpio)
        _numeroEmergencia.value = limpio
    }

    fun setNombreUsuario(nombre: String) {
        preferencesManager.setNombreUsuario(nombre.trim())
        _nombreUsuario.value = nombre.trim()
    }

    fun whatsAppInstalado(): Boolean = whatsAppManager.estaInstalado()

    fun autoEnvioWhatsAppDisponible(): Boolean = whatsAppManager.autoEnvioDisponible()

    fun haVistoBienvenida(): Boolean = preferencesManager.haVistoBienvenida()

    fun marcarBienvenidaVista() {
        preferencesManager.setBienvenidaVista(true)
    }

    // ---------- Cuenta local (PIN) ----------

    fun tieneCuenta(): Boolean = preferencesManager.getPinHash().isNotEmpty()

    fun registrarCuenta(nombre: String, pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.toHex()
        preferencesManager.setPin(hashPin(pin, salt), salt)
        setNombreUsuario(nombre)
    }

    fun validarPin(pin: String): Boolean =
        tieneCuenta() && hashPin(pin, preferencesManager.getPinSalt()) == preferencesManager.getPinHash()

    private fun hashPin(pin: String, salt: String): String =
        MessageDigest.getInstance("SHA-256").digest("$salt:$pin".toByteArray()).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    // ---------- Activación ----------

    /**
     * Llamado desde el arranque de la app (MainViewModel).
     * Devuelve true si correspondió disparar la alerta automática.
     */
    fun procesarAperturaDeApp(): Boolean {
        val esDoble = appOpenTracker.registrarAperturaYEsDoble()
        return esDoble && _dobleAperturaAutoLlamada.value
    }

    /** La siguiente vuelta a primer plano (desde WhatsApp, ajustes...) no cuenta como apertura. */
    fun omitirSiguienteApertura() {
        appOpenTracker.omitirSiguienteApertura()
    }

    /**
     * Dispara la alerta completa:
     * 1. Obtiene la ubicación (máx. ~8 s).
     * 2. Envía el SMS con la ubicación (silencioso).
     * 3. Inicia el seguimiento en tiempo real.
     * 4. Abre WhatsApp con el mensaje y la ubicación (se envía solo si el
     *    servicio de accesibilidad está activo).
     * 5. Llama al número de emergencia, dando unos segundos a WhatsApp antes.
     */
    fun activarEmergencia() {
        if (_estadoAlerta.value == EstadoAlerta.Enviando) return
        _estadoAlerta.value = EstadoAlerta.Enviando
        appOpenTracker.iniciarEnfriamiento()

        scope.launch {
            val ubicacion = locationHelper.obtenerUbicacionActual()
            val numero = _numeroSms.value
            val nombre = _nombreUsuario.value
            val seguir = _seguimientoActivo.value && locationHelper.tienePermiso()
            val intervalo = if (seguir) _intervaloSeguimientoMin.value else null

            val sms = _smsActivo.value &&
                smsAlertManager.enviarAlerta(numero, mensajes.alerta(nombre, ubicacion, intervalo, paraWhatsApp = false))

            // El servicio en primer plano debe arrancar mientras la app sigue visible
            // (Android 12+), es decir, antes de abrir WhatsApp.
            val seguimiento = seguir && runCatching { TrackingService.iniciar(context) }.isSuccess

            val whatsApp = if (_whatsAppActivo.value) {
                appOpenTracker.omitirSiguienteApertura()
                whatsAppManager.enviar(numero, mensajes.alerta(nombre, ubicacion, intervalo, paraWhatsApp = true))
            } else {
                ResultadoWhatsApp.DESACTIVADO
            }

            var llamada = false
            if (_llamarEmergencia.value) {
                delay(
                    when (whatsApp) {
                        ResultadoWhatsApp.ENVIO_AUTOMATICO -> ESPERA_LLAMADA_AUTO_MS
                        ResultadoWhatsApp.ABIERTO_PARA_ENVIAR -> ESPERA_LLAMADA_MANUAL_MS
                        else -> 0L
                    }
                )
                llamada = emergencyCallManager.llamar(_numeroEmergencia.value)
            }

            _estadoAlerta.value = EstadoAlerta.Enviada(
                hora = System.currentTimeMillis(),
                ubicacionObtenida = ubicacion != null,
                smsEnviado = sms,
                whatsApp = whatsApp,
                seguimientoIniciado = seguimiento,
                llamadaIniciada = llamada
            )
        }
    }

    /**
     * Envía un mensaje de prueba por los canales activos, sin llamar ni
     * iniciar seguimiento. Devuelve (sms enviado, resultado de WhatsApp).
     */
    suspend fun enviarPrueba(): Pair<Boolean, ResultadoWhatsApp> {
        val ubicacion = locationHelper.obtenerUbicacionActual()
        val numero = _numeroSms.value
        val nombre = _nombreUsuario.value
        val sms = _smsActivo.value &&
            smsAlertManager.enviarAlerta(numero, mensajes.prueba(nombre, ubicacion, paraWhatsApp = false))
        val whatsApp = if (_whatsAppActivo.value) {
            appOpenTracker.omitirSiguienteApertura()
            whatsAppManager.enviar(numero, mensajes.prueba(nombre, ubicacion, paraWhatsApp = true))
        } else {
            ResultadoWhatsApp.DESACTIVADO
        }
        return sms to whatsApp
    }

    // ---------- Seguimiento en tiempo real (lo usa TrackingService) ----------

    fun detenerSeguimiento() {
        TrackingService.detener(context)
    }

    fun onSeguimientoIniciado() {
        _seguimientoEnCurso.value = true
    }

    fun onSeguimientoDetenido() {
        _seguimientoEnCurso.value = false
    }

    /**
     * Envía la ubicación actual. Por WhatsApp solo si el envío es automático:
     * abrir el chat cada pocos minutos esperando un toque no tendría sentido.
     */
    suspend fun enviarActualizacionDeUbicacion(numeroEnvio: Int): Location? {
        val ubicacion = locationHelper.obtenerUbicacionActual()
        val numero = _numeroSms.value
        if (_smsActivo.value) {
            smsAlertManager.enviarAlerta(numero, mensajes.actualizacion(numeroEnvio, ubicacion, paraWhatsApp = false))
        }
        if (_whatsAppActivo.value && whatsAppManager.autoEnvioDisponible()) {
            whatsAppManager.enviar(numero, mensajes.actualizacion(numeroEnvio, ubicacion, paraWhatsApp = true))
        }
        return ubicacion
    }

    suspend fun enviarFinDeSeguimiento() {
        val ubicacion = locationHelper.obtenerUbicacionActual(timeoutMs = 4_000L)
        if (_smsActivo.value) {
            smsAlertManager.enviarAlerta(_numeroSms.value, mensajes.finSeguimiento(ubicacion))
        }
        _estadoAlerta.value = EstadoAlerta.Inactiva
    }

    private fun limpiarNumero(numero: String): String =
        numero.filterIndexed { i, c -> c.isDigit() || (c == '+' && i == 0) }

    companion object {
        /** Tiempo para que el servicio de accesibilidad pulse "Enviar" antes de la llamada. */
        private const val ESPERA_LLAMADA_AUTO_MS = 6_000L
        /** Tiempo para que el usuario toque "Enviar" en WhatsApp antes de la llamada. */
        private const val ESPERA_LLAMADA_MANUAL_MS = 15_000L
    }
}
