package com.forcetech.jmfitapp.model

/** Códigos de rol (tabla ROL del MER). El cliente externo NO tiene usuario ni rol: usa la tienda sin sesión. */
object RolCodigo {
    const val SUPERADMIN = "SUPERADMIN"
    const val ENTRENADOR = "ENTRENADOR"
    const val CLIENTE_INTERNO = "CLIENTE_INTERNO"
}

object EstadoUsuario {
    const val ACTIVO = "ACTIVO"
    const val INHABILITADO = "INHABILITADO"
}

object TipoRutina {
    const val ESTANDAR = "ESTANDAR"
    const val PERSONALIZADA = "PERSONALIZADA"
}

object EstadoRutina {
    const val ACTIVA = "ACTIVA"
    const val INHABILITADA = "INHABILITADA"
}

/** Estado de la asignación de una rutina a un cliente. Un cliente solo puede tener UNA asignación ACTIVA. */
object EstadoAsignacion {
    const val ACTIVA = "ACTIVA"
    const val FINALIZADA = "FINALIZADA"
}

object TipoAgrupacion {
    const val SIMPLE = "SIMPLE"
    const val BISERIE = "BISERIE"
    const val TRISERIE = "TRISERIE"
    const val CIRCUITO = "CIRCUITO"
}

object TecnicaEjercicio {
    const val NORMAL = "NORMAL"
    const val DROPSET = "DROPSET"
}

object EstadoCatalogo {
    const val ACTIVO = "ACTIVO"
    const val INACTIVO = "INACTIVO"
}

/** Opciones de los selectores de "Objetivo" y "Nivel" de las rutinas. */
object CatalogoRutinas {
    val OBJETIVOS = listOf(
        "Hipertrofia",
        "Pérdida de grasa",
        "Fuerza",
        "Resistencia",
        "Acondicionamiento general"
    )
    val NIVELES = listOf("Principiante", "Intermedio", "Avanzado")

    /** Días de la semana: 1 = lunes … 7 = domingo (columna `diaSemana` de RutinaDia). */
    val DIAS = listOf(
        1 to "Lunes", 2 to "Martes", 3 to "Miércoles", 4 to "Jueves",
        5 to "Viernes", 6 to "Sábado", 7 to "Domingo"
    )
}
