package com.PasajesYa.pasajesya

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.PasajesYa.pasajesya.databinding.ActivityPerfilBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class PerfilActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPerfilBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private var perfilCargado = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        if (auth.currentUser == null) {
            abrirLogin()
            return
        }

        binding = ActivityPerfilBinding.inflate(
            layoutInflater
        )

        setContentView(binding.root)

        configurarPantalla()
        configurarEventos()
        cargarPerfil()
    }

    private fun configurarPantalla() {

        window.statusBarColor =
            getColor(R.color.pasajes_primary_dark)

        window.navigationBarColor =
            getColor(R.color.pasajes_background)
    }

    private fun configurarEventos() {

        binding.btnVolver.setOnClickListener {
            finish()
        }

        binding.btnGuardar.setOnClickListener {
            guardarCambios()
        }

        binding.btnCambiarPassword.setOnClickListener {
            confirmarCambioPassword()
        }

        binding.etCelular.setOnEditorActionListener {
                _, actionId, _ ->

            if (actionId == EditorInfo.IME_ACTION_DONE) {
                guardarCambios()
                true
            } else {
                false
            }
        }

        binding.etNombres.setOnFocusChangeListener { _, tieneFoco ->

            if (tieneFoco) {
                binding.tilNombres.error = null
            }
        }

        binding.etApellidos.setOnFocusChangeListener { _, tieneFoco ->

            if (tieneFoco) {
                binding.tilApellidos.error = null
            }
        }

        binding.etCelular.setOnFocusChangeListener { _, tieneFoco ->

            if (tieneFoco) {
                binding.tilCelular.error = null
            }
        }
    }

    private fun cargarPerfil() {

        val usuarioActual = auth.currentUser

        if (usuarioActual == null) {
            abrirLogin()
            return
        }

        mostrarCargando(true)

        firestore.collection(COLECCION_USUARIOS)
            .document(usuarioActual.uid)
            .get()
            .addOnSuccessListener { documento ->

                mostrarCargando(false)

                if (!documento.exists()) {

                    Toast.makeText(
                        this,
                        "No se encontró el perfil del pasajero.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addOnSuccessListener
                }

                val nombres = documento
                    .getString(CAMPO_NOMBRES)
                    .orEmpty()
                    .trim()

                val apellidos = documento
                    .getString(CAMPO_APELLIDOS)
                    .orEmpty()
                    .trim()

                val dni = documento
                    .getString(CAMPO_DNI)
                    .orEmpty()
                    .trim()

                val celular = documento
                    .getString(CAMPO_CELULAR)
                    .orEmpty()
                    .trim()

                val correoFirestore = documento
                    .getString(CAMPO_CORREO)
                    .orEmpty()
                    .trim()

                val correo = usuarioActual.email
                    .orEmpty()
                    .ifBlank {
                        correoFirestore
                    }

                binding.etNombres.setText(nombres)
                binding.etApellidos.setText(apellidos)
                binding.etDni.setText(dni)
                binding.etCelular.setText(celular)
                binding.etCorreo.setText(correo)

                actualizarEncabezado(
                    nombres = nombres,
                    apellidos = apellidos,
                    correo = correo
                )

                perfilCargado = true
            }
            .addOnFailureListener { error ->

                mostrarCargando(false)

                Toast.makeText(
                    this,
                    "No se pudo cargar el perfil: " +
                            (error.localizedMessage
                                ?: "error desconocido"),
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun guardarCambios() {

        if (!perfilCargado) {

            Toast.makeText(
                this,
                "Espera mientras se carga tu perfil.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        limpiarErrores()

        val nombres = binding.etNombres.text
            ?.toString()
            ?.trim()
            .orEmpty()

        val apellidos = binding.etApellidos.text
            ?.toString()
            ?.trim()
            .orEmpty()

        val celular = binding.etCelular.text
            ?.toString()
            ?.trim()
            .orEmpty()

        if (
            !validarFormulario(
                nombres = nombres,
                apellidos = apellidos,
                celular = celular
            )
        ) {
            return
        }

        val usuarioActual = auth.currentUser

        if (usuarioActual == null) {
            abrirLogin()
            return
        }

        mostrarCargando(true)

        val datosActualizados = mapOf<String, Any>(
            CAMPO_NOMBRES to nombres,
            CAMPO_APELLIDOS to apellidos,
            CAMPO_CELULAR to celular,
            CAMPO_FECHA_ACTUALIZACION to
                    FieldValue.serverTimestamp()
        )

        firestore.collection(COLECCION_USUARIOS)
            .document(usuarioActual.uid)
            .update(datosActualizados)
            .addOnSuccessListener {

                mostrarCargando(false)

                actualizarEncabezado(
                    nombres = nombres,
                    apellidos = apellidos,
                    correo = usuarioActual.email.orEmpty()
                )

                setResult(RESULT_OK)

                Toast.makeText(
                    this,
                    "Tu perfil fue actualizado correctamente.",
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnFailureListener { error ->

                mostrarCargando(false)

                Toast.makeText(
                    this,
                    "No se pudieron guardar los cambios: " +
                            (error.localizedMessage
                                ?: "error desconocido"),
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun validarFormulario(
        nombres: String,
        apellidos: String,
        celular: String
    ): Boolean {

        var formularioValido = true

        if (nombres.isBlank()) {

            binding.tilNombres.error =
                "Ingresa tus nombres"

            formularioValido = false

        } else if (nombres.length < 2) {

            binding.tilNombres.error =
                "Ingresa un nombre válido"

            formularioValido = false
        }

        if (apellidos.isBlank()) {

            binding.tilApellidos.error =
                "Ingresa tus apellidos"

            formularioValido = false

        } else if (apellidos.length < 2) {

            binding.tilApellidos.error =
                "Ingresa apellidos válidos"

            formularioValido = false
        }

        if (celular.isBlank()) {

            binding.tilCelular.error =
                "Ingresa tu número de celular"

            formularioValido = false

        } else if (celular.length != 9) {

            binding.tilCelular.error =
                "El celular debe tener 9 dígitos"

            formularioValido = false

        } else if (!celular.startsWith("9")) {

            binding.tilCelular.error =
                "Ingresa un celular válido"

            formularioValido = false
        }

        return formularioValido
    }

    private fun confirmarCambioPassword() {

        val correo = auth.currentUser
            ?.email
            .orEmpty()

        if (correo.isBlank()) {

            Toast.makeText(
                this,
                "La cuenta no tiene un correo registrado.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Cambiar contraseña")
            .setMessage(
                "Enviaremos un enlace de recuperación a:\n\n" +
                        correo
            )
            .setNegativeButton(
                "CANCELAR",
                null
            )
            .setPositiveButton(
                "ENVIAR"
            ) { _, _ ->

                enviarEnlacePassword(correo)
            }
            .show()
    }

    private fun enviarEnlacePassword(
        correo: String
    ) {

        binding.btnCambiarPassword.isEnabled = false

        auth.setLanguageCode("es")

        auth.sendPasswordResetEmail(correo)
            .addOnCompleteListener { tarea ->

                binding.btnCambiarPassword.isEnabled = true

                if (tarea.isSuccessful) {

                    Toast.makeText(
                        this,
                        "Revisa tu correo para cambiar la contraseña.",
                        Toast.LENGTH_LONG
                    ).show()

                } else {

                    val mensaje =
                        if (
                            tarea.exception
                                    is FirebaseNetworkException
                        ) {

                            "Revisa tu conexión a internet."

                        } else {

                            tarea.exception?.localizedMessage
                                ?: "No se pudo enviar el enlace."
                        }

                    Toast.makeText(
                        this,
                        mensaje,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun actualizarEncabezado(
        nombres: String,
        apellidos: String,
        correo: String
    ) {

        val nombreCompleto = listOf(
            nombres.trim(),
            apellidos.trim()
        )
            .filter { texto ->
                texto.isNotBlank()
            }
            .joinToString(" ")

        binding.tvNombrePerfil.text =
            nombreCompleto.ifBlank {
                "Pasajero"
            }

        binding.tvCorreoPerfil.text =
            correo.ifBlank {
                "Correo no disponible"
            }
    }

    private fun limpiarErrores() {

        binding.tilNombres.error = null
        binding.tilApellidos.error = null
        binding.tilCelular.error = null
    }

    private fun mostrarCargando(
        cargando: Boolean
    ) {

        binding.progressPerfil.visibility =
            if (cargando) {
                View.VISIBLE
            } else {
                View.GONE
            }

        binding.btnGuardar.text =
            if (cargando) {
                ""
            } else {
                "GUARDAR CAMBIOS"
            }

        binding.btnGuardar.isEnabled =
            !cargando

        binding.btnVolver.isEnabled =
            !cargando

        binding.btnCambiarPassword.isEnabled =
            !cargando

        binding.etNombres.isEnabled =
            !cargando

        binding.etApellidos.isEnabled =
            !cargando

        binding.etCelular.isEnabled =
            !cargando
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

    companion object {

        private const val COLECCION_USUARIOS =
            "usuariospasajes"

        private const val CAMPO_NOMBRES =
            "nombres"

        private const val CAMPO_APELLIDOS =
            "apellidos"

        private const val CAMPO_DNI =
            "dni"

        private const val CAMPO_CELULAR =
            "celular"

        private const val CAMPO_CORREO =
            "correo"

        private const val CAMPO_FECHA_ACTUALIZACION =
            "fechaActualizacion"
    }
}