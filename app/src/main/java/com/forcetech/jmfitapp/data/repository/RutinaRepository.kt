package com.forcetech.jmfitapp.data.repository

import androidx.room.withTransaction
import com.forcetech.jmfitapp.data.dao.ClienteOpcion
import com.forcetech.jmfitapp.data.dao.EjercicioOpcion
import com.forcetech.jmfitapp.data.dao.RutinaResumen
import com.forcetech.jmfitapp.data.database.AppDatabase
import com.forcetech.jmfitapp.model.AsignacionRutina
import com.forcetech.jmfitapp.model.EstadoAsignacion
import com.forcetech.jmfitapp.model.EstadoRutina
import com.forcetech.jmfitapp.model.Rutina
import com.forcetech.jmfitapp.model.RutinaBloque
import com.forcetech.jmfitapp.model.RutinaDia
import com.forcetech.jmfitapp.model.RutinaEjercicio
import com.forcetech.jmfitapp.model.TipoRutina
import kotlinx.coroutines.CancellationException
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

/** Un ejercicio dentro de la rutina que se está creando o editando. */
data class LineaRutina(
    /** 1 = lunes … 7 = domingo */
    val dia: Int,
    val ejercicioId: Long,
    val ejercicioNombre: String,
    val series: Int,
    val repeticiones: String,
    val pesoKg: Double?
)

/** Datos del formulario ya validados. [clienteId] puede ser null solo al editar una rutina sin cliente. */
data class DatosRutina(
    val nombre: String,
    val objetivo: String,
    val nivel: String,
    val notas: String?,
    val clienteId: Long?,
    val lineas: List<LineaRutina>
)

/** Lo necesario para llenar el formulario al editar. */
data class RutinaParaEditar(
    val rutina: Rutina,
    /** Cliente con la asignación activa; null si la rutina no tiene cliente. */
    val cliente: ClienteOpcion?,
    val lineas: List<LineaRutina>
)

