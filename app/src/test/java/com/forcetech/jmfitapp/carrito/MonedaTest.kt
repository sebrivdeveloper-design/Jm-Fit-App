package com.forcetech.jmfitapp.carrito

import com.forcetech.jmfitapp.ui.store.Moneda
import org.junit.Assert.assertEquals
import org.junit.Test

/** HU-20: valores en pesos colombianos con separador de miles. */
class MonedaTest {

    @Test
    fun `usa punto como separador de miles`() {
        assertEquals("\$149.900", Moneda.pesos(149_900L))
        assertEquals("\$12.000", Moneda.pesos(12_000L))
        assertEquals("\$308.730", Moneda.pesos(308_730L))
    }

    @Test
    fun `formatea millones con dos separadores`() {
        assertEquals("\$1.234.567", Moneda.pesos(1_234_567L))
    }

    @Test
    fun `los valores menores a mil no llevan separador`() {
        assertEquals("\$999", Moneda.pesos(999L))
    }

    @Test
    fun `cero se muestra como cero pesos`() {
        assertEquals("\$0", Moneda.pesos(0L))
    }
}