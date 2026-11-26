package com.forcetech.jmfitapp.entrenador

import com.forcetech.jmfitapp.R
import com.forcetech.jmfitapp.data.dao.ClienteOpcion
import com.forcetech.jmfitapp.data.dao.EjercicioOpcion
import com.forcetech.jmfitapp.data.repository.DatosRutina
import com.forcetech.jmfitapp.data.repository.LineaRutina
import com.forcetech.jmfitapp.data.repository.ResultadoGuardado
import com.forcetech.jmfitapp.data.repository.RutinaParaEditar
import com.forcetech.jmfitapp.data.repository.RutinaRepository
import com.forcetech.jmfitapp.model.Rutina
import com.forcetech.jmfitapp.model.TipoRutina
import com.forcetech.jmfitapp.ui.trainer.RutinaFormEvento
import com.forcetech.jmfitapp.ui.trainer.RutinaFormViewModel
import com.forcetech.jmfitapp.ui.trainer.TextosIniciales
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

/** ENT-5 (crear) y ENT-8 (editar): lógica del formulario de rutina personalizada en [RutinaFormViewModel]. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner.StrictStubs::class)
class RutinaFormViewModelTest {

    @get:Rule
    val principal = MainDispatcherRule()

    @Mock private lateinit var repo: RutinaRepository

    private val camila = ClienteOpcion(7L, "Camila Restrepo")
    private val ejercicios = listOf(EjercicioOpcion(100L, "Press de banca"), EjercicioOpcion(101L, "Remo con barra"))

    private fun linea(dia: Int = 1, ejercicioId: Long = 100L, nombre: String = "Press de banca", series: Int = 4) =
        LineaRutina(dia, ejercicioId, nombre, series, "10", null)

    private val rutinaExistente = Rutina(
        id = 5L,
        nombre = "Hipertrofia Fase 2",
        tipo = TipoRutina.PERSONALIZADA,
        objetivo = "Hipertrofia",
        nivel = "Intermedio",
        notas = null,
        creadoPorId = ENTRENADOR,
        creadoEn = 500L,
        actualizadoEn = 500L
    )

    /**
     * Recoge los eventos que el ViewModel envía a la pantalla, en el orden en que salen. Incluye los que
     * ya estaban pendientes de entregar antes de empezar a recoger (el canal los guarda).
     */
    private fun TestScope.eventosDe(vm: RutinaFormViewModel): MutableList<RutinaFormEvento> {
        val recogidos = mutableListOf<RutinaFormEvento>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.eventos.toList(recogidos) }
        return recogidos
    }

    /** Igual que [eventosDe], pero descarta lo anterior: solo quedan los eventos que ocurran desde ahora. */
    private fun TestScope.eventosNuevos(vm: RutinaFormViewModel): MutableList<RutinaFormEvento> =
        eventosDe(vm).also { it.clear() }

    /** ViewModel en modo crear, con los selectores ya cargados. */
    private suspend fun crearVm(): RutinaFormViewModel {
        `when`(repo.clientesDelEntrenador(ENTRENADOR)).thenReturn(listOf(camila))
        `when`(repo.ejerciciosActivos()).thenReturn(ejercicios)
        return RutinaFormViewModel(repo, ENTRENADOR, 0L)
    }

    /** ViewModel en modo editar la rutina 5 con el cliente y las líneas indicados. */
    private suspend fun editarVm(
        cliente: ClienteOpcion? = camila,
        lineas: List<LineaRutina> = listOf(linea())
    ): RutinaFormViewModel {
        `when`(repo.clientesDelEntrenador(ENTRENADOR)).thenReturn(listOf(camila))
        `when`(repo.ejerciciosActivos()).thenReturn(ejercicios)
        `when`(repo.cargarParaEditar(5L, ENTRENADOR)).thenReturn(RutinaParaEditar(rutinaExistente, cliente, lineas))
        return RutinaFormViewModel(repo, ENTRENADOR, 5L)
    }

    // ======================================================================
    // Carga
    // ======================================================================

    @Test
    fun `en modo crear carga clientes y ejercicios y empieza vacio`() = runTest {
        val vm = crearVm()

        val estado = vm.uiState.value
        assertFalse(vm.esEdicion)
        assertFalse(estado.cargando)
        assertEquals(listOf(camila), estado.clientes)
        assertEquals(ejercicios, estado.ejercicios)
        assertNull(estado.cliente)
        assertTrue(estado.lineas.isEmpty())
        assertFalse(estado.clienteBloqueado)
    }

    @Test
    fun `si falla la carga avisa del error y deja de cargar`() = runTest {
        `when`(repo.clientesDelEntrenador(ENTRENADOR)).thenThrow(IllegalStateException("fallo"))

        val vm = RutinaFormViewModel(repo, ENTRENADOR, 0L)
        val eventos = eventosDe(vm)

        assertFalse(vm.uiState.value.cargando)
        assertEquals(listOf<RutinaFormEvento>(RutinaFormEvento.Mensaje(R.string.form_error_cargar)), eventos)
    }

    @Test
    fun `en modo editar precarga los textos el cliente y los ejercicios`() = runTest {
        val vm = editarVm(cliente = camila, lineas = listOf(linea(), linea(dia = 3, ejercicioId = 101L, nombre = "Remo con barra")))
        val eventos = eventosDe(vm)

        val estado = vm.uiState.value
        assertTrue(vm.esEdicion)
        assertEquals(camila, estado.cliente)
        assertTrue(estado.clienteBloqueado)
        assertEquals(2, estado.lineas.size)
        assertEquals(
            listOf<RutinaFormEvento>(
                RutinaFormEvento.Precargar(TextosIniciales("Hipertrofia Fase 2", "Hipertrofia", "Intermedio", ""))
            ),
            eventos
        )
    }

    @Test
    fun `al editar una rutina sin cliente el selector queda libre`() = runTest {
        val vm = editarVm(cliente = null)

        assertNull(vm.uiState.value.cliente)
        assertFalse(vm.uiState.value.clienteBloqueado)
    }

    @Test
    fun `al editar una rutina no disponible lo indica y no precarga nada`() = runTest {
        `when`(repo.clientesDelEntrenador(ENTRENADOR)).thenReturn(listOf(camila))
        `when`(repo.ejerciciosActivos()).thenReturn(ejercicios)
        `when`(repo.cargarParaEditar(5L, ENTRENADOR)).thenReturn(null)

        val vm = RutinaFormViewModel(repo, ENTRENADOR, 5L)
        val eventos = eventosDe(vm)

        assertTrue(vm.uiState.value.noDisponible)
        assertFalse(vm.uiState.value.cargando)
        assertTrue(eventos.isEmpty())
    }

    // ======================================================================
    // Cliente
    // ======================================================================

    @Test
    fun `seleccionar cliente lo guarda y se puede quitar`() = runTest {
        val vm = crearVm()

        vm.seleccionarCliente(camila)
        assertEquals(camila, vm.uiState.value.cliente)

        vm.seleccionarCliente(null)
        assertNull(vm.uiState.value.cliente)
    }

    @Test
    fun `no se puede cambiar el cliente de una rutina que ya lo tiene`() = runTest {
        val vm = editarVm(cliente = camila)

        vm.seleccionarCliente(ClienteOpcion(8L, "Felipe Ramírez"))
        vm.seleccionarCliente(null)

        assertEquals(camila, vm.uiState.value.cliente)
    }

    // ======================================================================
    // Ejercicios de la rutina
    // ======================================================================

    @Test
    fun `agregar un ejercicio lo suma a la lista y limpia el editor`() = runTest {
        val vm = crearVm()
        val eventos = eventosDe(vm)

        vm.guardarLinea(linea())

        assertEquals(listOf(linea()), vm.uiState.value.lineas)
        assertEquals(listOf<RutinaFormEvento>(RutinaFormEvento.LimpiarEditor), eventos)
    }

    @Test
    fun `no permite repetir el mismo ejercicio en el mismo dia`() = runTest {
        val vm = crearVm()
        vm.guardarLinea(linea())
        val eventos = eventosNuevos(vm)

        vm.guardarLinea(linea(series = 5))

        assertEquals(1, vm.uiState.value.lineas.size)
        assertEquals(
            listOf<RutinaFormEvento>(RutinaFormEvento.Mensaje(R.string.form_ejercicio_repetido)),
            eventos
        )
    }

    @Test
    fun `permite el mismo ejercicio en otro dia y otro ejercicio el mismo dia`() = runTest {
        val vm = crearVm()

        vm.guardarLinea(linea(dia = 1))
        vm.guardarLinea(linea(dia = 3))
        vm.guardarLinea(linea(dia = 1, ejercicioId = 101L, nombre = "Remo con barra"))

        assertEquals(3, vm.uiState.value.lineas.size)
    }

    @Test
    fun `modificar un ejercicio carga sus datos en el editor y marca cual se modifica`() = runTest {
        val vm = crearVm()
        vm.guardarLinea(linea())
        vm.guardarLinea(linea(dia = 3))
        val eventos = eventosNuevos(vm)

        vm.editarLinea(1)

        assertEquals(1, vm.uiState.value.editandoIndice)
        assertEquals(listOf<RutinaFormEvento>(RutinaFormEvento.CargarLinea(linea(dia = 3))), eventos)
    }

    @Test
    fun `modificar un indice que no existe no hace nada`() = runTest {
        val vm = crearVm()
        val eventos = eventosDe(vm)

        vm.editarLinea(3)

        assertNull(vm.uiState.value.editandoIndice)
        assertTrue(eventos.isEmpty())
    }

    @Test
    fun `guardar un ejercicio modificado reemplaza el original sin duplicarlo`() = runTest {
        val vm = crearVm()
        vm.guardarLinea(linea(series = 4))
        vm.editarLinea(0)

        // Mismo día y ejercicio que él mismo: no cuenta como repetido
        vm.guardarLinea(linea(series = 6))

        assertEquals(listOf(linea(series = 6)), vm.uiState.value.lineas)
        assertNull(vm.uiState.value.editandoIndice)
    }

    @Test
    fun `modificar un ejercicio para que coincida con otro se rechaza`() = runTest {
        val vm = crearVm()
        vm.guardarLinea(linea())
        vm.guardarLinea(linea(ejercicioId = 101L, nombre = "Remo con barra"))
        vm.editarLinea(1)
        val eventos = eventosNuevos(vm)

        vm.guardarLinea(linea())

        assertEquals(
            listOf(linea(), linea(ejercicioId = 101L, nombre = "Remo con barra")),
            vm.uiState.value.lineas
        )
        assertEquals(
            listOf<RutinaFormEvento>(RutinaFormEvento.Mensaje(R.string.form_ejercicio_repetido)),
            eventos
        )
    }

    @Test
    fun `cancelar la modificacion limpia el editor`() = runTest {
        val vm = crearVm()
        vm.guardarLinea(linea())
        vm.editarLinea(0)
        val eventos = eventosNuevos(vm)

        vm.cancelarEdicionLinea()

        assertNull(vm.uiState.value.editandoIndice)
        assertEquals(listOf<RutinaFormEvento>(RutinaFormEvento.LimpiarEditor), eventos)
    }

    @Test
    fun `cancelar sin estar modificando no hace nada`() = runTest {
        val vm = crearVm()
        val eventos = eventosDe(vm)

        vm.cancelarEdicionLinea()

        assertTrue(eventos.isEmpty())
    }

    @Test
    fun `quitar un ejercicio lo elimina de la lista`() = runTest {
        val vm = crearVm()
        vm.guardarLinea(linea(dia = 1))
        vm.guardarLinea(linea(dia = 3))

        vm.quitarLinea(0)

        assertEquals(listOf(linea(dia = 3)), vm.uiState.value.lineas)
    }

    @Test
    fun `quitar el ejercicio que se estaba modificando cancela la modificacion`() = runTest {
        val vm = crearVm()
        vm.guardarLinea(linea(dia = 1))
        vm.guardarLinea(linea(dia = 3))
        vm.editarLinea(1)
        val eventos = eventosNuevos(vm)

        vm.quitarLinea(1)

        assertNull(vm.uiState.value.editandoIndice)
        assertEquals(listOf<RutinaFormEvento>(RutinaFormEvento.LimpiarEditor), eventos)
    }

    @Test
    fun `quitar un ejercicio anterior al que se modifica corre su posicion`() = runTest {
        val vm = crearVm()
        vm.guardarLinea(linea(dia = 1))
        vm.guardarLinea(linea(dia = 3))
        vm.editarLinea(1)

        vm.quitarLinea(0)

        assertEquals(0, vm.uiState.value.editandoIndice)
    }

    @Test
    fun `quitar un ejercicio posterior al que se modifica no cambia su posicion`() = runTest {
        val vm = crearVm()
        vm.guardarLinea(linea(dia = 1))
        vm.guardarLinea(linea(dia = 3))
        vm.editarLinea(0)

        vm.quitarLinea(1)

        assertEquals(0, vm.uiState.value.editandoIndice)
    }

    @Test
    fun `quitar un indice que no existe no cambia la lista`() = runTest {
        val vm = crearVm()
        vm.guardarLinea(linea())

        vm.quitarLinea(5)

        assertEquals(1, vm.uiState.value.lineas.size)
    }

    // ======================================================================
    // Confirmación y guardado
    // ======================================================================

    @Test
    fun `al pedir confirmacion avisa la rutina activa del cliente que se finalizara`() = runTest {
        val vm = crearVm()
        vm.seleccionarCliente(camila)
        `when`(repo.rutinaActivaDeCliente(7L)).thenReturn("Hipertrofia Fase 2")
        val eventos = eventosDe(vm)

        vm.pedirConfirmacion("Fuerza base")

        assertEquals(
            listOf<RutinaFormEvento>(RutinaFormEvento.Confirmar("Fuerza base", camila, "Hipertrofia Fase 2")),
            eventos
        )
    }

    @Test
    fun `al pedir confirmacion de un cliente sin rutina activa no hay nada que avisar`() = runTest {
        val vm = crearVm()
        vm.seleccionarCliente(camila)
        `when`(repo.rutinaActivaDeCliente(7L)).thenReturn(null)
        val eventos = eventosDe(vm)

        vm.pedirConfirmacion("Fuerza base")

        assertEquals(listOf<RutinaFormEvento>(RutinaFormEvento.Confirmar("Fuerza base", camila, null)), eventos)
    }

    @Test
    fun `si no se pudo consultar la rutina activa igual pide confirmacion`() = runTest {
        val vm = crearVm()
        vm.seleccionarCliente(camila)
        `when`(repo.rutinaActivaDeCliente(7L)).thenThrow(IllegalStateException("fallo"))
        val eventos = eventosDe(vm)

        vm.pedirConfirmacion("Fuerza base")

        assertEquals(listOf<RutinaFormEvento>(RutinaFormEvento.Confirmar("Fuerza base", camila, null)), eventos)
    }

    @Test
    fun `al editar una rutina que ya tiene cliente no consulta su rutina activa`() = runTest {
        val vm = editarVm(cliente = camila)
        val eventos = eventosDe(vm)

        vm.pedirConfirmacion("Hipertrofia Fase 2")

        verify(repo, never()).rutinaActivaDeCliente(7L)
        assertEquals(
            RutinaFormEvento.Confirmar("Hipertrofia Fase 2", camila, null),
            eventos.last()
        )
    }

    @Test
    fun `al editar una rutina sin cliente y elegir uno si consulta su rutina activa`() = runTest {
        val vm = editarVm(cliente = null)
        vm.seleccionarCliente(camila)
        `when`(repo.rutinaActivaDeCliente(7L)).thenReturn("Rutina vieja")
        val eventos = eventosDe(vm)

        vm.pedirConfirmacion("Hipertrofia Fase 2")

        assertEquals(RutinaFormEvento.Confirmar("Hipertrofia Fase 2", camila, "Rutina vieja"), eventos.last())
    }

    private val datosEsperados = DatosRutina(
        nombre = "Fuerza base",
        objetivo = "Fuerza",
        nivel = "Intermedio",
        notas = "Descansar 90 s",
        clienteId = 7L,
        lineas = listOf(linea())
    )

    @Test
    fun `guardar en modo crear manda los datos recortados y avisa que se creo`() = runTest {
        val vm = crearVm()
        vm.seleccionarCliente(camila)
        vm.guardarLinea(linea())
        `when`(repo.crear(ENTRENADOR, datosEsperados)).thenReturn(ResultadoGuardado.Exito)
        val eventos = eventosDe(vm)

        vm.guardar("  Fuerza base  ", "Fuerza", "Intermedio", "  Descansar 90 s ")

        assertEquals(RutinaFormEvento.Guardado(R.string.form_creada_ok), eventos.last())
        assertFalse(vm.uiState.value.guardando)
    }

    @Test
    fun `guardar con notas en blanco manda notas nulas`() = runTest {
        val vm = crearVm()
        vm.seleccionarCliente(camila)
        vm.guardarLinea(linea())
        `when`(repo.crear(ENTRENADOR, datosEsperados.copy(notas = null))).thenReturn(ResultadoGuardado.Exito)

        vm.guardar("Fuerza base", "Fuerza", "Intermedio", "    ")

        verify(repo).crear(ENTRENADOR, datosEsperados.copy(notas = null))
    }

    /** Guarda con el resultado indicado y devuelve el último evento que recibió la pantalla. */
    private suspend fun TestScope.guardarConResultado(resultado: ResultadoGuardado): RutinaFormEvento {
        val vm = crearVm()
        vm.seleccionarCliente(camila)
        vm.guardarLinea(linea())
        `when`(repo.crear(ENTRENADOR, datosEsperados)).thenReturn(resultado)
        val eventos = eventosDe(vm)

        vm.guardar("Fuerza base", "Fuerza", "Intermedio", "Descansar 90 s")

        assertFalse(vm.uiState.value.guardando)
        return eventos.last()
    }

    @Test
    fun `guardar con nombre duplicado lo avisa para marcar el campo`() = runTest {
        assertEquals(RutinaFormEvento.NombreDuplicado, guardarConResultado(ResultadoGuardado.NombreDuplicado))
    }

    @Test
    fun `guardar con cliente invalido muestra el mensaje`() = runTest {
        assertEquals(
            RutinaFormEvento.Mensaje(R.string.form_cliente_invalido),
            guardarConResultado(ResultadoGuardado.ClienteInvalido)
        )
    }

    @Test
    fun `guardar una rutina que ya no esta disponible muestra el mensaje`() = runTest {
        assertEquals(
            RutinaFormEvento.Mensaje(R.string.form_no_disponible),
            guardarConResultado(ResultadoGuardado.NoDisponible)
        )
    }

    @Test
    fun `guardar con error muestra el mensaje de error`() = runTest {
        assertEquals(
            RutinaFormEvento.Mensaje(R.string.form_error_guardar),
            guardarConResultado(ResultadoGuardado.Error)
        )
    }

    @Test
    fun `guardar en modo editar actualiza la rutina con su cliente y avisa que se actualizo`() = runTest {
        val vm = editarVm(cliente = camila, lineas = listOf(linea()))
        `when`(repo.actualizar(5L, ENTRENADOR, datosEsperados.copy(nombre = "Hipertrofia Fase 2")))
            .thenReturn(ResultadoGuardado.Exito)
        val eventos = eventosDe(vm)

        vm.guardar("Hipertrofia Fase 2", "Fuerza", "Intermedio", "Descansar 90 s")

        assertEquals(RutinaFormEvento.Guardado(R.string.form_actualizada_ok), eventos.last())
        verify(repo, never()).crear(ENTRENADOR, datosEsperados)
    }

    private companion object {
        const val ENTRENADOR = 1L
    }
}
