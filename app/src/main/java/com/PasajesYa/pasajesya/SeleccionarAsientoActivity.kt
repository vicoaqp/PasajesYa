package com.PasajesYa.pasajesya

import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.PasajesYa.pasajesya.databinding.ActivitySeleccionarAsientoBinding
import com.PasajesYa.pasajesya.databinding.DialogDatosPasajeroBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import java.util.Locale

class SeleccionarAsientoActivity : AppCompatActivity() {

    private lateinit var binding:
            ActivitySeleccionarAsientoBinding

    private lateinit var auth:
            FirebaseAuth

    private lateinit var firestore:
            FirebaseFirestore

    private var listenerViaje:
            ListenerRegistration? = null

    private var empresaId = ""
    private var empresaNombre = ""
    private var viajeId = ""

    private var precioViaje = 0.0

    private var asientoSeleccionado:
            String? = null

    private var asientosReservados:
            Set<String> = emptySet()

    /*
     * Indica si el viaje todavía está visible
     * y acepta nuevas reservas.
     */
    private var ventasDisponiblesViaje = true

    /*
     * Evita mostrar varias veces el mismo aviso
     * cuando Firestore actualiza el documento.
     */
    private var avisoVentasCerradasMostrado = false

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        auth =
            FirebaseAuth.getInstance()

        firestore =
            FirebaseFirestore.getInstance()

        if (auth.currentUser == null) {

            abrirLogin()
            return
        }

        recuperarDatos()

