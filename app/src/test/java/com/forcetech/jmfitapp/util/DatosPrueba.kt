package com.forcetech.jmfitapp.util

import com.forcetech.jmfitapp.data.dao.ClienteDeEntrenadorFila
import com.forcetech.jmfitapp.data.dao.EjercicioClienteFila
import com.forcetech.jmfitapp.data.dao.EntrenadorContactoFila
import com.forcetech.jmfitapp.data.dao.PerfilClienteFila
import com.forcetech.jmfitapp.data.dao.RutinaAsignadaFila
import com.forcetech.jmfitapp.model.EstadoUsuario
import com.forcetech.jmfitapp.model.TecnicaEjercicio
import com.forcetech.jmfitapp.model.TipoAgrupacion

/** Filas de ejemplo reutilizadas por las pruebas con Mockito. */
object DatosPrueba {

    fun perfil(id: Long = 10L) = PerfilClienteFila(
        id = id, nombreUsuario = "camila", nombreCompleto = "Camila Restrepo", cedula = "1000000001",
        correo = "camila@correo.com", celular = "3000000001", creadoEn = 0L,
        fechaNacimiento = "1995-04-12", sexo = "F", estaturaCm = 165, tipoRutina = "PERSONALIZADA"
    )

    fun rutina(rutinaId: Long = 7L) = RutinaAsignadaFila(
        rutinaId = rutinaId, nombre = "Fuerza tren superior", objetivo = "Hipertrofia", nivel = "Intermedio",
        notas = null, fechaInicio = 0L, asignadoPor = "Juan Entrenador"
    )

    fun ejercicio(
        dia: Int,
        nombre: String,
        tipo: String = TipoAgrupacion.SIMPLE,
        series: Int? = 4,
        reps: String? = "10",
        pesoKg: Double? = null,
        descanso: Int? = null,
        tecnica: String = TecnicaEjercicio.NORMAL
    ) = EjercicioClienteFila(dia, nombre, tipo, series, reps, pesoKg, descanso, tecnica, null)

    fun entrenador(celular: String? = "3000000002") =
        EntrenadorContactoFila(nombre = "Juan Entrenador", celular = celular, especialidad = "Fuerza")

    fun cliente(id: Long, nombre: String, estado: String = EstadoUsuario.ACTIVO, rutina: String? = null) =
        ClienteDeEntrenadorFila(id, nombre, nombre.lowercase(), estado, rutina)
}
