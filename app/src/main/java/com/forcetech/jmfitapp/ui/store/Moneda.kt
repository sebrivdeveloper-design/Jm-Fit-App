package com.forcetech.jmfitapp.ui.store

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Da formato de pesos colombianos: 149900 -> "$149.900" (punto como separador de miles). */
object Moneda {

    fun pesos(valor: Long): String {
        val simbolos = DecimalFormatSymbols(Locale.forLanguageTag("es-CO")).apply {
            groupingSeparator = '.'
        }
        return "\$" + DecimalFormat("#,##0", simbolos).format(valor)
    }
}