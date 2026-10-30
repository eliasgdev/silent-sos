package com.example.silentsos.domain.usecase

import com.example.silentsos.data.repository.EmergencyRepository
import com.example.silentsos.domain.model.OrigenAlerta
import javax.inject.Inject

/**
 * Alerta de emergencia disparada manualmente por la usuaria
 * (botón SOS o acceso rápido, después de la cuenta regresiva de confirmación).
 */
class LlamarEmergenciaUseCase @Inject constructor(
    private val repository: EmergencyRepository
) {
    operator fun invoke(origen: OrigenAlerta = OrigenAlerta.BOTON_SOS) {
        repository.activarEmergencia(origen)
    }
}