        if (
            empresaId.isBlank() ||
            viajeId.isBlank()
        ) {

            Toast.makeText(
                this,
                "No se pudo identificar el viaje.",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        binding =
            ActivitySeleccionarAsientoBinding.inflate(
                layoutInflater
            )

        setContentView(binding.root)

        configurarPantalla()
        configurarEventos()
        mostrarDatosIniciales()
        escucharEstadoViaje()
    }

    private fun recuperarDatos() {

        empresaId =
            intent.getStringExtra(
                EXTRA_EMPRESA_ID
            ).orEmpty()

        empresaNombre =
            intent.getStringExtra(
                EXTRA_EMPRESA_NOMBRE
            ).orEmpty()

        viajeId =
            intent.getStringExtra(
                EXTRA_VIAJE_ID
            ).orEmpty()
    }

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

    private fun configurarEventos() {

        binding.btnVolver.setOnClickListener {

            finish()
        }

        binding.btnAsientoA1.setOnClickListener {

            seleccionarAsiento(
                ASIENTO_A1
            )
        }

        binding.btnAsientoB1.setOnClickListener {

            seleccionarAsiento(
                ASIENTO_B1
            )
        }

        binding.btnAsientoB2.setOnClickListener {

            seleccionarAsiento(
                ASIENTO_B2
            )
        }

        binding.btnAsientoB3.setOnClickListener {

            seleccionarAsiento(
                ASIENTO_B3
            )
        }

        binding.btnConfirmarAsiento
            .setOnClickListener {

                mostrarOpcionesPasajero()
            }
    }

    private fun mostrarDatosIniciales() {

        binding.tvEmpresa.text =
            empresaNombre.ifBlank {

                "Empresa de transporte"
            }

        binding.tvRuta.text =
            "Cargando ruta..."

        binding.tvVehiculo.text =
            "Cargando vehículo..."

        actualizarMapaAsientos()
        actualizarResumen()
    }

    // -------------------------------------------------
    // ESCUCHAR EL VIAJE EN TIEMPO REAL
    // -------------------------------------------------

    private fun escucharEstadoViaje() {

        listenerViaje?.remove()
        listenerViaje = null

        listenerViaje =
            firestore
                .collection(
                    COLECCION_EMPRESAS
                )
                .document(
                    empresaId
                )
                .collection(
                    SUBCOLECCION_VIAJES
                )
                .document(
                    viajeId
                )
                .addSnapshotListener {
                        documento, error ->

                    if (error != null) {

                        Toast.makeText(
                            this,
                            "No se pudo actualizar el viaje: " +
                                    (
                                            error.localizedMessage
                                                ?: "error desconocido"
                                            ),
                            Toast.LENGTH_LONG
                        ).show()

                        return@addSnapshotListener
                    }

                    if (
                        documento == null ||
                        !documento.exists()
                    ) {

                        Toast.makeText(
                            this,
                            "El viaje ya no está disponible.",
                            Toast.LENGTH_LONG
                        ).show()

                        finish()

                        return@addSnapshotListener
                    }

                    /*
                     * Verificar en tiempo real si la vendedora
                     * cerró las ventas.
                     */
                    val ventasCerradas =
                        documento.getBoolean(
                            CAMPO_VENTAS_CERRADAS
                        ) ?: false

                    val aceptaReservas =
                        documento.getBoolean(
                            CAMPO_ACEPTA_RESERVAS
                        ) ?: true

                    val visibleUsuarios =
                        documento.getBoolean(
                            CAMPO_VISIBLE_USUARIOS
                        ) ?: true

                    ventasDisponiblesViaje =
                        !ventasCerradas &&
                                aceptaReservas &&
                                visibleUsuarios

                    if (!ventasDisponiblesViaje) {

                        asientoSeleccionado =
                            null

                        if (
                            !avisoVentasCerradasMostrado
                        ) {

                            avisoVentasCerradasMostrado =
                                true

                            Toast.makeText(
                                this,
                                "Las ventas de este viaje ya fueron cerradas.",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                    } else {

                        avisoVentasCerradasMostrado =
                            false
                    }

                    val origen =
                        documento
                            .getString(
                                CAMPO_ORIGEN
                            )
                            .orEmpty()

                    val destino =
                        documento
                            .getString(
                                CAMPO_DESTINO
                            )
                            .orEmpty()

                    val vehiculo =
                        documento
                            .getString(
                                CAMPO_VEHICULO
                            )
                            .orEmpty()

                    val placa =
                        documento
                            .getString(
                                CAMPO_PLACA
                            )
                            .orEmpty()

                    precioViaje =
                        (
                                documento.get(
                                    CAMPO_PRECIO
                                ) as? Number
                                )
                            ?.toDouble()
                            ?: 0.0

                    asientosReservados =
                        (
                                documento.get(
                                    CAMPO_ASIENTOS_RESERVADOS
                                ) as? List<*>
                                )
                            ?.mapNotNull {
                                    valor ->

                                valor as? String
                            }
                            ?.map {
                                    codigo ->

                                codigo
                                    .trim()
                                    .uppercase(
                                        Locale.ROOT
                                    )
                            }
                            ?.toSet()
                            .orEmpty()

                    binding.tvRuta.text =
                        "$origen → $destino"

                    binding.tvVehiculo.text =
                        when {

                            vehiculo.isNotBlank() &&
                                    placa.isNotBlank() -> {

                                "$vehiculo · $placa"
                            }

                            vehiculo.isNotBlank() -> {

                                vehiculo
                            }

                            placa.isNotBlank() -> {

                                "Vehículo · $placa"
                            }

                            else -> {

                                "Vehículo asignado"
                            }
                        }

                    val asientoActual =
                        asientoSeleccionado

                    if (
                        asientoActual != null &&
                        asientosReservados.contains(
                            asientoActual
                        )
                    ) {

                        asientoSeleccionado =
                            null

                        Toast.makeText(
                            this,
                            "Ese asiento acaba de ser " +
                                    "reservado. Selecciona otro.",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    actualizarMapaAsientos()
                    actualizarResumen()
                }
    }

    // -------------------------------------------------
    // SELECCIÓN DEL ASIENTO
    // -------------------------------------------------

    private fun seleccionarAsiento(
        codigoAsiento: String
    ) {

        if (!ventasDisponiblesViaje) {

            Toast.makeText(
                this,
                "Las ventas de este viaje ya fueron cerradas.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        if (
            asientosReservados.contains(
                codigoAsiento
            )
        ) {

            Toast.makeText(
                this,
                "Este asiento ya está ocupado.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        asientoSeleccionado =
            if (
                asientoSeleccionado ==
                codigoAsiento
            ) {

                null

            } else {

                codigoAsiento
            }

        actualizarMapaAsientos()
        actualizarResumen()
    }

    private fun actualizarMapaAsientos() {

        configurarBotonAsiento(
            boton =
                binding.btnAsientoA1,

            codigo =
                ASIENTO_A1,

            descripcion =
                "DELANTERO"
        )

        configurarBotonAsiento(
            boton =
                binding.btnAsientoB1,

            codigo =
                ASIENTO_B1,

            descripcion =
                "VENTANA IZQ."
        )

        configurarBotonAsiento(
            boton =
                binding.btnAsientoB2,

            codigo =
                ASIENTO_B2,

            descripcion =
                "CENTRO"
        )

        configurarBotonAsiento(
            boton =
                binding.btnAsientoB3,

            codigo =
                ASIENTO_B3,

            descripcion =
                "VENTANA DER."
        )
    }

    private fun configurarBotonAsiento(
        boton: MaterialButton,
        codigo: String,
        descripcion: String
    ) {

        val ocupado =
            asientosReservados.contains(
                codigo
            )

        val seleccionado =
            asientoSeleccionado ==
                    codigo

        when {

            !ventasDisponiblesViaje -> {

                boton.isEnabled =
                    false

                boton.text =
                    "$codigo\nNO DISPONIBLE"

                boton.backgroundTintList =
                    ColorStateList.valueOf(
                        getColor(
                            R.color
                                .pasajes_asiento_ocupado
                        )
                    )

                boton.setTextColor(
                    getColor(
                        R.color
                            .pasajes_text_secondary
                    )
                )

                boton.iconTint =
                    ColorStateList.valueOf(
                        getColor(
                            R.color
                                .pasajes_text_secondary
                        )
                    )

                boton.strokeWidth =
                    0
            }

            ocupado -> {

                boton.isEnabled =
                    false

                boton.text =
                    "$codigo\nOCUPADO"

                boton.backgroundTintList =
                    ColorStateList.valueOf(
                        getColor(
                            R.color
                                .pasajes_asiento_ocupado
                        )
                    )

                boton.setTextColor(
                    getColor(
                        R.color
                            .pasajes_text_secondary
                    )
                )

                boton.iconTint =
                    ColorStateList.valueOf(
                        getColor(
                            R.color
                                .pasajes_text_secondary
                        )
                    )

                boton.strokeWidth =
                    0
            }

            seleccionado -> {

                boton.isEnabled =
                    true

                boton.text =
                    "$codigo\nSELECCIONADO"

                boton.backgroundTintList =
                    ColorStateList.valueOf(
                        getColor(
                            R.color
                                .pasajes_asiento_seleccionado
                        )
                    )

                boton.setTextColor(
                    getColor(
                        R.color
                            .pasajes_text_primary
                    )
                )

                boton.iconTint =
                    ColorStateList.valueOf(
                        getColor(
                            R.color
                                .pasajes_text_primary
                        )
                    )

                boton.strokeColor =
                    ColorStateList.valueOf(
                        getColor(
                            R.color
                                .pasajes_secondary
                        )
                    )

                boton.strokeWidth =
                    2
            }

            else -> {

                boton.isEnabled =
                    true

                boton.text =
                    "$codigo\n$descripcion"

                boton.backgroundTintList =
                    ColorStateList.valueOf(
                        getColor(
                            R.color
                                .pasajes_asiento_disponible
                        )
                    )

                boton.setTextColor(
                    getColor(
                        R.color.pasajes_primary
                    )
                )

                boton.iconTint =
                    ColorStateList.valueOf(
                        getColor(
                            R.color.pasajes_primary
                        )
                    )

                boton.strokeColor =
                    ColorStateList.valueOf(
                        getColor(
                            R.color.pasajes_primary
                        )
                    )

                boton.strokeWidth =
                    1
            }
        }
    }

    private fun actualizarResumen() {

        binding.tvAsientoSeleccionado.text =
            asientoSeleccionado
                ?: "Ninguno"

        binding.tvPrecio.text =
            String.format(
                Locale("es", "PE"),
                "S/ %.2f",
                precioViaje
            )

        binding.btnConfirmarAsiento.isEnabled =
            ventasDisponiblesViaje &&
                    asientoSeleccionado != null
    }

    // -------------------------------------------------
    // DATOS DEL PASAJERO
    // -------------------------------------------------

    private fun mostrarOpcionesPasajero() {

        if (!ventasDisponiblesViaje) {

            Toast.makeText(
                this,
                "Las ventas de este viaje ya fueron cerradas.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val asiento =
            asientoSeleccionado

        if (asiento == null) {

            Toast.makeText(
                this,
                "Selecciona un asiento.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val opciones =
            arrayOf(
                "YO VIAJARÉ",
                "OTRA PERSONA",
                "COMPLETAR DATOS DESPUÉS"
            )

        MaterialAlertDialogBuilder(this)
            .setTitle(
                "¿Quién ocupará el asiento $asiento?"
            )
            .setItems(
                opciones
            ) { _, posicion ->

                when (posicion) {

                    0 -> {

                        reservarParaTitular(
                            asiento
                        )
                    }

                    1 -> {

                        mostrarFormularioOtraPersona(
                            asiento
                        )
                    }

                    2 -> {

                        reservarConDatosPendientes(
                            asiento
                        )
                    }
                }
            }
            .setNegativeButton(
                "CANCELAR",
                null
            )
            .show()
    }

    private fun reservarParaTitular(
        asiento: String
    ) {

        cargarDatosComprador {
                comprador ->

            val pasajero =
                comprador.copy(
                    esTitular = true,
                    datosPendientes = false
                )

            mostrarConfirmacionReserva(
                asiento = asiento,
                comprador = comprador,
                pasajero = pasajero
            )
        }
    }

    private fun mostrarFormularioOtraPersona(
        asiento: String
    ) {

        val dialogBinding =
            DialogDatosPasajeroBinding.inflate(
                layoutInflater
            )

        val dialog =
            MaterialAlertDialogBuilder(this)
                .setTitle(
                    "Datos del pasajero"
                )
                .setView(
                    dialogBinding.root
                )
                .setNegativeButton(
                    "CANCELAR",
                    null
                )
                .setPositiveButton(
                    "CONTINUAR",
                    null
                )
                .create()

        dialog.setOnShowListener {

            dialog.getButton(
                DialogInterface.BUTTON_POSITIVE
            ).setOnClickListener {

                dialogBinding
                    .tilDniPasajero
                    .error = null

                dialogBinding
                    .tilNombresPasajero
                    .error = null

                dialogBinding
                    .tilApellidosPasajero
                    .error = null

                dialogBinding
                    .tilCelularPasajero
                    .error = null

                val dni =
                    dialogBinding
                        .etDniPasajero
                        .text
                        ?.toString()
                        ?.trim()
                        .orEmpty()

                val nombres =
                    dialogBinding
                        .etNombresPasajero
                        .text
                        ?.toString()
                        ?.trim()
                        .orEmpty()

                val apellidos =
                    dialogBinding
                        .etApellidosPasajero
                        .text
                        ?.toString()
                        ?.trim()
                        .orEmpty()

                val celular =
                    dialogBinding
                        .etCelularPasajero
                        .text
                        ?.toString()
                        ?.trim()
                        .orEmpty()

                var formularioValido =
                    true

                if (
                    dni.length != 8 ||
                    !dni.all {
                            caracter ->

                        caracter.isDigit()
                    }
                ) {

                    dialogBinding
                        .tilDniPasajero
                        .error =
                        "El DNI debe tener 8 dígitos"

                    formularioValido =
                        false
                }

                if (nombres.length < 2) {

                    dialogBinding
                        .tilNombresPasajero
                        .error =
                        "Ingresa los nombres"

                    formularioValido =
                        false
                }

                if (apellidos.length < 2) {

                    dialogBinding
                        .tilApellidosPasajero
                        .error =
                        "Ingresa los apellidos"

                    formularioValido =
                        false
                }

                if (
                    celular.isNotBlank() &&
                    (
                            celular.length != 9 ||
                                    !celular.all {
                                            caracter ->

                                        caracter.isDigit()
                                    }
                            )
                ) {

                    dialogBinding
                        .tilCelularPasajero
                        .error =
                        "El celular debe tener 9 dígitos"

                    formularioValido =
                        false
                }

                if (!formularioValido) {

                    return@setOnClickListener
                }

                val pasajero =
                    DatosPasajero(
                        dni = dni,
                        nombres = nombres,
                        apellidos = apellidos,
                        celular = celular,
                        esTitular = false,
                        datosPendientes = false
                    )

                dialog.dismiss()

                cargarDatosComprador {
                        comprador ->

                    mostrarConfirmacionReserva(
                        asiento = asiento,
                        comprador = comprador,
                        pasajero = pasajero
                    )
                }
            }
        }

        dialog.show()
    }

    private fun reservarConDatosPendientes(
        asiento: String
    ) {

        cargarDatosComprador {
                comprador ->

            val pasajeroPendiente =
                DatosPasajero(
                    dni = "",
                    nombres = "",
                    apellidos = "",
                    celular = "",
                    esTitular = false,
                    datosPendientes = true
                )

            mostrarConfirmacionReserva(
                asiento = asiento,
                comprador = comprador,
                pasajero = pasajeroPendiente
            )
        }
    }

    private fun cargarDatosComprador(
        alCargar: (DatosPasajero) -> Unit
    ) {

        val usuarioActual =
            auth.currentUser

        if (usuarioActual == null) {

            abrirLogin()
            return
        }

        mostrarCargando(
            true
        )

        firestore
            .collection(
                COLECCION_USUARIOS
            )
            .document(
                usuarioActual.uid
            )
            .get()
            .addOnSuccessListener {
                    documento ->

                mostrarCargando(
                    false
                )

                if (!documento.exists()) {

                    Toast.makeText(
                        this,
                        "No se encontró tu perfil.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addOnSuccessListener
                }

                val comprador =
                    DatosPasajero(

                        dni =
                            documento
                                .getString(
                                    CAMPO_DNI
                                )
                                .orEmpty()
                                .trim(),

                        nombres =
                            documento
                                .getString(
                                    CAMPO_NOMBRES
                                )
                                .orEmpty()
                                .trim(),

                        apellidos =
                            documento
                                .getString(
                                    CAMPO_APELLIDOS
                                )
                                .orEmpty()
                                .trim(),

                        celular =
                            documento
                                .getString(
                                    CAMPO_CELULAR
                                )
                                .orEmpty()
                                .trim(),

                        esTitular =
                            true,

                        datosPendientes =
                            false
                    )

                if (
                    comprador.nombres.isBlank() ||
                    comprador.apellidos.isBlank()
                ) {

                    Toast.makeText(
                        this,
                        "Completa tus datos personales " +
                                "en Mi perfil.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addOnSuccessListener
                }

                alCargar(
                    comprador
                )
            }
            .addOnFailureListener {
                    error ->

                mostrarCargando(
                    false
                )

                Toast.makeText(
                    this,
                    "No se pudieron cargar tus datos: " +
                            (
                                    error.localizedMessage
                                        ?: "error desconocido"
                                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun mostrarConfirmacionReserva(
        asiento: String,
        comprador: DatosPasajero,
        pasajero: DatosPasajero
    ) {

        if (!ventasDisponiblesViaje) {

            Toast.makeText(
                this,
                "Las ventas de este viaje ya fueron cerradas.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val nombreComprador =
            comprador
                .nombreCompleto()
                .ifBlank {

                    "Usuario registrado"
                }

        val informacionPasajero =
            when {

                pasajero.datosPendientes -> {

                    "Datos por completar"
                }

                pasajero.esTitular -> {

                    pasajero.nombreCompleto() +
                            " (titular)"
                }

                else -> {

                    buildString {

                        append(
                            pasajero.nombreCompleto()
                        )

                        if (
                            pasajero.dni.isNotBlank()
                        ) {

                            append(
                                "\nDNI: ${pasajero.dni}"
                            )
                        }
                    }
                }
            }

        val mensaje =
            buildString {

                append(
                    "Empresa: $empresaNombre\n"
                )

                append(
                    "Comprador: $nombreComprador\n\n"
                )

                append(
                    "Asiento: $asiento\n"
                )

                append(
                    "Pasajero: $informacionPasajero\n\n"
                )

                append(
                    String.format(
                        Locale("es", "PE"),
                        "Precio: S/ %.2f",
                        precioViaje
                    )
                )

                if (
                    pasajero.datosPendientes
                ) {

                    append(
                        "\n\nDeberás completar los datos " +
                                "del pasajero antes del viaje."
                    )
                }
            }

        MaterialAlertDialogBuilder(this)
            .setTitle(
                "Confirmar reserva"
            )
            .setMessage(
                mensaje
            )
            .setNegativeButton(
                "CANCELAR",
                null
            )
            .setPositiveButton(
                "RESERVAR"
            ) { _, _ ->

                reservarAsiento(
                    asiento = asiento,
                    comprador = comprador,
                    pasajero = pasajero
                )
            }
            .show()
    }

    // -------------------------------------------------
    // REGISTRAR RESERVA
    // -------------------------------------------------

    private fun reservarAsiento(
        asiento: String,
        comprador: DatosPasajero,
        pasajero: DatosPasajero
    ) {

        val usuarioActual =
            auth.currentUser

        if (usuarioActual == null) {

            abrirLogin()
            return
        }

        mostrarCargando(
            true
        )

        val viajeReferencia =
            firestore
                .collection(
                    COLECCION_EMPRESAS
                )
                .document(
                    empresaId
                )
                .collection(
                    SUBCOLECCION_VIAJES
                )
                .document(
                    viajeId
                )

        /*
         * Se crea un ID automático.
         * Cada asiento tendrá una reserva diferente.
         */
        val reservaReferencia =
            firestore
                .collection(
                    COLECCION_RESERVAS
                )
                .document()

        val reservaId =
            reservaReferencia.id

        firestore
            .runTransaction {
                    transaccion ->

                /*
                 * Primero se realiza la lectura del viaje.
                 */
                val viajeDocumento =
                    transaccion.get(
                        viajeReferencia
                    )

                if (!viajeDocumento.exists()) {

                    throw FirebaseFirestoreException(
                        "El viaje ya no existe.",
                        FirebaseFirestoreException
                            .Code.NOT_FOUND
                    )
                }

                /*
                 * Validación obligatoria dentro de la transacción.
                 *
                 * Esto impide que un usuario reserve cuando tenía
                 * abierta la pantalla antes del cierre de ventas.
                 */
                val ventasCerradas =
                    viajeDocumento.getBoolean(
                        CAMPO_VENTAS_CERRADAS
                    ) ?: false

                val aceptaReservas =
                    viajeDocumento.getBoolean(
                        CAMPO_ACEPTA_RESERVAS
                    ) ?: true

                val visibleUsuarios =
                    viajeDocumento.getBoolean(
                        CAMPO_VISIBLE_USUARIOS
                    ) ?: true

                if (
                    ventasCerradas ||
                    !aceptaReservas ||
                    !visibleUsuarios
                ) {

                    throw FirebaseFirestoreException(
                        "Las ventas de este viaje ya fueron cerradas.",
                        FirebaseFirestoreException
                            .Code.ABORTED
                    )
                }

                val estadoViaje =
                    viajeDocumento
                        .getString(
                            CAMPO_ESTADO
                        )
                        .orEmpty()
                        .trim()
                        .lowercase(
                            Locale.ROOT
                        )

                if (
                    estadoViaje !=
                    ESTADO_ACTIVO &&

                    estadoViaje !=
                    ESTADO_DISPONIBLE &&

                    estadoViaje !=
                    ESTADO_RECIBIENDO
                ) {

                    throw FirebaseFirestoreException(
                        "El viaje ya no recibe pasajeros.",
                        FirebaseFirestoreException
                            .Code.ABORTED
                    )
                }

                if (
                    !ASIENTOS_VALIDOS.contains(
                        asiento
                    )
                ) {

                    throw FirebaseFirestoreException(
                        "El asiento seleccionado no es válido.",
                        FirebaseFirestoreException
                            .Code.INVALID_ARGUMENT
                    )
                }

                val capacidad =
                    (
                            viajeDocumento.get(
                                CAMPO_CAPACIDAD
                            ) as? Number
                            )
                        ?.toInt()
                        ?: 4

                val listaActual =
                    (
                            viajeDocumento.get(
                                CAMPO_ASIENTOS_RESERVADOS
                            ) as? List<*>
                            )
                        ?.mapNotNull {
                                valor ->

                            valor as? String
                        }
                        ?.map {
                                codigo ->

                            codigo
                                .trim()
                                .uppercase(
                                    Locale.ROOT
                                )
                        }
                        ?.distinct()
                        ?.toMutableList()
                        ?: mutableListOf()

                if (
                    listaActual.contains(
                        asiento
                    )
                ) {

                    throw FirebaseFirestoreException(
                        "El asiento acaba de ser reservado.",
                        FirebaseFirestoreException
                            .Code.ABORTED
                    )
                }

                if (
                    listaActual.size >=
                    capacidad
                ) {

                    throw FirebaseFirestoreException(
                        "La unidad ya está completa.",
                        FirebaseFirestoreException
                            .Code.ABORTED
                    )
                }

                val origen =
                    viajeDocumento
                        .getString(
                            CAMPO_ORIGEN
                        )
                        .orEmpty()

                val destino =
                    viajeDocumento
                        .getString(
                            CAMPO_DESTINO
                        )
                        .orEmpty()

                val fechaSalida =
                    viajeDocumento
                        .getString(
                            CAMPO_FECHA_SALIDA
                        )
                        .orEmpty()

                val horaSalida =
                    viajeDocumento
                        .getString(
                            CAMPO_HORA_SALIDA
                        )
                        .orEmpty()

                val puntoEmbarque =
                    viajeDocumento
                        .getString(
                            CAMPO_PUNTO_EMBARQUE
                        )
                        .orEmpty()

                val placa =
                    viajeDocumento
                        .getString(
                            CAMPO_PLACA
                        )
                        .orEmpty()

                val vehiculoDescripcion =
                    viajeDocumento
                        .getString(
                            CAMPO_VEHICULO
                        )
                        .orEmpty()

                val precio =
                    (
                            viajeDocumento.get(
                                CAMPO_PRECIO
                            ) as? Number
                            )
                        ?.toDouble()
                        ?: precioViaje

                listaActual.add(
                    asiento
                )

                val asientosActualizados =
                    listaActual
                        .distinct()

                val asientosDisponibles =
                    ASIENTOS_VALIDOS.filter {
                            codigo ->

                        !asientosActualizados.contains(
                            codigo
                        )
                    }

                val cuposDisponibles =
                    (
                            capacidad -
                                    asientosActualizados.size
                            ).coerceAtLeast(0)

                val tipoPasajero =
                    when {

                        pasajero.datosPendientes -> {

                            TIPO_PASAJERO_PENDIENTE
                        }

                        pasajero.esTitular -> {

                            TIPO_PASAJERO_TITULAR
                        }

                        else -> {

                            TIPO_PASAJERO_OTRA_PERSONA
                        }
                    }

                val nombrePasajero =
                    pasajero
                        .nombreCompleto()
                        .ifBlank {

                            if (
                                pasajero.datosPendientes
                            ) {

                                "Datos por completar"

                            } else {

                                "Pasajero"
                            }
                        }

                val datosReserva =
                    hashMapOf<String, Any>(

                        "reservaId" to
                                reservaId,

                        "empresaId" to
                                empresaId,

                        "empresaNombre" to
                                empresaNombre,

                        "viajeId" to
                                viajeId,

                        /*
                         * Cuenta que realizó la reserva.
                         */
                        "usuarioId" to
                                usuarioActual.uid,

                        "compradorUid" to
                                usuarioActual.uid,

                        "usuarioCorreo" to
                                usuarioActual.email
                                    .orEmpty(),

                        "compradorDni" to
                                comprador.dni,

                        "compradorNombres" to
                                comprador.nombres,

                        "compradorApellidos" to
                                comprador.apellidos,

                        "compradorCelular" to
                                comprador.celular,

                        /*
                         * Datos del pasajero.
                         */
                        "pasajeroNombre" to
                                nombrePasajero,

                        "nombrePasajero" to
                                nombrePasajero,

                        "pasajeroDni" to
                                pasajero.dni,

                        "pasajeroNombres" to
                                pasajero.nombres,

                        "pasajeroApellidos" to
                                pasajero.apellidos,

                        "pasajeroCelular" to
                                pasajero.celular,

                        "esTitular" to
                                pasajero.esTitular,

                        "datosPasajeroPendientes" to
                                pasajero.datosPendientes,

                        "tipoPasajero" to
                                tipoPasajero,

                        /*
                         * Datos del viaje.
                         */
                        "origen" to
                                origen,

                        "destino" to
                                destino,

                        "asiento" to
                                asiento,

                        "numeroAsiento" to
                                asiento,

                        "cantidadPasajeros" to
                                1,

                        "precio" to
                                precio,

                        "precioTotal" to
                                precio,

                        /*
                         * Estado oficial de la reserva.
                         */
                        "estadoReserva" to
                                ESTADO_RESERVA_PENDIENTE,

                        "estadoPago" to
                                ESTADO_PAGO_PENDIENTE,

                        "estadoAbordaje" to
                                ESTADO_ABORDAJE_PENDIENTE,

                        "boletoValidado" to
                                false,

                        "fechaSalida" to
                                fechaSalida,

                        "horaSalida" to
                                horaSalida,

                        "puntoEmbarque" to
                                puntoEmbarque,

                        "placa" to
                                placa,

                        "vehiculoDescripcion" to
                                vehiculoDescripcion,

                        "fechaReserva" to
                                FieldValue
                                    .serverTimestamp(),

                        "fechaActualizacion" to
                                FieldValue
                                    .serverTimestamp(),

                        "ultimaActualizacion" to
                                FieldValue
                                    .serverTimestamp()
                    )

                /*
                 * Actualizar la disponibilidad del viaje.
                 */
                transaccion.update(
                    viajeReferencia,
                    mapOf(

                        CAMPO_ASIENTOS_RESERVADOS to
                                asientosActualizados,

                        CAMPO_ASIENTOS_OCUPADOS to
                                asientosActualizados.size,

                        CAMPO_CUPOS_DISPONIBLES to
                                cuposDisponibles,

                        CAMPO_ASIENTOS_DISPONIBLES to
                                asientosDisponibles,

                        CAMPO_FECHA_ACTUALIZACION to
                                FieldValue
                                    .serverTimestamp(),

                        CAMPO_ULTIMA_ACTUALIZACION to
                                FieldValue
                                    .serverTimestamp()
                    )
                )

                /*
                 * Crear la reserva.
                 */
                transaccion.set(
                    reservaReferencia,
                    datosReserva
                )

                reservaId
            }
            .addOnSuccessListener {

                mostrarCargando(
                    false
                )

                asientosReservados =
                    asientosReservados +
                            asiento

                asientoSeleccionado =
                    null

                actualizarMapaAsientos()
                actualizarResumen()

                val mensajeResultado =
                    if (
                        pasajero.datosPendientes
                    ) {

                        "El asiento $asiento fue reservado.\n\n" +
                                "Los datos del pasajero quedaron pendientes."

                    } else {

                        "El asiento $asiento fue reservado para " +
                                pasajero.nombreCompleto() +
                                "."
                    }

                MaterialAlertDialogBuilder(this)
                    .setTitle(
                        "Reserva realizada"
                    )
                    .setMessage(
                        mensajeResultado
                    )
                    .setNegativeButton(
                        "RESERVAR OTRO"
                    ) { _, _ ->

                        asientoSeleccionado =
                            null

                        actualizarMapaAsientos()
                        actualizarResumen()
                    }
                    .setNeutralButton(
                        "VOLVER"
                    ) { _, _ ->

                        finish()
                    }
                    .setPositiveButton(
                        "VER MIS RESERVAS"
                    ) { _, _ ->

                        abrirMisReservas()
                    }
                    .setCancelable(
                        false
                    )
                    .show()
            }
            .addOnFailureListener {
                    error ->

                mostrarCargando(
                    false
                )

                Toast.makeText(
                    this,
                    error.localizedMessage
                        ?: "No se pudo reservar el asiento.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // -------------------------------------------------
    // ESTADO DE CARGA
    // -------------------------------------------------

    private fun mostrarCargando(
        cargando: Boolean
    ) {

        binding.progressConfirmar.visibility =
            if (cargando) {

                View.VISIBLE

            } else {

                View.GONE
            }

        binding.btnConfirmarAsiento.text =
            if (cargando) {

                ""

            } else {

                "CONFIRMAR ASIENTO"
            }

        binding.btnConfirmarAsiento.isEnabled =
            !cargando &&
                    ventasDisponiblesViaje &&
                    asientoSeleccionado != null

        binding.btnVolver.isEnabled =
            !cargando

        binding.btnAsientoA1.isEnabled =
            !cargando &&
                    ventasDisponiblesViaje &&
                    !asientosReservados.contains(
                        ASIENTO_A1
                    )

        binding.btnAsientoB1.isEnabled =
            !cargando &&
                    ventasDisponiblesViaje &&
                    !asientosReservados.contains(
                        ASIENTO_B1
                    )

        binding.btnAsientoB2.isEnabled =
            !cargando &&
                    ventasDisponiblesViaje &&
                    !asientosReservados.contains(
                        ASIENTO_B2
                    )

        binding.btnAsientoB3.isEnabled =
            !cargando &&
                    ventasDisponiblesViaje &&
                    !asientosReservados.contains(
                        ASIENTO_B3
                    )
    }

    private fun abrirMisReservas() {

        val intent =
            Intent(
                this,
                MisReservasActivity::class.java
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

        startActivity(
            intent
        )

        finish()
    }

    private fun abrirLogin() {

        val intent =
            Intent(
                this,
                LoginActivity::class.java
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

        startActivity(
            intent
        )

        finish()
    }

    override fun onDestroy() {

        listenerViaje?.remove()
        listenerViaje = null

        super.onDestroy()
    }

    companion object {

        const val EXTRA_EMPRESA_ID =
            "extra_empresa_id_asiento"

        const val EXTRA_EMPRESA_NOMBRE =
            "extra_empresa_nombre_asiento"

        const val EXTRA_VIAJE_ID =
            "extra_viaje_id_asiento"

        private const val COLECCION_EMPRESAS =
            "empresaspasajes"

        private const val COLECCION_USUARIOS =
            "usuariospasajes"

        private const val COLECCION_RESERVAS =
            "reservaspasajes"

        private const val SUBCOLECCION_VIAJES =
            "viajespasajes"

        private const val CAMPO_ORIGEN =
            "origen"

        private const val CAMPO_DESTINO =
            "destino"

        private const val CAMPO_PRECIO =
            "precio"

        private const val CAMPO_ESTADO =
            "estado"

        private const val CAMPO_CAPACIDAD =
            "capacidad"

        private const val CAMPO_ASIENTOS_RESERVADOS =
            "asientosReservados"

        private const val CAMPO_ASIENTOS_OCUPADOS =
            "asientosOcupados"

        private const val CAMPO_ASIENTOS_DISPONIBLES =
            "asientosDisponibles"

        private const val CAMPO_CUPOS_DISPONIBLES =
            "cuposDisponibles"

        private const val CAMPO_VENTAS_CERRADAS =
            "ventasCerradas"

        private const val CAMPO_ACEPTA_RESERVAS =
            "aceptaReservas"

        private const val CAMPO_VISIBLE_USUARIOS =
            "visibleUsuarios"

        private const val CAMPO_FECHA_ACTUALIZACION =
            "fechaActualizacion"

        private const val CAMPO_FECHA_SALIDA =
            "fechaSalida"

        private const val CAMPO_HORA_SALIDA =
            "horaSalida"

        private const val CAMPO_PUNTO_EMBARQUE =
            "puntoEmbarque"

        private const val CAMPO_PLACA =
            "placa"

        private const val CAMPO_VEHICULO =
            "vehiculoDescripcion"

        private const val CAMPO_ULTIMA_ACTUALIZACION =
            "ultimaActualizacion"

        private const val CAMPO_DNI =
            "dni"

        private const val CAMPO_NOMBRES =
            "nombres"

        private const val CAMPO_APELLIDOS =
            "apellidos"

        private const val CAMPO_CELULAR =
            "celular"

        private const val ASIENTO_A1 =
            "A1"

        private const val ASIENTO_B1 =
            "B1"

        private const val ASIENTO_B2 =
            "B2"

        private const val ASIENTO_B3 =
            "B3"

        private val ASIENTOS_VALIDOS =
            setOf(
                ASIENTO_A1,
                ASIENTO_B1,
                ASIENTO_B2,
                ASIENTO_B3
            )

        private const val ESTADO_ACTIVO =
            "activo"

        private const val ESTADO_DISPONIBLE =
            "disponible"

        private const val ESTADO_RECIBIENDO =
            "recibiendo_pasajeros"

        private const val ESTADO_RESERVA_PENDIENTE =
            "pendiente"

        private const val ESTADO_PAGO_PENDIENTE =
            "pendiente"

        private const val ESTADO_ABORDAJE_PENDIENTE =
            "pendiente"

        private const val TIPO_PASAJERO_TITULAR =
            "titular"

        private const val TIPO_PASAJERO_OTRA_PERSONA =
            "otra_persona"

        private const val TIPO_PASAJERO_PENDIENTE =
            "pendiente_datos"
    }
}