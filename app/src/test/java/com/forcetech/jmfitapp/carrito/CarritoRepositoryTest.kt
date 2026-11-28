package com.forcetech.jmfitapp.carrito

import com.forcetech.jmfitapp.data.dao.CarritoDao
import com.forcetech.jmfitapp.data.repository.CarritoRepository
import com.forcetech.jmfitapp.model.CarritoItem
import com.forcetech.jmfitapp.model.ReglasCarrito
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.junit.MockitoJUnitRunner

/** HU-18 (eliminar), HU-19 (cantidad) y HU-20 (carrito en vivo) a nivel de repositorio, con el DAO simulado. */
@RunWith(MockitoJUnitRunner.StrictStubs::class)
class CarritoRepositoryTest {

    @Mock private lateinit var dao: CarritoDao
    private lateinit var repository: CarritoRepository
    private var transacciones = 0

    private val whey = CarritoItem(
        productoId = 1L,
        nombre = "Whey isolate",
        presentacion = "900 g · Vainilla",
        precioUnitario = 149_900L,
        imagen = "prod_whey",
        cantidad = 1,
        agregadoEn = 0L
    )

    @Before
    fun preparar() {
        transacciones = 0
        // En lugar de Room.withTransaction, cuenta cuántas transacciones se abren y ejecuta el bloque.
        repository = CarritoRepository(dao) { bloque ->
            transacciones++
            bloque()
        }
    }

    // ---------- agregar ----------

    @Test
    fun `agregar un producto nuevo lo guarda con sus datos y la cantidad pedida`() = runTest {
        repository.agregar(1L, "Whey isolate", "900 g · Vainilla", 149_900L, "prod_whey", 2)

        val captor: ArgumentCaptor<CarritoItem> = ArgumentCaptor.forClass(CarritoItem::class.java)
        verify(dao).buscar(1L)
        verify(dao).guardar(capturar(captor))
        verifyNoMoreInteractions(dao)

        val guardado = captor.value
        assertEquals(1L, guardado.productoId)
        assertEquals("Whey isolate", guardado.nombre)
        assertEquals("900 g · Vainilla", guardado.presentacion)
        assertEquals(149_900L, guardado.precioUnitario)
        assertEquals("prod_whey", guardado.imagen)
        assertEquals(2, guardado.cantidad)
    }

    @Test
    fun `agregar un producto nuevo sin imagen ni cantidad usa una unidad`() = runTest {
        repository.agregar(1L, "Whey isolate", "900 g · Vainilla", 149_900L)

        val captor: ArgumentCaptor<CarritoItem> = ArgumentCaptor.forClass(CarritoItem::class.java)
        verify(dao).guardar(capturar(captor))

        assertNull(captor.value.imagen)
        assertEquals(1, captor.value.cantidad)
    }

    @Test
    fun `agregar un producto nuevo no permite pasar de la cantidad maxima`() = runTest {
        repository.agregar(1L, "Whey isolate", "900 g · Vainilla", 149_900L, null, 500)

        val captor: ArgumentCaptor<CarritoItem> = ArgumentCaptor.forClass(CarritoItem::class.java)
        verify(dao).guardar(capturar(captor))

        assertEquals(ReglasCarrito.CANTIDAD_MAXIMA, captor.value.cantidad)
    }

    @Test
    fun `agregar un producto que ya esta suma unidades y no lo duplica`() = runTest {
        `when`(dao.buscar(1L)).thenReturn(whey)

        repository.agregar(1L, "Whey isolate", "900 g · Vainilla", 149_900L, null, 3)

        verify(dao).buscar(1L)
        verify(dao).ajustarCantidad(1L, 3, ReglasCarrito.CANTIDAD_MAXIMA)
        verifyNoMoreInteractions(dao)
    }

    @Test
    fun `agregar corre dentro de una sola transaccion`() = runTest {
        repository.agregar(1L, "Whey isolate", "900 g · Vainilla", 149_900L)

        assertEquals(1, transacciones)
    }

    @Test
    fun `agregar con cantidad menor a uno se rechaza sin tocar el carrito`() = runTest {
        val error = runCatching {
            repository.agregar(1L, "Whey isolate", "900 g · Vainilla", 149_900L, cantidad = 0)
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        verifyNoInteractions(dao)
        assertEquals(0, transacciones)
    }

    // ---------- HU-19: modificar cantidad ----------

    @Test
    fun `sumar una unidad pide ajustar +1 con el limite maximo`() = runTest {
        repository.ajustarCantidad(1L, +1)

        verify(dao).ajustarCantidad(1L, 1, ReglasCarrito.CANTIDAD_MAXIMA)
        verifyNoMoreInteractions(dao)
    }

    @Test
    fun `restar una unidad pide ajustar -1 y no elimina el producto`() = runTest {
        repository.ajustarCantidad(1L, -1)

        verify(dao).ajustarCantidad(1L, -1, ReglasCarrito.CANTIDAD_MAXIMA)
        verifyNoMoreInteractions(dao) // no se llama a eliminar(): para quitarlo se usa la papelera
    }

    @Test
    fun `error al ajustar la cantidad se propaga al llamador`() = runTest {
        val error = IllegalStateException("Error de cantidad")
        doThrow(error).`when`(dao).ajustarCantidad(1L, 1, ReglasCarrito.CANTIDAD_MAXIMA)

        assertSame(error, runCatching { repository.ajustarCantidad(1L, 1) }.exceptionOrNull())
    }

    // ---------- HU-18: eliminar ----------

    @Test
    fun `eliminar quita solo el producto indicado`() = runTest {
        repository.eliminar(1L)

        verify(dao).eliminar(1L)
        verifyNoMoreInteractions(dao) // no vacia el carrito ni modifica otros productos
    }

    @Test
    fun `error al eliminar se propaga al llamador`() = runTest {
        val error = IllegalStateException("Error al eliminar")
        doThrow(error).`when`(dao).eliminar(1L)

        assertSame(error, runCatching { repository.eliminar(1L) }.exceptionOrNull())
    }

    @Test
    fun `restaurar vuelve a guardar el mismo producto tal como estaba`() = runTest {
        repository.restaurar(whey)

        verify(dao).guardar(whey)
        verifyNoMoreInteractions(dao)
    }

    @Test
    fun `vaciar borra todo el carrito`() = runTest {
        repository.vaciar()

        verify(dao).vaciar()
        verifyNoMoreInteractions(dao)
    }

    // ---------- HU-20: carrito en vivo ----------

    @Test
    fun `observarItems entrega la lista que emite el DAO`() = runTest {
        `when`(dao.observarItems()).thenReturn(flowOf(listOf(whey)))

        assertEquals(listOf(whey), repository.observarItems().first())
    }
}

/**
 * Mockito devuelve null al capturar y Kotlin no lo acepta en parámetros no nulos;
 * este ayudante registra el captor y devuelve un valor que Kotlin acepta.
 */
@Suppress("UNCHECKED_CAST")
private fun <T> capturar(captor: ArgumentCaptor<T>): T {
    captor.capture()
    return null as T
}