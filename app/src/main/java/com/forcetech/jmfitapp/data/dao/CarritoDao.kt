package com.forcetech.jmfitapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.forcetech.jmfitapp.model.CarritoItem
import kotlinx.coroutines.flow.Flow

@Dao
interface CarritoDao {

    /** Se vuelve a emitir sola cada vez que cambia el carrito (agregar, sumar, restar, eliminar). */
    @Query("SELECT * FROM carrito_item ORDER BY agregadoEn ASC")
    fun observarItems(): Flow<List<CarritoItem>>

    @Query("SELECT * FROM carrito_item WHERE productoId = :productoId")
    suspend fun buscar(productoId: Long): CarritoItem?

    /** Inserta o reemplaza (también sirve para "Deshacer" una eliminación). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(item: CarritoItem)

    /** Suma o resta [delta] a la cantidad, siempre entre 1 y [maximo]. Es atómico, no depende de datos viejos. */
    @Query("UPDATE carrito_item SET cantidad = MAX(1, MIN(:maximo, cantidad + :delta)) WHERE productoId = :productoId")
    suspend fun ajustarCantidad(productoId: Long, delta: Int, maximo: Int)

    @Query("DELETE FROM carrito_item WHERE productoId = :productoId")
    suspend fun eliminar(productoId: Long)

    @Query("DELETE FROM carrito_item")
    suspend fun vaciar()
}