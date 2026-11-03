package com.forcetech.jmfitapp.ui.store

import android.os.Bundle
import android.view.View
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.forcetech.jmfitapp.JmFitApplication
import com.forcetech.jmfitapp.R
import com.forcetech.jmfitapp.databinding.FragmentCarritoBinding
import com.forcetech.jmfitapp.model.ReglasCarrito
import com.forcetech.jmfitapp.model.RolCodigo
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Carrito de compras de la tienda. Muestra los productos agregados (con cantidades, descuento de socio,
 * envío y total) o, si no hay ninguno, la vista vacía con el botón para explorar el catálogo.
 */
class CarritoFragment : Fragment(R.layout.fragment_carrito) {

    private var _binding: FragmentCarritoBinding? = null
    private val binding get() = _binding!!

    private val appContainer
        get() = (requireActivity().application as JmFitApplication).container

    private val viewModel: CarritoViewModel by viewModels {
        // Solo los clientes internos (socios) con sesión reciben el descuento. El cliente externo no tiene sesión.
        val esSocio = appContainer.sessionManager.sesionActual()?.rolCodigo == RolCodigo.CLIENTE_INTERNO
        CarritoViewModel.Factory(appContainer.carritoRepository, esSocio)
    }

    private lateinit var adapter: CarritoAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCarritoBinding.bind(view)

        respetarBarraDeNavegacion()

        adapter = CarritoAdapter(
            alSumar = { viewModel.sumar(it) },
            alRestar = { viewModel.restar(it) },
            alEliminar = { viewModel.eliminar(it) }
        )
        binding.rvItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvItems.adapter = adapter

        binding.btnVolver.setOnClickListener { findNavController().navigateUp() }
        binding.btnExplorar.setOnClickListener { irAlCatalogo() }
        binding.btnContinuar.setOnClickListener {
            // TODO: aquí se navega al flujo de pago cuando exista esa pantalla.
            crearSnackbar(getString(R.string.carrito_continuar_pendiente), Snackbar.LENGTH_SHORT).show()
        }

        // ===== TEMPORAL (para probar sin catálogo): mantener presionado el título "Carrito" agrega 2 productos. =====
        // BORRAR este bloque y la función agregarProductosDeEjemplo() cuando el catálogo ya agregue productos.
        binding.tvTituloCabecera.setOnLongClickListener {
            agregarProductosDeEjemplo()
            true
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { render(it) } }
                launch { viewModel.eventos.collect { manejar(it) } }
            }
        }
    }

    private fun render(estado: CarritoUiState) {
        // Mientras Room hace la primera lectura no se muestra ninguna de las dos vistas (evita el parpadeo).
        if (estado.cargando) return

        val resumen = estado.resumen
        val vacio = estado.items.isEmpty()

        binding.vistaVacia.isVisible = vacio
        binding.vistaLlena.isVisible = !vacio

        // Contador del ícono del carrito
        binding.tvBadge.isVisible = resumen.unidades > 0
        binding.tvBadge.text = if (resumen.unidades > 99) "99+" else resumen.unidades.toString()

        if (vacio) {
            binding.notaVaciaSocio.isVisible = estado.esSocio
            binding.tvVaciaSocio.text =
                getString(R.string.carrito_vacio_socio, ReglasCarrito.DESCUENTO_SOCIO_PORCENTAJE)
            return
        }

        adapter.submitList(estado.items)

        binding.tvSubtitulo.text =
            resources.getQuantityString(R.plurals.carrito_lleno_subtitulo, resumen.unidades, resumen.unidades)
        binding.tvSubtotalLabel.text =
            resources.getQuantityString(R.plurals.carrito_subtotal, resumen.unidades, resumen.unidades)
        binding.tvSubtotal.text = Moneda.pesos(resumen.subtotal)

        binding.bannerBeneficio.isVisible = estado.esSocio
        binding.filaDescuento.isVisible = estado.esSocio
        if (estado.esSocio) {
            binding.tvBeneficioDetalle.text =
                getString(R.string.carrito_beneficio_detalle, Moneda.pesos(resumen.descuento))
            binding.tvDescuentoLabel.text =
                getString(R.string.carrito_descuento_socio, ReglasCarrito.DESCUENTO_SOCIO_PORCENTAJE)
            binding.tvDescuento.text = "−" + Moneda.pesos(resumen.descuento)
        }

        binding.tvEnvio.text = Moneda.pesos(resumen.envio)
        binding.tvTotal.text = Moneda.pesos(resumen.total)
    }

    private fun manejar(evento: CarritoEvento) {
        when (evento) {
            is CarritoEvento.Eliminado ->
                crearSnackbar(getString(R.string.carrito_eliminado, evento.item.nombre), Snackbar.LENGTH_LONG)
                    .setAction(R.string.carrito_deshacer) { viewModel.restaurar(evento.item) }
                    .show()

            is CarritoEvento.Error ->
                crearSnackbar(getString(evento.mensaje), Snackbar.LENGTH_SHORT).show()
        }
    }

    /** Vuelve al catálogo si ya estaba en la pila; si no, entra a él (también si el carrito se abrió desde otro grafo). */
    private fun irAlCatalogo() {
        val nav = findNavController()
        if (nav.popBackStack(R.id.storeCatalogFragment, false)) return
        val dentroDeLaTienda = nav.currentDestination?.parent?.id == R.id.nav_store
        nav.navigate(if (dentroDeLaTienda) R.id.storeCatalogFragment else R.id.nav_store)
    }

    /** El Snackbar se ancla sobre la barra inferior para no tapar el botón "Continuar compra". */
    private fun crearSnackbar(texto: String, duracion: Int): Snackbar {
        val snackbar = Snackbar.make(binding.root, texto, duracion)
        if (binding.vistaLlena.isVisible) snackbar.setAnchorView(binding.barraInferior)
        return snackbar
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

    // TEMPORAL: borrar junto con el bloque de arriba.
    private fun agregarProductosDeEjemplo() {
        viewLifecycleOwner.lifecycleScope.launch {
            val carrito = appContainer.carritoRepository
            carrito.agregar(1L, "Whey isolate", "900 g · Vainilla", 149_900L)
            carrito.agregar(2L, "Creatina mono", "300 g · Sin sabor", 89_900L, cantidad = 2)
        }
    }

    override fun onDestroyView() {
        binding.rvItems.adapter = null
        _binding = null
        super.onDestroyView()
    }
}