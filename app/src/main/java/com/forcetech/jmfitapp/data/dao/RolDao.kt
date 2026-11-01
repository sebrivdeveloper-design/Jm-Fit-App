package com.forcetech.jmfitapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.forcetech.jmfitapp.model.Rol

@Dao
interface RolDao {

    @Insert
    suspend fun insertarTodos(roles: List<Rol>)

    @Query("SELECT COUNT(*) FROM rol")
    suspend fun contar(): Int

    @Query("SELECT codigo FROM rol WHERE id = :id")
    suspend fun codigoDe(id: Int): String?
}
