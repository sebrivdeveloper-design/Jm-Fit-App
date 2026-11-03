package com.forcetech.jmfitapp.ui.store

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.forcetech.jmfitapp.data.repository.CarritoRepository
import com.forcetech.jmfitapp.data.repository.CatalogoRepository
import com.forcetech.jmfitapp.model.ProductoDetalle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
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

data class DetalleUiState(
    val cargando: Boolean = true,
    val producto: ProductoDetalle? = null,
    val noEncontrado: Boolean = false,
    val cantidad: Int = 1
) {
    /** Precio por la cantidad elegida (COP). */
    val total: Long get() = (producto?.precioPesos ?: 0L) * cantidad
}

class DetalleProductoViewModel(
    private val catalogo: CatalogoRepository,
    private val carrito: CarritoRepository,
    private val productoId: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetalleUiState())
    val uiState: StateFlow<DetalleUiState> = _uiState.asStateFlow()

    /** Unidades en el carrito, para el contador del ícono. */
    val unidadesCarrito: StateFlow<Int> = carrito.observarItems()
        .map { items -> items.sumOf { it.cantidad } }
        .catch { emit(0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _eventos = Channel<TiendaEvento>(Channel.BUFFERED)
    val eventos: Flow<TiendaEvento> = _eventos.receiveAsFlow()

    init {
        cargar()
    }

    fun aumentar() {
        _uiState.update { if (it.cantidad < MAX_CANTIDAD) it.copy(cantidad = it.cantidad + 1) else it }
    }

    fun disminuir() {
        _uiState.update { if (it.cantidad > 1) it.copy(cantidad = it.cantidad - 1) else it }
    }

    /** Agrega al carrito la cantidad elegida (si el producto ya estaba, se suma). */
    fun agregarAlCarrito() {
        val estado = _uiState.value
        val producto = estado.producto ?: return
        if (!producto.disponible) return

        viewModelScope.launch {
            try {
                carrito.agregar(
                    productoId = producto.id.toLong(),
                    nombre = producto.nombre,
                    presentacion = producto.presentacion,
                    precioUnitario = producto.precioPesos,
                    imagen = producto.imagen,
                    cantidad = estado.cantidad
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

    private fun cargar() {
        viewModelScope.launch {
            try {
                val producto = catalogo.detalle(productoId)
                _uiState.update { it.copy(cargando = false, producto = producto, noEncontrado = producto == null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo cargar el producto $productoId", e)
                _uiState.update { it.copy(cargando = false, producto = null, noEncontrado = true) }
            }
        }
    }

    class Factory(
        private val catalogo: CatalogoRepository,
        private val carrito: CarritoRepository,
        private val productoId: Int
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DetalleProductoViewModel(catalogo, carrito, productoId) as T
    }

    companion object {
        private const val TAG = "DetalleProductoVM"

        /** Máximo de unidades que se pueden elegir de una vez (el carrito admite hasta 99 en total). */
        const val MAX_CANTIDAD = 10
    }
}
