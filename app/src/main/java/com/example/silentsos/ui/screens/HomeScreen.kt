package com.example.silentsos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.silentsos.domain.model.EstadoAlerta
import com.example.silentsos.ui.theme.OkGreen
import com.example.silentsos.ui.theme.SosRed
import com.example.silentsos.ui.viewmodel.HomeViewModel
import com.example.silentsos.utils.ResultadoWhatsApp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla principal: botón SOS, contacto configurado, estado de la última
 * alerta y del seguimiento en tiempo real.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onConfiguracionClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val nombreUsuario by viewModel.nombreUsuario.collectAsState()
    val nombreContacto by viewModel.nombreContacto.collectAsState()
    val numeroContacto by viewModel.numeroContacto.collectAsState()
    val estado by viewModel.estadoAlerta.collectAsState()
    val seguimiento by viewModel.seguimientoEnCurso.collectAsState()
    val intervalo by viewModel.intervaloSeguimientoMin.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SilentSOS", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onConfiguracionClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Configuración")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            if (nombreUsuario.isNotBlank()) {
                Text("Hola, $nombreUsuario", style = MaterialTheme.typography.titleLarge)
            }

            if (numeroContacto.isBlank()) {
                AvisoCard(
                    texto = "Aún no tienes contacto de emergencia. Configúralo para que la alerta llegue a alguien.",
                    accion = "Configurar",
                    onAccion = onConfiguracionClick
                )
            } else {
                ContactoCard(nombre = nombreContacto, numero = numeroContacto)
            }

            Spacer(modifier = Modifier.height(8.dp))

            SosButton(
                onActivar = viewModel::activarSos,
                habilitado = estado != EstadoAlerta.Enviando
            )

            EstadoCard(estado)

            if (seguimiento) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ubicación en tiempo real activa", style = MaterialTheme.typography.titleLarge)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Tu contacto recibe tu ubicación cada $intervalo min.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = viewModel::detenerSeguimiento, modifier = Modifier.fillMaxWidth()) {
                            Text("Estoy a salvo · detener seguimiento")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactoCard(nombre: String, numero: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Contacto de emergencia", style = MaterialTheme.typography.labelLarge)
                Text(nombre.ifBlank { numero }, style = MaterialTheme.typography.titleLarge)
                if (nombre.isNotBlank()) Text(numero, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun AvisoCard(texto: String, accion: String, onAccion: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(texto, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onAccion, modifier = Modifier.fillMaxWidth()) { Text(accion) }
        }
    }
}

@Composable
private fun EstadoCard(estado: EstadoAlerta) {
    val (color, titulo, detalles) = when (estado) {
        EstadoAlerta.Inactiva -> Triple(OkGreen, "Sistema listo", emptyList())
        EstadoAlerta.Enviando -> Triple(SosRed, "Enviando alerta…", listOf("Obteniendo tu ubicación"))
        is EstadoAlerta.Enviada -> {
            val hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(estado.hora))
            Triple(
                SosRed,
                "Alerta enviada a las $hora",
                listOfNotNull(
                    if (estado.ubicacionObtenida) "Ubicación incluida" else "Sin ubicación (GPS no disponible)",
                    if (estado.smsEnviado) "SMS enviado" else "SMS no enviado",
                    when (estado.whatsApp) {
                        ResultadoWhatsApp.ENVIO_AUTOMATICO -> "WhatsApp enviado"
                        ResultadoWhatsApp.ABIERTO_PARA_ENVIAR -> "WhatsApp abierto para enviar"
                        ResultadoWhatsApp.NO_INSTALADO -> "WhatsApp no instalado"
                        ResultadoWhatsApp.SIN_NUMERO -> "WhatsApp: falta el número"
                        ResultadoWhatsApp.ERROR -> "WhatsApp: error al abrir"
                        ResultadoWhatsApp.DESACTIVADO -> null
                    },
                    if (estado.llamadaIniciada) "Llamada de emergencia iniciada" else null
                )
            )
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(titulo, style = MaterialTheme.typography.titleLarge)
            }
            if (detalles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    detalles.joinToString(" · "),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}
