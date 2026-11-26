package com.forcetech.jmfitapp.data.repository

import com.forcetech.jmfitapp.data.dao.ClienteDao
import com.forcetech.jmfitapp.util.DatosPrueba
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

/**
 * ClienteRepository con el DAO simulado (Mockito).
 * HU-27B/29 (rutina asignada), HU-33 (perfil), HU-36 (entrenador) y HU-51/57 (clientes del entrenador).
 */
class ClienteRepositoryMockitoTest {

    private lateinit var dao: ClienteDao
    private lateinit var repo: ClienteRepository
    private var esperas = 0

    @Before
    fun setUp() {
        dao = mock()
        esperas = 0
        repo = ClienteRepository(dao) { esperas++ }
    }

    // ---------- HU-33: MY PROFILE ----------

    @Test
    fun perfil_devuelveLaFilaDelDaoYEsperaLaBase() = runTest {
        val fila = DatosPrueba.perfil(10L)
        whenever(dao.perfil(10L)).thenReturn(fila)

        assertSame(fila, repo.perfil(10L))
        assertEquals(1, esperas)
        verify(dao).perfil(10L)
    }

    @Test
    fun perfil_usuarioInexistenteDevuelveNull() = runTest {
        whenever(dao.perfil(99L)).thenReturn(null)

        assertNull(repo.perfil(99L))
    }

    // ---------- HU-27B / HU-29: rutina asignada ----------

    @Test
    fun rutinaAsignada_sinRutinaDevuelveNullYNoConsultaEjercicios() = runTest {
        whenever(dao.rutinaAsignada(10L)).thenReturn(null)

        assertNull(repo.rutinaAsignada(10L))
        verify(dao).rutinaAsignada(10L)
        verify(dao, never()).ejerciciosDeRutina(any())
    }

    @Test
    fun rutinaAsignada_agrupaLosEjerciciosPorDia() = runTest {
        val datos = DatosPrueba.rutina(rutinaId = 7L)
        whenever(dao.rutinaAsignada(10L)).thenReturn(datos)
        whenever(dao.ejerciciosDeRutina(7L)).thenReturn(
            listOf(
                DatosPrueba.ejercicio(1, "Press banca"),
                DatosPrueba.ejercicio(1, "Remo con barra"),
                DatosPrueba.ejercicio(3, "Sentadilla")
            )
        )

        val rutina = repo.rutinaAsignada(10L)!!

        assertSame(datos, rutina.datos)
        assertEquals(listOf("Press banca", "Remo con barra"), rutina.ejerciciosDe(1).map { it.nombre })
        assertEquals(listOf("Sentadilla"), rutina.ejerciciosDe(3).map { it.nombre })
        verify(dao).ejerciciosDeRutina(7L)
    }

    @Test
    fun rutinaAsignada_diaSinEjerciciosEsDescanso() = runTest {
        whenever(dao.rutinaAsignada(10L)).thenReturn(DatosPrueba.rutina(7L))
        whenever(dao.ejerciciosDeRutina(7L)).thenReturn(
            listOf(DatosPrueba.ejercicio(1, "Press banca"), DatosPrueba.ejercicio(5, "Peso muerto"))
        )

        val rutina = repo.rutinaAsignada(10L)!!

        assertEquals(2, rutina.diasConEjercicios)
        assertTrue(rutina.ejerciciosDe(7).isEmpty()) // domingo: descanso
    }

    @Test
    fun rutinaAsignada_rutinaSinEjerciciosNoTieneDiasDeEntreno() = runTest {
        whenever(dao.rutinaAsignada(10L)).thenReturn(DatosPrueba.rutina(7L))
        whenever(dao.ejerciciosDeRutina(7L)).thenReturn(emptyList())

        assertEquals(0, repo.rutinaAsignada(10L)!!.diasConEjercicios)
    }

    @Test(expected = IllegalStateException::class)
    fun rutinaAsignada_propagaElErrorDeLaBase() = runTest {
        whenever(dao.rutinaAsignada(10L)).thenThrow(IllegalStateException("BD no disponible"))

        repo.rutinaAsignada(10L)
    }

    // ---------- HU-36: entrenador para WhatsApp ----------

    @Test
    fun entrenadorAsignado_delegaEnElDao() = runTest {
        val entrenador = DatosPrueba.entrenador()
        whenever(dao.entrenadorAsignado(10L)).thenReturn(entrenador)

        assertSame(entrenador, repo.entrenadorAsignado(10L))
        assertEquals(1, esperas)
    }

    @Test
    fun entrenadorAsignado_sinEntrenadorDevuelveNull() = runTest {
        whenever(dao.entrenadorAsignado(10L)).thenReturn(null)

        assertNull(repo.entrenadorAsignado(10L))
    }

    // ---------- HU-51 / HU-57: Mis clientes ----------

    @Test
    fun clientesDeEntrenador_devuelveLaListaDelDao() = runTest {
        val lista = listOf(DatosPrueba.cliente(1, "Camila Restrepo"), DatosPrueba.cliente(2, "Felipe Ramírez"))
        whenever(dao.clientesDeEntrenador(5L)).thenReturn(lista)

        assertEquals(lista, repo.clientesDeEntrenador(5L))
        verify(dao).clientesDeEntrenador(5L)
        verifyNoMoreInteractions(dao)
    }
}
