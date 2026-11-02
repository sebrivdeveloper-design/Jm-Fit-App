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
}
