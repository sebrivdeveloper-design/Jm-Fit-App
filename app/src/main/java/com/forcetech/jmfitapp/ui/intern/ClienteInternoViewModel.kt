package com.forcetech.jmfitapp.ui.intern

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.forcetech.jmfitapp.data.dao.EntrenadorContactoFila
import com.forcetech.jmfitapp.data.dao.PerfilClienteFila
import com.forcetech.jmfitapp.data.repository.ClienteRepository
import com.forcetech.jmfitapp.data.repository.RutinaCliente
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Datos del cliente con sesión. [rutina] y [entrenador] son null cuando no tiene asignación. */
data class DatosCliente(
    val perfil: PerfilClienteFila?,
    val rutina: RutinaCliente?,
    val entrenador: EntrenadorContactoFila?
)

sealed interface EstadoCliente {
    data object Cargando : EstadoCliente
    data object Error : EstadoCliente
    data class Listo(val datos: DatosCliente) : EstadoCliente
}

/**
 * Carga los datos reales del cliente interno que inició sesión. Lo usan HOME, WORKOUT y MY PROFILE
 * (cada pantalla tiene su propia instancia; la consulta es local y rápida).
 * Cada pantalla llama [cargar] al crear su vista, así al volver se ve la rutina vigente (HU-27B, c.a. 5).
 */
class ClienteInternoViewModel(
    private val repo: ClienteRepository,
    private val clienteId: Long?
) : ViewModel() {

    private val _estado = MutableStateFlow<EstadoCliente>(EstadoCliente.Cargando)
    val estado: StateFlow<EstadoCliente> = _estado.asStateFlow()

    private var carga: Job? = null

    fun cargar() {
        carga?.cancel()
        // Si ya hay datos se conservan en pantalla mientras se actualizan (sin parpadeo de "Cargando…").
        if (_estado.value !is EstadoCliente.Listo) _estado.value = EstadoCliente.Cargando
        carga = viewModelScope.launch {
            _estado.value = try {
                val id = clienteId
                if (id == null) {
                    EstadoCliente.Listo(DatosCliente(null, null, null))
                } else {
                    EstadoCliente.Listo(
                        DatosCliente(
                            perfil = repo.perfil(id),
                            rutina = repo.rutinaAsignada(id),
                            entrenador = repo.entrenadorAsignado(id)
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ClienteInternoVM", "No se pudieron cargar los datos del cliente", e)
                EstadoCliente.Error
            }
        }
    }

    class Factory(
        private val repo: ClienteRepository,
        private val clienteId: Long?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ClienteInternoViewModel(repo, clienteId) as T
    }
}
