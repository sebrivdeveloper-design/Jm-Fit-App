package com.forcetech.jmfitapp.data.session

import android.content.Context
import androidx.core.content.edit

/** Datos mínimos de la persona que inició sesión. */
data class Sesion(
    val usuarioId: Long,
    val nombreUsuario: String,
    val nombreCompleto: String,
    val rolCodigo: String
)

/**
 * Guarda la sesión activa en SharedPreferences. Mientras exista, la app abre directo en el inicio
 * de su rol; se borra al cerrar sesión. Nunca guarda contraseñas.
 */
class SessionManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    fun iniciar(sesion: Sesion) {
        prefs.edit {
            putLong(K_ID, sesion.usuarioId)
            putString(K_USUARIO, sesion.nombreUsuario)
            putString(K_NOMBRE, sesion.nombreCompleto)
            putString(K_ROL, sesion.rolCodigo)
        }
    }

    fun sesionActual(): Sesion? {
        val id = prefs.getLong(K_ID, -1L)
        val rol = prefs.getString(K_ROL, null)
        if (id < 0 || rol == null) return null
        return Sesion(
            usuarioId = id,
            nombreUsuario = prefs.getString(K_USUARIO, "") ?: "",
            nombreCompleto = prefs.getString(K_NOMBRE, "") ?: "",
            rolCodigo = rol
        )
    }

    fun cerrarSesion() {
        prefs.edit { clear() }
    }

    private companion object {
        const val ARCHIVO = "jmfit_session"
        const val K_ID = "usuario_id"
        const val K_USUARIO = "nombre_usuario"
        const val K_NOMBRE = "nombre_completo"
        const val K_ROL = "rol_codigo"
    }
}
