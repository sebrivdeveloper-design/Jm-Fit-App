package com.forcetech.jmfitapp.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.forcetech.jmfitapp.data.dao.EjercicioDao
import com.forcetech.jmfitapp.data.dao.RolDao
import com.forcetech.jmfitapp.data.dao.RutinaDao
import com.forcetech.jmfitapp.data.dao.UsuarioDao
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
 *
 * La anotación @Database especifica:
 * - entities: El listado de clases entidad que componen la base de datos.
 * - version: El número de versión del esquema. SUBIRLO cada vez que cambie una entidad
 *   (con fallbackToDestructiveMigration la base se recrea y se vuelven a sembrar los datos de ejemplo).
 *
 * IMPORTANTE para el equipo: todos editan esta lista. Al integrar ramas, conservar las entidades de todos
 * y quedarse con el número de versión más alto.
 */
@Database(
    entities = [
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
        AsignacionRutina::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun rolDao(): RolDao

    abstract fun usuarioDao(): UsuarioDao

    abstract fun ejercicioDao(): EjercicioDao

    abstract fun rutinaDao(): RutinaDao

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
