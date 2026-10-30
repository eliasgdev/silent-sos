package com.example.silentsos.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.silentsos.R
import com.example.silentsos.ui.viewmodel.WelcomeViewModel

/**
 * Pantalla mostrada únicamente la primera vez que se abre la app.
 * Al continuar, marca en el repositorio (vía WelcomeViewModel) que
 * la bienvenida ya fue vista, para no volver a mostrarla.
 */
@Composable
fun WelcomeScreen(
    onContinuarClick: () -> Unit,
    viewModel: WelcomeViewModel = hiltViewModel()
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher),
            contentDescription = null,
            modifier = Modifier.size(120.dp)
        )

        Text(
            text = "Bienvenida a SilentSOS",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Pide ayuda sin hacer ruido. Con un toque —o abriendo la app dos veces seguidas— " +
                "SilentSOS avisa a tu contacto de confianza por SMS y WhatsApp con tu ubicación, " +
                "y la sigue enviando en tiempo real mientras lo necesites.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Tus datos se guardan solo en este teléfono.",
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                viewModel.onBienvenidaVista()
                onContinuarClick()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Comenzar")
        }
    }
}
