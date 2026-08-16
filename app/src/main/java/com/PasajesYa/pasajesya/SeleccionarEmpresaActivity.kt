package com.PasajesYa.pasajesya

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.PasajesYa.pasajesya.databinding.ActivitySeleccionarEmpresaBinding
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Locale

class SeleccionarEmpresaActivity : AppCompatActivity() {

    private lateinit var binding:
            ActivitySeleccionarEmpresaBinding

    private lateinit var auth:
            FirebaseAuth

    private lateinit var firestore:
            FirebaseFirestore

    private lateinit var empresaAdapter:
            EmpresaAdapter

    private var listenerEmpresas:
            ListenerRegistration? = null


    private val preferenciasEmpresa by lazy {

        getSharedPreferences(
            PREFERENCIAS_EMPRESA,
            MODE_PRIVATE
        )
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


        auth =
            FirebaseAuth.getInstance()


        firestore =
            FirebaseFirestore.getInstance()


        // ---------------------------------------------
        // COMPROBAR SESIÓN
        // ---------------------------------------------

        if (
            auth.currentUser == null
        ) {

            abrirLogin()

            return
        }


        // ---------------------------------------------
        // VIEW BINDING
        // ---------------------------------------------

        binding =
            ActivitySeleccionarEmpresaBinding
                .inflate(
                    layoutInflater
                )


        setContentView(
            binding.root
        )


        // ---------------------------------------------
        // CONFIGURAR
        // ---------------------------------------------

        configurarPantalla()

        configurarRecycler()

        configurarEventos()

        configurarBotonAtras()

        cargarNombreUsuario()

        escucharEmpresas()
    }


    // =================================================
    // ON RESUME
    // =================================================

    override fun onResume() {

        super.onResume()

        if (::binding.isInitialized && auth.currentUser != null) {

            cargarNombreUsuario()
        }
    }


    // =================================================
    // CONFIGURAR PANTALLA
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
    // CONFIGURAR RECYCLER
    // =================================================

    private fun configurarRecycler() {

        empresaAdapter =
            EmpresaAdapter {
                    empresa ->


                confirmarSeleccionEmpresa(
                    empresa
                )
            }


        binding.recyclerEmpresas.apply {

            layoutManager =
                LinearLayoutManager(
                    this@SeleccionarEmpresaActivity
                )


            adapter =
                empresaAdapter


            setHasFixedSize(
                true
            )
        }
    }


    // =================================================
    // CONFIGURAR EVENTOS
    // =================================================

