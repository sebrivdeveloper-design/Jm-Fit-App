package com.forcetech.jmfitapp.data.repository

import com.forcetech.jmfitapp.data.dao.CatalogoDao
import com.forcetech.jmfitapp.model.CategoriaProducto
import com.forcetech.jmfitapp.model.ProductoDetalle
import com.forcetech.jmfitapp.model.ProductoResumen

/** Lectura del catálogo de la tienda. Agregar al carrito se hace con CarritoRepository. */
class CatalogoRepository(
    private val catalogoDao: CatalogoDao,
    /** Espera a que los datos de ejemplo estén cargados (solo importa en el primer arranque). */
    private val esperarBaseLista: suspend () -> Unit
) {

    suspend fun categorias(): List<CategoriaProducto> {
        esperarBaseLista()
        return catalogoDao.categoriasActivas()
    }

    /** [categoriaId] null = todas. [texto] se busca en nombre o marca. */
    suspend fun productos(categoriaId: Int?, texto: String): List<ProductoResumen> {
        esperarBaseLista()
        return catalogoDao.buscarProductos(categoriaId, escaparLike(texto.trim()))
    }

    suspend fun detalle(id: Int): ProductoDetalle? {
        esperarBaseLista()
        return catalogoDao.detalleDe(id)
    }

    /** Para que "%" y "_" que escriba la persona se busquen como texto normal. */
    private fun escaparLike(texto: String): String =
        texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}
