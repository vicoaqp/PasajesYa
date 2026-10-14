package com.PasajesYa.pasajesya

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.PasajesYa.pasajesya.databinding.ActivityMovilidadDisponibleBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore


class MovilidadDisponibleActivity : AppCompatActivity() {


    // =========================================================
    // VIEW BINDING
    // =========================================================

    private lateinit var binding:
            ActivityMovilidadDisponibleBinding


    // =========================================================
    // FIREBASE
    // =========================================================

    private lateinit var auth:
            FirebaseAuth

    private lateinit var firestore:
            FirebaseFirestore


    // =========================================================
    // DATOS DEL USUARIO
    // =========================================================

    private var nombreUsuario = ""

    private var celularUsuario = ""

    private var fotoUsuario = ""


    // =========================================================
    // ORIGEN
    // =========================================================

    private var origenDireccion = ""

    private var origenLatitud: Double? = null

    private var origenLongitud: Double? = null

    private var origenReferencia = ""


    // =========================================================
    // DESTINO
    // =========================================================

    private var destinoDireccion = ""

    private var destinoLatitud: Double? = null

    private var destinoLongitud: Double? = null

    private var destinoReferencia = ""


    // =========================================================
    // TIPO DE SERVICIO
    // =========================================================

    private var tipoServicio =
        TIPO_TAXI


    // =========================================================
    // CONTROL
    // =========================================================

    private var solicitudEnProceso =
        false


    // =========================================================
    // RESULTADO: CONFIGURAR CASA
    // =========================================================

    private val launcherConfigurarCasa =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { resultado ->


            if (
                resultado.resultCode ==
                Activity.RESULT_OK
            ) {

                val data =
                    resultado.data


                if (data != null) {

                    origenDireccion =
                        data.getStringExtra(
                            SeleccionarUbicacionActivity
                                .EXTRA_DIRECCION_RESULTADO
                        ).orEmpty()


                    origenLatitud =
                        obtenerDoubleExtra(
                            data,
                            SeleccionarUbicacionActivity
                                .EXTRA_LATITUD_RESULTADO
                        )


                    origenLongitud =
                        obtenerDoubleExtra(
                            data,
                            SeleccionarUbicacionActivity
                                .EXTRA_LONGITUD_RESULTADO
                        )


                    origenReferencia =
                        data.getStringExtra(
                            SeleccionarUbicacionActivity
                                .EXTRA_REFERENCIA_RESULTADO
                        ).orEmpty()


                    actualizarOrigenPantalla()
                }

            } else {


                // Si canceló la configuración de casa

                if (
                    origenLatitud == null ||
                    origenLongitud == null
                ) {

                    binding.tvOrigen.text =
                        "Necesitas configurar tu ubicación de casa"
                }
            }
        }


    // =========================================================
    // RESULTADO: CAMBIAR ORIGEN
    // =========================================================

    private val launcherSeleccionarOrigen =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { resultado ->


            if (
                resultado.resultCode ==
                Activity.RESULT_OK
            ) {

                val data =
                    resultado.data
                        ?: return@registerForActivityResult


                origenDireccion =
                    data.getStringExtra(
                        SeleccionarUbicacionActivity
                            .EXTRA_DIRECCION_RESULTADO
                    ).orEmpty()


                origenLatitud =
                    obtenerDoubleExtra(
                        data,
                        SeleccionarUbicacionActivity
                            .EXTRA_LATITUD_RESULTADO
                    )


                origenLongitud =
                    obtenerDoubleExtra(
                        data,
                        SeleccionarUbicacionActivity
                            .EXTRA_LONGITUD_RESULTADO
                    )


                origenReferencia =
                    data.getStringExtra(
                        SeleccionarUbicacionActivity
                            .EXTRA_REFERENCIA_RESULTADO
                    ).orEmpty()


                actualizarOrigenPantalla()
            }
        }


    // =========================================================
    // RESULTADO: DESTINO
    // =========================================================

