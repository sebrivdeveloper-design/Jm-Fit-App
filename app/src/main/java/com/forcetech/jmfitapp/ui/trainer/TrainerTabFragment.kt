package com.forcetech.jmfitapp.ui.trainer

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.forcetech.jmfitapp.R
import com.forcetech.jmfitapp.databinding.FragmentTrainerTabBinding

/**
 * Pantalla provisional de las pestañas de la barra inferior que no pertenecen a las HU de esta entrega
 * (Inicio, Mis clientes, Catálogo). El título sale del `android:label` del destino.
 */
class TrainerTabFragment : Fragment(R.layout.fragment_trainer_tab) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentTrainerTabBinding.bind(view)
        val nav = findNavController()
        binding.tvTituloTab.text = nav.currentDestination?.label.toString().orEmpty()
        BarraEntrenador.configurar(binding.barraEntrenador, nav, nav.currentDestination?.id ?: R.id.trainerRutinasFragment)
    }
}
