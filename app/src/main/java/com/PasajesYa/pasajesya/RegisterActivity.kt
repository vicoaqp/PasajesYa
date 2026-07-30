package com.PasajesYa.pasajesya

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import android.content.Intent
import android.util.Patterns
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import com.PasajesYa.pasajesya.databinding.ActivityRegisterBinding
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale




class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        configurarPantalla()
        configurarEventos()
    }

    private fun configurarPantalla() {

        window.statusBarColor =
            getColor(R.color.pasajes_primary_dark)

        window.navigationBarColor =
            getColor(R.color.pasajes_background)
    }

    private fun configurarEventos() {

        binding.btnRegistrar.setOnClickListener {
            registrarUsuario()
        }

        binding.tvIniciarSesion.setOnClickListener {
            volverAlLogin()
        }

        binding.etConfirmarPassword.setOnEditorActionListener {
                _, actionId, _ ->

            if (actionId == EditorInfo.IME_ACTION_DONE) {
                registrarUsuario()
                true
            } else {
                false
            }
        }

        binding.checkTerminos.setOnCheckedChangeListener { _, marcado ->

            if (marcado) {
                binding.tvErrorTerminos.visibility = View.GONE
            }
        }

        configurarLimpiezaErrores()
    }

    private fun configurarLimpiezaErrores() {

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

        binding.etDni.setOnFocusChangeListener { _, tieneFoco ->
            if (tieneFoco) {
                binding.tilDni.error = null
            }
        }

        binding.etCelular.setOnFocusChangeListener { _, tieneFoco ->
            if (tieneFoco) {
                binding.tilCelular.error = null
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

        binding.etConfirmarPassword.setOnFocusChangeListener { _, tieneFoco ->
            if (tieneFoco) {
                binding.tilConfirmarPassword.error = null
            }
        }
    }

    private fun registrarUsuario() {

        limpiarErrores()

        val nombres = binding.etNombres.text
            ?.toString()
            ?.trim()
            .orEmpty()

        val apellidos = binding.etApellidos.text
            ?.toString()
            ?.trim()
            .orEmpty()

        val dni = binding.etDni.text
            ?.toString()
            ?.trim()
            .orEmpty()

        val celular = binding.etCelular.text
            ?.toString()
            ?.trim()
            .orEmpty()

        val correo = binding.etCorreo.text
            ?.toString()
            ?.trim()
            ?.lowercase(Locale.ROOT)
            .orEmpty()

        val password = binding.etPassword.text
            ?.toString()
            .orEmpty()

        val confirmarPassword = binding.etConfirmarPassword.text
            ?.toString()
            .orEmpty()

        val aceptaTerminos = binding.checkTerminos.isChecked

        val formularioValido = validarFormulario(
            nombres = nombres,
            apellidos = apellidos,
            dni = dni,
            celular = celular,
            correo = correo,
            password = password,
            confirmarPassword = confirmarPassword,
            aceptaTerminos = aceptaTerminos
        )

        if (!formularioValido) {
            return
        }

        mostrarCargando(true)

        verificarDniRegistrado(
            nombres = nombres,
            apellidos = apellidos,
            dni = dni,
            celular = celular,
            correo = correo,
            password = password
        )
    }

    private fun verificarDniRegistrado(
        nombres: String,
        apellidos: String,
        dni: String,
        celular: String,
        correo: String,
        password: String
    ) {

        firestore.collection(COLECCION_USUARIOS)
            .whereEqualTo(CAMPO_DNI, dni)
            .limit(1)
            .get()
            .addOnSuccessListener { resultado ->

                if (!resultado.isEmpty) {

                    mostrarCargando(false)

                    binding.tilDni.error =
                        "Este DNI ya se encuentra registrado"

                    binding.etDni.requestFocus()
                    return@addOnSuccessListener
                }

                crearCuentaFirebase(
                    nombres = nombres,
                    apellidos = apellidos,
                    dni = dni,
                    celular = celular,
                    correo = correo,
                    password = password
                )
            }
            .addOnFailureListener { error ->

                mostrarCargando(false)

                Toast.makeText(
                    this,
                    "No se pudo verificar el DNI: " +
                            (error.localizedMessage
                                ?: "error desconocido"),
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun crearCuentaFirebase(
        nombres: String,
        apellidos: String,
        dni: String,
        celular: String,
        correo: String,
        password: String
    ) {

        auth.createUserWithEmailAndPassword(
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

            val usuarioFirebase = auth.currentUser

            if (usuarioFirebase == null) {

                mostrarCargando(false)

                Toast.makeText(
                    this,
                    "No se pudo recuperar la cuenta creada.",
                    Toast.LENGTH_LONG
                ).show()

                return@addOnCompleteListener
            }

            guardarPerfilFirestore(
                uid = usuarioFirebase.uid,
                nombres = nombres,
                apellidos = apellidos,
                dni = dni,
                celular = celular,
                correo = correo
            )
        }
    }

    private fun guardarPerfilFirestore(
        uid: String,
        nombres: String,
        apellidos: String,
        dni: String,
        celular: String,
        correo: String
    ) {

        val datosUsuario = hashMapOf<String, Any>(
            CAMPO_UID to uid,
            CAMPO_NOMBRES to nombres,
            CAMPO_APELLIDOS to apellidos,
            CAMPO_DNI to dni,
            CAMPO_CELULAR to celular,
            CAMPO_CORREO to correo,
            CAMPO_ROL to ROL_USUARIO,
            CAMPO_ESTADO to ESTADO_ACTIVO,
            CAMPO_FOTO_URL to "",
            CAMPO_ULTIMA_EMPRESA to "",
            CAMPO_FECHA_REGISTRO to FieldValue.serverTimestamp()
        )

        firestore.collection(COLECCION_USUARIOS)
            .document(uid)
            .set(datosUsuario)
            .addOnSuccessListener {

                mostrarCargando(false)

                Toast.makeText(
                    this,
                    "Tu cuenta fue creada correctamente.",
                    Toast.LENGTH_LONG
                ).show()

                abrirSeleccionEmpresa()
            }
            .addOnFailureListener { error ->

                /*
                 * Si Authentication creó la cuenta, pero Firestore
                 * no pudo guardar el perfil, eliminamos la cuenta
                 * recién creada para evitar un usuario incompleto.
                 */
                val usuarioActual = auth.currentUser

                usuarioActual?.delete()
                    ?.addOnCompleteListener {

                        auth.signOut()
                        mostrarCargando(false)

                        Toast.makeText(
                            this,
                            "No se pudo completar el registro: " +
                                    (error.localizedMessage
                                        ?: "error desconocido"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
    }

    private fun validarFormulario(
        nombres: String,
        apellidos: String,
        dni: String,
        celular: String,
        correo: String,
        password: String,
        confirmarPassword: String,
        aceptaTerminos: Boolean
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

        if (dni.isBlank()) {

            binding.tilDni.error =
                "Ingresa tu DNI"

            formularioValido = false

        } else if (dni.length != 8) {

            binding.tilDni.error =
                "El DNI debe tener 8 dígitos"

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
                "Ingresa un número celular válido"

            formularioValido = false
        }

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
                "Ingresa una contraseña"

            formularioValido = false

        } else if (password.length < 6) {

            binding.tilPassword.error =
                "Utiliza al menos 6 caracteres"

            formularioValido = false
        }

        if (confirmarPassword.isBlank()) {

            binding.tilConfirmarPassword.error =
                "Confirma tu contraseña"

            formularioValido = false

        } else if (password != confirmarPassword) {

            binding.tilConfirmarPassword.error =
                "Las contraseñas no coinciden"

            formularioValido = false
        }

        if (!aceptaTerminos) {

            binding.tvErrorTerminos.visibility =
                View.VISIBLE

            formularioValido = false
        }

        return formularioValido
    }

    private fun mostrarCargando(cargando: Boolean) {

        binding.btnRegistrar.isEnabled = !cargando
        binding.tvIniciarSesion.isEnabled = !cargando
        binding.checkTerminos.isEnabled = !cargando

        binding.etNombres.isEnabled = !cargando
        binding.etApellidos.isEnabled = !cargando
        binding.etDni.isEnabled = !cargando
        binding.etCelular.isEnabled = !cargando
        binding.etCorreo.isEnabled = !cargando
        binding.etPassword.isEnabled = !cargando
        binding.etConfirmarPassword.isEnabled = !cargando

        binding.progressRegistro.visibility =
            if (cargando) {
                View.VISIBLE
            } else {
                View.GONE
            }

        binding.btnRegistrar.text =
            if (cargando) {
                ""
            } else {
                "CREAR CUENTA"
            }
    }

    private fun limpiarErrores() {

        binding.tilNombres.error = null
        binding.tilApellidos.error = null
        binding.tilDni.error = null
        binding.tilCelular.error = null
        binding.tilCorreo.error = null
        binding.tilPassword.error = null
        binding.tilConfirmarPassword.error = null

        binding.tvErrorTerminos.visibility =
            View.GONE
    }

    private fun abrirSeleccionEmpresa() {

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

    private fun volverAlLogin() {

        val intent = Intent(
            this,
            LoginActivity::class.java
        )

        startActivity(intent)
        finish()
    }

    private fun obtenerMensajeError(
        error: Exception?
    ): String {

        return when (error) {

            is FirebaseAuthUserCollisionException ->
                "Este correo ya se encuentra registrado."

            is FirebaseAuthWeakPasswordException ->
                "La contraseña es demasiado débil."

            is FirebaseNetworkException ->
                "No se pudo conectar. Revisa tu internet."

            is FirebaseTooManyRequestsException ->
                "Demasiados intentos. Espera unos minutos."

            else ->
                error?.localizedMessage
                    ?: "No se pudo crear la cuenta."
        }
    }

    companion object {

        private const val COLECCION_USUARIOS =
            "usuariospasajes"

        private const val CAMPO_UID =
            "uid"

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

        private const val CAMPO_ROL =
            "rol"

        private const val CAMPO_ESTADO =
            "estado"

        private const val CAMPO_FOTO_URL =
            "fotoUrl"

        private const val CAMPO_ULTIMA_EMPRESA =
            "ultimaEmpresaId"

        private const val CAMPO_FECHA_REGISTRO =
            "fechaRegistro"

        private const val ROL_USUARIO =
            "usuario"

        private const val ESTADO_ACTIVO =
            "activo"
    }
}