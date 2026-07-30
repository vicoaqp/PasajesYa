package com.PasajesYa.pasajesya

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.PasajesYa.pasajesya.databinding.ActivityMisReservasBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Locale

class MisReservasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMisReservasBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var reservaAdapter: ReservaAdapter

    private var listenerReservas: ListenerRegistration? = null

    private val reservasCompletas =
        mutableListOf<Reserva>()

    private var filtroActual =
        FILTRO_TODAS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        if (auth.currentUser == null) {
            abrirLogin()
            return
        }

        binding = ActivityMisReservasBinding.inflate(
            layoutInflater
        )

        setContentView(binding.root)

        configurarPantalla()
        configurarRecycler()
        configurarFiltros()
        configurarEventos()
        escucharReservas()
    }

    private fun configurarPantalla() {

        window.statusBarColor =
            getColor(R.color.pasajes_primary_dark)

        window.navigationBarColor =
            getColor(R.color.pasajes_background)
    }

    private fun configurarRecycler() {

        reservaAdapter = ReservaAdapter(

            onVerDetalle = { reserva ->
                mostrarDetalleReserva(reserva)
            },

            onCancelar = { reserva ->
                confirmarCancelacion(reserva)
            }
        )

        binding.recyclerReservas.apply {

            layoutManager = LinearLayoutManager(
                this@MisReservasActivity
            )

            adapter = reservaAdapter

            setHasFixedSize(false)
        }
    }

    private fun configurarFiltros() {

        binding.chipTodas.setOnClickListener {

            filtroActual = FILTRO_TODAS
            aplicarFiltro()
        }

        binding.chipPendientes.setOnClickListener {

            filtroActual = ESTADO_PENDIENTE
            aplicarFiltro()
        }

        binding.chipConfirmadas.setOnClickListener {

            filtroActual = ESTADO_CONFIRMADA
            aplicarFiltro()
        }

        binding.chipCanceladas.setOnClickListener {

            filtroActual = ESTADO_CANCELADA
            aplicarFiltro()
        }
    }

    private fun configurarEventos() {

        binding.btnVolver.setOnClickListener {
            finish()
        }
    }

    private fun escucharReservas() {

        val usuarioActual = auth.currentUser

        if (usuarioActual == null) {
            abrirLogin()
            return
        }

        mostrarCargando(true)

        listenerReservas?.remove()
        listenerReservas = null

        /*
         * Busca todos los boletos comprados por la
         * cuenta que actualmente inició sesión.
         *
         * Cada boleto tiene un documento diferente,
         * pero todos conservan el mismo usuarioId.
         */
        listenerReservas = firestore
            .collection(COLECCION_RESERVAS)
            .whereEqualTo(
                CAMPO_USUARIO_ID,
                usuarioActual.uid
            )
            .addSnapshotListener { resultado, error ->

                mostrarCargando(false)

                if (error != null) {

                    reservasCompletas.clear()

                    reservaAdapter.actualizarReservas(
                        emptyList()
                    )

                    mostrarSinReservas(
                        mostrar = true,
                        mensaje = "No se pudieron cargar " +
                                "tus reservas.\n" +
                                (
                                        error.localizedMessage
                                            ?: "Error desconocido"
                                        )
                    )

                    Toast.makeText(
                        this,
                        "Error al cargar reservas: " +
                                (
                                        error.localizedMessage
                                            ?: "error desconocido"
                                        ),
                        Toast.LENGTH_LONG
                    ).show()

                    return@addSnapshotListener
                }

                if (resultado == null) {

                    reservasCompletas.clear()

                    reservaAdapter.actualizarReservas(
                        emptyList()
                    )

                    mostrarSinReservas(
                        mostrar = true,
                        mensaje = "No se recibió información " +
                                "de tus reservas."
                    )

                    return@addSnapshotListener
                }

                val reservas = resultado.documents
                    .map { documento ->

                        Reserva(

                            id = documento.id,

                            documentoPath =
                                documento.reference.path,

                            empresaId = documento
                                .getString(CAMPO_EMPRESA_ID)
                                .orEmpty(),

                            empresaNombre = documento
                                .getString(CAMPO_EMPRESA_NOMBRE)
                                .orEmpty(),

                            viajeId = documento
                                .getString(CAMPO_VIAJE_ID)
                                .orEmpty(),

                            /*
                             * Cuenta que realizó la compra.
                             */
                            usuarioId = documento
                                .getString(CAMPO_USUARIO_ID)
                                .orEmpty(),

                            usuarioCorreo = documento
                                .getString(CAMPO_USUARIO_CORREO)
                                .orEmpty(),

                            compradorDni = documento
                                .getString(CAMPO_COMPRADOR_DNI)
                                .orEmpty(),

                            compradorNombres = documento
                                .getString(CAMPO_COMPRADOR_NOMBRES)
                                .orEmpty(),

                            compradorApellidos = documento
                                .getString(CAMPO_COMPRADOR_APELLIDOS)
                                .orEmpty(),

                            compradorCelular = documento
                                .getString(CAMPO_COMPRADOR_CELULAR)
                                .orEmpty(),

                            /*
                             * Persona que ocupará el asiento.
                             */
                            pasajeroDni = documento
                                .getString(CAMPO_PASAJERO_DNI)
                                .orEmpty(),

                            pasajeroNombres = documento
                                .getString(CAMPO_PASAJERO_NOMBRES)
                                .orEmpty(),

                            pasajeroApellidos = documento
                                .getString(CAMPO_PASAJERO_APELLIDOS)
                                .orEmpty(),

                            pasajeroCelular = documento
                                .getString(CAMPO_PASAJERO_CELULAR)
                                .orEmpty(),

                            esTitular = documento
                                .getBoolean(CAMPO_ES_TITULAR)
                                ?: false,

                            datosPasajeroPendientes = documento
                                .getBoolean(
                                    CAMPO_DATOS_PASAJERO_PENDIENTES
                                ) ?: false,

                            tipoPasajero = documento
                                .getString(CAMPO_TIPO_PASAJERO)
                                .orEmpty(),

                            origen = documento
                                .getString(CAMPO_ORIGEN)
                                .orEmpty(),

                            destino = documento
                                .getString(CAMPO_DESTINO)
                                .orEmpty(),

                            asiento = documento
                                .getString(CAMPO_ASIENTO)
                                .orEmpty(),

                            precio = (
                                    documento.get(CAMPO_PRECIO)
                                            as? Number
                                    )?.toDouble() ?: 0.0,

                            estadoReserva = documento
                                .getString(CAMPO_ESTADO_RESERVA)
                                .orEmpty(),

                            estadoPago = documento
                                .getString(CAMPO_ESTADO_PAGO)
                                .orEmpty(),

                            fechaReserva = documento
                                .getTimestamp(CAMPO_FECHA_RESERVA),

                            fechaSalida = documento
                                .getString(CAMPO_FECHA_SALIDA)
                                .orEmpty(),

                            horaSalida = documento
                                .getString(CAMPO_HORA_SALIDA)
                                .orEmpty(),

                            puntoEmbarque = documento
                                .getString(CAMPO_PUNTO_EMBARQUE)
                                .orEmpty(),

                            placa = documento
                                .getString(CAMPO_PLACA)
                                .orEmpty(),

                            vehiculoDescripcion = documento
                                .getString(CAMPO_VEHICULO)
                                .orEmpty()
                        )
                    }
                    .sortedByDescending { reserva ->

                        reserva.fechaReserva?.seconds
                            ?: 0L
                    }

                reservasCompletas.clear()
                reservasCompletas.addAll(reservas)

                aplicarFiltro()
            }
    }

    private fun aplicarFiltro() {

        val reservasFiltradas =
            if (filtroActual == FILTRO_TODAS) {

                reservasCompletas.toList()

            } else {

                reservasCompletas.filter { reserva ->

                    reserva.estadoReserva
                        .trim()
                        .lowercase(Locale.ROOT) ==
                            filtroActual
                }
            }

        reservaAdapter.actualizarReservas(
            reservasFiltradas
        )

        binding.tvCantidadReservas.text =
            when (reservasCompletas.size) {

                0 -> {
                    "No tienes reservas registradas"
                }

                1 -> {
                    "Tienes 1 reserva registrada"
                }

                else -> {
                    "Tienes ${reservasCompletas.size} " +
                            "reservas registradas"
                }
            }

        val mensajeVacio =
            if (
                reservasCompletas.isNotEmpty() &&
                reservasFiltradas.isEmpty()
            ) {

                "No tienes reservas con este estado."

            } else {

                "Las reservas que realices aparecerán aquí."
            }

        mostrarSinReservas(
            mostrar = reservasFiltradas.isEmpty(),
            mensaje = mensajeVacio
        )
    }

    private fun mostrarDetalleReserva(
        reserva: Reserva
    ) {

        val nombreComprador =
            obtenerNombreCompleto(
                nombres = reserva.compradorNombres,
                apellidos = reserva.compradorApellidos
            ).ifBlank {
                "Usuario de PasajesYa"
            }

        val nombrePasajero =
            obtenerNombrePasajero(reserva)

        val tipoPasajeroTexto =
            when {

                reserva.datosPasajeroPendientes -> {
                    "Datos por completar"
                }

                reserva.esTitular -> {
                    "Titular de la cuenta"
                }

                reserva.tipoPasajero ==
                        TIPO_OTRA_PERSONA -> {

                    "Otra persona"
                }

                else -> {
                    "Pasajero"
                }
            }

        val mensaje = buildString {

            append(
                "Empresa: ${
                    reserva.empresaNombre.ifBlank {
                        "Empresa de transporte"
                    }
                }\n\n"
            )

            append(
                "Ruta: ${reserva.origen} → " +
                        "${reserva.destino}\n"
            )

            if (reserva.fechaSalida.isNotBlank()) {

                append(
                    "Fecha de salida: " +
                            "${reserva.fechaSalida}\n"
                )
            }

            if (reserva.horaSalida.isNotBlank()) {

                append(
                    "Hora de salida: " +
                            "${reserva.horaSalida}\n"
                )
            }

            if (reserva.puntoEmbarque.isNotBlank()) {

                append(
                    "Embarque: " +
                            "${reserva.puntoEmbarque}\n"
                )
            }

            append("\n")

            append(
                "Comprador: $nombreComprador\n"
            )

            if (reserva.compradorDni.isNotBlank()) {

                append(
                    "DNI del comprador: " +
                            "${reserva.compradorDni}\n"
                )
            }

            if (reserva.compradorCelular.isNotBlank()) {

                append(
                    "Celular del comprador: " +
                            "${reserva.compradorCelular}\n"
                )
            }

            append("\n")

            append(
                "Pasajero: $nombrePasajero\n"
            )

            append(
                "Tipo: $tipoPasajeroTexto\n"
            )

            if (reserva.pasajeroDni.isNotBlank()) {

                append(
                    "DNI del pasajero: " +
                            "${reserva.pasajeroDni}\n"
                )
            }

            if (reserva.pasajeroCelular.isNotBlank()) {

                append(
                    "Celular del pasajero: " +
                            "${reserva.pasajeroCelular}\n"
                )
            }

            if (reserva.datosPasajeroPendientes) {

                append(
                    "Estado de los datos: " +
                            "PENDIENTES DE COMPLETAR\n"
                )
            }

            append("\n")

            append(
                "Asiento: ${reserva.asiento}\n"
            )

            append(
                String.format(
                    Locale("es", "PE"),
                    "Precio: S/ %.2f\n",
                    reserva.precio
                )
            )

            append(
                "Reserva: ${
                    reserva.estadoReserva
                        .ifBlank {
                            "sin estado"
                        }
                        .uppercase(Locale.ROOT)
                }\n"
            )

            append(
                "Pago: ${
                    reserva.estadoPago
                        .ifBlank {
                            "pendiente"
                        }
                        .uppercase(Locale.ROOT)
                }\n"
            )

            if (
                reserva.vehiculoDescripcion.isNotBlank()
            ) {

                append(
                    "Vehículo: " +
                            "${reserva.vehiculoDescripcion}\n"
                )
            }

            if (reserva.placa.isNotBlank()) {

                append(
                    "Placa: ${reserva.placa}\n"
                )
            }

            reserva.fechaReserva?.let { fecha ->

                append(
                    "\nReservado el: " +
                            formatearFecha(fecha)
                )
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Detalle de la reserva")
            .setMessage(mensaje)
            .setPositiveButton(
                "CERRAR",
                null
            )
            .show()
    }

    private fun obtenerNombrePasajero(
        reserva: Reserva
    ): String {

        if (reserva.datosPasajeroPendientes) {
            return "Datos por completar"
        }

        val nombrePasajero =
            obtenerNombreCompleto(
                nombres = reserva.pasajeroNombres,
                apellidos = reserva.pasajeroApellidos
            )

        if (nombrePasajero.isNotBlank()) {
            return nombrePasajero
        }

        /*
         * Compatibilidad con reservas antiguas que
         * todavía no tienen campos del pasajero.
         */
        val nombreComprador =
            obtenerNombreCompleto(
                nombres = reserva.compradorNombres,
                apellidos = reserva.compradorApellidos
            )

        return nombreComprador.ifBlank {
            "Pasajero no registrado"
        }
    }

    private fun obtenerNombreCompleto(
        nombres: String,
        apellidos: String
    ): String {

        return listOf(
            nombres.trim(),
            apellidos.trim()
        )
            .filter { texto ->
                texto.isNotBlank()
            }
            .joinToString(" ")
    }

    private fun confirmarCancelacion(
        reserva: Reserva
    ) {

        val estado = reserva.estadoReserva
            .trim()
            .lowercase(Locale.ROOT)

        if (estado != ESTADO_PENDIENTE) {

            Toast.makeText(
                this,
                "Solo se pueden cancelar " +
                        "reservas pendientes.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val pasajero =
            obtenerNombrePasajero(reserva)

        MaterialAlertDialogBuilder(this)
            .setTitle("Cancelar reserva")
            .setMessage(
                "¿Deseas cancelar esta reserva?\n\n" +
                        "Ruta: ${reserva.origen} → " +
                        "${reserva.destino}\n" +
                        "Asiento: ${reserva.asiento}\n" +
                        "Pasajero: $pasajero\n\n" +
                        "El asiento volverá a quedar disponible."
            )
            .setNegativeButton(
                "NO",
                null
            )
            .setPositiveButton(
                "SÍ, CANCELAR"
            ) { _, _ ->

                cancelarReserva(reserva)
            }
            .show()
    }

    private fun cancelarReserva(
        reserva: Reserva
    ) {

        if (
            reserva.documentoPath.isBlank() ||
            reserva.empresaId.isBlank() ||
            reserva.viajeId.isBlank()
        ) {

            Toast.makeText(
                this,
                "No se pudo identificar la reserva.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        binding.progressReservas.visibility =
            View.VISIBLE

        val reservaReferencia =
            firestore.document(
                reserva.documentoPath
            )

        val viajeReferencia = firestore
            .collection(COLECCION_EMPRESAS)
            .document(reserva.empresaId)
            .collection(SUBCOLECCION_VIAJES)
            .document(reserva.viajeId)

        firestore.runTransaction { transaccion ->

            /*
             * Todas las lecturas se realizan antes
             * de comenzar las escrituras.
             */
            val documentoReserva =
                transaccion.get(
                    reservaReferencia
                )

            val documentoViaje =
                transaccion.get(
                    viajeReferencia
                )

            if (!documentoReserva.exists()) {

                throw FirebaseFirestoreException(
                    "La reserva ya no existe.",
                    FirebaseFirestoreException
                        .Code.NOT_FOUND
                )
            }

            val estadoActual = documentoReserva
                .getString(CAMPO_ESTADO_RESERVA)
                .orEmpty()
                .trim()
                .lowercase(Locale.ROOT)

            if (
                estadoActual != ESTADO_PENDIENTE
            ) {

                throw FirebaseFirestoreException(
                    "La reserva ya no está pendiente.",
                    FirebaseFirestoreException
                        .Code.ABORTED
                )
            }

            val asiento = documentoReserva
                .getString(CAMPO_ASIENTO)
                .orEmpty()
                .trim()
                .uppercase(Locale.ROOT)

            if (documentoViaje.exists()) {

                val listaActual = (
                        documentoViaje.get(
                            CAMPO_ASIENTOS_RESERVADOS
                        ) as? List<*>
                        )
                    ?.mapNotNull { valor ->
                        valor as? String
                    }
                    ?.map { codigo ->

                        codigo
                            .trim()
                            .uppercase(Locale.ROOT)
                    }
                    ?.toMutableList()
                    ?: mutableListOf()

                listaActual.removeAll { codigo ->
                    codigo == asiento
                }

                transaccion.update(
                    viajeReferencia,
                    mapOf(

                        CAMPO_ASIENTOS_RESERVADOS to
                                listaActual,

                        CAMPO_ASIENTOS_OCUPADOS to
                                listaActual.size,

                        CAMPO_ULTIMA_ACTUALIZACION to
                                FieldValue.serverTimestamp()
                    )
                )
            }

            transaccion.update(
                reservaReferencia,
                mapOf(

                    CAMPO_ESTADO_RESERVA to
                            ESTADO_CANCELADA,

                    CAMPO_FECHA_CANCELACION to
                            FieldValue.serverTimestamp()
                )
            )

            true
        }
            .addOnSuccessListener {

                binding.progressReservas.visibility =
                    View.GONE

                Toast.makeText(
                    this,
                    "La reserva fue cancelada y " +
                            "el asiento quedó disponible.",
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnFailureListener { error ->

                binding.progressReservas.visibility =
                    View.GONE

                Toast.makeText(
                    this,
                    error.localizedMessage
                        ?: "No se pudo cancelar la reserva.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun formatearFecha(
        timestamp: Timestamp
    ): String {

        val formato = SimpleDateFormat(
            "dd MMM yyyy · hh:mm a",
            Locale("es", "PE")
        )

        return formato.format(
            timestamp.toDate()
        )
    }

    private fun mostrarCargando(
        cargando: Boolean
    ) {

        binding.progressReservas.visibility =
            if (cargando) {
                View.VISIBLE
            } else {
                View.GONE
            }

        if (cargando) {

            binding.recyclerReservas.visibility =
                View.INVISIBLE

            binding.contenedorSinReservas.visibility =
                View.GONE
        }
    }

    private fun mostrarSinReservas(
        mostrar: Boolean,
        mensaje: String
    ) {

        binding.contenedorSinReservas.visibility =
            if (mostrar) {
                View.VISIBLE
            } else {
                View.GONE
            }

        binding.recyclerReservas.visibility =
            if (mostrar) {
                View.GONE
            } else {
                View.VISIBLE
            }

        binding.tvMensajeSinReservas.text =
            mensaje
    }

    private fun abrirLogin() {

        val intent = Intent(
            this,
            LoginActivity::class.java
        ).apply {

            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }

    override fun onDestroy() {

        listenerReservas?.remove()
        listenerReservas = null

        super.onDestroy()
    }

    companion object {

        private const val COLECCION_EMPRESAS =
            "empresaspasajes"

        private const val COLECCION_RESERVAS =
            "reservaspasajes"

        private const val SUBCOLECCION_VIAJES =
            "viajespasajes"

        private const val CAMPO_EMPRESA_ID =
            "empresaId"

        private const val CAMPO_EMPRESA_NOMBRE =
            "empresaNombre"

        private const val CAMPO_VIAJE_ID =
            "viajeId"

        private const val CAMPO_USUARIO_ID =
            "usuarioId"

        private const val CAMPO_USUARIO_CORREO =
            "usuarioCorreo"

        private const val CAMPO_COMPRADOR_DNI =
            "compradorDni"

        private const val CAMPO_COMPRADOR_NOMBRES =
            "compradorNombres"

        private const val CAMPO_COMPRADOR_APELLIDOS =
            "compradorApellidos"

        private const val CAMPO_COMPRADOR_CELULAR =
            "compradorCelular"

        private const val CAMPO_PASAJERO_DNI =
            "pasajeroDni"

        private const val CAMPO_PASAJERO_NOMBRES =
            "pasajeroNombres"

        private const val CAMPO_PASAJERO_APELLIDOS =
            "pasajeroApellidos"

        private const val CAMPO_PASAJERO_CELULAR =
            "pasajeroCelular"

        private const val CAMPO_ES_TITULAR =
            "esTitular"

        private const val CAMPO_DATOS_PASAJERO_PENDIENTES =
            "datosPasajeroPendientes"

        private const val CAMPO_TIPO_PASAJERO =
            "tipoPasajero"

        private const val CAMPO_ORIGEN =
            "origen"

        private const val CAMPO_DESTINO =
            "destino"

        private const val CAMPO_ASIENTO =
            "asiento"

        private const val CAMPO_PRECIO =
            "precio"

        private const val CAMPO_ESTADO_RESERVA =
            "estadoReserva"

        private const val CAMPO_ESTADO_PAGO =
            "estadoPago"

        private const val CAMPO_FECHA_RESERVA =
            "fechaReserva"

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

        private const val CAMPO_ASIENTOS_RESERVADOS =
            "asientosReservados"

        private const val CAMPO_ASIENTOS_OCUPADOS =
            "asientosOcupados"

        private const val CAMPO_ULTIMA_ACTUALIZACION =
            "ultimaActualizacion"

        private const val CAMPO_FECHA_CANCELACION =
            "fechaCancelacion"

        private const val FILTRO_TODAS =
            "todas"

        private const val ESTADO_PENDIENTE =
            "pendiente"

        private const val ESTADO_CONFIRMADA =
            "confirmada"

        private const val ESTADO_CANCELADA =
            "cancelada"

        private const val TIPO_OTRA_PERSONA =
            "otra_persona"
    }
}