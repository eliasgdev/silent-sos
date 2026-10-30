package com.example.silentsos.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.silentsos.domain.model.OrigenAlerta
import com.example.silentsos.domain.model.RegistroAlerta
import com.example.silentsos.ui.viewmodel.HistorialViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Historial de alertas y pruebas enviadas (guardado solo en el teléfono). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    onVolver: () -> Unit,
    viewModel: HistorialViewModel = hiltViewModel()
) {
    val historial by viewModel.historial.collectAsState()
    val context = LocalContext.current
    var confirmarBorrado by remember { mutableStateOf(false) }

    if (confirmarBorrado) {
        AlertDialog(
            onDismissRequest = { confirmarBorrado = false },
            title = { Text("¿Borrar historial?") },
            text = { Text("Se eliminarán todos los registros de este teléfono.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.borrarHistorial()
                    confirmarBorrado = false
                }) { Text("Borrar") }
            },
            dismissButton = { TextButton(onClick = { confirmarBorrado = false }) { Text("Cancelar") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (historial.isNotEmpty()) {
                        IconButton(onClick = { confirmarBorrado = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Borrar historial")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (historial.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Aún no hay alertas registradas.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(historial, key = { it.hora }) { registro ->
                    RegistroCard(registro) { enlace ->
                        viewModel.antesDeSalirDeLaApp()
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(enlace)))
                    }
                }
            }
        }
    }
}

@Composable
private fun RegistroCard(registro: RegistroAlerta, onVerMapa: (String) -> Unit) {
    val fecha = SimpleDateFormat("EEE d MMM · HH:mm", Locale("es")).format(Date(registro.hora))
    val esPrueba = registro.origen == OrigenAlerta.PRUEBA

    Card(
        colors = if (esPrueba) CardDefaults.cardColors() else
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (esPrueba) "Prueba" else "Alerta · ${registro.origen.descripcion}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(fecha, style = MaterialTheme.typography.labelLarge)
            Text(
                listOfNotNull(
                    "SMS ${registro.smsEnviados}/${registro.contactos}",
                    "WhatsApp ${registro.whatsAppEnviados}",
                    if (registro.llamada) "Llamada realizada" else null
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyLarge
            )
            val enlace = registro.enlaceMapa
            if (enlace != null) {
                TextButton(onClick = { onVerMapa(enlace) }, contentPadding = PaddingValues(0.dp)) {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ver ubicación en el mapa")
                }
            } else {
                Text("Sin ubicación", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
