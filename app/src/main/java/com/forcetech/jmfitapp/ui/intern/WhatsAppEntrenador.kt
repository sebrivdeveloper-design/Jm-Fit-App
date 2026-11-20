package com.forcetech.jmfitapp.ui.intern

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.forcetech.jmfitapp.JmFitApplication
import com.forcetech.jmfitapp.R
import com.forcetech.jmfitapp.data.dao.EntrenadorContactoFila
import com.forcetech.jmfitapp.data.repository.ClienteRepository
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * HU-36: abre WhatsApp con el entrenador asignado al cliente con sesión.
 * Se usa desde la pestaña "Chat" de la barra inferior y desde la tarjeta del entrenador en HOME.
 */
object WhatsAppEntrenador {

    /** Consulta el entrenador vigente y abre el chat. [ancla] es la barra inferior (el aviso se muestra encima). */
    fun abrir(fragment: Fragment, ancla: View) {
        val container = (fragment.requireActivity().application as JmFitApplication).container
        val clienteId = container.sessionManager.sesionActual()?.usuarioId
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            val entrenador = try {
                clienteId?.let { container.clienteRepository.entrenadorAsignado(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("WhatsAppEntrenador", "No se pudo consultar el entrenador", e)
                avisar(fragment, ancla, R.string.cli_whatsapp_no_abre)
                return@launch
            }
            abrir(fragment, ancla, entrenador)
        }
    }

    /** Abre el chat con un entrenador ya cargado (o avisa si no hay contacto). */
    fun abrir(fragment: Fragment, ancla: View, entrenador: EntrenadorContactoFila?) {
        val numero = ClienteRepository.numeroWhatsApp(entrenador?.celular)
        if (numero == null) {
            avisar(fragment, ancla, R.string.cli_whatsapp_sin_contacto)
            return
        }
        // wa.me abre la app de WhatsApp si está instalada; si no, el navegador muestra la opción de descargarla.
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$numero"))
        try {
            fragment.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            avisar(fragment, ancla, R.string.cli_whatsapp_no_abre)
        }
    }

    private fun avisar(fragment: Fragment, ancla: View, mensaje: Int) {
        if (fragment.view == null) return
        Snackbar.make(fragment.requireView(), mensaje, Snackbar.LENGTH_LONG)
            .setAnchorView(ancla)
            .show()
    }
}
