package com.forcetech.jmfitapp.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.forcetech.jmfitapp.data.dao.ProductoDao
import com.forcetech.jmfitapp.model.InitialEntity
import com.forcetech.jmfitapp.model.Producto

/**
 * Clase principal de la base de datos de Room.
 *
 * La anotación @Database especifica:
 * - entities: El listado de clases entidad que componen la base de datos ().
 * - version: El número de versión del esquema de la base de datos.
 */
@Database(
    entities = [Producto::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(BigDecimalConverter::class)
abstract class AppDatabase : RoomDatabase() {

    /** Proporciona acceso a las operaciones del DAO de productos. */
    abstract fun productoDao(): ProductoDao

    companion object {
        /**
         * Garantiza que el valor de INSTANCE siempre esté actualizado y sea visible para todos los hilos.
         */
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Obtiene la instancia única (Singleton) de la base de datos [AppDatabase].
         *
         * @param context Contexto de la actividad o aplicación.
         * @return Instancia única activa de [AppDatabase].
         */
        fun obtener(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jmfitapp_db",
                ).fallbackToDestructiveMigration(dropAllTables = false)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}