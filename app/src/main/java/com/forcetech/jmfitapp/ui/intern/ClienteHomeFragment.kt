package com.forcetech.jmfitapp.ui.intern

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.forcetech.jmfitapp.R
import com.forcetech.jmfitapp.databinding.FragmentClienteHomeBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * HU Cliente — CA01: inicio del cliente interno.
 * Única acción: "Iniciar entrenamiento" abre Workout. El resto de la pantalla es solo visual.
 */
class ClienteHomeFragment : Fragment(R.layout.fragment_cliente_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentClienteHomeBinding.bind(view)
        val nav = findNavController()
        val datos = DatosClienteSimulados
        val entreno = datos.entrenamientoDeHoy

        val primerNombre = BarraCliente.nombreCliente(requireContext()).trim().substringBefore(' ')
        binding.tvSaludo.text = getString(R.string.cli_saludo, primerNombre)
        binding.tvFecha.text = SimpleDateFormat("EEEE, d MMM", Locale.forLanguageTag("es-CO")).format(Date())

        binding.tvNombreEntreno.text = entreno.nombreCompleto
        binding.tvDetalleEntreno.text =
            getString(R.string.cli_entreno_detalle, entreno.ejercicios.size, entreno.minutosEstimados)
        binding.btnIniciarEntreno.setOnClickListener {
            BarraCliente.irA(nav, R.id.internWorkoutFragment)
        }

        binding.tvStatEntrenos.text = datos.ENTRENOS_SEMANA
        binding.tvStatPeso.text = datos.CAMBIO_PESO
        binding.tvStatRacha.text = datos.RACHA

        listOf(binding.tvFoto1, binding.tvFoto2, binding.tvFoto3)
            .zip(datos.SEMANAS_FOTOS)
            .forEach { (vista, semana) -> vista.text = semana }

        binding.tvNombreEntrenador.text = datos.ENTRENADOR
        binding.tvMensajeEntrenador.text = datos.MENSAJE_ENTRENADOR
        binding.tvHaceMensaje.text = datos.HACE_MENSAJE

        BarraCliente.configurar(binding.barraCliente, nav, R.id.internHomeFragment)
    }
}
