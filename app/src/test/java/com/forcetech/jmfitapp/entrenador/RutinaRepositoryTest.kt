package com.forcetech.jmfitapp.entrenador

import com.forcetech.jmfitapp.data.dao.ClienteOpcion
import com.forcetech.jmfitapp.data.dao.EjercicioDao
import com.forcetech.jmfitapp.data.dao.EjercicioOpcion
import com.forcetech.jmfitapp.data.dao.FilaEdicion
import com.forcetech.jmfitapp.data.dao.FilaRutinaDetalle
import com.forcetech.jmfitapp.data.dao.RutinaDao
import com.forcetech.jmfitapp.data.dao.RutinaResumen
import com.forcetech.jmfitapp.data.dao.UsuarioDao
import com.forcetech.jmfitapp.data.database.AppDatabase
import com.forcetech.jmfitapp.data.repository.DatosRutina
import com.forcetech.jmfitapp.data.repository.DetalleRutina
import com.forcetech.jmfitapp.data.repository.DiaDetalle
import com.forcetech.jmfitapp.data.repository.EjercicioDetalle
import com.forcetech.jmfitapp.data.repository.LineaRutina
import com.forcetech.jmfitapp.data.repository.ResultadoGuardado
import com.forcetech.jmfitapp.data.repository.RutinaParaEditar
import com.forcetech.jmfitapp.data.repository.RutinaRepository
import com.forcetech.jmfitapp.data.repository.Transaccion
import com.forcetech.jmfitapp.model.AsignacionRutina
import com.forcetech.jmfitapp.model.EstadoAsignacion
import com.forcetech.jmfitapp.model.EstadoRutina
import com.forcetech.jmfitapp.model.Rutina
import com.forcetech.jmfitapp.model.RutinaBloque
import com.forcetech.jmfitapp.model.RutinaDia
import com.forcetech.jmfitapp.model.RutinaEjercicio
import com.forcetech.jmfitapp.model.TipoRutina
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.lenient
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

/**
 * ENT-5, ENT-6, ENT-7 y ENT-8: reglas de negocio de [RutinaRepository] con la base de datos simulada.
 * No comprueba el SQL de las consultas (eso lo cubriría una prueba con Room real), solo cómo el repositorio
 * las combina: validaciones, orden de las operaciones y qué se guarda.
 *
 * Los DAO simulados devuelven null para las funciones `suspend` que no se preparan; por eso cada prueba
 * prepara todo lo que el repositorio llega a consultar.
 */
@RunWith(MockitoJUnitRunner.StrictStubs::class)
class RutinaRepositoryTest {

    @Mock private lateinit var db: AppDatabase
    @Mock private lateinit var rutinaDao: RutinaDao
    @Mock private lateinit var usuarioDao: UsuarioDao
    @Mock private lateinit var ejercicioDao: EjercicioDao

    private lateinit var repo: RutinaRepository
    private var esperasDeBase = 0

    /** Sin transacción real: ejecuta el bloque directamente. */
    private object SinTransaccion : Transaccion {
        override suspend fun <T> ejecutar(bloque: suspend () -> T): T = bloque()
    }

    @Before
    fun preparar() {
        lenient().`when`(db.rutinaDao()).thenReturn(rutinaDao)
        lenient().`when`(db.usuarioDao()).thenReturn(usuarioDao)
        lenient().`when`(db.ejercicioDao()).thenReturn(ejercicioDao)
        repo = RutinaRepository(
            db = db,
            esperarBaseLista = { esperasDeBase++ },
            reloj = { AHORA },
            transaccion = SinTransaccion
        )
    }

    // ---------- Datos de apoyo ----------

    private fun rutina(
        id: Long = 5,
        estado: String = EstadoRutina.ACTIVA,
        creadoPorId: Long = ENTRENADOR,
        tipo: String = TipoRutina.PERSONALIZADA
    ) = Rutina(
        id = id,
        nombre = "Rutina vieja",
        tipo = tipo,
        objetivo = "Hipertrofia",
        nivel = "Principiante",
        notas = null,
        estado = estado,
        creadoPorId = creadoPorId,
        creadoEn = 500L,
        actualizadoEn = 500L
    )

