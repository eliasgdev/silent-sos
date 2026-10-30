package com.example.silentsos.domain.usecase

import com.example.silentsos.data.repository.EmergencyRepository
import com.example.silentsos.domain.model.OrigenAlerta
import javax.inject.Inject

/**
 * Se ejecuta cada vez que la app pasa a primer plano (MainActivity.onStart).
 * Decide, según configuración y AppOpenTracker, si corresponde
 * disparar la alerta automática por doble apertura.
 */
class ProcesarAperturaDeAppUseCase @Inject constructor(
    private val repository: EmergencyRepository
) {
    /** Devuelve true si se disparó la alerta. */
    operator fun invoke(): Boolean {
        val debeAlertar = repository.procesarAperturaDeApp()
        if (debeAlertar) repository.activarEmergencia(OrigenAlerta.DOBLE_APERTURA)
        return debeAlertar
    }
}
