package com.forcetech.jmfitapp.data.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Cifrado de contraseñas (requisito no funcional de la entrega).
 *
 * Usa PBKDF2 con HMAC-SHA256 y una sal aleatoria por usuario. Se implementa con [Mac] porque el
 * algoritmo "PBKDF2WithHmacSHA256" de SecretKeyFactory solo existe desde Android 8 y la app admite API 24.
 *
 * Formato guardado: `pbkdf2_sha256$<iteraciones>$<sal en hex>$<hash en hex>`
 */
object PasswordHasher {

    private const val PREFIJO = "pbkdf2_sha256"
    private const val ITERACIONES = 100_000
    private const val BYTES_SAL = 16
    private const val BYTES_HASH = 32

    private val random = SecureRandom()

    fun hash(password: String): String {
        val sal = ByteArray(BYTES_SAL).also { random.nextBytes(it) }
        val derivada = pbkdf2(password.toByteArray(Charsets.UTF_8), sal, ITERACIONES, BYTES_HASH)
        return listOf(PREFIJO, ITERACIONES.toString(), aHex(sal), aHex(derivada)).joinToString("$")
    }

    /** Devuelve false ante cualquier formato inválido, nunca lanza excepción por datos corruptos. */
    fun verify(password: String, guardado: String): Boolean {
        if (password.isEmpty()) return false
        val partes = guardado.split('$')
        if (partes.size != 4 || partes[0] != PREFIJO) return false
        val iteraciones = partes[1].toIntOrNull() ?: return false
        if (iteraciones <= 0) return false
        val sal = deHex(partes[2]) ?: return false
        val esperado = deHex(partes[3]) ?: return false
        if (esperado.isEmpty()) return false

        val calculado = pbkdf2(password.toByteArray(Charsets.UTF_8), sal, iteraciones, esperado.size)
        // Comparación en tiempo constante
        return MessageDigest.isEqual(calculado, esperado)
    }

    /** PBKDF2-HMAC-SHA256 (RFC 8018). Visible para las pruebas unitarias. */
    internal fun pbkdf2(password: ByteArray, sal: ByteArray, iteraciones: Int, largo: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(password, "HmacSHA256"))

        val tamanoBloque = mac.macLength
        val bloques = (largo + tamanoBloque - 1) / tamanoBloque
        val resultado = ByteArray(largo)

        for (indice in 1..bloques) {
            mac.update(sal)
            mac.update(byteArrayOf(
                (indice ushr 24).toByte(),
                (indice ushr 16).toByte(),
                (indice ushr 8).toByte(),
                indice.toByte()
            ))
            var u = mac.doFinal()
            val t = u.copyOf()
            for (i in 2..iteraciones) {
                u = mac.doFinal(u)
                for (j in t.indices) t[j] = (t[j].toInt() xor u[j].toInt()).toByte()
            }
            val desde = (indice - 1) * tamanoBloque
            val cuantos = minOf(tamanoBloque, largo - desde)
            System.arraycopy(t, 0, resultado, desde, cuantos)
        }
        return resultado
    }

    internal fun aHex(bytes: ByteArray): String {
        val digitos = "0123456789abcdef"
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            sb.append(digitos[v ushr 4]).append(digitos[v and 0x0F])
        }
        return sb.toString()
    }

    internal fun deHex(texto: String): ByteArray? {
        if (texto.length % 2 != 0) return null
        val salida = ByteArray(texto.length / 2)
        for (i in salida.indices) {
            val alto = Character.digit(texto[i * 2], 16)
            val bajo = Character.digit(texto[i * 2 + 1], 16)
            if (alto < 0 || bajo < 0) return null
            salida[i] = ((alto shl 4) or bajo).toByte()
        }
        return salida
    }
}
