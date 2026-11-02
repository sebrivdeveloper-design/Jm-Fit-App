package com.forcetech.jmfitapp

import android.app.Application

class JmFitApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
