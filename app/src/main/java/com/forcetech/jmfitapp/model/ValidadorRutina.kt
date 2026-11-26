package com.forcetech.jmfitapp.model

/**
 * Reglas de los campos de un ejercicio del formulario de rutina (ENT-5 y ENT-8),
 * independientes de Android para poder probarlas sin la interfaz.
 */
object ValidadorRutina {

    const val SERIES_MINIMAS = 1
    const val SERIES_MAXIMAS = 20
    const val REPETICIONES_MAX_CARACTERES = 20
    const val PESO_MAXIMO_KG = 999.9

    enum class Campo { SERIES, REPETICIONES, PESO }

    sealed interface ResultadoEjercicio {
        /** [campos] son los que tienen error; la pantalla marca cada uno. */
        data class Invalido(val campos: Set<Campo>) : ResultadoEjercicio

        /** [pesoKg] es null cuando el peso se deja vacío (es opcional). */
        data class Valido(val series: Int, val repeticiones: String, val pesoKg: Double?) : ResultadoEjercicio
    }

    private val PESO_DECIMAL = Regex("""(\d+(\.\d*)?|\.\d+)""")

    /**
     * - Series: número entero entre 1 y 20.
     * - Repeticiones: al menos un número y hasta 20 caracteres (10, 8-12, 12/10/8…).
     * - Peso: opcional; si se escribe debe ser mayor que 0 y no pasar de 999,9 (acepta coma o punto).
     */
    fun validarEjercicio(series: String, repeticiones: String, peso: String): ResultadoEjercicio {
        val errores = mutableSetOf<Campo>()

        val textoSeries = series.trim()
        val numeroSeries = if (textoSeries.isNotEmpty() && textoSeries.all { it in '0'..'9' }) {
            textoSeries.toIntOrNull()
        } else {
            null
        }
        if (numeroSeries == null || numeroSeries !in SERIES_MINIMAS..SERIES_MAXIMAS) errores += Campo.SERIES

        val reps = repeticiones.trim()
        if (reps.isEmpty() || reps.none { it in '0'..'9' } || reps.length > REPETICIONES_MAX_CARACTERES) {
            errores += Campo.REPETICIONES
        }

        val textoPeso = peso.trim().replace(',', '.')
        val numeroPeso: Double? = if (textoPeso.isEmpty()) {
            null
        } else if (PESO_DECIMAL.matches(textoPeso)) {
            textoPeso.toDoubleOrNull()
        } else {
            null
        }
        if (textoPeso.isNotEmpty() && (numeroPeso == null || numeroPeso <= 0.0 || numeroPeso > PESO_MAXIMO_KG)) {
            errores += Campo.PESO
        }

        if (errores.isNotEmpty() || numeroSeries == null) return ResultadoEjercicio.Invalido(errores)
        return ResultadoEjercicio.Valido(numeroSeries, reps, numeroPeso)
    }
}
