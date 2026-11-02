package com.forcetech.jmfitapp.ui.trainer

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.forcetech.jmfitapp.R
import com.forcetech.jmfitapp.data.dao.RutinaResumen
import com.forcetech.jmfitapp.data.repository.RutinaRepository
import com.forcetech.jmfitapp.model.EstadoRutina
import java.text.Normalizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PestanaRutinas { ESTANDAR, PERSONALIZADAS }

data class RutinasUiState(
    val pestana: PestanaRutinas = PestanaRutinas.PERSONALIZADAS,
    val busqueda: String = "",
    val cargando: Boolean = true,
    /** Rutinas ya filtradas por la búsqueda. */
    val rutinas: List<RutinaResumen> = emptyList(),
    /** Cuántas rutinas hay en la pestaña antes de filtrar (para distinguir "vacío" de "sin resultados"). */
    val totalSinFiltrar: Int = 0,
    val error: Boolean = false
)

class RutinasViewModel(
    private val repo: RutinaRepository,
    private val entrenadorId: Long
) : ViewModel() {

    private val pestana = MutableStateFlow(PestanaRutinas.PERSONALIZADAS)
    private val busqueda = MutableStateFlow("")

    /** Mensajes de una sola vez (confirmaciones y errores). */
    private val _mensajes = Channel<Int>(Channel.BUFFERED)
    val mensajes: Flow<Int> = _mensajes.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val listaDeLaPestana: Flow<Pair<PestanaRutinas, List<RutinaResumen>>> =
        pestana.flatMapLatest { actual ->
            val fuente = when (actual) {
                PestanaRutinas.ESTANDAR -> repo.observarEstandar()
                PestanaRutinas.PERSONALIZADAS -> repo.observarPersonalizadas(entrenadorId)
            }
            fuente.map { actual to it }
        }

    val uiState: StateFlow<RutinasUiState> = combine(listaDeLaPestana, busqueda) { (actual, lista), texto ->
        RutinasUiState(
            pestana = actual,
            busqueda = texto,
            cargando = false,
            rutinas = filtrar(lista, texto),
            totalSinFiltrar = lista.size
        )
    }
        .catch { emit(RutinasUiState(cargando = false, error = true)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RutinasUiState())

    fun seleccionarPestana(nueva: PestanaRutinas) {
        pestana.value = nueva
    }

    fun buscar(texto: String) {
        busqueda.value = texto
    }

    /** ENT-7: inhabilita una rutina activa o reactiva una inhabilitada. */
    fun cambiarEstado(rutina: RutinaResumen) {
        val activar = rutina.estado != EstadoRutina.ACTIVA
        viewModelScope.launch {
            val ok = try {
                repo.cambiarEstado(rutina.id, entrenadorId, activar)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
            enviarMensaje(
                when {
                    !ok -> R.string.rutinas_error_estado
                    activar -> R.string.rutinas_reactivada_ok
                    else -> R.string.rutinas_inhabilitada_ok
                }
            )
        }
    }

    private suspend fun enviarMensaje(@StringRes mensaje: Int) {
        _mensajes.send(mensaje)
    }

    /** Busca por nombre, objetivo, nivel o cliente, sin distinguir mayúsculas ni tildes. */
    private fun filtrar(lista: List<RutinaResumen>, texto: String): List<RutinaResumen> {
        val buscado = normalizar(texto.trim())
        if (buscado.isEmpty()) return lista
        return lista.filter { r ->
            listOf(r.nombre, r.objetivo, r.nivel, r.clienteNombre)
                .any { campo -> campo != null && normalizar(campo).contains(buscado) }
        }
    }

    private fun normalizar(texto: String): String =
        Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()

    class Factory(
        private val repo: RutinaRepository,
        private val entrenadorId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RutinasViewModel(repo, entrenadorId) as T
    }
}
