package com.forcetech.jmfitapp.data.security

/**
 * Bloqueo temporal tras varios intentos fallidos consecutivos (HU-26 y HU-49 del backlog).
 * Es lógica pura (sin Android) para poder probarla con pruebas unitarias.
 */
class LockoutPolicy(
    private val maxIntentos: Int = 5,
    private val duracionBloqueoMs: Long = 5 * 60_000L
) {

    data class Estado(val fallos: Int = 0, val bloqueadoHasta: Long = 0L)

    fun estaBloqueado(estado: Estado, ahora: Long): Boolean = estado.bloqueadoHasta > ahora

    /** Minutos que faltan, redondeado hacia arriba (mínimo 1 si sigue bloqueado). */
    fun minutosRestantes(estado: Estado, ahora: Long): Int {
        val faltan = estado.bloqueadoHasta - ahora
        if (faltan <= 0) return 0
        return ((faltan + 59_999L) / 60_000L).toInt()
    }

    /** Registra un intento fallido. Al llegar al máximo bloquea y reinicia el contador. */
    fun alFallar(estado: Estado, ahora: Long): Estado {
        // Si un bloqueo anterior ya venció, se empieza de cero.
        val base = if (estado.bloqueadoHasta in 1..ahora) Estado() else estado
        val fallos = base.fallos + 1
        return if (fallos >= maxIntentos) {
            Estado(fallos = 0, bloqueadoHasta = ahora + duracionBloqueoMs)
        } else {
            Estado(fallos = fallos, bloqueadoHasta = 0L)
        }
    }

    fun alEntrar(): Estado = Estado()
}
