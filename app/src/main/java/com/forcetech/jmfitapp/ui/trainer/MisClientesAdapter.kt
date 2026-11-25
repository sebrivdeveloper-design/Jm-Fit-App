package com.forcetech.jmfitapp.ui.trainer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetech.jmfitapp.R
import com.forcetech.jmfitapp.data.dao.ClienteDeEntrenadorFila
import com.forcetech.jmfitapp.databinding.ItemMiClienteBinding
import com.forcetech.jmfitapp.model.EstadoUsuario

/** Lista de "Mis clientes" (HU-51). Solo lectura: la ficha del cliente (HU-58) aún no existe. */
class MisClientesAdapter : ListAdapter<ClienteDeEntrenadorFila, MisClientesAdapter.Holder>(Comparador) {

    class Holder(private val b: ItemMiClienteBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(c: ClienteDeEntrenadorFila) {
            val contexto = b.root.context
            val activo = c.estado == EstadoUsuario.ACTIVO
            b.tvNombre.text = c.nombre
            b.tvUsuario.text = contexto.getString(R.string.mcli_usuario, c.nombreUsuario)
            b.tvRutina.text = c.rutinaActiva ?: contexto.getString(R.string.mcli_sin_rutina)
            b.tvEstado.setText(if (activo) R.string.mcli_estado_activo else R.string.mcli_estado_inhabilitado)
            b.tvEstado.setBackgroundResource(if (activo) R.drawable.bg_chip_activa else R.drawable.bg_chip_inhabilitada)
            b.tvEstado.setTextColor(
                ContextCompat.getColor(contexto, if (activo) R.color.jmfit_primary else R.color.jmfit_text_secondary)
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemMiClienteBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    private object Comparador : DiffUtil.ItemCallback<ClienteDeEntrenadorFila>() {
        override fun areItemsTheSame(a: ClienteDeEntrenadorFila, b: ClienteDeEntrenadorFila) = a.id == b.id
        override fun areContentsTheSame(a: ClienteDeEntrenadorFila, b: ClienteDeEntrenadorFila) = a == b
    }
}
