package com.forcetech.jmfitapp.carrito

import com.forcetech.jmfitapp.R
import com.forcetech.jmfitapp.data.repository.CarritoRepository
import com.forcetech.jmfitapp.model.CarritoItem
import com.forcetech.jmfitapp.model.ResumenCarrito
import com.forcetech.jmfitapp.ui.store.CarritoEvento
import com.forcetech.jmfitapp.ui.store.CarritoViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.lenient
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

/**
 * HU-18 (eliminar), HU-19 (cantidad) y HU-20 (total) a nivel de ViewModel, con el repositorio simulado.
 * No comprueba textos ni pantallas: verifica el estado y los eventos que la pantalla convierte en mensajes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner.StrictStubs::class)
class CarritoViewModelTest {

    @Mock private lateinit var repo: CarritoRepository

    private val whey = CarritoItem(1L, "Whey isolate", "900 g · Vainilla", 149_900L, null, 1, 0L)
    private val creatina = CarritoItem(2L, "Creatina mono", "300 g · Sin sabor", 89_900L, null, 2, 0L)

    @Before
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun crearVm(
        items: Flow<List<CarritoItem>> = flowOf(emptyList()),
        esSocio: Boolean = false
    ): CarritoViewModel {
        // lenient: hay pruebas que no leen el estado y no deben fallar por este stub sin usar
        lenient().`when`(repo.observarItems()).thenReturn(items)
        return CarritoViewModel(repo, esSocio)
    }

    /** uiState usa WhileSubscribed: necesita alguien suscrito para empezar a leer el carrito. */
    private fun TestScope.observarEstado(vm: CarritoViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }
    }

    private fun TestScope.recolectarEventos(vm: CarritoViewModel): List<CarritoEvento> {
        val recibidos = mutableListOf<CarritoEvento>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.eventos.toList(recibidos) }
        return recibidos
    }

    // ---------- HU-20: ver el total ----------

    @Test
    fun `al inicio el estado es cargando y vacio`() {
        val vm = crearVm()

        assertTrue(vm.uiState.value.cargando)
        assertTrue(vm.uiState.value.items.isEmpty())
        assertEquals(ResumenCarrito.VACIO, vm.uiState.value.resumen)
    }

    @Test
    fun `con productos muestra la lista y el desglose de un cliente externo`() = runTest {
        val vm = crearVm(flowOf(listOf(whey, creatina)), esSocio = false)
        observarEstado(vm)

        val estado = vm.uiState.value
        assertFalse(estado.cargando)
        assertFalse(estado.esSocio)
        assertEquals(listOf(whey, creatina), estado.items)
        assertEquals(3, estado.resumen.unidades)
        assertEquals(329_700L, estado.resumen.subtotal)
        assertEquals(0L, estado.resumen.descuento)
        assertEquals(12_000L, estado.resumen.envio)
        assertEquals(341_700L, estado.resumen.total)
    }

    @Test
    fun `un socio recibe el descuento en el desglose`() = runTest {
        val vm = crearVm(flowOf(listOf(whey, creatina)), esSocio = true)
        observarEstado(vm)

        val estado = vm.uiState.value
        assertTrue(estado.esSocio)
        assertEquals(329_700L, estado.resumen.subtotal)
        assertEquals(32_970L, estado.resumen.descuento)
        assertEquals(308_730L, estado.resumen.total)
    }

    @Test
    fun `carrito vacio muestra total cero y no cobra envio`() = runTest {
        val vm = crearVm(flowOf(emptyList()), esSocio = true)
        observarEstado(vm)

        val estado = vm.uiState.value
        assertFalse(estado.cargando)
        assertTrue(estado.items.isEmpty())
        assertEquals(ResumenCarrito.VACIO, estado.resumen)
        assertEquals(0L, estado.resumen.total)
    }

    @Test
    fun `el total se actualiza solo al agregar y al cambiar la cantidad`() = runTest {
        val carrito = MutableStateFlow(listOf(whey))
        val vm = crearVm(carrito)
        observarEstado(vm)
        assertEquals(161_900L, vm.uiState.value.resumen.total) // 149.900 + 12.000

        carrito.value = listOf(whey, creatina) // agregar
        assertEquals(341_700L, vm.uiState.value.resumen.total) // 329.700 + 12.000

        carrito.value = listOf(whey.copy(cantidad = 3), creatina) // modificar cantidad
        assertEquals(629_500L, vm.uiState.value.resumen.subtotal) // 449.700 + 179.800
        assertEquals(641_500L, vm.uiState.value.resumen.total)
    }

    @Test
    fun `si Room falla se muestra el carrito vacio y no se queda cargando`() = runTest {
        val vm = crearVm(flow { throw IllegalStateException("Room falló") })
        observarEstado(vm)

        assertFalse(vm.uiState.value.cargando)
        assertTrue(vm.uiState.value.items.isEmpty())
        assertEquals(ResumenCarrito.VACIO, vm.uiState.value.resumen)
    }

    // ---------- HU-18: eliminar ----------

    @Test
    fun `eliminar quita el producto y avisa con el producto eliminado`() = runTest {
        val vm = crearVm()
        val eventos = recolectarEventos(vm)

        vm.eliminar(whey)

        verify(repo).eliminar(1L)
        assertEquals(listOf<CarritoEvento>(CarritoEvento.Eliminado(whey)), eventos)
    }

    @Test
    fun `eliminar un producto no cambia las cantidades de los demas y actualiza el total`() = runTest {
        val carrito = MutableStateFlow(listOf(whey, creatina))
        val vm = crearVm(carrito)
        observarEstado(vm)

        carrito.value = listOf(creatina) // lo que emite Room tras eliminar Whey

        val estado = vm.uiState.value
        assertEquals(listOf(creatina), estado.items)
        assertEquals(2, estado.items.single().cantidad)
        assertEquals(179_800L, estado.resumen.subtotal)
        assertEquals(191_800L, estado.resumen.total)
    }

    @Test
    fun `eliminar el ultimo producto deja el carrito vacio con total cero`() = runTest {
        val carrito = MutableStateFlow(listOf(whey))
        val vm = crearVm(carrito)
        observarEstado(vm)

        carrito.value = emptyList()

        assertTrue(vm.uiState.value.items.isEmpty())
        assertEquals(ResumenCarrito.VACIO, vm.uiState.value.resumen)
    }

    @Test
    fun `si falla al eliminar avisa del error y no dice que se elimino`() = runTest {
        doThrow(IllegalStateException("Room falló")).`when`(repo).eliminar(1L)
        val vm = crearVm()
        val eventos = recolectarEventos(vm)

        vm.eliminar(whey)

        assertEquals(listOf<CarritoEvento>(CarritoEvento.Error(R.string.carrito_error)), eventos)
    }

    @Test
    fun `una cancelacion al eliminar no se muestra como error`() = runTest {
        doThrow(CancellationException("Pantalla cerrada")).`when`(repo).eliminar(1L)
        val vm = crearVm()
        val eventos = recolectarEventos(vm)

        vm.eliminar(whey)

        assertTrue(eventos.isEmpty())
    }

    @Test
    fun `deshacer vuelve a poner el producto tal como estaba`() = runTest {
        val vm = crearVm()

        vm.restaurar(creatina)

        verify(repo).restaurar(creatina)
    }

    @Test
    fun `si falla deshacer avisa del error`() = runTest {
        doThrow(IllegalStateException("Room falló")).`when`(repo).restaurar(creatina)
        val vm = crearVm()
        val eventos = recolectarEventos(vm)

        vm.restaurar(creatina)

        assertEquals(listOf<CarritoEvento>(CarritoEvento.Error(R.string.carrito_error)), eventos)
    }

    // ---------- HU-19: modificar cantidad ----------

    @Test
    fun `el boton mas suma una unidad a ese producto`() = runTest {
        val vm = crearVm()

        vm.sumar(creatina)

        verify(repo).ajustarCantidad(2L, 1)
        verify(repo, never()).eliminar(2L)
    }

    @Test
    fun `el boton menos resta una unidad y nunca elimina el producto`() = runTest {
        val vm = crearVm()

        vm.restar(creatina)

        verify(repo).ajustarCantidad(2L, -1)
        verify(repo, never()).eliminar(2L)
    }

    @Test
    fun `si falla al sumar avisa del error`() = runTest {
        doThrow(IllegalStateException("Room falló")).`when`(repo).ajustarCantidad(1L, 1)
        val vm = crearVm()
        val eventos = recolectarEventos(vm)

        vm.sumar(whey)

        assertEquals(listOf<CarritoEvento>(CarritoEvento.Error(R.string.carrito_error)), eventos)
    }

    @Test
    fun `si falla al restar avisa del error`() = runTest {
        doThrow(IllegalStateException("Room falló")).`when`(repo).ajustarCantidad(2L, -1)
        val vm = crearVm()
        val eventos = recolectarEventos(vm)

        vm.restar(creatina)

        assertEquals(listOf<CarritoEvento>(CarritoEvento.Error(R.string.carrito_error)), eventos)
    }

    // ---------- Factory ----------

    @Test
    fun `la fabrica crea el CarritoViewModel`() {
        lenient().`when`(repo.observarItems()).thenReturn(flowOf(emptyList()))

        val vm = CarritoViewModel.Factory(repo, true).create(CarritoViewModel::class.java)

        assertNotNull(vm)
        assertTrue(vm.uiState.value.esSocio)
    }
}