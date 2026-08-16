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
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale


class SeleccionarUbicacionActivity :
    AppCompatActivity(),
    OnMapReadyCallback {


    // =========================================================
    // VIEW BINDING
    // =========================================================

    private lateinit var binding: ActivitySeleccionarUbicacionBinding


    // =========================================================
    // GOOGLE MAPS
    // =========================================================

    private var googleMap: GoogleMap? = null

    private var ubicacionSeleccionada: LatLng? = null


    // =========================================================
    // FIREBASE
    // =========================================================

    private lateinit var auth: FirebaseAuth

    private lateinit var firestore: FirebaseFirestore


    // =========================================================
    // UBICACIÓN DEL CELULAR
    // =========================================================

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }


    // =========================================================
    // CASA O TRABAJO
    // =========================================================

    private var tipoUbicacion = TIPO_CASA

    private var origenUbicacion = ORIGEN_PERFIL


    // =========================================================
    // DIRECCIÓN
    // =========================================================

    private var direccionSeleccionada = ""

    private var numeroSolicitudDireccion = 0


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
            ActivitySeleccionarUbicacionBinding.inflate(layoutInflater)

        setContentView(binding.root)


        // Firebase

        auth = FirebaseAuth.getInstance()

        firestore = FirebaseFirestore.getInstance()


        // Leer datos recibidos

        tipoUbicacion =
            intent.getStringExtra(EXTRA_TIPO_UBICACION)
                ?.uppercase()
                ?: TIPO_CASA


        origenUbicacion =
            intent.getStringExtra(EXTRA_ORIGEN_UBICACION)
                ?: ORIGEN_PERFIL


        configurarPantalla()

        configurarEventos()

        iniciarMapa()
    }


    // =========================================================
    // CONFIGURAR PANTALLA
    // =========================================================

    private fun configurarPantalla() {

        if (tipoUbicacion == TIPO_TRABAJO) {

            binding.tvTituloUbicacion.text =
                "Ubicación de mi trabajo"

            binding.tvSubtituloUbicacion.text =
                "Mueve el mapa hasta ubicar tu centro de trabajo"

            binding.tvMensajeUbicacion.text =
                "Esta ubicación es opcional y se guardará en tu perfil"

        } else {

            tipoUbicacion = TIPO_CASA

            binding.tvTituloUbicacion.text =
                "Ubicación de mi casa"

            binding.tvSubtituloUbicacion.text =
                "Mueve el mapa y coloca el punto donde corresponde"

            binding.tvMensajeUbicacion.text =
                "Esta será tu ubicación principal"
        }
    }


    // =========================================================
    // EVENTOS
    // =========================================================

    private fun configurarEventos() {

        binding.btnVolverUbicacion.setOnClickListener {

            finish()
        }


        binding.btnMiUbicacion.setOnClickListener {

            verificarPermisoUbicacion()
        }


        binding.btnGuardarUbicacion.setOnClickListener {

            guardarUbicacion()
        }
    }


    // =========================================================
    // INICIAR GOOGLE MAP
    // =========================================================

    private fun iniciarMapa() {

        binding.progressMapa.visibility = View.VISIBLE


        val mapFragment =
            supportFragmentManager.findFragmentById(
                R.id.mapFragment
            ) as? SupportMapFragment


        if (mapFragment == null) {

            binding.progressMapa.visibility = View.GONE

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

        binding.progressMapa.visibility = View.GONE


        // Configuración del mapa

        map.uiSettings.isCompassEnabled = true

        map.uiSettings.isZoomControlsEnabled = false

        map.uiSettings.isMapToolbarEnabled = false


        habilitarCapaMiUbicacion()


        // Cuando el usuario deja de mover el mapa

        map.setOnCameraIdleListener {

            val centro =
                map.cameraPosition.target

            ubicacionSeleccionada = centro

            mostrarCoordenadas(centro)

            obtenerDireccion(centro)
        }


        // Primero buscamos si el usuario ya tenía
        // una ubicación guardada.

        cargarUbicacionGuardada()
    }


    // =========================================================
    // CARGAR UBICACIÓN YA GUARDADA
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
                    documento.get(campoUbicacion) as? Map<*, *>


                val latitud =
                    (mapaUbicacion?.get(CAMPO_LATITUD) as? Number)
                        ?.toDouble()


                val longitud =
                    (mapaUbicacion?.get(CAMPO_LONGITUD) as? Number)
                        ?.toDouble()


                val direccion =
                    mapaUbicacion?.get(CAMPO_DIRECCION)
                        ?.toString()
                        .orEmpty()


                val referencia =
                    mapaUbicacion?.get(CAMPO_REFERENCIA)
                        ?.toString()
                        .orEmpty()


                // Si ya hay ubicación guardada

                if (latitud != null && longitud != null) {

                    val posicion =
                        LatLng(
                            latitud,
                            longitud
                        )


                    ubicacionSeleccionada = posicion


                    if (direccion.isNotBlank()) {

                        direccionSeleccionada =
                            direccion

                        binding.tvDireccionSeleccionada.text =
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

                    // Si aún no tiene casa/trabajo guardado,
                    // mostramos Aplao como zona inicial.
                    //
                    // El usuario puede mover el mapa o tocar
                    // "USAR MI UBICACIÓN ACTUAL".

                    val aplao =
                        LatLng(
                            -16.0765,
                            -72.4923
                        )


                    googleMap?.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            aplao,
                            ZOOM_INICIAL
                        )
                    )
                }
            }
            .addOnFailureListener {

                val aplao =
                    LatLng(
                        -16.0765,
                        -72.4923
                    )


                googleMap?.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        aplao,
                        ZOOM_INICIAL
                    )
                )
            }
    }


    // =========================================================
    // VERIFICAR PERMISO
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


        if (permisoFino || permisoAproximado) {

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
    // MOSTRAR PUNTO AZUL DE UBICACIÓN
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


        if (!permisoFino && !permisoAproximado) {

            return
        }


        try {

            googleMap?.isMyLocationEnabled = true

        } catch (_: SecurityException) {

        }
    }


    // =========================================================
    // OBTENER UBICACIÓN ACTUAL DEL CELULAR
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


        if (!permisoFino && !permisoAproximado) {

            verificarPermisoUbicacion()

            return
        }


        binding.btnMiUbicacion.isEnabled = false

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
                "No fue posible acceder a la ubicación.",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    // =========================================================
    // UBICACIÓN ANTERIOR COMO RESPALDO
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


        if (!permisoFino && !permisoAproximado) {

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
                            "No pudimos obtener tu ubicación. Activa el GPS y vuelve a intentarlo.",
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

        binding.btnMiUbicacion.isEnabled = true

        binding.btnMiUbicacion.text =
            "📍  USAR MI UBICACIÓN ACTUAL"
    }


    // =========================================================
    // MOSTRAR COORDENADAS
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
    // OBTENER DIRECCIÓN DESDE LATITUD Y LONGITUD
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


        // Android 13 o superior

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            geocoder.getFromLocation(
                posicion.latitude,
                posicion.longitude,
                1
            ) { direcciones ->


                runOnUiThread {

                    if (solicitudActual != numeroSolicitudDireccion) {

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

        } else {

            // Android 12 o inferior

            lifecycleScope.launch(
                Dispatchers.IO
            ) {

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


                withContext(
                    Dispatchers.Main
                ) {

                    if (solicitudActual != numeroSolicitudDireccion) {

                        return@withContext
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
    // MOSTRAR DIRECCIÓN EN PANTALLA
    // =========================================================

    private fun mostrarDireccionObtenida(
        direccion: String?,
        posicion: LatLng
    ) {

        binding.progressDireccion.visibility =
            View.GONE


        direccionSeleccionada =

            if (!direccion.isNullOrBlank()) {

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
    // GUARDAR UBICACIÓN EN FIRESTORE
    // =========================================================

    private fun guardarUbicacion() {

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
                "Selecciona primero una ubicación en el mapa.",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        val referencia =
            binding.etReferencia.text
                ?.toString()
                ?.trim()
                .orEmpty()


        var direccion =
            direccionSeleccionada.trim()


        if (direccion.isBlank()) {

            direccion =
                String.format(
                    Locale.US,
                    "Ubicación seleccionada (%.6f, %.6f)",
                    posicion.latitude,
                    posicion.longitude
                )
        }


        val campoUbicacion =

            if (tipoUbicacion == TIPO_TRABAJO) {

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


        mostrarGuardando(
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


                mostrarGuardando(
                    false
                )


                val nombreTipo =

                    if (tipoUbicacion == TIPO_TRABAJO) {

                        "trabajo"

                    } else {

                        "casa"
                    }


                Toast.makeText(
                    this,
                    "Ubicación de $nombreTipo guardada correctamente.",
                    Toast.LENGTH_SHORT
                ).show()


                val resultado =
                    Intent().apply {

                        putExtra(
                            EXTRA_TIPO_UBICACION,
                            tipoUbicacion
                        )

                        putExtra(
                            "direccionUbicacion",
                            direccion
                        )

                        putExtra(
                            "latitudUbicacion",
                            posicion.latitude
                        )

                        putExtra(
                            "longitudUbicacion",
                            posicion.longitude
                        )

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
            .addOnFailureListener { error ->


                mostrarGuardando(
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
    // ESTADO GUARDANDO
    // =========================================================

    private fun mostrarGuardando(
        guardando: Boolean
    ) {

        binding.progressGuardarUbicacion.visibility =

            if (guardando) {

                View.VISIBLE

            } else {

                View.GONE
            }


        binding.btnGuardarUbicacion.isEnabled =
            !guardando


        binding.btnGuardarUbicacion.text =

            if (guardando) {

                ""

            } else {

                "GUARDAR UBICACIÓN"
            }
    }


    // =========================================================
    // CONSTANTES
    // =========================================================

    companion object {


        // Extras

        const val EXTRA_TIPO_UBICACION =
            "tipoUbicacion"

        const val EXTRA_ORIGEN_UBICACION =
            "origenUbicacion"


        // Tipos

        const val TIPO_CASA =
            "CASA"

        const val TIPO_TRABAJO =
            "TRABAJO"


        // Orígenes

        const val ORIGEN_PERFIL =
            "PERFIL"


        // Firebase

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


        // Mapa

        private const val ZOOM_INICIAL =
            15f

        private const val ZOOM_UBICACION =
            17f
    }
}