    private fun configurarEventos() {


        // ---------------------------------------------
        // BUSCAR EMPRESA
        // ---------------------------------------------

        binding.etBuscarEmpresa
            .doAfterTextChanged {
                    texto ->


                empresaAdapter.filtrar(
                    texto
                        ?.toString()
                        .orEmpty()
                )
            }


        // ---------------------------------------------
        // ABRIR MENÚ LATERAL
        // ---------------------------------------------

        binding.btnMenu
            .setOnClickListener {

                binding.drawerLayout
                    .openDrawer(
                        GravityCompat.START
                    )
            }


        // ---------------------------------------------
        // MOVILIDAD LOCAL
        // ---------------------------------------------

        binding.cardMovilidadLocal
            .setOnClickListener {

                abrirMovilidadLocal()
            }


        // ---------------------------------------------
        // ENCOMIENDA LOCAL
        // ---------------------------------------------

        binding.cardEncomiendaLocal
            .setOnClickListener {

                abrirEncomiendaLocal()
            }


        // ---------------------------------------------
        // COMPRAS Y MANDADOS
        // ---------------------------------------------

        binding.cardCompraLocal
            .setOnClickListener {

                abrirCompraLocal()
            }


        // ---------------------------------------------
        // OPCIONES DEL MENÚ
        // ---------------------------------------------

        binding.navigationView
            .setNavigationItemSelectedListener {
                    item ->


                when (
                    item.itemId
                ) {

                    // ---------------------------------
                    // MI PERFIL
                    // ---------------------------------

                    R.id.menuPerfil -> {

                        cerrarMenu()

                        abrirPerfil()

                        true
                    }


                    // ---------------------------------
                    // MIS PASAJES
                    // ---------------------------------

                    R.id.menuMisPasajes -> {

                        cerrarMenu()

                        Toast.makeText(
                            this,
                            "Mis pasajes estará disponible próximamente.",
                            Toast.LENGTH_SHORT
                        ).show()

                        true
                    }


                    // ---------------------------------
                    // MIS ENCOMIENDAS
                    // ---------------------------------

                    R.id.menuMisEncomiendas -> {

                        cerrarMenu()

                        abrirMisEncomiendas()

                        true
                    }


                    // ---------------------------------
                    // MIS COMPRAS
                    // ---------------------------------

                    R.id.menuMisCompras -> {

                        cerrarMenu()

                        Toast.makeText(
                            this,
                            "Mis compras y mandados estará disponible próximamente.",
                            Toast.LENGTH_SHORT
                        ).show()

                        true
                    }


                    // ---------------------------------
                    // AYUDA
                    // ---------------------------------

                    R.id.menuAyuda -> {

                        cerrarMenu()

                        mostrarAyuda()

                        true
                    }


                    // ---------------------------------
                    // ACERCA DE
                    // ---------------------------------

                    R.id.menuAcerca -> {

                        cerrarMenu()

                        mostrarAcercaDe()

                        true
                    }


                    // ---------------------------------
                    // CERRAR SESIÓN
                    // ---------------------------------

                    R.id.menuCerrarSesion -> {

                        cerrarMenu()

                        mostrarDialogoCerrarSesion()

                        true
                    }


                    else -> {

                        false
                    }
                }
            }
    }


    // =================================================
    // BOTÓN ATRÁS
    // =================================================

    private fun configurarBotonAtras() {

        onBackPressedDispatcher
            .addCallback(
                this
            ) {


                if (
                    binding.drawerLayout
                        .isDrawerOpen(
                            GravityCompat.START
                        )
                ) {

                    binding.drawerLayout
                        .closeDrawer(
                            GravityCompat.START
                        )

                } else {

                    finish()
                }
            }
    }


    // =================================================
    // CERRAR MENÚ
    // =================================================

    private fun cerrarMenu() {

        if (
            binding.drawerLayout
                .isDrawerOpen(
                    GravityCompat.START
                )
        ) {

            binding.drawerLayout
                .closeDrawer(
                    GravityCompat.START
                )
        }
    }


    // =================================================
    // ABRIR PERFIL
    // =================================================

