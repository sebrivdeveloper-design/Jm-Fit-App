package com.forcetech.jmfitapp.data.database

import androidx.room.withTransaction
import com.forcetech.jmfitapp.data.security.PasswordHasher
import com.forcetech.jmfitapp.model.AsignacionEntrenador
import com.forcetech.jmfitapp.model.AsignacionRutina
import com.forcetech.jmfitapp.model.ClienteInterno
import com.forcetech.jmfitapp.model.Ejercicio
import com.forcetech.jmfitapp.model.Entrenador
import com.forcetech.jmfitapp.model.EstadoUsuario
import com.forcetech.jmfitapp.model.GrupoMuscular
import com.forcetech.jmfitapp.model.Rol
import com.forcetech.jmfitapp.model.RolCodigo
import com.forcetech.jmfitapp.model.Rutina
import com.forcetech.jmfitapp.model.RutinaBloque
import com.forcetech.jmfitapp.model.RutinaDia
import com.forcetech.jmfitapp.model.RutinaEjercicio
import com.forcetech.jmfitapp.model.TipoRutina
import com.forcetech.jmfitapp.model.Usuario

/**
 * Datos de ejemplo para poder probar la app sin backend. Se cargan una sola vez, cuando la base está vacía.
 *
 * USUARIOS DE PRUEBA (usuario / contraseña):
 *   admin         / Admin#2026   → Superadministrador
 *   entrenador    / Entren#26    → Entrenador (Juan Madroñero; tiene a Camila, Felipe y Laura)
 *   entrenador2   / Entren#26    → Entrenador (Carolina Ortiz; tiene a Diego)
 *   camila, felipe, laura, diego / Cliente#26 → Clientes internos
 *   inactivo      / Cliente#26   → Cliente interno INHABILITADO (para probar el mensaje de cuenta inhabilitada)
 */
object DatabaseSeeder {

    const val PASS_ADMIN = "Admin#2026"
    const val PASS_ENTRENADOR = "Entren#26"
    const val PASS_CLIENTE = "Cliente#26"

    private const val ROL_SUPERADMIN = 1
    private const val ROL_ENTRENADOR = 2
    private const val ROL_CLIENTE = 3

    private class EjercicioSeed(
        val nombre: String,
        val grupoId: Int,
        val descripcion: String
    )

    /** Un ejercicio dentro de un día de una rutina de ejemplo. */
    private class LineaSeed(
        val ejercicio: String,
        val series: Int,
        val repeticiones: String,
        val pesoKg: Double? = null,
        val descanso: Int = 60
    )

    private class DiaSeed(val dia: Int, val lineas: List<LineaSeed>)

