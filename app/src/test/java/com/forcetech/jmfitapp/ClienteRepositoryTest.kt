package com.forcetech.jmfitapp

import com.forcetech.jmfitapp.data.dao.ClienteDeEntrenadorFila
import com.forcetech.jmfitapp.data.repository.ClienteRepository
import com.forcetech.jmfitapp.ui.trainer.MisClientesUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

/** Reglas puras de HU-27B (día de la semana), HU-33 (edad), HU-36 (WhatsApp) y HU-51/57 (Mis clientes). */
class ClienteRepositoryTest {

    private fun fecha(anio: Int, mes: Int, dia: Int): Calendar =
        Calendar.getInstance().apply { clear(); set(anio, mes - 1, dia) }

    @Test
    fun diaSemana_lunesEsUnoYDomingoEsSiete() {
        assertEquals(1, ClienteRepository.diaSemana(fecha(2026, 11, 16))) // lunes
        assertEquals(5, ClienteRepository.diaSemana(fecha(2026, 11, 20))) // viernes
        assertEquals(7, ClienteRepository.diaSemana(fecha(2026, 11, 22))) // domingo
    }

    @Test
    fun edad_cuentaSoloAniosCumplidos() {
        val hoy = fecha(2026, 11, 20)
        assertEquals(31, ClienteRepository.edad("1995-04-12", hoy))
        assertEquals(30, ClienteRepository.edad("1995-11-21", hoy)) // cumple mañana
        assertEquals(31, ClienteRepository.edad("1995-11-20", hoy)) // cumple hoy
    }

    @Test
    fun edad_fechaInvalidaVaciaOFuturaDevuelveNull() {
        val hoy = fecha(2026, 11, 20)
        assertNull(ClienteRepository.edad(null, hoy))
        assertNull(ClienteRepository.edad("", hoy))
        assertNull(ClienteRepository.edad("1995-13-40", hoy))
        assertNull(ClienteRepository.edad("2030-01-01", hoy))
    }

    @Test
    fun numeroWhatsApp_agregaIndicativoDeColombia() {
        assertEquals("573000000002", ClienteRepository.numeroWhatsApp("3000000002"))
        assertEquals("573001234567", ClienteRepository.numeroWhatsApp("300 123 4567"))
        assertEquals("573001234567", ClienteRepository.numeroWhatsApp("+57 300 123 4567"))
    }

    @Test
    fun numeroWhatsApp_sinNumeroValidoDevuelveNull() {
        assertNull(ClienteRepository.numeroWhatsApp(null))
        assertNull(ClienteRepository.numeroWhatsApp(""))
        assertNull(ClienteRepository.numeroWhatsApp("12345"))
    }

    private fun cliente(id: Long, nombre: String, estado: String = "ACTIVO") =
        ClienteDeEntrenadorFila(id, nombre, nombre.lowercase(), estado, null)

    @Test
    fun misClientes_cuentaSoloActivosYBuscaSinTildes() {
        val estado = MisClientesUiState(
            cargando = false,
            todos = listOf(
                cliente(1, "Camila Restrepo"),
                cliente(2, "Felipe Ramírez"),
                cliente(3, "Pedro Inactivo", "INHABILITADO")
            )
        )
        assertEquals(2, estado.totalActivos)
        assertEquals(listOf(2L), estado.copy(texto = "ramirez").visibles.map { it.id })
        assertEquals(listOf(1L), estado.copy(texto = "  CAMI ").visibles.map { it.id })
        assertEquals(3, estado.copy(texto = "").visibles.size)
        assertEquals(0, estado.copy(texto = "zzz").visibles.size)
    }
}
