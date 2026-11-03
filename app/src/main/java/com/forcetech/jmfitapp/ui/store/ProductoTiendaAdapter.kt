package com.forcetech.jmfitapp.ui.store

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetech.jmfitapp.R
import com.forcetech.jmfitapp.databinding.ItemProductoTiendaBinding
import com.forcetech.jmfitapp.model.ProductoResumen

/**
 * Tarjetas del catálogo de la tienda (cuadrícula de 2 columnas).
 * Ojo: NO es ui/ProductoAdapter.kt, ese es la lista del administrador.
 */
class ProductoTiendaAdapter(
    private val onProductoClick: (ProductoResumen) -> Unit,
    private val onAgregarClick: (ProductoResumen) -> Unit
) : ListAdapter<ProductoResumen, ProductoTiendaAdapter.ProductoViewHolder>(Diferencias) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val binding = ItemProductoTiendaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        holder.enlazar(getItem(position))
    }

    inner class ProductoViewHolder(private val binding: ItemProductoTiendaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun enlazar(producto: ProductoResumen) {
            val contexto = binding.root.context

            binding.ivProducto.setImageResource(TiendaUi.recursoImagen(contexto, producto.imagen))

            val categoria = producto.categoriaNombre
            binding.tvCategoria.isVisible = !categoria.isNullOrBlank()
            binding.tvCategoria.text = categoria.orEmpty().uppercase(TiendaUi.LOCALE_CO)

            binding.tvNombre.text = producto.nombre
            binding.tvPresentacion.text = producto.presentacion
            binding.tvPrecio.text = Moneda.pesos(producto.precioPesos)

            val conCalificacion = producto.calificacion > 0
            binding.tvCalificacion.isVisible = conCalificacion
            binding.tvCalificacion.text =
                contexto.getString(R.string.tienda_calificacion, TiendaUi.calificacion(producto.calificacion))

            binding.btnAgregar.isEnabled = producto.disponible
            binding.btnAgregar.setText(if (producto.disponible) R.string.tienda_agregar else R.string.tienda_agotado)

            binding.root.setOnClickListener { onProductoClick(producto) }
            binding.btnAgregar.setOnClickListener { onAgregarClick(producto) }
        }
    }

    private object Diferencias : DiffUtil.ItemCallback<ProductoResumen>() {
        override fun areItemsTheSame(a: ProductoResumen, b: ProductoResumen) = a.id == b.id
        override fun areContentsTheSame(a: ProductoResumen, b: ProductoResumen) = a == b
    }
}
