package com.example.silentsos.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.silentsos.data.repository.EmergencyRepository
import com.example.silentsos.domain.model.RegistroAlerta
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class HistorialViewModel @Inject constructor(
    private val repository: EmergencyRepository
) : ViewModel() {

    val historial: StateFlow<List<RegistroAlerta>> = repository.historial

    fun borrarHistorial() {
        repository.borrarHistorial()
    }

    /** Abrir el mapa sale de la app: al volver no debe contar como apertura. */
    fun antesDeSalirDeLaApp() = repository.omitirSiguienteApertura()
}
