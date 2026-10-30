package com.example.silentsos.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.silentsos.ui.screens.HomeScreen
import com.example.silentsos.ui.screens.LoginScreen
import com.example.silentsos.ui.screens.SettingsScreen
import com.example.silentsos.ui.screens.WelcomeScreen

object Rutas {
    const val BIENVENIDA = "bienvenida"
    const val LOGIN = "login"
    const val INICIO = "inicio"
    const val CONFIGURACION = "configuracion"
}

@Composable
fun SilentSOSNavGraph(
    navController: NavHostController = rememberNavController(),
    // La ruta de inicio ya llega resuelta desde MainViewModel.haVistoBienvenida:
    // si ya vio la bienvenida, se salta directo a LOGIN.
    rutaInicio: String = Rutas.BIENVENIDA
) {
    NavHost(navController = navController, startDestination = rutaInicio) {

        composable(Rutas.BIENVENIDA) {
            WelcomeScreen(
                onContinuarClick = {
                    navController.navigate(Rutas.LOGIN) {
                        popUpTo(Rutas.BIENVENIDA) { inclusive = true }
                    }
                }
            )
        }

        composable(Rutas.LOGIN) {
            LoginScreen(
                onIngresarClick = {
                    // Sin volver atrás al login con el botón "atrás"
                    navController.navigate(Rutas.INICIO) {
                        popUpTo(Rutas.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Rutas.INICIO) {
            HomeScreen(
                onConfiguracionClick = { navController.navigate(Rutas.CONFIGURACION) }
            )
        }

        composable(Rutas.CONFIGURACION) {
            SettingsScreen(onVolver = { navController.popBackStack() })
        }
    }
}
