package com.forcetech.jmfitapp.ui.trainer

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
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
import com.forcetech.jmfitapp.data.dao.RutinaResumen
import com.forcetech.jmfitapp.databinding.FragmentRutinasBinding
import com.forcetech.jmfitapp.model.EstadoRutina
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * ENT-6 (listado de rutinas) y ENT-7 (inhabilitar / reactivar). Pestañas "Estándar" (solo lectura)
 * y "Mis personalizadas" (las del entrenador con sesión).
 */
class RutinasFragment : Fragment(R.layout.fragment_rutinas) {

    private var _binding: FragmentRutinasBinding? = null
    private val binding get() = _binding!!

    private val appContainer
        get() = (requireActivity().application as JmFitApplication).container

    private val viewModel: RutinasViewModel by viewModels {
        // Si por alguna razón no hay sesión, -1 no coincide con ningún entrenador y la lista sale vacía.
        val entrenadorId = appContainer.sessionManager.sesionActual()?.usuarioId ?: -1L
        RutinasViewModel.Factory(appContainer.rutinaRepository, entrenadorId)
    }

    private lateinit var adapter: RutinaAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRutinasBinding.bind(view)

        binding.tvFecha.text = fechaDeHoy()

        adapter = RutinaAdapter(
            alVer = { abrirDetalle(it) },
            alEditar = { avisarProximamente() }, // ENT-8
            alCambiarEstado = { confirmarCambioEstado(it) }
        )
        binding.rvRutinas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRutinas.adapter = adapter

        binding.btnCrearPersonalizada.setOnClickListener { avisarProximamente() } // ENT-5

        binding.btnCerrarSesion.setOnClickListener {
            appContainer.authRepository.cerrarSesion()
            findNavController().navigate(R.id.action_global_login)
        }

        binding.toggleTipo.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                viewModel.seleccionarPestana(
                    if (checkedId == R.id.btnEstandar) PestanaRutinas.ESTANDAR else PestanaRutinas.PERSONALIZADAS
                )
            }
        }
        binding.etBuscar.doOnTextChanged { texto, _, _, _ -> viewModel.buscar(texto?.toString().orEmpty()) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { render(it) } }
                launch {
                    viewModel.mensajes.collect { Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show() }
                }
            }
        }
    }

    private fun render(estado: RutinasUiState) {
        // Mantener la pestaña marcada en sincronía con el estado (por ejemplo, al rotar la pantalla)
        val idEsperado =
            if (estado.pestana == PestanaRutinas.ESTANDAR) R.id.btnEstandar else R.id.btnPersonalizadas
        if (binding.toggleTipo.checkedButtonId != idEsperado) binding.toggleTipo.check(idEsperado)

        adapter.submitList(estado.rutinas)

        val mensaje = when {
            estado.cargando -> null
            estado.error -> R.string.detalle_error_cargar
            estado.rutinas.isNotEmpty() -> null
            estado.totalSinFiltrar > 0 -> R.string.rutinas_sin_resultados
            estado.pestana == PestanaRutinas.ESTANDAR -> R.string.rutinas_vacio_estandar
            else -> R.string.rutinas_vacio_personalizadas
        }
        binding.tvVacio.visibility = if (mensaje == null) View.GONE else View.VISIBLE
        if (mensaje != null) binding.tvVacio.setText(mensaje)
    }

    private fun abrirDetalle(rutina: RutinaResumen) {
        findNavController().navigate(
            R.id.trainerRutinaDetalleFragment,
            bundleOf(RutinaDetalleFragment.ARG_RUTINA_ID to rutina.id)
        )
    }

    /** ENT-7: pide confirmación (Aceptar / Cancelar) antes de cambiar el estado. */
    private fun confirmarCambioEstado(rutina: RutinaResumen) {
        val inhabilitar = rutina.estado == EstadoRutina.ACTIVA
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (inhabilitar) R.string.rutinas_inhabilitar_titulo else R.string.rutinas_reactivar_titulo)
            .setMessage(
                getString(
                    if (inhabilitar) R.string.rutinas_inhabilitar_mensaje else R.string.rutinas_reactivar_mensaje,
                    rutina.nombre
                )
            )
            .setPositiveButton(R.string.dialogo_aceptar) { _, _ -> viewModel.cambiarEstado(rutina) }
            .setNegativeButton(R.string.dialogo_cancelar, null)
            .show()
    }

    private fun avisarProximamente() {
        Snackbar.make(binding.root, R.string.rutinas_proximamente, Snackbar.LENGTH_SHORT).show()
    }

    private fun fechaDeHoy(): String {
        val local = Locale("es", "CO")
        return SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", local)
            .format(Date())
            .replaceFirstChar { it.titlecase(local) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
