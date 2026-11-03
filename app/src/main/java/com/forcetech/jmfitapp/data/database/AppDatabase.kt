package com.forcetech.jmfitapp.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

import com.forcetech.jmfitapp.data.dao.CarritoDao
import com.forcetech.jmfitapp.data.dao.ProductoDao
import com.forcetech.jmfitapp.data.dao.EjercicioDao
import com.forcetech.jmfitapp.data.dao.RolDao
import com.forcetech.jmfitapp.data.dao.RutinaDao
import com.forcetech.jmfitapp.data.dao.UsuarioDao

import com.forcetech.jmfitapp.model.CarritoItem
import com.forcetech.jmfitapp.model.Producto
import com.forcetech.jmfitapp.model.AsignacionEntrenador
import com.forcetech.jmfitapp.model.AsignacionRutina
import com.forcetech.jmfitapp.model.ClienteInterno
import com.forcetech.jmfitapp.model.Ejercicio
import com.forcetech.jmfitapp.model.Entrenador
import com.forcetech.jmfitapp.model.GrupoMuscular
import com.forcetech.jmfitapp.model.Rol
import com.forcetech.jmfitapp.model.Rutina
import com.forcetech.jmfitapp.model.RutinaBloque
import com.forcetech.jmfitapp.model.RutinaDia
import com.forcetech.jmfitapp.model.RutinaEjercicio
import com.forcetech.jmfitapp.model.Usuario

/**
 * Clase principal de la base de datos de Room.
 */
@Database(
    entities = [
        // Entidades de usuarios y rutinas
        Rol::class,
        Usuario::class,
        Entrenador::class,
        ClienteInterno::class,
        AsignacionEntrenador::class,
        GrupoMuscular::class,
        Ejercicio::class,
        Rutina::class,
        RutinaDia::class,
        RutinaBloque::class,
        RutinaEjercicio::class,
        AsignacionRutina::class,

        // Entidad de productos
        Producto::class,

        // Entidad del carrito
        CarritoItem::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(BigDecimalConverter::class)
abstract class AppDatabase : RoomDatabase() {

    // DAO de productos
    abstract fun productoDao(): ProductoDao

    // DAO de usuarios y rutinas
    abstract fun rolDao(): RolDao
    abstract fun usuarioDao(): UsuarioDao
    abstract fun ejercicioDao(): EjercicioDao
    abstract fun rutinaDao(): RutinaDao

    // DAO del carrito
    abstract fun carritoDao(): CarritoDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun obtener(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jmfitapp_db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}