package com.forcetech.jmfitapp.productos

import com.forcetech.jmfitapp.MainDispatcherRule
import com.forcetech.jmfitapp.data.repository.CarritoRepository
import com.forcetech.jmfitapp.data.repository.CatalogoRepository
import com.forcetech.jmfitapp.model.CarritoItem
import com.forcetech.jmfitapp.model.CategoriaProducto
import com.forcetech.jmfitapp.model.ProductoResumen
import com.forcetech.jmfitapp.ui.store.CatalogoViewModel
import com.forcetech.jmfitapp.ui.store.TiendaEvento
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.lenient
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import java.math.BigDecimal

/** Estado y eventos del catálogo con repositorios simulados; sin base de datos ni pantalla. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner.StrictStubs::class)
class CatalogoViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    @Mock private lateinit var catalogo: CatalogoRepository
    @Mock private lateinit var carrito: CarritoRepository

    private val categorias = listOf(CategoriaProducto(1, "Proteínas"), CategoriaProducto(2, "Creatinas"))

    private val whey = ProductoResumen(
        id = 1, nombre = "Whey Isolate", presentacion = "900 g",
        precio = BigDecimal("149900"), imagen = null, calificacion = 4.9,
        disponible = true, categoriaNombre = "Proteínas"
    )
    private val creatina = ProductoResumen(
        id = 2, nombre = "Creatina Mono", presentacion = "300 g",
        precio = BigDecimal("89900"), imagen = null, calificacion = 4.7,
        disponible = true, categoriaNombre = "Creatinas"
    )

    /** Valores por defecto (lenient): cada prueba puede cambiarlos antes de crear el ViewModel. */
    @Before
    fun preparar() {
        runBlocking {
            lenient().`when`(catalogo.categorias()).thenReturn(categorias)
            lenient().`when`(catalogo.productos(null, "")).thenReturn(listOf(whey, creatina))
        }
        lenient().`when`(carrito.observarItems()).thenReturn(flowOf(emptyList()))
    }

    private fun crearViewModel() = CatalogoViewModel(catalogo, carrito)

    private fun item(id: Long, cantidad: Int) = CarritoItem(
        productoId = id, nombre = "Producto $id", presentacion = "1 kg",
        precioUnitario = 1_000L, cantidad = cantidad
    )

    @Test
    fun `al abrir carga categorias y productos`() = runTest {
        val vm = crearViewModel()

        assertTrue(vm.uiState.value.cargando)
        assertTrue(vm.uiState.value.productos.isEmpty())

        advanceUntilIdle()

        val estado = vm.uiState.value
        assertFalse(estado.cargando)
        assertFalse(estado.error)
        assertEquals(categorias, estado.categorias)
        assertEquals(listOf(whey, creatina), estado.productos)
        assertEquals(null, estado.categoriaId)
    }

    @Test
    fun `seleccionar una categoria filtra los productos`() = runTest {
        `when`(catalogo.productos(2, "")).thenReturn(listOf(creatina))
        val vm = crearViewModel()
        advanceUntilIdle()

        vm.seleccionarCategoria(2)
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.categoriaId)
        assertEquals(listOf(creatina), vm.uiState.value.productos)
        verify(catalogo).productos(2, "")
    }

    @Test
    fun `seleccionar la categoria que ya esta activa no vuelve a consultar`() = runTest {
        val vm = crearViewModel()
        advanceUntilIdle()

        vm.seleccionarCategoria(null)
        advanceUntilIdle()

        verify(catalogo, times(1)).productos(null, "")
    }

    @Test
    fun `buscar guarda el texto al instante pero consulta despues de 300 ms`() = runTest {
        `when`(catalogo.productos(null, "whey")).thenReturn(listOf(whey))
        val vm = crearViewModel()
        advanceUntilIdle()

        vm.buscar("whey")
        assertEquals("whey", vm.uiState.value.texto)

        advanceTimeBy(299)
        runCurrent()
        verify(catalogo, never()).productos(null, "whey")

        advanceTimeBy(1)
        runCurrent()
        verify(catalogo).productos(null, "whey")
        assertEquals(listOf(whey), vm.uiState.value.productos)
    }

    @Test
    fun `si se sigue escribiendo solo se consulta el ultimo texto`() = runTest {
        `when`(catalogo.productos(null, "wh")).thenReturn(listOf(whey))
        val vm = crearViewModel()
        advanceUntilIdle()

        vm.buscar("w")
        advanceTimeBy(100)
        runCurrent()
        vm.buscar("wh")
        advanceUntilIdle()

        verify(catalogo, never()).productos(null, "w")
        verify(catalogo).productos(null, "wh")
        assertEquals(listOf(whey), vm.uiState.value.productos)
    }

    @Test
    fun `buscar el mismo texto no vuelve a consultar`() = runTest {
        val vm = crearViewModel()
        advanceUntilIdle()

        vm.buscar("")
        advanceUntilIdle()

        verify(catalogo, times(1)).productos(null, "")
    }

    @Test
    fun `si falla la consulta de productos muestra error y lista vacia`() = runTest {
        `when`(catalogo.productos(null, "")).thenThrow(RuntimeException("fallo"))
        val vm = crearViewModel()

        advanceUntilIdle()

        val estado = vm.uiState.value
        assertTrue(estado.error)
        assertFalse(estado.cargando)
        assertTrue(estado.productos.isEmpty())
    }

    @Test
    fun `si fallan las categorias el catalogo sigue mostrando productos`() = runTest {
        `when`(catalogo.categorias()).thenThrow(RuntimeException("fallo"))
        val vm = crearViewModel()

        advanceUntilIdle()

        assertTrue(vm.uiState.value.categorias.isEmpty())
        assertEquals(listOf(whey, creatina), vm.uiState.value.productos)
    }

    @Test
    fun `agregar al carrito suma una unidad y avisa con el nombre del producto`() = runTest {
        val vm = crearViewModel()
        advanceUntilIdle()

        vm.agregarAlCarrito(whey)
        advanceUntilIdle()

        verify(carrito).agregar(1L, "Whey Isolate", "900 g", 149_900L, null, 1)
        assertEquals(TiendaEvento.ProductoAgregado("Whey Isolate"), vm.eventos.first())
    }

    @Test
    fun `si el carrito falla avisa con el evento de error`() = runTest {
        `when`(carrito.agregar(1L, "Whey Isolate", "900 g", 149_900L, null, 1))
            .thenThrow(RuntimeException("sin espacio"))
        val vm = crearViewModel()
        advanceUntilIdle()

        vm.agregarAlCarrito(whey)
        advanceUntilIdle()

        assertEquals(TiendaEvento.ErrorAlAgregar, vm.eventos.first())
    }

    @Test
    fun `el contador del carrito suma las cantidades de todos los items`() = runTest {
        `when`(carrito.observarItems()).thenReturn(flowOf(listOf(item(1, 2), item(2, 3))))
        val vm = crearViewModel()

        backgroundScope.launch { vm.unidadesCarrito.collect { } }
        advanceUntilIdle()

        assertEquals(5, vm.unidadesCarrito.value)
    }
}