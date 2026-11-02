package com.forcetech.jmfitapp.ui.trainer

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.forcetech.jmfitapp.R
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Barra inferior del entrenador. Los ids del menú (menu_entrenador.xml) son los mismos de los destinos
 * de nav_trainer.xml, así que cada ícono navega al destino de su mismo id.
 * Las pestañas siempre cuelgan de "Rutinas" (inicio del subgrafo): "Atrás" desde cualquiera vuelve a Rutinas.
 */
object BarraEntrenador {

    fun configurar(barra: BottomNavigationView, nav: NavController, actual: Int) {
        barra.setOnItemSelectedListener(null)
        barra.selectedItemId = actual
        barra.setOnItemSelectedListener { item ->
            if (item.itemId != actual) irA(nav, item.itemId)
            true
        }
    }

    private fun irA(nav: NavController, destino: Int) {
        if (destino == R.id.trainerRutinasFragment) {
            nav.popBackStack(R.id.trainerRutinasFragment, false)
        } else {
            val opciones = NavOptions.Builder()
                .setPopUpTo(R.id.trainerRutinasFragment, false)
                .setLaunchSingleTop(true)
                .build()
            nav.navigate(destino, null, opciones)
        }
    }
}
