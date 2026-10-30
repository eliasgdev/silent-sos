package com.example.silentsos.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.silentsos.data.repository.EmergencyRepository
import com.example.silentsos.domain.usecase.ProcesarAperturaDeAppUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: EmergencyRepository,
    private val procesarAperturaDeApp: ProcesarAperturaDeAppUseCase
) : ViewModel() {

    val haVistoBienvenida: Boolean get() = repository.haVistoBienvenida()

    /**
     * Llamar cada vez que la app pasa a primer plano (MainActivity.onStart).
     * Dispara la alerta automática si corresponde por doble apertura.
     */
    fun onAppAbierta() {
        procesarAperturaDeApp()
    }
}
