package com.example.silentsos.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encapsula la lógica para realizar la llamada de emergencia.
 *
 * Usa TelecomManager.placeCall, que no abre una Activity propia y por eso
 * funciona aunque la app ya no esté en primer plano (ej. después de abrir
 * WhatsApp). Sin permiso CALL_PHONE se abre el marcador con el número listo.
 *
 * Nota: Android no deja que apps que no son el marcador por defecto llamen
 * directamente a números de emergencia oficiales (123, 911...); en ese caso
 * el sistema muestra el marcador con el número para confirmar con un toque.
 */
@Singleton
class EmergencyCallManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun tienePermiso(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED

    /** Devuelve true si se inició la llamada o se abrió el marcador. */
    @SuppressLint("MissingPermission")
    fun llamar(numero: String): Boolean {
        if (numero.isBlank()) {
            Log.w(TAG, "No hay número de emergencia configurado")
            return false
        }
        val uri = Uri.fromParts("tel", numero, null)

        if (!tienePermiso()) return abrirMarcador(uri)

        return try {
            val telecom = context.getSystemService(TelecomManager::class.java)
            telecom.placeCall(uri, Bundle())
            true
        } catch (e: Exception) {
            Log.e(TAG, "placeCall falló, se intenta con ACTION_CALL", e)
            try {
                context.startActivity(
                    Intent(Intent.ACTION_CALL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                true
            } catch (e2: Exception) {
                Log.e(TAG, "No se pudo realizar la llamada", e2)
                abrirMarcador(uri)
            }
        }
    }

    private fun abrirMarcador(uri: Uri): Boolean = try {
        context.startActivity(Intent(Intent.ACTION_DIAL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: Exception) {
        Log.e(TAG, "No se pudo abrir el marcador", e)
        false
    }

    companion object {
        private const val TAG = "EmergencyCallManager"
    }
}
