package com.example.silentsos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.silentsos.ui.viewmodel.LoginViewModel

/**
 * Ingreso con PIN local. La primera vez pide crear la cuenta (nombre + PIN).
 * Si la opción "SOS en la pantalla de ingreso" está activa, el botón SOS
 * aparece aquí mismo para no tener que escribir el PIN en una emergencia.
 */
@Composable
fun LoginScreen(
    onIngresarClick: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sosEnIngreso by viewModel.sosEnIngreso.collectAsState()

    var nombre by remember { mutableStateOf(uiState.nombre) }
    var pin by remember { mutableStateOf("") }
    var confirmacion by remember { mutableStateOf("") }

    val soloDigitos: (String) -> String = { it.filter(Char::isDigit).take(6) }
    val tecladoPin = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp)
    ) {
        if (sosEnIngreso && !uiState.modoRegistro) {
            SosButton(onActivar = viewModel::activarSos, tamano = 160.dp)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text(
            text = if (uiState.modoRegistro) "Crea tu cuenta" else "Hola${uiState.nombre.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""}",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        if (uiState.modoRegistro) {
            Text(
                text = "El PIN protege tu configuración de emergencia para que nadie más pueda cambiarla.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Tu nombre") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        OutlinedTextField(
            value = pin,
            onValueChange = { pin = soloDigitos(it) },
            label = { Text(if (uiState.modoRegistro) "PIN (4 a 6 dígitos)" else "PIN") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = tecladoPin,
            modifier = Modifier.fillMaxWidth()
        )

        if (uiState.modoRegistro) {
            OutlinedTextField(
                value = confirmacion,
                onValueChange = { confirmacion = soloDigitos(it) },
                label = { Text("Confirma el PIN") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = tecladoPin,
                modifier = Modifier.fillMaxWidth()
            )
        }

        uiState.error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
        }

        Button(
            onClick = {
                val ok = if (uiState.modoRegistro) {
                    viewModel.registrar(nombre, pin, confirmacion)
                } else {
                    viewModel.ingresar(pin)
                }
                if (ok) onIngresarClick() else if (!uiState.modoRegistro) pin = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (uiState.modoRegistro) "Crear cuenta" else "Ingresar")
        }
    }
}
