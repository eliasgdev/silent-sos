package com.example.silentsos.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Servicio de accesibilidad que pulsa "Enviar" en WhatsApp, solo durante unos
 * segundos después de que SilentSOS abrió el chat con una alerta
 * ([programarEnvio]). Fuera de esa ventana ignora todos los eventos.
 *
 * Tras enviar vuelve a la pantalla de inicio para que la alerta sea discreta.
 */
class WhatsAppAutoSendService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!hayEnvioPendiente()) return
        val paquete = event?.packageName?.toString() ?: return
        if (paquete !in PAQUETES_WHATSAPP) return

        val raiz = rootInActiveWindow ?: return
        val boton = buscarBotonEnviar(raiz, paquete) ?: return

        if (boton.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            cancelarEnvio()
            handler.postDelayed({ performGlobalAction(GLOBAL_ACTION_HOME) }, 700)
        }
    }

    private fun buscarBotonEnviar(raiz: AccessibilityNodeInfo, paquete: String): AccessibilityNodeInfo? {
        raiz.findAccessibilityNodeInfosByViewId("$paquete:id/send")
            .firstOrNull { it.isVisibleToUser }
            ?.let { return nodoClicable(it) }

        // Respaldo por si WhatsApp cambia el id del botón
        for (texto in DESCRIPCIONES_ENVIAR) {
            raiz.findAccessibilityNodeInfosByText(texto)
                .firstOrNull { it.contentDescription?.toString().equals(texto, ignoreCase = true) }
                ?.let { return nodoClicable(it) }
        }
        return null
    }

    private fun nodoClicable(nodo: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var actual: AccessibilityNodeInfo? = nodo
        while (actual != null && !actual.isClickable) actual = actual.parent
        return actual
    }

    override fun onInterrupt() = Unit

    companion object {
        private val PAQUETES_WHATSAPP = setOf("com.whatsapp", "com.whatsapp.w4b")
        private val DESCRIPCIONES_ENVIAR = listOf("Enviar", "Send")
        private const val VENTANA_ENVIO_MS = 20_000L

        @Volatile
        private var envioPendienteHasta = 0L

        fun programarEnvio() {
            envioPendienteHasta = SystemClock.elapsedRealtime() + VENTANA_ENVIO_MS
        }

        fun cancelarEnvio() {
            envioPendienteHasta = 0L
        }

        private fun hayEnvioPendiente(): Boolean = SystemClock.elapsedRealtime() < envioPendienteHasta

        fun estaHabilitado(context: Context): Boolean {
            val habilitados = Settings.Secure.getString(
                context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val componente = ComponentName(context, WhatsAppAutoSendService::class.java)
            return habilitados.split(':').any {
                ComponentName.unflattenFromString(it) == componente
            }
        }
    }
}
