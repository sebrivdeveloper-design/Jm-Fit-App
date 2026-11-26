package com.forcetech.jmfitapp.ui.intern

import android.content.Context
import android.content.res.Resources
import com.forcetech.jmfitapp.R
import com.forcetech.jmfitapp.model.TecnicaEjercicio
import com.forcetech.jmfitapp.model.TipoAgrupacion
import com.forcetech.jmfitapp.util.DatosPrueba
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Textos de la rutina en HOME (HU-27B) y WORKOUT (HU-29), con Context y Resources simulados.
 * Cada recurso devuelve "<ETIQUETA>:<args>" para comprobar qué partes se arman y en qué orden.
 */
class FormatoRutinaTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        val resources = mock<Resources> {
            on { getStringArray(R.array.cli_dias_largos) } doReturn
                arrayOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
            on { getStringArray(R.array.cli_dias_cortos) } doReturn arrayOf("L", "M", "X", "J", "V", "S", "D")
        }
        context = mock { on { this.resources } doReturn resources }

        simular(R.string.cli_series_reps, "SR")
        simular(R.string.cli_series_solo, "S")
        simular(R.string.cli_reps_solo, "R")
        simular(R.string.cli_peso_kg, "KG")
        simular(R.string.cli_descanso_seg, "DESC")
        whenever(context.getString(R.string.cli_dropset)).thenReturn("Dropset")
    }

    private fun simular(id: Int, etiqueta: String) {
        whenever(context.getString(eq(id), anyVararg())).thenAnswer { inv ->
            etiqueta + ":" + inv.arguments.drop(1).joinToString(",")
        }
    }

    @Test
    fun nombreDia_usaElArregloDeRecursos() {
        assertEquals("Lunes", FormatoRutina.nombreDia(context, 1))
        assertEquals("Domingo", FormatoRutina.nombreDia(context, 7))
        assertEquals("V", FormatoRutina.nombreCortoDia(context, 5))
    }

    @Test
    fun nombreDia_fueraDeRangoDevuelveVacio() {
        assertEquals("", FormatoRutina.nombreDia(context, 0))
        assertEquals("", FormatoRutina.nombreDia(context, 8))
    }

    @Test
    fun detalle_ejercicioCompleto_unePartesEnOrden() {
        val e = DatosPrueba.ejercicio(
            dia = 1, nombre = "Curl", tipo = TipoAgrupacion.BISERIE, series = 4, reps = "10",
            pesoKg = 17.5, descanso = 90, tecnica = TecnicaEjercicio.DROPSET
        )

        assertEquals("Biserie · SR:4,10 · KG:17,5 · DESC:90 · Dropset", FormatoRutina.detalle(context, e))
    }

    @Test
    fun detalle_ejercicioSimple_noMuestraElTipoDeBloque() {
        val e = DatosPrueba.ejercicio(1, "Press banca", series = 3, reps = "12")

        assertEquals("SR:3,12", FormatoRutina.detalle(context, e))
    }

    @Test
    fun detalle_omiteLosDatosQueFaltan() {
        // HU-27B c.a. 3: los datos que faltan se omiten sin error
        assertEquals("S:4", FormatoRutina.detalle(context, DatosPrueba.ejercicio(1, "A", reps = null)))
        assertEquals("R:15", FormatoRutina.detalle(context, DatosPrueba.ejercicio(1, "B", series = null, reps = "15")))
        assertEquals("", FormatoRutina.detalle(context, DatosPrueba.ejercicio(1, "C", series = null, reps = "  ")))
    }

    @Test
    fun repeticiones_enBlancoEsNull() {
        assertNull(FormatoRutina.repeticiones(DatosPrueba.ejercicio(1, "A", reps = " ")))
        assertEquals("8-10", FormatoRutina.repeticiones(DatosPrueba.ejercicio(1, "A", reps = "8-10")))
    }

    @Test
    fun peso_usaComaDecimalYSinCerosSobrantes() {
        assertEquals("18", FormatoRutina.peso(18.0))
        assertEquals("17,5", FormatoRutina.peso(17.5))
    }

    @Test
    fun tipoBloque_soloNombraLosBloquesCompuestos() {
        assertNull(FormatoRutina.tipoBloque(TipoAgrupacion.SIMPLE))
        assertEquals("Biserie", FormatoRutina.tipoBloque(TipoAgrupacion.BISERIE))
        assertEquals("Triserie", FormatoRutina.tipoBloque(TipoAgrupacion.TRISERIE))
        assertEquals("Circuito", FormatoRutina.tipoBloque(TipoAgrupacion.CIRCUITO))
    }

    @Test
    fun pesoYDescanso_nullSiNoHayDato() {
        val e = DatosPrueba.ejercicio(1, "A", pesoKg = null, descanso = null)

        assertNull(FormatoRutina.pesoTexto(context, e))
        assertNull(FormatoRutina.descanso(context, e))
    }
}
