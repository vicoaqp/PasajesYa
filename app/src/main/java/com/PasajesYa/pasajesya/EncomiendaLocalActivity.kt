package com.PasajesYa.pasajesya

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.PasajesYa.pasajesya.databinding.ActivityEncomiendaLocalBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class EncomiendaLocalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEncomiendaLocalBinding

    private lateinit var auth: FirebaseAuth

    private lateinit var firestore: FirebaseFirestore

    private lateinit var storage: FirebaseStorage


    private var fotoUri: Uri? = null


    private var usuarioNombre = ""

    private var usuarioCelular = ""

    private var usuarioCorreo = ""


    // =================================================
    // SELECCIONAR FOTO
    // =================================================

    private val selectorFoto =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri != null) {

                fotoUri = uri

                binding.imgEncomienda.setImageURI(
                    uri
                )

                binding.imgEncomienda.scaleType =
                    ImageView.ScaleType.CENTER_CROP
            }
        }


    // =================================================
    // ON CREATE
    // =================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )


        binding =
            ActivityEncomiendaLocalBinding.inflate(
                layoutInflater
            )


        setContentView(
            binding.root
        )


        auth =
            FirebaseAuth.getInstance()


        firestore =
            FirebaseFirestore.getInstance()


        storage =
            FirebaseStorage.getInstance()


        if (
            auth.currentUser == null
        ) {

            finish()

            return
        }


        configurarPantalla()

        configurarEventos()

        cargarDatosUsuario()
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
    // EVENTOS
    // =================================================

    private fun configurarEventos() {

        binding.btnVolver
            .setOnClickListener {

                finish()
            }


        binding.btnAgregarFoto
            .setOnClickListener {

                selectorFoto.launch(
                    "image/*"
                )
            }


        binding.imgEncomienda
            .setOnClickListener {

                selectorFoto.launch(
                    "image/*"
                )
            }


        binding.btnPublicar
            .setOnClickListener {

                validarYPublicar()
            }
    }


    // =================================================
    // DATOS DEL USUARIO
    // =================================================

    private fun cargarDatosUsuario() {

        val usuario =
            auth.currentUser
                ?: return


        usuarioCorreo =
            usuario.email.orEmpty()


        firestore
            .collection(
                COLECCION_USUARIOS
            )
            .document(
                usuario.uid
            )
            .get()
            .addOnSuccessListener { documento ->


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


                val correoFirestore =
                    documento
                        .getString(
                            "correo"
                        )
                        .orEmpty()
                        .trim()


                if (
                    correoFirestore.isNotBlank()
                ) {

                    usuarioCorreo =
                        correoFirestore
                }
            }
            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "No se pudieron cargar todos los datos de tu perfil.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }


    // =================================================
    // VALIDAR
    // =================================================

    private fun validarYPublicar() {

        val descripcion =
            binding.etDescripcion
                .text
                ?.toString()
                .orEmpty()
                .trim()


        val cantidadTexto =
            binding.etCantidad
                .text
                ?.toString()
                .orEmpty()
                .trim()


        val peso =
            binding.etPeso
                .text
                ?.toString()
                .orEmpty()
                .trim()


        val direccionRecojo =
            binding.etDireccionRecojo
                .text
                ?.toString()
                .orEmpty()
                .trim()


        val referenciaRecojo =
            binding.etReferenciaRecojo
                .text
                ?.toString()
                .orEmpty()
                .trim()


        val direccionEntrega =
            binding.etDireccionEntrega
                .text
                ?.toString()
                .orEmpty()
                .trim()


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


        // ---------------------------------------------
        // DESCRIPCIÓN
        // ---------------------------------------------

        if (
            descripcion.isBlank()
        ) {

            binding.etDescripcion.error =
                "Describe lo que deseas trasladar"


            binding.etDescripcion
                .requestFocus()


            return
        }


        // ---------------------------------------------
        // CANTIDAD
        // ---------------------------------------------

        if (
            cantidadTexto.isBlank()
        ) {

            binding.etCantidad.error =
                "Ingresa la cantidad"


            binding.etCantidad
                .requestFocus()


            return
        }


        val cantidad =
            cantidadTexto.toIntOrNull()


        if (
            cantidad == null ||
            cantidad <= 0
        ) {

            binding.etCantidad.error =
                "Ingresa una cantidad válida"


            binding.etCantidad
                .requestFocus()


            return
        }


        // ---------------------------------------------
        // RECOJO
        // ---------------------------------------------

        if (
            direccionRecojo.isBlank()
        ) {

            binding.etDireccionRecojo.error =
                "Ingresa el lugar de recojo"


            binding.etDireccionRecojo
                .requestFocus()


            return
        }


        // ---------------------------------------------
        // ENTREGA
        // ---------------------------------------------

        if (
            direccionEntrega.isBlank()
        ) {

            binding.etDireccionEntrega.error =
                "Ingresa el lugar de entrega"


            binding.etDireccionEntrega
                .requestFocus()


            return
        }


        // ---------------------------------------------
        // CELULAR DEL USUARIO
        // ---------------------------------------------

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


        val tamano =
            obtenerTamano()


        val tipoUnidad =
            obtenerTipoUnidad()


        confirmarPublicacion(

            descripcion =
                descripcion,

            cantidad =
                cantidad,

            peso =
                peso,

            tamano =
                tamano,

            direccionRecojo =
                direccionRecojo,

            referenciaRecojo =
                referenciaRecojo,

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
    // TAMAÑO
    // =================================================

    private fun obtenerTamano(): String {

        return when {

            binding.rbGrande.isChecked -> {

                TAMANO_GRANDE
            }


            binding.rbMediano.isChecked -> {

                TAMANO_MEDIANO
            }


            else -> {

                TAMANO_PEQUENO
            }
        }
    }


    // =================================================
    // TIPO DE UNIDAD
    // =================================================

    private fun obtenerTipoUnidad(): String {

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
    // CONFIRMAR PUBLICACIÓN
    // =================================================

    private fun confirmarPublicacion(
        descripcion: String,
        cantidad: Int,
        peso: String,
        tamano: String,
        direccionRecojo: String,
        referenciaRecojo: String,
        direccionEntrega: String,
        referenciaEntrega: String,
        indicaciones: String,
        tipoUnidad: String
    ) {

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
                "Publicar encomienda"
            )
            .setMessage(
                "Se solicitará el traslado de:\n\n" +
                        "$descripcion\n\n" +
                        "Recojo:\n$direccionRecojo\n\n" +
                        "Entrega:\n$direccionEntrega\n\n" +
                        "Unidad: $textoUnidad\n\n" +
                        "Los conductores podrán enviarte sus ofertas."
            )
            .setNegativeButton(
                "CANCELAR",
                null
            )
            .setPositiveButton(
                "PUBLICAR"
            ) { _, _ ->


                publicarEncomienda(

                    descripcion =
                        descripcion,

                    cantidad =
                        cantidad,

                    peso =
                        peso,

                    tamano =
                        tamano,

                    direccionRecojo =
                        direccionRecojo,

                    referenciaRecojo =
                        referenciaRecojo,

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
    // PUBLICAR ENCOMIENDA
    // =================================================

    private fun publicarEncomienda(
        descripcion: String,
        cantidad: Int,
        peso: String,
        tamano: String,
        direccionRecojo: String,
        referenciaRecojo: String,
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


        // ---------------------------------------------
        // GENERAR ID PARA LA SOLICITUD
        // ---------------------------------------------

        val referenciaSolicitud =
            firestore
                .collection(
                    COLECCION_SOLICITUDES
                )
                .document()


        val solicitudId =
            referenciaSolicitud.id


        val uri =
            fotoUri


        // ---------------------------------------------
        // SIN FOTO
        // ---------------------------------------------

        if (
            uri == null
        ) {

            guardarSolicitudFirestore(

                solicitudId =
                    solicitudId,

                fotoUrl =
                    "",

                descripcion =
                    descripcion,

                cantidad =
                    cantidad,

                peso =
                    peso,

                tamano =
                    tamano,

                direccionRecojo =
                    direccionRecojo,

                referenciaRecojo =
                    referenciaRecojo,

                direccionEntrega =
                    direccionEntrega,

                referenciaEntrega =
                    referenciaEntrega,

                indicaciones =
                    indicaciones,

                tipoUnidad =
                    tipoUnidad
            )


            return
        }


        // ---------------------------------------------
        // CON FOTO
        // ---------------------------------------------

        subirFoto(

            solicitudId =
                solicitudId,

            uri =
                uri,

            descripcion =
                descripcion,

            cantidad =
                cantidad,

            peso =
                peso,

            tamano =
                tamano,

            direccionRecojo =
                direccionRecojo,

            referenciaRecojo =
                referenciaRecojo,

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
    // SUBIR FOTO
    // =================================================

    private fun subirFoto(
        solicitudId: String,
        uri: Uri,
        descripcion: String,
        cantidad: Int,
        peso: String,
        tamano: String,
        direccionRecojo: String,
        referenciaRecojo: String,
        direccionEntrega: String,
        referenciaEntrega: String,
        indicaciones: String,
        tipoUnidad: String
    ) {

        // ---------------------------------------------
        // USUARIO ACTUAL
        // ---------------------------------------------

        val usuario =
            auth.currentUser


        if (
            usuario == null
        ) {

            mostrarCargando(
                false
            )


            Toast.makeText(
                this,
                "Tu sesión ha finalizado.",
                Toast.LENGTH_LONG
            ).show()


            return
        }


        // ---------------------------------------------
        // RUTA DE STORAGE
        //
        // solicitudeslocales
        //   └── UID USUARIO
        //        └── ID SOLICITUD
        //             └── foto_encomienda.jpg
        // ---------------------------------------------

        val referenciaFoto =
            storage
                .reference
                .child(
                    "solicitudeslocales"
                )
                .child(
                    usuario.uid
                )
                .child(
                    solicitudId
                )
                .child(
                    "foto_encomienda.jpg"
                )


        // ---------------------------------------------
        // SUBIR FOTO
        // ---------------------------------------------

        referenciaFoto
            .putFile(
                uri
            )
            .continueWithTask { tarea ->


                if (
                    !tarea.isSuccessful
                ) {

                    tarea.exception
                        ?.let { error ->

                            throw error
                        }
                }


                referenciaFoto.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->


                guardarSolicitudFirestore(

                    solicitudId =
                        solicitudId,

                    fotoUrl =
                        downloadUri.toString(),

                    descripcion =
                        descripcion,

                    cantidad =
                        cantidad,

                    peso =
                        peso,

                    tamano =
                        tamano,

                    direccionRecojo =
                        direccionRecojo,

                    referenciaRecojo =
                        referenciaRecojo,

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
            .addOnFailureListener { error ->


                mostrarCargando(
                    false
                )


                Toast.makeText(
                    this,
                    "No se pudo subir la foto: " +
                            (
                                    error.localizedMessage
                                        ?: "Error desconocido"
                                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
    }


    // =================================================
    // GUARDAR SOLICITUD EN FIRESTORE
    // =================================================

    private fun guardarSolicitudFirestore(
        solicitudId: String,
        fotoUrl: String,
        descripcion: String,
        cantidad: Int,
        peso: String,
        tamano: String,
        direccionRecojo: String,
        referenciaRecojo: String,
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

            mostrarCargando(
                false
            )


            Toast.makeText(
                this,
                "Tu sesión ha finalizado.",
                Toast.LENGTH_LONG
            ).show()


            return
        }


        val datos =
            hashMapOf<String, Any>(


                // =========================================
                // IDENTIFICACION
                // =========================================

                "id" to
                        solicitudId,

                "tipoSolicitud" to
                        TIPO_SOLICITUD_ENCOMIENDA,


                // =========================================
                // CLIENTE
                // =========================================

                "usuarioId" to
                        usuario.uid,

                "usuarioNombre" to
                        usuarioNombre,

                "usuarioCelular" to
                        usuarioCelular,

                "usuarioCorreo" to
                        usuarioCorreo,


                // =========================================
                // ENCOMIENDA
                // =========================================

                "descripcion" to
                        descripcion,

                "cantidad" to
                        cantidad,

                "pesoAproximado" to
                        peso,

                "tamano" to
                        tamano,

                "fotoUrl" to
                        fotoUrl,


                // =========================================
                // RECOJO
                // =========================================

                "direccionRecojo" to
                        direccionRecojo,

                "referenciaRecojo" to
                        referenciaRecojo,

                "latitudRecojo" to
                        0.0,

                "longitudRecojo" to
                        0.0,


                // =========================================
                // ENTREGA
                // =========================================

                "direccionEntrega" to
                        direccionEntrega,

                "referenciaEntrega" to
                        referenciaEntrega,

                "latitudEntrega" to
                        0.0,

                "longitudEntrega" to
                        0.0,


                // =========================================
                // INDICACIONES
                // =========================================

                "indicaciones" to
                        indicaciones,


                // =========================================
                // UNIDAD SOLICITADA
                // =========================================

                "tipoUnidad" to
                        tipoUnidad,


                // =========================================
                // ESTADO
                // =========================================

                "estado" to
                        ESTADO_PUBLICADO,

                "aceptaOfertas" to
                        true,

                "cantidadOfertas" to
                        0,


                // =========================================
                // CONDUCTOR SELECCIONADO
                // =========================================

                "conductorSeleccionadoId" to
                        "",

                "conductorSeleccionadoNombre" to
                        "",

                "conductorSeleccionadoCelular" to
                        "",

                "precioAceptado" to
                        0.0,


                // =========================================
                // FECHAS
                // =========================================

                "fechaPublicacion" to
                        FieldValue.serverTimestamp(),

                "fechaActualizacion" to
                        FieldValue.serverTimestamp()
            )


        firestore
            .collection(
                COLECCION_SOLICITUDES
            )
            .document(
                solicitudId
            )
            .set(
                datos
            )
            .addOnSuccessListener {


                mostrarCargando(
                    false
                )


                mostrarPublicadoCorrectamente(
                    solicitudId
                )
            }
            .addOnFailureListener { error ->


                mostrarCargando(
                    false
                )


                Toast.makeText(
                    this,
                    "No se pudo publicar la encomienda: " +
                            (
                                    error.localizedMessage
                                        ?: "Error desconocido"
                                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
    }


    // =================================================
    // PUBLICADO CORRECTAMENTE
    // =================================================

    private fun mostrarPublicadoCorrectamente(
        solicitudId: String
    ) {

        MaterialAlertDialogBuilder(
            this
        )
            .setTitle(
                "Encomienda publicada"
            )
            .setMessage(
                "Tu solicitud fue publicada correctamente.\n\n" +
                        "Ahora los conductores disponibles podrán revisar " +
                        "tu encomienda y enviarte una oferta."
            )
            .setCancelable(
                false
            )
            .setPositiveButton(
                "ACEPTAR"
            ) { _, _ ->


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


        binding.btnPublicar.isEnabled =
            !mostrar


        binding.btnAgregarFoto.isEnabled =
            !mostrar


        binding.imgEncomienda.isEnabled =
            !mostrar
    }


    // =================================================
    // CONSTANTES
    // =================================================

    companion object {

        private const val COLECCION_USUARIOS =
            "usuariospasajes"


        private const val COLECCION_SOLICITUDES =
            "solicitudeslocales"


        private const val TIPO_SOLICITUD_ENCOMIENDA =
            "encomienda"


        private const val TIPO_MOTOTAXI =
            "mototaxi"


        private const val TIPO_TAXI =
            "taxi"


        private const val TIPO_CUALQUIERA =
            "cualquiera"


        private const val TAMANO_PEQUENO =
            "pequeno"


        private const val TAMANO_MEDIANO =
            "mediano"


        private const val TAMANO_GRANDE =
            "grande"


        private const val ESTADO_PUBLICADO =
            "publicado"
    }
}