    private val pressBanca = LineaRutina(1, 100L, "Press de banca", 4, "10", 40.0)
    private val remo = LineaRutina(1, 101L, "Remo con barra", 3, "8-12", null)
    private val sentadilla = LineaRutina(3, 102L, "Sentadilla", 5, "5", 80.5)

    private fun datos(
        clienteId: Long? = CLIENTE,
        lineas: List<LineaRutina> = listOf(pressBanca, remo, sentadilla),
        nombre: String = "Fuerza base"
    ) = DatosRutina(
        nombre = nombre,
        objetivo = "Fuerza",
        nivel = "Intermedio",
        notas = "Descansar 90 s",
        clienteId = clienteId,
        lineas = lineas
    )

    private fun asignacion(rutinaId: Long, clienteId: Long = CLIENTE) = AsignacionRutina(
        clienteId = clienteId,
        rutinaId = rutinaId,
        fechaInicio = AHORA,
        estado = EstadoAsignacion.ACTIVA,
        asignadoPorId = ENTRENADOR,
        asignadoEn = AHORA
    )

    /**
     * Prepara las inserciones de días, bloques y ejercicios de [lineas] y devuelve los ejercicios que
     * el repositorio debe insertar. Si el repositorio insertara algo distinto, la prueba falla.
     */
    private suspend fun prepararEstructura(rutinaId: Long, lineas: List<LineaRutina>): List<RutinaEjercicio> {
        val esperados = mutableListOf<RutinaEjercicio>()
        var siguienteId = rutinaId * 100
        for ((dia, delDia) in lineas.groupBy { it.dia }.toSortedMap()) {
            siguienteId += 1
            val diaId = siguienteId
            `when`(rutinaDao.insertarDia(RutinaDia(rutinaId = rutinaId, diaSemana = dia))).thenReturn(diaId)
            for ((indice, linea) in delDia.withIndex()) {
                siguienteId += 1
                val bloqueId = siguienteId
                `when`(rutinaDao.insertarBloque(RutinaBloque(rutinaDiaId = diaId, orden = indice + 1)))
                    .thenReturn(bloqueId)
                val ejercicio = RutinaEjercicio(
                    rutinaBloqueId = bloqueId,
                    ejercicioId = linea.ejercicioId,
                    ordenEnBloque = 1,
                    series = linea.series,
                    repeticiones = linea.repeticiones,
                    descansoSegundos = 60,
                    pesoKg = linea.pesoKg
                )
                `when`(rutinaDao.insertarEjercicio(ejercicio)).thenReturn(1L)
                esperados += ejercicio
            }
        }
        return esperados
    }

    private val rutinaNuevaEsperada = Rutina(
        nombre = "Fuerza base",
        tipo = TipoRutina.PERSONALIZADA,
        objetivo = "Fuerza",
        nivel = "Intermedio",
        notas = "Descansar 90 s",
        estado = EstadoRutina.ACTIVA,
        creadoPorId = ENTRENADOR,
        creadoEn = AHORA,
        actualizadoEn = AHORA
    )

    // ======================================================================
    // ENT-5: crear
    // ======================================================================

    @Test
    fun `crear guarda rutina personalizada activa con su estructura y la asigna al cliente`() = runTest {
        `when`(usuarioDao.contarClienteVigente(ENTRENADOR, CLIENTE)).thenReturn(1)
        `when`(rutinaDao.contarNombreDuplicado(ENTRENADOR, "Fuerza base", 0L)).thenReturn(0)
        `when`(rutinaDao.insertar(rutinaNuevaEsperada)).thenReturn(10L)
        val ejercicios = prepararEstructura(10L, datos().lineas)
        `when`(rutinaDao.insertarAsignacion(asignacion(10L))).thenReturn(50L)

        val resultado = repo.crear(ENTRENADOR, datos())

        assertEquals(ResultadoGuardado.Exito, resultado)
        verify(rutinaDao).insertar(rutinaNuevaEsperada)
        verify(rutinaDao).insertarDia(RutinaDia(rutinaId = 10L, diaSemana = 1))
        verify(rutinaDao).insertarDia(RutinaDia(rutinaId = 10L, diaSemana = 3))
        ejercicios.forEach { verify(rutinaDao).insertarEjercicio(it) }
        verify(rutinaDao).insertarAsignacion(asignacion(10L))
    }

