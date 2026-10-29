package com.forcetech.jmfitapp.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.forcetech.jmfitapp.model.InitialEntity

/**
 * Clase principal de la base de datos de Room.
 *
 * La anotación @Database especifica:
 * - entities: El listado de clases entidad que componen la base de datos ().
 * - version: El número de versión del esquema de la base de datos.
 */
@Database(
    entities = [InitialEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    /** Proporciona acceso a las operaciones del DAO de usuarios. */

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