package com.forcetech.jmfitapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.forcetech.jmfitapp.model.CategoriaProducto
import com.forcetech.jmfitapp.model.ProductoDetalle
import com.forcetech.jmfitapp.model.ProductoResumen
import com.forcetech.jmfitapp.model.ProductoTienda

/**
 * Consultas de LECTURA de la tienda sobre la tabla "productos" (ProductoDao) más sus datos de tienda.
 * Solo se muestran productos con activo = 1.
 */
@Dao
interface CatalogoDao {

    @Insert
    suspend fun insertarCategorias(categorias: List<CategoriaProducto>)

    @Insert
    suspend fun insertarDetalle(detalle: ProductoTienda)

    @Query("SELECT COUNT(*) FROM categoria_producto")
    suspend fun contarCategorias(): Int

    @Query("SELECT * FROM categoria_producto WHERE estado = 'ACTIVO' ORDER BY id")
    suspend fun categoriasActivas(): List<CategoriaProducto>

    /**
     * Catálogo con filtros opcionales.
     * - [categoriaId] null → todas las categorías (incluye productos sin categoría).
     * - [texto] vacío → sin búsqueda; con texto, busca en nombre o marca.
     *   (LIKE ignora mayúsculas solo en letras sin tilde; el repositorio escapa % y _.)
     */
    @Query(
        """
        SELECT p.id AS id, p.nombre AS nombre, p.presentacion AS presentacion, p.precio AS precio,
               t.imagen AS imagen, COALESCE(t.calificacion, 0) AS calificacion,
               COALESCE(t.disponible, 1) AS disponible, c.nombre AS categoriaNombre
        FROM productos p
        LEFT JOIN producto_tienda t ON t.productoId = p.id
        LEFT JOIN categoria_producto c ON c.id = t.categoriaId AND c.estado = 'ACTIVO'
        WHERE p.activo = 1
          AND (:categoriaId IS NULL OR t.categoriaId = :categoriaId)
          AND (:texto = '' OR p.nombre LIKE '%' || :texto || '%' ESCAPE '\'
                           OR p.marca LIKE '%' || :texto || '%' ESCAPE '\')
        ORDER BY p.nombre COLLATE NOCASE ASC
        """
    )
    suspend fun buscarProductos(categoriaId: Int?, texto: String): List<ProductoResumen>

    @Query(
        """
        SELECT p.id AS id, p.nombre AS nombre, p.marca AS marca, p.presentacion AS presentacion,
               p.precio AS precio, p.objetivo AS objetivo,
               COALESCE(t.descripcion, '') AS descripcion, COALESCE(t.beneficios, '') AS beneficios,
               t.imagen AS imagen, COALESCE(t.calificacion, 0) AS calificacion,
               COALESCE(t.disponible, 1) AS disponible, c.nombre AS categoriaNombre
        FROM productos p
        LEFT JOIN producto_tienda t ON t.productoId = p.id
        LEFT JOIN categoria_producto c ON c.id = t.categoriaId AND c.estado = 'ACTIVO'
        WHERE p.id = :id AND p.activo = 1
        """
    )
    suspend fun detalleDe(id: Int): ProductoDetalle?
}
