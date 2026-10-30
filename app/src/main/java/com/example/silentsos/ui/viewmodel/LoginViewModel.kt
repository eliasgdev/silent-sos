package com.example.silentsos.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.silentsos.data.repository.EmergencyRepository
import com.example.silentsos.domain.usecase.LlamarEmergenciaUseCase
import com.example.silentsos.utils.PinHasher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class LoginUiState(
    /** Sin cuenta todavía: se muestra el formulario para crear el PIN. */
    val modoRegistro: Boolean,
    val nombre: String,
    val error: String? = null
)

/**
 * Cuenta local protegida con PIN: evita que otra persona con el teléfono
 * cambie o desactive la configuración de emergencia.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: EmergencyRepository,
    private val llamarEmergencia: LlamarEmergenciaUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LoginUiState(modoRegistro = !repository.tieneCuenta(), nombre = repository.nombreUsuario.value)
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /** Opción 1 de configuración: mostrar el botón SOS antes de pedir el PIN. */
    val sosEnIngreso: StateFlow<Boolean> = repository.llamadaAlAbrir

    /** Devuelve true si el PIN es correcto. */
    fun ingresar(pin: String): Boolean {
        val ok = repository.validarPin(pin)
        _uiState.update { it.copy(error = if (ok) null else "PIN incorrecto") }
        return ok
    }

    /** Devuelve true si la cuenta quedó creada. */
    fun registrar(nombre: String, pin: String, confirmacion: String): Boolean {
        val error = when {
            nombre.isBlank() -> "Escribe tu nombre (se incluye en los mensajes de alerta)"
            !PinHasher.esPinValido(pin) -> "El PIN debe tener entre 4 y 6 dígitos"
            pin != confirmacion -> "Los PIN no coinciden"
            else -> null
        }
        _uiState.update { it.copy(error = error) }
        if (error != null) return false
        repository.registrarCuenta(nombre, pin)
        return true
    }

    fun activarSos() {
        llamarEmergencia()
    }
}
