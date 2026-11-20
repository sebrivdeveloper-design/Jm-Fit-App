package com.forcetech.jmfitapp.data.dao

import androidx.room.Dao
import androidx.room.Query

/**
 * Consultas de SOLO LECTURA para las pantallas del cliente interno (HOME, WORKOUT, MY PROFILE)
 * y para "Mis clientes" del entrenador. No agrega tablas ni columnas: no cambia el esquema de la base.
 */
@Dao
interface ClienteDao {

    /** Datos de la cuenta y de la ficha del cliente (HU-33). Null si el usuario no existe. */
    @Query(
        """
        SELECT u.id AS id, u.nombreUsuario AS nombreUsuario, u.nombreCompleto AS nombreCompleto,
               u.cedula AS cedula, u.correo AS correo, u.celular AS celular, u.creadoEn AS creadoEn,
               c.fechaNacimiento AS fechaNacimiento, c.sexo AS sexo, c.estaturaCm AS estaturaCm,
               c.tipoRutina AS tipoRutina
          FROM usuario u
          LEFT JOIN cliente_interno c ON c.usuarioId = u.id
         WHERE u.id = :usuarioId
        """
    )
    suspend fun perfil(usuarioId: Long): PerfilClienteFila?

    /** Rutina ACTIVA asignada al cliente (HU-27B). Null si no tiene. */
    @Query(
        """
        SELECT r.id AS rutinaId, r.nombre AS nombre, r.objetivo AS objetivo, r.nivel AS nivel,
               r.notas AS notas, a.fechaInicio AS fechaInicio, u.nombreCompleto AS asignadoPor
          FROM asignacion_rutina a
          INNER JOIN rutina r ON r.id = a.rutinaId
          LEFT JOIN usuario u ON u.id = a.asignadoPorId
         WHERE a.clienteId = :clienteId AND a.estado = 'ACTIVA' AND r.estado = 'ACTIVA'
         ORDER BY a.asignadoEn DESC
         LIMIT 1
        """
    )
    suspend fun rutinaAsignada(clienteId: Long): RutinaAsignadaFila?

    /** Ejercicios de la rutina en el orden definido (día, bloque y posición en el bloque). */
    @Query(
        """
        SELECT d.diaSemana AS diaSemana, e.nombre AS nombre, b.tipoAgrupacion AS tipoAgrupacion,
               re.series AS series, re.repeticiones AS repeticiones, re.pesoKg AS pesoKg,
               re.descansoSegundos AS descansoSegundos, re.tecnica AS tecnica,
               re.observaciones AS observaciones
          FROM rutina_dia d
          INNER JOIN rutina_bloque b ON b.rutinaDiaId = d.id
          INNER JOIN rutina_ejercicio re ON re.rutinaBloqueId = b.id
          INNER JOIN ejercicio e ON e.id = re.ejercicioId
         WHERE d.rutinaId = :rutinaId
         ORDER BY d.diaSemana, b.orden, re.ordenEnBloque
        """
    )
    suspend fun ejerciciosDeRutina(rutinaId: Long): List<EjercicioClienteFila>

    /** Entrenador con asignación vigente y cuenta activa (HU-36). Null si no tiene. */
    @Query(
        """
        SELECT u.nombreCompleto AS nombre, u.celular AS celular, e.especialidad AS especialidad
          FROM asignacion_entrenador ae
          INNER JOIN usuario u ON u.id = ae.entrenadorId
          LEFT JOIN entrenador e ON e.usuarioId = ae.entrenadorId
         WHERE ae.clienteId = :clienteId AND ae.fechaFin IS NULL AND u.estado = 'ACTIVO'
         ORDER BY ae.fechaInicio DESC
         LIMIT 1
        """
    )
    suspend fun entrenadorAsignado(clienteId: Long): EntrenadorContactoFila?

    /**
     * Clientes con asignación vigente a este entrenador (HU-51), en orden alfabético, con su
     * rutina activa. Incluye los inhabilitados para que el entrenador vea su estado.
     */
    @Query(
        """
        SELECT u.id AS id, u.nombreCompleto AS nombre, u.nombreUsuario AS nombreUsuario,
               u.estado AS estado,
               (SELECT r.nombre FROM asignacion_rutina a
                  INNER JOIN rutina r ON r.id = a.rutinaId
                 WHERE a.clienteId = u.id AND a.estado = 'ACTIVA' AND r.estado = 'ACTIVA'
                 ORDER BY a.asignadoEn DESC LIMIT 1) AS rutinaActiva
          FROM asignacion_entrenador ae
          INNER JOIN usuario u ON u.id = ae.clienteId
         WHERE ae.entrenadorId = :entrenadorId AND ae.fechaFin IS NULL
         ORDER BY u.nombreCompleto COLLATE NOCASE
        """
    )
    suspend fun clientesDeEntrenador(entrenadorId: Long): List<ClienteDeEntrenadorFila>
}

data class PerfilClienteFila(
    val id: Long,
    val nombreUsuario: String,
    val nombreCompleto: String,
    val cedula: String,
    val correo: String?,
    val celular: String?,
    val creadoEn: Long,
    /** "yyyy-MM-dd" */
    val fechaNacimiento: String?,
    val sexo: String?,
    val estaturaCm: Int?,
    val tipoRutina: String?
)

data class RutinaAsignadaFila(
    val rutinaId: Long,
    val nombre: String,
    val objetivo: String?,
    val nivel: String?,
    val notas: String?,
    val fechaInicio: Long,
    val asignadoPor: String?
)

data class EjercicioClienteFila(
    val diaSemana: Int,
    val nombre: String,
    val tipoAgrupacion: String,
    val series: Int?,
    val repeticiones: String?,
    val pesoKg: Double?,
    val descansoSegundos: Int?,
    val tecnica: String,
    val observaciones: String?
)

data class EntrenadorContactoFila(
    val nombre: String,
    val celular: String?,
    val especialidad: String?
)

data class ClienteDeEntrenadorFila(
    val id: Long,
    val nombre: String,
    val nombreUsuario: String,
    val estado: String,
    val rutinaActiva: String?
)
