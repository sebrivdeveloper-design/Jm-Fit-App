package com.forcetech.jmfitapp.ui.intern

import android.content.Context
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.forcetech.jmfitapp.JmFitApplication
import com.forcetech.jmfitapp.R
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Barra inferior del cliente interno (mismo patrón que BarraEntrenador).
 * Los ids de menu_cliente.xml coinciden con los destinos de nav_intern.xml.
 * Progreso y Chat se muestran pero no navegan (fuera del alcance de la HU).
 * Workout y Perfil siempre cuelgan de Home, así "Atrás" desde cualquiera vuelve a Home.
 */
object BarraCliente {

    private val DESTINOS = setOf(
        R.id.internHomeFragment,
        R.id.internWorkoutFragment,
        R.id.internProfileFragment
    )

    fun configurar(barra: BottomNavigationView, nav: NavController, actual: Int) {
        // Cada pantalla marca su propia pestaña. Sin esto, al volver con "Atrás" la barra
        // restauraría la pestaña que se tocó al salir (p. ej. Perfil) en lugar de Inicio.
        barra.isSaveEnabled = false
        barra.setOnItemSelectedListener(null)
        barra.selectedItemId = actual
        barra.setOnItemSelectedListener { item ->
            when {
                item.itemId !in DESTINOS -> false // solo visual: no cambia la pestaña activa
                item.itemId == actual -> true
                else -> {
                    irA(nav, item.itemId)
                    true
                }
            }
        }
        // Volver a tocar la pestaña activa no hace nada
        barra.setOnItemReselectedListener { }
    }

    fun irA(nav: NavController, destino: Int) {
        if (destino == R.id.internHomeFragment) {
            nav.popBackStack(R.id.internHomeFragment, false)
        } else {
            val opciones = NavOptions.Builder()
                .setPopUpTo(R.id.internHomeFragment, false)
                .setLaunchSingleTop(true)
                .build()
            nav.navigate(destino, null, opciones)
        }
    }

    /** Nombre completo de quien inició sesión; si no hay sesión se usa el nombre simulado. */
    fun nombreCliente(context: Context): String {
        val app = context.applicationContext as JmFitApplication
        return app.container.sessionManager.sesionActual()?.nombreCompleto
            ?.takeIf { it.isNotBlank() }
            ?: DatosClienteSimulados.NOMBRE_POR_DEFECTO
    }
}