    @Test
    fun `crear coloca cada ejercicio en su propio bloque y numera los bloques de cada dia`() = runTest {
        `when`(usuarioDao.contarClienteVigente(ENTRENADOR, CLIENTE)).thenReturn(1)
        `when`(rutinaDao.contarNombreDuplicado(ENTRENADOR, "Fuerza base", 0L)).thenReturn(0)
        `when`(rutinaDao.insertar(rutinaNuevaEsperada)).thenReturn(10L)
        prepararEstructura(10L, datos().lineas)
        `when`(rutinaDao.insertarAsignacion(asignacion(10L))).thenReturn(50L)

        repo.crear(ENTRENADOR, datos())

        // El día 1 (lunes) tiene dos ejercicios: bloques 1 y 2 del mismo día. El día 3 (miércoles) tiene uno: bloque 1.
        verify(rutinaDao).insertarBloque(RutinaBloque(rutinaDiaId = 1001L, orden = 1))
        verify(rutinaDao).insertarBloque(RutinaBloque(rutinaDiaId = 1001L, orden = 2))
        verify(rutinaDao).insertarBloque(RutinaBloque(rutinaDiaId = 1004L, orden = 1))
    }

    @Test
    fun `crear finaliza la rutina activa anterior del cliente antes de asignar la nueva`() = runTest {
        `when`(usuarioDao.contarClienteVigente(ENTRENADOR, CLIENTE)).thenReturn(1)
        `when`(rutinaDao.contarNombreDuplicado(ENTRENADOR, "Fuerza base", 0L)).thenReturn(0)
        `when`(rutinaDao.insertar(rutinaNuevaEsperada)).thenReturn(10L)
        prepararEstructura(10L, datos().lineas)
        `when`(rutinaDao.insertarAsignacion(asignacion(10L))).thenReturn(50L)

        repo.crear(ENTRENADOR, datos())

        val orden = inOrder(rutinaDao)
        orden.verify(rutinaDao).finalizarAsignacionesActivasDeCliente(CLIENTE, AHORA)
        orden.verify(rutinaDao).insertarAsignacion(asignacion(10L))
    }

    @Test
    fun `crear sin cliente devuelve cliente invalido y no guarda nada`() = runTest {
        val resultado = repo.crear(ENTRENADOR, datos(clienteId = null))

        assertEquals(ResultadoGuardado.ClienteInvalido, resultado)
        verifyNoInteractions(rutinaDao)
        verifyNoInteractions(usuarioDao)
    }

    @Test
    fun `crear para un cliente que no es del entrenador o no esta vigente devuelve cliente invalido`() = runTest {
        `when`(usuarioDao.contarClienteVigente(ENTRENADOR, CLIENTE)).thenReturn(0)

        val resultado = repo.crear(ENTRENADOR, datos())

        assertEquals(ResultadoGuardado.ClienteInvalido, resultado)
        verifyNoInteractions(rutinaDao)
    }

    @Test
    fun `crear con nombre repetido devuelve nombre duplicado y no guarda nada`() = runTest {
        `when`(usuarioDao.contarClienteVigente(ENTRENADOR, CLIENTE)).thenReturn(1)
        `when`(rutinaDao.contarNombreDuplicado(ENTRENADOR, "Fuerza base", 0L)).thenReturn(1)

        val resultado = repo.crear(ENTRENADOR, datos())

        assertEquals(ResultadoGuardado.NombreDuplicado, resultado)
        verify(rutinaDao).contarNombreDuplicado(ENTRENADOR, "Fuerza base", 0L)
        verifyNoMoreInteractions(rutinaDao)
    }

    @Test
    fun `crear devuelve error si la base de datos falla`() = runTest {
        `when`(usuarioDao.contarClienteVigente(ENTRENADOR, CLIENTE)).thenThrow(IllegalStateException("fallo"))

        assertEquals(ResultadoGuardado.Error, repo.crear(ENTRENADOR, datos()))
    }

