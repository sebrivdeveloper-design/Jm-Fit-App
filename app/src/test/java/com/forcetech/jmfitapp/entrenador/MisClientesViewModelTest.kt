package com.forcetech.jmfitapp.entrenador

import com.forcetech.jmfitapp.data.dao.ClienteDao
import com.forcetech.jmfitapp.data.dao.ClienteDeEntrenadorFila
import com.forcetech.jmfitapp.data.repository.ClienteRepository
import com.forcetech.jmfitapp.model.EstadoUsuario
import com.forcetech.jmfitapp.ui.trainer.MisClientesViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

/**
 * HU-51 y HU-57 (pestaña "Mis clientes" del entrenador): carga, búsqueda y total de clientes activos.
 *
 * Nota: [MisClientesViewModel] escribe los fallos con `android.util.Log`. Para que esa llamada no
 * falle en una prueba local, `app/build.gradle.kts` debe tener `unitTests.isReturnDefaultValues = true`.
 */
@RunWith(MockitoJUnitRunner.StrictStubs::class)
class MisClientesViewModelTest {

    @get:Rule
    val principal = MainDispatcherRule()

    @Mock private lateinit var repo: ClienteRepository

    private fun cliente(id: Long, nombre: String, estado: String = EstadoUsuario.ACTIVO, rutina: String? = null) =
        ClienteDeEntrenadorFila(id, nombre, nombre.lowercase().replace(" ", "."), estado, rutina)

    private val camila = cliente(1, "Camila Restrepo", rutina = "Hipertrofia Fase 2")
    private val felipe = cliente(2, "Felipe Ramírez")
    private val pedro = cliente(3, "Pedro Inactivo", estado = EstadoUsuario.INHABILITADO)

    private fun crearViewModel(entrenadorId: Long? = ENTRENADOR) = MisClientesViewModel(repo, entrenadorId)

    // ---------- Carga ----------

    @Test
    fun `antes de cargar el estado esta cargando y sin clientes`() {
        val estado = crearViewModel().uiState.value

        assertTrue(estado.cargando)
        assertTrue(estado.todos.isEmpty())
        assertFalse(estado.error)
    }

    @Test
    fun `cargar muestra los clientes del entrenador con sesion`() = runTest {
        `when`(repo.clientesDeEntrenador(ENTRENADOR)).thenReturn(listOf(camila, felipe))
        val vm = crearViewModel()

        vm.cargar()

        val estado = vm.uiState.value
        assertEquals(listOf(camila, felipe), estado.todos)
        assertFalse(estado.cargando)
        assertFalse(estado.error)
    }

    @Test
    fun `cargar sin entrenador con sesion deja la lista vacia sin consultar`() = runTest {
        val vm = crearViewModel(entrenadorId = null)

        vm.cargar()

        assertTrue(vm.uiState.value.todos.isEmpty())
        assertFalse(vm.uiState.value.cargando)
        verifyNoInteractions(repo)
    }

    @Test
    fun `un entrenador sin clientes termina de cargar con lista vacia`() = runTest {
        `when`(repo.clientesDeEntrenador(ENTRENADOR)).thenReturn(emptyList())
        val vm = crearViewModel()

        vm.cargar()

        assertTrue(vm.uiState.value.todos.isEmpty())
        assertFalse(vm.uiState.value.cargando)
        assertEquals(0, vm.uiState.value.totalActivos)
    }

    @Test
    fun `si la carga falla el estado indica error y deja de cargar`() = runTest {
        `when`(repo.clientesDeEntrenador(ENTRENADOR)).thenThrow(IllegalStateException("fallo"))
        val vm = crearViewModel()

        vm.cargar()

        assertTrue(vm.uiState.value.error)
        assertFalse(vm.uiState.value.cargando)
    }

    @Test
    fun `volver a cargar despues de un error limpia el error y muestra los clientes`() = runTest {
        `when`(repo.clientesDeEntrenador(ENTRENADOR))
            .thenThrow(IllegalStateException("fallo"))
            .thenReturn(listOf(camila))
        val vm = crearViewModel()

        vm.cargar()
        vm.cargar()

        assertFalse(vm.uiState.value.error)
        assertEquals(listOf(camila), vm.uiState.value.todos)
    }

