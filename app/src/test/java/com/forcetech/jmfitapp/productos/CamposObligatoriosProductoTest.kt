package com.forcetech.jmfitapp.productos

import com.forcetech.jmfitapp.model.ValidadorProducto
import com.forcetech.jmfitapp.model.ValidadorProducto.Campo
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class CamposObligatoriosProductoTest(private val campo: Campo, private val texto: String) {
    @Test
    fun `rechaza cada campo obligatorio vacio o con espacios`() {
        val valores = mutableMapOf(
            Campo.NOMBRE to "Proteína", Campo.MARCA to "Fit", Campo.PRECIO to "15000",
            Campo.OBJETIVO to "Recuperación", Campo.PRESENTACION to "500 g"
        ).apply { this[campo] = texto }

        val resultado = ValidadorProducto.validar(
            valores.getValue(Campo.NOMBRE), valores.getValue(Campo.MARCA), valores.getValue(Campo.PRECIO),
            valores.getValue(Campo.OBJETIVO), valores.getValue(Campo.PRESENTACION)
        )

        assertEquals(ValidadorProducto.Resultado.CamposObligatorios(setOf(campo)), resultado)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: campo={0}, texto=[{1}]")
        fun casos(): List<Array<Any>> = Campo.entries.flatMap { campo ->
            listOf("", "   ").map { arrayOf<Any>(campo, it) }
        }
    }
}
