package com.forcetech.jmfitapp.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidades de ejercicios y rutinas. Estructura de una rutina:
 *
 *   Rutina ─< RutinaDia ─< RutinaBloque ─< RutinaEjercicio >─ Ejercicio
 *
 * Simplificación acordada: cada ejercicio va en su propio bloque SIMPLE (se crea automáticamente),
 * y series / repeticiones / peso viven en RutinaEjercicio. No se usa RUTINA_EJERCICIO_SERIE.
 */

@Entity(tableName = "grupo_muscular")
data class GrupoMuscular(
    @PrimaryKey val id: Int,
    val nombre: String,
    val estado: String = EstadoCatalogo.ACTIVO
)

@Entity(
    tableName = "ejercicio",
    foreignKeys = [
        ForeignKey(entity = GrupoMuscular::class, parentColumns = ["id"], childColumns = ["grupoMuscularId"]),
        ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["creadoPorId"])
    ],
    indices = [
        Index("grupoMuscularId"),
        Index("creadoPorId"),
        Index(value = ["nombre"], unique = true)
    ]
)
data class Ejercicio(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val nombre: String,
    val grupoMuscularId: Int,
    val descripcion: String,
    val estado: String = EstadoCatalogo.ACTIVO,
    val creadoPorId: Long,
    val creadoEn: Long = System.currentTimeMillis(),
    val actualizadoEn: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "rutina",
    foreignKeys = [
        ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["creadoPorId"])
    ],
    indices = [Index("creadoPorId")]
)
data class Rutina(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    /** [TipoRutina.ESTANDAR] o [TipoRutina.PERSONALIZADA] */
    val tipo: String,
    val objetivo: String? = null,
    val nivel: String? = null,
    val notas: String? = null,
    /** [EstadoRutina.ACTIVA] o [EstadoRutina.INHABILITADA] */
    val estado: String = EstadoRutina.ACTIVA,
    val creadoPorId: Long,
    val creadoEn: Long = System.currentTimeMillis(),
    val actualizadoEn: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "rutina_dia",
    foreignKeys = [
        ForeignKey(
            entity = Rutina::class,
            parentColumns = ["id"],
            childColumns = ["rutinaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["rutinaId", "diaSemana"], unique = true)]
)
data class RutinaDia(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rutinaId: Long,
    /** 1 = lunes … 7 = domingo */
    val diaSemana: Int,
    val notas: String? = null
)

@Entity(
    tableName = "rutina_bloque",
    foreignKeys = [
        ForeignKey(
            entity = RutinaDia::class,
            parentColumns = ["id"],
            childColumns = ["rutinaDiaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["rutinaDiaId", "orden"], unique = true)]
)
data class RutinaBloque(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rutinaDiaId: Long,
    val orden: Int,
    val tipoAgrupacion: String = TipoAgrupacion.SIMPLE,
    val descansoTrasBloqueS: Int? = null,
    val notas: String? = null
)

@Entity(
    tableName = "rutina_ejercicio",
    foreignKeys = [
        ForeignKey(
            entity = RutinaBloque::class,
            parentColumns = ["id"],
            childColumns = ["rutinaBloqueId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(entity = Ejercicio::class, parentColumns = ["id"], childColumns = ["ejercicioId"])
    ],
    indices = [
        Index(value = ["rutinaBloqueId", "ordenEnBloque"], unique = true),
        Index("ejercicioId")
    ]
)
data class RutinaEjercicio(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rutinaBloqueId: Long,
    val ejercicioId: Long,
    val ordenEnBloque: Int = 1,
    val series: Int? = null,
    /** Texto para admitir rangos como "10-12". */
    val repeticiones: String? = null,
    val descansoSegundos: Int? = null,
    /** Peso en kg, opcional (agregado a lo que trae el MER). */
    val pesoKg: Double? = null,
    val tecnica: String = TecnicaEjercicio.NORMAL,
    val observaciones: String? = null
)

/**
 * Rutina asignada a un cliente. Regla de negocio: un cliente solo puede tener UNA asignación
 * ACTIVA a la vez. Room no soporta índices parciales, así que lo garantiza el repositorio.
 */
@Entity(
    tableName = "asignacion_rutina",
    foreignKeys = [
        ForeignKey(entity = ClienteInterno::class, parentColumns = ["usuarioId"], childColumns = ["clienteId"]),
        ForeignKey(entity = Rutina::class, parentColumns = ["id"], childColumns = ["rutinaId"]),
        ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["asignadoPorId"])
    ],
    indices = [Index("clienteId"), Index("rutinaId"), Index("asignadoPorId")]
)
data class AsignacionRutina(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clienteId: Long,
    val rutinaId: Long,
    val fechaInicio: Long = System.currentTimeMillis(),
    val fechaFin: Long? = null,
    /** [EstadoAsignacion.ACTIVA] o [EstadoAsignacion.FINALIZADA] */
    val estado: String = EstadoAsignacion.ACTIVA,
    val asignadoPorId: Long,
    val asignadoEn: Long = System.currentTimeMillis()
)
