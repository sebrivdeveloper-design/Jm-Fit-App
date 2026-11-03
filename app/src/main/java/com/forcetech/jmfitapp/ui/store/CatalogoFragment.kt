package com.forcetech.jmfitapp.ui.store

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.forcetech.jmfitapp.JmFitApplication
import com.forcetech.jmfitapp.R
import com.forcetech.jmfitapp.databinding.FragmentCatalogoBinding
import com.forcetech.jmfitapp.model.ProductoResumen
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Vista 1 de la tienda: catálogo de suplementos con buscador, chips de categoría y acceso al carrito.
 * El cliente externo entra sin sesión desde "Ver catálogo de suplementos" en el login.
 */
class CatalogoFragment : Fragment(R.layout.fragment_catalogo) {

    private var _binding: FragmentCatalogoBinding? = null
    private val binding get() = _binding!!

    private val appContainer
        get() = (requireActivity().application as JmFitApplication).container

    private val viewModel: CatalogoViewModel by viewModels {
        CatalogoViewModel.Factory(appContainer.catalogoRepository, appContainer.carritoRepository)
    }

    private val adapter = ProductoTiendaAdapter(
        onProductoClick = { abrirDetalle(it) },
        onAgregarClick = { viewModel.agregarAlCarrito(it) }
    )

    /** Los chips se crean una vez por vista, cuando llegan las categorías. */
    private var chipsConstruidos = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCatalogoBinding.bind(view)
        chipsConstruidos = false

        respetarBarraDeNavegacion()

        binding.rvProductos.adapter = adapter
        binding.btnCarrito.setOnClickListener { irAlCarrito() }

        binding.etBuscar.doOnTextChanged { texto, _, _, _ ->
            viewModel.buscar(texto?.toString().orEmpty())
        }
        binding.etBuscar.setOnEditorActionListener { campo, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                requireContext().getSystemService(InputMethodManager::class.java)
                    ?.hideSoftInputFromWindow(campo.windowToken, 0)
                campo.clearFocus()
                true
            } else {
                false
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { mostrar(it) } }
                launch { viewModel.unidadesCarrito.collect { mostrarContador(it) } }
                launch { viewModel.eventos.collect { manejar(it) } }
            }
        }
    }

    private fun mostrar(estado: CatalogoUiState) {
        construirChips(estado)
        adapter.submitList(estado.productos)

        binding.progreso.isVisible = estado.cargando && estado.productos.isEmpty()
        val sinResultados = !estado.cargando && estado.productos.isEmpty()
        binding.tvVacio.isVisible = sinResultados
        binding.tvVacio.setText(if (estado.error) R.string.tienda_error else R.string.tienda_vacio)
    }

    private fun mostrarContador(unidades: Int) {
        binding.tvBadge.isVisible = unidades > 0
        binding.tvBadge.text = if (unidades > 99) "99+" else unidades.toString()
    }

    private fun manejar(evento: TiendaEvento) {
        when (evento) {
            is TiendaEvento.ProductoAgregado ->
                Snackbar.make(
                    binding.root,
                    getString(R.string.tienda_agregado, evento.nombre),
                    Snackbar.LENGTH_SHORT
                ).setAction(R.string.tienda_ver_carrito) { irAlCarrito() }.show()

            TiendaEvento.ErrorAlAgregar ->
                Snackbar.make(binding.root, R.string.tienda_error_agregar, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun construirChips(estado: CatalogoUiState) {
        if (chipsConstruidos || estado.categorias.isEmpty()) return
        chipsConstruidos = true

        val grupo = binding.chipsCategorias
        agregarChip(grupo, getString(R.string.tienda_chip_todos), null, estado.categoriaId == null)
        estado.categorias.forEach { categoria ->
            agregarChip(grupo, categoria.nombre, categoria.id, estado.categoriaId == categoria.id)
        }

        // Se asigna al final para que marcar los chips iniciales no dispare consultas.
        grupo.setOnCheckedStateChangeListener { g, idsMarcados ->
            val chip = idsMarcados.firstOrNull()?.let { g.findViewById<Chip>(it) }
            viewModel.seleccionarCategoria(chip?.tag as? Int)
        }
    }

    private fun agregarChip(grupo: ChipGroup, texto: String, categoriaId: Int?, marcado: Boolean) {
        val chip = layoutInflater.inflate(R.layout.item_chip_categoria, grupo, false) as Chip
        chip.id = View.generateViewId()
        chip.text = texto
        chip.tag = categoriaId
        chip.isChecked = marcado
        grupo.addView(chip)
    }

    private fun abrirDetalle(producto: ProductoResumen) {
        findNavController().navigate(
            R.id.action_catalogo_to_detalle,
            bundleOf(DetalleProductoFragment.ARG_PRODUCTO_ID to producto.id)
        )
    }

    /** carritoFragment está en el mismo subgrafo (nav_store.xml). */
    private fun irAlCarrito() {
        findNavController().navigate(R.id.carritoFragment)
    }

    /** La Activity no aplica el margen de la barra de navegación del sistema, así que lo agregamos a la lista. */
    private fun respetarBarraDeNavegacion() {
        val lista = binding.rvProductos
        val paddingBase = lista.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(lista) { v, insets ->
            val navegacion = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.updatePadding(bottom = paddingBase + navegacion.bottom)
            insets
        }
    }

    override fun onDestroyView() {
        binding.rvProductos.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
