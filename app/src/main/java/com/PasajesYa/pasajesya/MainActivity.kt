package com.PasajesYa.pasajesya

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.PasajesYa.pasajesya.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Locale



class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var viajeAdapter: ViajeAdapter

    private var listenerViajes: ListenerRegistration? = null

    private var empresaId = ""
    private var empresaNombre = ""
    private var empresaTelefono = ""
    private var empresaColorPrincipal = COLOR_PREDETERMINADO

    private val preferenciasEmpresa by lazy {
        getSharedPreferences(
            PREFERENCIAS_EMPRESA,
            MODE_PRIVATE
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        if (auth.currentUser == null) {
            abrirLogin()
            return
        }

        recuperarEmpresaSeleccionada()

        if (empresaId.isBlank()) {
            abrirSeleccionEmpresa()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarPantalla()
        configurarRecycler()
        configurarEventos()
        mostrarDatosTemporales()
        cargarNombreUsuario()
        cargarEmpresa()
        escucharViajes()
    }

    private fun recuperarEmpresaSeleccionada() {

        empresaId = intent.getStringExtra(
            SeleccionarEmpresaActivity.EXTRA_EMPRESA_ID
        ).orEmpty()

        empresaNombre = intent.getStringExtra(
            SeleccionarEmpresaActivity.EXTRA_EMPRESA_NOMBRE
        ).orEmpty()

        empresaColorPrincipal = intent.getStringExtra(
            SeleccionarEmpresaActivity.EXTRA_EMPRESA_COLOR_PRINCIPAL
        ).orEmpty()

        if (empresaId.isBlank()) {
            empresaId = preferenciasEmpresa.getString(
                CLAVE_EMPRESA_ID,
                ""
            ).orEmpty()
        }

        if (empresaNombre.isBlank()) {
            empresaNombre = preferenciasEmpresa.getString(
                CLAVE_EMPRESA_NOMBRE,
                ""
            ).orEmpty()
        }

        if (empresaColorPrincipal.isBlank()) {
            empresaColorPrincipal = preferenciasEmpresa.getString(
                CLAVE_EMPRESA_COLOR_PRINCIPAL,
                COLOR_PREDETERMINADO
            ).orEmpty()
        }

        if (empresaColorPrincipal.isBlank()) {
            empresaColorPrincipal = COLOR_PREDETERMINADO
        }
    }

    private fun configurarPantalla() {

        window.statusBarColor =
            getColor(R.color.pasajes_primary_dark)

        window.navigationBarColor =
            getColor(R.color.pasajes_background)
    }

    private fun configurarRecycler() {

        viajeAdapter = ViajeAdapter { viaje ->
            mostrarDetalleReserva(viaje)
        }

        viajeAdapter.actualizarColorEmpresa(
            empresaColorPrincipal
        )

        binding.recyclerViajes.apply {

            layoutManager = LinearLayoutManager(
                this@MainActivity
            )

            adapter = viajeAdapter
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
        }
    }

    private fun configurarEventos() {

        binding.btnCambiarEmpresa.setOnClickListener {
            mostrarDialogoCambiarEmpresa()
        }

        binding.btnCerrarSesion.setOnClickListener {
            mostrarDialogoCerrarSesion()
        }

        binding.btnMisReservas.setOnClickListener {
            abrirMisReservas()
        }

        binding.btnPerfil.setOnClickListener {

            val intent = Intent(
                this,
                PerfilActivity::class.java
            )

            startActivity(intent)
        }

        binding.btnContactar.setOnClickListener {
            contactarEmpresa()
        }
    }

    private fun mostrarDatosTemporales() {

        binding.tvNombreEmpresa.text =
            empresaNombre.ifBlank {
                "Empresa de transporte"
            }

        binding.tvDescripcionEmpresa.text =
            "Servicio de transporte de pasajeros"

        binding.tvTelefonoEmpresa.text =
            "Teléfono: no disponible"

        binding.tvDireccionEmpresa.text =
            "Dirección: no disponible"

        binding.btnContactar.isEnabled = false

        aplicarColorEmpresa(
            empresaColorPrincipal
        )
    }

    private fun cargarNombreUsuario() {

        val usuarioActual = auth.currentUser ?: return

        binding.tvSaludoUsuario.text = "¡Hola!"

        firestore.collection(COLECCION_USUARIOS)
            .document(usuarioActual.uid)
            .get()
            .addOnSuccessListener { documento ->

                val nombres = documento
                    .getString(CAMPO_NOMBRES)
                    .orEmpty()
                    .trim()

                val primerNombre = nombres
                    .split(" ")
                    .firstOrNull()
                    .orEmpty()

                binding.tvSaludoUsuario.text =
                    if (primerNombre.isBlank()) {
                        "¡Hola!"
                    } else {
                        "¡Hola, $primerNombre!"
                    }
            }
            .addOnFailureListener {

                binding.tvSaludoUsuario.text =
                    "¡Hola!"
            }
    }

    private fun cargarEmpresa() {

        firestore.collection(COLECCION_EMPRESAS)
            .document(empresaId)
            .get()
            .addOnSuccessListener { documento ->

                if (!documento.exists()) {

                    Toast.makeText(
                        this,
                        "La empresa seleccionada no existe.",
                        Toast.LENGTH_LONG
                    ).show()

                    abrirSeleccionEmpresa()
                    return@addOnSuccessListener
                }

                val estado = documento
                    .getString(CAMPO_ESTADO)
                    .orEmpty()
                    .trim()
                    .lowercase(Locale.ROOT)

                val visibleUsuarios =
                    documento.getBoolean(
                        CAMPO_VISIBLE_USUARIOS
                    ) ?: false

                if (
                    estado != ESTADO_ACTIVO ||
                    !visibleUsuarios
                ) {

                    Toast.makeText(
                        this,
                        "Esta empresa no está disponible.",
                        Toast.LENGTH_LONG
                    ).show()

                    abrirSeleccionEmpresa()
                    return@addOnSuccessListener
                }

                empresaNombre = documento
                    .getString(CAMPO_NOMBRE)
                    .orEmpty()
                    .ifBlank {
                        empresaNombre
                    }

                empresaTelefono = documento
                    .getString(CAMPO_TELEFONO)
                    .orEmpty()
                    .trim()

                val descripcion = documento
                    .getString(CAMPO_DESCRIPCION)
                    .orEmpty()
                    .trim()

                val direccion = documento
                    .getString(CAMPO_DIRECCION)
                    .orEmpty()
                    .trim()

                empresaColorPrincipal = documento
                    .getString(CAMPO_COLOR_PRINCIPAL)
                    .orEmpty()
                    .ifBlank {
                        COLOR_PREDETERMINADO
                    }

                binding.tvNombreEmpresa.text =
                    empresaNombre.ifBlank {
                        "Empresa de transporte"
                    }

                binding.tvDescripcionEmpresa.text =
                    descripcion.ifBlank {
                        "Servicio de transporte de pasajeros"
                    }

                binding.tvTelefonoEmpresa.text =
                    if (empresaTelefono.isBlank()) {
                        "Teléfono: no disponible"
                    } else {
                        "Teléfono: $empresaTelefono"
                    }

                binding.tvDireccionEmpresa.text =
                    if (direccion.isBlank()) {
                        "Dirección: no disponible"
                    } else {
                        "Dirección: $direccion"
                    }

                binding.btnContactar.isEnabled =
                    empresaTelefono.isNotBlank()

                aplicarColorEmpresa(
                    empresaColorPrincipal
                )

                guardarEmpresaEnPreferencias()
            }
            .addOnFailureListener { error ->

                Toast.makeText(
                    this,
                    "Error al cargar la empresa: " +
                            (error.localizedMessage
                                ?: "error desconocido"),
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun escucharViajes() {

        mostrarCargandoViajes(true)

        listenerViajes?.remove()
        listenerViajes = null

        listenerViajes = firestore
            .collection(COLECCION_EMPRESAS)
            .document(empresaId)
            .collection(SUBCOLECCION_VIAJES)
            .addSnapshotListener { resultado, error ->

                mostrarCargandoViajes(false)

                if (error != null) {

                    viajeAdapter.actualizarViajes(
                        emptyList()
                    )

                    binding.tvCantidadViajes.text =
                        "0 disponibles"

                    mostrarSinViajes(
                        mostrar = true,
                        mensaje = "No se pudieron cargar los viajes.\n" +
                                (error.localizedMessage
                                    ?: "Error desconocido")
                    )

                    Toast.makeText(
                        this,
                        "Error al cargar viajes: " +
                                (error.localizedMessage
                                    ?: "error desconocido"),
                        Toast.LENGTH_LONG
                    ).show()

                    return@addSnapshotListener
                }

                if (resultado == null) {

                    viajeAdapter.actualizarViajes(
                        emptyList()
                    )

                    binding.tvCantidadViajes.text =
                        "0 disponibles"

                    mostrarSinViajes(
                        mostrar = true,
                        mensaje = "No se recibió información de los viajes."
                    )

                    return@addSnapshotListener
                }

                val viajes = resultado.documents
                    .map { documento ->

                        Viaje(
                            id = documento.id,

                            rutaId = documento
                                .getString(CAMPO_RUTA_ID)
                                .orEmpty(),

                            origen = documento
                                .getString(CAMPO_ORIGEN)
                                .orEmpty(),

                            destino = documento
                                .getString(CAMPO_DESTINO)
                                .orEmpty(),

                            fechaSalida = documento
                                .getString(CAMPO_FECHA_SALIDA)
                                .orEmpty(),

                            horaSalida = documento
                                .getString(CAMPO_HORA_SALIDA)
                                .orEmpty(),

                            puntoEmbarque = documento
                                .getString(CAMPO_PUNTO_EMBARQUE)
                                .orEmpty(),

                            choferNombre = documento
                                .getString(CAMPO_CHOFER)
                                .orEmpty(),

                            vehiculoDescripcion = documento
                                .getString(CAMPO_VEHICULO)
                                .orEmpty(),

                            placa = documento
                                .getString(CAMPO_PLACA)
                                .orEmpty(),

                            capacidad = (
                                    documento.get(CAMPO_CAPACIDAD)
                                            as? Number
                                    )?.toLong() ?: 4L,

                            asientosOcupados = (
                                    documento.get(CAMPO_ASIENTOS_OCUPADOS)
                                            as? Number
                                    )?.toLong() ?: 0L,

                            precio = (
                                    documento.get(CAMPO_PRECIO)
                                            as? Number
                                    )?.toDouble() ?: 0.0,

                            estado = documento
                                .getString(CAMPO_ESTADO)
                                .orEmpty(),

                            orden = (
                                    documento.get(CAMPO_ORDEN)
                                            as? Number
                                    )?.toLong() ?: 0L
                        )
                    }
                    .filter { viaje ->

                        val estadoViaje = viaje.estado
                            .trim()
                            .lowercase(Locale.ROOT)

                        estadoViaje == ESTADO_RECIBIENDO ||
                                estadoViaje == ESTADO_DISPONIBLE ||
                                estadoViaje == ESTADO_ACTIVO
                    }
                    .sortedWith(
                        compareBy<Viaje> {
                            it.orden
                        }.thenBy {
                            it.origen
                        }
                    )

                viajeAdapter.actualizarViajes(
                    viajes
                )

                binding.tvCantidadViajes.text =
                    when (viajes.size) {

                        0 ->
                            "0 disponibles"

                        1 ->
                            "1 disponible"

                        else ->
                            "${viajes.size} disponibles"
                    }

                mostrarSinViajes(
                    mostrar = viajes.isEmpty(),
                    mensaje = "La empresa todavía no ha activado " +
                            "una unidad para recibir pasajeros."
                )
            }
    }

    private fun mostrarDetalleReserva(
        viaje: Viaje
    ) {

        val disponibles =
            viaje.asientosDisponibles()

        if (disponibles <= 0L) {

            Toast.makeText(
                this,
                "Esta unidad ya está completa.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val mensaje = buildString {

            append(
                "${viaje.origen} → ${viaje.destino}\n\n"
            )

            append(
                "Asientos disponibles: $disponibles\n"
            )

            append(
                String.format(
                    Locale("es", "PE"),
                    "Precio: S/ %.2f",
                    viaje.precio
                )
            )

            if (viaje.fechaSalida.isNotBlank()) {
                append("\nFecha: ${viaje.fechaSalida}")
            }

            if (viaje.horaSalida.isNotBlank()) {
                append("\nSalida: ${viaje.horaSalida}")
            }

            if (viaje.puntoEmbarque.isNotBlank()) {
                append(
                    "\nEmbarque: ${viaje.puntoEmbarque}"
                )
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Reservar asiento")
            .setMessage(mensaje)
            .setNegativeButton(
                "CANCELAR",
                null
            )
            .setPositiveButton(
                "CONTINUAR"
            ) { _, _ ->

                abrirSeleccionAsiento(viaje)
            }
            .show()
    }

    private fun abrirSeleccionAsiento(
        viaje: Viaje
    ) {

        if (viaje.id.isBlank()) {

            Toast.makeText(
                this,
                "No se pudo identificar el viaje.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val intent = Intent(
            this,
            SeleccionarAsientoActivity::class.java
        ).apply {

            putExtra(
                SeleccionarAsientoActivity.EXTRA_EMPRESA_ID,
                empresaId
            )

            putExtra(
                SeleccionarAsientoActivity.EXTRA_EMPRESA_NOMBRE,
                empresaNombre
            )

            putExtra(
                SeleccionarAsientoActivity.EXTRA_VIAJE_ID,
                viaje.id
            )
        }

        startActivity(intent)
    }

    private fun abrirMisReservas() {

        val intent = Intent(
            this,
            MisReservasActivity::class.java
        )

        startActivity(intent)
    }

    private fun aplicarColorEmpresa(
        colorHexadecimal: String
    ) {

        val color = try {

            Color.parseColor(
                colorHexadecimal
            )

        } catch (_: IllegalArgumentException) {

            Color.parseColor(
                COLOR_PREDETERMINADO
            )
        }

        binding.cardEncabezado.setCardBackgroundColor(
            color
        )

        binding.btnContactar.backgroundTintList =
            ColorStateList.valueOf(color)

        binding.tvCantidadViajes.setTextColor(
            color
        )

        viajeAdapter.actualizarColorEmpresa(
            colorHexadecimal
        )

        window.statusBarColor =
            oscurecerColor(color)
    }

    private fun oscurecerColor(
        color: Int
    ): Int {

        val factor = 0.75f

        val rojo =
            (Color.red(color) * factor).toInt()

        val verde =
            (Color.green(color) * factor).toInt()

        val azul =
            (Color.blue(color) * factor).toInt()

        return Color.rgb(
            rojo,
            verde,
            azul
        )
    }

    private fun mostrarCargandoViajes(
        cargando: Boolean
    ) {

        binding.progressViajes.visibility =
            if (cargando) {
                View.VISIBLE
            } else {
                View.GONE
            }

        if (cargando) {

            binding.recyclerViajes.visibility =
                View.GONE

            binding.contenedorSinViajes.visibility =
                View.GONE
        }
    }

    private fun mostrarSinViajes(
        mostrar: Boolean,
        mensaje: String
    ) {

        binding.contenedorSinViajes.visibility =
            if (mostrar) {
                View.VISIBLE
            } else {
                View.GONE
            }

        binding.recyclerViajes.visibility =
            if (mostrar) {
                View.GONE
            } else {
                View.VISIBLE
            }

        binding.tvMensajeSinViajes.text =
            mensaje
    }

    private fun contactarEmpresa() {

        if (empresaTelefono.isBlank()) {

            Toast.makeText(
                this,
                "La empresa no registró un teléfono.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val telefonoLimpio = empresaTelefono
            .replace(" ", "")
            .replace("-", "")

        val intent = Intent(
            Intent.ACTION_DIAL,
            Uri.parse("tel:$telefonoLimpio")
        )

        startActivity(intent)
    }

    private fun guardarEmpresaEnPreferencias() {

        preferenciasEmpresa.edit()
            .putString(
                CLAVE_EMPRESA_ID,
                empresaId
            )
            .putString(
                CLAVE_EMPRESA_NOMBRE,
                empresaNombre
            )
            .putString(
                CLAVE_EMPRESA_COLOR_PRINCIPAL,
                empresaColorPrincipal
            )
            .apply()
    }

    private fun mostrarDialogoCambiarEmpresa() {

        MaterialAlertDialogBuilder(this)
            .setTitle("Cambiar empresa")
            .setMessage(
                "Actualmente estás consultando " +
                        "$empresaNombre. " +
                        "¿Deseas elegir otra empresa?"
            )
            .setNegativeButton(
                "CANCELAR",
                null
            )
            .setPositiveButton(
                "CAMBIAR"
            ) { _, _ ->

                abrirSeleccionEmpresa()
            }
            .show()
    }

    private fun mostrarDialogoCerrarSesion() {

        MaterialAlertDialogBuilder(this)
            .setTitle("Cerrar sesión")
            .setMessage(
                "¿Deseas salir de PasajesYa?"
            )
            .setNegativeButton(
                "CANCELAR",
                null
            )
            .setPositiveButton(
                "SALIR"
            ) { _, _ ->

                cerrarSesion()
            }
            .show()
    }

    private fun cerrarSesion() {

        listenerViajes?.remove()
        listenerViajes = null

        auth.signOut()

        preferenciasEmpresa.edit()
            .clear()
            .apply()

        abrirLogin()
    }

    private fun abrirSeleccionEmpresa() {

        listenerViajes?.remove()
        listenerViajes = null

        val intent = Intent(
            this,
            SeleccionarEmpresaActivity::class.java
        ).apply {

            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
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


    override fun onResume() {
        super.onResume()

        if (::binding.isInitialized) {
            cargarNombreUsuario()
        }
    }


    override fun onDestroy() {

        listenerViajes?.remove()
        listenerViajes = null

        super.onDestroy()
    }

    companion object {

        private const val COLECCION_EMPRESAS =
            "empresaspasajes"

        private const val COLECCION_USUARIOS =
            "usuariospasajes"

        private const val SUBCOLECCION_VIAJES =
            "viajespasajes"

        private const val CAMPO_NOMBRE =
            "nombre"

        private const val CAMPO_NOMBRES =
            "nombres"

        private const val CAMPO_DESCRIPCION =
            "descripcion"

        private const val CAMPO_TELEFONO =
            "telefono"

        private const val CAMPO_DIRECCION =
            "direccion"

        private const val CAMPO_COLOR_PRINCIPAL =
            "colorPrincipal"

        private const val CAMPO_VISIBLE_USUARIOS =
            "visibleUsuarios"

        private const val CAMPO_RUTA_ID =
            "rutaId"

        private const val CAMPO_ORIGEN =
            "origen"

        private const val CAMPO_DESTINO =
            "destino"

        private const val CAMPO_FECHA_SALIDA =
            "fechaSalida"

        private const val CAMPO_HORA_SALIDA =
            "horaSalida"

        private const val CAMPO_PUNTO_EMBARQUE =
            "puntoEmbarque"

        private const val CAMPO_CHOFER =
            "choferNombre"

        private const val CAMPO_VEHICULO =
            "vehiculoDescripcion"

        private const val CAMPO_PLACA =
            "placa"

        private const val CAMPO_CAPACIDAD =
            "capacidad"

        private const val CAMPO_ASIENTOS_OCUPADOS =
            "asientosOcupados"

        private const val CAMPO_PRECIO =
            "precio"

        private const val CAMPO_ESTADO =
            "estado"

        private const val CAMPO_ORDEN =
            "orden"

        private const val ESTADO_ACTIVO =
            "activo"

        private const val ESTADO_DISPONIBLE =
            "disponible"

        private const val ESTADO_RECIBIENDO =
            "recibiendo_pasajeros"

        private const val PREFERENCIAS_EMPRESA =
            "preferencias_empresa_pasajes"

        private const val CLAVE_EMPRESA_ID =
            "empresa_id"

        private const val CLAVE_EMPRESA_NOMBRE =
            "empresa_nombre"

        private const val CLAVE_EMPRESA_COLOR_PRINCIPAL =
            "empresa_color_principal"

        private const val COLOR_PREDETERMINADO =
            "#1261A6"
    }
}