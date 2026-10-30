package com.example.silentsos.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.silentsos.MainActivity
import com.example.silentsos.R
import com.example.silentsos.data.repository.EmergencyRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Servicio en primer plano que envía la ubicación al contacto de emergencia
 * cada N minutos hasta que se detiene (desde la app o la notificación).
 */
@AndroidEntryPoint
class TrackingService : Service() {

    @Inject
    lateinit var repository: EmergencyRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var bucle: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACCION_DETENER) {
            detener()
            return START_NOT_STICKY
        }

        crearCanal()
        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICACION_ID,
                notificacion("Enviando tu ubicación a tu contacto de emergencia"),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
            )
        } catch (e: Exception) {
            // Ej. sin permiso de ubicación en Android 14
            Log.e(TAG, "No se pudo iniciar el seguimiento", e)
            stopSelf()
            return START_NOT_STICKY
        }

        if (bucle == null) {
            repository.onSeguimientoIniciado()
            bucle = scope.launch {
                var envios = 0
                while (isActive) {
                    delay(repository.intervaloSeguimientoMin.value * 60_000L)
                    envios++
                    repository.enviarActualizacionDeUbicacion(envios)
                    val hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    actualizarNotificacion("Última ubicación enviada a las $hora ($envios envíos)")
                }
            }
        }
        return START_STICKY
    }

    private fun detener() {
        val estabaActivo = bucle != null
        bucle?.cancel()
        bucle = null
        scope.launch {
            if (estabaActivo) repository.enviarFinDeSeguimiento()
            ServiceCompat.stopForeground(this@TrackingService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        repository.onSeguimientoDetenido()
        super.onDestroy()
    }

    private fun crearCanal() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val canal = NotificationChannel(
            CANAL_ID, getString(R.string.canal_seguimiento), NotificationManager.IMPORTANCE_LOW
        ).apply { setShowBadge(false) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    private fun notificacion(texto: String) =
        NotificationCompat.Builder(this, CANAL_ID)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle("Seguimiento de emergencia activo")
            .setContentText(texto)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
                )
            )
            .addAction(
                0, "Detener",
                PendingIntent.getService(
                    this, 1, intentDetener(this),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .build()

    private fun actualizarNotificacion(texto: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICACION_ID, notificacion(texto))
    }

    companion object {
        private const val TAG = "TrackingService"
        private const val CANAL_ID = "seguimiento_emergencia"
        private const val NOTIFICACION_ID = 1001
        private const val ACCION_DETENER = "com.example.silentsos.DETENER_SEGUIMIENTO"

        fun iniciar(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, TrackingService::class.java))
        }

        fun detener(context: Context) {
            context.startService(intentDetener(context))
        }

        private fun intentDetener(context: Context) =
            Intent(context, TrackingService::class.java).setAction(ACCION_DETENER)
    }
}
