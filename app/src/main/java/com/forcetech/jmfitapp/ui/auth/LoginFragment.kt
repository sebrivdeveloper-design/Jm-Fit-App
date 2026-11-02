package com.forcetech.jmfitapp.ui.auth

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.annotation.IdRes
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.forcetech.jmfitapp.JmFitApplication
import com.forcetech.jmfitapp.R
import com.forcetech.jmfitapp.databinding.FragmentLoginBinding
import kotlinx.coroutines.launch

/**
 * Pantalla de inicio de sesión única para todos los roles. Según el rol de la cuenta, redirige al
 * subgrafo correspondiente. El cliente externo no inicia sesión: entra al catálogo con el enlace inferior.
 */
class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val appContainer
        get() = (requireActivity().application as JmFitApplication).container

    private val viewModel: LoginViewModel by viewModels {
        LoginViewModel.Factory(appContainer.authRepository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)

        // Si ya hay una sesión activa, se entra directo al inicio del rol.
        val sesion = appContainer.sessionManager.sesionActual()
        val destinoSesion = RolRouter.graphIdFor(sesion?.rolCodigo)
        if (destinoSesion != null) {
            irA(destinoSesion)
            return
        }

        binding.btnIniciarSesion.setOnClickListener { enviar() }
        binding.btnVerCatalogo.setOnClickListener {
            findNavController().navigate(R.id.nav_store)
        }
        binding.etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                enviar()
                true
            } else {
                false
            }
        }
        binding.etUsuario.doOnTextChanged { _, _, _, _ -> viewModel.limpiarErrores() }
        binding.etPassword.doOnTextChanged { _, _, _, _ -> viewModel.limpiarErrores() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { render(it) } }
                launch { viewModel.destino.collect { irA(it) } }
            }
        }
    }

    private fun enviar() {
        viewModel.iniciarSesion(
            usuario = binding.etUsuario.text?.toString().orEmpty(),
            password = binding.etPassword.text?.toString().orEmpty()
        )
    }

    private fun render(estado: LoginUiState) {
        binding.tilUsuario.error = estado.usuarioError?.let { getString(it) }
        binding.tilPassword.error = estado.passwordError?.let { getString(it) }

        val banner = estado.banner
        if (banner == null) {
            binding.tvBannerError.visibility = View.GONE
        } else {
            binding.tvBannerError.text = if (banner.argumento != null) {
                getString(banner.mensaje, banner.argumento)
            } else {
                getString(banner.mensaje)
            }
            binding.tvBannerError.visibility = View.VISIBLE
        }

        binding.btnIniciarSesion.isEnabled = !estado.cargando
        binding.btnIniciarSesion.setText(
            if (estado.cargando) R.string.login_boton_cargando else R.string.login_boton
        )
        binding.etUsuario.isEnabled = !estado.cargando
        binding.etPassword.isEnabled = !estado.cargando
        binding.btnVerCatalogo.isEnabled = !estado.cargando
    }

    /** Entra al subgrafo del rol y saca el login del historial (el botón atrás no vuelve aquí). */
    private fun irA(@IdRes graphId: Int) {
        val opciones = NavOptions.Builder()
            .setPopUpTo(R.id.loginFragment, true)
            .build()
        findNavController().navigate(graphId, null, opciones)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
