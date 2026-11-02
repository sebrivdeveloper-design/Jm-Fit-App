package com.forcetech.jmfitapp

import android.content.Context
import android.util.Log
import com.forcetech.jmfitapp.data.database.AppDatabase
import com.forcetech.jmfitapp.data.database.DatabaseSeeder
import com.forcetech.jmfitapp.data.repository.AuthRepository
import com.forcetech.jmfitapp.data.session.LoginAttemptStore
import com.forcetech.jmfitapp.data.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Contenedor de dependencias de la app (una sola instancia, creada en [JmFitApplication]).
 * Aquí se agregan los repositorios de cada módulo para que los fragments los obtengan con:
 *
 *     val container = (requireActivity().application as JmFitApplication).container
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase = AppDatabase.obtener(appContext)
    val sessionManager = SessionManager(appContext)

    /** Carga los datos de ejemplo en segundo plano la primera vez que se abre la app. */
    private val seedJob = appScope.launch {
        try {
            DatabaseSeeder.seedIfNeeded(database)
        } catch (e: Exception) {
            Log.e("AppContainer", "No se pudieron cargar los datos de ejemplo", e)
        }
    }

    /** Los repositorios llaman esto antes de leer, para no consultar mientras se cargan los datos de ejemplo. */
    suspend fun esperarBaseLista() = seedJob.join()

    val authRepository = AuthRepository(
        usuarioDao = database.usuarioDao(),
        rolDao = database.rolDao(),
        sessionManager = sessionManager,
        intentos = LoginAttemptStore(appContext),
        esperarBaseLista = ::esperarBaseLista
    )
}
