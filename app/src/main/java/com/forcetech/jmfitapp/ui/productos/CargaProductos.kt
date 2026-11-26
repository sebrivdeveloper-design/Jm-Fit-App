package com.forcetech.jmfitapp.ui.productos

import com.forcetech.jmfitapp.data.repository.ProductoRepository
import com.forcetech.jmfitapp.model.Producto
import kotlinx.coroutines.CancellationException

/** Una consulta reemplazada o cancelada por el ciclo de vida no es un error de carga. */
class CargaProductos(private val repository: ProductoRepository) {
    suspend fun cargar(
        busqueda: String,
        estado: Int,
        onProductos: (List<Producto>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            onProductos(repository.listar(busqueda, estado))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            onError(e)
        }
    }
}
