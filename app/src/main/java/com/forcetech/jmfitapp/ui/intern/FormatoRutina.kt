package com.forcetech.jmfitapp.ui.intern

import android.content.Context
import com.forcetech.jmfitapp.R
import com.forcetech.jmfitapp.data.dao.EjercicioClienteFila
import com.forcetech.jmfitapp.model.TecnicaEjercicio
import com.forcetech.jmfitapp.model.TipoAgrupacion
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Textos de la rutina para las pantallas del cliente. Los datos que faltan se omiten sin error (HU-27B c.a. 3). */
object FormatoRutina {

    private val LOCALE_CO = Locale.forLanguageTag("es-CO")

    /** Nombre del día: 1 = Lunes … 7 = Domingo. */
    fun nombreDia(context: Context, dia: Int): String =
        context.resources.getStringArray(R.array.cli_dias_largos).getOrElse(dia - 1) { "" }

    fun nombreCortoDia(context: Context, dia: Int): String =
        context.resources.getStringArray(R.array.cli_dias_cortos).getOrElse(dia - 1) { "" }

    /** "18" o "17,5" */
    fun peso(kg: Double): String =
        DecimalFormat("#.#", DecimalFormatSymbols(LOCALE_CO)).format(kg)

    fun repeticiones(e: EjercicioClienteFila): String? = e.repeticiones?.takeIf { it.isNotBlank() }

    fun descanso(context: Context, e: EjercicioClienteFila): String? =
        e.descansoSegundos?.let { context.getString(R.string.cli_descanso_seg, it) }

    fun pesoTexto(context: Context, e: EjercicioClienteFila): String? =
        e.pesoKg?.let { context.getString(R.string.cli_peso_kg, peso(it)) }

    /** Una línea con lo que exista: "Biserie · 4 × 10 · 18 kg · 90 s · Dropset". */
    fun detalle(context: Context, e: EjercicioClienteFila): String {
        val partes = mutableListOf<String>()
        tipoBloque(e.tipoAgrupacion)?.let { partes += it }
        val reps = repeticiones(e)
        when {
            e.series != null && reps != null -> partes += context.getString(R.string.cli_series_reps, e.series, reps)
            e.series != null -> partes += context.getString(R.string.cli_series_solo, e.series)
            reps != null -> partes += context.getString(R.string.cli_reps_solo, reps)
        }
        pesoTexto(context, e)?.let { partes += it }
        descanso(context, e)?.let { partes += it }
        if (e.tecnica == TecnicaEjercicio.DROPSET) partes += context.getString(R.string.cli_dropset)
        return partes.joinToString(" · ")
    }

    /** Nombre del bloque cuando no es SIMPLE (el más común no se muestra para no saturar). */
    fun tipoBloque(tipo: String): String? = when (tipo) {
        TipoAgrupacion.BISERIE -> "Biserie"
        TipoAgrupacion.TRISERIE -> "Triserie"
        TipoAgrupacion.CIRCUITO -> "Circuito"
        else -> null
    }
}
