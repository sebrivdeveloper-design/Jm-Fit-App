package com.forcetech.jmfitapp.ui.trainer

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.forcetech.jmfitapp.JmFitApplication
import com.forcetech.jmfitapp.R
import com.forcetech.jmfitapp.data.dao.ClienteOpcion
import com.forcetech.jmfitapp.data.repository.LineaRutina
import com.forcetech.jmfitapp.databinding.FragmentRutinaFormBinding
import com.forcetech.jmfitapp.databinding.ItemLineaFormBinding
import com.forcetech.jmfitapp.model.CatalogoRutinas
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

/**
 * ENT-5 (crear rutina personalizada y asignarla en un solo paso) y ENT-8 (editar).
 * Es la misma pantalla: con `rutinaId` = 0 crea; con `rutinaId` > 0 edita esa rutina.
 */
class RutinaFormFragment : Fragment(R.layout.fragment_rutina_form) {

    private var _binding: FragmentRutinaFormBinding? = null
    private val binding get() = _binding!!

    private val appContainer
        get() = (requireActivity().application as JmFitApplication).container

    private val viewModel: RutinaFormViewModel by viewModels {
        val entrenadorId = appContainer.sessionManager.sesionActual()?.usuarioId ?: -1L
        val rutinaId = arguments?.getLong(ARG_RUTINA_ID, 0L) ?: 0L
        RutinaFormViewModel.Factory(appContainer.rutinaRepository, entrenadorId, rutinaId)
    }

    // Para no reconstruir vistas cuando el estado no cambió en lo que importa
    private var clientesMostrados: List<ClienteOpcion>? = null
    private var ejerciciosMostrados: List<*>? = null
    private var lineasMostradas: List<LineaRutina>? = null
    private var indiceMostrado: Int? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRutinaFormBinding.bind(view)
        clientesMostrados = null
        ejerciciosMostrados = null
        lineasMostradas = null
        indiceMostrado = null

        configurarTextos()
        configurarListasFijas()
        configurarCliente()
        configurarLimpiezaDeErrores()