    @Test
    fun `crear no convierte una cancelacion en error`() = runTest {
        val cancelacion = CancellationException("cancelada")
        `when`(usuarioDao.contarClienteVigente(ENTRENADOR, CLIENTE)).thenThrow(cancelacion)

        val lanzada = runCatching { repo.crear(ENTRENADOR, datos()) }.exceptionOrNull()

        assertSame(cancelacion, lanzada)
    }

    @Test
    fun `crear espera a que la base de datos este lista`() = runTest {
        repo.crear(ENTRENADOR, datos(clienteId = null))

        assertEquals(1, esperasDeBase)
    }

    // ======================================================================
    // ENT-8: editar
    // ======================================================================

    @Test
    fun `actualizar una rutina sin cliente cambia los datos reemplaza la estructura y asigna el cliente`() = runTest {
        val lineas = listOf(pressBanca)
        `when`(rutinaDao.obtenerRutina(5L)).thenReturn(rutina())
        `when`(rutinaDao.contarNombreDuplicado(ENTRENADOR, "Fuerza base", 5L)).thenReturn(0)
        `when`(rutinaDao.clienteActivoOpcionDe(5L)).thenReturn(null)
        `when`(usuarioDao.contarClienteVigente(ENTRENADOR, CLIENTE)).thenReturn(1)
        val ejercicios = prepararEstructura(5L, lineas)
        `when`(rutinaDao.insertarAsignacion(asignacion(5L))).thenReturn(60L)

        val resultado = repo.actualizar(5L, ENTRENADOR, datos(lineas = lineas))

        assertEquals(ResultadoGuardado.Exito, resultado)
        val orden = inOrder(rutinaDao)
        orden.verify(rutinaDao).actualizarDatos(5L, "Fuerza base", "Fuerza", "Intermedio", "Descansar 90 s", AHORA)
        orden.verify(rutinaDao).eliminarDiasDe(5L)
        orden.verify(rutinaDao).insertarDia(RutinaDia(rutinaId = 5L, diaSemana = 1))
        ejercicios.forEach { orden.verify(rutinaDao).insertarEjercicio(it) }
        orden.verify(rutinaDao).finalizarAsignacionesActivasDeCliente(CLIENTE, AHORA)
        orden.verify(rutinaDao).insertarAsignacion(asignacion(5L))
    }

    @Test
    fun `actualizar una rutina que ya tiene cliente no cambia su cliente`() = runTest {
        val lineas = listOf(pressBanca)
        `when`(rutinaDao.obtenerRutina(5L)).thenReturn(rutina())
        `when`(rutinaDao.contarNombreDuplicado(ENTRENADOR, "Fuerza base", 5L)).thenReturn(0)
        `when`(rutinaDao.clienteActivoOpcionDe(5L)).thenReturn(ClienteOpcion(CLIENTE, "Camila Restrepo"))
        prepararEstructura(5L, lineas)

        // Aunque se intente mandar otro cliente (9), se ignora
        val resultado = repo.actualizar(5L, ENTRENADOR, datos(clienteId = 9L, lineas = lineas))

        assertEquals(ResultadoGuardado.Exito, resultado)
        verifyNoInteractions(usuarioDao)
        verify(rutinaDao, never()).finalizarAsignacionesActivasDeCliente(9L, AHORA)
        verify(rutinaDao, never()).insertarAsignacion(asignacion(5L, clienteId = 9L))
    }

    @Test
    fun `actualizar sin cliente y sin cliente nuevo guarda los cambios sin asignar a nadie`() = runTest {
        val lineas = listOf(pressBanca)
        `when`(rutinaDao.obtenerRutina(5L)).thenReturn(rutina())
        `when`(rutinaDao.contarNombreDuplicado(ENTRENADOR, "Fuerza base", 5L)).thenReturn(0)
        `when`(rutinaDao.clienteActivoOpcionDe(5L)).thenReturn(null)
        prepararEstructura(5L, lineas)

        val resultado = repo.actualizar(5L, ENTRENADOR, datos(clienteId = null, lineas = lineas))

        assertEquals(ResultadoGuardado.Exito, resultado)
        verifyNoInteractions(usuarioDao)
        verify(rutinaDao, never()).insertarAsignacion(asignacion(5L))
    }

