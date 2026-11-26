package com.forcetech.jmfitapp.productos

import com.forcetech.jmfitapp.model.ValidadorProducto
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.math.BigDecimal

@RunWith(Parameterized::class)
class PrecioProductoTest(private val texto: String, private val esperado: String?) {
    @Test
    fun `valida precio sin perder precision decimal`() {
        val resultado = ValidadorProducto.validar("Proteína", "Fit", texto, "Recuperación", "500 g")

        if (esperado == null) {
            assertEquals(ValidadorProducto.Resultado.PrecioInvalido, resultado)
        } else {
            val producto = (resultado as ValidadorProducto.Resultado.Valido).producto
            assertEquals(BigDecimal(esperado), producto.precio)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: precio=[{0}], esperado={1}")
        fun casos(): List<Array<String?>> = listOf(
            arrayOf("0", null), arrayOf("0.00", null), arrayOf("-0,01", null),
            arrayOf("abc", null), arrayOf("NaN", null), arrayOf("Infinity", null),
            arrayOf("1.2.3", null), arrayOf("1,234.56", null),
            arrayOf("1", "1"), arrayOf("0.01", "0.01"), arrayOf("0,01", "0.01"),
            arrayOf(" 15000,50 ", "15000.50"), arrayOf("15000.50", "15000.50"),
            arrayOf("12345678901234567890.123456789", "12345678901234567890.123456789")
        )
    }
}
