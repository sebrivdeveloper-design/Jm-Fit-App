package com.forcetech.jmfitapp.ui.trainer

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.forcetech.jmfitapp.data.dao.ClienteDeEntrenadorFila
import com.forcetech.jmfitapp.data.repository.ClienteRepository
import com.forcetech.jmfitapp.model.EstadoUsuario
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Normalizer

data class MisClientesUiState(
    val cargando: Boolean = true,
    val error: Boolean = false,
    val todos: List<ClienteDeEntrenadorFila> = emptyList(),
    val texto: String = ""
) {
    /** HU-57: solo cuentas habilitadas, sin importar la vigencia del plan. */
    val totalActivos: Int get() = todos.count { it.estado == EstadoUsuario.ACTIVO }

    /** HU-51: búsqueda por nombre o parte del nombre (sin distinguir mayúsculas ni tildes). */
    val visibles: List<ClienteDeEntrenadorFila>
        get() {
            val buscado = normalizar(texto)
            return if (buscado.isEmpty()) todos else todos.filter { normalizar(it.nombre).contains(buscado) }
        }

    private companion object {
        val MARCAS = "\\p{Mn}+".toRegex()
        fun normalizar(s: String): String =
            Normalizer.normalize(s.trim().lowercase(), Normalizer.Form.NFD).replace(MARCAS, "")
    }
}

/** HU-51 y HU-57: clientes con asignación vigente al entrenador con sesión. */
class MisClientesViewModel(
    private val repo: ClienteRepository,
    private val entrenadorId: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(MisClientesUiState())
    val uiState: StateFlow<MisClientesUiState> = _uiState.asStateFlow()

    private var carga: Job? = null

    /** Se llama al mostrar la pantalla: así el total refleja asignaciones y cambios de estado recientes. */
    fun cargar() {
        carga?.cancel()
        _uiState.update { it.copy(cargando = it.todos.isEmpty(), error = false) }
        carga = viewModelScope.launch {
            try {
                val lista = entrenadorId?.let { repo.clientesDeEntrenador(it) }.orEmpty()
                _uiState.update { it.copy(cargando = false, error = false, todos = lista) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("MisClientesVM", "No se pudieron cargar los clientes", e)
                _uiState.update { it.copy(cargando = false, error = true) }
            }
        }
    }

    fun buscar(texto: String) {
        if (texto != _uiState.value.texto) _uiState.update { it.copy(texto = texto) }
    }

    class Factory(
        private val repo: ClienteRepository,
        private val entrenadorId: Long?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MisClientesViewModel(repo, entrenadorId) as T
    }
}
