package com.example.silentsos.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.silentsos.domain.model.ContactoEmergencia

/**
 * Agregar un contacto escribiéndolo o eligiéndolo de la agenda.
 * El selector del sistema da acceso solo al contacto elegido, así que
 * no hace falta el permiso READ_CONTACTS.
 */
@Composable
fun DialogoContacto(
    inicial: ContactoEmergencia,
    onAntesDeElegir: () -> Unit,
    onGuardar: (nombre: String, numero: String) -> String?,
    onCerrar: () -> Unit
) {
    val context = LocalContext.current
    var nombre by remember { mutableStateOf(inicial.nombre) }
    var numero by remember { mutableStateOf(inicial.numero) }
    var error by remember { mutableStateOf<String?>(null) }

    val selector = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
        val uri = resultado.data?.data ?: return@rememberLauncherForActivityResult
        leerContacto(context, uri)?.let { (n, tel) ->
            nombre = n
            numero = tel
            error = null
        }
    }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Nuevo contacto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        onAntesDeElegir()
                        selector.launch(
                            Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Person, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Elegir de mis contactos")
                }
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = numero,
                    onValueChange = { numero = it; error = null },
                    label = { Text("Número con indicativo") },
                    placeholder = { Text("+57 300 123 4567") },
                    supportingText = {
                        Text(
                            error ?: if (numero.isNotBlank() && !numero.trim().startsWith("+")) {
                                "Agrega el indicativo del país (ej. +57) para que funcione en WhatsApp"
                            } else {
                                "Recibe el SMS y el WhatsApp con tu ubicación"
                            }
                        )
                    },
                    isError = error != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                error = onGuardar(nombre, numero)
                if (error == null) onCerrar()
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}

private fun leerContacto(context: Context, uri: Uri): Pair<String, String>? = try {
    context.contentResolver.query(
        uri,
        arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        ),
        null, null, null
    )?.use { cursor ->
        if (cursor.moveToFirst()) (cursor.getString(0) ?: "") to (cursor.getString(1) ?: "") else null
    }
} catch (e: SecurityException) {
    null
}

@Composable
fun DialogoCambiarPin(
    onCambiar: (actual: String, nuevo: String) -> String?,
    onCerrar: () -> Unit,
    onListo: () -> Unit
) {
    var actual by remember { mutableStateOf("") }
    var nuevo by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val soloDigitos: (String) -> String = { it.filter(Char::isDigit).take(6) }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Cambiar PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    Triple("PIN actual", actual) { v: String -> actual = soloDigitos(v) },
                    Triple("PIN nuevo (4 a 6 dígitos)", nuevo) { v: String -> nuevo = soloDigitos(v) }
                ).forEach { (etiqueta, valor, alCambiar) ->
                    OutlinedTextField(
                        value = valor,
                        onValueChange = { alCambiar(it); error = null },
                        label = { Text(etiqueta) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                error = onCambiar(actual, nuevo)
                if (error == null) {
                    onListo()
                    onCerrar()
                }
            }) { Text("Cambiar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}
