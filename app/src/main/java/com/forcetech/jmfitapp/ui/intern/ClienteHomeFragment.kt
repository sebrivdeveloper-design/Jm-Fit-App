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
import com.forcetech.jmfitapp.data.dao.EntrenadorContactoFila
import com.forcetech.jmfitapp.data.repository.ClienteRepository
import com.forcetech.jmfitapp.data.repository.RutinaCliente
import com.forcetech.jmfitapp.databinding.FragmentClienteHomeBinding
import com.forcetech.jmfitapp.databinding.ItemClienteEjercicioResumenBinding
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * HU Cliente — CA01: inicio del cliente interno.
 * - HU-27B: muestra la rutina ASIGNADA al cliente, con los siete días y el día actual seleccionado.
 * - HU-29: "Iniciar entrenamiento" abre WORKOUT; sin rutina asignada no se permite iniciar.
 * - HU-36: tarjeta del entrenador asignado con acceso a WhatsApp.
 * El resumen semanal y las fotos siguen siendo visuales (dependen de PROGRESS, que aún no existe).
 */
class ClienteHomeFragment : Fragment(R.layout.fragment_cliente_home) {

    private var _binding: FragmentClienteHomeBinding? = null
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
    private var diaSeleccionado = hoy
    private var entrenador: EntrenadorContactoFila? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentClienteHomeBinding.bind(view)
        diaSeleccionado = savedInstanceState?.getInt(K_DIA, hoy) ?: hoy
        val nav = findNavController()
        val datos = DatosClienteSimulados

        val primerNombre = BarraCliente.nombreCliente(requireContext()).trim().substringBefore(' ')
        binding.tvSaludo.text = getString(R.string.cli_saludo, primerNombre)
        binding.tvFecha.text = SimpleDateFormat("EEEE, d MMM", Locale.forLanguageTag("es-CO")).format(Date())

        binding.btnIniciarEntreno.setOnClickListener {
            BarraCliente.irA(nav, R.id.internWorkoutFragment)
        }
        binding.btnReintentar.setOnClickListener { viewModel.cargar() }
        binding.btnWhatsApp.setOnClickListener {
            WhatsAppEntrenador.abrir(this, binding.barraCliente, entrenador)
        }

        // Resumen semanal y fotos: siguen simulados (fuera del alcance de estas HU)
        binding.tvStatEntrenos.text = datos.ENTRENOS_SEMANA
        binding.tvStatPeso.text = datos.CAMBIO_PESO
        binding.tvStatRacha.text = datos.RACHA
        listOf(binding.tvFoto1, binding.tvFoto2, binding.tvFoto3)
            .zip(datos.SEMANAS_FOTOS)
            .forEach { (vista, semana) -> vista.text = semana }

        construirChipsDias()
        BarraCliente.configurar(this, binding.barraCliente, nav, R.id.internHomeFragment)

