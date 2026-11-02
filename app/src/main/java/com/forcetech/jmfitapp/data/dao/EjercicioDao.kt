package com.forcetech.jmfitapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.forcetech.jmfitapp.model.Ejercicio
import com.forcetech.jmfitapp.model.GrupoMuscular

/** Catálogo de ejercicios y grupos musculares. */
@Dao
interface EjercicioDao {

    @Insert
    suspend fun insertarGrupos(grupos: List<GrupoMuscular>)

    @Insert
    suspend fun insertar(ejercicio: Ejercicio): Long

    /** Ejercicios activos del catálogo, en orden alfabético. */
    @Query("SELECT id AS id, nombre AS nombre FROM ejercicio WHERE estado = 'ACTIVO' ORDER BY nombre")
    suspend fun activos(): List<EjercicioOpcion>
}
