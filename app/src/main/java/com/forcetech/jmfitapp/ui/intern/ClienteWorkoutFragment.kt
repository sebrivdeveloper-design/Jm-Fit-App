package com.forcetech.jmfitapp.ui.intern

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.forcetech.jmfitapp.JmFitApplication
import com.forcetech.jmfitapp.R
import com.forcetech.jmfitapp.data.repository.ClienteRepository
import com.forcetech.jmfitapp.data.repository.RutinaCliente
import com.forcetech.jmfitapp.databinding.FragmentClienteWorkoutBinding
import com.forcetech.jmfitapp.databinding.ItemClienteEjercicioBinding
import com.forcetech.jmfitapp.model.TecnicaEjercicio
import kotlinx.coroutines.launch

/**
 * HU Cliente — CA02 / HU-29: ejercicios del día ACTUAL de la rutina asignada al cliente.
 * - Sin rutina: "Aún no tienes una rutina asignada…"; día sin ejercicios: "Hoy es día de descanso".
 * - Error al cargar: "No fue posible cargar tu entrenamiento" con [Reintentar].
 * Las casillas siguen siendo visuales: registrar el avance es la HU-28 (necesita tablas nuevas).
 */
class ClienteWorkoutFragment : Fragment(R.layout.fragment_cliente_workout) {

    private var _binding: FragmentClienteWorkoutBinding? = null
    private val binding get() = _binding!!

    private val appContainer
        get() = (requireActivity().application as JmFitApplication).container

    private val viewModel: ClienteInternoViewModel by viewModels {
        ClienteInternoViewModel.Factory(
            appContainer.clienteRepository,
            appContainer.sessionManager.sesionActual()?.usuarioId
        )
    }

    private val hoy = ClienteRepository.diaSemana()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentClienteWorkoutBinding.bind(view)
        val nav = findNavController()

        binding.btnVolver.setOnClickListener {
            BarraCliente.irA(nav, R.id.internHomeFragment)
        }
        binding.btnReintentar.setOnClickListener { viewModel.cargar() }
        binding.tvDiaPrograma.text = FormatoRutina.nombreDia(requireContext(), hoy)

        BarraCliente.configurar(this, binding.barraCliente, nav, R.id.internWorkoutFragment)

        viewModel.cargar()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.estado.collect { render(it) }
            }
        }
    }

    private fun render(estado: EstadoCliente) {
        val b = binding
        b.contenedorEjercicios.removeAllViews()
        b.btnReintentar.visibility = View.GONE
        when (estado) {
            EstadoCliente.Cargando -> {
                b.tvNombreEntreno.setText(R.string.cli_cargando)
                b.tvGrupoSemana.visibility = View.GONE
                b.tvMensajeEntreno.visibility = View.GONE
            }
            EstadoCliente.Error -> {
                b.tvNombreEntreno.setText(R.string.cli_label_workout)
                b.tvGrupoSemana.visibility = View.GONE
                mostrarMensaje(getString(R.string.cli_error_entreno))
                b.btnReintentar.visibility = View.VISIBLE
            }
            is EstadoCliente.Listo -> mostrarRutina(estado.datos.rutina)
        }
    }

    private fun mostrarRutina(rutina: RutinaCliente?) {
        val b = binding
        if (rutina == null) {
            b.tvNombreEntreno.setText(R.string.cli_label_workout)
            b.tvGrupoSemana.visibility = View.GONE
            mostrarMensaje(getString(R.string.cli_sin_rutina))
            return
        }

        b.tvNombreEntreno.text = rutina.datos.nombre
        val subtitulo = listOfNotNull(
            rutina.datos.objetivo?.takeIf { it.isNotBlank() },
            rutina.datos.nivel?.takeIf { it.isNotBlank() }
        ).joinToString(" · ")
        b.tvGrupoSemana.text = subtitulo
        b.tvGrupoSemana.visibility = if (subtitulo.isEmpty()) View.GONE else View.VISIBLE

        val ejercicios = rutina.ejerciciosDe(hoy)
        if (ejercicios.isEmpty()) {
            mostrarMensaje(getString(R.string.cli_dia_descanso))
            return
        }
        b.tvMensajeEntreno.visibility = View.GONE

        val guion = getString(R.string.cli_guion)
        ejercicios.forEach { e ->
            val item = ItemClienteEjercicioBinding.inflate(layoutInflater, b.contenedorEjercicios, true)
            item.tvNombre.text = e.nombre
            item.tvSeries.text = e.series?.toString() ?: guion
            item.tvReps.text = FormatoRutina.repeticiones(e) ?: guion
            item.tvPeso.text = FormatoRutina.pesoTexto(requireContext(), e) ?: guion
            item.tvDescanso.text = FormatoRutina.descanso(requireContext(), e) ?: guion

            // Indicaciones: tipo de bloque, técnica y observaciones (lo que exista)
            val indicacion = listOfNotNull(
                FormatoRutina.tipoBloque(e.tipoAgrupacion),
                if (e.tecnica == TecnicaEjercicio.DROPSET) getString(R.string.cli_dropset) else null,
                e.observaciones?.takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            item.tvIndicacion.text = indicacion
            item.tvIndicacion.visibility = if (indicacion.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun mostrarMensaje(texto: String) {
        binding.tvMensajeEntreno.text = texto
        binding.tvMensajeEntreno.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
