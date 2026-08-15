package com.PasajesYa.pasajesya

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.PasajesYa.pasajesya.databinding.ActivityCompraLocalBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class CompraLocalActivity : AppCompatActivity() {

    private lateinit var binding:
            ActivityCompraLocalBinding

    private lateinit var auth:
            FirebaseAuth

    private lateinit var firestore:
            FirebaseFirestore

    private lateinit var productoAdapter:
            ProductoCompraAdapter


    private val productos =
        mutableListOf<ProductoCompra>()


    private var usuarioNombre =
        ""

    private var usuarioCelular =
        ""

    private var usuarioCorreo =
        ""


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )


        binding =
            ActivityCompraLocalBinding.inflate(
                layoutInflater
            )


        setContentView(
            binding.root
        )


        auth =
            FirebaseAuth.getInstance()


        firestore =
            FirebaseFirestore.getInstance()


        if (
            auth.currentUser == null
        ) {

            finish()

            return
        }


        configurarPantalla()

        configurarRecycler()

        configurarEventos()

        cargarDatosUsuario()

        actualizarLista()
    }


    // =================================================
    // PANTALLA
    // =================================================

    private fun configurarPantalla() {

        window.statusBarColor =
            getColor(
                R.color.pasajes_primary_dark
            )


        window.navigationBarColor =
            getColor(
                R.color.pasajes_background
            )
    }


    // =================================================
    // RECYCLER
    // =================================================

    private fun configurarRecycler() {

        productoAdapter =
            ProductoCompraAdapter {
                    posicion ->

                eliminarProducto(
                    posicion
                )
            }


        binding.recyclerProductos.apply {

            layoutManager =
                LinearLayoutManager(
                    this@CompraLocalActivity
                )

            adapter =
                productoAdapter

            isNestedScrollingEnabled =
                false
        }
    }


    // =================================================
    // EVENTOS
    // =================================================

    private fun configurarEventos() {

        binding.btnVolver
            .setOnClickListener {

                finish()
            }


        binding.btnAgregarProducto
            .setOnClickListener {

                agregarProducto()
            }


        binding.rgLugarCompra
            .setOnCheckedChangeListener {
                    _,
                    checkedId ->


                binding.contenedorTiendaEspecifica.visibility =
                    if (
                        checkedId ==
                        R.id.rbTiendaEspecifica
                    ) {

                        View.VISIBLE

                    } else {

                        View.GONE
                    }
            }


        binding.btnPublicarCompra
            .setOnClickListener {

                validarYPublicar()
            }
    }


    // =================================================
    // AGREGAR PRODUCTO
    // =================================================

    private fun agregarProducto() {

        val nombre =
            binding.etProducto
                .text
                ?.toString()
                .orEmpty()
                .trim()


        val cantidad =
            binding.etCantidadProducto
                .text
                ?.toString()
                .orEmpty()
                .trim()


        val observacion =
            binding.etObservacionProducto
                .text
                ?.toString()
                .orEmpty()
                .trim()


        if (
            nombre.isBlank()
        ) {

            binding.etProducto.error =
                "Escribe el producto"

            binding.etProducto
                .requestFocus()

            return
        }


        if (
            cantidad.isBlank()
        ) {

            binding.etCantidadProducto.error =
                "Indica la cantidad"

            binding.etCantidadProducto
                .requestFocus()

            return
        }


        productos.add(

            ProductoCompra(

                nombre =
                    nombre,

                cantidad =
                    cantidad,

                observacion =
                    observacion
            )
        )


        binding.etProducto
            .setText(
                ""
            )


        binding.etCantidadProducto
            .setText(
                ""
            )


        binding.etObservacionProducto
            .setText(
                ""
            )


        binding.etProducto
            .requestFocus()


        actualizarLista()
    }


    // =================================================
    // ELIMINAR PRODUCTO
    // =================================================

    private fun eliminarProducto(
        posicion: Int
    ) {

        if (
            posicion < 0 ||
            posicion >= productos.size
        ) {

            return
        }


        productos.removeAt(
            posicion
        )


        actualizarLista()
    }


    // =================================================
    // ACTUALIZAR LISTA
    // =================================================

    private fun actualizarLista() {

        productoAdapter.actualizar(
            productos
        )


        binding.tvCantidadProductos.text =
            "Productos agregados: ${productos.size}"


        binding.tvSinProductos.visibility =
            if (
                productos.isEmpty()
            ) {

                View.VISIBLE

            } else {

                View.GONE
            }


        binding.recyclerProductos.visibility =
            if (
                productos.isEmpty()
            ) {

                View.GONE

            } else {

                View.VISIBLE
            }
    }


    // =================================================
    // DATOS USUARIO
    // =================================================

    private fun cargarDatosUsuario() {

        val usuario =
            auth.currentUser
                ?: return


        usuarioCorreo =
            usuario.email
                .orEmpty()


        firestore
            .collection(
                COLECCION_USUARIOS
            )
            .document(
                usuario.uid
            )
            .get()
            .addOnSuccessListener {
                    documento ->


                val nombres =
                    documento
                        .getString(
                            "nombres"
                        )
                        .orEmpty()
                        .trim()


                val apellidos =
                    documento
                        .getString(
                            "apellidos"
                        )
                        .orEmpty()
                        .trim()


                usuarioNombre =
                    "$nombres $apellidos"
                        .trim()


                usuarioCelular =
                    documento
                        .getString(
                            "celular"
                        )
                        .orEmpty()
                        .trim()


                val correo =
                    documento
                        .getString(
                            "correo"
                        )
                        .orEmpty()
                        .trim()


                if (
                    correo.isNotBlank()
                ) {

                    usuarioCorreo =
                        correo
                }
            }
    }


    // =================================================
    // VALIDAR
    // =================================================

    private fun validarYPublicar() {

        if (
            productos.isEmpty()
        ) {

            Toast.makeText(
                this,
                "Agrega al menos un producto a tu lista.",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        val tiendaEspecifica =
            binding.rbTiendaEspecifica
                .isChecked


        val nombreTienda =
            binding.etNombreTienda
                .text
                ?.toString()
                .orEmpty()
                .trim()


        val direccionTienda =
            binding.etDireccionTienda
                .text
                ?.toString()
                .orEmpty()
                .trim()


        if (
            tiendaEspecifica &&
            nombreTienda.isBlank()
        ) {

            binding.etNombreTienda.error =
                "Ingresa el nombre de la tienda"

            binding.etNombreTienda
                .requestFocus()

            return
        }


        val direccionEntrega =
            binding.etDireccionEntrega
                .text
                ?.toString()
                .orEmpty()
                .trim()


        if (
            direccionEntrega.isBlank()
        ) {

            binding.etDireccionEntrega.error =
                "Ingresa la dirección de entrega"

            binding.etDireccionEntrega
                .requestFocus()

            return
        }


        if (
            usuarioCelular.isBlank()
        ) {

            Toast.makeText(
                this,
                "No se pudo obtener tu número de celular. Revisa tu perfil.",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        val referenciaEntrega =
            binding.etReferenciaEntrega
                .text
                ?.toString()
                .orEmpty()
                .trim()


        val indicaciones =
            binding.etIndicaciones
                .text
                ?.toString()
                .orEmpty()
                .trim()


        val tipoTienda =
            if (
                tiendaEspecifica
            ) {

                TIPO_TIENDA_ESPECIFICA

            } else {

                TIPO_TIENDA_CUALQUIERA
            }


        val tipoUnidad =
            obtenerTipoUnidad()


        confirmarPublicacion(

            tipoTienda =
                tipoTienda,

            nombreTienda =
                nombreTienda,

            direccionTienda =
                direccionTienda,

            direccionEntrega =
                direccionEntrega,

            referenciaEntrega =
                referenciaEntrega,

            indicaciones =
                indicaciones,

            tipoUnidad =
                tipoUnidad
        )
    }


    // =================================================
    // TIPO UNIDAD
    // =================================================

    private fun obtenerTipoUnidad():
            String {

        return when {

            binding.rbMototaxi.isChecked -> {

                TIPO_MOTOTAXI
            }


            binding.rbTaxi.isChecked -> {

                TIPO_TAXI
            }


            else -> {

                TIPO_CUALQUIERA
            }
        }
    }


    // =================================================
    // CONFIRMAR
    // =================================================

    private fun confirmarPublicacion(
        tipoTienda: String,
        nombreTienda: String,
        direccionTienda: String,
        direccionEntrega: String,
        referenciaEntrega: String,
        indicaciones: String,
        tipoUnidad: String
    ) {

        val listaProductos =
            productos.joinToString(
                separator = "\n"
            ) { producto ->

                "• ${producto.cantidad} - ${producto.nombre}"
            }


        val textoTienda =
            if (
                tipoTienda ==
                TIPO_TIENDA_ESPECIFICA
            ) {

                nombreTienda

            } else {

                "Cualquier tienda cercana"
            }


        val textoUnidad =
            when (
                tipoUnidad
            ) {

                TIPO_MOTOTAXI -> {

                    "Mototaxi"
                }


                TIPO_TAXI -> {

                    "Taxi / vehículo"
                }


                else -> {

                    "Cualquier unidad"
                }
            }


        MaterialAlertDialogBuilder(
            this
        )
            .setTitle(
                "Publicar compra"
            )
            .setMessage(
                "Productos:\n\n" +
                        listaProductos +
                        "\n\nComprar en:\n" +
                        textoTienda +
                        "\n\nEntregar en:\n" +
                        direccionEntrega +
                        "\n\nUnidad:\n" +
                        textoUnidad +
                        "\n\nLos conductores podrán enviarte sus ofertas."
            )
            .setNegativeButton(
                "CANCELAR",
                null
            )
            .setPositiveButton(
                "PUBLICAR"
            ) {
                    _, _ ->


                publicarCompra(

                    tipoTienda =
                        tipoTienda,

                    nombreTienda =
                        nombreTienda,

                    direccionTienda =
                        direccionTienda,

                    direccionEntrega =
                        direccionEntrega,

                    referenciaEntrega =
                        referenciaEntrega,

                    indicaciones =
                        indicaciones,

                    tipoUnidad =
                        tipoUnidad
                )
            }
            .show()
    }


    // =================================================
    // PUBLICAR
    // =================================================

    private fun publicarCompra(
        tipoTienda: String,
        nombreTienda: String,
        direccionTienda: String,
        direccionEntrega: String,
        referenciaEntrega: String,
        indicaciones: String,
        tipoUnidad: String
    ) {

        val usuario =
            auth.currentUser


        if (
            usuario == null
        ) {

            Toast.makeText(
                this,
                "Tu sesión ha finalizado.",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        mostrarCargando(
            true
        )


        val referencia =
            firestore
                .collection(
                    COLECCION_SOLICITUDES
                )
                .document()


        val solicitudId =
            referencia.id


        val listaProductosFirestore =
            productos.map {
                    producto ->


                hashMapOf<String, Any>(

                    "nombre" to
                            producto.nombre,

                    "cantidad" to
                            producto.cantidad,

                    "observacion" to
                            producto.observacion
                )
            }


        val datos =
            hashMapOf<String, Any>(


                // -----------------------------------------
                // IDENTIFICACIÓN
                // -----------------------------------------

                "id" to
                        solicitudId,

                "tipoSolicitud" to
                        TIPO_SOLICITUD_COMPRA,


                // -----------------------------------------
                // CLIENTE
                // -----------------------------------------

                "usuarioId" to
                        usuario.uid,

                "usuarioNombre" to
                        usuarioNombre,

                "usuarioCelular" to
                        usuarioCelular,

                "usuarioCorreo" to
                        usuarioCorreo,


                // -----------------------------------------
                // PRODUCTOS
                // -----------------------------------------

                "productos" to
                        listaProductosFirestore,

                "cantidadProductos" to
                        productos.size,


                // -----------------------------------------
                // LUGAR DE COMPRA
                // -----------------------------------------

                "tipoTienda" to
                        tipoTienda,

                "nombreTienda" to
                        nombreTienda,

                "direccionTienda" to
                        direccionTienda,


                // -----------------------------------------
                // ENTREGA
                // -----------------------------------------

                "direccionEntrega" to
                        direccionEntrega,

                "referenciaEntrega" to
                        referenciaEntrega,

                "latitudEntrega" to
                        0.0,

                "longitudEntrega" to
                        0.0,


                // -----------------------------------------
                // INDICACIONES
                // -----------------------------------------

                "indicaciones" to
                        indicaciones,


                // -----------------------------------------
                // UNIDAD
                // -----------------------------------------

                "tipoUnidad" to
                        tipoUnidad,


                // -----------------------------------------
                // ESTADO
                // -----------------------------------------

                "estado" to
                        ESTADO_PUBLICADO,

                "aceptaOfertas" to
                        true,

                "cantidadOfertas" to
                        0,


                // -----------------------------------------
                // CONDUCTOR SELECCIONADO
                // -----------------------------------------

                "conductorSeleccionadoId" to
                        "",

                "conductorSeleccionadoNombre" to
                        "",

                "conductorSeleccionadoCelular" to
                        "",

                "precioServicioAceptado" to
                        0.0,


                // -----------------------------------------
                // COSTO DE COMPRA
                // -----------------------------------------

                "costoCompra" to
                        0.0,

                "comprobanteCompraUrl" to
                        "",


                // -----------------------------------------
                // FECHAS
                // -----------------------------------------

                "fechaPublicacion" to
                        FieldValue.serverTimestamp(),

                "fechaActualizacion" to
                        FieldValue.serverTimestamp()
            )


        referencia
            .set(
                datos
            )
            .addOnSuccessListener {

                mostrarCargando(
                    false
                )

                compraPublicada()
            }
            .addOnFailureListener {
                    error ->


                mostrarCargando(
                    false
                )


                Toast.makeText(
                    this,
                    "No se pudo publicar la compra: " +
                            (
                                    error.localizedMessage
                                        ?: "Error desconocido"
                                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
    }


    // =================================================
    // PUBLICADO
    // =================================================

    private fun compraPublicada() {

        MaterialAlertDialogBuilder(
            this
        )
            .setTitle(
                "Solicitud publicada"
            )
            .setMessage(
                "Tu lista de compras fue publicada correctamente.\n\n" +
                        "Ahora los conductores podrán revisar tus productos " +
                        "y ofrecerte cuánto cobrarían por realizar el mandado."
            )
            .setCancelable(
                false
            )
            .setPositiveButton(
                "ACEPTAR"
            ) {
                    _, _ ->

                finish()
            }
            .show()
    }


    // =================================================
    // CARGANDO
    // =================================================

    private fun mostrarCargando(
        mostrar: Boolean
    ) {

        binding.progressPublicar.visibility =
            if (
                mostrar
            ) {

                View.VISIBLE

            } else {

                View.GONE
            }


        binding.btnPublicarCompra.isEnabled =
            !mostrar


        binding.btnAgregarProducto.isEnabled =
            !mostrar
    }


    // =================================================
    // CONSTANTES
    // =================================================

    companion object {

        private const val
                COLECCION_USUARIOS =
            "usuariospasajes"


        private const val
                COLECCION_SOLICITUDES =
            "solicitudeslocales"


        private const val
                TIPO_SOLICITUD_COMPRA =
            "compra"


        private const val
                TIPO_TIENDA_CUALQUIERA =
            "cualquiera"


        private const val
                TIPO_TIENDA_ESPECIFICA =
            "especifica"


        private const val
                TIPO_MOTOTAXI =
            "mototaxi"


        private const val
                TIPO_TAXI =
            "taxi"


        private const val
                TIPO_CUALQUIERA =
            "cualquiera"


        private const val
                ESTADO_PUBLICADO =
            "publicado"
    }
}