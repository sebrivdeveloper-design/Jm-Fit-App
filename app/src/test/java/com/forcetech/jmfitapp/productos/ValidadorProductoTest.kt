package com.forcetech.jmfitapp.productos

import com.forcetech.jmfitapp.model.Producto
import com.forcetech.jmfitapp.model.ValidadorProducto
import com.forcetech.jmfitapp.model.ValidadorProducto.Campo
import com.forcetech.jmfitapp.model.ValidadorProducto.Resultado
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class ValidadorProductoTest {
    @Test
    fun `crear asigna id cero y estado activo por defecto`() {
        val resultado = ValidadorProducto.validar("Proteína", "Fit", "15000", "Recuperación", "500 g")

        assertEquals(
            Resultado.Valido(Producto(0, "Proteína", "Fit", BigDecimal("15000"), "Recuperación", "500 g", true)),
            resultado
        )
    }

    @Test
    fun `editar conserva id y estado y recorta todos los campos`() {
        val resultado = ValidadorProducto.validar(
            "  Proteína  ", " Fit ", " 15000,50 ", " Recuperación ", " 500 g ", activo = false, id = 42
        )

        assertEquals(
            Resultado.Valido(Producto(42, "Proteína", "Fit", BigDecimal("15000.50"), "Recuperación", "500 g", false)),
            resultado
        )
    }

    @Test
    fun `informa todos los campos vacios y prioriza obligatorios sobre precio invalido`() {
        val resultado = ValidadorProducto.validar(" ", "", "abc", "\t", "\n")

        assertEquals(Resultado.CamposObligatorios(setOf(Campo.NOMBRE, Campo.MARCA, Campo.OBJETIVO, Campo.PRESENTACION)), resultado)
    }

    @Test
    fun `formulario vacio informa los cinco campos obligatorios`() {
        assertEquals(
            Resultado.CamposObligatorios(Campo.entries.toSet()),
            ValidadorProducto.validar("", "", "", "", "")
        )
    }
}
