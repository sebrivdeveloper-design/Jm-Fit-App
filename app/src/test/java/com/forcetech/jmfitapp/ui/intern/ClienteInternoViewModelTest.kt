package com.forcetech.jmfitapp.ui.intern

import com.forcetech.jmfitapp.data.repository.ClienteRepository
import com.forcetech.jmfitapp.data.repository.RutinaCliente
import com.forcetech.jmfitapp.util.DatosPrueba
import com.forcetech.jmfitapp.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

/**
 * ViewModel compartido por HOME (HU-27B), WORKOUT (HU-29) y MY PROFILE (HU-33),
 * con el repositorio simulado (Mockito).
 */
class ClienteInternoViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private lateinit var repo: ClienteRepository

    @Before
    fun setUp() {
        repo = mock()
    }

    private fun rutinaConLunes() = RutinaCliente(
        DatosPrueba.rutina(),
        mapOf(1 to listOf(DatosPrueba.ejercicio(1, "Press banca")))
    )

    @Test
    fun estadoInicialEsCargando() {
        val vm = ClienteInternoViewModel(repo, 10L)

        assertEquals(EstadoCliente.Cargando, vm.estado.value)
    }

    @Test
    fun cargar_conRutinaYEntrenador_quedaListoConTodosLosDatos() = runTest {
        val perfil = DatosPrueba.perfil(10L)
        val rutina = rutinaConLunes()
        val entrenador = DatosPrueba.entrenador()
        whenever(repo.perfil(10L)).thenReturn(perfil)
        whenever(repo.rutinaAsignada(10L)).thenReturn(rutina)
        whenever(repo.entrenadorAsignado(10L)).thenReturn(entrenador)

        val vm = ClienteInternoViewModel(repo, 10L)
        vm.cargar()

        val datos = (vm.estado.value as EstadoCliente.Listo).datos
        assertSame(perfil, datos.perfil)
        assertSame(rutina, datos.rutina)
        assertSame(entrenador, datos.entrenador)
    }

    @Test
    fun cargar_sinRutinaAsignada_quedaListoConRutinaNull() = runTest {
        // HU-27B / HU-29: "Aún no tienes una rutina asignada" y botón de iniciar deshabilitado
        whenever(repo.perfil(10L)).thenReturn(DatosPrueba.perfil(10L))
        whenever(repo.rutinaAsignada(10L)).thenReturn(null)
        whenever(repo.entrenadorAsignado(10L)).thenReturn(null)

        val vm = ClienteInternoViewModel(repo, 10L)
        vm.cargar()

        val datos = (vm.estado.value as EstadoCliente.Listo).datos
        assertNull(datos.rutina)
        assertNull(datos.entrenador)
    }

    @Test
    fun cargar_sinSesion_noConsultaElRepositorio() = runTest {
        val vm = ClienteInternoViewModel(repo, null)
        vm.cargar()

        assertEquals(EstadoCliente.Listo(DatosCliente(null, null, null)), vm.estado.value)
        verifyNoInteractions(repo)
    }

    @Test
    fun cargar_siLaBaseFalla_muestraError() = runTest {
        whenever(repo.perfil(10L)).thenReturn(DatosPrueba.perfil(10L))
        whenever(repo.rutinaAsignada(10L)).thenThrow(IllegalStateException("BD no disponible"))

        val vm = ClienteInternoViewModel(repo, 10L)
        vm.cargar()

        assertEquals(EstadoCliente.Error, vm.estado.value)
    }

    @Test
    fun reintentar_despuesDeUnError_cargaLosDatos() = runTest {
        // Botón [Reintentar]: el primer intento falla y el segundo funciona
        whenever(repo.perfil(10L))
            .thenThrow(IllegalStateException("BD no disponible"))
            .thenReturn(DatosPrueba.perfil(10L))
        whenever(repo.rutinaAsignada(10L)).thenReturn(rutinaConLunes())
        whenever(repo.entrenadorAsignado(10L)).thenReturn(null)

        val vm = ClienteInternoViewModel(repo, 10L)
        vm.cargar()
        assertEquals(EstadoCliente.Error, vm.estado.value)

        vm.cargar()
        assertTrue(vm.estado.value is EstadoCliente.Listo)
        verify(repo, times(2)).perfil(10L)
    }

    @Test
    fun recargar_reflejaLaRutinaVigente() = runTest {
        // HU-27B c.a. 5: al volver a la pantalla se ve el cambio de rutina
        val nueva = RutinaCliente(DatosPrueba.rutina(rutinaId = 8L), emptyMap())
        whenever(repo.perfil(any())).thenReturn(DatosPrueba.perfil(10L))
        whenever(repo.rutinaAsignada(10L)).thenReturn(rutinaConLunes()).thenReturn(nueva)
        whenever(repo.entrenadorAsignado(any())).thenReturn(null)

        val vm = ClienteInternoViewModel(repo, 10L)
        vm.cargar()
        vm.cargar()

        assertSame(nueva, (vm.estado.value as EstadoCliente.Listo).datos.rutina)
    }

    @Test
    fun factory_creaElViewModel() {
        val vm = ClienteInternoViewModel.Factory(repo, 10L).create(ClienteInternoViewModel::class.java)

        assertEquals(ClienteInternoViewModel::class.java, vm.javaClass)
    }
}
