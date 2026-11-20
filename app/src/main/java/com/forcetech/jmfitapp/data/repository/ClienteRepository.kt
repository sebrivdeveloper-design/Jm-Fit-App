package com.forcetech.jmfitapp.data.repository

import com.forcetech.jmfitapp.data.dao.ClienteDao
import com.forcetech.jmfitapp.data.dao.ClienteDeEntrenadorFila
import com.forcetech.jmfitapp.data.dao.EjercicioClienteFila
import com.forcetech.jmfitapp.data.dao.EntrenadorContactoFila
import com.forcetech.jmfitapp.data.dao.PerfilClienteFila
import com.forcetech.jmfitapp.data.dao.RutinaAsignadaFila
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Rutina asignada al cliente con sus ejercicios agrupados por día (1 = lunes … 7 = domingo). */
data class RutinaCliente(
    val datos: RutinaAsignadaFila,
    val ejerciciosPorDia: Map<Int, List<EjercicioClienteFila>>
) {
    fun ejerciciosDe(dia: Int): List<EjercicioClienteFila> = ejerciciosPorDia[dia].orEmpty()

    /** Días con al menos un ejercicio; los demás son de descanso. */
    val diasConEjercicios: Int get() = ejerciciosPorDia.count { it.value.isNotEmpty() }
}

/**
 * Lectura de los datos reales del cliente interno (reemplaza a DatosClienteSimulados en HOME,
 * WORKOUT y MY PROFILE) y de los clientes asignados a un entrenador.
 */
class ClienteRepository(
    private val dao: ClienteDao,
    private val esperarBaseLista: suspend () -> Unit
) {

    suspend fun perfil(usuarioId: Long): PerfilClienteFila? {
        esperarBaseLista()
        return dao.perfil(usuarioId)
    }

    /** Null si el cliente no tiene una rutina activa asignada. */
    suspend fun rutinaAsignada(clienteId: Long): RutinaCliente? {
        esperarBaseLista()
        val datos = dao.rutinaAsignada(clienteId) ?: return null
        val porDia = dao.ejerciciosDeRutina(datos.rutinaId).groupBy { it.diaSemana }
        return RutinaCliente(datos, porDia)
    }

    suspend fun entrenadorAsignado(clienteId: Long): EntrenadorContactoFila? {
        esperarBaseLista()
        return dao.entrenadorAsignado(clienteId)
    }

    suspend fun clientesDeEntrenador(entrenadorId: Long): List<ClienteDeEntrenadorFila> {
        esperarBaseLista()
        return dao.clientesDeEntrenador(entrenadorId)
    }

    companion object {
        /** Día de la rutina que corresponde a una fecha: 1 = lunes … 7 = domingo. */
        fun diaSemana(fecha: Calendar = Calendar.getInstance()): Int {
            // Calendar: domingo = 1, lunes = 2 … sábado = 7
            val dia = fecha.get(Calendar.DAY_OF_WEEK)
            return if (dia == Calendar.SUNDAY) 7 else dia - 1
        }

        /** Edad cumplida a [hoy] a partir de "yyyy-MM-dd". Null si la fecha no es válida o es futura. */
        fun edad(fechaNacimiento: String?, hoy: Calendar = Calendar.getInstance()): Int? {
            if (fechaNacimiento.isNullOrBlank()) return null
            val formato = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
            val fecha = try {
                formato.parse(fechaNacimiento.trim())
            } catch (e: ParseException) {
                null
            } ?: return null
            val nacimiento = Calendar.getInstance().apply { time = fecha }
            if (nacimiento.after(hoy)) return null

            var anios = hoy.get(Calendar.YEAR) - nacimiento.get(Calendar.YEAR)
            val mesHoy = hoy.get(Calendar.MONTH)
            val mesNac = nacimiento.get(Calendar.MONTH)
            if (mesHoy < mesNac || (mesHoy == mesNac && hoy.get(Calendar.DAY_OF_MONTH) < nacimiento.get(Calendar.DAY_OF_MONTH))) {
                anios--
            }
            return anios
        }

        /**
         * Número para abrir WhatsApp (formato internacional sin "+"). Acepta celulares colombianos de
         * 10 dígitos (se antepone 57) o números que ya traen el indicativo. Null si no es válido.
         */
        fun numeroWhatsApp(celular: String?): String? {
            val digitos = celular?.filter { it.isDigit() }.orEmpty()
            return when {
                digitos.length == 10 -> "57$digitos"
                digitos.length == 12 && digitos.startsWith("57") -> digitos
                else -> null
            }
        }
    }
}
