package com.forcetech.jmfitapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Un producto dentro del carrito de la tienda.
 *
 * Guarda una "foto" del producto (nombre, presentación y precio) para poder mostrar el carrito
 * sin depender de la tabla del catálogo. El carrito es del dispositivo y no de un usuario,
 * así funciona también para el cliente externo, que no tiene sesión.
 */
@Entity(tableName = "carrito_item")
data class CarritoItem(
    @PrimaryKey val productoId: Long,
    val nombre: String,
    /** Texto bajo el nombre, por ejemplo "900 g · Vainilla". */
    val presentacion: String,
    /** Precio de UNA unidad, en pesos colombianos (sin decimales). */
    val precioUnitario: Long,
    /** Nombre de un drawable (ej. "prod_whey_isolate"). Si es null o no existe, se muestra un marcador. */
    val imagen: String? = null,
    val cantidad: Int = 1,
    /** Mantiene el orden en que se fueron agregando los productos. */
    val agregadoEn: Long = System.currentTimeMillis()
)

/** Reglas de negocio del carrito (fáciles de cambiar en un solo lugar). */
object ReglasCarrito {
    const val DESCUENTO_SOCIO_PORCENTAJE = 10
    const val COSTO_ENVIO = 12_000L
    const val CANTIDAD_MAXIMA = 99
}

/** Totales que se muestran en el resumen del carrito (todo en pesos colombianos). */
data class ResumenCarrito(
    val unidades: Int,
    val subtotal: Long,
    val descuento: Long,
    val envio: Long,
    val total: Long
) {
    companion object {
        val VACIO = ResumenCarrito(unidades = 0, subtotal = 0L, descuento = 0L, envio = 0L, total = 0L)
    }
}

object CalculoCarrito {

    /**
     * Subtotal = suma de precio x cantidad. Si la persona es socio (cliente interno) se descuenta el 10 %.
     * El envío se cobra solo cuando hay productos.
     */
    fun resumen(items: List<CarritoItem>, esSocio: Boolean): ResumenCarrito {
        if (items.isEmpty()) return ResumenCarrito.VACIO

        val unidades = items.sumOf { it.cantidad }
        val subtotal = items.sumOf { it.precioUnitario * it.cantidad }
        val descuento = if (esSocio) subtotal * ReglasCarrito.DESCUENTO_SOCIO_PORCENTAJE / 100 else 0L
        val envio = ReglasCarrito.COSTO_ENVIO

        return ResumenCarrito(
            unidades = unidades,
            subtotal = subtotal,
            descuento = descuento,
            envio = envio,
            total = subtotal - descuento + envio
        )
    }
}