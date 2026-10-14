package com.PasajesYa.pasajesya

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.PasajesYa.pasajesya.databinding.ActivitySeleccionarUbicacionBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.Locale
import java.util.concurrent.Executors


class SeleccionarUbicacionActivity :
    AppCompatActivity(),
    OnMapReadyCallback {


    // =========================================================
    // VIEW BINDING
    // =========================================================

    private lateinit var binding: ActivitySeleccionarUbicacionBinding


    // =========================================================
    // GOOGLE MAP
    // =========================================================

    private var googleMap: GoogleMap? = null

    private var ubicacionSeleccionada: LatLng? = null


    // =========================================================
    // FIREBASE
    // =========================================================

    private lateinit var auth: FirebaseAuth

    private lateinit var firestore: FirebaseFirestore


    // =========================================================
    // GPS
    // =========================================================

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }


    // =========================================================
    // GEOCODER
    // =========================================================

    private val geocoderExecutor =
        Executors.newSingleThreadExecutor()


    private var numeroSolicitudDireccion = 0

    private var direccionSeleccionada = ""


    // =========================================================
    // TIPO DE UBICACIÓN
    // CASA
    // TRABAJO
    // ORIGEN
    // DESTINO
    // =========================================================

    private var tipoUbicacion = TIPO_CASA


    // =========================================================
    // DESDE DÓNDE SE ABRIÓ
    // =========================================================

    private var origenUbicacion = ORIGEN_PERFIL


    // =========================================================
    // PERMISOS
    // =========================================================

    private val solicitarPermisoUbicacion =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permisos ->

            val permisoFino =
                permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true

            val permisoAproximado =
                permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true


            if (permisoFino || permisoAproximado) {

                habilitarCapaMiUbicacion()

                obtenerUbicacionActual()

            } else {

                Toast.makeText(
                    this,
                    "Necesitamos permiso de ubicación para usar tu posición actual.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }


    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)


        binding =
            ActivitySeleccionarUbicacionBinding.inflate(
                layoutInflater
            )

        setContentView(binding.root)


        // Firebase

        auth =
            FirebaseAuth.getInstance()

        firestore =
            FirebaseFirestore.getInstance()


        // Tipo recibido

        tipoUbicacion =
            intent
                .getStringExtra(EXTRA_TIPO_UBICACION)
                ?.uppercase(Locale.ROOT)
                ?: TIPO_CASA


        // Origen de la pantalla

        origenUbicacion =
            intent
                .getStringExtra(EXTRA_ORIGEN_UBICACION)
                ?: ORIGEN_PERFIL


        validarTipoUbicacion()

        configurarPantalla()

        configurarEventos()

        iniciarMapa()
    }


    // =========================================================
    // VALIDAR TIPO
    // =========================================================

    private fun validarTipoUbicacion() {

        tipoUbicacion =

            when (tipoUbicacion) {

                TIPO_CASA ->
                    TIPO_CASA

                TIPO_TRABAJO ->
                    TIPO_TRABAJO

                TIPO_ORIGEN ->
                    TIPO_ORIGEN

                TIPO_DESTINO ->
                    TIPO_DESTINO

                else ->
                    TIPO_CASA
            }
    }


    // =========================================================
    // CONFIGURAR PANTALLA
    // =========================================================

    private fun configurarPantalla() {

        when (tipoUbicacion) {


            // -------------------------------------------------
            // CASA
            // -------------------------------------------------

            TIPO_CASA -> {

                binding.tvTituloUbicacion.text =
                    "Ubicación de mi casa"

                binding.tvSubtituloUbicacion.text =
                    "Mueve el mapa hasta ubicar tu casa"

                binding.tvMensajeUbicacion.text =
                    "Esta será tu ubicación principal"

                binding.tilReferencia.hint =
                    "Ej. Casa azul frente al colegio"

                binding.btnGuardarUbicacion.text =
                    "GUARDAR UBICACIÓN"
            }


            // -------------------------------------------------
            // TRABAJO
            // -------------------------------------------------

            TIPO_TRABAJO -> {

                binding.tvTituloUbicacion.text =
                    "Ubicación de mi trabajo"

                binding.tvSubtituloUbicacion.text =
                    "Mueve el mapa hasta ubicar tu centro de trabajo"

                binding.tvMensajeUbicacion.text =
                    "Esta ubicación es opcional"

                binding.tilReferencia.hint =
                    "Ej. Oficina al costado del banco"

                binding.btnGuardarUbicacion.text =
                    "GUARDAR UBICACIÓN"
            }


            // -------------------------------------------------
            // ORIGEN
            // -------------------------------------------------

            TIPO_ORIGEN -> {

                binding.tvTituloUbicacion.text =
                    "Punto de partida"

                binding.tvSubtituloUbicacion.text =
                    "Selecciona dónde deseas que te recojan"

                binding.tvMensajeUbicacion.text =
                    "Este punto solo se utilizará para este viaje"

                binding.tilReferencia.hint =
                    "Ej. Estoy frente a la municipalidad"

                binding.btnGuardarUbicacion.text =
                    "USAR COMO ORIGEN"
            }


            // -------------------------------------------------
            // DESTINO
            // -------------------------------------------------

            TIPO_DESTINO -> {

                binding.tvTituloUbicacion.text =
                    "Seleccionar destino"

                binding.tvSubtituloUbicacion.text =
                    "Mueve el mapa hasta el lugar donde quieres ir"

                binding.tvMensajeUbicacion.text =
                    "Este destino solo se utilizará para este viaje"

                binding.tilReferencia.hint =
                    "Referencia del destino (opcional)"

                binding.btnGuardarUbicacion.text =
                    "USAR COMO DESTINO"
            }
        }
    }


    // =========================================================
    // EVENTOS
    // =========================================================

    private fun configurarEventos() {


        // Volver

        binding.btnVolverUbicacion.setOnClickListener {

            finish()
        }


        // GPS

        binding.btnMiUbicacion.setOnClickListener {

            verificarPermisoUbicacion()
        }


        // Guardar / seleccionar

        binding.btnGuardarUbicacion.setOnClickListener {

            procesarUbicacionSeleccionada()
        }
    }


    // =========================================================
    // INICIAR MAPA
    // =========================================================

    private fun iniciarMapa() {

        binding.progressMapa.visibility =
            View.VISIBLE


        val mapFragment =
            supportFragmentManager.findFragmentById(
                R.id.mapFragment
            ) as? SupportMapFragment


        if (mapFragment == null) {

            binding.progressMapa.visibility =
                View.GONE

            Toast.makeText(
                this,
                "No se pudo iniciar Google Maps.",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        mapFragment.getMapAsync(this)
    }


    // =========================================================
    // MAPA LISTO
    // =========================================================

    override fun onMapReady(map: GoogleMap) {

        googleMap = map


        binding.progressMapa.visibility =
            View.GONE


        // Controles

        map.uiSettings.isCompassEnabled =
            true

        map.uiSettings.isZoomControlsEnabled =
            false

        map.uiSettings.isMapToolbarEnabled =
            false

        map.uiSettings.isRotateGesturesEnabled =
            true

        map.uiSettings.isTiltGesturesEnabled =
            true

        map.uiSettings.isZoomGesturesEnabled =
            true

        map.uiSettings.isScrollGesturesEnabled =
            true


        habilitarCapaMiUbicacion()


        // -----------------------------------------------------
        // CUANDO EL USUARIO TERMINA DE MOVER EL MAPA
        // -----------------------------------------------------

        map.setOnCameraIdleListener {

            val centro =
                map.cameraPosition.target


            ubicacionSeleccionada =
                centro


            mostrarCoordenadas(
                centro
            )


            obtenerDireccion(
                centro
            )
        }


        // Punto inicial

        cargarPuntoInicial()
    }


    // =========================================================
    // PUNTO INICIAL
    // =========================================================

    private fun cargarPuntoInicial() {

        when (tipoUbicacion) {

            TIPO_CASA,
            TIPO_TRABAJO -> {

                cargarUbicacionGuardada()
            }


            TIPO_ORIGEN,
            TIPO_DESTINO -> {

                cargarPuntoInicialTemporal()
            }
        }
    }


    // =========================================================
    // CASA / TRABAJO YA GUARDADOS
    // =========================================================

    private fun cargarUbicacionGuardada() {

        val usuario =
            auth.currentUser


        if (usuario == null) {

            Toast.makeText(
                this,
                "No se encontró la sesión del usuario.",
                Toast.LENGTH_LONG
            ).show()

            finish()

            return
        }


        val campoUbicacion =

            if (tipoUbicacion == TIPO_TRABAJO) {

                CAMPO_UBICACION_TRABAJO

            } else {

                CAMPO_UBICACION_CASA
            }


        firestore
            .collection(COLECCION_USUARIOS)
            .document(usuario.uid)
            .get()
            .addOnSuccessListener { documento ->


                val mapaUbicacion =
                    documento.get(campoUbicacion)
                            as? Map<*, *>


                val latitud =
                    (mapaUbicacion
                        ?.get(CAMPO_LATITUD) as? Number)
                        ?.toDouble()


                val longitud =
                    (mapaUbicacion
                        ?.get(CAMPO_LONGITUD) as? Number)
                        ?.toDouble()


                val direccion =
                    mapaUbicacion
                        ?.get(CAMPO_DIRECCION)
                        ?.toString()
                        .orEmpty()


                val referencia =
                    mapaUbicacion
                        ?.get(CAMPO_REFERENCIA)
                        ?.toString()
                        .orEmpty()


                if (
                    latitud != null &&
                    longitud != null
                ) {

                    val posicion =
                        LatLng(
                            latitud,
                            longitud
                        )


                    ubicacionSeleccionada =
                        posicion


                    direccionSeleccionada =
                        direccion


                    if (direccion.isNotBlank()) {

                        binding
                            .tvDireccionSeleccionada
                            .text =
                            direccion
                    }


                    binding.etReferencia.setText(
                        referencia
                    )


                    mostrarCoordenadas(
                        posicion
                    )


                    googleMap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            posicion,
                            ZOOM_UBICACION
                        )
                    )


                } else {

                    centrarEnAplao()
                }
            }
            .addOnFailureListener {

                centrarEnAplao()
            }
    }


    // =========================================================
    // PUNTO INICIAL PARA ORIGEN / DESTINO
    // =========================================================

    private fun cargarPuntoInicialTemporal() {


        // -----------------------------------------------------
        // PRIMERO REVISAMOS SI MovilidadDisponibleActivity
        // NOS ENVIÓ UN PUNTO INICIAL
        // -----------------------------------------------------

        val latitudInicial =
            intent.getDoubleExtra(
                EXTRA_LATITUD_INICIAL,
                Double.NaN
            )


        val longitudInicial =
            intent.getDoubleExtra(
                EXTRA_LONGITUD_INICIAL,
                Double.NaN
            )


        val direccionInicial =
            intent.getStringExtra(
                EXTRA_DIRECCION_INICIAL
            ).orEmpty()


        if (
            !latitudInicial.isNaN() &&
            !longitudInicial.isNaN()
        ) {

            val posicion =
                LatLng(
                    latitudInicial,
                    longitudInicial
                )


            ubicacionSeleccionada =
                posicion


            if (direccionInicial.isNotBlank()) {

                direccionSeleccionada =
                    direccionInicial

                binding
                    .tvDireccionSeleccionada
                    .text =
                    direccionInicial
            }


            mostrarCoordenadas(
                posicion
            )


            googleMap?.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    posicion,
                    ZOOM_UBICACION
                )
            )


            return
        }


        // -----------------------------------------------------
        // SI NO RECIBIMOS PUNTO:
        // USAMOS CASA COMO REFERENCIA INICIAL
        // -----------------------------------------------------

        cargarCasaComoPuntoInicial()
    }


    // =========================================================
    // CASA COMO CENTRO INICIAL
    // PARA MOVILIDAD
    // =========================================================

    private fun cargarCasaComoPuntoInicial() {

        val usuario =
            auth.currentUser


        if (usuario == null) {

            centrarEnAplao()

            return
        }


        firestore
            .collection(COLECCION_USUARIOS)
            .document(usuario.uid)
            .get()
            .addOnSuccessListener { documento ->


                val ubicacionCasa =
                    documento.get(
                        CAMPO_UBICACION_CASA
                    ) as? Map<*, *>


                val latitud =
                    (ubicacionCasa
                        ?.get(CAMPO_LATITUD) as? Number)
                        ?.toDouble()


                val longitud =
                    (ubicacionCasa
                        ?.get(CAMPO_LONGITUD) as? Number)
                        ?.toDouble()


                val direccion =
                    ubicacionCasa
                        ?.get(CAMPO_DIRECCION)
                        ?.toString()
                        .orEmpty()


                if (
                    latitud != null &&
                    longitud != null
                ) {

                    val posicion =
                        LatLng(
                            latitud,
                            longitud
                        )


                    ubicacionSeleccionada =
                        posicion


                    if (direccion.isNotBlank()) {

                        direccionSeleccionada =
                            direccion

                        binding
                            .tvDireccionSeleccionada
                            .text =
                            direccion
                    }


                    mostrarCoordenadas(
                        posicion
                    )


                    googleMap?.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            posicion,
                            ZOOM_UBICACION
                        )
                    )


                } else {

                    centrarEnAplao()
                }
            }
            .addOnFailureListener {

                centrarEnAplao()
            }
    }


    // =========================================================
    // APLAO COMO RESPALDO
    // =========================================================

    private fun centrarEnAplao() {

        val aplao =
            LatLng(
                LATITUD_APLAO,
                LONGITUD_APLAO
            )


        ubicacionSeleccionada =
            aplao


        googleMap?.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                aplao,
                ZOOM_INICIAL
            )
        )
    }


    // =========================================================
    // VERIFICAR PERMISO GPS
    // =========================================================

    private fun verificarPermisoUbicacion() {

        val permisoFino =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED


        val permisoAproximado =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED


        if (
            permisoFino ||
            permisoAproximado
        ) {

            habilitarCapaMiUbicacion()

            obtenerUbicacionActual()

        } else {

            solicitarPermisoUbicacion.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }


    // =========================================================
    // CAPA AZUL DE GOOGLE MAPS
    // =========================================================

    private fun habilitarCapaMiUbicacion() {

        val permisoFino =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED


        val permisoAproximado =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED


        if (
            !permisoFino &&
            !permisoAproximado
        ) {

            return
        }


        try {

            googleMap?.isMyLocationEnabled =
                true

        } catch (_: SecurityException) {

        }
    }


    // =========================================================
    // UBICACIÓN ACTUAL
    // =========================================================

    private fun obtenerUbicacionActual() {

        val permisoFino =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED


        val permisoAproximado =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED


        if (
            !permisoFino &&
            !permisoAproximado
        ) {

            verificarPermisoUbicacion()

            return
        }


        binding.btnMiUbicacion.isEnabled =
            false


        binding.btnMiUbicacion.text =
            "BUSCANDO UBICACIÓN..."


        try {

            val cancellationTokenSource =
                CancellationTokenSource()


            fusedLocationClient
                .getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                )
                .addOnSuccessListener { location ->


                    restaurarBotonMiUbicacion()


                    if (location != null) {

                        val posicion =
                            LatLng(
                                location.latitude,
                                location.longitude
                            )


                        ubicacionSeleccionada =
                            posicion


                        googleMap?.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                posicion,
                                ZOOM_UBICACION
                            )
                        )


                    } else {

                        obtenerUltimaUbicacion()
                    }
                }
                .addOnFailureListener {

                    restaurarBotonMiUbicacion()

                    obtenerUltimaUbicacion()
                }


        } catch (_: SecurityException) {

            restaurarBotonMiUbicacion()


            Toast.makeText(
                this,
                "No fue posible acceder a tu ubicación.",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    // =========================================================
    // ÚLTIMA UBICACIÓN COMO RESPALDO
    // =========================================================

    private fun obtenerUltimaUbicacion() {

        val permisoFino =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED


        val permisoAproximado =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED


        if (
            !permisoFino &&
            !permisoAproximado
        ) {

            return
        }


        try {

            fusedLocationClient
                .lastLocation
                .addOnSuccessListener { location ->


                    if (location != null) {

                        val posicion =
                            LatLng(
                                location.latitude,
                                location.longitude
                            )


                        ubicacionSeleccionada =
                            posicion


                        googleMap?.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                posicion,
                                ZOOM_UBICACION
                            )
                        )


                    } else {

                        Toast.makeText(
                            this,
                            "No pudimos obtener tu ubicación. Activa el GPS e inténtalo nuevamente.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }


        } catch (_: SecurityException) {

        }
    }


    // =========================================================
    // RESTAURAR BOTÓN GPS
    // =========================================================

    private fun restaurarBotonMiUbicacion() {

        binding.btnMiUbicacion.isEnabled =
            true


        binding.btnMiUbicacion.text =
            "📍  USAR MI UBICACIÓN ACTUAL"
    }


    // =========================================================
    // COORDENADAS
    // =========================================================

    private fun mostrarCoordenadas(
        posicion: LatLng
    ) {

        binding.tvCoordenadas.text =
            String.format(
                Locale.US,
                "%.6f, %.6f",
                posicion.latitude,
                posicion.longitude
            )
    }


    // =========================================================
    // OBTENER DIRECCIÓN
    // =========================================================

    private fun obtenerDireccion(
        posicion: LatLng
    ) {

        numeroSolicitudDireccion++


        val solicitudActual =
            numeroSolicitudDireccion


        binding.progressDireccion.visibility =
            View.VISIBLE


        binding.tvDireccionSeleccionada.text =
            "Buscando dirección..."


        val geocoder =
            Geocoder(
                this,
                Locale("es", "PE")
            )


        // -----------------------------------------------------
        // ANDROID 13+
        // -----------------------------------------------------

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            try {

                geocoder.getFromLocation(
                    posicion.latitude,
                    posicion.longitude,
                    1
                ) { direcciones ->


                    runOnUiThread {

                        if (
                            solicitudActual !=
                            numeroSolicitudDireccion
                        ) {

                            return@runOnUiThread
                        }


                        val direccion =
                            direcciones
                                .firstOrNull()
                                ?.getAddressLine(0)


                        mostrarDireccionObtenida(
                            direccion,
                            posicion
                        )
                    }
                }


            } catch (_: Exception) {

                mostrarDireccionObtenida(
                    null,
                    posicion
                )
            }


        } else {


            // -------------------------------------------------
            // ANDROID 12 O ANTERIOR
            // -------------------------------------------------

            geocoderExecutor.execute {

                val direccion =

                    try {

                        @Suppress("DEPRECATION")
                        geocoder
                            .getFromLocation(
                                posicion.latitude,
                                posicion.longitude,
                                1
                            )
                            ?.firstOrNull()
                            ?.getAddressLine(0)


                    } catch (_: Exception) {

                        null
                    }


                runOnUiThread {

                    if (
                        solicitudActual !=
                        numeroSolicitudDireccion
                    ) {

                        return@runOnUiThread
                    }


                    mostrarDireccionObtenida(
                        direccion,
                        posicion
                    )
                }
            }
        }
    }


    // =========================================================
    // MOSTRAR DIRECCIÓN
    // =========================================================

    private fun mostrarDireccionObtenida(
        direccion: String?,
        posicion: LatLng
    ) {

        binding.progressDireccion.visibility =
            View.GONE


        direccionSeleccionada =

            if (
                !direccion.isNullOrBlank()
            ) {

                direccion

            } else {

                String.format(
                    Locale.US,
                    "Ubicación seleccionada (%.6f, %.6f)",
                    posicion.latitude,
                    posicion.longitude
                )
            }


        binding.tvDireccionSeleccionada.text =
            direccionSeleccionada
    }


    // =========================================================
    // PROCESAR BOTÓN PRINCIPAL
    // =========================================================

    private fun procesarUbicacionSeleccionada() {

        when (tipoUbicacion) {


            // CASA / TRABAJO SE GUARDAN

            TIPO_CASA,
            TIPO_TRABAJO -> {

                guardarUbicacionPerfil()
            }


            // ORIGEN / DESTINO SOLO SE DEVUELVEN

            TIPO_ORIGEN,
            TIPO_DESTINO -> {

                devolverUbicacionTemporal()
            }
        }
    }


    // =========================================================
    // GUARDAR CASA / TRABAJO EN FIRESTORE
    // =========================================================

    private fun guardarUbicacionPerfil() {

        val usuario =
            auth.currentUser


        if (usuario == null) {

            Toast.makeText(
                this,
                "Tu sesión ha finalizado. Ingresa nuevamente.",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        val posicion =
            ubicacionSeleccionada


        if (posicion == null) {

            Toast.makeText(
                this,
                "Selecciona primero una ubicación.",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        val referencia =
            binding.etReferencia.text
                ?.toString()
                ?.trim()
                .orEmpty()


        val direccion =
            obtenerDireccionParaGuardar(
                posicion
            )


        val campoUbicacion =

            if (
                tipoUbicacion ==
                TIPO_TRABAJO
            ) {

                CAMPO_UBICACION_TRABAJO

            } else {

                CAMPO_UBICACION_CASA
            }


        val datosUbicacion =
            hashMapOf<String, Any>(

                CAMPO_DIRECCION to
                        direccion,

                CAMPO_LATITUD to
                        posicion.latitude,

                CAMPO_LONGITUD to
                        posicion.longitude,

                CAMPO_REFERENCIA to
                        referencia,

                CAMPO_FECHA_ACTUALIZACION to
                        FieldValue.serverTimestamp()
            )


        val datosUsuario =
            hashMapOf<String, Any>(

                campoUbicacion to
                        datosUbicacion
            )


        mostrarProcesando(
            true
        )


        firestore
            .collection(COLECCION_USUARIOS)
            .document(usuario.uid)
            .set(
                datosUsuario,
                SetOptions.merge()
            )
            .addOnSuccessListener {


                mostrarProcesando(
                    false
                )


                val nombreTipo =

                    if (
                        tipoUbicacion ==
                        TIPO_TRABAJO
                    ) {

                        "trabajo"

                    } else {

                        "casa"
                    }


                Toast.makeText(
                    this,
                    "Ubicación de $nombreTipo guardada correctamente.",
                    Toast.LENGTH_SHORT
                ).show()


                devolverResultado(
                    posicion = posicion,
                    direccion = direccion,
                    referencia = referencia
                )
            }
            .addOnFailureListener { error ->


                mostrarProcesando(
                    false
                )


                Toast.makeText(
                    this,
                    "No se pudo guardar la ubicación: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }


    // =========================================================
    // ORIGEN / DESTINO
    // NO GUARDA EN FIRESTORE
    // =========================================================

    private fun devolverUbicacionTemporal() {

        val posicion =
            ubicacionSeleccionada


        if (posicion == null) {

            Toast.makeText(
                this,
                "Selecciona un punto en el mapa.",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        val direccion =
            obtenerDireccionParaGuardar(
                posicion
            )


        val referencia =
            binding.etReferencia.text
                ?.toString()
                ?.trim()
                .orEmpty()


        devolverResultado(
            posicion = posicion,
            direccion = direccion,
            referencia = referencia
        )
    }


    // =========================================================
    // DIRECCIÓN FINAL
    // =========================================================

    private fun obtenerDireccionParaGuardar(
        posicion: LatLng
    ): String {

        val direccion =
            direccionSeleccionada.trim()


        if (
            direccion.isNotBlank() &&
            direccion != "Buscando dirección..."
        ) {

            return direccion
        }


        return String.format(
            Locale.US,
            "Ubicación seleccionada (%.6f, %.6f)",
            posicion.latitude,
            posicion.longitude
        )
    }


    // =========================================================
    // DEVOLVER RESULTADO
    // =========================================================

    private fun devolverResultado(
        posicion: LatLng,
        direccion: String,
        referencia: String
    ) {

        val resultado =
            Intent().apply {


                // Tipo

                putExtra(
                    EXTRA_TIPO_UBICACION,
                    tipoUbicacion
                )


                // Dirección

                putExtra(
                    EXTRA_DIRECCION_RESULTADO,
                    direccion
                )


                // Latitud

                putExtra(
                    EXTRA_LATITUD_RESULTADO,
                    posicion.latitude
                )


                // Longitud

                putExtra(
                    EXTRA_LONGITUD_RESULTADO,
                    posicion.longitude
                )


                // Referencia

                putExtra(
                    EXTRA_REFERENCIA_RESULTADO,
                    referencia
                )


                // De dónde vino

                putExtra(
                    EXTRA_ORIGEN_UBICACION,
                    origenUbicacion
                )
            }


        setResult(
            Activity.RESULT_OK,
            resultado
        )


        finish()
    }


    // =========================================================
    // MOSTRAR PROCESANDO
    // =========================================================

    private fun mostrarProcesando(
        procesando: Boolean
    ) {

        binding.progressGuardarUbicacion.visibility =

            if (procesando) {

                View.VISIBLE

            } else {

                View.GONE
            }


        binding.btnGuardarUbicacion.isEnabled =
            !procesando


        if (procesando) {

            binding.btnGuardarUbicacion.text =
                ""

        } else {

            configurarTextoBotonPrincipal()
        }
    }


    // =========================================================
    // RESTAURAR TEXTO BOTÓN
    // =========================================================

    private fun configurarTextoBotonPrincipal() {

        binding.btnGuardarUbicacion.text =

            when (tipoUbicacion) {

                TIPO_CASA,
                TIPO_TRABAJO ->
                    "GUARDAR UBICACIÓN"

                TIPO_ORIGEN ->
                    "USAR COMO ORIGEN"

                TIPO_DESTINO ->
                    "USAR COMO DESTINO"

                else ->
                    "GUARDAR UBICACIÓN"
            }
    }


    // =========================================================
    // ON DESTROY
    // =========================================================

    override fun onDestroy() {

        super.onDestroy()


        try {

            geocoderExecutor.shutdownNow()

        } catch (_: Exception) {

        }
    }


    // =========================================================
    // CONSTANTES
    // =========================================================

    companion object {


        // =====================================================
        // TIPOS DE UBICACIÓN
        // =====================================================

        const val TIPO_CASA =
            "CASA"

        const val TIPO_TRABAJO =
            "TRABAJO"

        const val TIPO_ORIGEN =
            "ORIGEN"

        const val TIPO_DESTINO =
            "DESTINO"


        // =====================================================
        // ORIGEN DE LA PANTALLA
        // =====================================================

        const val ORIGEN_PERFIL =
            "PERFIL"

        const val ORIGEN_MOVILIDAD =
            "MOVILIDAD"

        const val ORIGEN_ENCOMIENDA =
            "ENCOMIENDA"

        const val ORIGEN_MANDADO =
            "MANDADO"


        // =====================================================
        // EXTRAS DE ENTRADA
        // =====================================================

        const val EXTRA_TIPO_UBICACION =
            "tipoUbicacion"

        const val EXTRA_ORIGEN_UBICACION =
            "origenUbicacion"


        // Punto inicial opcional

        const val EXTRA_LATITUD_INICIAL =
            "latitudInicial"

        const val EXTRA_LONGITUD_INICIAL =
            "longitudInicial"

        const val EXTRA_DIRECCION_INICIAL =
            "direccionInicial"


        // =====================================================
        // RESULTADOS
        // =====================================================

        const val EXTRA_DIRECCION_RESULTADO =
            "direccionUbicacion"

        const val EXTRA_LATITUD_RESULTADO =
            "latitudUbicacion"

        const val EXTRA_LONGITUD_RESULTADO =
            "longitudUbicacion"

        const val EXTRA_REFERENCIA_RESULTADO =
            "referenciaUbicacion"


        // =====================================================
        // FIRESTORE
        // =====================================================

        private const val COLECCION_USUARIOS =
            "usuariospasajes"


        private const val CAMPO_UBICACION_CASA =
            "ubicacionCasa"

        private const val CAMPO_UBICACION_TRABAJO =
            "ubicacionTrabajo"


        private const val CAMPO_DIRECCION =
            "direccion"

        private const val CAMPO_LATITUD =
            "latitud"

        private const val CAMPO_LONGITUD =
            "longitud"

        private const val CAMPO_REFERENCIA =
            "referencia"

        private const val CAMPO_FECHA_ACTUALIZACION =
            "fechaActualizacion"


        // =====================================================
        // MAPA
        // =====================================================

        private const val ZOOM_INICIAL =
            15f

        private const val ZOOM_UBICACION =
            17f


        // Aplao como respaldo

        private const val LATITUD_APLAO =
            -16.0765

        private const val LONGITUD_APLAO =
            -72.4923
    }
}