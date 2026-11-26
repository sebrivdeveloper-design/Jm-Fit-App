package com.forcetech.jmfitapp.ui.intern

import android.content.ActivityNotFoundException
import android.view.View
import androidx.fragment.app.Fragment
import com.forcetech.jmfitapp.util.DatosPrueba
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * HU-36: abrir WhatsApp con el entrenador asignado. El Fragment se simula con Mockito para
 * comprobar cuándo se lanza el Intent y que un error al abrirlo no cierre la app.
 */
class WhatsAppEntrenadorTest {

    private lateinit var fragment: Fragment
    private lateinit var ancla: View

    @Before
    fun setUp() {
        fragment = mock()
        ancla = mock()
        // view == null: el aviso (Snackbar) no se muestra, así la prueba no depende de la interfaz
        whenever(fragment.view).thenReturn(null)
    }

    @Test
    fun conCelularValido_abreElChat() {
        WhatsAppEntrenador.abrir(fragment, ancla, DatosPrueba.entrenador(celular = "3000000002"))

        verify(fragment).startActivity(any())
    }

    @Test
    fun sinEntrenadorAsignado_noAbreNada() {
        WhatsAppEntrenador.abrir(fragment, ancla, null)

        verify(fragment, never()).startActivity(any())
    }

    @Test
    fun conCelularInvalido_noAbreNada() {
        WhatsAppEntrenador.abrir(fragment, ancla, DatosPrueba.entrenador(celular = "12345"))
        WhatsAppEntrenador.abrir(fragment, ancla, DatosPrueba.entrenador(celular = null))

        verify(fragment, never()).startActivity(any())
    }

    @Test
    fun siNoHayAppParaAbrirlo_noLanzaError() {
        doThrow(ActivityNotFoundException()).whenever(fragment).startActivity(any())

        WhatsAppEntrenador.abrir(fragment, ancla, DatosPrueba.entrenador())

        verify(fragment).startActivity(any())
    }
}
