package com.forcetech.jmfitapp.data.dao

/** Opción de cliente para el selector del formulario. [toString] es lo que muestra y filtra la lista desplegable. */
data class ClienteOpcion(val id: Long, val nombre: String) {
    override fun toString(): String = nombre
}

/** Opción de ejercicio del catálogo para el selector del formulario. */
data class EjercicioOpcion(val id: Long, val nombre: String) {
    override fun toString(): String = nombre
}

/** Un ejercicio de una rutina, tal como se carga para editarla. */
data class FilaEdicion(
    val diaSemana: Int,
    val ejercicioId: Long,
    val ejercicioNombre: String,
    val series: Int?,
    val repeticiones: String?,
    val pesoKg: Double?
)
