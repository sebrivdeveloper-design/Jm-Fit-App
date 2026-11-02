package com.forcetech.jmfitapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.forcetech.jmfitapp.model.AsignacionEntrenador
import com.forcetech.jmfitapp.model.ClienteInterno
import com.forcetech.jmfitapp.model.Entrenador
import com.forcetech.jmfitapp.model.Usuario

/** Usuarios y tablas de personas (entrenador, cliente interno y su relación). */
@Dao
interface UsuarioDao {

    @Insert
    suspend fun insertar(usuario: Usuario): Long

    @Insert
    suspend fun insertarEntrenador(entrenador: Entrenador)

    @Insert
    suspend fun insertarClienteInterno(cliente: ClienteInterno)

    @Insert
    suspend fun insertarAsignacionEntrenador(asignacion: AsignacionEntrenador): Long

    /** La columna es NOCASE: "Admin" encuentra a "admin". */
    @Query("SELECT * FROM usuario WHERE nombreUsuario = :nombreUsuario LIMIT 1")
    suspend fun buscarPorNombreUsuario(nombreUsuario: String): Usuario?

    @Query("UPDATE usuario SET ultimoAccesoEn = :ahora WHERE id = :id")
    suspend fun actualizarUltimoAcceso(id: Long, ahora: Long)

    /** Clientes activos con asignación vigente a este entrenador (los que puede elegir al crear una rutina). */
    @Query(
        """
        SELECT u.id AS id, u.nombreCompleto AS nombre
          FROM asignacion_entrenador ae
          INNER JOIN usuario u ON u.id = ae.clienteId
         WHERE ae.entrenadorId = :entrenadorId AND ae.fechaFin IS NULL AND u.estado = 'ACTIVO'
         ORDER BY u.nombreCompleto
        """
    )
    suspend fun clientesVigentesDe(entrenadorId: Long): List<ClienteOpcion>

    /** 1 si el cliente está activo y asignado a ese entrenador; 0 si no. */
    @Query(
        """
        SELECT COUNT(*)
          FROM asignacion_entrenador ae
          INNER JOIN usuario u ON u.id = ae.clienteId
         WHERE ae.entrenadorId = :entrenadorId AND ae.clienteId = :clienteId
           AND ae.fechaFin IS NULL AND u.estado = 'ACTIVO'
        """
    )
    suspend fun contarClienteVigente(entrenadorId: Long, clienteId: Long): Int
}

