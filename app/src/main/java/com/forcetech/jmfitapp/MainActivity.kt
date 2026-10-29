package com.forcetech.jmfitapp

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.forcetech.jmfitapp.data.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { v, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        // Comprobar base de datos Room
        lifecycleScope.launch {

            val db = withContext(Dispatchers.IO) {
                AppDatabase.obtener(applicationContext)
                    .openHelper.writableDatabase
            }

            val archivo = getDatabasePath("jmfitapp_db")

            Log.d("ROOM_CHECK", "Base abierta: ${db.isOpen}")
            Log.d("ROOM_CHECK", "Archivo existe: ${archivo.exists()}")
            Log.d("ROOM_CHECK", "Ruta: ${archivo.absolutePath}")
        }
    }
}
