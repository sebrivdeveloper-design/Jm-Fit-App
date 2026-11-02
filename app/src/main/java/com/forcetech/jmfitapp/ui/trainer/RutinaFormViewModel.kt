package com.forcetech.jmfitapp.ui.trainer

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.forcetech.jmfitapp.R
import com.forcetech.jmfitapp.data.dao.ClienteOpcion
import com.forcetech.jmfitapp.data.dao.EjercicioOpcion
import com.forcetech.jmfitapp.data.repository.DatosRutina
import com.forcetech.jmfitapp.data.repository.LineaRutina
import com.forcetech.jmfitapp.data.repository.ResultadoGuardado
import com.forcetech.jmfitapp.data.repository.RutinaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** Datos de texto del formulario, para precargarlos al editar. */
data class TextosIniciales(
    val nombre: String,
    val objetivo: String,
    val nivel: String,
    val notas: String
)

data class RutinaFormUiState(
    val cargando: Boolean = true,
    /** La rutina que se quiere editar no existe, no es del entrenador o no está activa. */
    val noDisponible: Boolean = false,
    val guardando: Boolean = false,
    val clientes: List<ClienteOpcion> = emptyList(),
    val ejercicios: List<EjercicioOpcion> = emptyList(),
    val cliente: ClienteOpcion? = null,
    /** En edición, si la rutina ya tiene cliente no se puede cambiar. */
    val clienteBloqueado: Boolean = false,
    val lineas: List<LineaRutina> = emptyList(),
    /** Posición (dentro de [lineas]) del ejercicio que se está modificando. */
    val editandoIndice: Int? = null
)

/** Eventos de una sola vez hacia la pantalla. */
sealed interface RutinaFormEvento {
    data class Precargar(val textos: TextosIniciales) : RutinaFormEvento
    data class CargarLinea(val linea: LineaRutina) : RutinaFormEvento
    data object LimpiarEditor : RutinaFormEvento
    data class Confirmar(
        val nombre: String,
        val cliente: ClienteOpcion?,
        val rutinaActivaDelCliente: String?
    ) : RutinaFormEvento
    data class Mensaje(@StringRes val texto: Int) : RutinaFormEvento
    data object NombreDuplicado : RutinaFormEvento
    data class Guardado(@StringRes val texto: Int) : RutinaFormEvento
}

/**
 * Lógica del formulario de rutina personalizada. Sirve para crear (rutinaId = 0) y para editar (rutinaId > 0).
 * La lista de ejercicios y el cliente elegido viven aquí para sobrevivir a una rotación de pantalla;
 * los textos los conservan las propias vistas.
 */
