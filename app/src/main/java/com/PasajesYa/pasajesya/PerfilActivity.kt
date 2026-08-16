package com.PasajesYa.pasajesya

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.PasajesYa.pasajesya.databinding.ActivityPerfilBinding
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore


class PerfilActivity : AppCompatActivity() {


    private lateinit var binding:
            ActivityPerfilBinding


    private lateinit var auth:
            FirebaseAuth


    private lateinit var firestore:
            FirebaseFirestore


    private var perfilCargado =
        false


    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)


        auth =
            FirebaseAuth.getInstance()


        firestore =
            FirebaseFirestore.getInstance()


        /*
         * Si no existe una sesión,
         * regresamos inmediatamente al login.
         */

        if (auth.currentUser == null) {

            abrirLogin()

            return
        }


        binding =
            ActivityPerfilBinding.inflate(
                layoutInflater
            )


        setContentView(
            binding.root
        )


        configurarPantalla()

        configurarEventos()

        cargarPerfil()
    }


    // =========================================================
    // ON RESUME
    // =========================================================

    override fun onResume() {

        super.onResume()

        /*
         * Cuando regresamos desde SeleccionarUbicacionActivity,
         * volvemos a consultar solamente las ubicaciones para que
         * Casa y Trabajo se actualicen inmediatamente.
         */

        if (
            ::binding.isInitialized &&
            auth.currentUser != null
        ) {

            cargarUbicacionesUsuario()
        }
    }


    // =========================================================
    // CONFIGURAR PANTALLA
    // =========================================================

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


    // =========================================================
    // EVENTOS
    // =========================================================

    private fun configurarEventos() {


        binding.btnVolver
            .setOnClickListener {

                finish()
            }


        binding.btnGuardar
            .setOnClickListener {

                guardarCambios()
            }


        binding.btnCambiarPassword
            .setOnClickListener {

                confirmarCambioPassword()
            }


        // UBICACIÓN DE CASA

        binding.btnUbicacionCasa
            .setOnClickListener {

                abrirSeleccionUbicacion(
                    TIPO_CASA
                )
            }


        // UBICACIÓN DE TRABAJO

        binding.btnUbicacionTrabajo
            .setOnClickListener {

                abrirSeleccionUbicacion(
                    TIPO_TRABAJO
                )
            }


        // También permitimos tocar toda la tarjeta

        binding.cardUbicacionCasa
            .setOnClickListener {

                abrirSeleccionUbicacion(
                    TIPO_CASA
                )
            }


        binding.cardUbicacionTrabajo
            .setOnClickListener {

                abrirSeleccionUbicacion(
                    TIPO_TRABAJO
                )
            }


        binding.etCelular
            .setOnEditorActionListener {
                    _, actionId, _ ->


                if (
                    actionId ==
                    EditorInfo.IME_ACTION_DONE
                ) {


                    guardarCambios()

                    true


                } else {


                    false
                }
            }


        binding.etNombres
            .setOnFocusChangeListener {
                    _, tieneFoco ->


                if (tieneFoco) {

                    binding.tilNombres.error =
                        null
                }
            }


        binding.etApellidos
            .setOnFocusChangeListener {
                    _, tieneFoco ->


                if (tieneFoco) {

                    binding.tilApellidos.error =
                        null
                }
            }


        binding.etCelular
            .setOnFocusChangeListener {
                    _, tieneFoco ->


                if (tieneFoco) {

                    binding.tilCelular.error =
                        null
                }
            }
    }


    // =========================================================
    // CARGAR PERFIL
    // =========================================================

    private fun cargarPerfil() {


        val usuarioActual =
            auth.currentUser


        if (usuarioActual == null) {


            abrirLogin()

            return
        }


        mostrarCargando(true)


        firestore
            .collection(
                COLECCION_USUARIOS
            )
            .document(
                usuarioActual.uid
            )
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


                // =============================================
                // NOMBRES
                // =============================================

                val nombres =
                    documento
                        .getString(
                            CAMPO_NOMBRES
                        )
                        .orEmpty()
                        .trim()


                // =============================================
                // APELLIDOS
                // =============================================

                val apellidos =
                    documento
                        .getString(
                            CAMPO_APELLIDOS
                        )
                        .orEmpty()
                        .trim()


                // =============================================
                // DNI
                // =============================================

                val dni =
                    documento
                        .getString(
                            CAMPO_DNI
                        )
                        .orEmpty()
                        .trim()


                // =============================================
                // CELULAR
                // =============================================

                val celular =
                    documento
                        .getString(
                            CAMPO_CELULAR
                        )
                        .orEmpty()
                        .trim()


                // =============================================
                // CORREO FIRESTORE
                // =============================================

                val correoFirestore =
                    documento
                        .getString(
                            CAMPO_CORREO
                        )
                        .orEmpty()
                        .trim()


                // =============================================
                // CORREO FIREBASE
                // =============================================

                val correo =

                    usuarioActual
                        .email
                        .orEmpty()
                        .trim()
                        .ifBlank {

                            correoFirestore
                        }


                // =============================================
                // FOTO FIRESTORE
                // =============================================

                val fotoFirestore =
                    documento
                        .getString(
                            CAMPO_FOTO_URL
                        )
                        .orEmpty()
                        .trim()


                // =============================================
                // FOTO FIREBASE / GOOGLE
                // =============================================

                val fotoFirebase =
                    usuarioActual
                        .photoUrl
                        ?.toString()
                        .orEmpty()
                        .trim()


                /*
                 * Primero usamos la foto guardada
                 * en Firestore.
                 *
                 * Si está vacía usamos la foto
                 * que Firebase obtiene de Google.
                 */

                val fotoUrl =

                    if (
                        fotoFirestore.isNotBlank()
                    ) {


                        fotoFirestore


                    } else {


                        fotoFirebase
                    }


                // =============================================
                // PROVEEDOR DE AUTENTICACIÓN
                // =============================================

                val proveedorAuth =
                    documento
                        .getString(
                            CAMPO_PROVEEDOR_AUTH
                        )
                        .orEmpty()
                        .trim()


                // =============================================
                // MOSTRAR DATOS
                // =============================================

                binding.etNombres
                    .setText(
                        nombres
                    )


                binding.etApellidos
                    .setText(
                        apellidos
                    )


                binding.etDni
                    .setText(
                        dni
                    )


                binding.etCelular
                    .setText(
                        celular
                    )


                binding.etCorreo
                    .setText(
                        correo
                    )


                // FOTO

                cargarFotoPerfil(
                    fotoUrl
                )


                // ENCABEZADO

                actualizarEncabezado(
                    nombres = nombres,
                    apellidos = apellidos,
                    correo = correo
                )


                // CONFIGURAR SEGURIDAD

                configurarSeguridadCuenta(
                    proveedorAuth
                )


                // UBICACIONES

                mostrarUbicacionesDesdeDocumento(
                    documento.get(
                        CAMPO_UBICACION_CASA
                    ),
                    documento.get(
                        CAMPO_UBICACION_TRABAJO
                    )
                )


                perfilCargado =
                    true
            }
            .addOnFailureListener { error ->


                mostrarCargando(false)


                Toast.makeText(
                    this,
                    "No se pudo cargar el perfil: " +
                            (
                                    error.localizedMessage
                                        ?: "error desconocido"
                                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
    }


    // =========================================================
    // CARGAR UBICACIONES DEL USUARIO
    // =========================================================

    private fun cargarUbicacionesUsuario() {


        val usuarioActual =
            auth.currentUser
                ?: return


        firestore
            .collection(
                COLECCION_USUARIOS
            )
            .document(
                usuarioActual.uid
            )
            .get()
            .addOnSuccessListener { documento ->


                if (!documento.exists()) {

                    mostrarUbicacionCasa(
                        direccion = "",
                        referencia = ""
                    )

                    mostrarUbicacionTrabajo(
                        direccion = "",
                        referencia = ""
                    )

                    return@addOnSuccessListener
                }


                mostrarUbicacionesDesdeDocumento(
                    documento.get(
                        CAMPO_UBICACION_CASA
                    ),
                    documento.get(
                        CAMPO_UBICACION_TRABAJO
                    )
                )
            }
    }


    // =========================================================
    // LEER MAPAS DE FIRESTORE
    // =========================================================

    private fun mostrarUbicacionesDesdeDocumento(
        valorCasa: Any?,
        valorTrabajo: Any?
    ) {


        val ubicacionCasa =
            valorCasa as? Map<*, *>


        val ubicacionTrabajo =
            valorTrabajo as? Map<*, *>


        val direccionCasa =
            ubicacionCasa
                ?.get(
                    CAMPO_DIRECCION
                )
                ?.toString()
                ?.trim()
                .orEmpty()


        val referenciaCasa =
            ubicacionCasa
                ?.get(
                    CAMPO_REFERENCIA
                )
                ?.toString()
                ?.trim()
                .orEmpty()


        val direccionTrabajo =
            ubicacionTrabajo
                ?.get(
                    CAMPO_DIRECCION
                )
                ?.toString()
                ?.trim()
                .orEmpty()


        val referenciaTrabajo =
            ubicacionTrabajo
                ?.get(
                    CAMPO_REFERENCIA
                )
                ?.toString()
                ?.trim()
                .orEmpty()


        mostrarUbicacionCasa(
            direccion = direccionCasa,
            referencia = referenciaCasa
        )


        mostrarUbicacionTrabajo(
            direccion = direccionTrabajo,
            referencia = referenciaTrabajo
        )
    }


    // =========================================================
    // MOSTRAR CASA
    // =========================================================

    private fun mostrarUbicacionCasa(
        direccion: String,
        referencia: String
    ) {


        val tieneCasa =
            direccion.isNotBlank()


        if (tieneCasa) {


            binding.tvDireccionCasa.text =
                direccion


            binding.btnUbicacionCasa.text =
                "CAMBIAR UBICACIÓN"


        } else {


            binding.tvDireccionCasa.text =
                "Aún no has registrado la ubicación de tu casa."


            binding.btnUbicacionCasa.text =
                "AGREGAR UBICACIÓN"
        }


        if (referencia.isNotBlank()) {


            binding.tvReferenciaCasa.text =
                "Referencia: $referencia"


            binding.tvReferenciaCasa.visibility =
                View.VISIBLE


        } else {


            binding.tvReferenciaCasa.text =
                ""


            binding.tvReferenciaCasa.visibility =
                View.GONE
        }
    }


    // =========================================================
    // MOSTRAR TRABAJO
    // =========================================================

    private fun mostrarUbicacionTrabajo(
        direccion: String,
        referencia: String
    ) {


        val tieneTrabajo =
            direccion.isNotBlank()


        if (tieneTrabajo) {


            binding.tvDireccionTrabajo.text =
                direccion


            binding.btnUbicacionTrabajo.text =
                "CAMBIAR UBICACIÓN"


        } else {


            binding.tvDireccionTrabajo.text =
                "Aún no has registrado la ubicación de tu trabajo."


            binding.btnUbicacionTrabajo.text =
                "AGREGAR UBICACIÓN"
        }


        if (referencia.isNotBlank()) {


            binding.tvReferenciaTrabajo.text =
                "Referencia: $referencia"


            binding.tvReferenciaTrabajo.visibility =
                View.VISIBLE


        } else {


            binding.tvReferenciaTrabajo.text =
                ""


            binding.tvReferenciaTrabajo.visibility =
                View.GONE
        }
    }


    // =========================================================
    // ABRIR SELECTOR DE UBICACIÓN
    // =========================================================

    private fun abrirSeleccionUbicacion(
        tipoUbicacion: String
    ) {


        val usuarioActual =
            auth.currentUser


        if (usuarioActual == null) {

            abrirLogin()

            return
        }


        val intent =
            Intent(
                this,
                SeleccionarUbicacionActivity::class.java
            ).apply {


                putExtra(
                    EXTRA_TIPO_UBICACION,
                    tipoUbicacion
                )


                putExtra(
                    EXTRA_ORIGEN_UBICACION,
                    ORIGEN_PERFIL
                )
            }


        startActivity(
            intent
        )
    }


    // =========================================================
    // FOTO DE PERFIL
    // =========================================================

    private fun cargarFotoPerfil(
        fotoUrl: String
    ) {


        /*
         * Si el usuario se registró normalmente
         * y no tiene foto, mostramos el icono.
         */

        if (fotoUrl.isBlank()) {


            binding.ivFotoPerfil
                .setImageResource(
                    R.drawable.ic_persona_pasajes
                )


            return
        }


        /*
         * Si tiene una foto de Google,
         * Glide la descarga y muestra.
         */

        Glide
            .with(this)
            .load(fotoUrl)
            .placeholder(
                R.drawable.ic_persona_pasajes
            )
            .error(
                R.drawable.ic_persona_pasajes
            )
            .circleCrop()
            .into(
                binding.ivFotoPerfil
            )
    }


    // =========================================================
    // CONFIGURAR SEGURIDAD SEGÚN TIPO DE CUENTA
    // =========================================================

    private fun configurarSeguridadCuenta(
        proveedorFirestore: String
    ) {


        val usuarioActual =
            auth.currentUser


        /*
         * Comprobamos dos cosas:
         *
         * 1. proveedorAuth guardado en Firestore
         *
         * 2. Los proveedores reales de Firebase Auth.
         */

        val proveedorGoogleFirebase =

            usuarioActual
                ?.providerData
                ?.any { proveedor ->

                    proveedor.providerId ==
                            GoogleAuthProvider.PROVIDER_ID
                }
                ?: false


        val esCuentaGoogle =

            proveedorFirestore.equals(
                PROVEEDOR_GOOGLE,
                ignoreCase = true
            ) ||
                    proveedorGoogleFirebase


        /*
         * Los usuarios Google no necesitan
         * recuperar contraseña desde PasajesYa.
         *
         * Ellos inician sesión nuevamente
         * mediante "Continuar con Google".
         */

        if (esCuentaGoogle) {


            binding.tvSeccionSeguridad.visibility =
                View.GONE


            binding.cardSeguridad.visibility =
                View.GONE


        } else {


            binding.tvSeccionSeguridad.visibility =
                View.VISIBLE


            binding.cardSeguridad.visibility =
                View.VISIBLE
        }
    }


    // =========================================================
    // GUARDAR CAMBIOS
    // =========================================================

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


        val nombres =
            binding.etNombres
                .text
                ?.toString()
                ?.trim()
                .orEmpty()


        val apellidos =
            binding.etApellidos
                .text
                ?.toString()
                ?.trim()
                .orEmpty()


        val celular =
            binding.etCelular
                .text
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


        val usuarioActual =
            auth.currentUser


        if (usuarioActual == null) {


            abrirLogin()

            return
        }


        mostrarCargando(true)


        val datosActualizados =
            mapOf<String, Any>(


                CAMPO_NOMBRES to
                        nombres,


                CAMPO_APELLIDOS to
                        apellidos,


                CAMPO_CELULAR to
                        celular,


                CAMPO_FECHA_ACTUALIZACION to
                        FieldValue.serverTimestamp()
            )


        firestore
            .collection(
                COLECCION_USUARIOS
            )
            .document(
                usuarioActual.uid
            )
            .update(
                datosActualizados
            )
            .addOnSuccessListener {


                mostrarCargando(false)


                actualizarEncabezado(
                    nombres = nombres,
                    apellidos = apellidos,
                    correo =
                        usuarioActual
                            .email
                            .orEmpty()
                )


                /*
                 * Avisamos a la pantalla anterior
                 * que hubo modificaciones.
                 */

                setResult(
                    RESULT_OK
                )


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
                            (
                                    error.localizedMessage
                                        ?: "error desconocido"
                                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
    }


    // =========================================================
    // VALIDAR FORMULARIO
    // =========================================================

    private fun validarFormulario(
        nombres: String,
        apellidos: String,
        celular: String
    ): Boolean {


        var formularioValido =
            true


        // NOMBRES

        if (nombres.isBlank()) {


            binding.tilNombres.error =
                "Ingresa tus nombres"


            formularioValido =
                false


        } else if (
            nombres.length < 2
        ) {


            binding.tilNombres.error =
                "Ingresa un nombre válido"


            formularioValido =
                false
        }


        // APELLIDOS

        if (apellidos.isBlank()) {


            binding.tilApellidos.error =
                "Ingresa tus apellidos"


            formularioValido =
                false


        } else if (
            apellidos.length < 2
        ) {


            binding.tilApellidos.error =
                "Ingresa apellidos válidos"


            formularioValido =
                false
        }


        // CELULAR

        if (celular.isBlank()) {


            binding.tilCelular.error =
                "Ingresa tu número de celular"


            formularioValido =
                false


        } else if (
            celular.length != 9
        ) {


            binding.tilCelular.error =
                "El celular debe tener 9 dígitos"


            formularioValido =
                false


        } else if (
            !celular.startsWith("9")
        ) {


            binding.tilCelular.error =
                "Ingresa un celular válido"


            formularioValido =
                false
        }


        return formularioValido
    }


    // =========================================================
    // CAMBIAR CONTRASEÑA
    // =========================================================

    private fun confirmarCambioPassword() {


        val correo =
            auth.currentUser
                ?.email
                .orEmpty()
                .trim()


        if (correo.isBlank()) {


            Toast.makeText(
                this,
                "La cuenta no tiene un correo registrado.",
                Toast.LENGTH_LONG
            ).show()


            return
        }


        MaterialAlertDialogBuilder(
            this
        )
            .setTitle(
                "Cambiar contraseña"
            )
            .setMessage(
                "Enviaremos un enlace de recuperación a:\n\n$correo"
            )
            .setNegativeButton(
                "CANCELAR",
                null
            )
            .setPositiveButton(
                "ENVIAR"
            ) { _, _ ->


                enviarEnlacePassword(
                    correo
                )
            }
            .show()
    }


    // =========================================================
    // ENVIAR ENLACE PASSWORD
    // =========================================================

    private fun enviarEnlacePassword(
        correo: String
    ) {


        binding.btnCambiarPassword
            .isEnabled =
            false


        auth.setLanguageCode(
            "es"
        )


        auth.sendPasswordResetEmail(
            correo
        )
            .addOnCompleteListener { tarea ->


                binding.btnCambiarPassword
                    .isEnabled =
                    true


                if (
                    tarea.isSuccessful
                ) {


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


                            tarea.exception
                                ?.localizedMessage
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


    // =========================================================
    // ACTUALIZAR ENCABEZADO
    // =========================================================

    private fun actualizarEncabezado(
        nombres: String,
        apellidos: String,
        correo: String
    ) {


        val nombreCompleto =
            listOf(
                nombres.trim(),
                apellidos.trim()
            )
                .filter { texto ->

                    texto.isNotBlank()
                }
                .joinToString(
                    " "
                )


        binding.tvNombrePerfil.text =

            nombreCompleto
                .ifBlank {

                    "Pasajero"
                }


        binding.tvCorreoPerfil.text =

            correo
                .ifBlank {

                    "Correo no disponible"
                }
    }


    // =========================================================
    // LIMPIAR ERRORES
    // =========================================================

    private fun limpiarErrores() {


        binding.tilNombres.error =
            null


        binding.tilApellidos.error =
            null


        binding.tilCelular.error =
            null
    }


    // =========================================================
    // CARGANDO
    // =========================================================

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


        binding.btnUbicacionCasa.isEnabled =
            !cargando


        binding.btnUbicacionTrabajo.isEnabled =
            !cargando


        binding.cardUbicacionCasa.isEnabled =
            !cargando


        binding.cardUbicacionTrabajo.isEnabled =
            !cargando


        binding.etNombres.isEnabled =
            !cargando


        binding.etApellidos.isEnabled =
            !cargando


        binding.etCelular.isEnabled =
            !cargando


        /*
         * DNI y correo permanecen siempre
         * bloqueados desde el XML.
         */
    }


    // =========================================================
    // IR AL LOGIN
    // =========================================================

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


    // =========================================================
    // CONSTANTES
    // =========================================================

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


        private const val CAMPO_FOTO_URL =
            "fotoUrl"


        private const val CAMPO_PROVEEDOR_AUTH =
            "proveedorAuth"


        private const val CAMPO_UBICACION_CASA =
            "ubicacionCasa"


        private const val CAMPO_UBICACION_TRABAJO =
            "ubicacionTrabajo"


        private const val CAMPO_DIRECCION =
            "direccion"


        private const val CAMPO_REFERENCIA =
            "referencia"


        const val EXTRA_TIPO_UBICACION =
            "tipoUbicacion"


        const val EXTRA_ORIGEN_UBICACION =
            "origenUbicacion"


        const val TIPO_CASA =
            "CASA"


        const val TIPO_TRABAJO =
            "TRABAJO"


        const val ORIGEN_PERFIL =
            "PERFIL"


        private const val CAMPO_FECHA_ACTUALIZACION =
            "fechaActualizacion"


        private const val PROVEEDOR_GOOGLE =
            "google"
    }
}