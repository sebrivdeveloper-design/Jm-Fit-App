package com.forcetech.jmfitapp.ui.store

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.forcetech.jmfitapp.JmFitApplication
import com.forcetech.jmfitapp.R
import com.forcetech.jmfitapp.databinding.FragmentDetalleProductoBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Vista 2 de la tienda: detalle de un producto. Recibe el argumento [ARG_PRODUCTO_ID]
 * (definido en nav_store.xml), permite elegir la cantidad y agregarla al carrito.
 */
class DetalleProductoFragment : Fragment(R.layout.fragment_detalle_producto) {

    private var _binding: FragmentDetalleProductoBinding? = null
    private val binding get() = _binding!!

    private val appContainer
        get() = (requireActivity().application as JmFitApplication).container

    private val viewModel: DetalleProductoViewModel by viewModels {
        DetalleProductoViewModel.Factory(
            appContainer.catalogoRepository,
            appContainer.carritoRepository,
            requireArguments().getInt(ARG_PRODUCTO_ID)
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDetalleProductoBinding.bind(view)

        respetarBarraDeNavegacion()

        binding.btnAtras.setOnClickListener { findNavController().navigateUp() }
        binding.btnCarrito.setOnClickListener { irAlCarrito() }
        binding.btnMenos.setOnClickListener { viewModel.disminuir() }
        binding.btnMas.setOnClickListener { viewModel.aumentar() }
        binding.btnAgregar.setOnClickListener { viewModel.agregarAlCarrito() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { mostrar(it) } }
                launch { viewModel.unidadesCarrito.collect { mostrarContador(it) } }
                launch { viewModel.eventos.collect { manejar(it) } }
            }
        }
    }

    private fun mostrar(estado: DetalleUiState) {
        binding.progreso.isVisible = estado.cargando
        binding.tvNoEncontrado.isVisible = estado.noEncontrado

        val producto = estado.producto
        binding.contenido.isVisible = producto != null
        binding.barraInferior.isVisible = producto != null
        if (producto == null) return

        val contexto = requireContext()

        binding.ivProducto.setImageResource(TiendaUi.recursoImagen(contexto, producto.imagen))

        val marca = producto.marca.uppercase(TiendaUi.LOCALE_CO)
        val categoria = producto.categoriaNombre
        binding.tvMarcaCategoria.text =
            if (categoria.isNullOrBlank()) marca
            else getString(R.string.tienda_marca_categoria, marca, categoria.uppercase(TiendaUi.LOCALE_CO))

        binding.tvCalificacion.isVisible = producto.calificacion > 0
        binding.tvCalificacion.text =
            getString(R.string.tienda_calificacion, TiendaUi.calificacion(producto.calificacion))

        binding.tvNombre.text = producto.nombre
        binding.tvPresentacion.text = producto.presentacion
        binding.tvPrecio.text = Moneda.pesos(producto.precioPesos)

        binding.tvDisponibilidad.setText(
            if (producto.disponible) R.string.tienda_estado_disponible else R.string.tienda_estado_agotado
        )
        binding.tvDisponibilidad.setTextColor(
            ContextCompat.getColor(
                contexto,
                if (producto.disponible) R.color.jmfit_success else R.color.jmfit_error
            )
        )

        binding.tvDescripcion.isVisible = producto.descripcion.isNotBlank()
        binding.tvDescripcion.text = producto.descripcion

        binding.tvObjetivo.isVisible = producto.objetivo.isNotBlank()
        binding.tvObjetivo.text = getString(R.string.tienda_objetivo, producto.objetivo)

        val beneficios = producto.beneficios.lines()
            .filter { it.isNotBlank() }
            .joinToString("\n") { "•  ${it.trim()}" }
        binding.tvBeneficiosTitulo.isVisible = beneficios.isNotEmpty()
        binding.tvBeneficios.isVisible = beneficios.isNotEmpty()
        binding.tvBeneficios.text = beneficios

        // Cantidad y total
        binding.tvCantidad.text = estado.cantidad.toString()
        binding.tvTotal.text = Moneda.pesos(estado.total)
        binding.btnMenos.isEnabled = producto.disponible && estado.cantidad > 1
        binding.btnMas.isEnabled = producto.disponible && estado.cantidad < DetalleProductoViewModel.MAX_CANTIDAD
        binding.btnAgregar.isEnabled = producto.disponible
        binding.btnAgregar.setText(if (producto.disponible) R.string.tienda_agregar else R.string.tienda_agotado)
    }

    private fun mostrarContador(unidades: Int) {
        binding.tvBadge.isVisible = unidades > 0
        binding.tvBadge.text = if (unidades > 99) "99+" else unidades.toString()
    }

    private fun manejar(evento: TiendaEvento) {
        val snackbar = when (evento) {
            is TiendaEvento.ProductoAgregado ->
                Snackbar.make(
                    binding.root,
                    getString(R.string.tienda_agregado, evento.nombre),
                    Snackbar.LENGTH_SHORT
                ).setAction(R.string.tienda_ver_carrito) { irAlCarrito() }

            TiendaEvento.ErrorAlAgregar ->
                Snackbar.make(binding.root, R.string.tienda_error_agregar, Snackbar.LENGTH_SHORT)
        }
        // Se ancla sobre la barra inferior para no tapar el botón "Agregar al carrito".
        if (binding.barraInferior.isVisible) snackbar.setAnchorView(binding.barraInferior)
        snackbar.show()
    }

    /** carritoFragment está en el mismo subgrafo (nav_store.xml). */
    private fun irAlCarrito() {
        findNavController().navigate(R.id.carritoFragment)
    }

    /** La Activity no aplica el margen de la barra de navegación del sistema, así que lo agregamos a la barra inferior. */
    private fun respetarBarraDeNavegacion() {
        val barra = binding.barraInferior
        val paddingBase = barra.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(barra) { v, insets ->
            val navegacion = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.updatePadding(bottom = paddingBase + navegacion.bottom)
            insets
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        /** Debe coincidir con <argument android:name="productoId"> de nav_store.xml. */
        const val ARG_PRODUCTO_ID = "productoId"
    }
}
