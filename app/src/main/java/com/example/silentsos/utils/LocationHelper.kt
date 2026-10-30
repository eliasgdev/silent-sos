package com.example.silentsos.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Obtiene la ubicación del dispositivo con el proveedor "fused" de Google Play Services.
 */
@Singleton
class LocationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    fun tienePermiso(): Boolean =
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    /**
     * Devuelve la ubicación actual, o la última conocida si el GPS no responde
     * a tiempo. Null si no hay permiso o el dispositivo no tiene ninguna.
     * En una emergencia no se puede esperar indefinidamente, de ahí el [timeoutMs].
     */
    @SuppressLint("MissingPermission")
    suspend fun obtenerUbicacionActual(timeoutMs: Long = 8_000L): Location? {
        if (!tienePermiso()) return null

        val ultima = runCatching { client.lastLocation.await() }.getOrNull()
        if (ultima != null && System.currentTimeMillis() - ultima.time < UBICACION_FRESCA_MS) {
            return ultima
        }

        val cancelacion = CancellationTokenSource()
        val actual = withTimeoutOrNull(timeoutMs) {
            runCatching {
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancelacion.token).await()
            }.onFailure { Log.w(TAG, "No se pudo obtener la ubicación actual", it) }.getOrNull()
        }
        if (actual == null) cancelacion.cancel()
        return actual ?: ultima
    }

    companion object {
        private const val TAG = "LocationHelper"
        private const val UBICACION_FRESCA_MS = 60_000L
    }
}
