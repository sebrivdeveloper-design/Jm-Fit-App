package com.forcetech.jmfitapp.entrenador

import com.forcetech.jmfitapp.R
import com.forcetech.jmfitapp.data.dao.RutinaResumen
import com.forcetech.jmfitapp.data.repository.RutinaRepository
import com.forcetech.jmfitapp.model.EstadoRutina
import com.forcetech.jmfitapp.model.TipoRutina
import com.forcetech.jmfitapp.ui.trainer.PestanaRutinas
import com.forcetech.jmfitapp.ui.trainer.RutinasViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

/** ENT-6 (listado, pestañas y búsqueda) y ENT-7 (mensajes al inhabilitar o reactivar) en [RutinasViewModel]. */
@RunWith(MockitoJUnitRunner.StrictStubs::class)
class RutinasViewModelTest {

    @get:Rule
    val principal = MainDispatcherRule()

    @Mock private lateinit var repo: RutinaRepository

    private fun resumen(
        id: Long,
        nombre: String,
        objetivo: String? = "Hipertrofia",
        nivel: String? = "Intermedio",
        cliente: String? = null,
        estado: String = EstadoRutina.ACTIVA,
        tipo: String = TipoRutina.PERSONALIZADA
    ) = RutinaResumen(id, nombre, tipo, objetivo, nivel, estado, 4, cliente)

    private fun crearViewModel() = RutinasViewModel(repo, ENTRENADOR)

    // ---------- ENT-6: listado ----------

    @Test
    fun `al abrir muestra las rutinas personalizadas del entrenador`() = runTest {
        val lista = listOf(resumen(1, "Hipertrofia Fase 2", cliente = "Camila Restrepo"), resumen(2, "Recomposición"))
        `when`(repo.observarPersonalizadas(ENTRENADOR)).thenReturn(flowOf(lista))

        val estado = crearViewModel().uiState.first { !it.cargando }

        assertEquals(PestanaRutinas.PERSONALIZADAS, estado.pestana)
        assertEquals(lista, estado.rutinas)
        assertEquals(2, estado.totalSinFiltrar)
        assertFalse(estado.error)
    }

    @Test
    fun `al elegir la pestana estandar muestra las rutinas estandar`() = runTest {
        val estandar = listOf(resumen(10, "Full body", tipo = TipoRutina.ESTANDAR))
        `when`(repo.observarEstandar()).thenReturn(flowOf(estandar))
        val vm = crearViewModel()

        vm.seleccionarPestana(PestanaRutinas.ESTANDAR)
        val estado = vm.uiState.first { !it.cargando }

        assertEquals(PestanaRutinas.ESTANDAR, estado.pestana)
        assertEquals(estandar, estado.rutinas)
    }

    @Test
    fun `una pestana sin rutinas queda vacia sin error`() = runTest {
        `when`(repo.observarPersonalizadas(ENTRENADOR)).thenReturn(flowOf(emptyList()))

        val estado = crearViewModel().uiState.first { !it.cargando }

        assertTrue(estado.rutinas.isEmpty())
        assertEquals(0, estado.totalSinFiltrar)
        assertFalse(estado.error)
    }

    @Test
    fun `si la consulta falla el estado indica error`() = runTest {
        `when`(repo.observarPersonalizadas(ENTRENADOR)).thenReturn(flow { throw IllegalStateException("fallo") })

        val estado = crearViewModel().uiState.first { !it.cargando }

        assertTrue(estado.error)
        assertTrue(estado.rutinas.isEmpty())
    }

    // ---------- Búsqueda ----------

    private val camila = resumen(1, "Hipertrofia Fase 2", objetivo = "Hipertrofia", nivel = "Intermedio", cliente = "Camila Restrepo")
    private val felipe = resumen(2, "Recomposición corporal", objetivo = "Pérdida de grasa", nivel = "Avanzado", cliente = "Felipe Ramírez")