    @Test
    fun `actualizar una rutina inexistente devuelve no disponible`() = runTest {
        `when`(rutinaDao.obtenerRutina(5L)).thenReturn(null)

        assertEquals(ResultadoGuardado.NoDisponible, repo.actualizar(5L, ENTRENADOR, datos()))
        verifyNoMoreInteractions(rutinaDao)
    }

    @Test
    fun `actualizar una rutina inhabilitada devuelve no disponible`() = runTest {
        `when`(rutinaDao.obtenerRutina(5L)).thenReturn(rutina(estado = EstadoRutina.INHABILITADA))

        assertEquals(ResultadoGuardado.NoDisponible, repo.actualizar(5L, ENTRENADOR, datos()))
        verifyNoMoreInteractions(rutinaDao)
    }

    @Test
    fun `actualizar la rutina de otro entrenador devuelve no disponible`() = runTest {
        `when`(rutinaDao.obtenerRutina(5L)).thenReturn(rutina(creadoPorId = 2L))

        assertEquals(ResultadoGuardado.NoDisponible, repo.actualizar(5L, ENTRENADOR, datos()))
        verifyNoMoreInteractions(rutinaDao)
    }

    @Test
    fun `actualizar una rutina estandar devuelve no disponible`() = runTest {
        `when`(rutinaDao.obtenerRutina(5L)).thenReturn(rutina(tipo = TipoRutina.ESTANDAR))

        assertEquals(ResultadoGuardado.NoDisponible, repo.actualizar(5L, ENTRENADOR, datos()))
        verifyNoMoreInteractions(rutinaDao)
    }

    @Test
    fun `actualizar con un nombre de otra rutina propia devuelve nombre duplicado y excluye la propia`() = runTest {
        `when`(rutinaDao.obtenerRutina(5L)).thenReturn(rutina())
        // El tercer argumento es el id que se excluye: la propia rutina no cuenta como duplicado
        `when`(rutinaDao.contarNombreDuplicado(ENTRENADOR, "Fuerza base", 5L)).thenReturn(1)

        assertEquals(ResultadoGuardado.NombreDuplicado, repo.actualizar(5L, ENTRENADOR, datos()))
        verify(rutinaDao, never()).eliminarDiasDe(5L)
    }

    @Test
    fun `actualizar con cliente no vigente devuelve cliente invalido y no modifica la rutina`() = runTest {
        `when`(rutinaDao.obtenerRutina(5L)).thenReturn(rutina())
        `when`(rutinaDao.contarNombreDuplicado(ENTRENADOR, "Fuerza base", 5L)).thenReturn(0)
        `when`(rutinaDao.clienteActivoOpcionDe(5L)).thenReturn(null)
        `when`(usuarioDao.contarClienteVigente(ENTRENADOR, CLIENTE)).thenReturn(0)

        assertEquals(ResultadoGuardado.ClienteInvalido, repo.actualizar(5L, ENTRENADOR, datos()))
        verify(rutinaDao, never()).eliminarDiasDe(5L)
    }

    @Test
    fun `actualizar devuelve error si la base de datos falla`() = runTest {
        `when`(rutinaDao.obtenerRutina(5L)).thenThrow(IllegalStateException("fallo"))

        assertEquals(ResultadoGuardado.Error, repo.actualizar(5L, ENTRENADOR, datos()))
    }

    // ---------- Cargar para editar ----------

    @Test
    fun `cargar para editar devuelve rutina cliente y ejercicios con valores por defecto si faltan datos`() = runTest {
        val rutina = rutina()
        `when`(rutinaDao.obtenerRutina(5L)).thenReturn(rutina)
        `when`(rutinaDao.filasParaEditar(5L)).thenReturn(
            listOf(
                FilaEdicion(1, 100L, "Press de banca", 4, "10", 40.0),
                FilaEdicion(2, 101L, "Remo con barra", null, null, null)
            )
        )
        `when`(rutinaDao.clienteActivoOpcionDe(5L)).thenReturn(ClienteOpcion(CLIENTE, "Camila Restrepo"))

        val cargada = repo.cargarParaEditar(5L, ENTRENADOR)

        assertEquals(
            RutinaParaEditar(
                rutina = rutina,
                cliente = ClienteOpcion(CLIENTE, "Camila Restrepo"),
                lineas = listOf(
                    LineaRutina(1, 100L, "Press de banca", 4, "10", 40.0),
                    LineaRutina(2, 101L, "Remo con barra", 1, "", null)
                )
            ),
            cargada
        )
    }

