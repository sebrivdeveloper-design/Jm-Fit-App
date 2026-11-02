package com.forcetech.jmfitapp.ui.trainer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetech.jmfitapp.R
import com.forcetech.jmfitapp.data.dao.RutinaResumen
import com.forcetech.jmfitapp.databinding.ItemRutinaBinding
import com.forcetech.jmfitapp.model.EstadoRutina
import com.forcetech.jmfitapp.model.TipoRutina

class RutinaAdapter(
    private val alVer: (RutinaResumen) -> Unit,
    private val alEditar: (RutinaResumen) -> Unit,
    private val alCambiarEstado: (RutinaResumen) -> Unit
) : ListAdapter<RutinaResumen, RutinaAdapter.Holder>(Comparador) {

    inner class Holder(private val b: ItemRutinaBinding) : RecyclerView.ViewHolder(b.root) {

        fun enlazar(r: RutinaResumen) {
            val contexto = b.root.context
            val activa = r.estado == EstadoRutina.ACTIVA
            val personalizada = r.tipo == TipoRutina.PERSONALIZADA
            val sinDato = contexto.getString(R.string.rutinas_sin_dato)

            b.tvNombre.text = r.nombre
            b.tvObjetivo.text = r.objetivo ?: sinDato
            b.tvNivel.text = r.nivel ?: sinDato
            b.tvEjercicios.text = r.totalEjercicios.toString()

            // Etiqueta de estado
            b.tvEstado.setText(
                if (activa) R.string.rutinas_estado_activa else R.string.rutinas_estado_inhabilitada
            )
            b.tvEstado.setBackgroundResource(
                if (activa) R.drawable.bg_chip_activa else R.drawable.bg_chip_inhabilitada
            )
            b.tvEstado.setTextColor(
                ContextCompat.getColor(contexto, if (activa) R.color.jmfit_primary else R.color.jmfit_text_secondary)
            )

            // "Asignada a" solo aplica a las personalizadas
            b.bloqueAsignada.visibility = if (personalizada) View.VISIBLE else View.INVISIBLE
            b.tvAsignada.text = r.clienteNombre ?: contexto.getString(R.string.rutinas_sin_asignar)

            // Las estándar son de solo lectura: únicamente se pueden ver
            // y solo se editan las personalizadas activas (una inhabilitada se reactiva primero)
            b.btnEditar.visibility = if (personalizada && activa) View.VISIBLE else View.GONE
            b.btnEstadoRutina.visibility = if (personalizada) View.VISIBLE else View.GONE
            b.btnEstadoRutina.contentDescription = contexto.getString(
                if (activa) R.string.rutinas_cd_inhabilitar else R.string.rutinas_cd_reactivar
            )
            ImageViewCompat.setImageTintList(
                b.btnEstadoRutina,
                ContextCompat.getColorStateList(
                    contexto, if (activa) R.color.jmfit_primary else R.color.jmfit_text_secondary
                )
            )

            b.btnVer.setOnClickListener { alVer(r) }
            b.btnEditar.setOnClickListener { alEditar(r) }
            b.btnEstadoRutina.setOnClickListener { alCambiarEstado(r) }
            b.root.setOnClickListener { alVer(r) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemRutinaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.enlazar(getItem(position))
    }

    private object Comparador : DiffUtil.ItemCallback<RutinaResumen>() {
        override fun areItemsTheSame(a: RutinaResumen, b: RutinaResumen) = a.id == b.id
        override fun areContentsTheSame(a: RutinaResumen, b: RutinaResumen) = a == b
    }
}
