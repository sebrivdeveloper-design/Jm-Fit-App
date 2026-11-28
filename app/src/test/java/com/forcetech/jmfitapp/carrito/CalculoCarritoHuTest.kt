package com.forcetech.jmfitapp.carrito

import com.forcetech.jmfitapp.model.CalculoCarrito
import com.forcetech.jmfitapp.model.CarritoItem
import com.forcetech.jmfitapp.model.ReglasCarrito
import com.forcetech.jmfitapp.model.ResumenCarrito
import org.junit.Assert.assertEquals
import org.junit.Test

/** Reglas de cálculo detrás de HU-18, HU-19 y HU-20. Son funciones puras: no necesitan mocks. */
class CalculoCarritoHuTest {

    private fun item(id: Long, precio: Long, cantidad: Int) =
        CarritoItem(id, "Producto $id", "Presentación", precio, null, cantidad, 0L)

    // ---------- HU-20 ----------

    @Test
    fun `el subtotal suma precio por cantidad de cada producto`() {
        val r = CalculoCarrito.resumen(listOf(item(1, 149_900L, 1), item(2, 89_900L, 2)), esSocio = false)

        assertEquals(329_700L, r.subtotal)
        assertEquals(3, r.unidades)
    }

    @Test
    fun `sin descuento el total es subtotal mas envio`() {
        val r = CalculoCarrito.resumen(listOf(item(1, 100_000L, 1)), esSocio = false)

        assertEquals(0L, r.descuento)
        assertEquals(100_000L + ReglasCarrito.COSTO_ENVIO, r.total)
    }

    @Test
    fun `con descuento el total es subtotal menos descuento mas envio`() {
        val r = CalculoCarrito.resumen(listOf(item(1, 100_000L, 1)), esSocio = true)

        assertEquals(10_000L, r.descuento)
        assertEquals(100_000L - 10_000L + ReglasCarrito.COSTO_ENVIO, r.total)
    }

    @Test
    fun `el descuento se redondea hacia abajo a pesos enteros`() {
        val r = CalculoCarrito.resumen(listOf(item(1, 12_345L, 1)), esSocio = true)

        assertEquals(1_234L, r.descuento) // 10 % de 12.345 = 1.234,5
    }

    @Test
    fun `carrito vacio da total cero y no cobra envio aunque sea socio`() {
        assertEquals(ResumenCarrito.VACIO, CalculoCarrito.resumen(emptyList(), esSocio = true))
        assertEquals(0L, CalculoCarrito.resumen(emptyList(), esSocio = false).total)
        assertEquals(0L, ResumenCarrito.VACIO.envio)
    }

    // ---------- HU-19 ----------

    @Test
    fun `cambiar la cantidad actualiza el subtotal y el total`() {
        val antes = CalculoCarrito.resumen(listOf(item(1, 50_000L, 1)), esSocio = false)
        val despues = CalculoCarrito.resumen(listOf(item(1, 50_000L, 3)), esSocio = false)

        assertEquals(50_000L, antes.subtotal)
        assertEquals(150_000L, despues.subtotal)
        assertEquals(150_000L + ReglasCarrito.COSTO_ENVIO, despues.total)
        assertEquals(3, despues.unidades)
    }

    // ---------- HU-18 ----------

    @Test
    fun `quitar un producto recalcula con los que quedan sin tocar sus cantidades`() {
        val whey = item(1, 149_900L, 1)
        val creatina = item(2, 89_900L, 2)

        val conAmbos = CalculoCarrito.resumen(listOf(whey, creatina), esSocio = false)
        val soloCreatina = CalculoCarrito.resumen(listOf(creatina), esSocio = false)

        assertEquals(329_700L, conAmbos.subtotal)
        assertEquals(179_800L, soloCreatina.subtotal) // 2 x 89.900, igual que antes
        assertEquals(2, creatina.cantidad)
        assertEquals(2, soloCreatina.unidades)
    }

    @Test
    fun `quitar el ultimo producto deja el resumen vacio`() {
        val resumen = CalculoCarrito.resumen(emptyList(), esSocio = false)

        assertEquals(0, resumen.unidades)
        assertEquals(0L, resumen.subtotal)
        assertEquals(0L, resumen.total)
    }
}