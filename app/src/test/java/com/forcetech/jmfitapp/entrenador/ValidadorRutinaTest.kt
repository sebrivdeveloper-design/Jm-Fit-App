package com.forcetech.jmfitapp.entrenador

import com.forcetech.jmfitapp.model.ValidadorRutina
import com.forcetech.jmfitapp.model.ValidadorRutina.Campo
import com.forcetech.jmfitapp.model.ValidadorRutina.ResultadoEjercicio
import org.junit.Assert.assertEquals
import org.junit.Test

/** ENT-5 y ENT-8: reglas de series, repeticiones y peso del formulario de rutina. */
class ValidadorRutinaTest {

    private fun validar(series: String, repeticiones: String, peso: String = "") =
        ValidadorRutina.validarEjercicio(series, repeticiones, peso)

    @Test
    fun `ejercicio correcto sin peso devuelve peso nulo`() {
        assertEquals(ResultadoEjercicio.Valido(4, "10", null), validar("4", "10"))
    }

    @Test
    fun `ejercicio correcto con peso decimal`() {
        assertEquals(ResultadoEjercicio.Valido(3, "8-12", 42.5), validar("3", "8-12", "42.5"))
    }

    @Test
    fun `acepta coma como separador decimal del peso`() {
        assertEquals(ResultadoEjercicio.Valido(3, "10", 42.5), validar("3", "10", "42,5"))
    }

    @Test
    fun `recorta espacios en todos los campos`() {
        assertEquals(ResultadoEjercicio.Valido(5, "12/10/8", 20.0), validar(" 5 ", " 12/10/8 ", " 20 "))
    }

    @Test
    fun `series acepta los limites 1 y 20`() {
        assertEquals(ResultadoEjercicio.Valido(1, "10", null), validar("1", "10"))
        assertEquals(ResultadoEjercicio.Valido(20, "10", null), validar("20", "10"))
    }

    @Test
    fun `series fuera de rango o no numericas son invalidas`() {
        for (texto in listOf("", "0", "21", "-1", "abc", "2.5", "1 0", "+5")) {
            assertEquals(
                "series=[$texto]",
                ResultadoEjercicio.Invalido(setOf(Campo.SERIES)),
                validar(texto, "10")
            )
        }
    }

    @Test
    fun `repeticiones vacias o sin ningun numero son invalidas`() {
        for (texto in listOf("", "   ", "max", "al fallo")) {
            assertEquals(
                "repeticiones=[$texto]",
                ResultadoEjercicio.Invalido(setOf(Campo.REPETICIONES)),
                validar("4", texto)
            )
        }
    }

    @Test
    fun `repeticiones acepta rangos y esquemas con texto`() {
        assertEquals(ResultadoEjercicio.Valido(4, "8-12", null), validar("4", "8-12"))
        assertEquals(ResultadoEjercicio.Valido(4, "10 por lado", null), validar("4", "10 por lado"))
    }

    @Test
    fun `repeticiones con mas de 20 caracteres es invalido`() {
        val veinte = "1".repeat(20)
        assertEquals(ResultadoEjercicio.Valido(4, veinte, null), validar("4", veinte))
        assertEquals(
            ResultadoEjercicio.Invalido(setOf(Campo.REPETICIONES)),
            validar("4", veinte + "1")
        )
    }

    @Test
    fun `peso cero negativo o no numerico es invalido`() {
        for (texto in listOf("0", "0.0", "-5", "abc", "1.2.3", "NaN", "Infinity", "1e2", "--")) {
            assertEquals(
                "peso=[$texto]",
                ResultadoEjercicio.Invalido(setOf(Campo.PESO)),
                validar("4", "10", texto)
            )
        }
    }

    @Test
    fun `peso mayor a 999 coma 9 es invalido y el limite es valido`() {
        assertEquals(ResultadoEjercicio.Valido(4, "10", 999.9), validar("4", "10", "999.9"))
        assertEquals(ResultadoEjercicio.Invalido(setOf(Campo.PESO)), validar("4", "10", "1000"))
    }

    @Test
    fun `peso acepta punto final y punto inicial`() {
        assertEquals(ResultadoEjercicio.Valido(4, "10", 40.0), validar("4", "10", "40."))
        assertEquals(ResultadoEjercicio.Valido(4, "10", 0.5), validar("4", "10", ".5"))
    }

    @Test
    fun `informa todos los campos con error a la vez`() {
        assertEquals(
            ResultadoEjercicio.Invalido(setOf(Campo.SERIES, Campo.REPETICIONES, Campo.PESO)),
            validar("0", "", "-1")
        )
    }
}
