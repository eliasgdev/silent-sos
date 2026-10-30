package com.example.silentsos.data.repository

import android.content.Context
import android.location.Location
import com.example.silentsos.data.PreferencesManager
import com.example.silentsos.di.ApplicationScope
import com.example.silentsos.domain.model.ContactoEmergencia
import com.example.silentsos.domain.model.EstadoAlerta
import com.example.silentsos.domain.model.OrigenAlerta
import com.example.silentsos.domain.model.RegistroAlerta
import com.example.silentsos.service.TrackingService
import com.example.silentsos.service.WhatsAppAutoSendService
import com.example.silentsos.utils.AlertMessageBuilder
import com.example.silentsos.utils.AppOpenTracker
import com.example.silentsos.utils.EstadoSms
import com.example.silentsos.utils.EmergencyCallManager
import com.example.silentsos.utils.LocationHelper
import com.example.silentsos.utils.PinHasher
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

    private val _contactos = MutableStateFlow(preferencesManager.getContactos())
    val contactos: StateFlow<List<ContactoEmergencia>> = _contactos.asStateFlow()

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

    private val _historial = MutableStateFlow(preferencesManager.getHistorial())
    val historial: StateFlow<List<RegistroAlerta>> = _historial.asStateFlow()

    private val _estadoAlerta = MutableStateFlow<EstadoAlerta>(EstadoAlerta.Inactiva)
    val estadoAlerta: StateFlow<EstadoAlerta> = _estadoAlerta.asStateFlow()

    /** Envío y entrega del último SMS a cada número (confirmados por el operador). */
    val estadosSms: StateFlow<Map<String, EstadoSms>> get() = smsAlertManager.estados

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

    /** Devuelve un mensaje de error, o null si el contacto se agregó. */
    fun agregarContacto(nombre: String, numero: String): String? {
        val limpio = ContactoEmergencia.limpiarNumero(numero)
        val actuales = _contactos.value
        val error = when {
            actuales.size >= ContactoEmergencia.MAXIMO -> "Máximo ${ContactoEmergencia.MAXIMO} contactos"
            limpio.count { it.isDigit() } < 7 -> "Número no válido"
            actuales.any { it.numero == limpio } -> "Ese número ya está en la lista"
            else -> null
        }
        if (error == null) guardarContactos(actuales + ContactoEmergencia(nombre.trim(), limpio))
        return error
    }

    fun eliminarContacto(contacto: ContactoEmergencia) {
        guardarContactos(_contactos.value - contacto)
    }

    private fun guardarContactos(contactos: List<ContactoEmergencia>) {
        preferencesManager.setContactos(contactos)
        _contactos.value = contactos
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
        val limpio = ContactoEmergencia.limpiarNumero(numero)
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
        guardarPin(pin)
        setNombreUsuario(nombre)
    }

    fun validarPin(pin: String): Boolean =
        tieneCuenta() && PinHasher.hash(pin, preferencesManager.getPinSalt()) == preferencesManager.getPinHash()

    /** Devuelve un mensaje de error, o null si el PIN se cambió. */
    fun cambiarPin(actual: String, nuevo: String): String? = when {
        !validarPin(actual) -> "El PIN actual no es correcto"
        !PinHasher.esPinValido(nuevo) -> "El PIN nuevo debe tener entre 4 y 6 dígitos"
        else -> {
            guardarPin(nuevo)
            null
        }
    }

    private fun guardarPin(pin: String) {
        val salt = PinHasher.nuevoSalt()
        preferencesManager.setPin(PinHasher.hash(pin, salt), salt)
    }

    // ---------- Historial ----------

    private fun registrarEnHistorial(registro: RegistroAlerta) {
        val nuevo = (listOf(registro) + _historial.value).take(RegistroAlerta.MAXIMO_GUARDADOS)
        preferencesManager.setHistorial(nuevo)
        _historial.value = nuevo
    }

    fun borrarHistorial() {
        preferencesManager.setHistorial(emptyList())
        _historial.value = emptyList()
    }

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
     * 2. Envía el SMS con la ubicación a todos los contactos (silencioso).
     * 3. Inicia el seguimiento en tiempo real.
     * 4. Abre WhatsApp con el mensaje y la ubicación (se envía solo si el
     *    servicio de accesibilidad está activo, y en ese caso a todos los contactos).
     * 5. Llama al número de emergencia, dando unos segundos a WhatsApp antes.
     */
    /** La usuaria ya leyó el aviso de "sin contactos". */
    fun descartarAvisoSinContactos() {
        if (_estadoAlerta.value is EstadoAlerta.SinContactos) _estadoAlerta.value = EstadoAlerta.Inactiva
    }

    fun activarEmergencia(origen: OrigenAlerta) {
        if (_estadoAlerta.value == EstadoAlerta.Enviando) return
        _estadoAlerta.value = EstadoAlerta.Enviando
        appOpenTracker.iniciarEnfriamiento()

        scope.launch {
            if (_contactos.value.isEmpty()) {
                // Sin contactos no hay a quién avisar: solo se hace la llamada (si está
                // activa) y se informa a la usuaria para que no crea que la alerta salió.
                val llamada = _llamarEmergencia.value && emergencyCallManager.llamar(_numeroEmergencia.value)
                _estadoAlerta.value = EstadoAlerta.SinContactos(llamada)
                return@launch
            }
            val ubicacion = locationHelper.obtenerUbicacionActual()
            val contactos = _contactos.value
            val nombre = _nombreUsuario.value
            val seguir = _seguimientoActivo.value && locationHelper.tienePermiso()
            val intervalo = if (seguir) _intervaloSeguimientoMin.value else null

            val sms = enviarSmsATodos(contactos, mensajes.alerta(nombre, ubicacion, intervalo, paraWhatsApp = false))

            // El servicio en primer plano debe arrancar mientras la app sigue visible
            // (Android 12+), es decir, antes de abrir WhatsApp.
            val seguimiento = seguir && runCatching { TrackingService.iniciar(context) }.isSuccess

            val (whatsApp, whatsAppEnviados) =
                enviarWhatsAppATodos(
                    contactos,
                    mensajes.alerta(nombre, ubicacion, intervalo, paraWhatsApp = true),
                    WhatsAppAutoSendService.DuracionUbicacion.HORAS_8
                )

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
                contactos = contactos.size,
                smsEnviados = sms,
                whatsApp = whatsApp,
                seguimientoIniciado = seguimiento,
                llamadaIniciada = llamada
            )
            registrarEnHistorial(
                RegistroAlerta(
                    hora = System.currentTimeMillis(),
                    origen = origen,
                    latitud = ubicacion?.latitude,
                    longitud = ubicacion?.longitude,
                    smsEnviados = sms,
                    whatsAppEnviados = whatsAppEnviados,
                    contactos = contactos.size,
                    llamada = llamada
                )
            )
        }
    }

    /**
     * Envía un mensaje de prueba por los canales activos, sin llamar ni
     * iniciar seguimiento. Devuelve (SMS enviados, resultado de WhatsApp).
     */
    suspend fun enviarPrueba(): Pair<Int, ResultadoWhatsApp> {
        val ubicacion = locationHelper.obtenerUbicacionActual()
        val contactos = _contactos.value
        val nombre = _nombreUsuario.value
        val sms = enviarSmsATodos(contactos, mensajes.prueba(nombre, ubicacion, paraWhatsApp = false))
        val (whatsApp, whatsAppEnviados) =
            enviarWhatsAppATodos(
                contactos,
                mensajes.prueba(nombre, ubicacion, paraWhatsApp = true),
                WhatsAppAutoSendService.DuracionUbicacion.MIN_15
            )
        registrarEnHistorial(
            RegistroAlerta(
                hora = System.currentTimeMillis(),
                origen = OrigenAlerta.PRUEBA,
                latitud = ubicacion?.latitude,
                longitud = ubicacion?.longitude,
                smsEnviados = sms,
                whatsAppEnviados = whatsAppEnviados,
                contactos = contactos.size,
                llamada = false
            )
        )
        return sms to whatsApp
    }

    /** Devuelve cuántos SMS se entregaron al sistema para envío. */
    private fun enviarSmsATodos(contactos: List<ContactoEmergencia>, mensaje: String): Int {
        if (!_smsActivo.value) return 0
        smsAlertManager.reiniciarEstados()
        return contactos.count { smsAlertManager.enviarAlerta(it.numero, mensaje) }
    }

    /**
     * Con envío automático, abre el chat de cada contacto por turnos (el servicio
     * de accesibilidad pulsa "Enviar" y vuelve al inicio antes del siguiente).
     * Sin envío automático solo se abre el del primer contacto: abrir varios
     * chats seguidos esperando un toque en cada uno no es práctico en una emergencia.
     *
     * Devuelve el resultado del primer intento y cuántos chats se enviaron o abrieron.
     */
    private suspend fun enviarWhatsAppATodos(
        contactos: List<ContactoEmergencia>,
        mensaje: String,
        ubicacionEnVivo: WhatsAppAutoSendService.DuracionUbicacion?
    ): Pair<ResultadoWhatsApp, Int> {
        if (!_whatsAppActivo.value) return ResultadoWhatsApp.DESACTIVADO to 0
        if (contactos.isEmpty()) return ResultadoWhatsApp.SIN_NUMERO to 0

        val automatico = whatsAppManager.autoEnvioDisponible()
        val destinatarios = if (automatico) contactos else contactos.take(1)

        var primero: ResultadoWhatsApp? = null
        var enviados = 0
        destinatarios.forEachIndexed { i, contacto ->
            if (i > 0) WhatsAppAutoSendService.esperarFin()
            appOpenTracker.omitirSiguienteApertura()
            val resultado = whatsAppManager.enviar(contacto.numero, mensaje, ubicacionEnVivo)
            if (primero == null) primero = resultado
            if (resultado == ResultadoWhatsApp.ENVIO_AUTOMATICO || resultado == ResultadoWhatsApp.ABIERTO_PARA_ENVIAR) {
                enviados++
            }
        }
        // Que la llamada no interrumpa al servicio mientras completa el último chat
        if (automatico) WhatsAppAutoSendService.esperarFin()
        return (primero ?: ResultadoWhatsApp.ERROR) to enviados
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

    /** Envía la ubicación actual por SMS (ver la nota de WhatsApp más abajo). */
    suspend fun enviarActualizacionDeUbicacion(numeroEnvio: Int): Location? {
        val ubicacion = locationHelper.obtenerUbicacionActual()
        val contactos = _contactos.value
        enviarSmsATodos(contactos, mensajes.actualizacion(numeroEnvio, ubicacion, paraWhatsApp = false))
        // Por WhatsApp no hace falta: el contacto ya recibió la ubicación en tiempo
        // real de WhatsApp con la alerta, y abrir WhatsApp cada pocos minutos
        // interrumpiría a la usuaria.
        return ubicacion
    }

    suspend fun enviarFinDeSeguimiento() {
        val ubicacion = locationHelper.obtenerUbicacionActual(timeoutMs = 4_000L)
        enviarSmsATodos(_contactos.value, mensajes.finSeguimiento(ubicacion))
        _estadoAlerta.value = EstadoAlerta.Inactiva
    }

    companion object {
        /** Margen tras volver de WhatsApp (el servicio ya terminó) antes de la llamada. */
        private const val ESPERA_LLAMADA_AUTO_MS = 2_000L
        /** Tiempo para que el usuario toque "Enviar" en WhatsApp antes de la llamada. */
        private const val ESPERA_LLAMADA_MANUAL_MS = 15_000L
    }
}
