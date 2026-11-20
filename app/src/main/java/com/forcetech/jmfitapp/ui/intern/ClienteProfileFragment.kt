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
import com.forcetech.jmfitapp.data.dao.PerfilClienteFila
import com.forcetech.jmfitapp.data.repository.ClienteRepository
import com.forcetech.jmfitapp.data.repository.RutinaCliente
import com.forcetech.jmfitapp.databinding.FragmentClienteProfileBinding
import com.forcetech.jmfitapp.databinding.ItemClienteDatoBinding
import com.forcetech.jmfitapp.ui.common.TemaApp
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * HU Cliente — CA03 / HU-33: perfil del cliente interno con los datos reales de su cuenta.
 * - Datos de la cuenta: nombre, usuario, cédula, correo, celular, fecha de nacimiento y sexo.
 * - Cuerpo: edad (calculada), estatura; peso actual y objetivo quedan "No registrado" hasta HU-63.
 * - Programa: rutina asignada (o "Actualmente no tienes un programa…").
 * Cualquier dato vacío se muestra como "No registrado". Solo se consultan los datos del cliente con sesión.
 * Las opciones de la lista (historial, pedidos, notificaciones, configuración) siguen siendo visuales.
 */
class ClienteProfileFragment : Fragment(R.layout.fragment_cliente_profile) {

    private var _binding: FragmentClienteProfileBinding? = null
    private val binding get() = _binding!!

    private val appContainer
        get() = (requireActivity().application as JmFitApplication).container

    private val viewModel: ClienteInternoViewModel by viewModels {
        ClienteInternoViewModel.Factory(
            appContainer.clienteRepository,
            appContainer.sessionManager.sesionActual()?.usuarioId
        )
    }

    private val localeCo = Locale.forLanguageTag("es-CO")
    private var avisoError: Snackbar? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentClienteProfileBinding.bind(view)

        binding.tvNombreCliente.text = BarraCliente.nombreCliente(requireContext())
        binding.btnTema.setOnClickListener { TemaApp.mostrarSelector(requireContext()) }

        // Igual que RolePlaceholderFragment: borra la sesión y vuelve al login limpiando el historial
        binding.btnCerrarSesion.setOnClickListener {
            appContainer.authRepository.cerrarSesion()
            findNavController().navigate(R.id.action_global_login)
        }

        BarraCliente.configurar(this, binding.barraCliente, findNavController(), R.id.internProfileFragment)