        binding.btnVolverForm.setOnClickListener { findNavController().popBackStack() }
        binding.btnCancelarForm.setOnClickListener { findNavController().popBackStack() }
        binding.btnAgregarEjercicio.setOnClickListener { agregarOActualizarEjercicio() }
        binding.btnCancelarEdicionLinea.setOnClickListener { viewModel.cancelarEdicionLinea() }
        binding.btnGuardar.setOnClickListener { intentarGuardar() }
        binding.etPeso.setOnEditorActionListener { _, _, _ ->
            agregarOActualizarEjercicio()
            true
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { render(it) } }
                launch { viewModel.eventos.collect { atender(it) } }
            }
        }
    }

    // ---------- Configuración inicial ----------

    private fun configurarTextos() {
        val edicion = viewModel.esEdicion
        binding.tvTituloForm.setText(if (edicion) R.string.form_titulo_editar else R.string.form_titulo_crear)
        binding.tvSubtituloForm.setText(
            if (edicion) R.string.form_subtitulo_editar else R.string.form_subtitulo_crear
        )
        binding.btnGuardar.setText(if (edicion) R.string.form_guardar_cambios else R.string.form_guardar_rutina)
    }

    private fun configurarListasFijas() {
        binding.actObjetivo.setAdapter(AdaptadorSinFiltro(requireContext(), CatalogoRutinas.OBJETIVOS))
        binding.actNivel.setAdapter(AdaptadorSinFiltro(requireContext(), CatalogoRutinas.NIVELES))
        binding.actDia.setAdapter(AdaptadorSinFiltro(requireContext(), CatalogoRutinas.DIAS.map { it.second }))
    }

    private fun configurarCliente() {
        binding.actCliente.onItemClickListener = AdapterView.OnItemClickListener { parent, _, posicion, _ ->
            viewModel.seleccionarCliente(parent.getItemAtPosition(posicion) as? ClienteOpcion)
        }
        // Si la persona edita el texto después de elegir, la selección deja de ser válida
        binding.actCliente.doOnTextChanged { texto, _, _, _ ->
            binding.tilCliente.error = null
            val elegido = viewModel.uiState.value.cliente
            if (elegido != null && texto?.toString() != elegido.nombre) viewModel.seleccionarCliente(null)
        }
    }

    private fun configurarLimpiezaDeErrores() {
        binding.etNombre.doOnTextChanged { _, _, _, _ -> binding.tilNombre.error = null }
        binding.actObjetivo.doOnTextChanged { _, _, _, _ -> binding.tilObjetivo.error = null }
        binding.actNivel.doOnTextChanged { _, _, _, _ -> binding.tilNivel.error = null }
        binding.actDia.doOnTextChanged { _, _, _, _ -> binding.tilDia.error = null }
        binding.actEjercicio.doOnTextChanged { _, _, _, _ -> binding.tilEjercicio.error = null }
        binding.etSeries.doOnTextChanged { _, _, _, _ -> binding.tilSeries.error = null }
        binding.etRepeticiones.doOnTextChanged { _, _, _, _ -> binding.tilRepeticiones.error = null }
        binding.etPeso.doOnTextChanged { _, _, _, _ -> binding.tilPeso.error = null }
    }

    // ---------- Pintar el estado ----------

    private fun render(estado: RutinaFormUiState) {
        binding.progresoForm.visibility = if (estado.cargando || estado.guardando) View.VISIBLE else View.GONE
        binding.tvNoDisponible.visibility = if (estado.noDisponible) View.VISIBLE else View.GONE
        binding.bloqueForm.visibility = if (estado.cargando || estado.noDisponible) View.GONE else View.VISIBLE
        binding.btnGuardar.isEnabled = !estado.guardando

        // Cliente
        if (estado.clientes != clientesMostrados) {
            clientesMostrados = estado.clientes
            binding.actCliente.setAdapter(
                ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, estado.clientes)
            )
        }
        binding.tilCliente.isEnabled = !estado.clienteBloqueado
        binding.actCliente.isEnabled = !estado.clienteBloqueado
        val cliente = estado.cliente
        if (cliente != null && binding.actCliente.text.toString() != cliente.nombre) {
            binding.actCliente.setText(cliente.nombre, false)
        }

        // Ejercicios disponibles (se escoge por nombre)
        if (estado.ejercicios != ejerciciosMostrados) {
            ejerciciosMostrados = estado.ejercicios
            binding.actEjercicio.setAdapter(AdaptadorSinFiltro(requireContext(), estado.ejercicios))
        }

        // Botón del ejercicio: agregar o actualizar
        val editando = estado.editandoIndice != null
        binding.btnAgregarEjercicio.setText(
            if (editando) R.string.form_actualizar_ejercicio else R.string.form_agregar_ejercicio
        )
        binding.btnCancelarEdicionLinea.visibility = if (editando) View.VISIBLE else View.GONE

        // Lista de ejercicios agregados
        if (estado.lineas != lineasMostradas || estado.editandoIndice != indiceMostrado) {
            lineasMostradas = estado.lineas
            indiceMostrado = estado.editandoIndice
            pintarLineas(estado.lineas, estado.editandoIndice)
        }
        if (estado.lineas.isNotEmpty()) binding.tvErrorLineas.visibility = View.GONE
    }

    /** Agrupa los ejercicios por día (lunes → domingo), respetando el orden en que se agregaron. */
    private fun pintarLineas(lineas: List<LineaRutina>, editando: Int?) {
        val contenedor = binding.contenedorLineas
        contenedor.removeAllViews()
        binding.tvContador.text = getString(R.string.form_contador, lineas.size)
        binding.tvSinLineas.visibility = if (lineas.isEmpty()) View.VISIBLE else View.GONE

        val inflater = LayoutInflater.from(requireContext())
        val porDia = lineas.withIndex().groupBy { it.value.dia }.toSortedMap()
        for ((dia, items) in porDia) {
            val encabezado = TextView(requireContext()).apply {
                text = CatalogoRutinas.DIAS.firstOrNull { it.first == dia }?.second ?: dia.toString()
                setTextColor(requireContext().getColor(R.color.jmfit_primary))
                textSize = 13f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(0, dp(8), 0, dp(2))
            }
            contenedor.addView(encabezado)

            for ((indice, linea) in items) {
                val fila = ItemLineaFormBinding.inflate(inflater, contenedor, false)
                fila.tvLineaNombre.text = linea.ejercicioNombre
                fila.tvLineaDatos.text = if (linea.pesoKg != null) {
                    getString(R.string.form_linea_datos_peso, linea.series, linea.repeticiones, formatearPeso(linea.pesoKg))
                } else {
                    getString(R.string.form_linea_datos, linea.series, linea.repeticiones)
                }
                if (indice == editando) fila.rootLinea.setBackgroundResource(R.drawable.bg_linea_editando)
                fila.btnEditarLinea.setOnClickListener { viewModel.editarLinea(indice) }
                fila.rootLinea.setOnClickListener { viewModel.editarLinea(indice) }
                fila.btnQuitarLinea.setOnClickListener { viewModel.quitarLinea(indice) }
                contenedor.addView(fila.root)
            }
        }
    }

    // ---------- Eventos del ViewModel ----------

    private fun atender(evento: RutinaFormEvento) {
        when (evento) {
            is RutinaFormEvento.Precargar -> {
                binding.etNombre.setText(evento.textos.nombre)
                binding.actObjetivo.setText(evento.textos.objetivo, false)
                binding.actNivel.setText(evento.textos.nivel, false)
                binding.etNotas.setText(evento.textos.notas)
            }
            is RutinaFormEvento.CargarLinea -> cargarLineaEnEditor(evento.linea)
            RutinaFormEvento.LimpiarEditor -> limpiarEditor()
            is RutinaFormEvento.Confirmar -> mostrarConfirmacion(evento)
            is RutinaFormEvento.Mensaje -> Snackbar.make(binding.root, evento.texto, Snackbar.LENGTH_LONG).show()
            RutinaFormEvento.NombreDuplicado -> {
                binding.tilNombre.error = getString(R.string.form_nombre_duplicado)
                enfocar(binding.etNombre)
            }
            is RutinaFormEvento.Guardado -> {
                parentFragmentManager.setFragmentResult(RESULT_KEY, bundleOf(RESULT_MENSAJE to evento.texto))
                findNavController().popBackStack()
            }
        }
    }

    private fun cargarLineaEnEditor(linea: LineaRutina) {
        binding.actDia.setText(CatalogoRutinas.DIAS.firstOrNull { it.first == linea.dia }?.second.orEmpty(), false)
        binding.actEjercicio.setText(linea.ejercicioNombre, false)
        binding.etSeries.setText(linea.series.toString())
        binding.etRepeticiones.setText(linea.repeticiones)
        binding.etPeso.setText(linea.pesoKg?.let { formatearPeso(it) }.orEmpty())
        limpiarErroresEjercicio()
        enfocar(binding.actEjercicio)
    }

    /** Deja el editor listo para otro ejercicio; el día se conserva porque suelen agregarse varios seguidos. */
    private fun limpiarEditor() {
        binding.actEjercicio.setText("", false)
        binding.etSeries.setText("")
        binding.etRepeticiones.setText("")
        binding.etPeso.setText("")
        limpiarErroresEjercicio()
    }

    private fun limpiarErroresEjercicio() {
        binding.tilDia.error = null
        binding.tilEjercicio.error = null
        binding.tilSeries.error = null
        binding.tilRepeticiones.error = null
        binding.tilPeso.error = null
    }

    // ---------- Validación ----------

    private fun agregarOActualizarEjercicio() {
        val estado = viewModel.uiState.value
        var primerError: View? = null
        fun fallo(capa: TextInputLayout, mensaje: Int, foco: View) {
            capa.error = getString(mensaje)
            if (primerError == null) primerError = foco
        }

        val nombreDia = binding.actDia.text.toString().trim()
        val dia = CatalogoRutinas.DIAS.firstOrNull { it.second == nombreDia }?.first
        if (dia == null) fallo(binding.tilDia, R.string.form_error_requerido, binding.actDia)

        val ejercicio = estado.ejercicios.firstOrNull { it.nombre == binding.actEjercicio.text.toString().trim() }
        if (ejercicio == null) fallo(binding.tilEjercicio, R.string.form_error_requerido, binding.actEjercicio)

        val series = binding.etSeries.text.toString().trim().toIntOrNull()
        if (series == null || series !in 1..20) fallo(binding.tilSeries, R.string.form_error_series, binding.etSeries)

        val reps = binding.etRepeticiones.text.toString().trim()
        if (reps.isEmpty() || reps.none { it.isDigit() } || reps.length > 20) {
            fallo(binding.tilRepeticiones, R.string.form_error_repeticiones, binding.etRepeticiones)
        }

        // El peso es opcional, pero si se escribe debe ser un número mayor que 0
        val textoPeso = binding.etPeso.text.toString().trim().replace(',', '.')
        val peso = textoPeso.toDoubleOrNull()
        if (textoPeso.isNotEmpty() && (peso == null || peso <= 0.0 || peso > 999.9)) {
            fallo(binding.tilPeso, R.string.form_error_peso, binding.etPeso)
        }

        primerError?.let { enfocar(it); return }
        if (dia == null || ejercicio == null || series == null) return

        viewModel.guardarLinea(
            LineaRutina(
                dia = dia,
                ejercicioId = ejercicio.id,
                ejercicioNombre = ejercicio.nombre,
                series = series,
                repeticiones = reps,
                pesoKg = if (textoPeso.isEmpty()) null else peso
            )
        )
    }

    private fun intentarGuardar() {
        val estado = viewModel.uiState.value
        var primerError: View? = null
        fun fallo(capa: TextInputLayout, mensaje: Int, foco: View) {
            capa.error = getString(mensaje)
            if (primerError == null) primerError = foco
        }

        // Cliente: obligatorio al crear; al editar solo si la rutina ya lo tenía (viene bloqueado y precargado)
        val textoCliente = binding.actCliente.text.toString().trim()
        when {
            estado.cliente != null -> Unit
            viewModel.esEdicion && textoCliente.isEmpty() -> Unit
            textoCliente.isEmpty() && estado.clientes.isEmpty() ->
                fallo(binding.tilCliente, R.string.form_sin_clientes, binding.actCliente)
            textoCliente.isEmpty() -> fallo(binding.tilCliente, R.string.form_error_requerido, binding.actCliente)
            else -> fallo(binding.tilCliente, R.string.form_error_cliente_lista, binding.actCliente)
        }

        if (binding.etNombre.text.toString().trim().isEmpty()) {
            fallo(binding.tilNombre, R.string.form_error_requerido, binding.etNombre)
        }
        if (binding.actObjetivo.text.toString() !in CatalogoRutinas.OBJETIVOS) {
            fallo(binding.tilObjetivo, R.string.form_error_requerido, binding.actObjetivo)
        }
        if (binding.actNivel.text.toString() !in CatalogoRutinas.NIVELES) {
            fallo(binding.tilNivel, R.string.form_error_requerido, binding.actNivel)
        }
        if (estado.lineas.isEmpty()) {
            binding.tvErrorLineas.visibility = View.VISIBLE
            if (primerError == null) primerError = binding.tvErrorLineas
        }

        primerError?.let { enfocar(it); return }
        viewModel.pedirConfirmacion(binding.etNombre.text.toString().trim())
    }

    private fun mostrarConfirmacion(evento: RutinaFormEvento.Confirmar) {
        val estado = viewModel.uiState.value
        val edicion = viewModel.esEdicion
        val sb = StringBuilder()
        if (edicion) {
            sb.append(getString(R.string.form_confirmar_editar_mensaje, evento.nombre))
            // En edición solo se asigna cliente si la rutina no tenía uno
            if (evento.cliente != null && !estado.clienteBloqueado) {
                sb.append(getString(R.string.form_confirmar_editar_cliente, evento.cliente.nombre))
            }
        } else {
            sb.append(getString(R.string.form_confirmar_crear_mensaje, evento.nombre, evento.cliente?.nombre.orEmpty()))
        }
        evento.rutinaActivaDelCliente?.let { sb.append(getString(R.string.form_confirmar_crear_reemplazo, it)) }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (edicion) R.string.form_confirmar_editar_titulo else R.string.form_confirmar_crear_titulo)
            .setMessage(sb.toString())
            .setPositiveButton(R.string.dialogo_aceptar) { _, _ ->
                viewModel.guardar(
                    nombre = binding.etNombre.text.toString(),
                    objetivo = binding.actObjetivo.text.toString(),
                    nivel = binding.actNivel.text.toString(),
                    notas = binding.etNotas.text.toString()
                )
            }
            .setNegativeButton(R.string.dialogo_cancelar, null)
            .show()
    }

    // ---------- Utilidades ----------

    /** Lleva el foco a la vista con error y desplaza la pantalla hasta ella. */
    private fun enfocar(vista: View) {
        vista.requestFocus()
        var y = 0
        var actual: View = vista
        while (actual !== binding.scrollForm) {
            y += actual.top
            actual = actual.parent as? View ?: break
        }
        binding.scrollForm.smoothScrollTo(0, (y - dp(24)).coerceAtLeast(0))
    }

    private fun formatearPeso(peso: Double): String =
        if (peso % 1.0 == 0.0) peso.toLong().toString() else peso.toString()

    private fun dp(valor: Int): Int = (valor * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Adaptador para listas desplegables fijas: muestra siempre todas las opciones, aunque el campo ya tenga
     * un texto (por ejemplo, al precargar o al restaurar la pantalla), cosa que el filtro normal no permite.
     */
    private class AdaptadorSinFiltro<T>(context: Context, private val datos: List<T>) :
        ArrayAdapter<T>(context, android.R.layout.simple_dropdown_item_1line, datos) {

        private val todos = object : Filter() {
            override fun performFiltering(constraint: CharSequence?) =
                FilterResults().apply {
                    values = datos
                    count = datos.size
                }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                notifyDataSetChanged()
            }

            override fun convertResultToString(resultValue: Any?): CharSequence = resultValue.toString()
        }

        override fun getFilter(): Filter = todos
    }

    companion object {
        const val ARG_RUTINA_ID = "rutinaId"

        /** Clave y dato con que el formulario avisa al listado que guardó (para mostrar el mensaje). */
        const val RESULT_KEY = "rutina_form_resultado"
        const val RESULT_MENSAJE = "mensaje"
    }
}
