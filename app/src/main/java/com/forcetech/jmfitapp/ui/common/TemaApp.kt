package com.forcetech.jmfitapp.ui.common

import android.content.Context
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import com.forcetech.jmfitapp.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Opciones de tema que puede elegir el usuario. */
enum class ModoTema(val modoNoche: Int, @StringRes val etiqueta: Int) {
    CLARO(AppCompatDelegate.MODE_NIGHT_NO, R.string.tema_claro),
    OSCURO(AppCompatDelegate.MODE_NIGHT_YES, R.string.tema_oscuro),
    SISTEMA(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, R.string.tema_sistema)
}

/**
 * Guarda y aplica el tema claro/oscuro en toda la app.
 * Se guarda en un archivo propio (no en la sesión) para que no se pierda al cerrar sesión.
 */
object TemaApp {

    private const val ARCHIVO = "jmfit_preferencias"
    private const val K_TEMA = "tema"

    /** Tema guardado. Por defecto, claro (el diseño original de la app). */
    fun actual(context: Context): ModoTema {
        val guardado = prefs(context).getString(K_TEMA, null)
        return ModoTema.values().firstOrNull { it.name == guardado } ?: ModoTema.CLARO
    }

    /** Se llama al iniciar la app para usar el tema que el usuario dejó elegido. */
    fun aplicarGuardado(context: Context) {
        AppCompatDelegate.setDefaultNightMode(actual(context).modoNoche)
    }

    /** Guarda el tema y lo aplica: Android redibuja las pantallas abiertas con los nuevos colores. */
    fun cambiar(context: Context, modo: ModoTema) {
        prefs(context).edit { putString(K_TEMA, modo.name) }
        AppCompatDelegate.setDefaultNightMode(modo.modoNoche)
    }

    /** Muestra el diálogo para elegir Claro / Oscuro / Según el sistema. */
    fun mostrarSelector(context: Context) {
        val opciones = ModoTema.values()
        val etiquetas = opciones.map { context.getString(it.etiqueta) }.toTypedArray()
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.tema_titulo)
            .setSingleChoiceItems(etiquetas, opciones.indexOf(actual(context))) { dialogo, posicion ->
                dialogo.dismiss()
                cambiar(context, opciones[posicion])
            }
            .setNegativeButton(R.string.tema_cancelar, null)
            .show()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
}
