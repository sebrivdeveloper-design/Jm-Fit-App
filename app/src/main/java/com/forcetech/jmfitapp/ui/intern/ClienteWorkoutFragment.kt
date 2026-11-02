package com.forcetech.jmfitapp.ui.intern

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.forcetech.jmfitapp.R
import com.forcetech.jmfitapp.databinding.FragmentClienteWorkoutBinding
import com.forcetech.jmfitapp.databinding.ItemClienteEjercicioBinding

/**
 * HU Cliente — CA02: detalle del entrenamiento del día.
 * Acciones: regresar a Home y la barra inferior. Las casillas son solo visuales
 * (no registran series, pesos ni progreso).
 */
class ClienteWorkoutFragment : Fragment(R.layout.fragment_cliente_workout) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentClienteWorkoutBinding.bind(view)
        val nav = findNavController()
        val entreno = DatosClienteSimulados.entrenamientoDeHoy

        binding.btnVolver.setOnClickListener {
            BarraCliente.irA(nav, R.id.internHomeFragment)
        }

        binding.tvDiaPrograma.text = getString(R.string.cli_dia_programa, entreno.dia, entreno.diasTotales)
        binding.tvNombreEntreno.text = entreno.nombreCorto
        binding.tvGrupoSemana.text = getString(R.string.cli_grupo_semana, entreno.grupoMuscular, entreno.semana)

        binding.contenedorEjercicios.removeAllViews()
        entreno.ejercicios.forEach { ejercicio ->
            val item = ItemClienteEjercicioBinding.inflate(layoutInflater, binding.contenedorEjercicios, true)
            item.tvNombre.text = ejercicio.nombre
            item.tvSeries.text = ejercicio.series.toString()
            item.tvReps.text = ejercicio.repeticiones
            item.tvPeso.text = ejercicio.peso
            item.tvDescanso.text = ejercicio.descanso
            item.tvIndicacion.text = ejercicio.indicacion
        }

        BarraCliente.configurar(binding.barraCliente, nav, R.id.internWorkoutFragment)
    }
}