        viewModel.cargar()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.estado.collect { render(it) }
            }
        }
    }

    private fun render(estado: EstadoCliente) {
        val b = binding
        when (estado) {
            EstadoCliente.Cargando -> {
                b.tvNombreEntreno.setText(R.string.cli_cargando)
                b.tvDetalleEntreno.visibility = View.GONE
                b.btnIniciarEntreno.isEnabled = false
                b.btnReintentar.visibility = View.GONE
                b.seccionMiRutina.visibility = View.GONE
                mostrarEntrenador(null, cargando = true)
            }
            EstadoCliente.Error -> {
                b.tvNombreEntreno.setText(R.string.cli_error_rutina)
                b.tvDetalleEntreno.visibility = View.GONE
                b.btnIniciarEntreno.isEnabled = false
                b.btnReintentar.visibility = View.VISIBLE
                b.seccionMiRutina.visibility = View.GONE
                mostrarEntrenador(null, cargando = false)
            }
            is EstadoCliente.Listo -> {
                b.btnReintentar.visibility = View.GONE
                mostrarRutina(estado.datos.rutina)
                mostrarEntrenador(estado.datos.entrenador, cargando = false)
            }
        }
    }

    private fun mostrarRutina(rutina: RutinaCliente?) {
        val b = binding
        if (rutina == null) {
            // HU-27B c.a. 1 / HU-29 c.a. 1: sin rutina no se puede iniciar el entrenamiento
            b.tvNombreEntreno.setText(R.string.cli_sin_rutina)
            b.tvDetalleEntreno.visibility = View.GONE
            b.btnIniciarEntreno.isEnabled = false
            b.seccionMiRutina.visibility = View.GONE
            return
        }

        val ejerciciosHoy = rutina.ejerciciosDe(hoy)
        b.tvNombreEntreno.text = rutina.datos.nombre
        b.tvDetalleEntreno.visibility = View.VISIBLE
        b.tvDetalleEntreno.text = if (ejerciciosHoy.isEmpty()) {
            getString(R.string.cli_dia_descanso)
        } else {
            getString(R.string.cli_entreno_hoy_detalle, FormatoRutina.nombreDia(requireContext(), hoy), ejerciciosHoy.size)
        }
        b.btnIniciarEntreno.isEnabled = true

        b.seccionMiRutina.visibility = View.VISIBLE
        b.tvNombreRutina.text = listOfNotNull(
            rutina.datos.nombre,
            rutina.datos.objetivo?.takeIf { it.isNotBlank() },
            rutina.datos.nivel?.takeIf { it.isNotBlank() }
        ).joinToString(" · ")
        mostrarDia(rutina)
    }

    /** Ejercicios del día elegido, en el orden de la rutina. Solo lectura. */
    private fun mostrarDia(rutina: RutinaCliente) {
        val b = binding
        val ejercicios = rutina.ejerciciosDe(diaSeleccionado)
        b.contenedorDiaRutina.removeAllViews()
        b.tvDiaVacio.visibility = if (ejercicios.isEmpty()) View.VISIBLE else View.GONE
        ejercicios.forEach { e ->
            val item = ItemClienteEjercicioResumenBinding.inflate(layoutInflater, b.contenedorDiaRutina, true)
            item.tvNombre.text = e.nombre
            val detalle = FormatoRutina.detalle(requireContext(), e)
            item.tvDetalle.text = detalle
            item.tvDetalle.visibility = if (detalle.isEmpty()) View.GONE else View.VISIBLE
            val obs = e.observaciones?.takeIf { it.isNotBlank() }
            item.tvObservaciones.text = obs
            item.tvObservaciones.visibility = if (obs == null) View.GONE else View.VISIBLE
        }
    }

    /** Siete chips (L … D). El de hoy se marca con "· hoy" y queda seleccionado por defecto. */
    private fun construirChipsDias() {
        val grupo = binding.chipsDias
        grupo.setOnCheckedStateChangeListener(null)
        grupo.removeAllViews()
        for (dia in 1..7) {
            val chip = layoutInflater.inflate(R.layout.item_chip_categoria, grupo, false) as Chip
            chip.id = View.generateViewId()
            val corto = FormatoRutina.nombreCortoDia(requireContext(), dia)
            chip.text = if (dia == hoy) "$corto · hoy" else corto
            chip.contentDescription = FormatoRutina.nombreDia(requireContext(), dia)
            chip.tag = dia
            chip.isChecked = dia == diaSeleccionado
            grupo.addView(chip)
        }
        grupo.setOnCheckedStateChangeListener { g, ids ->
            val dia = ids.firstOrNull()?.let { g.findViewById<Chip>(it)?.tag as? Int } ?: return@setOnCheckedStateChangeListener
            diaSeleccionado = dia
            (viewModel.estado.value as? EstadoCliente.Listo)?.datos?.rutina?.let { mostrarDia(it) }
        }
    }

    private fun mostrarEntrenador(datos: EntrenadorContactoFila?, cargando: Boolean) {
        entrenador = datos
        val b = binding
        b.tvHaceMensaje.setText(R.string.cli_tu_entrenador)
        when {
            cargando -> {
                b.tvNombreEntrenador.setText(R.string.cli_cargando)
                b.tvMensajeEntrenador.visibility = View.GONE
                b.btnWhatsApp.visibility = View.GONE
            }
            datos == null -> {
                b.tvNombreEntrenador.setText(R.string.cli_sin_entrenador)
                b.tvMensajeEntrenador.visibility = View.GONE
                b.btnWhatsApp.visibility = View.GONE
            }
            else -> {
                b.tvNombreEntrenador.text = datos.nombre
                val especialidad = datos.especialidad?.takeIf { it.isNotBlank() }
                b.tvMensajeEntrenador.text = especialidad
                b.tvMensajeEntrenador.visibility = if (especialidad == null) View.GONE else View.VISIBLE
                b.btnWhatsApp.visibility = View.VISIBLE
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(K_DIA, diaSeleccionado)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val K_DIA = "dia_seleccionado"
    }
}
