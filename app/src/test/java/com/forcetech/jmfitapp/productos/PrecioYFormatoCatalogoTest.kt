package com.forcetech.jmfitapp.productos

import com.forcetech.jmfitapp.model.ProductoDetalle
import com.forcetech.jmfitapp.model.ProductoResumen
import com.forcetech.jmfitapp.ui.store.DetalleUiState
import com.forcetech.jmfitapp.ui.store.Moneda
import com.forcetech.jmfitapp.ui.store.TiendaUi
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

/** Cálculos y formatos del catálogo y el detalle; no usan base de datos ni Android. */
class PrecioYFormatoCatalogoTest {

    private fun resumen(precio: String) = ProductoResumen(
        id = 1, nombre = "Whey", presentacion = "900 g", precio = BigDecimal(precio),
        imagen = null, calificacion = 4.9, disponible = true, categoriaNombre = null
    )

    private fun detalle(precio: String) = ProductoDetalle(
        id = 1, nombre = "Whey", marca = "JM", presentacion = "900 g", precio = BigDecimal(precio),
        objetivo = "Recuperación", descripcion = "", beneficios = "",
        imagen = null, calificacion = 4.9, disponible = true, categoriaNombre = null
    )

    // ---- precioPesos (resumen del catálogo) ----

    @Test
    fun `precioPesos de un precio entero no cambia`() {
        assertEquals(149_900L, resumen("149900").precioPesos)
    }

    @Test
    fun `precioPesos redondea hacia arriba desde la mitad`() {
        assertEquals(149_901L, resumen("149900.50").precioPesos)
    }

    @Test
    fun `precioPesos redondea hacia abajo si los decimales son menores a la mitad`() {
        assertEquals(149_900L, resumen("149900.49").precioPesos)
    }

    // ---- precioPesos (detalle) ----

    @Test
    fun `el detalle redondea el precio igual que el catalogo`() {
        assertEquals(89_901L, detalle("89900.50").precioPesos)
        assertEquals(89_900L, detalle("89900.40").precioPesos)
    }

    // ---- total del detalle ----

    @Test
    fun `total multiplica el precio redondeado por la cantidad`() {
        val estado = DetalleUiState(cargando = false, producto = detalle("89900.50"), cantidad = 3)

        assertEquals(269_703L, estado.total)
    }

    // ---- Moneda.pesos ----

    @Test
    fun `pesos usa punto como separador de miles`() {
        assertEquals("\$149.900", Moneda.pesos(149_900L))
        assertEquals("\$1.250.000", Moneda.pesos(1_250_000L))
    }

    @Test
    fun `pesos formatea valores pequenos y cero sin separador`() {
        assertEquals("\$999", Moneda.pesos(999L))
        assertEquals("\$0", Moneda.pesos(0L))
    }

    // ---- TiendaUi.calificacion ----

    @Test
    fun `calificacion usa coma decimal y un decimal`() {
        assertEquals("4,9", TiendaUi.calificacion(4.9))
    }

    @Test
    fun `calificacion muestra un decimal aunque el valor sea entero o cero`() {
        assertEquals("5,0", TiendaUi.calificacion(5.0))
        assertEquals("0,0", TiendaUi.calificacion(0.0))
    }
}