    @Test
    fun `cargar para editar devuelve null si la rutina es ajena inhabilitada o no existe`() = runTest {
        `when`(rutinaDao.obtenerRutina(5L)).thenReturn(rutina(creadoPorId = 2L))
        assertNull(repo.cargarParaEditar(5L, ENTRENADOR))

        `when`(rutinaDao.obtenerRutina(6L)).thenReturn(rutina(id = 6L, estado = EstadoRutina.INHABILITADA))
        assertNull(repo.cargarParaEditar(6L, ENTRENADOR))

        `when`(rutinaDao.obtenerRutina(7L)).thenReturn(null)
        assertNull(repo.cargarParaEditar(7L, ENTRENADOR))
    }

    // ======================================================================
    // ENT-7: inhabilitar y reactivar
    // ======================================================================

    @Test
    fun `inhabilitar cambia el estado y finaliza la asignacion activa`() = runTest {
        `when`(rutinaDao.cambiarEstadoPropia(5L, ENTRENADOR, EstadoRutina.INHABILITADA, AHORA)).thenReturn(1)

        val ok = repo.cambiarEstado(5L, ENTRENADOR, activar = false)

        assertEquals(true, ok)
        val orden = inOrder(rutinaDao)
        orden.verify(rutinaDao).cambiarEstadoPropia(5L, ENTRENADOR, EstadoRutina.INHABILITADA, AHORA)
        orden.verify(rutinaDao).finalizarAsignacionesActivas(5L, AHORA)
    }

    @Test
    fun `reactivar vuelve a activa y no asigna ni finaliza nada`() = runTest {
        `when`(rutinaDao.cambiarEstadoPropia(5L, ENTRENADOR, EstadoRutina.ACTIVA, AHORA)).thenReturn(1)

        val ok = repo.cambiarEstado(5L, ENTRENADOR, activar = true)

        assertEquals(true, ok)
        verify(rutinaDao).cambiarEstadoPropia(5L, ENTRENADOR, EstadoRutina.ACTIVA, AHORA)
        verifyNoMoreInteractions(rutinaDao)
    }

    @Test
    fun `cambiar el estado de una rutina que no es del entrenador devuelve false y no toca asignaciones`() = runTest {
        `when`(rutinaDao.cambiarEstadoPropia(5L, ENTRENADOR, EstadoRutina.INHABILITADA, AHORA)).thenReturn(0)

        val ok = repo.cambiarEstado(5L, ENTRENADOR, activar = false)

        assertEquals(false, ok)
        verify(rutinaDao, never()).finalizarAsignacionesActivas(5L, AHORA)
    }

    // ======================================================================
    // ENT-6: listar y ver
    // ======================================================================

    private fun resumen(id: Long, nombre: String, tipo: String = TipoRutina.PERSONALIZADA) = RutinaResumen(
        id = id,
        nombre = nombre,
        tipo = tipo,
        objetivo = "Hipertrofia",
        nivel = "Intermedio",
        estado = EstadoRutina.ACTIVA,
        totalEjercicios = 4,
        clienteNombre = null
    )

    @Test
    fun `observar estandar consulta las rutinas estandar de cualquier autor`() = runTest {
        val lista = listOf(resumen(1, "Full body", TipoRutina.ESTANDAR))
        `when`(rutinaDao.observarResumenes(TipoRutina.ESTANDAR, null)).thenReturn(flowOf(lista))

        assertEquals(lista, repo.observarEstandar().first())
    }