    private fun abrirPerfil() {

        val intent =
            Intent(
                this,
                PerfilActivity::class.java
            )


        startActivity(
            intent
        )


        overridePendingTransition(
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
    }


    // =================================================
    // ABRIR MOVILIDAD LOCAL
    // =================================================

    private fun abrirMovilidadLocal() {

        val intent =
            Intent(
                this,
                MovilidadDisponibleActivity::class.java
            )


        startActivity(
            intent
        )


        overridePendingTransition(
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
    }


    // =================================================
    // ABRIR ENCOMIENDA LOCAL
    // =================================================

    private fun abrirEncomiendaLocal() {

        val intent =
            Intent(
                this,
                EncomiendaLocalActivity::class.java
            )


        startActivity(
            intent
        )


        overridePendingTransition(
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
    }


    // =================================================
    // ABRIR COMPRAS Y MANDADOS
    // =================================================

    private fun abrirCompraLocal() {

        val intent =
            Intent(
                this,
                CompraLocalActivity::class.java
            )


        startActivity(
            intent
        )


        overridePendingTransition(
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
    }


    // =================================================
    // ABRIR MIS ENCOMIENDAS
    // =================================================

    private fun abrirMisEncomiendas() {

        val intent =
            Intent(
                this,
                MisEncomiendasActivity::class.java
            )


        startActivity(
            intent
        )


        overridePendingTransition(
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
    }


    // =================================================
    // CARGAR DATOS DEL USUARIO
    // =================================================

    private fun cargarNombreUsuario() {

        val usuarioActual =
            auth.currentUser
                ?: return


        binding.tvSaludoUsuario.text =
            "¡Hola!"


        val nombreFirebase =
            usuarioActual.displayName
                ?.trim()
                .orEmpty()


        val fotoFirebase =
            usuarioActual.photoUrl
                ?.toString()
                .orEmpty()
                .trim()


        configurarCabeceraMenu(

            nombre =
                nombreFirebase.ifBlank {
                    "Usuario PasajesYa"
                },

            correo =
                usuarioActual.email
                    .orEmpty(),

            fotoUrl =
                fotoFirebase
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


                if (!documento.exists()) {

                    return@addOnSuccessListener
                }


                val nombres =
                    documento
                        .getString(
                            CAMPO_NOMBRES
                        )
                        .orEmpty()
                        .trim()


                val apellidos =
                    documento
                        .getString(
                            CAMPO_APELLIDOS
                        )
                        .orEmpty()
                        .trim()


                val correoFirestore =
                    documento
                        .getString(
                            CAMPO_CORREO
                        )
                        .orEmpty()
                        .trim()


                val correo =
                    if (correoFirestore.isNotBlank()) {

                        correoFirestore

                    } else {

                        usuarioActual.email
                            .orEmpty()
                    }


                val fotoFirestore =
                    documento
                        .getString(
                            CAMPO_FOTO_URL
                        )
                        .orEmpty()
                        .trim()


                val fotoUrl =
                    if (fotoFirestore.isNotBlank()) {

                        fotoFirestore

                    } else {

                        usuarioActual.photoUrl
                            ?.toString()
                            .orEmpty()
                            .trim()
                    }


                val primerNombre =
                    nombres
                        .split(" ")
                        .firstOrNull()
                        .orEmpty()


                binding.tvSaludoUsuario.text =
                    if (primerNombre.isBlank()) {

                        "¡Hola!"

                    } else {

                        "¡Hola, $primerNombre!"
                    }


                val nombreCompleto =
                    listOf(
                        nombres,
                        apellidos
                    )
                        .filter {
                            it.isNotBlank()
                        }
                        .joinToString(
                            " "
                        )
                        .ifBlank {
                            nombreFirebase
                        }


                configurarCabeceraMenu(

                    nombre =
                        nombreCompleto,

                    correo =
                        correo,

                    fotoUrl =
                        fotoUrl
                )
            }
            .addOnFailureListener {

                binding.tvSaludoUsuario.text =
                    "¡Hola!"
            }
    }


    // =================================================
    // CABECERA DEL MENÚ
    // =================================================

    private fun configurarCabeceraMenu(
        nombre: String,
        correo: String,
        fotoUrl: String
    ) {

        if (binding.navigationView.headerCount <= 0) {

            return
        }


        val headerView =
            binding.navigationView
                .getHeaderView(
                    0
                )


        val tvNombre =
            headerView
                .findViewById<TextView>(
                    R.id.tvMenuNombreUsuario
                )


        val tvCorreo =
            headerView
                .findViewById<TextView>(
                    R.id.tvMenuCorreoUsuario
                )


        val ivFoto =
            headerView
                .findViewById<ImageView>(
                    R.id.ivMenuFotoUsuario
                )


        tvNombre.text =
            nombre.ifBlank {
                "Usuario PasajesYa"
            }


        tvCorreo.text =
            correo.ifBlank {
                "PasajesYa"
            }


        if (fotoUrl.isBlank()) {

            Glide
                .with(this)
                .clear(
                    ivFoto
                )

            ivFoto
                .setImageResource(
                    R.drawable.ic_persona_pasajes
                )

        } else {

            Glide
                .with(this)
                .load(
                    fotoUrl
                )
                .placeholder(
                    R.drawable.ic_persona_pasajes
                )
                .error(
                    R.drawable.ic_persona_pasajes
                )
                .circleCrop()
                .into(
                    ivFoto
                )
        }
    }


    // =================================================
    // ESCUCHAR EMPRESAS
    // =================================================

    private fun escucharEmpresas() {

        mostrarCargando(
            true
        )


        listenerEmpresas
            ?.remove()


        listenerEmpresas =
            firestore
                .collection(
                    COLECCION_EMPRESAS
                )
                .addSnapshotListener {
                        resultado,
                        error ->


                    mostrarCargando(
                        false
                    )


                    // -------------------------------------
                    // ERROR
                    // -------------------------------------

                    if (
                        error != null
                    ) {

                        empresaAdapter
                            .actualizarEmpresas(
                                emptyList()
                            )


                        mostrarSinEmpresas(

                            mostrar =
                                true,

                            mensaje =
                                "No se pudieron cargar las empresas. " +
                                        "Revisa tu conexión a internet."
                        )


                        Toast.makeText(
                            this,
                            error.localizedMessage
                                ?: "Error al cargar las empresas.",
                            Toast.LENGTH_LONG
                        ).show()


                        return@addSnapshotListener
                    }


                    // -------------------------------------
                    // LEER EMPRESAS
                    // -------------------------------------

                    val empresas =
                        resultado
                            ?.documents
                            ?.mapNotNull {
                                    documento ->


                                documento
                                    .toObject(
                                        Empresa::class.java
                                    )
                                    ?.copy(
                                        id =
                                            documento.id
                                    )
                            }
                            ?.filter {
                                    empresa ->


                                val estadoEmpresa =
                                    empresa
                                        .estado
                                        .trim()
                                        .lowercase(
                                            Locale.ROOT
                                        )


                                estadoEmpresa ==
                                        ESTADO_ACTIVO &&
                                        empresa
                                            .visibleUsuarios
                            }
                            ?.sortedWith(

                                compareBy<Empresa> {
                                        empresa ->

                                    empresa.orden

                                }.thenBy {
                                        empresa ->

                                    empresa
                                        .nombre
                                        .lowercase(
                                            Locale.ROOT
                                        )
                                }
                            )
                            .orEmpty()


                    empresaAdapter
                        .actualizarEmpresas(
                            empresas
                        )


                    mostrarSinEmpresas(

                        mostrar =
                            empresas.isEmpty(),

                        mensaje =
                            "Pronto se agregarán nuevas " +
                                    "empresas de transporte."
                    )
                }
    }


    // =================================================
    // CONFIRMAR EMPRESA
    // =================================================

    private fun confirmarSeleccionEmpresa(
        empresa: Empresa
    ) {

        if (
            empresa.id.isBlank()
        ) {

            Toast.makeText(
                this,
                "No se pudo identificar esta empresa.",
                Toast.LENGTH_SHORT
            ).show()


            return
        }


        MaterialAlertDialogBuilder(
            this
        )
            .setTitle(

                empresa
                    .nombre
                    .ifBlank {

                        "Empresa de transporte"
                    }
            )
            .setMessage(

                "¿Deseas consultar las rutas, viajes y " +
                        "autos disponibles de esta empresa?"
            )
            .setNegativeButton(
                "CANCELAR",
                null
            )
            .setPositiveButton(
                "CONTINUAR"
            ) {
                    _, _ ->


                seleccionarEmpresa(
                    empresa
                )
            }
            .show()
    }


    // =================================================
    // SELECCIONAR EMPRESA
    // =================================================

    private fun seleccionarEmpresa(
        empresa: Empresa
    ) {

        guardarEmpresaSeleccionadaLocalmente(
            empresa
        )


        guardarUltimaEmpresaEnFirestore(
            empresa
        )
    }


    // =================================================
    // GUARDAR EMPRESA LOCALMENTE
    // =================================================

    private fun guardarEmpresaSeleccionadaLocalmente(
        empresa: Empresa
    ) {

        preferenciasEmpresa
            .edit()

            .putString(
                CLAVE_EMPRESA_ID,
                empresa.id
            )

            .putString(
                CLAVE_EMPRESA_NOMBRE,
                empresa.nombre
            )

            .putString(
                CLAVE_EMPRESA_COLOR_PRINCIPAL,
                empresa.colorPrincipal
            )

            .putString(
                CLAVE_EMPRESA_COLOR_SECUNDARIO,
                empresa.colorSecundario
            )

            .putString(
                CLAVE_EMPRESA_LOGO,
                empresa.logoUrl
            )

            .apply()
    }


    // =================================================
    // GUARDAR ULTIMA EMPRESA FIRESTORE
    // =================================================

    private fun guardarUltimaEmpresaEnFirestore(
        empresa: Empresa
    ) {

        val usuarioActual =
            auth.currentUser


        if (
            usuarioActual == null
        ) {

            abrirLogin()

            return
        }


        val datosActualizacion =
            mapOf(

                CAMPO_ULTIMA_EMPRESA_ID to
                        empresa.id,

                CAMPO_ULTIMA_EMPRESA_NOMBRE to
                        empresa.nombre
            )


        firestore
            .collection(
                COLECCION_USUARIOS
            )
            .document(
                usuarioActual.uid
            )
            .update(
                datosActualizacion
            )
            .addOnCompleteListener {

                abrirEmpresa(
                    empresa
                )
            }
    }


    // =================================================
    // ABRIR EMPRESA
    // =================================================

    private fun abrirEmpresa(
        empresa: Empresa
    ) {

        val intent =
            Intent(
                this,
                MainActivity::class.java
            )
                .apply {


                    putExtra(
                        EXTRA_EMPRESA_ID,
                        empresa.id
                    )


                    putExtra(
                        EXTRA_EMPRESA_NOMBRE,
                        empresa.nombre
                    )


                    putExtra(
                        EXTRA_EMPRESA_COLOR_PRINCIPAL,
                        empresa.colorPrincipal
                    )


                    putExtra(
                        EXTRA_EMPRESA_COLOR_SECUNDARIO,
                        empresa.colorSecundario
                    )


                    putExtra(
                        EXTRA_EMPRESA_LOGO,
                        empresa.logoUrl
                    )


                    flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                }


        startActivity(
            intent
        )


        overridePendingTransition(
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )


        finish()
    }


    // =================================================
    // AYUDA
    // =================================================

    private fun mostrarAyuda() {

        MaterialAlertDialogBuilder(
            this
        )
            .setTitle(
                "Ayuda"
            )
            .setMessage(
                "Desde PasajesYa puedes consultar viajes, " +
                        "encontrar movilidad local, solicitar encomiendas " +
                        "y realizar compras o mandados."
            )
            .setPositiveButton(
                "ENTENDIDO",
                null
            )
            .show()
    }


    // =================================================
    // ACERCA DE PASAJESYA
    // =================================================

    private fun mostrarAcercaDe() {

        MaterialAlertDialogBuilder(
            this
        )
            .setTitle(
                "PasajesYa"
            )
            .setMessage(
                "PasajesYa conecta a usuarios con empresas de transporte " +
                        "y servicios de movilidad local."
            )
            .setPositiveButton(
                "CERRAR",
                null
            )
            .show()
    }


    // =================================================
    // MOSTRAR CARGANDO
    // =================================================

    private fun mostrarCargando(
        cargando: Boolean
    ) {

        binding.progressEmpresas.visibility =
            if (
                cargando
            ) {

                View.VISIBLE

            } else {

                View.GONE
            }


        if (
            cargando
        ) {

            binding.recyclerEmpresas.visibility =
                View.INVISIBLE


            binding.contenedorSinEmpresas.visibility =
                View.GONE
        }
    }


    // =================================================
    // MOSTRAR SIN EMPRESAS
    // =================================================

    private fun mostrarSinEmpresas(
        mostrar: Boolean,
        mensaje: String
    ) {

        binding.contenedorSinEmpresas.visibility =
            if (
                mostrar
            ) {

                View.VISIBLE

            } else {

                View.GONE
            }


        binding.recyclerEmpresas.visibility =
            if (
                mostrar
            ) {

                View.GONE

            } else {

                View.VISIBLE
            }


        binding.tvMensajeSinEmpresas.text =
            mensaje
    }


    // =================================================
    // DIALOGO CERRAR SESION
    // =================================================

    private fun mostrarDialogoCerrarSesion() {

        MaterialAlertDialogBuilder(
            this
        )
            .setTitle(
                "Cerrar sesión"
            )
            .setMessage(
                "¿Deseas salir de PasajesYa?"
            )
            .setNegativeButton(
                "CANCELAR",
                null
            )
            .setPositiveButton(
                "SALIR"
            ) {
                    _, _ ->


                cerrarSesion()
            }
            .show()
    }


    // =================================================
    // CERRAR SESION
    // =================================================

    private fun cerrarSesion() {

        listenerEmpresas
            ?.remove()


        listenerEmpresas =
            null


        auth.signOut()


        preferenciasEmpresa
            .edit()
            .clear()
            .apply()


        abrirLogin()
    }


    // =================================================
    // ABRIR LOGIN
    // =================================================

    private fun abrirLogin() {

        val intent =
            Intent(
                this,
                LoginActivity::class.java
            )
                .apply {

                    flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                }


        startActivity(
            intent
        )


        finish()
    }


    // =================================================
    // ON DESTROY
    // =================================================

    override fun onDestroy() {

        listenerEmpresas
            ?.remove()


        listenerEmpresas =
            null


        super.onDestroy()
    }


    // =================================================
    // CONSTANTES
    // =================================================

    companion object {

        private const val COLECCION_EMPRESAS =
            "empresaspasajes"


        private const val COLECCION_USUARIOS =
            "usuariospasajes"


        private const val CAMPO_NOMBRES =
            "nombres"


        private const val CAMPO_APELLIDOS =
            "apellidos"


        private const val CAMPO_CORREO =
            "correo"


        private const val CAMPO_FOTO_URL =
            "fotoUrl"


        private const val CAMPO_ULTIMA_EMPRESA_ID =
            "ultimaEmpresaId"


        private const val CAMPO_ULTIMA_EMPRESA_NOMBRE =
            "ultimaEmpresaNombre"


        private const val ESTADO_ACTIVO =
            "activo"


        private const val PREFERENCIAS_EMPRESA =
            "preferencias_empresa_pasajes"


        private const val CLAVE_EMPRESA_ID =
            "empresa_id"


        private const val CLAVE_EMPRESA_NOMBRE =
            "empresa_nombre"


        private const val CLAVE_EMPRESA_COLOR_PRINCIPAL =
            "empresa_color_principal"


        private const val CLAVE_EMPRESA_COLOR_SECUNDARIO =
            "empresa_color_secundario"


        private const val CLAVE_EMPRESA_LOGO =
            "empresa_logo"


        const val EXTRA_EMPRESA_ID =
            "extra_empresa_id"


        const val EXTRA_EMPRESA_NOMBRE =
            "extra_empresa_nombre"


        const val EXTRA_EMPRESA_COLOR_PRINCIPAL =
            "extra_empresa_color_principal"


        const val EXTRA_EMPRESA_COLOR_SECUNDARIO =
            "extra_empresa_color_secundario"


        const val EXTRA_EMPRESA_LOGO =
            "extra_empresa_logo"
    }
}