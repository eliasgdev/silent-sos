package com.example.silentsos

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.silentsos.navigation.Rutas
import com.example.silentsos.navigation.SilentSOSNavGraph
import com.example.silentsos.ui.screens.permisosRequeridos
import com.example.silentsos.ui.theme.SilentSOSTheme
import com.example.silentsos.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    /** Si la Activity se recreó por rotación, ese onStart no es una apertura nueva. */
    private var omitirAperturaPorRotacion = false

    /** Solicita permisos peligrosos en tiempo de ejecución (SMS, llamada, ubicación, notificaciones). */
    private val permisosLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultado ->
        if (resultado.values.any { !it }) {
            Toast.makeText(
                this,
                "Sin todos los permisos la alerta puede no llegar. Revísalos en Configuración.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        omitirAperturaPorRotacion = savedInstanceState?.getBoolean(KEY_CAMBIO_CONFIG) == true
        if (savedInstanceState == null) pedirPermisosSiFaltan()

        val rutaInicio = if (viewModel.haVistoBienvenida) Rutas.LOGIN else Rutas.BIENVENIDA

        setContent {
            SilentSOSTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SilentSOSNavGraph(rutaInicio = rutaInicio)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Si la opción de doble apertura está activa y esta es la segunda
        // apertura seguida, dispara la alerta silenciosa.
        if (omitirAperturaPorRotacion) {
            omitirAperturaPorRotacion = false
        } else {
            viewModel.onAppAbierta()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_CAMBIO_CONFIG, isChangingConfigurations)
    }

    private fun pedirPermisosSiFaltan() {
        val faltantes = permisosRequeridos().filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (faltantes.isNotEmpty()) {
            permisosLauncher.launch(faltantes.toTypedArray())
        }
    }

    companion object {
        private const val KEY_CAMBIO_CONFIG = "cambio_config"
    }
}
