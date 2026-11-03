package com.forcetech.jmfitapp.data.database

import androidx.room.withTransaction
import com.forcetech.jmfitapp.model.CategoriaProducto
import com.forcetech.jmfitapp.model.Producto
import com.forcetech.jmfitapp.model.ProductoTienda
import java.math.BigDecimal

/**
 * Datos de ejemplo de la tienda. Crea los productos en la tabla "productos" (la misma que gestiona el
 * administrador en ProductoActivity) y sus datos de tienda. Se cargan una sola vez (si no hay categorías).
 * Va en su propio archivo para no chocar con DatabaseSeeder al integrar ramas.
 *
 * Las imágenes son nombres de drawable: si agregas un archivo con ese nombre en res/drawable
 * (por ejemplo prod_whey_isolate.png), se muestra solo; si no existe, se usa el marcador de posición.
 */
object CatalogoSeeder {

    private const val PROTEINAS = 1
    private const val CREATINAS = 2
    private const val PRE_ENTRENOS = 3
    private const val VITAMINAS = 4
    private const val AMINOACIDOS = 5
    private const val ACCESORIOS = 6

    private class Semilla(
        val categoriaId: Int,
        val nombre: String,
        val marca: String,
        val presentacion: String,
        val precio: Int,
        val objetivo: String,
        val descripcion: String,
        val beneficios: String,
        val imagen: String,
        val calificacion: Double,
        val disponible: Boolean = true
    )

    private val semillas = listOf(
        Semilla(
            PROTEINAS, "Whey isolate", "Pulso Nutrition", "900 g · Vainilla", 149_900, "Recuperación muscular",
            "25 g de proteína por porción para apoyar tu recuperación muscular. " +
                "Bajo en azúcar y fácil de mezclar. 30 porciones.",
            "25 g de proteína por porción\nBajo en azúcar\nSe mezcla fácil", "prod_whey_isolate", 4.9
        ),
        Semilla(
            PROTEINAS, "Whey concentrada", "Pulso Nutrition", "2 lb · Chocolate", 129_900, "Ganancia muscular",
            "Proteína de suero para completar tu consumo diario. Sabor chocolate.",
            "Proteína de suero\nSabor chocolate\n28 porciones", "prod_whey_concentrada", 4.7
        ),
        Semilla(
            PROTEINAS, "Proteína vegetal", "JM Nutrition", "900 g · Cacao", 139_900, "Alimentación vegetal",
            "Mezcla de proteínas de origen vegetal, sin lactosa.",
            "Sin lactosa\nOrigen vegetal\nSabor cacao", "prod_proteina_vegetal", 4.5
        ),
        Semilla(
            CREATINAS, "Creatina mono", "Pulso Nutrition", "300 g · Sin sabor", 89_900, "Fuerza y rendimiento",
            "Creatina monohidratada en polvo, sin sabor. Se mezcla con agua o con tu batido.",
            "Monohidratada\nSin sabor\n60 porciones", "prod_creatina_mono", 4.8
        ),
        Semilla(
            CREATINAS, "Creatina micronizada", "JM Nutrition", "500 g · Sin sabor", 119_900, "Fuerza y rendimiento",
            "Creatina micronizada de partícula fina que se disuelve mejor.",
            "Micronizada\nSin sabor\n100 porciones", "prod_creatina_micronizada", 4.6,
            disponible = false // para probar el estado "Agotado"
        ),
        Semilla(
            PRE_ENTRENOS, "Pre-entreno", "JM Nutrition", "300 g · Frutos rojos", 99_900, "Energía para entrenar",
            "Fórmula en polvo para tomar antes de entrenar. Contiene cafeína.",
            "Con cafeína\nSabor frutos rojos\n30 porciones", "prod_preentreno_frutos_rojos", 4.4
        ),
        Semilla(
            VITAMINAS, "Multivitamínico", "JM Nutrition", "60 cápsulas", 54_900, "Bienestar general",
            "Complemento diario de vitaminas y minerales.",
            "Una cápsula al día\n60 cápsulas", "prod_multivitaminico", 4.3
        ),
        Semilla(
            AMINOACIDOS, "BCAA 2:1:1", "Pulso Nutrition", "300 g · Limón", 79_900, "Recuperación muscular",
            "Aminoácidos ramificados en proporción 2:1:1. Sabor limón.",
            "Proporción 2:1:1\nSabor limón\n30 porciones", "prod_bcaa", 4.5
        ),
        Semilla(
            ACCESORIOS, "Shaker", "JM Nutrition", "700 ml", 24_900, "Accesorio de entrenamiento",
            "Vaso mezclador con tapa de seguridad y bola mezcladora.",
            "Capacidad de 700 ml\nTapa de seguridad", "prod_shaker", 4.2
        ),
        Semilla(
            ACCESORIOS, "Guantes de entrenamiento", "JM Nutrition", "Talla M", 39_900, "Accesorio de entrenamiento",
            "Guantes con almohadilla en la palma para mejorar el agarre.",
            "Palma acolchada\nCierre ajustable", "prod_guantes", 4.1
        )
    )

    suspend fun sembrarSiHaceFalta(db: AppDatabase) {
        val catalogo = db.catalogoDao()
        if (catalogo.contarCategorias() > 0) return

        val productos = db.productoDao()
        db.withTransaction {
            catalogo.insertarCategorias(
                listOf(
                    CategoriaProducto(PROTEINAS, "Proteínas"),
                    CategoriaProducto(CREATINAS, "Creatinas"),
                    CategoriaProducto(PRE_ENTRENOS, "Pre-entrenos"),
                    CategoriaProducto(VITAMINAS, "Vitaminas"),
                    CategoriaProducto(AMINOACIDOS, "Aminoácidos"),
                    CategoriaProducto(ACCESORIOS, "Accesorios")
                )
            )

            semillas.forEach { s ->
                val productoId = productos.guardarProductos(
                    Producto(
                        nombre = s.nombre,
                        marca = s.marca,
                        precio = BigDecimal(s.precio),
                        objetivo = s.objetivo,
                        presentacion = s.presentacion
                    )
                ).toInt()

                catalogo.insertarDetalle(
                    ProductoTienda(
                        productoId = productoId,
                        categoriaId = s.categoriaId,
                        descripcion = s.descripcion,
                        beneficios = s.beneficios,
                        imagen = s.imagen,
                        disponible = s.disponible,
                        calificacion = s.calificacion
                    )
                )
            }
        }
    }
}
