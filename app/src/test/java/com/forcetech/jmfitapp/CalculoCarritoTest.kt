package com.forcetech.jmfitapp

import com.forcetech.jmfitapp.model.CalculoCarrito
import com.forcetech.jmfitapp.model.CarritoItem
import com.forcetech.jmfitapp.model.ResumenCarrito
import org.junit.Assert
import org.junit.Test

/** Verifica los números del diseño: 149.900 + 2 x 89.900 = 329.700; socio -10 % y envío de 12.000. */
class CalculoCarritoTest {

    private val whey = CarritoItem(1, "Whey isolate", "900 g · Vainilla", 149_900L, cantidad = 1)
    private val creatina =
        CarritoItem(2, "Creatina mono", "300 g · Sin sabor", 89_900L, cantidad = 2)

    @Test
    fun socio_recibe_descuento_y_paga_envio() {
        val r = CalculoCarrito.resumen(listOf(whey, creatina), esSocio = true)

        Assert.assertEquals(3, r.unidades)
        Assert.assertEquals(329_700L, r.subtotal)
        Assert.assertEquals(32_970L, r.descuento)
        Assert.assertEquals(12_000L, r.envio)
        Assert.assertEquals(308_730L, r.total)
    }

    @Test
    fun cliente_externo_no_recibe_descuento() {
        val r = CalculoCarrito.resumen(listOf(whey, creatina), esSocio = false)

        Assert.assertEquals(0L, r.descuento)
        Assert.assertEquals(341_700L, r.total)
    }

    @Test
    fun carrito_vacio_no_cobra_envio() {
        Assert.assertEquals(
            ResumenCarrito.VACIO,
            CalculoCarrito.resumen(emptyList(), esSocio = true)
        )
    }
}