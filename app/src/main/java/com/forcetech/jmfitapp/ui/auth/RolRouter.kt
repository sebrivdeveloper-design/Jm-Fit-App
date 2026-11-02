package com.forcetech.jmfitapp.ui.auth

import androidx.annotation.IdRes
import com.forcetech.jmfitapp.R
import com.forcetech.jmfitapp.model.RolCodigo

/** Decide a qué subgrafo de navegación entra cada rol después de iniciar sesión. */
object RolRouter {

    @IdRes
    fun graphIdFor(rolCodigo: String?): Int? = when (rolCodigo) {
        RolCodigo.SUPERADMIN -> R.id.nav_admin
        RolCodigo.ENTRENADOR -> R.id.nav_trainer
        RolCodigo.CLIENTE_INTERNO -> R.id.nav_intern
        else -> null
    }
}
