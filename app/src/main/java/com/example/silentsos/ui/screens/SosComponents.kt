package com.example.silentsos.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.silentsos.ui.theme.SosRed
import com.example.silentsos.ui.theme.SosRedDark
import kotlinx.coroutines.delay

/** Segundos para cancelar antes de que se dispare la alerta manual. */
private const val CUENTA_REGRESIVA_S = 5

/**
 * Botón SOS circular. Al pulsarlo empieza una cuenta regresiva cancelable,
 * para evitar alertas por toques accidentales; al llegar a 0 llama a [onActivar].
 */
@Composable
fun SosButton(
    onActivar: () -> Unit,
    modifier: Modifier = Modifier,
    tamano: Dp = 200.dp,
    habilitado: Boolean = true
) {
    var restante by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(restante) {
        val r = restante ?: return@LaunchedEffect
        if (r == 0) {
            restante = null
            onActivar()
        } else {
            delay(1_000)
            restante = r - 1
        }
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = { if (restante == null) restante = CUENTA_REGRESIVA_S },
            enabled = habilitado,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (restante == null) SosRed else SosRedDark,
                contentColor = Color.White
            ),
            border = BorderStroke(6.dp, SosRed.copy(alpha = 0.3f)),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
            modifier = Modifier.size(tamano)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = restante?.toString() ?: "SOS",
                    fontSize = (tamano.value / 4).sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (restante == null) "Pulsa para pedir ayuda" else "Enviando alerta…",
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (restante != null) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = { restante = null }) {
                Text("Cancelar")
            }
        }
    }
}
