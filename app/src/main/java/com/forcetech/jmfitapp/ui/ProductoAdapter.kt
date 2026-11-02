package com.forcetech.jmfitapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.forcetech.jmfitapp.R
import com.forcetech.jmfitapp.model.Producto
import java.text.NumberFormat
import java.util.Locale

class ProductoAdapter(
    private val onVer: (Producto) -> Unit,
    private val onEditar: (Producto) -> Unit,
    private val onCambiarEstado: (Producto) -> Unit
) : RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder>() {

    private val listaProductos = mutableListOf<Producto>()

    fun actualizarLista(nuevaLista: List<Producto>) {
        listaProductos.clear()
        listaProductos.addAll(nuevaLista)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ProductoViewHolder {

        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto, parent, false)

        return ProductoViewHolder(vista)
    }

    override fun onBindViewHolder(
        holder: ProductoViewHolder,
        position: Int
    ) {
        holder.bind(listaProductos[position])
    }

    override fun getItemCount(): Int = listaProductos.size

    inner class ProductoViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        private val tvNombre: TextView =
            itemView.findViewById(R.id.tvNombreProducto)

        private val tvEstado: TextView =
            itemView.findViewById(R.id.tvEstadoProducto)

        private val tvObjetivo: TextView =
            itemView.findViewById(R.id.tvObjetivo)

        private val tvMarca: TextView =
            itemView.findViewById(R.id.tvMarca)

        private val tvPresentacion: TextView =
            itemView.findViewById(R.id.tvPresentacion)

        private val tvPrecio: TextView =
            itemView.findViewById(R.id.tvPrecio)

        private val btnVer: ImageButton =
            itemView.findViewById(R.id.btnVerProducto)

        private val btnEditar: ImageButton =
            itemView.findViewById(R.id.btnEditarProducto)

        private val btnEstado: ImageButton =
            itemView.findViewById(R.id.btnEstadoProducto)

        fun bind(producto: Producto) {

            tvNombre.text = producto.nombre
            tvMarca.text = producto.marca
            tvObjetivo.text = producto.objetivo
            tvPresentacion.text = producto.presentacion

            val formatoPrecio = NumberFormat.getCurrencyInstance(
                Locale("es", "CO")
            )

            tvPrecio.text = formatoPrecio.format(producto.precio)

            if (producto.activo) {
                tvEstado.text = itemView.context.getString(
                    R.string.estado_activo
                )

                tvEstado.setBackgroundResource(
                    R.drawable.bg_estado_activo
                )

                tvEstado.setTextColor(
                    itemView.context.getColor(R.color.verde)
                )

                btnEstado.setImageResource(R.drawable.ic_power)

                btnEstado.contentDescription =
                    itemView.context.getString(R.string.desactivar_producto)

            } else {
                tvEstado.text = itemView.context.getString(
                    R.string.estado_inactivo
                )

                tvEstado.setBackgroundResource(
                    R.drawable.bg_estado_inactivo
                )

                tvEstado.setTextColor(
                    itemView.context.getColor(R.color.gris)
                )

                btnEstado.setImageResource(R.drawable.ic_power_off)

                btnEstado.contentDescription =
                    itemView.context.getString(R.string.activar_producto)
            }

            btnVer.setOnClickListener {
                onVer(producto)
            }

            btnEditar.setOnClickListener {
                onEditar(producto)
            }

            btnEstado.setOnClickListener {
                onCambiarEstado(producto)
            }
        }
    }
}
