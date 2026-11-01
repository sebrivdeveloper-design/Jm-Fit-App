package com.forcetech.jmfitapp.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidades de personas y accesos. Siguen el MER (JMFIT_V2) simplificado para la app móvil.
 * Las fechas con hora se guardan como milisegundos (Long); las fechas sin hora, como texto "yyyy-MM-dd".
 */

@Entity(
    tableName = "rol",
    indices = [Index(value = ["codigo"], unique = true)]
)
data class Rol(
    @PrimaryKey val id: Int,
    val codigo: String,
    val nombre: String
)

@Entity(
    tableName = "usuario",
    foreignKeys = [
        ForeignKey(entity = Rol::class, parentColumns = ["id"], childColumns = ["rolId"])
    ],
    indices = [
        Index("rolId"),
        Index(value = ["nombreUsuario"], unique = true),
        Index(value = ["cedula"], unique = true)
    ]
)
data class Usuario(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rolId: Int,
    /** Sin distinguir mayúsculas: "Admin" y "admin" son el mismo usuario. */
    @ColumnInfo(collate = ColumnInfo.NOCASE) val nombreUsuario: String,
    val nombreCompleto: String,
    val cedula: String,
    val correo: String? = null,
    val celular: String? = null,
    /** Hash PBKDF2 con sal (ver PasswordHasher). Nunca la contraseña en texto plano. */
    val passwordHash: String,
    val estado: String = EstadoUsuario.ACTIVO,
    val ultimoAccesoEn: Long? = null,
    val creadoEn: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "entrenador",
    foreignKeys = [
        ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["usuarioId"])
    ]
)
data class Entrenador(
    @PrimaryKey val usuarioId: Long,
    val especialidad: String? = null
)

@Entity(
    tableName = "cliente_interno",
    foreignKeys = [
        ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["usuarioId"])
    ]
)
data class ClienteInterno(
    @PrimaryKey val usuarioId: Long,
    /** "yyyy-MM-dd" */
    val fechaNacimiento: String,
    val sexo: String,
    val estaturaCm: Int? = null,
    val observacionesMedicas: String? = null,
    /** "PREESTABLECIDA" o "PERSONALIZADA" */
    val tipoRutina: String
)

/** Relación cliente–entrenador. La vigente es la que tiene fechaFin = null (una por cliente). */
@Entity(
    tableName = "asignacion_entrenador",
    foreignKeys = [
        ForeignKey(entity = ClienteInterno::class, parentColumns = ["usuarioId"], childColumns = ["clienteId"]),
        ForeignKey(entity = Entrenador::class, parentColumns = ["usuarioId"], childColumns = ["entrenadorId"]),
        ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["asignadoPorId"])
    ],
    indices = [Index("clienteId"), Index("entrenadorId"), Index("asignadoPorId")]
)
data class AsignacionEntrenador(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clienteId: Long,
    val entrenadorId: Long,
    val fechaInicio: Long = System.currentTimeMillis(),
    val fechaFin: Long? = null,
    val motivoFin: String? = null,
    val asignadoPorId: Long
)
