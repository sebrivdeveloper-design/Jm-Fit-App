package com.forcetech.jmfitapp

import com.forcetech.jmfitapp.data.security.LockoutPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockoutPolicyTest {

    private val politica = LockoutPolicy(maxIntentos = 5, duracionBloqueoMs = 5 * 60_000L)
    private val t0 = 1_000_000L

    @Test
    fun cuatro_fallos_no_bloquean() {
        var estado = LockoutPolicy.Estado()
        repeat(4) { estado = politica.alFallar(estado, t0) }
        assertFalse(politica.estaBloqueado(estado, t0))
        assertEquals(4, estado.fallos)
    }

    @Test
    fun el_quinto_fallo_bloquea_cinco_minutos() {
        var estado = LockoutPolicy.Estado()
        repeat(5) { estado = politica.alFallar(estado, t0) }
        assertTrue(politica.estaBloqueado(estado, t0))
        assertEquals(5, politica.minutosRestantes(estado, t0))
        assertEquals(1, politica.minutosRestantes(estado, t0 + 4 * 60_000L + 30_000L))
    }

    @Test
    fun el_bloqueo_vence_y_el_conteo_empieza_de_cero() {
        var estado = LockoutPolicy.Estado()
        repeat(5) { estado = politica.alFallar(estado, t0) }
        val despues = t0 + 5 * 60_000L + 1
        assertFalse(politica.estaBloqueado(estado, despues))
        assertEquals(0, politica.minutosRestantes(estado, despues))

        val tras = politica.alFallar(estado, despues)
        assertEquals(1, tras.fallos)
        assertFalse(politica.estaBloqueado(tras, despues))
    }

    @Test
    fun entrar_reinicia_el_estado() {
        assertEquals(LockoutPolicy.Estado(), politica.alEntrar())
    }
}
