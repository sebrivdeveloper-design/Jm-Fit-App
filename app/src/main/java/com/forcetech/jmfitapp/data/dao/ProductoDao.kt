package com.forcetech.jmfitapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.forcetech.jmfitapp.model.Producto

@Dao
interface ProductoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarProductos(producto: Producto): Long

    @Update
    suspend fun updateProductos(producto: Producto): Int

    @Query("""
        UPDATE productos
        SET activo = :activo
        WHERE id = :idProducto
    """)
    suspend fun cambiarEstado(
        idProducto: Int,
        activo: Boolean
    ): Int

    @Query("UPDATE productos SET activo = 1 WHERE id = :idProducto")
    suspend fun activarProductos(idProducto: Int): Int

    @Query("""
        SELECT * FROM productos
        WHERE (:estado = 0
            OR (:estado = 1 AND activo = 1)
            OR (:estado = 2 AND activo = 0))
        AND (
            :busqueda = ''
            OR nombre LIKE '%' || :busqueda || '%' COLLATE NOCASE
            OR marca LIKE '%' || :busqueda || '%' COLLATE NOCASE
        )
        ORDER BY nombre COLLATE NOCASE ASC
    """)
    suspend fun obtenerProductosFiltrados(
        busqueda: String,
        estado: Int
    ): List<Producto>

    @Query("SELECT * FROM productos WHERE id = :idProducto LIMIT 1")
    suspend fun obtenerProductoPorId(idProducto: Int): Producto?
}