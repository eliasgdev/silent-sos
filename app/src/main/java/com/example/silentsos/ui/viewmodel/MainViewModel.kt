package com.example.silentsos.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.silentsos.data.repository.EmergencyRepository
import com.example.silentsos.domain.model.EstadoAlerta
import com.example.silentsos.domain.model.OrigenAlerta
import com.example.silentsos.domain.usecase.LlamarEmergenciaUseCase
import com.example.silentsos.domain.usecase.ProcesarAperturaDeAppUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: EmergencyRepository,
    private val procesarAperturaDeApp: ProcesarAperturaDeAppUseCase,
    private val llamarEmergencia: LlamarEmergenciaUseCase
) : ViewModel() {

    val haVistoBienvenida: Boolean get() = repository.haVistoBienvenida()

    val estadoAlerta: StateFlow<EstadoAlerta> = repository.estadoAlerta

    fun descartarAvisoSinContactos() = repository.descartarAvisoSinContactos()

    /** true mientras se muestra la cuenta regresiva de un acceso rápido (atajo o ajuste rápido). */
    private val _sosRapidoPendiente = MutableStateFlow(false)
    val sosRapidoPendiente: StateFlow<Boolean> = _sosRapidoPendiente.asStateFlow()

    /**
     * Llamar cada vez que la app pasa a primer plano (MainActivity.onStart).
     * Dispara la alerta automática si corresponde por doble apertura.
     */
    fun onAppAbierta() {
        procesarAperturaDeApp()
    }

    fun omitirSiguienteApertura() = repository.omitirSiguienteApertura()

    /** Llega un intent de acceso rápido: se muestra la cuenta regresiva sobre cualquier pantalla. */
    fun solicitarSosRapido() {
        _sosRapidoPendiente.value = true
    }

    fun confirmarSosRapido() {
        _sosRapidoPendiente.value = false
        llamarEmergencia(OrigenAlerta.ACCESO_RAPIDO)
    }

    fun cancelarSosRapido() {
        _sosRapidoPendiente.value = false
    }
}
