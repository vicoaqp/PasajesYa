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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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

        if (auth.currentUser == null) {
            abrirLogin()
            return
        }

        binding =
            ActivitySeleccionarEmpresaBinding.inflate(
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
            layoutManager =
                LinearLayoutManager(
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

        val usuario = auth.currentUser ?: return

        binding.tvSaludoUsuario.text = "¡Hola!"

        firestore.collection(COLECCION_USUARIOS)
            .document(usuario.uid)
            .get()
            .addOnSuccessListener { documento ->

                val nombres = documento
                    .getString("nombres")
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
    }

    private fun escucharEmpresas() {

        mostrarCargando(true)

        listenerEmpresas =
            firestore.collection(COLECCION_EMPRESAS)
                .addSnapshotListener { resultado, error ->

                    mostrarCargando(false)

                    if (error != null) {

                        mostrarSinEmpresas(
                            true,
                            "No se pudieron cargar las empresas. " +
                                    "Revisa tu conexión."
                        )

                        Toast.makeText(
                            this,
                            error.localizedMessage
                                ?: "Error al cargar empresas",
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

                            empresa.estado
                                .trim()
                                .lowercase(Locale.ROOT) ==
                                    ESTADO_ACTIVO &&
                                    empresa.visibleUsuarios
                        }
                        ?.sortedWith(
                            compareBy<Empresa> {
                                it.orden
                            }.thenBy {
                                it.nombre
                            }
                        )
                        .orEmpty()

                    empresaAdapter.actualizarEmpresas(
                        empresas
                    )

                    mostrarSinEmpresas(
                        empresas.isEmpty(),
                        "Pronto se agregarán nuevas " +
                                "empresas de transporte."
                    )
                }
    }

    private fun confirmarSeleccionEmpresa(
        empresa: Empresa
    ) {

        MaterialAlertDialogBuilder(this)
            .setTitle(empresa.nombre)
            .setMessage(
                "¿Deseas consultar las rutas y autos " +
                        "disponibles de esta empresa?"
            )
            .setNegativeButton(
                "CANCELAR",
                null
            )
            .setPositiveButton(
                "CONTINUAR"
            ) { _, _ ->

                guardarEmpresaSeleccionada(empresa)
                abrirEmpresa(empresa)
            }
            .show()
    }

    private fun guardarEmpresaSeleccionada(
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
                CLAVE_EMPRESA_COLOR,
                empresa.colorPrincipal
            )
            .putString(
                CLAVE_EMPRESA_LOGO,
                empresa.logoUrl
            )
            .apply()
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

            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
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

        binding.recyclerEmpresas.visibility =
            if (cargando) {
                View.INVISIBLE
            } else {
                View.VISIBLE
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

                auth.signOut()

                preferenciasEmpresa.edit()
                    .clear()
                    .apply()

                abrirLogin()
            }
            .show()
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
        super.onDestroy()
    }

    companion object {

        private const val COLECCION_EMPRESAS =
            "empresas"

        private const val COLECCION_USUARIOS =
            "usuariosapp"

        private const val ESTADO_ACTIVO =
            "activo"

        private const val PREFERENCIAS_EMPRESA =
            "preferencias_empresa"

        private const val CLAVE_EMPRESA_ID =
            "empresa_id"

        private const val CLAVE_EMPRESA_NOMBRE =
            "empresa_nombre"

        private const val CLAVE_EMPRESA_COLOR =
            "empresa_color"

        private const val CLAVE_EMPRESA_LOGO =
            "empresa_logo"

        const val EXTRA_EMPRESA_ID =
            "extra_empresa_id"

        const val EXTRA_EMPRESA_NOMBRE =
            "extra_empresa_nombre"
    }
}