        viewModel.cargar()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.estado.collect { render(it) }
            }
        }
    }

    private fun render(estado: EstadoCliente) {
        avisoError?.dismiss()
        avisoError = null
        when (estado) {
            EstadoCliente.Cargando -> mostrarPerfil(null, null, cargando = true)
            EstadoCliente.Error -> {
                mostrarPerfil(null, null, cargando = false)
                avisoError = Snackbar.make(binding.root, R.string.cli_error_perfil, Snackbar.LENGTH_INDEFINITE)
                    .setAnchorView(binding.barraCliente)
                    .setAction(R.string.cli_reintentar) { viewModel.cargar() }
                    .also { it.show() }
            }
            is EstadoCliente.Listo -> mostrarPerfil(estado.datos.perfil, estado.datos.rutina, cargando = false)
        }
    }

    private fun mostrarPerfil(perfil: PerfilClienteFila?, rutina: RutinaCliente?, cargando: Boolean) {
        val b = binding
        val noRegistrado = getString(R.string.cli_no_registrado)
        fun valor(texto: String?): String = texto?.takeIf { it.isNotBlank() }?.trim() ?: noRegistrado

        // ---- Encabezado ----
        if (perfil != null) {
            b.tvNombreCliente.text = perfil.nombreCompleto
            b.tvClienteDesde.text = getString(
                R.string.cli_usuario_desde,
                perfil.nombreUsuario,
                SimpleDateFormat("MMM yyyy", localeCo).format(Date(perfil.creadoEn))
            )
            b.tvClienteDesde.visibility = View.VISIBLE
        } else {
            b.tvClienteDesde.visibility = if (cargando) View.INVISIBLE else View.GONE
        }

        // ---- Programa actual (rutina asignada) ----
        mostrarPrograma(rutina, cargando)

        // ---- Datos de la cuenta ----
        b.contenedorDatosCuenta.removeAllViews()
        val filas = listOf(
            R.string.cli_dato_nombre to perfil?.nombreCompleto,
            R.string.cli_dato_usuario to perfil?.nombreUsuario,
            R.string.cli_dato_cedula to perfil?.cedula,
            R.string.cli_dato_correo to perfil?.correo,
            R.string.cli_dato_celular to perfil?.celular,
            R.string.cli_dato_nacimiento to fechaLegible(perfil?.fechaNacimiento),
            R.string.cli_dato_sexo to perfil?.sexo
        )
        filas.forEachIndexed { i, (etiqueta, dato) ->
            if (i > 0) {
                layoutInflater.inflate(R.layout.item_cliente_divisor, b.contenedorDatosCuenta, true)
            }
            val fila = ItemClienteDatoBinding.inflate(layoutInflater, b.contenedorDatosCuenta, true)
            fila.tvEtiqueta.setText(etiqueta)
            fila.tvValor.text = if (cargando) getString(R.string.cli_guion) else valor(dato)
        }

        // ---- Cuerpo y objetivo ----
        val guion = getString(R.string.cli_guion)
        b.tvEdad.text = when {
            cargando -> guion
            else -> ClienteRepository.edad(perfil?.fechaNacimiento)
                ?.let { getString(R.string.cli_edad_anios, it) } ?: noRegistrado
        }
        b.tvEstatura.text = when {
            cargando -> guion
            else -> perfil?.estaturaCm?.let { getString(R.string.cli_estatura_cm, it) } ?: noRegistrado
        }
        // El peso y las medidas se registran en HU-63 (aún no existe esa tabla)
        b.tvPesoActual.text = if (cargando) guion else noRegistrado
        b.tvPesoObjetivo.text = if (cargando) guion else noRegistrado

        val tipo = when (perfil?.tipoRutina) {
            "PERSONALIZADA" -> getString(R.string.cli_tipo_personalizada)
            "PREESTABLECIDA" -> getString(R.string.cli_tipo_preestablecida)
            else -> null
        }
        val chips = listOfNotNull(
            rutina?.datos?.objetivo?.takeIf { it.isNotBlank() },
            rutina?.datos?.nivel?.takeIf { it.isNotBlank() },
            tipo
        )
        listOf(b.tvObjetivo1, b.tvObjetivo2, b.tvObjetivo3).forEachIndexed { i, vista ->
            val texto = chips.getOrNull(i)
            vista.text = texto
            vista.visibility = if (texto == null) View.GONE else View.VISIBLE
        }
        b.grupoObjetivos.visibility = if (chips.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun mostrarPrograma(rutina: RutinaCliente?, cargando: Boolean) {
        val b = binding
        val guion = getString(R.string.cli_guion)
        if (rutina == null) {
            b.tvPrograma.setText(if (cargando) R.string.cli_cargando else R.string.cli_sin_programa)
            b.tvAsignadoPor.visibility = View.GONE
            b.filaDatosPrograma.visibility = View.GONE
            b.tarjetaNotas.visibility = View.GONE
            return
        }

        val desde = SimpleDateFormat("d MMM yyyy", localeCo).format(Date(rutina.datos.fechaInicio))
        b.tvPrograma.text = rutina.datos.nombre
        b.tvAsignadoPor.text = rutina.datos.asignadoPor?.takeIf { it.isNotBlank() }
            ?.let { getString(R.string.cli_asignada_por_desde, it, desde) }
            ?: getString(R.string.cli_asignada_desde, desde)
        b.tvAsignadoPor.visibility = View.VISIBLE

        b.filaDatosPrograma.visibility = View.VISIBLE
        b.tvSesionesSemana.text = rutina.diasConEjercicios.toString()
        b.tvFechaInicio.text = SimpleDateFormat("d MMM", localeCo).format(Date(rutina.datos.fechaInicio))
        b.tvProximoControl.text = guion // no hay registro de controles todavía

        val notas = rutina.datos.notas?.takeIf { it.isNotBlank() }
        b.tarjetaNotas.visibility = if (notas == null) View.GONE else View.VISIBLE
        b.tvNotasDe.setText(R.string.cli_notas_rutina)
        b.tvNotas.text = notas
    }

    /** "1995-04-12" → "12 abr 1995". Si no se puede leer, se devuelve tal cual. */
    private fun fechaLegible(fecha: String?): String? {
        if (fecha.isNullOrBlank()) return null
        return try {
            val f = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(fecha.trim())
            f?.let { SimpleDateFormat("d MMM yyyy", localeCo).format(it) } ?: fecha
        } catch (e: ParseException) {
            fecha
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        avisoError = null
        _binding = null
    }
}
