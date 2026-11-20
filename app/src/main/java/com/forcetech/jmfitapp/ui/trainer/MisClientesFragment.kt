package com.forcetech.jmfitapp.ui.trainer

import android.os.Bundle
import android.view.View
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.forcetech.jmfitapp.JmFitApplication
import com.forcetech.jmfitapp.R
import com.forcetech.jmfitapp.databinding.FragmentMisClientesBinding
import kotlinx.coroutines.launch

/**
 * HU-51: "Mis clientes" — solo los clientes con asignación vigente al entrenador con sesión,
 * en orden alfabético, con búsqueda por nombre, su rutina actual y el estado de la cuenta.
 * HU-57: total de clientes ACTIVOS asignados (se recalcula cada vez que se abre la pantalla).
 */
class MisClientesFragment : Fragment(R.layout.fragment_mis_clientes) {

    private var _binding: FragmentMisClientesBinding? = null
    private val binding get() = _binding!!

    private val appContainer
        get() = (requireActivity().application as JmFitApplication).container

    private val viewModel: MisClientesViewModel by viewModels {
        MisClientesViewModel.Factory(
            appContainer.clienteRepository,
            appContainer.sessionManager.sesionActual()?.usuarioId
        )
    }

    private val adapter = MisClientesAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMisClientesBinding.bind(view)
        val nav = findNavController()

        binding.rvClientes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvClientes.adapter = adapter
        binding.etBuscar.doOnTextChanged { texto, _, _, _ -> viewModel.buscar(texto?.toString().orEmpty()) }
        binding.btnReintentar.setOnClickListener { viewModel.cargar() }

        BarraEntrenador.configurar(binding.barraEntrenador, nav, R.id.trainerClientesFragment)

        viewModel.cargar()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { render(it) }
            }
        }
    }

    private fun render(estado: MisClientesUiState) {
        val b = binding
        b.tvTotalActivos.text = when {
            estado.cargando || estado.error -> getString(R.string.rutinas_sin_dato)
            else -> estado.totalActivos.toString()
        }

        val visibles = estado.visibles
        adapter.submitList(if (estado.error) emptyList() else visibles)

        val mensaje = when {
            estado.cargando -> null
            estado.error -> R.string.mcli_error
            estado.todos.isEmpty() -> R.string.mcli_vacio
            visibles.isEmpty() -> R.string.mcli_sin_resultados
            else -> null
        }
        b.panelMensaje.visibility = if (mensaje == null) View.GONE else View.VISIBLE
        mensaje?.let { b.tvMensaje.setText(it) }
        b.btnReintentar.visibility = if (estado.error) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvClientes.adapter = null
        _binding = null
    }
}
