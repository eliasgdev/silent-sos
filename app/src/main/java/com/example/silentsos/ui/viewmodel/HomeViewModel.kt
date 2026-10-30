package com.example.silentsos.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.silentsos.data.repository.EmergencyRepository
import com.example.silentsos.domain.model.ContactoEmergencia
import com.example.silentsos.domain.model.EstadoAlerta
import com.example.silentsos.domain.usecase.LlamarEmergenciaUseCase
import com.example.silentsos.utils.EstadoSms
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: EmergencyRepository,
    private val llamarEmergencia: LlamarEmergenciaUseCase
) : ViewModel() {

    val nombreUsuario: StateFlow<String> = repository.nombreUsuario
    val contactos: StateFlow<List<ContactoEmergencia>> = repository.contactos
    val estadoAlerta: StateFlow<EstadoAlerta> = repository.estadoAlerta
    val seguimientoEnCurso: StateFlow<Boolean> = repository.seguimientoEnCurso
    val intervaloSeguimientoMin: StateFlow<Int> = repository.intervaloSeguimientoMin
    val estadosSms: StateFlow<Map<String, EstadoSms>> = repository.estadosSms

    fun activarSos() {
        llamarEmergencia()
    }

    /** WhatsApp activado como canal e instalado, pero sin el servicio que envía solo. */
    fun faltaAutoEnvioWhatsApp(): Boolean =
        repository.whatsAppActivo.value && repository.whatsAppInstalado() && !repository.autoEnvioWhatsAppDisponible()

    fun detenerSeguimiento() {
        repository.detenerSeguimiento()
    }
}
