package com.forcetech.jmfitapp.model

import java.math.BigDecimal

/** Reglas del formulario administrativo, independientes de Android. */
object ValidadorProducto {
    enum class Campo { NOMBRE, MARCA, PRECIO, OBJETIVO, PRESENTACION }

    sealed interface Resultado {
        data class CamposObligatorios(val campos: Set<Campo>) : Resultado
        data object PrecioInvalido : Resultado
        data class Valido(val producto: Producto) : Resultado
    }

    fun validar(
        nombre: String,
        marca: String,
        precio: String,
        objetivo: String,
        presentacion: String,
        activo: Boolean = true,
        id: Int = 0
    ): Resultado {
        val campos = mapOf(
            Campo.NOMBRE to nombre.trim(),
            Campo.MARCA to marca.trim(),
            Campo.PRECIO to precio.trim(),
            Campo.OBJETIVO to objetivo.trim(),
            Campo.PRESENTACION to presentacion.trim()
        )
        val vacios = campos.filterValues { it.isEmpty() }.keys
        if (vacios.isNotEmpty()) return Resultado.CamposObligatorios(vacios)

        val importe = try {
            BigDecimal(precio.trim().replace(',', '.'))
        } catch (_: NumberFormatException) {
            return Resultado.PrecioInvalido
        }
        if (importe <= BigDecimal.ZERO) return Resultado.PrecioInvalido

        return Resultado.Valido(
            Producto(id, nombre.trim(), marca.trim(), importe, objetivo.trim(), presentacion.trim(), activo)
        )
    }
}
