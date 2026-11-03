package com.forcetech.jmfitapp.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Catálogo y detalle de la tienda (cliente).
 *
 * NO se toca la entidad [Producto] (tabla "productos"): es la que el administrador gestiona en
 * ProductoActivity. Los datos que solo necesita la tienda viven en una tabla 1 a 1:
 *
 *   Producto 1 ── 1 ProductoTienda >── 1 CategoriaProducto
 *
 * Un producto creado por el administrador SIN fila en producto_tienda también aparece en el catálogo
 * (en "Todos", sin categoría, imagen genérica y disponible).
 */

@Entity(
    tableName = "categoria_producto",
    indices = [Index(value = ["nombre"], unique = true)]
)
data class CategoriaProducto(
    @PrimaryKey val id: Int,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val nombre: String,
    val estado: String = EstadoCatalogo.ACTIVO
)

@Entity(
    tableName = "producto_tienda",
    foreignKeys = [
        ForeignKey(
            entity = Producto::class,
            parentColumns = ["id"],
            childColumns = ["productoId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoriaProducto::class,
            parentColumns = ["id"],
            childColumns = ["categoriaId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoriaId")]
)
data class ProductoTienda(
    @PrimaryKey val productoId: Int,
    val categoriaId: Int?,
    val descripcion: String = "",
    /** Un beneficio por línea (separados por salto de línea). */
    val beneficios: String = "",
    /** Nombre de un drawable en res/drawable (sin extensión), por ejemplo "prod_whey_isolate". */
    val imagen: String? = null,
    /** Hay existencias. Distinto de Producto.activo, que es "visible en la tienda" (lo decide el administrador). */
    val disponible: Boolean = true,
    val calificacion: Double = 0.0
)

/** Fila del catálogo (resultado de un JOIN, no es una tabla). */
data class ProductoResumen(
    val id: Int,
    val nombre: String,
    val presentacion: String,
    val precio: BigDecimal,
    val imagen: String?,
    val calificacion: Double,
    val disponible: Boolean,
    val categoriaNombre: String?
) {
    /** Pesos enteros (COP): lo que usan Moneda.pesos() y el carrito. */
    val precioPesos: Long get() = precio.setScale(0, RoundingMode.HALF_UP).toLong()
}

/** Datos completos para la pantalla de detalle (resultado de un JOIN, no es una tabla). */
data class ProductoDetalle(
    val id: Int,
    val nombre: String,
    val marca: String,
    val presentacion: String,
    val precio: BigDecimal,
    val objetivo: String,
    val descripcion: String,
    val beneficios: String,
    val imagen: String?,
    val calificacion: Double,
    val disponible: Boolean,
    val categoriaNombre: String?
) {
    val precioPesos: Long get() = precio.setScale(0, RoundingMode.HALF_UP).toLong()
}
