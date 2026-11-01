package com.forcetech.jmfitapp

import com.forcetech.jmfitapp.data.security.PasswordHasher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {

    @Test
    fun pbkdf2_coincide_con_vector_de_referencia() {
        // Valores calculados con hashlib.pbkdf2_hmac('sha256', ...) de Python.
        val simple = PasswordHasher.pbkdf2("password".toByteArray(), "salt".toByteArray(), 1000, 32)
        assertEquals(
            "632c2812e46d4604102ba7618e9d6d7d2f8128f6266b4a03264d2a0460b7dcb3",
            PasswordHasher.aHex(simple)
        )

        val sal = ByteArray(16) { it.toByte() }
        val conIteraciones = PasswordHasher.pbkdf2("Admin#2026".toByteArray(), sal, 100_000, 32)
        assertEquals(
            "74b53b0d63edf53ba32bbe0e82aa27cf21e9b8806c8aa368a4ccddaf562cd902",
            PasswordHasher.aHex(conIteraciones)
        )
    }

    @Test
    fun hash_y_verify_aceptan_la_clave_correcta() {
        val guardado = PasswordHasher.hash("Cliente#26")
        assertTrue(PasswordHasher.verify("Cliente#26", guardado))
    }

    @Test
    fun verify_rechaza_clave_incorrecta_o_vacia() {
        val guardado = PasswordHasher.hash("Cliente#26")
        assertFalse(PasswordHasher.verify("cliente#26", guardado))
        assertFalse(PasswordHasher.verify("", guardado))
    }

    @Test
    fun cada_hash_usa_una_sal_distinta() {
        assertNotEquals(PasswordHasher.hash("Misma#123"), PasswordHasher.hash("Misma#123"))
    }

    @Test
    fun verify_no_falla_con_formatos_invalidos() {
        assertFalse(PasswordHasher.verify("x", ""))
        assertFalse(PasswordHasher.verify("x", "texto-plano"))
        assertFalse(PasswordHasher.verify("x", "pbkdf2_sha256\$abc\$00\$00"))
        assertFalse(PasswordHasher.verify("x", "pbkdf2_sha256\$1000\$zz\$00"))
        assertFalse(PasswordHasher.verify("x", "otro\$1000\$00\$00"))
    }
}
