package com.forcetech.jmfitapp.ui.trainer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.forcetech.jmfitapp.JmFitApplication
import com.forcetech.jmfitapp.R
import com.forcetech.jmfitapp.data.repository.DetalleRutina
import com.forcetech.jmfitapp.data.repository.EjercicioDetalle
import com.forcetech.jmfitapp.databinding.FragmentRutinaDetalleBinding
import com.forcetech.jmfitapp.databinding.ItemDetalleDiaBinding
import com.forcetech.jmfitapp.databinding.ItemDetalleEjercicioBinding
import com.forcetech.jmfitapp.model.CatalogoRutinas
import com.forcetech.jmfitapp.model.EstadoRutina
import com.forcetech.jmfitapp.model.TipoRutina
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** ENT-6 (detalle): días de entrenamiento y ejercicios con series, repeticiones y peso. Solo lectura. */
class RutinaDetalleFragment : Fragment(R.layout.fragment_rutina_detalle) {

    private var _binding: FragmentRutinaDetalleBinding? = null
    private val binding get() = _binding!!

    private val appContainer
        get() = (requireActivity().application as JmFitApplication).container

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRutinaDetalleBinding.bind(view)

        binding.btnVolver.setOnClickListener { findNavController().popBackStack() }
        binding.btnReintentar.setOnClickListener { cargar() }
        cargar()
    }

    private fun cargar() {
        val rutinaId = requireArguments().getLong(ARG_RUTINA_ID)
        val entrenadorId = appContainer.sessionManager.sesionActual()?.usuarioId ?: -1L

        viewLifecycleOwner.lifecycleScope.launch {
            val detalle = try {
                appContainer.rutinaRepository.detalle(rutinaId, entrenadorId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mostrarMensaje(R.string.detalle_error_cargar, conReintento = true)
                return@launch
            }
            if (detalle == null) {
                mostrarMensaje(R.string.detalle_no_disponible, conReintento = false)
            } else {
                mostrarDetalle(detalle)
            }
        }
    }

    private fun mostrarMensaje(mensaje: Int, conReintento: Boolean) {
        binding.bloqueContenido.visibility = View.GONE
        binding.bloqueMensaje.visibility = View.VISIBLE
        binding.tvMensaje.setText(mensaje)
        binding.btnReintentar.visibility = if (conReintento) View.VISIBLE else View.GONE
    }

    private fun mostrarDetalle(detalle: DetalleRutina) {
        val rutina = detalle.rutina
        val activa = rutina.estado == EstadoRutina.ACTIVA
        val personalizada = rutina.tipo == TipoRutina.PERSONALIZADA
        val sinDato = getString(R.string.rutinas_sin_dato)

        binding.bloqueMensaje.visibility = View.GONE
        binding.bloqueContenido.visibility = View.VISIBLE

        binding.tvTipo.setText(
            if (personalizada) R.string.detalle_tipo_personalizada else R.string.rutinas_tab_estandar
        )
        binding.tvNombreDetalle.text = rutina.nombre
        binding.tvEstadoDetalle.setText(
            if (activa) R.string.rutinas_estado_activa else R.string.rutinas_estado_inhabilitada
        )
        binding.tvEstadoDetalle.setBackgroundResource(
            if (activa) R.drawable.bg_chip_activa else R.drawable.bg_chip_inhabilitada
        )
        binding.tvEstadoDetalle.setTextColor(
            ContextCompat.getColor(
                requireContext(), if (activa) R.color.jmfit_primary else R.color.jmfit_text_secondary
            )
        )

        binding.tvObjetivoDetalle.text = rutina.objetivo ?: sinDato
        binding.tvNivelDetalle.text = rutina.nivel ?: sinDato

        binding.bloqueAsignadaDetalle.visibility = if (personalizada) View.VISIBLE else View.GONE
        binding.tvAsignadaDetalle.text = detalle.clienteNombre ?: getString(R.string.rutinas_sin_asignar)

        val notas = rutina.notas
        binding.bloqueNotas.visibility = if (notas.isNullOrBlank()) View.GONE else View.VISIBLE
        binding.tvNotasDetalle.text = notas.orEmpty()

        // Días y ejercicios
        val inflater = LayoutInflater.from(requireContext())
        binding.contenedorDias.removeAllViews()
        val nombresDias = CatalogoRutinas.DIAS.toMap()
        for (dia in detalle.dias) {
            val itemDia = ItemDetalleDiaBinding.inflate(inflater, binding.contenedorDias, false)
            itemDia.tvDia.text = nombresDias[dia.diaSemana] ?: dia.diaSemana.toString()
            for (ejercicio in dia.ejercicios) {
                val itemEj = ItemDetalleEjercicioBinding.inflate(inflater, itemDia.contenedorEjercicios, false)
                itemEj.tvEjercicioNombre.text = ejercicio.nombre
                itemEj.tvEjercicioDatos.text = describir(ejercicio, sinDato)
                itemDia.contenedorEjercicios.addView(itemEj.root)
            }
            binding.contenedorDias.addView(itemDia.root)
        }
        binding.tvSinEjercicios.visibility = if (detalle.dias.isEmpty()) View.VISIBLE else View.GONE
    }

    /** Ej.: "4 series × 10 reps · 18 kg · Descanso 60 s". El peso y el descanso solo se muestran si existen. */
    private fun describir(e: EjercicioDetalle, sinDato: String): String {
        val partes = mutableListOf(
            getString(
                R.string.ejercicio_series_reps,
                e.series?.toString() ?: sinDato,
                e.repeticiones ?: sinDato
            )
        )
        e.pesoKg?.let { partes += getString(R.string.ejercicio_peso, formatearPeso(it)) }
        e.descansoSegundos?.let { partes += getString(R.string.ejercicio_descanso, it) }
        return partes.joinToString(" · ")
    }

    private fun formatearPeso(peso: Double): String =
        if (peso % 1.0 == 0.0) peso.toInt().toString() else peso.toString()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_RUTINA_ID = "rutinaId"
    }
}
