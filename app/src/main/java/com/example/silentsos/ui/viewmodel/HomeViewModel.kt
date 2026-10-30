package com.example.silentsos.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.silentsos.data.repository.EmergencyRepository
import com.example.silentsos.domain.model.EstadoAlerta
import com.example.silentsos.domain.usecase.LlamarEmergenciaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: EmergencyRepository,
    private val llamarEmergencia: LlamarEmergenciaUseCase
) : ViewModel() {

    val nombreUsuario: StateFlow<String> = repository.nombreUsuario
    val nombreContacto: StateFlow<String> = repository.nombreContacto
    val numeroContacto: StateFlow<String> = repository.numeroSms
    val estadoAlerta: StateFlow<EstadoAlerta> = repository.estadoAlerta
    val seguimientoEnCurso: StateFlow<Boolean> = repository.seguimientoEnCurso
    val intervaloSeguimientoMin: StateFlow<Int> = repository.intervaloSeguimientoMin

    fun activarSos() {
        llamarEmergencia()
    }

    fun detenerSeguimiento() {
        repository.detenerSeguimiento()
    }
}
