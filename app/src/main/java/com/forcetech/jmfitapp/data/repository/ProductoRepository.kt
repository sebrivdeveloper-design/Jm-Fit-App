package com.forcetech.jmfitapp.data.repository

import com.forcetech.jmfitapp.data.dao.ProductoDao
import com.forcetech.jmfitapp.model.Producto

/** Operaciones de gestión administrativa. Los errores se comunican a la pantalla llamadora. */
class ProductoRepository(private val dao: ProductoDao) {
    suspend fun listar(busqueda: String, estado: Int): List<Producto> =
        dao.obtenerProductosFiltrados(busqueda.trim(), estado)

    suspend fun guardar(producto: Producto, editando: Boolean): Boolean {
        return if (editando) {
            dao.updateProductos(producto) > 0
        } else {
            dao.guardarProductos(producto)
            true
        }
    }

    suspend fun cambiarEstado(producto: Producto): Boolean =
        dao.cambiarEstado(producto.id, !producto.activo) > 0
}
