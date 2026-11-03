package com.forcetech.jmfitapp.data.repository

import androidx.room.withTransaction
import com.forcetech.jmfitapp.data.database.AppDatabase
import com.forcetech.jmfitapp.model.CarritoItem
import com.forcetech.jmfitapp.model.ReglasCarrito
import kotlinx.coroutines.flow.Flow

/**
 * Carrito de compras de la tienda.
 *
 * Desde el CATÁLOGO se usa así:
 *
 *     val carrito = (requireActivity().application as JmFitApplication).container.carritoRepository
 *     viewLifecycleOwner.lifecycleScope.launch {
 *         carrito.agregar(
 *             productoId = producto.id,
 *             nombre = producto.nombre,
 *             presentacion = "900 g · Vainilla",
 *             precioUnitario = producto.precio,
 *             imagen = "prod_whey_isolate" // opcional: nombre de un drawable
 *         )
 *     }
 */
class CarritoRepository(private val db: AppDatabase) {

    private val dao = db.carritoDao()

    /** Lista del carrito en vivo, en el orden en que se agregaron los productos. */
    fun observarItems(): Flow<List<CarritoItem>> = dao.observarItems()

    /** Agrega el producto; si ya estaba en el carrito, le suma [cantidad] unidades. */
    suspend fun agregar(
        productoId: Long,
        nombre: String,
        presentacion: String,
        precioUnitario: Long,
        imagen: String? = null,
        cantidad: Int = 1
    ) {
        require(cantidad >= 1) { "La cantidad debe ser al menos 1" }
        db.withTransaction {
            if (dao.buscar(productoId) == null) {
                dao.guardar(
                    CarritoItem(
                        productoId = productoId,
                        nombre = nombre,
                        presentacion = presentacion,
                        precioUnitario = precioUnitario,
                        imagen = imagen,
                        cantidad = cantidad.coerceAtMost(ReglasCarrito.CANTIDAD_MAXIMA)
                    )
                )
            } else {
                dao.ajustarCantidad(productoId, cantidad, ReglasCarrito.CANTIDAD_MAXIMA)
            }
        }
    }

    /** [delta] = +1 para sumar una unidad, -1 para restar. Nunca baja de 1 ni pasa del máximo. */
    suspend fun ajustarCantidad(productoId: Long, delta: Int) =
        dao.ajustarCantidad(productoId, delta, ReglasCarrito.CANTIDAD_MAXIMA)

    suspend fun eliminar(productoId: Long) = dao.eliminar(productoId)

    /** Vuelve a poner un producto tal como estaba (usado por "Deshacer"). */
    suspend fun restaurar(item: CarritoItem) = dao.guardar(item)

    /** Vacía todo el carrito (por ejemplo, cuando se complete la compra). */
    suspend fun vaciar() = dao.vaciar()
}