    private val launcherSeleccionarDestino =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { resultado ->


            if (
                resultado.resultCode ==
                Activity.RESULT_OK
            ) {

                val data =
                    resultado.data
                        ?: return@registerForActivityResult


                destinoDireccion =
                    data.getStringExtra(
                        SeleccionarUbicacionActivity
                            .EXTRA_DIRECCION_RESULTADO
                    ).orEmpty()


                destinoLatitud =
                    obtenerDoubleExtra(
                        data,
                        SeleccionarUbicacionActivity
                            .EXTRA_LATITUD_RESULTADO
                    )


                destinoLongitud =
                    obtenerDoubleExtra(
                        data,
                        SeleccionarUbicacionActivity
                            .EXTRA_LONGITUD_RESULTADO
                    )


                destinoReferencia =
                    data.getStringExtra(
                        SeleccionarUbicacionActivity
                            .EXTRA_REFERENCIA_RESULTADO
                    ).orEmpty()


                actualizarDestinoPantalla()
            }
        }


    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )


        binding =
            ActivityMovilidadDisponibleBinding.inflate(
                layoutInflater
            )


        setContentView(
            binding.root
        )


        // Firebase

        auth =
            FirebaseAuth.getInstance()


        firestore =
            FirebaseFirestore.getInstance()


        // Configuración inicial

        configurarEventos()


        seleccionarTipoServicio(
            TIPO_TAXI
        )


        cargarUsuarioYOrigen()
    }


    // =========================================================
    // EVENTOS
    // =========================================================

    private fun configurarEventos() {


        // -----------------------------------------------------
        // VOLVER
        // -----------------------------------------------------

        binding.btnVolver.setOnClickListener {

            finish()
        }


        // -----------------------------------------------------
        // CAMBIAR ORIGEN
        // -----------------------------------------------------

        binding.cardOrigen.setOnClickListener {


            if (
                origenLatitud == null ||
                origenLongitud == null
            ) {

                abrirConfiguracionCasa()

            } else {

                abrirSelectorOrigen()
            }
        }


        // -----------------------------------------------------
        // DESTINO
        // -----------------------------------------------------

        binding.cardDestino.setOnClickListener {

            abrirSelectorDestino()
        }


        // -----------------------------------------------------
        // TAXI
        // -----------------------------------------------------

        binding.cardTaxi.setOnClickListener {

            seleccionarTipoServicio(
                TIPO_TAXI
            )
        }


        // -----------------------------------------------------
        // MOTOTAXI
        // -----------------------------------------------------

        binding.cardMototaxi.setOnClickListener {

            seleccionarTipoServicio(
                TIPO_MOTOTAXI
            )
        }


        // -----------------------------------------------------
        // SOLICITAR OFERTAS
        // -----------------------------------------------------

        binding.btnSolicitarOfertas
            .setOnClickListener {

                validarYSolicitarOfertas()
            }

        // -------------------------------------------------------
        // SERVICIO RÁPIDO
        // -------------------------------------------------------

        binding.btnServicioRapido.setOnClickListener {
            startActivity(
                android.content.Intent(
                    this,
                    SolicitudRapidaActivity::class.java
                )
            )
        }

        // -------------------------------------------------------
        // VER CONDUCTORES DISPONIBLES
        // -------------------------------------------------------

        binding.btnVerConductores.setOnClickListener {
            startActivity(
                android.content.Intent(
                    this,
                    ConductoresDisponiblesActivity::class.java
                )
            )
        }

        // -------------------------------------------------------
        // VER MIS PEDIDOS (historial)
        // -------------------------------------------------------

        binding.btnVerMisPedidos.setOnClickListener {
            startActivity(
                android.content.Intent(
                    this,
                    MisSolicitudesMovilidadActivity::class.java
                )
            )
        }
    }


    // =========================================================
    // CARGAR USUARIO Y CASA
    // =========================================================

    private fun cargarUsuarioYOrigen() {

        val usuario =
            auth.currentUser


        if (usuario == null) {

            Toast.makeText(
                this,
                "Tu sesión ha finalizado. Ingresa nuevamente.",
                Toast.LENGTH_LONG
            ).show()

            finish()

            return
        }


        binding.tvOrigen.text =
            "Cargando ubicación de casa..."


        firestore
            .collection(COLECCION_USUARIOS)
            .document(usuario.uid)
            .get()
            .addOnSuccessListener { documento ->


                // =================================================
                // DATOS DEL USUARIO
                // =================================================

                val nombres =
                    documento
                        .getString(CAMPO_NOMBRES)
                        .orEmpty()


                val apellidos =
                    documento
                        .getString(CAMPO_APELLIDOS)
                        .orEmpty()


                nombreUsuario =
                    "$nombres $apellidos"
                        .trim()


                if (nombreUsuario.isBlank()) {

                    nombreUsuario =
                        usuario.displayName
                            .orEmpty()
                }


                celularUsuario =
                    documento
                        .getString(CAMPO_CELULAR)
                        .orEmpty()


                fotoUsuario =
                    documento
                        .getString(CAMPO_FOTO_URL)
                        .orEmpty()


                // Si Firestore no tiene foto,
                // intentamos usar foto de Google.

                if (fotoUsuario.isBlank()) {

                    fotoUsuario =
                        usuario.photoUrl
                            ?.toString()
                            .orEmpty()
                }


                // =================================================
                // CASA
                // =================================================

                val ubicacionCasa =
                    documento.get(
                        CAMPO_UBICACION_CASA
                    ) as? Map<*, *>


                origenDireccion =
                    ubicacionCasa
                        ?.get(CAMPO_DIRECCION)
                        ?.toString()
                        .orEmpty()


                origenLatitud =
                    (
                            ubicacionCasa
                                ?.get(CAMPO_LATITUD)
                                    as? Number
                            )
                        ?.toDouble()


                origenLongitud =
                    (
                            ubicacionCasa
                                ?.get(CAMPO_LONGITUD)
                                    as? Number
                            )
                        ?.toDouble()


                origenReferencia =
                    ubicacionCasa
                        ?.get(CAMPO_REFERENCIA)
                        ?.toString()
                        .orEmpty()


                // =================================================
                // ¿TIENE CASA?
                // =================================================

                if (
                    origenLatitud != null &&
                    origenLongitud != null
                ) {

                    actualizarOrigenPantalla()

                } else {

                    binding.tvOrigen.text =
                        "Configura tu ubicación de casa"


                    mostrarAvisoConfigurarCasa()
                }
            }
            .addOnFailureListener { error ->


                binding.tvOrigen.text =
                    "No se pudo cargar tu ubicación"


                Toast.makeText(
                    this,
                    "No se pudo cargar tu perfil: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }


    // =========================================================
    // AVISO CONFIGURAR CASA
    // =========================================================

    private fun mostrarAvisoConfigurarCasa() {

        if (
            isFinishing ||
            isDestroyed
        ) {

            return
        }


        MaterialAlertDialogBuilder(this)
            .setTitle(
                "Configura tu ubicación"
            )
            .setMessage(
                "Para solicitar una movilidad necesitamos conocer tu ubicación principal. Podrás cambiar el punto de recojo para cada viaje."
            )
            .setPositiveButton(
                "CONFIGURAR CASA"
            ) { _, _ ->

                abrirConfiguracionCasa()
            }
            .setNegativeButton(
                "AHORA NO",
                null
            )
            .show()
    }


    // =========================================================
    // ABRIR CONFIGURACIÓN CASA
    // =========================================================

    private fun abrirConfiguracionCasa() {

        val intent =
            Intent(
                this,
                SeleccionarUbicacionActivity::class.java
            ).apply {


                putExtra(
                    SeleccionarUbicacionActivity
                        .EXTRA_TIPO_UBICACION,
                    SeleccionarUbicacionActivity
                        .TIPO_CASA
                )


                putExtra(
                    SeleccionarUbicacionActivity
                        .EXTRA_ORIGEN_UBICACION,
                    SeleccionarUbicacionActivity
                        .ORIGEN_MOVILIDAD
                )
            }


        launcherConfigurarCasa.launch(
            intent
        )
    }


    // =========================================================
    // CAMBIAR ORIGEN
    // =========================================================

    private fun abrirSelectorOrigen() {

        val intent =
            Intent(
                this,
                SeleccionarUbicacionActivity::class.java
            ).apply {


                putExtra(
                    SeleccionarUbicacionActivity
                        .EXTRA_TIPO_UBICACION,
                    SeleccionarUbicacionActivity
                        .TIPO_ORIGEN
                )


                putExtra(
                    SeleccionarUbicacionActivity
                        .EXTRA_ORIGEN_UBICACION,
                    SeleccionarUbicacionActivity
                        .ORIGEN_MOVILIDAD
                )


                // Punto actual como centro inicial

                origenLatitud?.let {

                    putExtra(
                        SeleccionarUbicacionActivity
                            .EXTRA_LATITUD_INICIAL,
                        it
                    )
                }


                origenLongitud?.let {

                    putExtra(
                        SeleccionarUbicacionActivity
                            .EXTRA_LONGITUD_INICIAL,
                        it
                    )
                }


                if (
                    origenDireccion.isNotBlank()
                ) {

                    putExtra(
                        SeleccionarUbicacionActivity
                            .EXTRA_DIRECCION_INICIAL,
                        origenDireccion
                    )
                }
            }


        launcherSeleccionarOrigen.launch(
            intent
        )
    }


    // =========================================================
    // DESTINO
    // =========================================================

    private fun abrirSelectorDestino() {

        val intent =
            Intent(
                this,
                SeleccionarUbicacionActivity::class.java
            ).apply {


                putExtra(
                    SeleccionarUbicacionActivity
                        .EXTRA_TIPO_UBICACION,
                    SeleccionarUbicacionActivity
                        .TIPO_DESTINO
                )


                putExtra(
                    SeleccionarUbicacionActivity
                        .EXTRA_ORIGEN_UBICACION,
                    SeleccionarUbicacionActivity
                        .ORIGEN_MOVILIDAD
                )


                // Si ya seleccionó destino,
                // volvemos a abrir en ese punto.

                destinoLatitud?.let {

                    putExtra(
                        SeleccionarUbicacionActivity
                            .EXTRA_LATITUD_INICIAL,
                        it
                    )
                }


                destinoLongitud?.let {

                    putExtra(
                        SeleccionarUbicacionActivity
                            .EXTRA_LONGITUD_INICIAL,
                        it
                    )
                }


                if (
                    destinoDireccion.isNotBlank()
                ) {

                    putExtra(
                        SeleccionarUbicacionActivity
                            .EXTRA_DIRECCION_INICIAL,
                        destinoDireccion
                    )
                }
            }


        launcherSeleccionarDestino.launch(
            intent
        )
    }


    // =========================================================
    // ACTUALIZAR ORIGEN
    // =========================================================

    private fun actualizarOrigenPantalla() {

        binding.tvOrigen.text =

            if (
                origenDireccion.isNotBlank()
            ) {

                origenDireccion

            } else {

                "Ubicación de origen seleccionada"
            }
    }


    // =========================================================
    // ACTUALIZAR DESTINO
    // =========================================================

    private fun actualizarDestinoPantalla() {

        binding.tvDestino.text =

            if (
                destinoDireccion.isNotBlank()
            ) {

                destinoDireccion

            } else {

                "Destino seleccionado"
            }
    }


    // =========================================================
    // TIPO DE SERVICIO
    // =========================================================

    private fun seleccionarTipoServicio(
        tipo: String
    ) {

        tipoServicio =
            tipo


        if (
            tipo == TIPO_TAXI
        ) {


            // =================================================
            // TAXI SELECCIONADO
            // =================================================

            binding.cardTaxi.strokeWidth =
                dp(2)


            binding.cardTaxi.strokeColor =
                ContextCompat.getColor(
                    this,
                    R.color.pasajes_primary
                )


            binding.tvTaxiSeleccionado.text =
                "Seleccionado"


            binding.tvTaxiSeleccionado.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.pasajes_primary
                )
            )


            // =================================================
            // MOTOTAXI NO SELECCIONADO
            // =================================================

            binding.cardMototaxi.strokeWidth =
                dp(1)


            binding.cardMototaxi.strokeColor =
                ContextCompat.getColor(
                    this,
                    R.color.pasajes_border
                )


            binding.tvMototaxiSeleccionado.text =
                "Tocar para elegir"


            binding.tvMototaxiSeleccionado.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.pasajes_text_secondary
                )
            )


        } else {


            // =================================================
            // MOTOTAXI SELECCIONADO
            // =================================================

            binding.cardMototaxi.strokeWidth =
                dp(2)


            binding.cardMototaxi.strokeColor =
                ContextCompat.getColor(
                    this,
                    R.color.pasajes_primary
                )


            binding.tvMototaxiSeleccionado.text =
                "Seleccionado"


            binding.tvMototaxiSeleccionado.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.pasajes_primary
                )
            )


            // =================================================
            // TAXI NO SELECCIONADO
            // =================================================

            binding.cardTaxi.strokeWidth =
                dp(1)


            binding.cardTaxi.strokeColor =
                ContextCompat.getColor(
                    this,
                    R.color.pasajes_border
                )


            binding.tvTaxiSeleccionado.text =
                "Tocar para elegir"


            binding.tvTaxiSeleccionado.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.pasajes_text_secondary
                )
            )
        }
    }


    // =========================================================
    // VALIDAR Y SOLICITAR
    // =========================================================

    private fun validarYSolicitarOfertas() {


        if (
            solicitudEnProceso
        ) {

            return
        }


        // -----------------------------------------------------
        // ORIGEN
        // -----------------------------------------------------

        if (
            origenLatitud == null ||
            origenLongitud == null
        ) {

            Toast.makeText(
                this,
                "Selecciona tu punto de partida.",
                Toast.LENGTH_SHORT
            ).show()


            abrirConfiguracionCasa()

            return
        }


        // -----------------------------------------------------
        // DESTINO
        // -----------------------------------------------------

        if (
            destinoLatitud == null ||
            destinoLongitud == null
        ) {

            Toast.makeText(
                this,
                "Selecciona primero tu destino.",
                Toast.LENGTH_SHORT
            ).show()


            abrirSelectorDestino()

            return
        }


        // -----------------------------------------------------
        // EVITAR ORIGEN = DESTINO
        // -----------------------------------------------------

        val diferenciaLatitud =
            kotlin.math.abs(
                origenLatitud!! -
                        destinoLatitud!!
            )


        val diferenciaLongitud =
            kotlin.math.abs(
                origenLongitud!! -
                        destinoLongitud!!
            )


        if (
            diferenciaLatitud < 0.00001 &&
            diferenciaLongitud < 0.00001
        ) {

            Toast.makeText(
                this,
                "El origen y el destino no pueden ser el mismo punto.",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        // -----------------------------------------------------
        // CREAR
        // -----------------------------------------------------

        crearSolicitudMovilidad()
    }


    // =========================================================
    // CREAR SOLICITUD EN FIRESTORE
    // =========================================================

    private fun crearSolicitudMovilidad() {

        val usuario =
            auth.currentUser


        if (usuario == null) {

            Toast.makeText(
                this,
                "Tu sesión ha finalizado.",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        val latOrigen =
            origenLatitud
                ?: return


        val lngOrigen =
            origenLongitud
                ?: return


        val latDestino =
            destinoLatitud
                ?: return


        val lngDestino =
            destinoLongitud
                ?: return


        val indicaciones =
            binding.etReferencia.text
                ?.toString()
                ?.trim()
                .orEmpty()


        mostrarSolicitando(
            true
        )


        // =====================================================
        // GENERAMOS ID
        // =====================================================

        val referenciaSolicitud =
            firestore
                .collection(
                    COLECCION_SOLICITUDES
                )
                .document()


        val solicitudId =
            referenciaSolicitud.id


        // =====================================================
        // ORIGEN
        // =====================================================

        val mapaOrigen =
            hashMapOf<String, Any>(

                CAMPO_DIRECCION to
                        origenDireccion,

                CAMPO_LATITUD to
                        latOrigen,

                CAMPO_LONGITUD to
                        lngOrigen,

                CAMPO_REFERENCIA to
                        origenReferencia
            )


        // =====================================================
        // DESTINO
        // =====================================================

        val mapaDestino =
            hashMapOf<String, Any>(

                CAMPO_DIRECCION to
                        destinoDireccion,

                CAMPO_LATITUD to
                        latDestino,

                CAMPO_LONGITUD to
                        lngDestino,

                CAMPO_REFERENCIA to
                        destinoReferencia
            )


        // =====================================================
        // SOLICITUD
        // =====================================================

        val datosSolicitud =
            hashMapOf<String, Any>(


                // ID

                "solicitudId" to
                        solicitudId,


                // Usuario

                "usuarioId" to
                        usuario.uid,

                "nombreUsuario" to
                        nombreUsuario,

                "celularUsuario" to
                        celularUsuario,

                "fotoUsuario" to
                        fotoUsuario,


                // Servicio

                "tipoServicio" to
                        tipoServicio,


                // Ruta

                "origen" to
                        mapaOrigen,

                "destino" to
                        mapaDestino,


                // Indicaciones

                "indicaciones" to
                        indicaciones,


                // Estado inicial

                "estado" to
                        ESTADO_BUSCANDO_OFERTAS,


                // Conductor todavía no elegido

                "conductorSeleccionadoId" to
                        "",

                "ofertaSeleccionadaId" to
                        "",


                // Precio todavía no existe

                "precioAceptado" to
                        0.0,


                // Origen de la solicitud

                "origenSolicitud" to
                        "PASAJESYA",


                // Fechas

                "fechaSolicitud" to
                        FieldValue.serverTimestamp(),

                "fechaActualizacion" to
                        FieldValue.serverTimestamp()
            )


        referenciaSolicitud
            .set(
                datosSolicitud
            )
            .addOnSuccessListener {


                mostrarSolicitando(
                    false
                )


                // =================================================
                // AHORA SÍ:
                // ABRIMOS LA PANTALLA DE OFERTAS
                // =================================================

                abrirPantallaOfertas(
                    solicitudId
                )
            }
            .addOnFailureListener { error ->


                mostrarSolicitando(
                    false
                )


                Toast.makeText(
                    this,
                    "No se pudo enviar la solicitud: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }


    // =========================================================
    // ABRIR OFERTAS
    // =========================================================

    private fun abrirPantallaOfertas(
        solicitudId: String
    ) {

        val intent =
            Intent(
                this,
                OfertasMovilidadActivity::class.java
            ).apply {


                putExtra(
                    OfertasMovilidadActivity
                        .EXTRA_SOLICITUD_ID,
                    solicitudId
                )
            }


        startActivity(
            intent
        )


        // Cerramos esta pantalla para evitar
        // que el usuario vuelva y cree la misma
        // solicitud accidentalmente.

        finish()
    }


    // =========================================================
    // MOSTRAR ESTADO SOLICITANDO
    // =========================================================

    private fun mostrarSolicitando(
        cargando: Boolean
    ) {

        solicitudEnProceso =
            cargando


        binding.progressSolicitar.visibility =

            if (cargando) {

                View.VISIBLE

            } else {

                View.GONE
            }


        binding.btnSolicitarOfertas.isEnabled =
            !cargando


        binding.cardOrigen.isEnabled =
            !cargando


        binding.cardDestino.isEnabled =
            !cargando


        binding.cardTaxi.isEnabled =
            !cargando


        binding.cardMototaxi.isEnabled =
            !cargando


        binding.btnSolicitarOfertas.text =

            if (cargando) {

                ""

            } else {

                "SOLICITAR OFERTAS"
            }
    }


    // =========================================================
    // OBTENER DOUBLE DE RESULTADO
    // =========================================================

    private fun obtenerDoubleExtra(
        intent: Intent,
        clave: String
    ): Double? {

        if (
            !intent.hasExtra(
                clave
            )
        ) {

            return null
        }


        val valor =
            intent.getDoubleExtra(
                clave,
                Double.NaN
            )


        return if (
            valor.isNaN()
        ) {

            null

        } else {

            valor
        }
    }


    // =========================================================
    // DP A PIXELES
    // =========================================================

    private fun dp(
        valor: Int
    ): Int {

        return (
                valor *
                        resources
                            .displayMetrics
                            .density
                )
            .toInt()
    }


    // =========================================================
    // CONSTANTES
    // =========================================================

    companion object {


        // =====================================================
        // FIRESTORE
        // =====================================================

        private const val COLECCION_USUARIOS =
            "usuariospasajes"


        private const val COLECCION_SOLICITUDES =
            "solicitudesMovilidad"


        // =====================================================
        // USUARIO
        // =====================================================

        private const val CAMPO_NOMBRES =
            "nombres"


        private const val CAMPO_APELLIDOS =
            "apellidos"


        private const val CAMPO_CELULAR =
            "celular"


        private const val CAMPO_FOTO_URL =
            "fotoUrl"


        // =====================================================
        // UBICACIÓN
        // =====================================================

        private const val CAMPO_UBICACION_CASA =
            "ubicacionCasa"


        private const val CAMPO_DIRECCION =
            "direccion"


        private const val CAMPO_LATITUD =
            "latitud"


        private const val CAMPO_LONGITUD =
            "longitud"


        private const val CAMPO_REFERENCIA =
            "referencia"


        // =====================================================
        // TIPO DE SERVICIO
        // =====================================================

        private const val TIPO_TAXI =
            "TAXI"


        private const val TIPO_MOTOTAXI =
            "MOTOTAXI"


        // =====================================================
        // ESTADOS
        // =====================================================

        private const val ESTADO_BUSCANDO_OFERTAS =
            "BUSCANDO_OFERTAS"
    }
}