package com.forcetech.jmfitapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import com.forcetech.jmfitapp.model.AsignacionRutina
import com.forcetech.jmfitapp.model.Rutina
import com.forcetech.jmfitapp.model.RutinaBloque
import com.forcetech.jmfitapp.model.RutinaDia
import com.forcetech.jmfitapp.model.RutinaEjercicio

/**
 * Rutinas y su estructura. Por ahora solo trae las inserciones necesarias para los datos de ejemplo;
 * las consultas del módulo del entrenador (listar, detalle, editar, inhabilitar) se agregan en esa fase.
 */
@Dao
interface RutinaDao {

    @Insert
    suspend fun insertar(rutina: Rutina): Long

    @Insert
    suspend fun insertarDia(dia: RutinaDia): Long

    @Insert
    suspend fun insertarBloque(bloque: RutinaBloque): Long

    @Insert
    suspend fun insertarEjercicio(ejercicio: RutinaEjercicio): Long

    @Insert
    suspend fun insertarAsignacion(asignacion: AsignacionRutina): Long
}
