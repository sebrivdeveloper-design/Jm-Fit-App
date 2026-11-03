package com.forcetech.jmfitapp.ui.store

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.forcetech.jmfitapp.R
import com.forcetech.jmfitapp.data.repository.CarritoRepository
import com.forcetech.jmfitapp.model.CalculoCarrito
import com.forcetech.jmfitapp.model.CarritoItem
import com.forcetech.jmfitapp.model.ResumenCarrito
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CarritoUiState(
    /** true solo hasta que Room entrega la primera lectura. */
    val cargando: Boolean = true,
    val items: List<CarritoItem> = emptyList(),
    val resumen: ResumenCarrito = ResumenCarrito.VACIO,
    /** Cliente interno con sesión: recibe el descuento de socio. */
    val esSocio: Boolean = false
)

/** Mensajes de una sola vez (no se repiten al rotar la pantalla). */
sealed interface CarritoEvento {
    data class Eliminado(val item: CarritoItem) : CarritoEvento
    data class Error(@StringRes val mensaje: Int) : CarritoEvento
}

class CarritoViewModel(
    private val repo: CarritoRepository,
    private val esSocio: Boolean
) : ViewModel() {

    val uiState: StateFlow<CarritoUiState> = repo.observarItems()
        .map { items ->
            CarritoUiState(
                cargando = false,
                items = items,
                resumen = CalculoCarrito.resumen(items, esSocio),
                esSocio = esSocio
            )
        }
        .catch { emit(CarritoUiState(cargando = false, esSocio = esSocio)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CarritoUiState(esSocio = esSocio))

    private val _eventos = Channel<CarritoEvento>(Channel.BUFFERED)
    val eventos: Flow<CarritoEvento> = _eventos.receiveAsFlow()

    fun sumar(item: CarritoItem) = ejecutar { repo.ajustarCantidad(item.productoId, +1) }

    fun restar(item: CarritoItem) = ejecutar { repo.ajustarCantidad(item.productoId, -1) }

    fun eliminar(item: CarritoItem) = ejecutar {
        repo.eliminar(item.productoId)
        _eventos.send(CarritoEvento.Eliminado(item))
    }

    /** "Deshacer" tras eliminar: devuelve el producto tal como estaba. */
    fun restaurar(item: CarritoItem) = ejecutar { repo.restaurar(item) }

    private fun ejecutar(bloque: suspend () -> Unit) = viewModelScope.launch {
        try {
            bloque()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _eventos.send(CarritoEvento.Error(R.string.carrito_error))
        }
    }

    class Factory(
        private val repo: CarritoRepository,
        private val esSocio: Boolean
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CarritoViewModel(repo, esSocio) as T
    }
}