    private suspend fun buscar(texto: String): List<RutinaResumen> {
        `when`(repo.observarPersonalizadas(ENTRENADOR)).thenReturn(flowOf(listOf(camila, felipe)))
        val vm = crearViewModel()
        vm.buscar(texto)
        return vm.uiState.first { !it.cargando && it.busqueda == texto }.rutinas
    }

    @Test
    fun `busca por nombre sin distinguir mayusculas ni tildes`() = runTest {
        assertEquals(listOf(felipe), buscar("RECOMPOSICION"))
    }

    @Test
    fun `busca por objetivo`() = runTest {
        assertEquals(listOf(felipe), buscar("perdida"))
    }

    @Test
    fun `busca por nivel`() = runTest {
        assertEquals(listOf(felipe), buscar("avanzado"))
    }

    @Test
    fun `busca por nombre del cliente asignado`() = runTest {
        assertEquals(listOf(camila), buscar("restrepo"))
    }

    @Test
    fun `una busqueda vacia o con espacios muestra todas`() = runTest {
        assertEquals(listOf(camila, felipe), buscar("   "))
    }

    @Test
    fun `una busqueda sin coincidencias deja la lista vacia pero conserva el total`() = runTest {
        `when`(repo.observarPersonalizadas(ENTRENADOR)).thenReturn(flowOf(listOf(camila, felipe)))
        val vm = crearViewModel()
        vm.buscar("zzz")

        val estado = vm.uiState.first { !it.cargando && it.busqueda == "zzz" }

        assertTrue(estado.rutinas.isEmpty())
        assertEquals(2, estado.totalSinFiltrar)
    }

    @Test
    fun `una rutina sin cliente no rompe la busqueda por cliente`() = runTest {
        `when`(repo.observarPersonalizadas(ENTRENADOR)).thenReturn(flowOf(listOf(resumen(3, "Sin cliente", cliente = null))))
        val vm = crearViewModel()
        vm.buscar("camila")

        val estado = vm.uiState.first { !it.cargando && it.busqueda == "camila" }

        assertTrue(estado.rutinas.isEmpty())
    }

    // ---------- ENT-7: inhabilitar y reactivar ----------

    @Test
    fun `inhabilitar una rutina activa pide desactivarla y avisa que se inhabilito`() = runTest {
        val activa = resumen(5, "Hipertrofia Fase 2", estado = EstadoRutina.ACTIVA)
        `when`(repo.cambiarEstado(5L, ENTRENADOR, false)).thenReturn(true)
        val vm = crearViewModel()

        vm.cambiarEstado(activa)

        assertEquals(R.string.rutinas_inhabilitada_ok, vm.mensajes.first())
        verify(repo).cambiarEstado(5L, ENTRENADOR, false)
    }

    @Test
    fun `reactivar una rutina inhabilitada pide activarla y avisa que se reactivo`() = runTest {
        val inhabilitada = resumen(5, "Hipertrofia Fase 2", estado = EstadoRutina.INHABILITADA)
        `when`(repo.cambiarEstado(5L, ENTRENADOR, true)).thenReturn(true)
        val vm = crearViewModel()

        vm.cambiarEstado(inhabilitada)

        assertEquals(R.string.rutinas_reactivada_ok, vm.mensajes.first())
    }

    @Test
    fun `si el repositorio no pudo cambiar el estado avisa del error`() = runTest {
        `when`(repo.cambiarEstado(5L, ENTRENADOR, false)).thenReturn(false)
        val vm = crearViewModel()

        vm.cambiarEstado(resumen(5, "Ajena", estado = EstadoRutina.ACTIVA))

        assertEquals(R.string.rutinas_error_estado, vm.mensajes.first())
    }

    @Test
    fun `si el repositorio lanza una excepcion avisa del error`() = runTest {
        `when`(repo.cambiarEstado(5L, ENTRENADOR, false)).thenThrow(IllegalStateException("fallo"))
        val vm = crearViewModel()

        vm.cambiarEstado(resumen(5, "Rutina", estado = EstadoRutina.ACTIVA))

        assertEquals(R.string.rutinas_error_estado, vm.mensajes.first())
    }

    private companion object {
        const val ENTRENADOR = 1L
    }
}
