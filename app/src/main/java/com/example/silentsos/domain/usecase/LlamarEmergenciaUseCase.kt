package com.example.silentsos.domain.usecase

import com.example.silentsos.data.repository.EmergencyRepository
import javax.inject.Inject

/**
 * Alerta de emergencia disparada manualmente por la usuaria
 * (botón SOS, después de la cuenta regresiva de confirmación).
 */
class LlamarEmergenciaUseCase @Inject constructor(
    private val repository: EmergencyRepository
) {
    operator fun invoke() {
        repository.activarEmergencia()
    }
}
