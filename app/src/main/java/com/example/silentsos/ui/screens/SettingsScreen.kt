package com.example.silentsos.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.silentsos.ui.theme.OkGreen
import com.example.silentsos.ui.theme.SosRed
import com.example.silentsos.ui.viewmodel.SettingsViewModel

/** Permisos peligrosos que la app pide en tiempo de ejecución. */
fun permisosRequeridos(): Array<String> = buildList {
    add(Manifest.permission.SEND_SMS)
    add(Manifest.permission.CALL_PHONE)
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    add(Manifest.permission.ACCESS_COARSE_LOCATION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
}.toTypedArray()

private fun concedido(context: Context, permiso: String) =
    ContextCompat.checkSelfPermission(context, permiso) == PackageManager.PERMISSION_GRANTED

/**
 * Configuración completa:
 * contacto, canales (SMS / WhatsApp), ubicación en tiempo real, llamada,
 * activadores, perfil, permisos y mensaje de prueba.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onVolver: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val llamadaAlAbrir by viewModel.llamadaAlAbrir.collectAsState()
    val dobleApertura by viewModel.dobleAperturaAutoLlamada.collectAsState()
    val smsActivo by viewModel.smsActivo.collectAsState()
    val whatsAppActivo by viewModel.whatsAppActivo.collectAsState()
    val seguimientoActivo by viewModel.seguimientoActivo.collectAsState()
    val intervalo by viewModel.intervaloSeguimientoMin.collectAsState()
    val llamar by viewModel.llamarEmergencia.collectAsState()
    val enviandoPrueba by viewModel.enviandoPrueba.collectAsState()

    // Estado local de los campos de texto para que escribir no tenga saltos de cursor
    var nombreContacto by remember { mutableStateOf(viewModel.nombreContacto.value) }
    var numeroContacto by remember { mutableStateOf(viewModel.numeroSms.value) }
    var numeroEmergencia by remember { mutableStateOf(viewModel.numeroEmergencia.value) }
    var nombreUsuario by remember { mutableStateOf(viewModel.nombreUsuario.value) }

    // Estado que cambia fuera de la app (permisos, accesibilidad): se relee al volver
    var refresco by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) refresco++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val permisos = remember(refresco) { permisosRequeridos().associateWith { concedido(context, it) } }
    val whatsAppInstalado = remember(refresco) { viewModel.whatsAppInstalado() }
    val autoEnvio = remember(refresco) { viewModel.autoEnvioWhatsAppDisponible() }

    val permisosLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refresco++ }

    fun abrirPantallaSistema(intent: Intent) {
        viewModel.antesDeSalirDeLaApp()
        context.startActivity(intent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Seccion("Contacto de emergencia") {
                OutlinedTextField(
                    value = nombreContacto,
                    onValueChange = {
                        nombreContacto = it
                        viewModel.onNombreContactoChange(it)
                    },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = numeroContacto,
                    onValueChange = {
                        numeroContacto = it
                        viewModel.onNumeroSmsChange(it)
                    },
                    label = { Text("Número con indicativo") },
                    placeholder = { Text("+57 300 123 4567") },
                    supportingText = {
                        if (numeroContacto.isNotBlank() && !numeroContacto.trim().startsWith("+")) {
                            Text("Incluye el indicativo del país (ej. +57) para que funcione en WhatsApp")
                        } else {
                            Text("Recibe el SMS y el WhatsApp con tu ubicación")
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Seccion("Canales de aviso") {
                OpcionSwitch(
                    titulo = "SMS",
                    descripcion = "Se envía en segundo plano, sin abrir ninguna app. Funciona sin internet.",
                    checked = smsActivo,
                    onCheckedChange = viewModel::onSmsActivoChange
                )
                HorizontalDivider()
                OpcionSwitch(
                    titulo = "WhatsApp",
                    descripcion = if (whatsAppInstalado) {
                        "Mensaje con enlace a tu ubicación en Google Maps."
                    } else {
                        "WhatsApp no está instalado en este teléfono."
                    },
                    checked = whatsAppActivo,
                    onCheckedChange = viewModel::onWhatsAppActivoChange
                )
                if (whatsAppActivo) {
                    EstadoLinea(
                        ok = autoEnvio,
                        texto = if (autoEnvio) {
                            "Envío automático activo: no tendrás que tocar \"Enviar\"."
                        } else {
                            "Envío automático desactivado: WhatsApp se abrirá con el mensaje listo y tendrás que tocar \"Enviar\"."
                        }
                    )
                    if (!autoEnvio) {
                        Text(
                            "Para activarlo: Accesibilidad → Apps instaladas → SilentSOS → activar. " +
                                "Si aparece \"Ajuste restringido\", ve a Ajustes → Apps → SilentSOS → ⋮ → " +
                                "Permitir ajustes restringidos.",
                            style = MaterialTheme.typography.labelLarge
                        )
                        OutlinedButton(
                            onClick = { abrirPantallaSistema(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Activar envío automático") }
                    }
                }
            }

            Seccion("Ubicación en tiempo real") {
                OpcionSwitch(
                    titulo = "Seguimiento continuo",
                    descripcion = "Después de la alerta, sigue enviando tu ubicación hasta que pulses \"Estoy a salvo\". " +
                        "Por WhatsApp solo si el envío automático está activo.",
                    checked = seguimientoActivo,
                    onCheckedChange = viewModel::onSeguimientoActivoChange
                )
                if (seguimientoActivo) {
                    Text("Enviar cada", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 2, 5, 10).forEach { minutos ->
                            FilterChip(
                                selected = intervalo == minutos,
                                onClick = { viewModel.onIntervaloChange(minutos) },
                                label = { Text("$minutos min") }
                            )
                        }
                    }
                }
            }

            Seccion("Llamada") {
                OpcionSwitch(
                    titulo = "Llamar a emergencias",
                    descripcion = "Después de enviar los mensajes, llama a este número.",
                    checked = llamar,
                    onCheckedChange = viewModel::onLlamarEmergenciaChange
                )
                if (llamar) {
                    OutlinedTextField(
                        value = numeroEmergencia,
                        onValueChange = {
                            numeroEmergencia = it
                            viewModel.onNumeroEmergenciaChange(it)
                        },
                        label = { Text("Número a llamar") },
                        supportingText = { Text("123 es la línea de emergencias en Colombia. Puede ser también un familiar.") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Seccion("Cómo activar la alerta") {
                OpcionSwitch(
                    titulo = "SOS en la pantalla de ingreso",
                    descripcion = "Muestra el botón SOS al abrir la app, sin tener que escribir el PIN.",
                    checked = llamadaAlAbrir,
                    onCheckedChange = viewModel::onLlamadaAlAbrirChange
                )
                HorizontalDivider()
                OpcionSwitch(
                    titulo = "Alerta silenciosa por doble apertura",
                    descripcion = "Si abres la app 2 veces en menos de 10 segundos, la alerta se envía sola, sin cuenta regresiva.",
                    checked = dobleApertura,
                    onCheckedChange = viewModel::onDobleAperturaAutoLlamadaChange
                )
            }

            Seccion("Tu perfil") {
                OutlinedTextField(
                    value = nombreUsuario,
                    onValueChange = {
                        nombreUsuario = it
                        viewModel.onNombreUsuarioChange(it)
                    },
                    label = { Text("Tu nombre (aparece en los mensajes)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Seccion("Permisos") {
                val nombres = mapOf(
                    Manifest.permission.SEND_SMS to "Enviar SMS",
                    Manifest.permission.CALL_PHONE to "Realizar llamadas",
                    Manifest.permission.ACCESS_FINE_LOCATION to "Ubicación precisa",
                    Manifest.permission.ACCESS_COARSE_LOCATION to "Ubicación aproximada",
                    "android.permission.POST_NOTIFICATIONS" to "Notificaciones"
                )
                permisos.forEach { (permiso, ok) ->
                    EstadoLinea(ok = ok, texto = nombres[permiso] ?: permiso)
                }
                if (permisos.values.any { !it }) {
                    Button(
                        onClick = { permisosLauncher.launch(permisosRequeridos()) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Conceder permisos") }
                    TextButton(
                        onClick = {
                            abrirPantallaSistema(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Abrir ajustes de la app") }
                }
            }

            Button(
                onClick = {
                    viewModel.enviarPrueba { resultado ->
                        Toast.makeText(context, resultado, Toast.LENGTH_LONG).show()
                    }
                },
                enabled = !enviandoPrueba,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (enviandoPrueba) "Enviando prueba…" else "Enviar mensaje de prueba")
            }
            Text(
                "La prueba envía el SMS y el WhatsApp con tu ubicación, sin llamar ni iniciar el seguimiento.",
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Seccion(titulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            contenido()
        }
    }
}

@Composable
private fun OpcionSwitch(
    titulo: String,
    descripcion: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = titulo, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(text = descripcion, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun EstadoLinea(ok: Boolean, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (ok) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = if (ok) "Activo" else "Pendiente",
            tint = if (ok) OkGreen else SosRed,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(texto, style = MaterialTheme.typography.bodyLarge)
    }
}
