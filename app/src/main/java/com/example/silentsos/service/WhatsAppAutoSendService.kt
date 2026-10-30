package com.example.silentsos.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.delay

/**
 * Servicio de accesibilidad que completa en WhatsApp lo que SilentSOS dejó
 * preparado, solo durante unos segundos después de abrir el chat
 * ([programarEnvio]). Fuera de esa ventana ignora todos los eventos.
 *
 * Pasos:
 * 1. Pulsa "Enviar" para mandar el mensaje de texto.
 * 2. Si se pidió ubicación en tiempo real: Adjuntar → Ubicación →
 *    Ubicación en tiempo real → duración → Enviar (la ubicación oficial de
 *    WhatsApp, que el contacto ve moverse en el mapa del chat).
 *    Si no se puede compartir en tiempo real, envía la ubicación actual.
 * 3. Vuelve a la pantalla de inicio para que la alerta sea discreta.
 */
class WhatsAppAutoSendService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())

    /** WhatsApp no siempre emite eventos tras cada cambio: se reintenta cada poco. */
    private val reintento = object : Runnable {
        override fun run() {
            if (!hayEnvioPendiente()) return
            avanzar()
            handler.postDelayed(this, INTERVALO_REINTENTO_MS)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!hayEnvioPendiente()) return
        val paquete = event?.packageName?.toString() ?: return
        if (paquete !in PAQUETES_WHATSAPP) return
        avanzar()
        handler.removeCallbacks(reintento)
        handler.postDelayed(reintento, INTERVALO_REINTENTO_MS)
    }

    private fun avanzar() {
        val raiz = rootInActiveWindow ?: return
        val paquete = raiz.packageName?.toString() ?: return
        if (paquete !in PAQUETES_WHATSAPP) return
        // Evita pulsar dos veces mientras WhatsApp todavía no cambió de pantalla
        if (SystemClock.elapsedRealtime() - ultimoClic < PAUSA_ENTRE_CLICS_MS) return

        fun id(nombre: String) = buscarPorId(raiz, "$paquete:id/$nombre")

        when (paso) {
            Paso.TEXTO -> {
                // En el chat: "send" junto al campo de texto (no el de la hoja de ubicación)
                if (id("live_location_sheet") != null) return
                val enviar = id("send") ?: buscarPorDescripcion(raiz, DESCRIPCIONES_ENVIAR) ?: return
                if (clic(enviar)) {
                    paso = if (duracionUbicacion != null) Paso.ADJUNTAR else Paso.FIN
                    if (paso == Paso.FIN) terminar()
                }
            }
            Paso.ADJUNTAR -> {
                id("pickfiletype_location_holder")?.let { if (clic(it)) paso = Paso.TIEMPO_REAL; return }
                id("input_attach_button")?.let { clic(it) }
            }
            Paso.TIEMPO_REAL -> {
                // Aviso que WhatsApp muestra la primera vez que se comparte en tiempo real
                buscarPorTexto(raiz, TEXTOS_CONTINUAR)?.let { clic(it); return }
                id("live_location_sheet")?.let { paso = Paso.CONFIRMAR; avanzar(); return }
                id("live_location_btn")?.let { clic(it); return }
                // Sin opción de tiempo real: al menos la ubicación actual
                id("send_current_location_btn")?.let { if (clic(it)) terminar() }
            }
            Paso.CONFIRMAR -> {
                if (id("live_location_sheet") == null) {
                    buscarPorTexto(raiz, TEXTOS_CONTINUAR)?.let { clic(it) }
                    return
                }
                val duracion = id(duracionUbicacion?.idWhatsApp ?: return)
                if (duracion != null && !duracion.isSelected && !duracion.isChecked) {
                    clic(duracion) // se envía en el siguiente intento, ya con la duración marcada
                    return
                }
                id("send")?.let { if (clic(it)) terminar() }
            }
            Paso.FIN -> Unit
        }
    }

    private fun terminar() {
        paso = Paso.FIN
        cancelarEnvio()
        handler.removeCallbacks(reintento)
        handler.postDelayed({ performGlobalAction(GLOBAL_ACTION_HOME) }, 800)
    }

    private fun clic(nodo: AccessibilityNodeInfo): Boolean {
        val clicable = nodoClicable(nodo) ?: return false
        val ok = clicable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        if (ok) ultimoClic = SystemClock.elapsedRealtime()
        Log.d(TAG, "clic ${nodo.viewIdResourceName ?: nodo.text} en paso $paso: $ok")
        return ok
    }

    private fun buscarPorId(raiz: AccessibilityNodeInfo, id: String): AccessibilityNodeInfo? =
        raiz.findAccessibilityNodeInfosByViewId(id).firstOrNull { it.isVisibleToUser }

    private fun buscarPorDescripcion(raiz: AccessibilityNodeInfo, textos: List<String>): AccessibilityNodeInfo? {
        for (texto in textos) {
            raiz.findAccessibilityNodeInfosByText(texto)
                .firstOrNull { it.contentDescription?.toString().equals(texto, ignoreCase = true) }
                ?.let { return it }
        }
        return null
    }

    private fun buscarPorTexto(raiz: AccessibilityNodeInfo, textos: List<String>): AccessibilityNodeInfo? {
        for (texto in textos) {
            raiz.findAccessibilityNodeInfosByText(texto)
                .firstOrNull { it.isVisibleToUser && it.text?.toString().equals(texto, ignoreCase = true) }
                ?.let { return it }
        }
        return null
    }

    private fun nodoClicable(nodo: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var actual: AccessibilityNodeInfo? = nodo
        while (actual != null && !actual.isClickable) actual = actual.parent
        return actual
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private enum class Paso { TEXTO, ADJUNTAR, TIEMPO_REAL, CONFIRMAR, FIN }

    /** Duraciones que ofrece WhatsApp para la ubicación en tiempo real. */
    enum class DuracionUbicacion(val idWhatsApp: String) {
        MIN_15("duration_15_min"),
        HORA_1("duration_60_min"),
        HORAS_8("duration_480_min")
    }

    companion object {
        private const val TAG = "WhatsAppAutoSend"
        private val PAQUETES_WHATSAPP = setOf("com.whatsapp", "com.whatsapp.w4b")
        private val DESCRIPCIONES_ENVIAR = listOf("Enviar", "Send")
        private val TEXTOS_CONTINUAR = listOf("Continuar", "Continue", "Aceptar", "OK")
        private const val VENTANA_SOLO_TEXTO_MS = 20_000L
        private const val VENTANA_CON_UBICACION_MS = 35_000L
        private const val INTERVALO_REINTENTO_MS = 600L
        private const val PAUSA_ENTRE_CLICS_MS = 900L

        @Volatile
        private var envioPendienteHasta = 0L

        @Volatile
        private var paso = Paso.FIN

        @Volatile
        private var duracionUbicacion: DuracionUbicacion? = null

        @Volatile
        private var ultimoClic = 0L

        /**
         * Llamar justo antes de abrir el chat. Con [ubicacionEnVivo] además del
         * texto se comparte la ubicación en tiempo real de WhatsApp.
         */
        fun programarEnvio(ubicacionEnVivo: DuracionUbicacion? = null) {
            duracionUbicacion = ubicacionEnVivo
            paso = Paso.TEXTO
            ultimoClic = 0L
            envioPendienteHasta = SystemClock.elapsedRealtime() +
                if (ubicacionEnVivo != null) VENTANA_CON_UBICACION_MS else VENTANA_SOLO_TEXTO_MS
        }

        fun cancelarEnvio() {
            envioPendienteHasta = 0L
        }

        private fun hayEnvioPendiente(): Boolean = SystemClock.elapsedRealtime() < envioPendienteHasta

        /** Espera a que termine (o venza) el envío en curso, para abrir el siguiente chat. */
        suspend fun esperarFin() {
            while (hayEnvioPendiente()) delay(250)
        }

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