    @Test
    fun `observar personalizadas consulta solo las del entrenador`() = runTest {
        val lista = listOf(resumen(2, "Hipertrofia Fase 2"))
        `when`(rutinaDao.observarResumenes(TipoRutina.PERSONALIZADA, ENTRENADOR)).thenReturn(flowOf(lista))

        assertEquals(lista, repo.observarPersonalizadas(ENTRENADOR).first())
    }

    @Test
    fun `detalle de una rutina inexistente es null`() = runTest {
        `when`(rutinaDao.obtenerRutina(5L)).thenReturn(null)

        assertNull(repo.detalle(5L, ENTRENADOR))
    }

    @Test
    fun `detalle de una personalizada de otro entrenador es null y no consulta ejercicios`() = runTest {
        `when`(rutinaDao.obtenerRutina(5L)).thenReturn(rutina(creadoPorId = 2L))

        assertNull(repo.detalle(5L, ENTRENADOR))
        verify(rutinaDao, never()).filasDetalle(5L)
    }

    @Test
    fun `detalle de una rutina estandar es visible para cualquier entrenador`() = runTest {
        val estandar = rutina(id = 9L, tipo = TipoRutina.ESTANDAR, creadoPorId = 99L)
        `when`(rutinaDao.obtenerRutina(9L)).thenReturn(estandar)
        `when`(rutinaDao.filasDetalle(9L)).thenReturn(emptyList())
        `when`(rutinaDao.clienteActivoDe(9L)).thenReturn(null)

        assertEquals(DetalleRutina(estandar, null, emptyList()), repo.detalle(9L, ENTRENADOR))
    }

    @Test
    fun `detalle agrupa los ejercicios por dia y ordena los dias de lunes a domingo`() = runTest {
        val propia = rutina()
        `when`(rutinaDao.obtenerRutina(5L)).thenReturn(propia)
        `when`(rutinaDao.filasDetalle(5L)).thenReturn(
            listOf(
                FilaRutinaDetalle(3, "Sentadilla", 5, "5", 80.0, 90),
                FilaRutinaDetalle(1, "Press de banca", 4, "10", null, 60),
                FilaRutinaDetalle(1, "Remo con barra", 3, "8-12", 30.5, 60)
            )
        )
        `when`(rutinaDao.clienteActivoDe(5L)).thenReturn("Camila Restrepo")

        val detalle = repo.detalle(5L, ENTRENADOR)

        assertEquals(
            DetalleRutina(
                rutina = propia,
                clienteNombre = "Camila Restrepo",
                dias = listOf(
                    DiaDetalle(
                        1,
                        listOf(
                            EjercicioDetalle("Press de banca", 4, "10", null, 60),
                            EjercicioDetalle("Remo con barra", 3, "8-12", 30.5, 60)
                        )
                    ),
                    DiaDetalle(3, listOf(EjercicioDetalle("Sentadilla", 5, "5", 80.0, 90)))
                )
            ),
            detalle
        )
    }

    // ---------- Datos de apoyo del formulario ----------

    @Test
    fun `clientes del entrenador y ejercicios activos vienen de sus consultas`() = runTest {
        val clientes = listOf(ClienteOpcion(7L, "Camila Restrepo"), ClienteOpcion(8L, "Felipe Ramírez"))
        val ejercicios = listOf(EjercicioOpcion(100L, "Press de banca"))
        `when`(usuarioDao.clientesVigentesDe(ENTRENADOR)).thenReturn(clientes)
        `when`(ejercicioDao.activos()).thenReturn(ejercicios)

        assertEquals(clientes, repo.clientesDelEntrenador(ENTRENADOR))
        assertEquals(ejercicios, repo.ejerciciosActivos())
    }

    @Test
    fun `rutina activa de un cliente devuelve el nombre o null`() = runTest {
        `when`(rutinaDao.nombreRutinaActivaDe(7L)).thenReturn("Hipertrofia Fase 2")
        `when`(rutinaDao.nombreRutinaActivaDe(8L)).thenReturn(null)

        assertEquals("Hipertrofia Fase 2", repo.rutinaActivaDeCliente(7L))
        assertNull(repo.rutinaActivaDeCliente(8L))
    }

    private companion object {
        const val ENTRENADOR = 1L
        const val CLIENTE = 7L
        const val AHORA = 1_000L
    }
}
