package com.example.silentsos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.silentsos.data.repository.EmergencyRepository
import com.example.silentsos.utils.ResultadoWhatsApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Expone el estado de la configuración y delega los cambios al repositorio.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: EmergencyRepository
) : ViewModel() {

    val llamadaAlAbrir: StateFlow<Boolean> = repository.llamadaAlAbrir
    val dobleAperturaAutoLlamada: StateFlow<Boolean> = repository.dobleAperturaAutoLlamada
    val numeroSms: StateFlow<String> = repository.numeroSms
    val nombreContacto: StateFlow<String> = repository.nombreContacto
    val smsActivo: StateFlow<Boolean> = repository.smsActivo
    val whatsAppActivo: StateFlow<Boolean> = repository.whatsAppActivo
    val seguimientoActivo: StateFlow<Boolean> = repository.seguimientoActivo
    val intervaloSeguimientoMin: StateFlow<Int> = repository.intervaloSeguimientoMin
    val llamarEmergencia: StateFlow<Boolean> = repository.llamarEmergencia
    val numeroEmergencia: StateFlow<String> = repository.numeroEmergencia
    val nombreUsuario: StateFlow<String> = repository.nombreUsuario

    private val _enviandoPrueba = MutableStateFlow(false)
    val enviandoPrueba: StateFlow<Boolean> = _enviandoPrueba.asStateFlow()

    fun onLlamadaAlAbrirChange(activo: Boolean) = repository.setLlamadaAlAbrir(activo)
    fun onDobleAperturaAutoLlamadaChange(activo: Boolean) = repository.setDobleAperturaAutoLlamada(activo)
    fun onNumeroSmsChange(numero: String) = repository.setNumeroSms(numero)
    fun onNombreContactoChange(nombre: String) = repository.setNombreContacto(nombre)
    fun onSmsActivoChange(activo: Boolean) = repository.setSmsActivo(activo)
    fun onWhatsAppActivoChange(activo: Boolean) = repository.setWhatsAppActivo(activo)
    fun onSeguimientoActivoChange(activo: Boolean) = repository.setSeguimientoActivo(activo)
    fun onIntervaloChange(minutos: Int) = repository.setIntervaloSeguimientoMin(minutos)
    fun onLlamarEmergenciaChange(activo: Boolean) = repository.setLlamarEmergencia(activo)
    fun onNumeroEmergenciaChange(numero: String) = repository.setNumeroEmergencia(numero)
    fun onNombreUsuarioChange(nombre: String) = repository.setNombreUsuario(nombre)

    fun whatsAppInstalado(): Boolean = repository.whatsAppInstalado()
    fun autoEnvioWhatsAppDisponible(): Boolean = repository.autoEnvioWhatsAppDisponible()

    /** Antes de abrir una pantalla del sistema, para que al volver no cuente como apertura. */
    fun antesDeSalirDeLaApp() = repository.omitirSiguienteApertura()

    /** Envía el mensaje de prueba y entrega un texto con el resultado. */
    fun enviarPrueba(onResultado: (String) -> Unit) {
        if (_enviandoPrueba.value) return
        _enviandoPrueba.value = true
        viewModelScope.launch {
            val (sms, whatsApp) = repository.enviarPrueba()
            _enviandoPrueba.value = false
            val textoSms = if (smsActivo.value) {
                if (sms) "SMS enviado" else "SMS no enviado (revisa número y permiso)"
            } else null
            val textoWhatsApp = when (whatsApp) {
                ResultadoWhatsApp.ENVIO_AUTOMATICO -> "WhatsApp enviado automáticamente"
                ResultadoWhatsApp.ABIERTO_PARA_ENVIAR -> "WhatsApp abierto: toca Enviar"
                ResultadoWhatsApp.NO_INSTALADO -> "WhatsApp no está instalado"
                ResultadoWhatsApp.SIN_NUMERO -> "Falta el número del contacto"
                ResultadoWhatsApp.ERROR -> "No se pudo abrir WhatsApp"
                ResultadoWhatsApp.DESACTIVADO -> null
            }
            onResultado(listOfNotNull(textoSms, textoWhatsApp).joinToString(" · ").ifEmpty { "No hay canales activos" })
        }
    }
}