    @Test
    fun `volver a cargar refleja los cambios recientes`() = runTest {
        `when`(repo.clientesDeEntrenador(ENTRENADOR))
            .thenReturn(listOf(camila))
            .thenReturn(listOf(camila, felipe))
        val vm = crearViewModel()

        vm.cargar()
        vm.cargar()

        assertEquals(listOf(camila, felipe), vm.uiState.value.todos)
        assertFalse(vm.uiState.value.cargando)
        verify(repo, org.mockito.Mockito.times(2)).clientesDeEntrenador(ENTRENADOR)
    }

    // ---------- Búsqueda (HU-51) ----------

    private suspend fun viewModelConClientes(): MisClientesViewModel {
        `when`(repo.clientesDeEntrenador(ENTRENADOR)).thenReturn(listOf(camila, felipe, pedro))
        return crearViewModel().also { it.cargar() }
    }

    @Test
    fun `sin texto de busqueda se ven todos los clientes`() = runTest {
        val vm = viewModelConClientes()

        assertEquals(listOf(camila, felipe, pedro), vm.uiState.value.visibles)
    }

    @Test
    fun `buscar filtra por parte del nombre sin distinguir mayusculas ni tildes`() = runTest {
        val vm = viewModelConClientes()

        vm.buscar("RAMIREZ")
        assertEquals(listOf(felipe), vm.uiState.value.visibles)

        vm.buscar("  cami ")
        assertEquals(listOf(camila), vm.uiState.value.visibles)
    }

    @Test
    fun `buscar algo que no existe deja la lista vacia`() = runTest {
        val vm = viewModelConClientes()

        vm.buscar("zzz")

        assertTrue(vm.uiState.value.visibles.isEmpty())
        // Los clientes cargados no se pierden: al borrar la búsqueda vuelven a verse
        vm.buscar("")
        assertEquals(3, vm.uiState.value.visibles.size)
    }

    @Test
    fun `buscar con el mismo texto no cambia el estado`() = runTest {
        val vm = viewModelConClientes()
        vm.buscar("cami")
        val antes = vm.uiState.value

        vm.buscar("cami")

        assertEquals(antes, vm.uiState.value)
    }

    @Test
    fun `una busqueda recargada se conserva despues de volver a cargar`() = runTest {
        val vm = viewModelConClientes()
        vm.buscar("felipe")

        vm.cargar()

        assertEquals("felipe", vm.uiState.value.texto)
        assertEquals(listOf(felipe), vm.uiState.value.visibles)
    }

    // ---------- Total de activos (HU-57) ----------

    @Test
    fun `el total cuenta solo las cuentas activas`() = runTest {
        val vm = viewModelConClientes()

        assertEquals(2, vm.uiState.value.totalActivos)
    }

    @Test
    fun `los clientes inhabilitados se listan pero no cuentan en el total`() = runTest {
        val vm = viewModelConClientes()

        assertTrue(pedro in vm.uiState.value.visibles)
        assertEquals(2, vm.uiState.value.totalActivos)
    }

    @Test
    fun `buscar no cambia el total de activos`() = runTest {
        val vm = viewModelConClientes()

        vm.buscar("felipe")

        assertEquals(2, vm.uiState.value.totalActivos)
    }

    private companion object {
        const val ENTRENADOR = 1L
    }
}

/** [ClienteRepository.clientesDeEntrenador]: espera a la base de datos y devuelve lo que entrega el DAO. */
@RunWith(MockitoJUnitRunner.StrictStubs::class)
class ClienteRepositoryClientesDeEntrenadorTest {

    @Mock private lateinit var dao: ClienteDao

    private var esperas = 0
    private lateinit var repo: ClienteRepository

    @Before
    fun preparar() {
        repo = ClienteRepository(dao) { esperas++ }
    }

    @Test
    fun `devuelve los clientes que entrega el DAO para ese entrenador`() = runTest {
        val clientes = listOf(ClienteDeEntrenadorFila(1, "Camila Restrepo", "camila", EstadoUsuario.ACTIVO, null))
        `when`(dao.clientesDeEntrenador(1L)).thenReturn(clientes)

        assertEquals(clientes, repo.clientesDeEntrenador(1L))
        verify(dao).clientesDeEntrenador(1L)
    }

    @Test
    fun `espera a que la base de datos este lista antes de consultar`() = runTest {
        `when`(dao.clientesDeEntrenador(1L)).thenReturn(emptyList())

        repo.clientesDeEntrenador(1L)

        assertEquals(1, esperas)
    }

    @Test
    fun `un entrenador sin clientes devuelve lista vacia`() = runTest {
        `when`(dao.clientesDeEntrenador(2L)).thenReturn(emptyList())

        assertTrue(repo.clientesDeEntrenador(2L).isEmpty())
    }
}
