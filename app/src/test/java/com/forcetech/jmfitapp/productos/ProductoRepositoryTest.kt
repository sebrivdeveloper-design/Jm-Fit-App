package com.forcetech.jmfitapp.productos

import com.forcetech.jmfitapp.data.dao.ProductoDao
import com.forcetech.jmfitapp.data.repository.ProductoRepository
import com.forcetech.jmfitapp.model.Producto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import java.math.BigDecimal

/** Contrato de coordinación con persistencia simulada; no comprueba SQL ni interfaz Android. */
@RunWith(MockitoJUnitRunner.StrictStubs::class)
class ProductoRepositoryTest {
    @Mock private lateinit var dao: ProductoDao
    private lateinit var repository: ProductoRepository

    private val producto = Producto(42, "Proteína", "Fit", BigDecimal("15000.50"), "Recuperación", "500 g", true)

    @Before
    fun preparar() {
        repository = ProductoRepository(dao)
    }

    @Test
    fun `crear inserta el producto completo sin actualizar ni cambiar estado`() = runTest {
        val nuevo = producto.copy(id = 0, activo = false)
        `when`(dao.guardarProductos(nuevo)).thenReturn(7L)

        assertTrue(repository.guardar(nuevo, editando = false))

        verify(dao).guardarProductos(nuevo)
        verifyNoMoreInteractions(dao)
    }

    @Test
    fun `editar actualiza el producto con su id y no inserta un duplicado`() = runTest {
        val editado = producto.copy(nombre = "Creatina", precio = BigDecimal("25000.25"), activo = false)
        `when`(dao.updateProductos(editado)).thenReturn(1)

        assertTrue(repository.guardar(editado, editando = true))

        verify(dao).updateProductos(editado)
        verifyNoMoreInteractions(dao)
    }

    @Test
    fun `editar sin filas afectadas comunica que no se guardo`() = runTest {
        `when`(dao.updateProductos(producto)).thenReturn(0)

        assertFalse(repository.guardar(producto, editando = true))

        verify(dao).updateProductos(producto)
        verifyNoMoreInteractions(dao)
    }

    @Test
    fun `desactivar solo cambia el estado del producto seleccionado`() = runTest {
        `when`(dao.cambiarEstado(42, false)).thenReturn(1)

        assertTrue(repository.cambiarEstado(producto))

        verify(dao).cambiarEstado(42, false)
        verifyNoMoreInteractions(dao)
    }

    @Test
    fun `activar un producto inactivo solicita estado verdadero`() = runTest {
        `when`(dao.cambiarEstado(42, true)).thenReturn(1)

        assertTrue(repository.cambiarEstado(producto.copy(activo = false)))

        verify(dao).cambiarEstado(42, true)
        verifyNoMoreInteractions(dao)
    }

    @Test
    fun `cambiar estado sin filas afectadas comunica que no hubo cambio`() = runTest {
        `when`(dao.cambiarEstado(42, false)).thenReturn(0)

        assertFalse(repository.cambiarEstado(producto))
    }

    @Test
    fun `listar recorta busqueda y conserva filtros y resultado del DAO`() = runTest {
        val esperados = listOf(producto)
        for (estado in 0..2) {
            `when`(dao.obtenerProductosFiltrados("Fit", estado)).thenReturn(esperados)

            assertEquals(esperados, repository.listar("  Fit  ", estado))

            verify(dao).obtenerProductosFiltrados("Fit", estado)
        }
        verifyNoMoreInteractions(dao)
    }

    @Test
    fun `listar sin busqueda admite lista vacia`() = runTest {
        `when`(dao.obtenerProductosFiltrados("", 0)).thenReturn(emptyList())

        assertTrue(repository.listar("   ", 0).isEmpty())

        verify(dao).obtenerProductosFiltrados("", 0)
    }

    @Test
    fun `error al insertar se propaga sin intentar otra escritura`() = runTest {
        val error = IllegalStateException("Error de inserción")
        `when`(dao.guardarProductos(producto)).thenThrow(error)

        assertSame(error, runCatching { repository.guardar(producto, false) }.exceptionOrNull())

        verify(dao).guardarProductos(producto)
        verifyNoMoreInteractions(dao)
    }

    @Test
    fun `error al editar se propaga sin insertar como alternativa`() = runTest {
        val error = IllegalStateException("Error de actualización")
        `when`(dao.updateProductos(producto)).thenThrow(error)

        assertSame(error, runCatching { repository.guardar(producto, true) }.exceptionOrNull())

        verify(dao).updateProductos(producto)
        verifyNoMoreInteractions(dao)
    }

    @Test
    fun `error al cambiar estado se propaga`() = runTest {
        val error = IllegalStateException("Error de estado")
        `when`(dao.cambiarEstado(42, false)).thenThrow(error)

        assertSame(error, runCatching { repository.cambiarEstado(producto) }.exceptionOrNull())
    }

    @Test
    fun `error al consultar no se convierte en lista vacia`() = runTest {
        val error = IllegalStateException("Error de consulta")
        `when`(dao.obtenerProductosFiltrados("Fit", 1)).thenThrow(error)

        assertSame(error, runCatching { repository.listar("Fit", 1) }.exceptionOrNull())
    }

    @Test
    fun `cancelacion de una consulta se propaga al llamador`() = runTest {
        val cancelacion = CancellationException("Consulta cancelada")
        `when`(dao.obtenerProductosFiltrados("", 0)).thenThrow(cancelacion)

        assertSame(cancelacion, runCatching { repository.listar("", 0) }.exceptionOrNull())
    }
}
