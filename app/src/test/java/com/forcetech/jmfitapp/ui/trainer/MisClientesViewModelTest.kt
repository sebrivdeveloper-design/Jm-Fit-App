package com.forcetech.jmfitapp.ui.trainer

import com.forcetech.jmfitapp.data.repository.ClienteRepository
import com.forcetech.jmfitapp.model.EstadoUsuario
import com.forcetech.jmfitapp.util.DatosPrueba
import com.forcetech.jmfitapp.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

/** HU-51 (lista y búsqueda de mis clientes) y HU-57 (total de activos), con el repositorio simulado. */
class MisClientesViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private lateinit var repo: ClienteRepository

    private val clientes = listOf(
        DatosPrueba.cliente(1, "Camila Restrepo", rutina = "Fuerza"),
        DatosPrueba.cliente(2, "Felipe Ramírez"),
        DatosPrueba.cliente(3, "Pedro Gómez", estado = "INHABILITADO")
    )

    @Before
    fun setUp() {
        repo = mock()
    }

    @Test
    fun estadoInicialEsCargando() {
        val vm = MisClientesViewModel(repo, 5L)

        assertTrue(vm.uiState.value.cargando)
    }

    @Test
    fun cargar_muestraLosClientesYCuentaSoloActivos() = runTest {
        whenever(repo.clientesDeEntrenador(5L)).thenReturn(clientes)

        val vm = MisClientesViewModel(repo, 5L)
        vm.cargar()

        val estado = vm.uiState.value
        assertFalse(estado.cargando)
        assertFalse(estado.error)
        assertEquals(3, estado.visibles.size)
        assertEquals(2, estado.totalActivos)
        verify(repo).clientesDeEntrenador(5L)
    }

    @Test
    fun cargar_sinClientes_listaVaciaYTotalCero() = runTest {
        whenever(repo.clientesDeEntrenador(5L)).thenReturn(emptyList())

        val vm = MisClientesViewModel(repo, 5L)
        vm.cargar()

        assertEquals(0, vm.uiState.value.totalActivos)
        assertTrue(vm.uiState.value.visibles.isEmpty())
    }

    @Test
    fun cargar_sinSesion_noConsultaElRepositorio() = runTest {
        val vm = MisClientesViewModel(repo, null)
        vm.cargar()

        assertFalse(vm.uiState.value.cargando)
        assertTrue(vm.uiState.value.todos.isEmpty())
        verifyNoInteractions(repo)
    }

    @Test
    fun cargar_siLaBaseFalla_muestraError() = runTest {
        whenever(repo.clientesDeEntrenador(5L)).thenThrow(IllegalStateException("BD no disponible"))

        val vm = MisClientesViewModel(repo, 5L)
        vm.cargar()

        assertTrue(vm.uiState.value.error)
        assertFalse(vm.uiState.value.cargando)
    }

    @Test
    fun recargar_actualizaElTotalDeActivos() = runTest {
        // HU-57: el total refleja cambios de estado recientes al volver a la pantalla
        whenever(repo.clientesDeEntrenador(5L))
            .thenReturn(clientes)
            .thenReturn(clientes.map { it.copy(estado = EstadoUsuario.ACTIVO) })

        val vm = MisClientesViewModel(repo, 5L)
        vm.cargar()
        assertEquals(2, vm.uiState.value.totalActivos)

        vm.cargar()
        assertEquals(3, vm.uiState.value.totalActivos)
        verify(repo, times(2)).clientesDeEntrenador(5L)
    }

    @Test
    fun buscar_filtraPorNombreSinTildesNiMayusculas() = runTest {
        whenever(repo.clientesDeEntrenador(5L)).thenReturn(clientes)
        val vm = MisClientesViewModel(repo, 5L)
        vm.cargar()

        vm.buscar("RAMIREZ")
        assertEquals(listOf(2L), vm.uiState.value.visibles.map { it.id })

        vm.buscar("gómez")
        assertEquals(listOf(3L), vm.uiState.value.visibles.map { it.id })

        vm.buscar("")
        assertEquals(3, vm.uiState.value.visibles.size)
    }

    @Test
    fun buscar_noCambiaElTotalDeActivos() = runTest {
        whenever(repo.clientesDeEntrenador(5L)).thenReturn(clientes)
        val vm = MisClientesViewModel(repo, 5L)
        vm.cargar()

        vm.buscar("zzz")

        assertTrue(vm.uiState.value.visibles.isEmpty())
        assertEquals(2, vm.uiState.value.totalActivos)
    }
}
