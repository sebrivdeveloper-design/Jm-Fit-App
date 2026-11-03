package com.forcetech.jmfitapp.ui.store

import android.content.Context
import androidx.annotation.DrawableRes
import com.forcetech.jmfitapp.R
import java.util.Locale

/** Utilidades de presentación compartidas por el catálogo y el detalle. (Los precios usan Moneda.pesos().) */
object TiendaUi {

    val LOCALE_CO: Locale = Locale.forLanguageTag("es-CO")

    /** 4.9 → "4,9" */
    fun calificacion(valor: Double): String = String.format(LOCALE_CO, "%.1f", valor)

    /** Busca el drawable por nombre; si es null o no existe todavía, usa el marcador de posición. */
    @DrawableRes
    fun recursoImagen(context: Context, nombre: String?): Int {
        if (nombre.isNullOrBlank()) return R.drawable.ic_producto_placeholder
        val id = context.resources.getIdentifier(nombre, "drawable", context.packageName)
        return if (id != 0) id else R.drawable.ic_producto_placeholder
    }
}

/** Mensajes de una sola vez que las pantallas de la tienda muestran en un Snackbar. */
sealed interface TiendaEvento {
    data class ProductoAgregado(val nombre: String) : TiendaEvento
    data object ErrorAlAgregar : TiendaEvento
}
