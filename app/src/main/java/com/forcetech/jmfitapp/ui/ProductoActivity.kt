package com.forcetech.jmfitapp.ui

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.forcetech.jmfitapp.R
import com.forcetech.jmfitapp.data.database.AppDatabase
import com.forcetech.jmfitapp.model.Producto
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ProductoActivity : AppCompatActivity() {

    private lateinit var productoDao: com.forcetech.jmfitapp.data.dao.ProductoDao

    private lateinit var adapter: ProductoAdapter

    private lateinit var rvProductos: RecyclerView
    private lateinit var tvSinProductos: TextView
    private lateinit var etBuscarProducto: EditText

    private lateinit var btnTodos: Button
    private lateinit var btnActivos: Button
    private lateinit var btnInactivos: Button

    // 0 = Todos, 1 = Activos, 2 = Inactivos
    private var estadoSeleccionado = 0

    private var busquedaActual = ""

    private var busquedaJob: Job? = null
    private var consultaJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_producto)

        configurarInsets()
        inicializarBaseDatos()
        inicializarVistas()
        configurarRecyclerView()
        configurarFiltros()
        configurarBuscador()
        configurarBotones()

        cargarProductos()
    }

    private fun configurarInsets() {

        val raiz = findViewById<View>(R.id.main)

        ViewCompat.setOnApplyWindowInsetsListener(raiz) { vista, insets ->

            val barras = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            vista.setPadding(
                barras.left,
                barras.top,
                barras.right,
                barras.bottom
            )

            insets
        }
    }

    private fun inicializarBaseDatos() {

        productoDao = AppDatabase.obtener(
            applicationContext
        ).productoDao()
    }

    private fun inicializarVistas() {

        rvProductos = findViewById(R.id.rvProductos)
        tvSinProductos = findViewById(R.id.tvSinProductos)

        etBuscarProducto = findViewById(
            R.id.etBuscarProducto
        )

        btnTodos = findViewById(R.id.btnTodos)
        btnActivos = findViewById(R.id.btnActivos)
        btnInactivos = findViewById(R.id.btnInactivos)
    }

    private fun configurarRecyclerView() {

        adapter = ProductoAdapter(

            onVer = { producto ->
                visualizarProducto(producto)
            },

            onEditar = { producto ->
                mostrarFormulario(producto)
            },

            onCambiarEstado = { producto ->
                confirmarCambioEstado(producto)
            }
        )

        rvProductos.layoutManager = LinearLayoutManager(this)

        rvProductos.adapter = adapter
    }

    private fun configurarFiltros() {

        btnTodos.setOnClickListener {
            estadoSeleccionado = 0
            actualizarFiltros()
            cargarProductos()
        }

        btnActivos.setOnClickListener {
            estadoSeleccionado = 1
            actualizarFiltros()
            cargarProductos()
        }

        btnInactivos.setOnClickListener {
            estadoSeleccionado = 2
            actualizarFiltros()
            cargarProductos()
        }

        actualizarFiltros()
    }

    private fun actualizarFiltros() {

        val botones = listOf(
            btnTodos to 0,
            btnActivos to 1,
            btnInactivos to 2
        )

        botones.forEach { (boton, estado) ->

            val seleccionado = estadoSeleccionado == estado

            if (seleccionado) {

                boton.setBackgroundResource(
                    R.drawable.bg_filtro_seleccionado
                )

                boton.setTextColor(
                    getColor(R.color.blanco)
                )

            } else {

                boton.setBackgroundResource(
                    R.drawable.bg_filtro_normal
                )

                boton.setTextColor(
                    getColor(R.color.texto)
                )
            }
        }
    }

    private fun configurarBuscador() {

        etBuscarProducto.addTextChangedListener(
            object : android.text.TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {}

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    busquedaActual = s?.toString()?.trim() ?: ""

                    busquedaJob?.cancel()

                    busquedaJob = lifecycleScope.launch {

                        delay(250)

                        cargarProductos()
                    }
                }

                override fun afterTextChanged(
                    s: android.text.Editable?
                ) {}
            }
        )
    }

    private fun configurarBotones() {

        findViewById<Button>(
            R.id.btnAgregarProducto
        ).setOnClickListener {

            mostrarFormulario()
        }
    }

    private fun cargarProductos() {

        consultaJob?.cancel()

        consultaJob = lifecycleScope.launch {

            try {

                val productos = productoDao.obtenerProductosFiltrados(
                    busqueda = busquedaActual,
                    estado = estadoSeleccionado
                )

                adapter.actualizarLista(productos)

                if (productos.isEmpty()) {

                    tvSinProductos.visibility = View.VISIBLE

                    rvProductos.visibility = View.GONE

                } else {

                    tvSinProductos.visibility = View.GONE

                    rvProductos.visibility = View.VISIBLE
                }

            } catch (e: Exception) {

                Toast.makeText(
                    this@ProductoActivity,
                    getString(R.string.error_consultar),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // REGISTRAR Y EDITAR

    private fun mostrarFormulario(producto: Producto? = null) {

        val vista = layoutInflater.inflate(
            R.layout.dialog_crear_producto,
            null
        )

        val tvTitulo = vista.findViewById<TextView>(
            R.id.tvTituloModal
        )

        val tvDescripcion = vista.findViewById<TextView>(
            R.id.tvDescripcionModal
        )

        val etNombre = vista.findViewById<EditText>(
            R.id.etNombreProducto
        )

        val etMarca = vista.findViewById<EditText>(
            R.id.etMarca
        )

        val etPrecio = vista.findViewById<EditText>(
            R.id.etPrecio
        )

        val etObjetivo = vista.findViewById<EditText>(
            R.id.etObjetivo
        )

        val etPresentacion = vista.findViewById<EditText>(
            R.id.etPresentacion
        )

        val switchActivo = vista.findViewById<Switch>(
            R.id.switchActivo
        )

        val btnGuardar = vista.findViewById<Button>(
            R.id.btnGuardarProducto
        )

        val btnCancelar = vista.findViewById<Button>(
            R.id.btnCancelarProducto
        )

        val btnCerrar = vista.findViewById<ImageButton>(
            R.id.btnCerrarModal
        )

        val editando = producto != null

        if (editando) {

            tvTitulo.text = getString(R.string.modal_editar)

            tvDescripcion.text = getString(
                R.string.modal_editar_descripcion
            )

            btnGuardar.text = getString(
                R.string.guardar_cambios
            )

            etNombre.setText(producto!!.nombre)
            etMarca.setText(producto.marca)
            etPrecio.setText(producto.precio.toPlainString())
            etObjetivo.setText(producto.objetivo)
            etPresentacion.setText(producto.presentacion)

            switchActivo.isChecked = producto.activo

        } else {

            tvTitulo.text = getString(R.string.modal_titulo)

            tvDescripcion.text = getString(
                R.string.modal_descripcion
            )

            btnGuardar.text = getString(
                R.string.crear_producto
            )

            switchActivo.isChecked = true
        }

        val dialogo = AlertDialog.Builder(this)
            .setView(vista)
            .create()

        dialogo.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        dialogo.setOnShowListener {

            dialogo.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.92).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }

        btnCerrar.setOnClickListener {
            dialogo.dismiss()
        }

        btnCancelar.setOnClickListener {
            dialogo.dismiss()
        }

        btnGuardar.setOnClickListener {

            val nombre = etNombre.text.toString().trim()
            val marca = etMarca.text.toString().trim()
            val precioTexto = etPrecio.text.toString().trim()
            val objetivo = etObjetivo.text.toString().trim()
            val presentacion = etPresentacion.text.toString().trim()

            // Validar campos obligatorios

            if (
                nombre.isEmpty() ||
                marca.isEmpty() ||
                precioTexto.isEmpty() ||
                objetivo.isEmpty() ||
                presentacion.isEmpty()
            ) {

                Toast.makeText(
                    this,
                    R.string.error_campos_obligatorios,
                    Toast.LENGTH_SHORT
                ).show()

                if (nombre.isEmpty()) etNombre.error = getString(R.string.error_campos_obligatorios)
                if (marca.isEmpty()) etMarca.error = getString(R.string.error_campos_obligatorios)
                if (objetivo.isEmpty()) etObjetivo.error = getString(R.string.error_campos_obligatorios)
                if (presentacion.isEmpty()) etPresentacion.error = getString(R.string.error_campos_obligatorios)

                return@setOnClickListener
            }

            // Validar precio

            val precio = try {

                BigDecimal(
                    precioTexto.replace(",", ".")
                )

            } catch (e: NumberFormatException) {

                null
            }

            if (precio == null || precio <= BigDecimal.ZERO) {

                etPrecio.error = getString(
                    R.string.error_precio
                )

                return@setOnClickListener
            }

            val nuevoProducto = Producto(

                id = producto?.id ?: 0,

                nombre = nombre,

                marca = marca,

                precio = precio,

                objetivo = objetivo,

                presentacion = presentacion,

                activo = switchActivo.isChecked
            )

            lifecycleScope.launch {

                try {

                    if (editando) {

                        val filasActualizadas =
                            productoDao.updateProductos(nuevoProducto)

                        if (filasActualizadas > 0) {

                            Toast.makeText(
                                this@ProductoActivity,
                                R.string.producto_actualizado,
                                Toast.LENGTH_SHORT
                            ).show()

                        } else {

                            Toast.makeText(
                                this@ProductoActivity,
                                R.string.error_actualizar,
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    } else {

                        productoDao.guardarProductos(nuevoProducto)

                        Toast.makeText(
                            this@ProductoActivity,
                            R.string.producto_creado,
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    dialogo.dismiss()

                    cargarProductos()

                } catch (e: Exception) {

                    Toast.makeText(
                        this@ProductoActivity,
                        if (editando) R.string.error_actualizar
                        else R.string.error_guardar,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        dialogo.show()

        dialogo.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        dialogo.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    // VISUALIZAR PRODUCTO

    private fun visualizarProducto(producto: Producto) {

        val formatoPrecio = NumberFormat.getCurrencyInstance(
            Locale("es", "CO")
        )

        val detalles = """
            Nombre: ${producto.nombre}

            Marca: ${producto.marca}

            Precio: ${formatoPrecio.format(producto.precio)}

            Objetivo: ${producto.objetivo}

            Presentación: ${producto.presentacion}

            Estado: ${
            if (producto.activo) "Activo" else "Inactivo"
        }
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle(R.string.producto_detalles)
            .setMessage(detalles)
            .setPositiveButton(R.string.aceptar, null)
            .show()
    }

    // ACTIVAR Y DESACTIVAR

    private fun confirmarCambioEstado(producto: Producto) {

        val nuevoEstado = !producto.activo

        val mensaje = if (nuevoEstado) {
            R.string.confirmar_activar
        } else {
            R.string.confirmar_desactivar
        }

        AlertDialog.Builder(this)
            .setTitle(producto.nombre)
            .setMessage(mensaje)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.aceptar) { _, _ ->

                lifecycleScope.launch {

                    try {

                        val filas = productoDao.cambiarEstado(
                            producto.id,
                            nuevoEstado
                        )

                        if (filas > 0) {

                            Toast.makeText(
                                this@ProductoActivity,
                                if (nuevoEstado)
                                    R.string.producto_activado
                                else
                                    R.string.producto_desactivado,
                                Toast.LENGTH_SHORT
                            ).show()

                            cargarProductos()
                        }

                    } catch (e: Exception) {

                        Toast.makeText(
                            this@ProductoActivity,
                            R.string.error_actualizar,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .show()
    }

    override fun onResume() {
        super.onResume()

        if (::adapter.isInitialized) {
            cargarProductos()
        }
    }

    override fun onDestroy() {
        busquedaJob?.cancel()
        consultaJob?.cancel()
        super.onDestroy()
    }
}
