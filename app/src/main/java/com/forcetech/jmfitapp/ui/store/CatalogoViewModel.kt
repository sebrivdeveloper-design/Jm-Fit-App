package com.forcetech.jmfitapp.ui.store

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.forcetech.jmfitapp.data.repository.CarritoRepository
import com.forcetech.jmfitapp.data.repository.CatalogoRepository
import com.forcetech.jmfitapp.model.CategoriaProducto
import com.forcetech.jmfitapp.model.ProductoResumen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CatalogoUiState(
    val cargando: Boolean = true,
    val categorias: List<CategoriaProducto> = emptyList(),
    /** null = chip "Todos". */
    val categoriaId: Int? = null,
    val texto: String = "",
    val productos: List<ProductoResumen> = emptyList(),
    val error: Boolean = false
)

class CatalogoViewModel(
    private val catalogo: CatalogoRepository,
    private val carrito: CarritoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CatalogoUiState())
    val uiState: StateFlow<CatalogoUiState> = _uiState.asStateFlow()

    /** Unidades en el carrito, para el contador del ícono (se actualiza sola cuando cambia el carrito). */
    val unidadesCarrito: StateFlow<Int> = carrito.observarItems()
        .map { items -> items.sumOf { it.cantidad } }
        .catch { emit(0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _eventos = Channel<TiendaEvento>(Channel.BUFFERED)
    val eventos: Flow<TiendaEvento> = _eventos.receiveAsFlow()

    private var consultaJob: Job? = null

    init {
        cargarCategorias()
        consultar()
    }

    fun seleccionarCategoria(categoriaId: Int?) {
        if (categoriaId == _uiState.value.categoriaId) return
        _uiState.update { it.copy(categoriaId = categoriaId) }
        consultar()
    }

    /** Espera un momento después de la última tecla para no consultar en cada letra. */
    fun buscar(texto: String) {
        if (texto == _uiState.value.texto) return
        _uiState.update { it.copy(texto = texto) }
        consultar(retrasoMs = RETRASO_BUSQUEDA_MS)
    }

    /** Botón "Agregar al carrito" de una tarjeta: suma 1 unidad. */
    fun agregarAlCarrito(producto: ProductoResumen) {
        viewModelScope.launch {
            try {
                carrito.agregar(
                    productoId = producto.id.toLong(),
                    nombre = producto.nombre,
                    presentacion = producto.presentacion,
                    precioUnitario = producto.precioPesos,
                    imagen = producto.imagen
                )
                _eventos.send(TiendaEvento.ProductoAgregado(producto.nombre))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo agregar al carrito", e)
                _eventos.send(TiendaEvento.ErrorAlAgregar)
            }
        }
    }

    private fun cargarCategorias() {
        viewModelScope.launch {
            try {
                val categorias = catalogo.categorias()
                _uiState.update { it.copy(categorias = categorias) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "No se pudieron cargar las categorías", e)
                _uiState.update { it.copy(error = true, cargando = false) }
            }
        }
    }

    private fun consultar(retrasoMs: Long = 0) {
        consultaJob?.cancel()
        consultaJob = viewModelScope.launch {
            if (retrasoMs > 0) delay(retrasoMs)
            _uiState.update { it.copy(cargando = true, error = false) }
            try {
                val filtros = _uiState.value
                val productos = catalogo.productos(filtros.categoriaId, filtros.texto)
                _uiState.update { it.copy(cargando = false, productos = productos) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "No se pudieron cargar los productos", e)
                _uiState.update { it.copy(cargando = false, productos = emptyList(), error = true) }
            }
        }
    }

    class Factory(
        private val catalogo: CatalogoRepository,
        private val carrito: CarritoRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = CatalogoViewModel(catalogo, carrito) as T
    }

    private companion object {
        const val TAG = "CatalogoViewModel"
        const val RETRASO_BUSQUEDA_MS = 300L
    }
}
