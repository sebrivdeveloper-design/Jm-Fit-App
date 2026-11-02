package com.forcetech.jmfitapp.data.session

import android.content.Context
import androidx.core.content.edit
import com.forcetech.jmfitapp.data.security.LockoutPolicy

/**
 * Persiste los intentos fallidos por nombre de usuario (sin distinguir mayúsculas), para que el
 * bloqueo sobreviva si se cierra la app. Se guarda también para usuarios que no existen, así no se
 * revela qué usuarios son válidos.
 */
class LoginAttemptStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("jmfit_login_attempts", Context.MODE_PRIVATE)

    fun leer(nombreUsuario: String): LockoutPolicy.Estado {
        val clave = normalizar(nombreUsuario)
        return LockoutPolicy.Estado(
            fallos = prefs.getInt("f_$clave", 0),
            bloqueadoHasta = prefs.getLong("l_$clave", 0L)
        )
    }

    fun guardar(nombreUsuario: String, estado: LockoutPolicy.Estado) {
        val clave = normalizar(nombreUsuario)
        prefs.edit {
            putInt("f_$clave", estado.fallos)
            putLong("l_$clave", estado.bloqueadoHasta)
        }
    }

    fun reiniciar(nombreUsuario: String) {
        val clave = normalizar(nombreUsuario)
        prefs.edit {
            remove("f_$clave")
            remove("l_$clave")
        }
    }

    private fun normalizar(nombreUsuario: String) = nombreUsuario.trim().lowercase()
}
