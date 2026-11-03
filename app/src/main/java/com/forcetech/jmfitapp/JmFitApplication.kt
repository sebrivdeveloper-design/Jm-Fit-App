package com.forcetech.jmfitapp

import android.app.Application
import com.forcetech.jmfitapp.ui.common.TemaApp

class JmFitApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // Aplica el tema elegido (claro/oscuro) antes de que se abra cualquier pantalla
        TemaApp.aplicarGuardado(this)
        container = AppContainer(this)
    }
}
