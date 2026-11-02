package com.forcetech.jmfitapp.ui.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.forcetech.jmfitapp.R
import com.forcetech.jmfitapp.data.repository.AuthRepository
import com.forcetech.jmfitapp.data.repository.LoginResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** Mensaje del recuadro rojo. [argumento] se usa en textos con un número, como los minutos de bloqueo. */
data class BannerError(@StringRes val mensaje: Int, val argumento: Int? = null)

data class LoginUiState(
    val cargando: Boolean = false,
    @StringRes val usuarioError: Int? = null,
    @StringRes val passwordError: Int? = null,
    val banner: BannerError? = null
) {
    val hayErrores: Boolean get() = usuarioError != null || passwordError != null || banner != null
}

class LoginViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /** Evento de una sola vez: id del subgrafo al que hay que navegar. */
    private val _destino = Channel<Int>(Channel.BUFFERED)
    val destino: Flow<Int> = _destino.receiveAsFlow()

    fun iniciarSesion(usuario: String, password: String) {
        if (_uiState.value.cargando) return

        val usuarioLimpio = usuario.trim()
        val usuarioError = if (usuarioLimpio.isEmpty()) R.string.error_campo_obligatorio else null
        val passwordError = if (password.isEmpty()) R.string.error_campo_obligatorio else null
        if (usuarioError != null || passwordError != null) {
            _uiState.value = LoginUiState(usuarioError = usuarioError, passwordError = passwordError)
            return
        }

        _uiState.value = LoginUiState(cargando = true)
        viewModelScope.launch {
            when (val resultado = auth.iniciarSesion(usuarioLimpio, password)) {
                is LoginResult.Exito -> {
                    val destino = RolRouter.graphIdFor(resultado.rolCodigo)
                    if (destino == null) {
                        auth.cerrarSesion()
                        mostrarBanner(BannerError(R.string.login_error_inesperado))
                    } else {
                        _uiState.value = LoginUiState()
                        _destino.send(destino)
                    }
                }
                LoginResult.CredencialesInvalidas ->
                    mostrarBanner(BannerError(R.string.login_error_credenciales))
                LoginResult.CuentaInhabilitada ->
                    mostrarBanner(BannerError(R.string.login_error_inhabilitado))
                is LoginResult.Bloqueado ->
                    mostrarBanner(BannerError(R.string.login_error_bloqueado, resultado.minutosRestantes))
                LoginResult.ErrorInesperado ->
                    mostrarBanner(BannerError(R.string.login_error_inesperado))
            }
        }
    }

    /** Se llama cuando la persona vuelve a escribir: quita los errores visibles. */
    fun limpiarErrores() {
        if (_uiState.value.hayErrores) {
            _uiState.value = _uiState.value.copy(usuarioError = null, passwordError = null, banner = null)
        }
    }

    private fun mostrarBanner(banner: BannerError) {
        _uiState.value = LoginUiState(banner = banner)
    }

    class Factory(private val auth: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = LoginViewModel(auth) as T
    }
}
