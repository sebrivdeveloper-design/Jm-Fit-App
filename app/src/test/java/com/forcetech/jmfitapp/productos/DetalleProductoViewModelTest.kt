package com.forcetech.jmfitapp.productos

import com.forcetech.jmfitapp.MainDispatcherRule
import com.forcetech.jmfitapp.data.repository.CarritoRepository
import com.forcetech.jmfitapp.data.repository.CatalogoRepository
import com.forcetech.jmfitapp.model.CarritoItem
import com.forcetech.jmfitapp.model.ProductoDetalle
import com.forcetech.jmfitapp.ui.store.DetalleProductoViewModel
import com.forcetech.jmfitapp.ui.store.TiendaEvento
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.lenient
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import java.math.BigDecimal

/** Estado, cantidad y eventos de la pantalla de detalle con repositorios simulados. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner.StrictStubs::class)
class DetalleProductoViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    @Mock private lateinit var catalogo: CatalogoRepository
    @Mock private lateinit var carrito: CarritoRepository

    private val whey = ProductoDetalle(
        id = 1, nombre = "Whey Isolate", marca = "JM", presentacion = "900 g",
        precio = BigDecimal("149900"), objetivo = "Recuperación",
        descripcion = "Proteína aislada", beneficios = "Rápida absorción",
        imagen = null, calificacion = 4.9, disponible = true, categoriaNombre = "Proteínas"
    )

    @Before
    fun preparar() {
        // El ViewModel llama a observarItems() al construirse.
        lenient().`when`(carrito.observarItems()).thenReturn(flowOf(emptyList()))
    }

    private fun crearViewModel() = DetalleProductoViewModel(catalogo, carrito, productoId = 1)

    private fun item(id: Long, cantidad: Int) = CarritoItem(
        productoId = id, nombre = "Producto $id", presentacion = "1 kg",
        precioUnitario = 1_000L, cantidad = cantidad
    )

    @Test
    fun `al abrir carga el producto con cantidad 1`() = runTest {
        `when`(catalogo.detalle(1)).thenReturn(whey)
        val vm = crearViewModel()

        assertTrue(vm.uiState.value.cargando)

        advanceUntilIdle()

        val estado = vm.uiState.value
        assertFalse(estado.cargando)
        assertFalse(estado.noEncontrado)
        assertEquals(whey, estado.producto)
        assertEquals(1, estado.cantidad)
        assertEquals(149_900L, estado.total)
    }

    @Test
    fun `si el producto no existe marca no encontrado`() = runTest {
        `when`(catalogo.detalle(1)).thenReturn(null)
        val vm = crearViewModel()

        advanceUntilIdle()

        val estado = vm.uiState.value
        assertFalse(estado.cargando)
        assertTrue(estado.noEncontrado)
        assertNull(estado.producto)
        assertEquals(0L, estado.total)
    }

    @Test
    fun `si falla la carga marca no encontrado`() = runTest {
        `when`(catalogo.detalle(1)).thenThrow(RuntimeException("fallo"))
        val vm = crearViewModel()

        advanceUntilIdle()

        val estado = vm.uiState.value
        assertFalse(estado.cargando)
        assertTrue(estado.noEncontrado)
        assertNull(estado.producto)
    }

    @Test
    fun `aumentar suma una unidad y recalcula el total`() = runTest {
        `when`(catalogo.detalle(1)).thenReturn(whey)
        val vm = crearViewModel()
        advanceUntilIdle()

        vm.aumentar()
        vm.aumentar()

        assertEquals(3, vm.uiState.value.cantidad)
        assertEquals(449_700L, vm.uiState.value.total)
    }

    @Test
    fun `aumentar no pasa del maximo permitido`() = runTest {
        `when`(catalogo.detalle(1)).thenReturn(whey)
        val vm = crearViewModel()
        advanceUntilIdle()

        repeat(15) { vm.aumentar() }

        assertEquals(DetalleProductoViewModel.MAX_CANTIDAD, vm.uiState.value.cantidad)
    }

    @Test
    fun `disminuir resta una unidad`() = runTest {
        `when`(catalogo.detalle(1)).thenReturn(whey)
        val vm = crearViewModel()
        advanceUntilIdle()

        vm.aumentar()
        vm.aumentar()
        vm.disminuir()

        assertEquals(2, vm.uiState.value.cantidad)
    }

    @Test
    fun `disminuir no baja de 1`() = runTest {
        `when`(catalogo.detalle(1)).thenReturn(whey)
        val vm = crearViewModel()
        advanceUntilIdle()

        vm.disminuir()
        vm.disminuir()

        assertEquals(1, vm.uiState.value.cantidad)
    }

    @Test
    fun `agregar al carrito envia la cantidad elegida y avisa con el nombre`() = runTest {
        `when`(catalogo.detalle(1)).thenReturn(whey)
        val vm = crearViewModel()
        advanceUntilIdle()
        vm.aumentar()
        vm.aumentar()

        vm.agregarAlCarrito()
        advanceUntilIdle()

        verify(carrito).agregar(1L, "Whey Isolate", "900 g", 149_900L, null, 3)
        assertEquals(TiendaEvento.ProductoAgregado("Whey Isolate"), vm.eventos.first())
    }

    @Test
    fun `un producto no disponible no se agrega al carrito`() = runTest {
        `when`(catalogo.detalle(1)).thenReturn(whey.copy(disponible = false))
        val vm = crearViewModel()
        advanceUntilIdle()

        vm.agregarAlCarrito()
        advanceUntilIdle()

        verify(carrito, never()).agregar(1L, "Whey Isolate", "900 g", 149_900L, null, 1)
    }

    @Test
    fun `agregar antes de que cargue el producto no hace nada`() = runTest {
        `when`(catalogo.detalle(1)).thenReturn(whey)
        val vm = crearViewModel()

        vm.agregarAlCarrito() // el producto todavía es null
        advanceUntilIdle()

        verify(carrito, never()).agregar(1L, "Whey Isolate", "900 g", 149_900L, null, 1)
    }

    @Test
    fun `si el carrito falla avisa con el evento de error`() = runTest {
        `when`(catalogo.detalle(1)).thenReturn(whey)
        `when`(carrito.agregar(1L, "Whey Isolate", "900 g", 149_900L, null, 1))
            .thenThrow(RuntimeException("sin espacio"))
        val vm = crearViewModel()
        advanceUntilIdle()

        vm.agregarAlCarrito()
        advanceUntilIdle()

        assertEquals(TiendaEvento.ErrorAlAgregar, vm.eventos.first())
    }

    @Test
    fun `el contador del carrito suma las cantidades de todos los items`() = runTest {
        `when`(carrito.observarItems()).thenReturn(flowOf(listOf(item(1, 2), item(2, 3))))
        `when`(catalogo.detalle(1)).thenReturn(whey)
        val vm = crearViewModel()

        backgroundScope.launch { vm.unidadesCarrito.collect { } }
        advanceUntilIdle()

        assertEquals(5, vm.unidadesCarrito.value)
    }
}