    suspend fun seedIfNeeded(db: AppDatabase) {
        if (db.rolDao().contar() > 0) return

        // El hash es costoso (PBKDF2), así que se calcula antes de abrir la transacción.
        val hashAdmin = PasswordHasher.hash(PASS_ADMIN)
        val hashEntrenador = PasswordHasher.hash(PASS_ENTRENADOR)
        val hashCliente = PasswordHasher.hash(PASS_CLIENTE)

        db.withTransaction {
            val rolDao = db.rolDao()
            val usuarioDao = db.usuarioDao()
            val ejercicioDao = db.ejercicioDao()
            val rutinaDao = db.rutinaDao()

            // ---- Roles ----
            rolDao.insertarTodos(
                listOf(
                    Rol(ROL_SUPERADMIN, RolCodigo.SUPERADMIN, "Superadministrador"),
                    Rol(ROL_ENTRENADOR, RolCodigo.ENTRENADOR, "Entrenador"),
                    Rol(ROL_CLIENTE, RolCodigo.CLIENTE_INTERNO, "Cliente interno")
                )
            )

            // ---- Usuarios ----
            val adminId = usuarioDao.insertar(
                Usuario(
                    rolId = ROL_SUPERADMIN, nombreUsuario = "admin",
                    nombreCompleto = "Administrador JM FIT", cedula = "1000000001",
                    correo = "admin@jmfit.com", celular = "3000000001", passwordHash = hashAdmin
                )
            )

            val juanId = usuarioDao.insertar(
                Usuario(
                    rolId = ROL_ENTRENADOR, nombreUsuario = "entrenador",
                    nombreCompleto = "Juan Madroñero", cedula = "1000000002",
                    correo = "juan@jmfit.com", celular = "3000000002", passwordHash = hashEntrenador
                )
            )
            usuarioDao.insertarEntrenador(Entrenador(juanId, "Hipertrofia y fuerza"))

            val carolinaId = usuarioDao.insertar(
                Usuario(
                    rolId = ROL_ENTRENADOR, nombreUsuario = "entrenador2",
                    nombreCompleto = "Carolina Ortiz", cedula = "1000000003",
                    correo = "carolina@jmfit.com", celular = "3000000003", passwordHash = hashEntrenador
                )
            )
            usuarioDao.insertarEntrenador(Entrenador(carolinaId, "Acondicionamiento general"))

            suspend fun crearCliente(
                usuario: String, nombre: String, cedula: String, celular: String,
                nacimiento: String, sexo: String, estatura: Int, tipoRutina: String,
                entrenadorId: Long?, estado: String = EstadoUsuario.ACTIVO
            ): Long {
                val id = usuarioDao.insertar(
                    Usuario(
                        rolId = ROL_CLIENTE, nombreUsuario = usuario, nombreCompleto = nombre,
                        cedula = cedula, celular = celular, passwordHash = hashCliente, estado = estado
                    )
                )
                usuarioDao.insertarClienteInterno(
                    ClienteInterno(
                        usuarioId = id, fechaNacimiento = nacimiento, sexo = sexo,
                        estaturaCm = estatura, tipoRutina = tipoRutina
                    )
                )
                if (entrenadorId != null) {
                    usuarioDao.insertarAsignacionEntrenador(
                        AsignacionEntrenador(clienteId = id, entrenadorId = entrenadorId, asignadoPorId = adminId)
                    )
                }
                return id
            }

            val camilaId = crearCliente(
                "camila", "Camila Restrepo", "1000000010", "3000000010",
                "1995-04-12", "Femenino", 165, "PERSONALIZADA", juanId
            )
            val felipeId = crearCliente(
                "felipe", "Felipe Ramírez", "1000000011", "3000000011",
                "1990-09-03", "Masculino", 178, "PERSONALIZADA", juanId
            )
            crearCliente(
                "laura", "Laura Gómez", "1000000012", "3000000012",
                "1998-01-25", "Femenino", 160, "PERSONALIZADA", juanId
            )
            crearCliente(
                "diego", "Diego Torres", "1000000013", "3000000013",
                "1988-07-19", "Masculino", 182, "PERSONALIZADA", carolinaId
            )
            crearCliente(
                "inactivo", "Pedro Inactivo", "1000000014", "3000000014",
                "1992-11-30", "Masculino", 175, "PREESTABLECIDA", null, EstadoUsuario.INHABILITADO
            )

            // ---- Grupos musculares y ejercicios ----
            ejercicioDao.insertarGrupos(
                listOf(
                    GrupoMuscular(1, "Pecho"), GrupoMuscular(2, "Espalda"), GrupoMuscular(3, "Piernas"),
                    GrupoMuscular(4, "Hombros"), GrupoMuscular(5, "Brazos"), GrupoMuscular(6, "Core"),
                    GrupoMuscular(7, "Glúteos")
                )
            )

            val ejercicios = listOf(
                EjercicioSeed("Press de banca", 1, "Empuje horizontal con barra sobre banco plano."),
                EjercicioSeed("Press inclinado con mancuernas", 1, "Empuje inclinado para la parte alta del pecho."),
                EjercicioSeed("Aperturas con mancuernas", 1, "Aislamiento de pecho con codos ligeramente flexionados."),
                EjercicioSeed("Dominadas", 2, "Tracción vertical con el peso corporal."),
                EjercicioSeed("Remo con barra", 2, "Tracción horizontal con la espalda recta."),
                EjercicioSeed("Jalón al pecho", 2, "Tracción vertical en polea alta."),
                EjercicioSeed("Sentadilla", 3, "Flexión de cadera y rodilla con barra al frente o atrás."),
                EjercicioSeed("Prensa de piernas", 3, "Empuje de piernas en máquina."),
                EjercicioSeed("Peso muerto rumano", 3, "Bisagra de cadera con barra para femorales y glúteos."),
                EjercicioSeed("Press militar", 4, "Empuje vertical con barra o mancuernas."),
                EjercicioSeed("Elevaciones laterales", 4, "Aislamiento del deltoides medio."),
                EjercicioSeed("Curl de bíceps", 5, "Flexión de codo con mancuernas o barra."),
                EjercicioSeed("Extensión de tríceps en polea", 5, "Extensión de codo en polea alta."),
                EjercicioSeed("Plancha abdominal", 6, "Isométrico de core en posición de plancha."),
                EjercicioSeed("Hip thrust", 7, "Extensión de cadera con la espalda apoyada en un banco.")
            )
            val idPorNombre = HashMap<String, Long>()
            for (e in ejercicios) {
                idPorNombre[e.nombre] = ejercicioDao.insertar(
                    Ejercicio(
                        nombre = e.nombre, grupoMuscularId = e.grupoId,
                        descripcion = e.descripcion, creadoPorId = adminId
                    )
                )
            }

            // ---- Rutinas (cada ejercicio va en su propio bloque SIMPLE) ----
            suspend fun crearRutina(rutina: Rutina, dias: List<DiaSeed>): Long {
                val rutinaId = rutinaDao.insertar(rutina)
                for (dia in dias) {
                    val diaId = rutinaDao.insertarDia(RutinaDia(rutinaId = rutinaId, diaSemana = dia.dia))
                    dia.lineas.forEachIndexed { indice, linea ->
                        val bloqueId = rutinaDao.insertarBloque(
                            RutinaBloque(rutinaDiaId = diaId, orden = indice + 1)
                        )
                        rutinaDao.insertarEjercicio(
                            RutinaEjercicio(
                                rutinaBloqueId = bloqueId,
                                ejercicioId = idPorNombre.getValue(linea.ejercicio),
                                series = linea.series,
                                repeticiones = linea.repeticiones,
                                descansoSegundos = linea.descanso,
                                pesoKg = linea.pesoKg
                            )
                        )
                    }
                }
                return rutinaId
            }

            // Estándar (las crea el administrador; las ve el entrenador en la pestaña "Estándar")
            crearRutina(
                Rutina(
                    nombre = "Torso / Pierna", tipo = TipoRutina.ESTANDAR, objetivo = "Hipertrofia",
                    nivel = "Intermedio", notas = "4 días por semana. Aumentar carga cada semana.",
                    creadoPorId = adminId
                ),
                listOf(
                    DiaSeed(1, listOf(
                        LineaSeed("Press de banca", 4, "8-10", 40.0, 90),
                        LineaSeed("Remo con barra", 4, "8-10", 35.0, 90),
                        LineaSeed("Press militar", 3, "10-12", 20.0)
                    )),
                    DiaSeed(2, listOf(
                        LineaSeed("Sentadilla", 4, "8-10", 50.0, 120),
                        LineaSeed("Peso muerto rumano", 3, "10-12", 40.0, 90),
                        LineaSeed("Prensa de piernas", 3, "12")
                    ))
                )
            )
            crearRutina(
                Rutina(
                    nombre = "Full Body Principiante", tipo = TipoRutina.ESTANDAR,
                    objetivo = "Acondicionamiento general", nivel = "Principiante",
                    notas = "3 días no consecutivos por semana.", creadoPorId = adminId
                ),
                listOf(
                    DiaSeed(1, listOf(
                        LineaSeed("Sentadilla", 3, "12"),
                        LineaSeed("Press de banca", 3, "12"),
                        LineaSeed("Jalón al pecho", 3, "12"),
                        LineaSeed("Plancha abdominal", 3, "30 s")
                    ))
                )
            )

            // Personalizadas del entrenador Juan (las del diseño: Camila y Felipe)
            val hipertrofiaId = crearRutina(
                Rutina(
                    nombre = "Hipertrofia — Fase 2", tipo = TipoRutina.PERSONALIZADA, objetivo = "Hipertrofia",
                    nivel = "Intermedio", notas = "Subir peso cuando se completen todas las series.",
                    creadoPorId = juanId
                ),
                listOf(
                    DiaSeed(1, listOf(
                        LineaSeed("Press inclinado con mancuernas", 4, "10", 18.0),
                        LineaSeed("Aperturas con mancuernas", 3, "12", 10.0)
                    )),
                    DiaSeed(3, listOf(
                        LineaSeed("Jalón al pecho", 4, "10", 35.0),
                        LineaSeed("Curl de bíceps", 3, "12", 8.0)
                    ))
                )
            )
            rutinaDao.insertarAsignacion(
                AsignacionRutina(clienteId = camilaId, rutinaId = hipertrofiaId, asignadoPorId = juanId)
            )

            val recomposicionId = crearRutina(
                Rutina(
                    nombre = "Recomposición corporal", tipo = TipoRutina.PERSONALIZADA,
                    objetivo = "Pérdida de grasa", nivel = "Intermedio", creadoPorId = juanId
                ),
                listOf(
                    DiaSeed(2, listOf(
                        LineaSeed("Sentadilla", 4, "12", 40.0),
                        LineaSeed("Hip thrust", 4, "12", 50.0)
                    )),
                    DiaSeed(4, listOf(
                        LineaSeed("Press militar", 3, "12", 15.0),
                        LineaSeed("Plancha abdominal", 3, "45 s")
                    ))
                )
            )
            rutinaDao.insertarAsignacion(
                AsignacionRutina(clienteId = felipeId, rutinaId = recomposicionId, asignadoPorId = juanId)
            )
        }
    }
}
