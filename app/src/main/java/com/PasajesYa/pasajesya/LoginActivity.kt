package com.PasajesYa.pasajesya

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.PasajesYa.pasajesya.databinding.ActivityLoginBinding
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.Locale


class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // Credential Manager para iniciar sesión con Google
    private lateinit var credentialManager: CredentialManager


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


        // Firebase
        auth = FirebaseAuth.getInstance()

        firestore = FirebaseFirestore.getInstance()


        // Google Credential Manager
        credentialManager = CredentialManager.create(this)


        configurarPantalla()

        cargarCorreoGuardado()

        configurarEventos()
    }


    // =========================================================
    // CONFIGURACIÓN DE LA PANTALLA
    // =========================================================

    private fun configurarPantalla() {

        window.statusBarColor =
            getColor(R.color.pasajes_primary_dark)

        window.navigationBarColor =
            getColor(R.color.pasajes_background)
    }


    // =========================================================
    // EVENTOS
    // =========================================================

    private fun configurarEventos() {


        // LOGIN NORMAL
        binding.btnIngresar.setOnClickListener {

            iniciarSesion()
        }


        // LOGIN CON GOOGLE
        binding.btnGoogle.setOnClickListener {

            iniciarSesionGoogle()
        }


        // RECUPERAR CONTRASEÑA
        binding.tvOlvidePassword.setOnClickListener {

            recuperarPassword()
        }


        // REGISTRO NORMAL
        binding.tvCrearCuenta.setOnClickListener {

            val intent = Intent(
                this,
                RegisterActivity::class.java
            )

            startActivity(intent)
        }


        // ENTER EN CONTRASEÑA
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


    // =========================================================
    // LOGIN NORMAL
    // CORREO + CONTRASEÑA
    // =========================================================

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


        mostrarCargando(
            cargando = true,
            google = false
        )


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
                correo = correo,
                ingresoGoogle = false
            )
        }
    }


    // =========================================================
    // LOGIN CON GOOGLE
    // =========================================================

    private fun iniciarSesionGoogle() {


        mostrarCargando(
            cargando = true,
            google = true
        )


        /*
         * Este es el CLIENT ID web que genera automáticamente
         * google-services.json.
         *
         * NO debemos colocar manualmente el número del Client ID.
         */

        val googleIdOption = GetGoogleIdOption.Builder()

            // false = permite mostrar cuentas Google disponibles
            .setFilterByAuthorizedAccounts(false)

            // Client ID generado por google-services.json
            .setServerClientId(
                getString(R.string.default_web_client_id)
            )

            // Queremos que el usuario elija su cuenta
            .setAutoSelectEnabled(false)

            .build()


        val request = GetCredentialRequest.Builder()

            .addCredentialOption(googleIdOption)

            .build()


        lifecycleScope.launch {

            try {


                // Abre selector de cuentas Google
                val resultado = credentialManager.getCredential(
                    context = this@LoginActivity,
                    request = request
                )


                val credential = resultado.credential


                procesarCredencialGoogle(
                    credential
                )


            } catch (error: GetCredentialException) {


                mostrarCargando(false)


                /*
                 * También entra aquí si el usuario simplemente
                 * cierra la ventana de Google.
                 */

                Toast.makeText(
                    this@LoginActivity,
                    "No se pudo iniciar sesión con Google.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    // =========================================================
    // PROCESAR CREDENCIAL DE GOOGLE
    // =========================================================

    private fun procesarCredencialGoogle(
        credential: androidx.credentials.Credential
    ) {


        /*
         * Google devuelve una CustomCredential.
         */

        if (credential !is CustomCredential) {

            mostrarCargando(false)

            Toast.makeText(
                this,
                "No se pudo obtener la cuenta de Google.",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        /*
         * Comprobamos que sea realmente una credencial Google.
         */

        if (
            credential.type !=
            GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {

            mostrarCargando(false)

            Toast.makeText(
                this,
                "La credencial recibida no corresponde a Google.",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        try {


            /*
             * Convertimos la respuesta de Google
             * en un GoogleIdTokenCredential.
             */

            val googleCredential =
                GoogleIdTokenCredential.createFrom(
                    credential.data
                )


            val idToken =
                googleCredential.idToken


            /*
             * Convertimos el token de Google
             * en una credencial Firebase.
             */

            val firebaseCredential =
                GoogleAuthProvider.getCredential(
                    idToken,
                    null
                )


            autenticarGoogleFirebase(
                firebaseCredential
            )


        } catch (error: Exception) {


            mostrarCargando(false)


            Toast.makeText(
                this,
                "No se pudo procesar la cuenta de Google.",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    // =========================================================
    // AUTENTICAR GOOGLE EN FIREBASE
    // =========================================================

    private fun autenticarGoogleFirebase(
        credential: com.google.firebase.auth.AuthCredential
    ) {


        auth.signInWithCredential(
            credential
        ).addOnCompleteListener { tarea ->


            if (!tarea.isSuccessful) {


                mostrarCargando(false)


                Toast.makeText(
                    this,
                    tarea.exception?.localizedMessage
                        ?: "No se pudo iniciar sesión con Google.",
                    Toast.LENGTH_LONG
                ).show()


                return@addOnCompleteListener
            }


            val usuario = auth.currentUser


            if (usuario == null) {


                mostrarCargando(false)


                Toast.makeText(
                    this,
                    "No se pudo recuperar la cuenta de Google.",
                    Toast.LENGTH_LONG
                ).show()


                return@addOnCompleteListener
            }


            /*
             * Google ya nos entrega:
             *
             * usuario.uid
             * usuario.displayName
             * usuario.email
             * usuario.photoUrl
             *
             * Ahora comprobamos si ya existe
             * el pasajero en Firestore.
             */

            verificarPerfilUsuario(
                uid = usuario.uid,
                correo = usuario.email.orEmpty(),
                ingresoGoogle = true
            )
        }
    }


    // =========================================================
    // VERIFICAR PERFIL EN FIRESTORE
    // =========================================================

    private fun verificarPerfilUsuario(
        uid: String,
        correo: String,
        ingresoGoogle: Boolean
    ) {


        firestore.collection(COLECCION_USUARIOS)

            .document(uid)

            .get()

            .addOnSuccessListener { documento ->


                // =================================================
                // NO EXISTE PERFIL
                // =================================================

                if (!documento.exists()) {


                    /*
                     * Si entró por Google, esto NO es un error.
                     *
                     * Significa que Google ya autenticó al usuario,
                     * pero todavía falta crear su perfil PasajesYa.
                     *
                     * Lo mandamos a completar:
                     *
                     * DNI
                     * CELULAR
                     */

                    if (ingresoGoogle) {


                        abrirCompletarPerfilGoogle()


                    } else {


                        rechazarAcceso(
                            "No existe un perfil de pasajero para esta cuenta."
                        )
                    }


                    return@addOnSuccessListener
                }


                // =================================================
                // PERFIL EXISTENTE
                // =================================================


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


                val dni = documento
                    .getString(CAMPO_DNI)
                    ?.trim()
                    .orEmpty()


                val celular = documento
                    .getString(CAMPO_CELULAR)
                    ?.trim()
                    .orEmpty()


                // =================================================
                // COMPROBAR ROL
                // =================================================

                if (rol != ROL_USUARIO) {


                    rechazarAcceso(
                        "Esta cuenta no corresponde a un pasajero."
                    )


                    return@addOnSuccessListener
                }


                // =================================================
                // COMPROBAR ESTADO
                // =================================================

                if (estado != ESTADO_ACTIVO) {


                    rechazarAcceso(
                        "La cuenta se encuentra inactiva."
                    )


                    return@addOnSuccessListener
                }


                // =================================================
                // GOOGLE SIN DNI O CELULAR
                // =================================================

                /*
                 * Puede ocurrir que el documento exista,
                 * pero el usuario todavía no haya terminado
                 * su registro Google.
                 */

                if (
                    ingresoGoogle &&
                    (
                            dni.isBlank() ||
                                    celular.isBlank()
                            )
                ) {


                    abrirCompletarPerfilGoogle()


                    return@addOnSuccessListener
                }


                // =================================================
                // TODO CORRECTO
                // =================================================


                if (!ingresoGoogle) {

                    guardarCorreo(correo)
                }


                abrirSeleccionEmpresa()
            }


            .addOnFailureListener { error ->


                auth.signOut()

                mostrarCargando(false)


                Toast.makeText(
                    this,
                    "No se pudo verificar el perfil: " +
                            (
                                    error.localizedMessage
                                        ?: "error desconocido"
                                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
    }


    // =========================================================
    // IR A COMPLETAR REGISTRO GOOGLE
    // =========================================================

    private fun abrirCompletarPerfilGoogle() {


        mostrarCargando(false)


        /*
         * Esta será la NUEVA pantalla que vamos
         * a crear en el siguiente paso.
         */

        val intent = Intent(
            this,
            CompletarPerfilGoogleActivity::class.java
        )


        startActivity(intent)

        finish()
    }


    // =========================================================
    // VALIDACIÓN LOGIN NORMAL
    // =========================================================

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
            !Patterns.EMAIL_ADDRESS
                .matcher(correo)
                .matches()
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


    // =========================================================
    // RECUPERAR CONTRASEÑA
    // =========================================================

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
            !Patterns.EMAIL_ADDRESS
                .matcher(correo)
                .matches()
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


                    tarea.exception?.localizedMessage
                        ?: "No se pudo enviar el enlace de recuperación."
                }


                Toast.makeText(
                    this,
                    mensaje,
                    Toast.LENGTH_LONG
                ).show()
            }
    }


    // =========================================================
    // MOSTRAR CARGANDO
    // =========================================================

    private fun mostrarCargando(
        cargando: Boolean,
        google: Boolean = false
    ) {


        binding.btnIngresar.isEnabled = !cargando

        binding.btnGoogle.isEnabled = !cargando

        binding.etCorreo.isEnabled = !cargando

        binding.etPassword.isEnabled = !cargando

        binding.checkRecordar.isEnabled = !cargando

        binding.tvOlvidePassword.isEnabled = !cargando

        binding.tvCrearCuenta.isEnabled = !cargando


        // Carga login normal
        binding.progressLogin.visibility =

            if (
                cargando &&
                !google
            ) {

                View.VISIBLE

            } else {

                View.GONE
            }


        // Carga Google
        binding.progressGoogle.visibility =

            if (
                cargando &&
                google
            ) {

                View.VISIBLE

            } else {

                View.GONE
            }


        // Texto botón ingresar
        binding.btnIngresar.text =

            if (
                cargando &&
                !google
            ) {

                ""

            } else {

                "INGRESAR"
            }


        // Texto botón Google
        binding.btnGoogle.text =

            if (
                cargando &&
                google
            ) {

                ""

            } else {

                "Continuar con Google"
            }
    }


    // =========================================================
    // LIMPIAR ERRORES
    // =========================================================

    private fun limpiarErrores() {

        binding.tilCorreo.error = null

        binding.tilPassword.error = null
    }


    // =========================================================
    // RECHAZAR ACCESO
    // =========================================================

    private fun rechazarAcceso(
        mensaje: String
    ) {


        auth.signOut()

        mostrarCargando(false)


        Toast.makeText(
            this,
            mensaje,
            Toast.LENGTH_LONG
        ).show()
    }


    // =========================================================
    // GUARDAR CORREO
    // =========================================================

    private fun guardarCorreo(
        correo: String
    ) {


        val editor =
            preferencias.edit()


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


            editor.remove(
                CLAVE_CORREO
            )


            editor.putBoolean(
                CLAVE_RECORDAR,
                false
            )
        }


        editor.apply()
    }


    // =========================================================
    // CARGAR CORREO GUARDADO
    // =========================================================

    private fun cargarCorreoGuardado() {


        val recordar =
            preferencias.getBoolean(
                CLAVE_RECORDAR,
                false
            )


        if (!recordar) {

            return
        }


        val correoGuardado =
            preferencias.getString(
                CLAVE_CORREO,
                ""
            ).orEmpty()


        binding.etCorreo.setText(
            correoGuardado
        )


        binding.checkRecordar.isChecked =
            true


        binding.etPassword.requestFocus()
    }


    // =========================================================
    // ENTRAR A PASAJESYA
    // =========================================================

    private fun abrirSeleccionEmpresa() {


        mostrarCargando(false)


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


    // =========================================================
    // ERRORES LOGIN NORMAL
    // =========================================================

    private fun obtenerMensajeError(
        error: Exception?
    ): String {


        return when (error) {


            is FirebaseNetworkException ->

                "No se pudo conectar. Revisa tu conexión a internet."


            is FirebaseTooManyRequestsException ->

                "Demasiados intentos. Espera unos minutos."


            is FirebaseAuthInvalidUserException ->

                "No existe una cuenta registrada con este correo."


            is FirebaseAuthInvalidCredentialsException ->

                "Correo o contraseña incorrectos."


            else ->

                error?.localizedMessage
                    ?: "No se pudo iniciar sesión."
        }
    }


    // =========================================================
    // CONSTANTES
    // =========================================================

    companion object {


        private const val COLECCION_USUARIOS =
            "usuariospasajes"


        private const val CAMPO_ROL =
            "rol"


        private const val CAMPO_ESTADO =
            "estado"


        private const val CAMPO_DNI =
            "dni"


        private const val CAMPO_CELULAR =
            "celular"


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