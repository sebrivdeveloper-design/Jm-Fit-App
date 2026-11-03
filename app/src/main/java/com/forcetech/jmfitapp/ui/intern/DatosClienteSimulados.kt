package com.forcetech.jmfitapp.ui.intern

/**
 * Datos estáticos de las pantallas del cliente interno (HU Cliente — CA06).
 * No vienen de la base de datos ni de una API; solo representan la información de la demostración.
 * Cuando se conecten los datos reales, basta con reemplazar este objeto por un repositorio.
 */
object DatosClienteSimulados {

    /** Se usan si no hay sesión activa (por ejemplo, en una vista previa). */
    const val NOMBRE_POR_DEFECTO = "Camila Restrepo"

    const val ENTRENADOR = "Juan Madroñero"
    const val ENTRENADOR_NOMBRE_CORTO = "Juan"

    data class Ejercicio(
        val nombre: String,
        val series: Int,
        val repeticiones: String,
        val peso: String,
        val descanso: String,
        val indicacion: String
    )

    data class Entrenamiento(
        val nombreCorto: String,
        val nombreCompleto: String,
        val grupoMuscular: String,
        val dia: Int,
        val diasTotales: Int,
        val semana: Int,
        val minutosEstimados: Int,
        val ejercicios: List<Ejercicio>
    )

    val entrenamientoDeHoy = Entrenamiento(
        nombreCorto = "Día de empuje",
        nombreCompleto = "Día de empuje — Pecho y hombros",
        grupoMuscular = "Pecho, hombros y tríceps",
        dia = 3,
        diasTotales = 5,
        semana = 6,
        minutosEstimados = 50,
        ejercicios = listOf(
            Ejercicio(
                "Press de banca con barra", 4, "8-10", "70 kg", "90 s",
                "Pies firmes, ligero arco, barra a la mitad del pecho. Controla la bajada."
            ),
            Ejercicio(
                "Press inclinado con mancuernas", 3, "10", "22 kg", "75 s",
                "Banco a 30°. Aprieta arriba y no dejes que las mancuernas se abran."
            ),
            Ejercicio(
                "Aperturas en polea", 3, "12", "15 kg", "60 s",
                "Inclínate un poco hacia adelante y baja lento. Concéntrate en apretar, no en el peso."
            ),
            Ejercicio(
                "Press militar sentado", 4, "8", "40 kg", "90 s",
                "Abdomen firme. Empuja hacia arriba, no hacia adelante."
            ),
            Ejercicio(
                "Extensión de tríceps en polea", 3, "12", "25 kg", "60 s",
                "Codos pegados al cuerpo durante toda la serie."
            )
        )
    )

    // Resumen semanal (Home)
    const val ENTRENOS_SEMANA = "3/4"
    const val CAMBIO_PESO = "-0,8 kg"
    const val RACHA = "6 días"
    val SEMANAS_FOTOS = listOf("Sem 1", "Sem 4", "Sem 8")

    // Mensaje del entrenador (Home)
    const val MENSAJE_ENTRENADOR = "Buen trabajo hoy. Mantén el codo pegado en el press…"
    const val HACE_MENSAJE = "hace 2 min"

    // Perfil
    const val CLIENTE_DESDE = "ago 2026"
    const val CIUDAD = "Bogotá"
    const val PROGRAMA = "Hipertrofia — Fase 2"
    const val SEMANA_PROGRAMA = 6
    const val SEMANAS_PROGRAMA = 12
    const val SESIONES_SEMANA = "4"
    const val FECHA_INICIO = "4 ago"
    const val PROXIMO_CONTROL = "2 oct"
    const val EDAD = "29"
    const val ESTATURA = "1,68 m"
    const val PESO_ACTUAL = "78,4 kg"
    const val PESO_OBJETIVO = "74 kg"
    val OBJETIVOS = listOf("Pérdida de grasa", "Ganancia muscular", "Intermedio")
    const val NOTAS_ENTRENADOR =
        "Leve molestia en el hombro izquierdo: se redujo la carga en los press por encima de la cabeza. " +
            "Días de entrenamiento: lun, mar, jue y sáb."
}
