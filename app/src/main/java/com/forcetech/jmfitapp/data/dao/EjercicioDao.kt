package com.forcetech.jmfitapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import com.forcetech.jmfitapp.model.Ejercicio
import com.forcetech.jmfitapp.model.GrupoMuscular

/** Catálogo de ejercicios y grupos musculares. */
@Dao
interface EjercicioDao {

    @Insert
    suspend fun insertarGrupos(grupos: List<GrupoMuscular>)

    @Insert
    suspend fun insertar(ejercicio: Ejercicio): Long
}
