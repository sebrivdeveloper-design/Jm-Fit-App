package com.forcetech.jmfitapp.ui.common

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.forcetech.jmfitapp.JmFitApplication
import com.forcetech.jmfitapp.R
import com.forcetech.jmfitapp.databinding.FragmentRolePlaceholderBinding

/**
 * Pantalla provisional de cada rol. Muestra quién inició sesión y permite cerrar sesión.
 * Cada responsable la reemplaza cambiando el `android:name` de su fragment en su archivo nav_*.xml.
 */
class RolePlaceholderFragment : Fragment(R.layout.fragment_role_placeholder) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentRolePlaceholderBinding.bind(view)

        val container = (requireActivity().application as JmFitApplication).container
        val sesion = container.sessionManager.sesionActual()

        // El título sale del `android:label` del destino en el archivo nav_*.xml
        binding.tvTituloPlaceholder.text = findNavController().currentDestination?.label.toString().orEmpty()

        if (sesion != null) {
            binding.tvSesionPlaceholder.text = getString(R.string.placeholder_sesion_de, sesion.nombreCompleto)
            binding.btnCerrarSesion.setText(R.string.placeholder_cerrar_sesion)
        } else {
            // Entró a la tienda sin iniciar sesión (cliente externo)
            binding.tvSesionPlaceholder.visibility = View.GONE
            binding.btnCerrarSesion.setText(R.string.placeholder_volver_login)
        }

        binding.btnCerrarSesion.setOnClickListener {
            container.authRepository.cerrarSesion()
            findNavController().navigate(R.id.action_global_login)
        }
    }
}
