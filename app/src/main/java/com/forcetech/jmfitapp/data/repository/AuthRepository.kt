package com.forcetech.jmfitapp.data.repository

import android.util.Log
import com.forcetech.jmfitapp.data.dao.RolDao
import com.forcetech.jmfitapp.data.dao.UsuarioDao
import com.forcetech.jmfitapp.data.security.LockoutPolicy
import com.forcetech.jmfitapp.data.security.PasswordHasher
import com.forcetech.jmfitapp.data.session.LoginAttemptStore
import com.forcetech.jmfitapp.data.session.SessionManager
import com.forcetech.jmfitapp.data.session.Sesion
import com.forcetech.jmfitapp.model.EstadoUsuario
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Resultado de intentar iniciar sesión. La pantalla lo traduce a mensajes. */
sealed interface LoginResult {
    data class Exito(val rolCodigo: String) : LoginResult
    data object CredencialesInvalidas : LoginResult
    data object CuentaInhabilitada : LoginResult
    data class Bloqueado(val minutosRestantes: Int) : LoginResult
    data object ErrorInesperado : LoginResult
}

class AuthRepository(
    private val usuarioDao: UsuarioDao,
    private val rolDao: RolDao,
    private val sessionManager: SessionManager,
    private val intentos: LoginAttemptStore,
    /** Espera a que los datos de ejemplo estén cargados (solo importa en el primer arranque). */
    private val esperarBaseLista: suspend () -> Unit,
    private val politica: LockoutPolicy = LockoutPolicy(),
    private val reloj: () -> Long = System::currentTimeMillis
) {

    /** Hash de mentira: se compara cuando el usuario no existe para que tarde lo mismo que un usuario real. */
    private val hashFicticio: String by lazy { PasswordHasher.hash("jmfit-hash-ficticio") }

    suspend fun iniciarSesion(nombreUsuario: String, password: String): LoginResult =
        withContext(Dispatchers.Default) {
            try {
                esperarBaseLista()
                val usuarioTexto = nombreUsuario.trim()

                // 1. ¿Está bloqueado por intentos fallidos?
                val estadoActual = intentos.leer(usuarioTexto)
                val ahora = reloj()
                if (politica.estaBloqueado(estadoActual, ahora)) {
                    return@withContext LoginResult.Bloqueado(politica.minutosRestantes(estadoActual, ahora))
                }

                // 2. Buscar usuario y verificar contraseña
                val usuario = usuarioDao.buscarPorNombreUsuario(usuarioTexto)
                val claveCorrecta = if (usuario != null) {
                    PasswordHasher.verify(password, usuario.passwordHash)
                } else {
                    PasswordHasher.verify(password, hashFicticio)
                    false
                }

                if (usuario == null || !claveCorrecta) {
                    val nuevo = politica.alFallar(estadoActual, ahora)
                    intentos.guardar(usuarioTexto, nuevo)
                    return@withContext if (politica.estaBloqueado(nuevo, ahora)) {
                        LoginResult.Bloqueado(politica.minutosRestantes(nuevo, ahora))
                    } else {
                        LoginResult.CredencialesInvalidas
                    }
                }

                // 3. Credenciales correctas, pero la cuenta puede estar inhabilitada
                if (usuario.estado != EstadoUsuario.ACTIVO) {
                    return@withContext LoginResult.CuentaInhabilitada
                }

                // 4. Ingreso exitoso
                val rolCodigo = rolDao.codigoDe(usuario.rolId)
                    ?: return@withContext LoginResult.ErrorInesperado
                intentos.reiniciar(usuarioTexto)
                usuarioDao.actualizarUltimoAcceso(usuario.id, ahora)
                sessionManager.iniciar(
                    Sesion(
                        usuarioId = usuario.id,
                        nombreUsuario = usuario.nombreUsuario,
                        nombreCompleto = usuario.nombreCompleto,
                        rolCodigo = rolCodigo
                    )
                )
                LoginResult.Exito(rolCodigo)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("AuthRepository", "Error al iniciar sesión", e)
                LoginResult.ErrorInesperado
            }
        }

    fun cerrarSesion() = sessionManager.cerrarSesion()
}
