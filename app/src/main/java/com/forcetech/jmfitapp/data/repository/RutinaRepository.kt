package com.forcetech.jmfitapp.data.repository

import androidx.room.withTransaction
import com.forcetech.jmfitapp.data.dao.RutinaResumen
import com.forcetech.jmfitapp.data.database.AppDatabase
import com.forcetech.jmfitapp.model.EstadoRutina
import com.forcetech.jmfitapp.model.Rutina
import com.forcetech.jmfitapp.model.TipoRutina
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

data class EjercicioDetalle(
    val nombre: String,
    val series: Int?,
    val repeticiones: String?,
    val pesoKg: Double?,
    val descansoSegundos: Int?
)

data class DiaDetalle(val diaSemana: Int, val ejercicios: List<EjercicioDetalle>)

data class DetalleRutina(
    val rutina: Rutina,
    val clienteNombre: String?,
    val dias: List<DiaDetalle>
)

/** Rutinas del módulo del entrenador. Todas las operaciones reciben el id del entrenador con sesión. */
class RutinaRepository(
    private val db: AppDatabase,
    private val esperarBaseLista: suspend () -> Unit,
    private val reloj: () -> Long = System::currentTimeMillis
) {

    private val dao get() = db.rutinaDao()

    fun observarEstandar(): Flow<List<RutinaResumen>> = flow {
        esperarBaseLista()
        emitAll(dao.observarResumenes(TipoRutina.ESTANDAR, null))
    }

    /** Solo las personalizadas creadas por este entrenador. */
    fun observarPersonalizadas(entrenadorId: Long): Flow<List<RutinaResumen>> = flow {
        esperarBaseLista()
        emitAll(dao.observarResumenes(TipoRutina.PERSONALIZADA, entrenadorId))
    }

    /**
     * Detalle de una rutina con sus días y ejercicios. Devuelve null si no existe o si es una
     * personalizada de otro entrenador (no puede verla).
     */
    suspend fun detalle(rutinaId: Long, entrenadorId: Long): DetalleRutina? {
        esperarBaseLista()
        val rutina = dao.obtenerRutina(rutinaId) ?: return null
        if (rutina.tipo == TipoRutina.PERSONALIZADA && rutina.creadoPorId != entrenadorId) return null

        val dias = dao.filasDetalle(rutinaId)
            .groupBy { it.diaSemana }
            .map { (dia, filas) ->
                DiaDetalle(
                    diaSemana = dia,
                    ejercicios = filas.map {
                        EjercicioDetalle(it.ejercicioNombre, it.series, it.repeticiones, it.pesoKg, it.descansoSegundos)
                    }
                )
            }
            .sortedBy { it.diaSemana }

        return DetalleRutina(rutina, dao.clienteActivoDe(rutinaId), dias)
    }

    /**
     * Inhabilita o reactiva una rutina personalizada propia (ENT-7).
     *  - Inhabilitar: la rutina pasa a INHABILITADA y su asignación activa se FINALIZA (el cliente queda sin rutina).
     *  - Reactivar: vuelve a ACTIVA, sin cliente asignado.
     * Devuelve false si la rutina no existe o no es del entrenador.
     */
    suspend fun cambiarEstado(rutinaId: Long, entrenadorId: Long, activar: Boolean): Boolean {
        esperarBaseLista()
        return db.withTransaction {
            val ahora = reloj()
            val nuevoEstado = if (activar) EstadoRutina.ACTIVA else EstadoRutina.INHABILITADA
            val cambiadas = dao.cambiarEstadoPropia(rutinaId, entrenadorId, nuevoEstado, ahora)
            if (cambiadas == 0) {
                false
            } else {
                if (!activar) dao.finalizarAsignacionesActivas(rutinaId, ahora)
                true
            }
        }
    }
}
