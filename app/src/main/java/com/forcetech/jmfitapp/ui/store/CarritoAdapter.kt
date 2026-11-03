package com.forcetech.jmfitapp.ui.store

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetech.jmfitapp.R
import com.forcetech.jmfitapp.databinding.ItemCarritoBinding
import com.forcetech.jmfitapp.model.CarritoItem
import com.forcetech.jmfitapp.model.ReglasCarrito

class CarritoAdapter(
    private val alSumar: (CarritoItem) -> Unit,
    private val alRestar: (CarritoItem) -> Unit,
    private val alEliminar: (CarritoItem) -> Unit
) : ListAdapter<CarritoItem, CarritoAdapter.Holder>(Comparador) {

    inner class Holder(private val b: ItemCarritoBinding) : RecyclerView.ViewHolder(b.root) {

        fun enlazar(item: CarritoItem) {
            val contexto = b.root.context

            b.tvNombre.text = item.nombre
            b.tvPresentacion.text = item.presentacion
            // El precio grande es el de la línea (unidad x cantidad); el de una unidad va abajo a la izquierda.
            b.tvPrecio.text = Moneda.pesos(item.precioUnitario * item.cantidad)
            b.tvDetalleUnidad.text =
                if (item.cantidad == 1) contexto.getString(R.string.carrito_unidad_una)
                else contexto.getString(R.string.carrito_por_unidad, Moneda.pesos(item.precioUnitario))
            b.tvCantidad.text = item.cantidad.toString()

            // Restar no baja de 1 (para quitar el producto está el tacho) y sumar se detiene en el máximo
            val puedeRestar = item.cantidad > 1
            val puedeSumar = item.cantidad < ReglasCarrito.CANTIDAD_MAXIMA
            b.btnRestar.isEnabled = puedeRestar
            b.btnRestar.alpha = if (puedeRestar) 1f else 0.35f
            b.btnSumar.isEnabled = puedeSumar
            b.btnSumar.alpha = if (puedeSumar) 1f else 0.35f

            b.btnEliminar.contentDescription = contexto.getString(R.string.carrito_cd_eliminar, item.nombre)

            // Imagen: nombre de un drawable guardado en el producto; si no existe se queda el marcador.
            val idImagen = item.imagen
                ?.let { contexto.resources.getIdentifier(it, "drawable", contexto.packageName) }
                ?: 0
            b.ivProducto.setImageResource(if (idImagen != 0) idImagen else R.drawable.ic_producto_placeholder)

            b.btnSumar.setOnClickListener { alSumar(item) }
            b.btnRestar.setOnClickListener { alRestar(item) }
            b.btnEliminar.setOnClickListener { alEliminar(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemCarritoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.enlazar(getItem(position))

    private object Comparador : DiffUtil.ItemCallback<CarritoItem>() {
        override fun areItemsTheSame(a: CarritoItem, b: CarritoItem) = a.productoId == b.productoId
        override fun areContentsTheSame(a: CarritoItem, b: CarritoItem) = a == b
    }
}
