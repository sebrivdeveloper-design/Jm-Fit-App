package com.forcetech.jmfitapp.productos

import com.forcetech.jmfitapp.data.database.BigDecimalConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

/** Comprueba la conversión monetaria usada por Producto, sin arrancar Room. */
class PrecioPersistidoProductoTest {
    private val converter = BigDecimalConverter()

    @Test
    fun `precio conserva precision y escala al convertir a texto y recuperarlo`() {
        val precio = BigDecimal("12345678901234567890.12345678900")

        val almacenado = converter.fromBigDecimal(precio)
        val recuperado = converter.toBigDecimal(almacenado)

        assertEquals("12345678901234567890.12345678900", almacenado)
        assertEquals(precio, recuperado)
    }

    @Test
    fun `precio en notacion cientifica se almacena como decimal plano`() {
        val almacenado = converter.fromBigDecimal(BigDecimal("1E+5"))

        assertEquals("100000", almacenado)
        assertEquals(BigDecimal("100000"), converter.toBigDecimal(almacenado))
    }

    @Test
    fun `conversion conserva valores nulos`() {
        assertNull(converter.fromBigDecimal(null))
        assertNull(converter.toBigDecimal(null))
    }
}
