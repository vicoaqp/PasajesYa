package com.PasajesYa.pasajesya

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.util.Patterns
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import com.PasajesYa.pasajesya.databinding.ActivityLoginBinding

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseNetworkException
import com.google.firebase.auth.FirebaseTooManyRequestsException
import com.google.firebase.firestore.FirebaseFirestore
import com.pasajesya.usuario.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private val preferencias by lazy {
        getSharedPreferences(
            PREFERENCIAS_LOGIN,
            MODE_PRIVATE
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        configurarPantalla()
        cargarCorreoGuardado()
        configurarEventos()
    }

    private fun configurarPantalla() {
        window.statusBarColor =
            getColor(R.color.pasajes_primary_dark)

        window.navigationBarColor =
            getColor(R.color.pasajes_background)
    }

    private fun configurarEventos() {

        binding.btnIngresar.setOnClickListener {
            iniciarSesion()
        }

        binding.tvOlvidePassword.setOnClickListener {
            recuperarPassword()
        }

        binding.tvCrearCuenta.setOnClickListener {
            Toast.makeText(
                this,
                "En el siguiente paso crearemos el registro.",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.etPassword.setOnEditorActionListener { _, actionId, _ ->

            if (actionId == EditorInfo.IME_ACTION_DONE) {
                iniciarSesion()
                true
            } else {
                false
            }
        }

        binding.etCorreo.setOnFocusChangeListener { _, tieneFoco ->
            if (tieneFoco) {
                binding.tilCorreo.error = null
            }
        }

        binding.etPassword.setOnFocusChangeListener { _, tieneFoco ->
            if (tieneFoco) {
                binding.tilPassword.error = null
            }
        }
    }

    private fun iniciarSesion() {

        limpiarErrores()

        val correo = binding.etCorreo.text
            ?.toString()
            ?.trim()
            .orEmpty()

        val password = binding.etPassword.text
            ?.toString()
            .orEmpty()

        if (!validarFormulario(correo, password)) {
            return
        }

        mostrarCargando(true)

        auth.signInWithEmailAndPassword(
            correo,
            password
        ).addOnCompleteListener { tarea ->

            if (!tarea.isSuccessful) {

                mostrarCargando(false)

                Toast.makeText(
                    this,
                    obtenerMensajeError(tarea.exception),
                    Toast.LENGTH_LONG
                ).show()

                return@addOnCompleteListener
            }

            val usuarioActual = auth.currentUser

            if (usuarioActual == null) {

                mostrarCargando(false)

                Toast.makeText(
                    this,
                    "No se pudo recuperar la cuenta.",
                    Toast.LENGTH_LONG
                ).show()

                return@addOnCompleteListener
            }

            verificarPerfilUsuario(
                uid = usuarioActual.uid,
                correo = correo
            )
        }
    }

    private fun verificarPerfilUsuario(
        uid: String,
        correo: String
    ) {

        firestore.collection(COLECCION_USUARIOS)
            .document(uid)
            .get()
            .addOnSuccessListener { documento ->

                if (!documento.exists()) {

                    rechazarAcceso(
                        "No existe un perfil de pasajero para esta cuenta."
                    )

                    return@addOnSuccessListener
                }

                val rol = documento
                    .getString(CAMPO_ROL)
                    ?.trim()
                    ?.lowercase(Locale.ROOT)
                    .orEmpty()

                val estado = documento
                    .getString(CAMPO_ESTADO)
                    ?.trim()
                    ?.lowercase(Locale.ROOT)
                    .orEmpty()

                when {

                    rol != ROL_USUARIO -> {

                        rechazarAcceso(
                            "Esta cuenta no corresponde a un pasajero."
                        )
                    }

                    estado != ESTADO_ACTIVO -> {

                        rechazarAcceso(
                            "La cuenta se encuentra inactiva."
                        )
                    }

                    else -> {

                        guardarCorreo(correo)
                        abrirPantallaPrincipal()
                    }
                }
            }
            .addOnFailureListener { error ->

                auth.signOut()
                mostrarCargando(false)

                Toast.makeText(
                    this,
                    "No se pudo verificar el perfil: " +
                            error.localizedMessage,
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun validarFormulario(
        correo: String,
        password: String
    ): Boolean {

        var formularioValido = true

        if (correo.isBlank()) {

            binding.tilCorreo.error =
                "Ingresa tu correo electrónico"

            formularioValido = false

        } else if (
            !Patterns.EMAIL_ADDRESS.matcher(correo).matches()
        ) {

            binding.tilCorreo.error =
                "Ingresa un correo válido"

            formularioValido = false
        }

        if (password.isBlank()) {

            binding.tilPassword.error =
                "Ingresa tu contraseña"

            formularioValido = false

        } else if (password.length < 6) {

            binding.tilPassword.error =
                "La contraseña debe tener al menos 6 caracteres"

            formularioValido = false
        }

        return formularioValido
    }

    private fun recuperarPassword() {

        limpiarErrores()

        val correo = binding.etCorreo.text
            ?.toString()
            ?.trim()
            .orEmpty()

        if (correo.isBlank()) {

            binding.tilCorreo.error =
                "Primero ingresa tu correo"

            binding.etCorreo.requestFocus()
            return
        }

        if (
            !Patterns.EMAIL_ADDRESS.matcher(correo).matches()
        ) {

            binding.tilCorreo.error =
                "Ingresa un correo válido"

            binding.etCorreo.requestFocus()
            return
        }

        binding.tvOlvidePassword.isEnabled = false

        auth.sendPasswordResetEmail(correo)
            .addOnCompleteListener { tarea ->

                binding.tvOlvidePassword.isEnabled = true

                val mensaje = if (tarea.isSuccessful) {

                    "Revisa tu correo para recuperar la contraseña."

                } else {

                    "No se pudo enviar el enlace de recuperación."
                }

                Toast.makeText(
                    this,
                    mensaje,
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun mostrarCargando(cargando: Boolean) {

        binding.btnIngresar.isEnabled = !cargando
        binding.etCorreo.isEnabled = !cargando
        binding.etPassword.isEnabled = !cargando
        binding.checkRecordar.isEnabled = !cargando
        binding.tvOlvidePassword.isEnabled = !cargando
        binding.tvCrearCuenta.isEnabled = !cargando

        binding.progressLogin.visibility =
            if (cargando) {
                View.VISIBLE
            } else {
                View.GONE
            }

        binding.btnIngresar.text =
            if (cargando) {
                ""
            } else {
                "INGRESAR"
            }
    }

    private fun limpiarErrores() {
        binding.tilCorreo.error = null
        binding.tilPassword.error = null
    }

    private fun rechazarAcceso(mensaje: String) {

        auth.signOut()
        mostrarCargando(false)

        Toast.makeText(
            this,
            mensaje,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun guardarCorreo(correo: String) {

        val editor = preferencias.edit()

        if (binding.checkRecordar.isChecked) {

            editor.putString(
                CLAVE_CORREO,
                correo
            )

            editor.putBoolean(
                CLAVE_RECORDAR,
                true
            )

        } else {

            editor.remove(CLAVE_CORREO)

            editor.putBoolean(
                CLAVE_RECORDAR,
                false
            )
        }

        editor.apply()
    }

    private fun cargarCorreoGuardado() {

        val recordar = preferencias.getBoolean(
            CLAVE_RECORDAR,
            false
        )

        if (!recordar) {
            return
        }

        val correoGuardado = preferencias.getString(
            CLAVE_CORREO,
            ""
        ).orEmpty()

        binding.etCorreo.setText(correoGuardado)
        binding.checkRecordar.isChecked = true
        binding.etPassword.requestFocus()
    }

    private fun abrirPantallaPrincipal() {

        mostrarCargando(false)

        /*
         * Temporalmente abrimos MainActivity.
         * Cuando creemos SeleccionEmpresaActivity,
         * reemplazaremos MainActivity por esa pantalla.
         */
        val intent = Intent(
            this,
            MainActivity::class.java
        ).apply {

            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }

    private fun obtenerMensajeError(
        error: Exception?
    ): String {

        return when (error) {

            is FirebaseNetworkException ->
                "No se pudo conectar. Revisa tu internet."

            is FirebaseTooManyRequestsException ->
                "Demasiados intentos. Espera unos minutos."

            else ->
                "Correo o contraseña incorrectos."
        }
    }

    companion object {

        private const val COLECCION_USUARIOS =
            "usuariosapp"

        private const val CAMPO_ROL =
            "rol"

        private const val CAMPO_ESTADO =
            "estado"

        private const val ROL_USUARIO =
            "usuario"

        private const val ESTADO_ACTIVO =
            "activo"

        private const val PREFERENCIAS_LOGIN =
            "preferencias_login_pasajes"

        private const val CLAVE_CORREO =
            "correo_guardado"

        private const val CLAVE_RECORDAR =
            "recordar_correo"
    }
}