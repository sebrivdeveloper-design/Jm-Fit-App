package com.forcetech.jmfitapp.productos

import com.forcetech.jmfitapp.data.dao.CatalogoDao
import com.forcetech.jmfitapp.data.repository.CatalogoRepository
import com.forcetech.jmfitapp.model.CategoriaProducto
import com.forcetech.jmfitapp.model.ProductoDetalle
import com.forcetech.jmfitapp.model.ProductoResumen
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.junit.MockitoJUnitRunner
import java.math.BigDecimal

/** Coordinación del catálogo con el DAO simulado; no comprueba SQL ni interfaz Android. */
@RunWith(MockitoJUnitRunner.StrictStubs::class)
class CatalogoRepositoryTest {

    @Mock private lateinit var dao: CatalogoDao
    private lateinit var repository: CatalogoRepository

    /** Registra el orden en que ocurren "espera" (base lista) y "dao". */
    private val orden = mutableListOf<String>()

    private val resumen = ProductoResumen(
        id = 1, nombre = "Whey Isolate", presentacion = "900 g",
        precio = BigDecimal("149900"), imagen = null, calificacion = 4.9,
        disponible = true, categoriaNombre = "Proteínas"
    )

    private val detalle = ProductoDetalle(
        id = 1, nombre = "Whey Isolate", marca = "JM", presentacion = "900 g",
        precio = BigDecimal("149900"), objetivo = "Recuperación",
        descripcion = "Proteína aislada", beneficios = "Rápida absorción",
        imagen = null, calificacion = 4.9, disponible = true, categoriaNombre = "Proteínas"
    )

    @Before
    fun preparar() {
        repository = CatalogoRepository(dao) { orden.add("espera") }
    }

    @Test
    fun `categorias espera la base lista y luego consulta las activas`() = runTest {
        val categorias = listOf(CategoriaProducto(1, "Proteínas"), CategoriaProducto(2, "Creatinas"))
        `when`(dao.categoriasActivas()).thenAnswer { orden.add("dao"); categorias }

        assertEquals(categorias, repository.categorias())

        assertEquals(listOf("espera", "dao"), orden)
    }

    @Test
    fun `productos sin filtros consulta todas las categorias y texto vacio`() = runTest {
        `when`(dao.buscarProductos(null, "")).thenReturn(listOf(resumen))

        assertEquals(listOf(resumen), repository.productos(null, ""))

        verify(dao).buscarProductos(null, "")
        verifyNoMoreInteractions(dao)
    }

    @Test
    fun `productos envia la categoria elegida al dao`() = runTest {
        `when`(dao.buscarProductos(3, "")).thenReturn(emptyList())

        assertEquals(emptyList<ProductoResumen>(), repository.productos(3, ""))

        verify(dao).buscarProductos(3, "")
    }

    @Test
    fun `productos quita los espacios del texto de busqueda`() = runTest {
        `when`(dao.buscarProductos(null, "whey")).thenReturn(listOf(resumen))

        assertEquals(listOf(resumen), repository.productos(null, "  whey  "))

        verify(dao).buscarProductos(null, "whey")
    }

    @Test
    fun `productos escapa porcentaje guion bajo y barra invertida`() = runTest {
        // Entrada:  50%_a\b   ->   Salida esperada:  50\%\_a\\b
        `when`(dao.buscarProductos(null, "50\\%\\_a\\\\b")).thenReturn(emptyList())

        repository.productos(null, "50%_a\\b")

        verify(dao).buscarProductos(null, "50\\%\\_a\\\\b")
    }

    @Test
    fun `detalle devuelve el producto del dao`() = runTest {
        `when`(dao.detalleDe(1)).thenReturn(detalle)

        assertEquals(detalle, repository.detalle(1))

        verify(dao).detalleDe(1)
    }

    @Test
    fun `detalle devuelve null cuando el producto no existe o esta inactivo`() = runTest {
        `when`(dao.detalleDe(99)).thenReturn(null)

        assertNull(repository.detalle(99))
    }

    @Test
    fun `si el dao falla la excepcion llega a quien llama`() = runTest {
        `when`(dao.detalleDe(1)).thenThrow(IllegalStateException("base de datos cerrada"))

        try {
            repository.detalle(1)
            fail("Debía lanzar IllegalStateException")
        } catch (e: IllegalStateException) {
            assertEquals("base de datos cerrada", e.message)
        }
    }
}