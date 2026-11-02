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
            // Se incluye el teclado (ime) para que los campos no queden tapados al escribir.
            val barras = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )

            v.setPadding(barras.left, barras.top, barras.right, barras.bottom)

            insets
        }
    }
}
