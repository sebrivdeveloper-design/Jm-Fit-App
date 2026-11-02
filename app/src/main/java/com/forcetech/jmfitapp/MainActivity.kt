package com.forcetech.jmfitapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Única Activity de la app. Aloja el NavHostFragment (ver res/navigation/nav_main.xml);
 * cada pantalla es un Fragment. Quien tenga sesión activa es redirigido por LoginFragment
 * al inicio de su rol.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { v, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())

            // Si el teclado está visible, aplicamos su altura como padding inferior.
            // Si no está visible, dejamos bottom = 0 para que la barra de navegación inferior
            // se extienda hasta el borde de la pantalla sin espacio blanco duplicado.
            val bottomPadding = if (ime.bottom > 0) ime.bottom else 0

            v.setPadding(statusBars.left, statusBars.top, statusBars.right, bottomPadding)

            insets
        }
    }
}