class RutinaFormViewModel(
    private val repo: RutinaRepository,
    private val entrenadorId: Long,
    private val rutinaId: Long
) : ViewModel() {

    val esEdicion: Boolean get() = rutinaId > 0

    private val _uiState = MutableStateFlow(RutinaFormUiState())
    val uiState: StateFlow<RutinaFormUiState> = _uiState.asStateFlow()

    private val _eventos = Channel<RutinaFormEvento>(Channel.BUFFERED)
    val eventos: Flow<RutinaFormEvento> = _eventos.receiveAsFlow()

    init {
        cargar()
    }

    fun cargar() {
        _uiState.value = _uiState.value.copy(cargando = true, noDisponible = false)
        viewModelScope.launch {
            try {
                val clientes = repo.clientesDelEntrenador(entrenadorId)
                val ejercicios = repo.ejerciciosActivos()

                if (!esEdicion) {
                    _uiState.value = _uiState.value.copy(
                        cargando = false, clientes = clientes, ejercicios = ejercicios
                    )
                    return@launch
                }

                val existente = repo.cargarParaEditar(rutinaId, entrenadorId)
                if (existente == null) {
                    _uiState.value = _uiState.value.copy(cargando = false, noDisponible = true)
                    return@launch
                }
                _uiState.value = _uiState.value.copy(
                    cargando = false,
                    clientes = clientes,
                    ejercicios = ejercicios,
                    cliente = existente.cliente,
                    clienteBloqueado = existente.cliente != null,
                    lineas = existente.lineas
                )
                _eventos.send(
                    RutinaFormEvento.Precargar(
                        TextosIniciales(
                            nombre = existente.rutina.nombre,
                            objetivo = existente.rutina.objetivo.orEmpty(),
                            nivel = existente.rutina.nivel.orEmpty(),
                            notas = existente.rutina.notas.orEmpty()
                        )
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(cargando = false)
                _eventos.send(RutinaFormEvento.Mensaje(R.string.form_error_cargar))
            }
        }
    }

    fun seleccionarCliente(cliente: ClienteOpcion?) {
        if (_uiState.value.clienteBloqueado) return
        _uiState.value = _uiState.value.copy(cliente = cliente)
    }

    // ---------- Ejercicios de la rutina ----------

    /** Agrega el ejercicio, o reemplaza el que se está modificando. */
    fun guardarLinea(linea: LineaRutina) {
        val estado = _uiState.value
        val editando = estado.editandoIndice
        val repetido = estado.lineas.withIndex().any { (i, l) ->
            i != editando && l.dia == linea.dia && l.ejercicioId == linea.ejercicioId
        }
        if (repetido) {
            enviar(RutinaFormEvento.Mensaje(R.string.form_ejercicio_repetido))
            return
        }
        val nuevas = estado.lineas.toMutableList()
        if (editando != null && editando in nuevas.indices) nuevas[editando] = linea else nuevas += linea
        _uiState.value = estado.copy(lineas = nuevas, editandoIndice = null)
        enviar(RutinaFormEvento.LimpiarEditor)
    }

    fun editarLinea(indice: Int) {
        val linea = _uiState.value.lineas.getOrNull(indice) ?: return
        _uiState.value = _uiState.value.copy(editandoIndice = indice)
        enviar(RutinaFormEvento.CargarLinea(linea))
    }

    fun cancelarEdicionLinea() {
        if (_uiState.value.editandoIndice == null) return
        _uiState.value = _uiState.value.copy(editandoIndice = null)
        enviar(RutinaFormEvento.LimpiarEditor)
    }

    fun quitarLinea(indice: Int) {
        val estado = _uiState.value
        if (indice !in estado.lineas.indices) return
        val nuevas = estado.lineas.toMutableList().also { it.removeAt(indice) }
        val editando = estado.editandoIndice
        when {
            editando == null -> _uiState.value = estado.copy(lineas = nuevas)
            editando == indice -> {
                _uiState.value = estado.copy(lineas = nuevas, editandoIndice = null)
                enviar(RutinaFormEvento.LimpiarEditor)
            }
            // Se quitó un ejercicio anterior al que se modifica: su posición baja una casilla
            editando > indice -> _uiState.value = estado.copy(lineas = nuevas, editandoIndice = editando - 1)
            else -> _uiState.value = estado.copy(lineas = nuevas)
        }
    }

    // ---------- Guardar ----------

    /** Paso 1: la pantalla ya validó los campos; aquí se prepara el diálogo de confirmación (Aceptar / Cancelar). */
    fun pedirConfirmacion(nombre: String) {
        val estado = _uiState.value
        viewModelScope.launch {
            val reemplazada = try {
                val asignaNuevo = !esEdicion || !estado.clienteBloqueado
                if (asignaNuevo && estado.cliente != null) repo.rutinaActivaDeCliente(estado.cliente.id) else null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            _eventos.send(RutinaFormEvento.Confirmar(nombre, estado.cliente, reemplazada))
        }
    }

    /** Paso 2: la persona pulsó Aceptar. */
    fun guardar(nombre: String, objetivo: String, nivel: String, notas: String) {
        val estado = _uiState.value
        if (estado.guardando) return
        _uiState.value = estado.copy(guardando = true)

        val datos = DatosRutina(
            nombre = nombre.trim(),
            objetivo = objetivo,
            nivel = nivel,
            notas = notas.trim().ifEmpty { null },
            clienteId = estado.cliente?.id,
            lineas = estado.lineas
        )

        viewModelScope.launch {
            val resultado = if (esEdicion) {
                repo.actualizar(rutinaId, entrenadorId, datos)
            } else {
                repo.crear(entrenadorId, datos)
            }
            _uiState.value = _uiState.value.copy(guardando = false)
            when (resultado) {
                ResultadoGuardado.Exito -> _eventos.send(
                    RutinaFormEvento.Guardado(
                        if (esEdicion) R.string.form_actualizada_ok else R.string.form_creada_ok
                    )
                )
                ResultadoGuardado.NombreDuplicado -> _eventos.send(RutinaFormEvento.NombreDuplicado)
                ResultadoGuardado.ClienteInvalido -> _eventos.send(RutinaFormEvento.Mensaje(R.string.form_cliente_invalido))
                ResultadoGuardado.NoDisponible -> _eventos.send(RutinaFormEvento.Mensaje(R.string.form_no_disponible))
                ResultadoGuardado.Error -> _eventos.send(RutinaFormEvento.Mensaje(R.string.form_error_guardar))
            }
        }
    }

    private fun enviar(evento: RutinaFormEvento) {
        _eventos.trySend(evento)
    }

    class Factory(
        private val repo: RutinaRepository,
        private val entrenadorId: Long,
        private val rutinaId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RutinaFormViewModel(repo, entrenadorId, rutinaId) as T
    }
}
