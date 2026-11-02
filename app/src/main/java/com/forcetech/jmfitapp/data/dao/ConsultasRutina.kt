package com.forcetech.jmfitapp.data.dao

/**
 * Resultados de consultas (no son tablas). Room los llena por el nombre de cada columna del SELECT.
 */

/** Una fila del listado de rutinas (ENT-6). */
data class RutinaResumen(
    val id: Long,
    val nombre: String,
    val tipo: String,
    val objetivo: String?,
    val nivel: String?,
    val estado: String,
    val totalEjercicios: Int,
    /** Cliente con la asignación ACTIVA; null si no tiene cliente. */
    val clienteNombre: String?
)

/** Un ejercicio de una rutina, con el día al que pertenece (para armar el detalle). */
data class FilaRutinaDetalle(
    val diaSemana: Int,
    val ejercicioNombre: String,
    val series: Int?,
    val repeticiones: String?,
    val pesoKg: Double?,
    val descansoSegundos: Int?
)
