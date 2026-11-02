package com.forcetech.jmfitapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.forcetech.jmfitapp.model.AsignacionRutina
import com.forcetech.jmfitapp.model.Rutina
import com.forcetech.jmfitapp.model.RutinaBloque
import com.forcetech.jmfitapp.model.RutinaDia
import com.forcetech.jmfitapp.model.RutinaEjercicio
import kotlinx.coroutines.flow.Flow

/**
 * Rutinas y su estructura. Incluye las consultas del módulo del entrenador
 * (listado, detalle y cambio de estado). Crear y editar se agregan en las siguientes HU.
 */
@Dao
interface RutinaDao {

    // ---------- Inserciones ----------

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

    // ---------- Listado (ENT-6) ----------

    /**
     * Rutinas de un tipo, activas primero y luego las más recientes. Se actualiza solo cuando cambian los datos.
     * [creadoPorId] = null trae las de cualquier autor (se usa para las estándar).
     */
    @Query(
        """
        SELECT r.id AS id, r.nombre AS nombre, r.tipo AS tipo, r.objetivo AS objetivo,
               r.nivel AS nivel, r.estado AS estado,
               (SELECT COUNT(*) FROM rutina_ejercicio re
                  INNER JOIN rutina_bloque b ON b.id = re.rutinaBloqueId
                  INNER JOIN rutina_dia d ON d.id = b.rutinaDiaId
                 WHERE d.rutinaId = r.id) AS totalEjercicios,
               (SELECT u.nombreCompleto FROM asignacion_rutina a
                  INNER JOIN usuario u ON u.id = a.clienteId
                 WHERE a.rutinaId = r.id AND a.estado = 'ACTIVA' LIMIT 1) AS clienteNombre
          FROM rutina r
         WHERE r.tipo = :tipo AND (:creadoPorId IS NULL OR r.creadoPorId = :creadoPorId)
         ORDER BY CASE r.estado WHEN 'ACTIVA' THEN 0 ELSE 1 END, r.actualizadoEn DESC, r.id DESC
        """
    )
    fun observarResumenes(tipo: String, creadoPorId: Long?): Flow<List<RutinaResumen>>

    // ---------- Detalle (ENT-6) ----------

    @Query("SELECT * FROM rutina WHERE id = :id")
    suspend fun obtenerRutina(id: Long): Rutina?

    @Query(
        """
        SELECT u.nombreCompleto FROM asignacion_rutina a
          INNER JOIN usuario u ON u.id = a.clienteId
         WHERE a.rutinaId = :rutinaId AND a.estado = 'ACTIVA' LIMIT 1
        """
    )
    suspend fun clienteActivoDe(rutinaId: Long): String?

    @Query(
        """
        SELECT d.diaSemana AS diaSemana, e.nombre AS ejercicioNombre, re.series AS series,
               re.repeticiones AS repeticiones, re.pesoKg AS pesoKg,
               re.descansoSegundos AS descansoSegundos
          FROM rutina_dia d
          INNER JOIN rutina_bloque b ON b.rutinaDiaId = d.id
          INNER JOIN rutina_ejercicio re ON re.rutinaBloqueId = b.id
          INNER JOIN ejercicio e ON e.id = re.ejercicioId
         WHERE d.rutinaId = :rutinaId
         ORDER BY d.diaSemana, b.orden, re.ordenEnBloque
        """
    )
    suspend fun filasDetalle(rutinaId: Long): List<FilaRutinaDetalle>

    // ---------- Crear y editar (ENT-5 y ENT-8) ----------

    /** Cuántas personalizadas de este entrenador tienen ya ese nombre (sin distinguir mayúsculas). */
    @Query(
        """
        SELECT COUNT(*) FROM rutina
         WHERE tipo = 'PERSONALIZADA' AND creadoPorId = :entrenadorId AND id != :excluirId
           AND lower(trim(nombre)) = lower(trim(:nombre))
        """
    )
    suspend fun contarNombreDuplicado(entrenadorId: Long, nombre: String, excluirId: Long): Int

    @Query(
        """
        UPDATE rutina SET nombre = :nombre, objetivo = :objetivo, nivel = :nivel, notas = :notas,
                          actualizadoEn = :ahora
         WHERE id = :id
        """
    )
    suspend fun actualizarDatos(id: Long, nombre: String, objetivo: String?, nivel: String?, notas: String?, ahora: Long)

    /** Borra los días de la rutina; sus bloques y ejercicios se borran en cascada. */
    @Query("DELETE FROM rutina_dia WHERE rutinaId = :rutinaId")
    suspend fun eliminarDiasDe(rutinaId: Long)

    @Query(
        """
        SELECT d.diaSemana AS diaSemana, re.ejercicioId AS ejercicioId, e.nombre AS ejercicioNombre,
               re.series AS series, re.repeticiones AS repeticiones, re.pesoKg AS pesoKg
          FROM rutina_dia d
          INNER JOIN rutina_bloque b ON b.rutinaDiaId = d.id
          INNER JOIN rutina_ejercicio re ON re.rutinaBloqueId = b.id
          INNER JOIN ejercicio e ON e.id = re.ejercicioId
         WHERE d.rutinaId = :rutinaId
         ORDER BY d.diaSemana, b.orden, re.ordenEnBloque
        """
    )
    suspend fun filasParaEditar(rutinaId: Long): List<FilaEdicion>

    /** Id y nombre del cliente con la asignación ACTIVA de esta rutina (null si no tiene). */
    @Query(
        """
        SELECT u.id AS id, u.nombreCompleto AS nombre FROM asignacion_rutina a
          INNER JOIN usuario u ON u.id = a.clienteId
         WHERE a.rutinaId = :rutinaId AND a.estado = 'ACTIVA' LIMIT 1
        """
    )
    suspend fun clienteActivoOpcionDe(rutinaId: Long): ClienteOpcion?

    /** Nombre de la rutina que el cliente tiene activa ahora (null si no tiene). */
    @Query(
        """
        SELECT r.nombre FROM asignacion_rutina a
          INNER JOIN rutina r ON r.id = a.rutinaId
         WHERE a.clienteId = :clienteId AND a.estado = 'ACTIVA' LIMIT 1
        """
    )
    suspend fun nombreRutinaActivaDe(clienteId: Long): String?

    @Query(
        """
        UPDATE asignacion_rutina SET estado = 'FINALIZADA', fechaFin = :ahora
         WHERE clienteId = :clienteId AND estado = 'ACTIVA'
        """
    )
    suspend fun finalizarAsignacionesActivasDeCliente(clienteId: Long, ahora: Long)

    // ---------- Inhabilitar / reactivar (ENT-7) ----------

    /** Solo cambia rutinas PERSONALIZADAS creadas por ese entrenador. Devuelve cuántas filas cambió (0 o 1). */
    @Query(
        """
        UPDATE rutina SET estado = :estado, actualizadoEn = :ahora
         WHERE id = :id AND creadoPorId = :entrenadorId AND tipo = 'PERSONALIZADA'
        """
    )
    suspend fun cambiarEstadoPropia(id: Long, entrenadorId: Long, estado: String, ahora: Long): Int

    @Query(
        """
        UPDATE asignacion_rutina SET estado = 'FINALIZADA', fechaFin = :ahora
         WHERE rutinaId = :rutinaId AND estado = 'ACTIVA'
        """
    )
    suspend fun finalizarAsignacionesActivas(rutinaId: Long, ahora: Long)
}
