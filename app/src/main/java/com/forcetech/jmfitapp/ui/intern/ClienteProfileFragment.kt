package com.forcetech.jmfitapp.ui.intern

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.forcetech.jmfitapp.JmFitApplication
import com.forcetech.jmfitapp.R
import com.forcetech.jmfitapp.databinding.FragmentClienteProfileBinding
import com.forcetech.jmfitapp.ui.common.TemaApp

/**
 * HU Cliente — CA03: perfil del cliente interno.
 * Acciones: la barra inferior y "Cerrar sesión" (vuelve al login para cambiar de usuario).
 * Las opciones del perfil son visuales (edición, historial, pedidos, notificaciones y configuración
 * están fuera del alcance).
 */
class ClienteProfileFragment : Fragment(R.layout.fragment_cliente_profile) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentClienteProfileBinding.bind(view)
        val datos = DatosClienteSimulados

        binding.tvNombreCliente.text = BarraCliente.nombreCliente(requireContext())
        binding.tvClienteDesde.text = getString(R.string.cli_cliente_desde, datos.CLIENTE_DESDE, datos.CIUDAD)

        binding.tvPrograma.text = datos.PROGRAMA
        binding.tvAsignadoPor.text = getString(
            R.string.cli_asignado_por, datos.ENTRENADOR, datos.SEMANA_PROGRAMA, datos.SEMANAS_PROGRAMA
        )
        binding.progresoPrograma.max = datos.SEMANAS_PROGRAMA
        binding.progresoPrograma.progress = datos.SEMANA_PROGRAMA
        binding.tvSesionesSemana.text = datos.SESIONES_SEMANA
        binding.tvFechaInicio.text = datos.FECHA_INICIO
        binding.tvProximoControl.text = datos.PROXIMO_CONTROL

        binding.tvEdad.text = datos.EDAD
        binding.tvEstatura.text = datos.ESTATURA
        binding.tvPesoActual.text = datos.PESO_ACTUAL
        binding.tvPesoObjetivo.text = datos.PESO_OBJETIVO
        listOf(binding.tvObjetivo1, binding.tvObjetivo2, binding.tvObjetivo3)
            .zip(datos.OBJETIVOS)
            .forEach { (vista, objetivo) -> vista.text = objetivo }

        binding.tvNotasDe.text = getString(R.string.cli_notas_de, datos.ENTRENADOR_NOMBRE_CORTO)
        binding.tvNotas.text = datos.NOTAS_ENTRENADOR

        binding.btnTema.setOnClickListener { TemaApp.mostrarSelector(requireContext()) }

        // Igual que RolePlaceholderFragment: borra la sesión y vuelve al login limpiando el historial
        binding.btnCerrarSesion.setOnClickListener {
            (requireActivity().application as JmFitApplication).container.authRepository.cerrarSesion()
            findNavController().navigate(R.id.action_global_login)
        }

        BarraCliente.configurar(binding.barraCliente, findNavController(), R.id.internProfileFragment)
    }
}
