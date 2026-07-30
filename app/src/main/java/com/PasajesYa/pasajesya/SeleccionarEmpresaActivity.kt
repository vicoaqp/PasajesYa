package com.PasajesYa.pasajesya

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.PasajesYa.pasajesya.databinding.ActivitySeleccionarEmpresaBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

import java.util.Locale

class SeleccionarEmpresaActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySeleccionarEmpresaBinding

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var empresaAdapter: EmpresaAdapter

    private var listenerEmpresas: ListenerRegistration? = null

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

        // Si no existe sesión, vuelve al login.
        if (auth.currentUser == null) {
            abrirLogin()
            return
        }

        binding = ActivitySeleccionarEmpresaBinding.inflate(
            layoutInflater
        )

        setContentView(binding.root)

        configurarPantalla()
        configurarRecycler()
        configurarEventos()
        cargarNombreUsuario()
        escucharEmpresas()
    }

    private fun configurarPantalla() {

        window.statusBarColor =
            getColor(R.color.pasajes_primary_dark)

        window.navigationBarColor =
            getColor(R.color.pasajes_background)
    }

    private fun configurarRecycler() {

        empresaAdapter = EmpresaAdapter { empresa ->
            confirmarSeleccionEmpresa(empresa)
        }

        binding.recyclerEmpresas.apply {

            layoutManager = LinearLayoutManager(
                this@SeleccionarEmpresaActivity
            )

            adapter = empresaAdapter
            setHasFixedSize(true)
        }
    }

    private fun configurarEventos() {

        binding.etBuscarEmpresa.doAfterTextChanged { texto ->

            empresaAdapter.filtrar(
                texto?.toString().orEmpty()
            )
        }

        binding.btnCerrarSesion.setOnClickListener {
            mostrarDialogoCerrarSesion()
        }
    }

    private fun cargarNombreUsuario() {

        val usuarioActual = auth.currentUser ?: return

        binding.tvSaludoUsuario.text = "¡Hola!"

        firestore.collection(COLECCION_USUARIOS)
            .document(usuarioActual.uid)
            .get()
            .addOnSuccessListener { documento ->

                if (!documento.exists()) {
                    return@addOnSuccessListener
                }

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

                binding.tvSaludoUsuario.text = "¡Hola!"
            }
    }

    private fun escucharEmpresas() {

        mostrarCargando(true)

        listenerEmpresas?.remove()

        listenerEmpresas = firestore
            .collection(COLECCION_EMPRESAS)
            .addSnapshotListener { resultado, error ->

                mostrarCargando(false)

                if (error != null) {

                    empresaAdapter.actualizarEmpresas(
                        emptyList()
                    )

                    mostrarSinEmpresas(
                        mostrar = true,
                        mensaje = "No se pudieron cargar las empresas. " +
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

                val empresas = resultado
                    ?.documents
                    ?.mapNotNull { documento ->

                        documento
                            .toObject(Empresa::class.java)
                            ?.copy(id = documento.id)
                    }
                    ?.filter { empresa ->

                        val estadoEmpresa = empresa.estado
                            .trim()
                            .lowercase(Locale.ROOT)

                        estadoEmpresa == ESTADO_ACTIVO &&
                                empresa.visibleUsuarios
                    }
                    ?.sortedWith(
                        compareBy<Empresa> { empresa ->
                            empresa.orden
                        }.thenBy { empresa ->
                            empresa.nombre.lowercase(Locale.ROOT)
                        }
                    )
                    .orEmpty()

                empresaAdapter.actualizarEmpresas(empresas)

                mostrarSinEmpresas(
                    mostrar = empresas.isEmpty(),
                    mensaje = "Pronto se agregarán nuevas " +
                            "empresas de transporte."
                )
            }
    }

    private fun confirmarSeleccionEmpresa(
        empresa: Empresa
    ) {

        if (empresa.id.isBlank()) {

            Toast.makeText(
                this,
                "No se pudo identificar esta empresa.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(
                empresa.nombre.ifBlank {
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
            ) { _, _ ->

                seleccionarEmpresa(empresa)
            }
            .show()
    }

    private fun seleccionarEmpresa(
        empresa: Empresa
    ) {

        guardarEmpresaSeleccionadaLocalmente(empresa)
        guardarUltimaEmpresaEnFirestore(empresa)
    }

    private fun guardarEmpresaSeleccionadaLocalmente(
        empresa: Empresa
    ) {

        preferenciasEmpresa.edit()
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

    private fun guardarUltimaEmpresaEnFirestore(
        empresa: Empresa
    ) {

        val usuarioActual = auth.currentUser

        if (usuarioActual == null) {
            abrirLogin()
            return
        }

        val datosActualizacion = mapOf(
            CAMPO_ULTIMA_EMPRESA_ID to empresa.id,
            CAMPO_ULTIMA_EMPRESA_NOMBRE to empresa.nombre
        )

        firestore.collection(COLECCION_USUARIOS)
            .document(usuarioActual.uid)
            .update(datosActualizacion)
            .addOnCompleteListener {

                /*
                 * Aunque falle esta actualización,
                 * la empresa ya está guardada en el teléfono.
                 */
                abrirEmpresa(empresa)
            }
    }

    private fun abrirEmpresa(
        empresa: Empresa
    ) {

        val intent = Intent(
            this,
            MainActivity::class.java
        ).apply {

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

        startActivity(intent)

        overridePendingTransition(
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )

        finish()
    }

    private fun mostrarCargando(
        cargando: Boolean
    ) {

        binding.progressEmpresas.visibility =
            if (cargando) {
                View.VISIBLE
            } else {
                View.GONE
            }

        if (cargando) {

            binding.recyclerEmpresas.visibility =
                View.INVISIBLE

            binding.contenedorSinEmpresas.visibility =
                View.GONE
        }
    }

    private fun mostrarSinEmpresas(
        mostrar: Boolean,
        mensaje: String
    ) {

        binding.contenedorSinEmpresas.visibility =
            if (mostrar) {
                View.VISIBLE
            } else {
                View.GONE
            }

        binding.recyclerEmpresas.visibility =
            if (mostrar) {
                View.GONE
            } else {
                View.VISIBLE
            }

        binding.tvMensajeSinEmpresas.text = mensaje
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

        listenerEmpresas?.remove()
        listenerEmpresas = null

        auth.signOut()

        preferenciasEmpresa.edit()
            .clear()
            .apply()

        abrirLogin()
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

        listenerEmpresas?.remove()
        listenerEmpresas = null

        super.onDestroy()
    }

    companion object {

        private const val COLECCION_EMPRESAS =
            "empresaspasajes"

        private const val COLECCION_USUARIOS =
            "usuariospasajes"

        private const val CAMPO_NOMBRES =
            "nombres"

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