sealed interface ResultadoGuardado {
    data object Exito : ResultadoGuardado
    data object NombreDuplicado : ResultadoGuardado
    data object ClienteInvalido : ResultadoGuardado
    /** La rutina no existe, no es del entrenador o ya no está activa. */
    data object NoDisponible : ResultadoGuardado
    data object Error : ResultadoGuardado
}

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

    // ---------- Formulario: datos de apoyo ----------

    suspend fun clientesDelEntrenador(entrenadorId: Long): List<ClienteOpcion> {
        esperarBaseLista()
        return db.usuarioDao().clientesVigentesDe(entrenadorId)
    }

    suspend fun ejerciciosActivos(): List<EjercicioOpcion> {
        esperarBaseLista()
        return db.ejercicioDao().activos()
    }

    /** Nombre de la rutina activa que el cliente tiene ahora, para avisar que se finalizará al asignar otra. */
    suspend fun rutinaActivaDeCliente(clienteId: Long): String? = dao.nombreRutinaActivaDe(clienteId)

    /** Carga una rutina personalizada propia y ACTIVA para editarla. Null si no se puede editar. */
    suspend fun cargarParaEditar(rutinaId: Long, entrenadorId: Long): RutinaParaEditar? {
        esperarBaseLista()
        val rutina = dao.obtenerRutina(rutinaId) ?: return null
        if (!puedeEditar(rutina, entrenadorId)) return null
        val lineas = dao.filasParaEditar(rutinaId).map {
            LineaRutina(
                dia = it.diaSemana,
                ejercicioId = it.ejercicioId,
                ejercicioNombre = it.ejercicioNombre,
                series = it.series ?: 1,
                repeticiones = it.repeticiones.orEmpty(),
                pesoKg = it.pesoKg
            )
        }
        return RutinaParaEditar(rutina, dao.clienteActivoOpcionDe(rutinaId), lineas)
    }

    // ---------- Crear (ENT-5) ----------

    /**
     * Crea la rutina personalizada ACTIVA y la asigna al cliente, todo en una transacción.
     * Si el cliente ya tenía una rutina activa, esa asignación se FINALIZA (solo puede tener una).
     */
    suspend fun crear(entrenadorId: Long, datos: DatosRutina): ResultadoGuardado {
        esperarBaseLista()
        return try {
            db.withTransaction<ResultadoGuardado> {
                val clienteId = datos.clienteId
                if (clienteId == null || db.usuarioDao().contarClienteVigente(entrenadorId, clienteId) == 0) {
                    return@withTransaction ResultadoGuardado.ClienteInvalido
                }
                if (dao.contarNombreDuplicado(entrenadorId, datos.nombre, 0L) > 0) {
                    return@withTransaction ResultadoGuardado.NombreDuplicado
                }

                val ahora = reloj()
                val rutinaId = dao.insertar(
                    Rutina(
                        nombre = datos.nombre,
                        tipo = TipoRutina.PERSONALIZADA,
                        objetivo = datos.objetivo,
                        nivel = datos.nivel,
                        notas = datos.notas,
                        estado = EstadoRutina.ACTIVA,
                        creadoPorId = entrenadorId,
                        creadoEn = ahora,
                        actualizadoEn = ahora
                    )
                )
                insertarEstructura(rutinaId, datos.lineas)
                asignar(rutinaId, clienteId, entrenadorId, ahora)
                ResultadoGuardado.Exito
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ResultadoGuardado.Error
        }
    }

    // ---------- Editar (ENT-8) ----------

    /**
     * Actualiza los datos y reemplaza los días y ejercicios de una rutina personalizada propia y activa.
     * Si la rutina no tenía cliente y [DatosRutina.clienteId] viene lleno, la asigna a ese cliente.
     */
    suspend fun actualizar(rutinaId: Long, entrenadorId: Long, datos: DatosRutina): ResultadoGuardado {
        esperarBaseLista()
        return try {
            db.withTransaction<ResultadoGuardado> {
                val rutina = dao.obtenerRutina(rutinaId)
                if (rutina == null || !puedeEditar(rutina, entrenadorId)) {
                    return@withTransaction ResultadoGuardado.NoDisponible
                }
                if (dao.contarNombreDuplicado(entrenadorId, datos.nombre, rutinaId) > 0) {
                    return@withTransaction ResultadoGuardado.NombreDuplicado
                }

                val sinCliente = dao.clienteActivoOpcionDe(rutinaId) == null
                val clienteNuevo = if (sinCliente) datos.clienteId else null
                if (clienteNuevo != null &&
                    db.usuarioDao().contarClienteVigente(entrenadorId, clienteNuevo) == 0
                ) {
                    return@withTransaction ResultadoGuardado.ClienteInvalido
                }

                val ahora = reloj()
                dao.actualizarDatos(rutinaId, datos.nombre, datos.objetivo, datos.nivel, datos.notas, ahora)
                dao.eliminarDiasDe(rutinaId)
                insertarEstructura(rutinaId, datos.lineas)
                if (clienteNuevo != null) asignar(rutinaId, clienteNuevo, entrenadorId, ahora)
                ResultadoGuardado.Exito
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ResultadoGuardado.Error
        }
    }

    private fun puedeEditar(rutina: Rutina, entrenadorId: Long): Boolean =
        rutina.tipo == TipoRutina.PERSONALIZADA &&
                rutina.creadoPorId == entrenadorId &&
                rutina.estado == EstadoRutina.ACTIVA

    /** Finaliza la rutina activa anterior del cliente (si tenía) y crea la nueva asignación ACTIVA. */
    private suspend fun asignar(rutinaId: Long, clienteId: Long, entrenadorId: Long, ahora: Long) {
        dao.finalizarAsignacionesActivasDeCliente(clienteId, ahora)
        dao.insertarAsignacion(
            AsignacionRutina(
                clienteId = clienteId,
                rutinaId = rutinaId,
                fechaInicio = ahora,
                estado = EstadoAsignacion.ACTIVA,
                asignadoPorId = entrenadorId,
                asignadoEn = ahora
            )
        )
    }

    /**
     * Guarda los días y ejercicios. Cada ejercicio va en su propio bloque SIMPLE (acuerdo de datos).
     * El descanso se deja en 60 s por defecto porque el formulario aún no lo pide.
     */
    private suspend fun insertarEstructura(rutinaId: Long, lineas: List<LineaRutina>) {
        val porDia = lineas.groupBy { it.dia }.toSortedMap()
        for ((dia, delDia) in porDia) {
            val diaId = dao.insertarDia(RutinaDia(rutinaId = rutinaId, diaSemana = dia))
            for ((indice, linea) in delDia.withIndex()) {
                val bloqueId = dao.insertarBloque(RutinaBloque(rutinaDiaId = diaId, orden = indice + 1))
                dao.insertarEjercicio(
                    RutinaEjercicio(
                        rutinaBloqueId = bloqueId,
                        ejercicioId = linea.ejercicioId,
                        ordenEnBloque = 1,
                        series = linea.series,
                        repeticiones = linea.repeticiones,
                        descansoSegundos = DESCANSO_POR_DEFECTO_S,
                        pesoKg = linea.pesoKg
                    )
                )
            }
        }
    }

    private companion object {
        const val DESCANSO_POR_DEFECTO_S = 60
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
