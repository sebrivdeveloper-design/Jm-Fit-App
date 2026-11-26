package com.forcetech.jmfitapp.productos

import com.forcetech.jmfitapp.data.dao.ProductoDao
import com.forcetech.jmfitapp.data.repository.ProductoRepository
import com.forcetech.jmfitapp.model.Producto
import com.forcetech.jmfitapp.ui.productos.CargaProductos
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import java.math.BigDecimal

/** Prueba el manejo de carga utilizado por la actividad, incluidos sus avisos de error. */
@RunWith(MockitoJUnitRunner.StrictStubs::class)
class CargaProductosTest {
    @Mock private lateinit var dao: ProductoDao
    private lateinit var carga: CargaProductos
    private val resultados = mutableListOf<List<Producto>>()
    private val errores = mutableListOf<Exception>()

    @Before
    fun preparar() {
        carga = CargaProductos(ProductoRepository(dao))
    }

    @Test
    fun `consulta exitosa entrega productos sin notificar error`() = runTest {
        val productos = listOf(Producto(1, "Proteína", "Fit", BigDecimal("15000"), "Recuperación", "500 g"))
        `when`(dao.obtenerProductosFiltrados("Fit", 1)).thenReturn(productos)

        carga.cargar("Fit", 1, resultados::add, errores::add)

        assertEquals(listOf(productos), resultados)
        assertTrue(errores.isEmpty())
        verify(dao).obtenerProductosFiltrados("Fit", 1)
    }

    @Test
    fun `consulta vacia entrega lista vacia sin notificar error`() = runTest {
        `when`(dao.obtenerProductosFiltrados("", 0)).thenReturn(emptyList())

        carga.cargar("", 0, resultados::add, errores::add)

        assertEquals(listOf(emptyList<Producto>()), resultados)
        assertTrue(errores.isEmpty())
    }

    @Test
    fun `fallo real notifica error sin entregar resultados`() = runTest {
        val error = IllegalStateException("Fallo de lectura")
        `when`(dao.obtenerProductosFiltrados("", 0)).thenThrow(error)

        carga.cargar("", 0, resultados::add, errores::add)

        assertEquals(listOf(error), errores)
        assertTrue(resultados.isEmpty())
    }

    @Test
    fun `cancelacion se propaga sin notificar error ni entregar resultados`() = runTest {
        val cancelacion = CancellationException("Consulta reemplazada")
        `when`(dao.obtenerProductosFiltrados("", 0)).thenThrow(cancelacion)

        val lanzada = runCatching {
            carga.cargar("", 0, resultados::add, errores::add)
        }.exceptionOrNull()

        assertSame(cancelacion, lanzada)
        assertTrue(errores.isEmpty())
        assertTrue(resultados.isEmpty())
    }

    @Test
    fun `consulta posterior a una cancelacion puede entregar resultados`() = runTest {
        `when`(dao.obtenerProductosFiltrados("", 0))
            .thenThrow(CancellationException("Consulta reemplazada"))
            .thenReturn(emptyList())
        runCatching { carga.cargar("", 0, resultados::add, errores::add) }

        carga.cargar("", 0, resultados::add, errores::add)

        assertEquals(listOf(emptyList<Producto>()), resultados)
        assertTrue(errores.isEmpty())
    }
}
