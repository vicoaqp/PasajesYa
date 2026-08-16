package com.PasajesYa.pasajesya

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.PasajesYa.pasajesya.databinding.ActivityCompletarPerfilGoogleBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.Locale


class CompletarPerfilGoogleActivity : AppCompatActivity() {

    private lateinit var binding:
            ActivityCompletarPerfilGoogleBinding

    private lateinit var auth: FirebaseAuth

    private lateinit var firestore: FirebaseFirestore


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        binding =
            ActivityCompletarPerfilGoogleBinding.inflate(
                layoutInflater
            )

        setContentView(binding.root)


        auth =
            FirebaseAuth.getInstance()

        firestore =
            FirebaseFirestore.getInstance()


        configurarPantalla()

        cargarDatosGoogle()

        configurarEventos()

        configurarBotonAtras()
    }


    // =========================================================
    // PANTALLA
    // =========================================================

    private fun configurarPantalla() {

        window.statusBarColor =
            getColor(R.color.pasajes_primary_dark)

        window.navigationBarColor =
            getColor(R.color.pasajes_background)
    }


    // =========================================================
    // MOSTRAR INFORMACIÓN DE GOOGLE
    // =========================================================

    private fun cargarDatosGoogle() {

        val usuario = auth.currentUser


        if (usuario == null) {

            Toast.makeText(
                this,
                "No existe una sesión de Google activa.",
                Toast.LENGTH_LONG
            ).show()

            volverAlLogin()

            return
        }


        val nombreGoogle =
            usuario.displayName
                ?.trim()
                .orEmpty()


        val correoGoogle =
            usuario.email
                ?.trim()
                .orEmpty()


        binding.tvNombreGoogle.text =

            if (nombreGoogle.isBlank()) {

                "Usuario de Google"

            } else {

                nombreGoogle
            }


        binding.tvCorreoGoogle.text =
            correoGoogle
    }


    // =========================================================
    // EVENTOS
    // =========================================================

    private fun configurarEventos() {


        binding.btnCompletar.setOnClickListener {

            completarRegistro()
        }


        binding.tvCancelar.setOnClickListener {

            cancelarRegistroGoogle()
        }


        binding.etCelular.setOnEditorActionListener {
                _, actionId, _ ->

            if (actionId == EditorInfo.IME_ACTION_DONE) {

                completarRegistro()

                true

            } else {

                false
            }
        }


        binding.etDni.setOnFocusChangeListener {
                _, tieneFoco ->

            if (tieneFoco) {

                binding.tilDni.error = null
            }
        }


        binding.etCelular.setOnFocusChangeListener {
                _, tieneFoco ->

            if (tieneFoco) {

                binding.tilCelular.error = null
            }
        }


        binding.checkTerminos
            .setOnCheckedChangeListener {
                    _, marcado ->

                if (marcado) {

                    binding.tvErrorTerminos.visibility =
                        View.GONE
                }
            }
    }


    // =========================================================
    // BOTÓN ATRÁS
    // =========================================================

    private fun configurarBotonAtras() {

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {

                    cancelarRegistroGoogle()
                }
            }
        )
    }


    // =========================================================
    // COMPLETAR REGISTRO
    // =========================================================

    private fun completarRegistro() {


        limpiarErrores()


        val usuario = auth.currentUser


        if (usuario == null) {

            Toast.makeText(
                this,
                "La sesión de Google finalizó.",
                Toast.LENGTH_LONG
            ).show()

            volverAlLogin()

            return
        }


        val dni =
            binding.etDni.text
                ?.toString()
                ?.trim()
                .orEmpty()


        val celular =
            binding.etCelular.text
                ?.toString()
                ?.trim()
                .orEmpty()


        val aceptaTerminos =
            binding.checkTerminos.isChecked


        if (
            !validarFormulario(
                dni = dni,
                celular = celular,
                aceptaTerminos = aceptaTerminos
            )
        ) {

            return
        }


        mostrarCargando(true)


        verificarDniRegistrado(
            dni = dni,
            celular = celular
        )
    }


    // =========================================================
    // VALIDAR DNI DUPLICADO
    // =========================================================

    private fun verificarDniRegistrado(
        dni: String,
        celular: String
    ) {


        val usuarioActual = auth.currentUser


        if (usuarioActual == null) {

            mostrarCargando(false)

            volverAlLogin()

            return
        }


        firestore
            .collection(COLECCION_USUARIOS)
            .whereEqualTo(
                CAMPO_DNI,
                dni
            )
            .limit(1)
            .get()
            .addOnSuccessListener { resultado ->


                /*
                 * Si encontramos un DNI registrado,
                 * debemos comprobar que no pertenezca
                 * al mismo usuario.
                 */

                if (!resultado.isEmpty) {


                    val documento =
                        resultado.documents.first()


                    if (
                        documento.id !=
                        usuarioActual.uid
                    ) {


                        mostrarCargando(false)


                        binding.tilDni.error =
                            "Este DNI ya se encuentra registrado"


                        binding.etDni.requestFocus()


                        return@addOnSuccessListener
                    }
                }


                guardarPerfil(
                    dni = dni,
                    celular = celular
                )
            }
            .addOnFailureListener { error ->


                mostrarCargando(false)


                Toast.makeText(
                    this,
                    "No se pudo verificar el DNI: " +
                            (
                                    error.localizedMessage
                                        ?: "error desconocido"
                                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
    }


    // =========================================================
    // GUARDAR PERFIL
    // =========================================================

    private fun guardarPerfil(
        dni: String,
        celular: String
    ) {


        val usuario =
            auth.currentUser


        if (usuario == null) {

            mostrarCargando(false)

            volverAlLogin()

            return
        }


        val uid =
            usuario.uid


        val correo =
            usuario.email
                ?.trim()
                ?.lowercase(Locale.ROOT)
                .orEmpty()


        val nombreCompleto =
            usuario.displayName
                ?.trim()
                .orEmpty()


        /*
         * Google normalmente entrega un nombre completo.
         *
         * Intentamos separarlo en:
         *
         * nombres
         * apellidos
         */

        val nombreSeparado =
            separarNombreCompleto(
                nombreCompleto
            )


        val nombres =
            nombreSeparado.first


        val apellidos =
            nombreSeparado.second


        val fotoUrl =
            usuario.photoUrl
                ?.toString()
                .orEmpty()


        /*
         * Guardamos exactamente la misma estructura
         * utilizada por tu registro normal.
         */

        val datosUsuario =
            hashMapOf<String, Any>(


                CAMPO_UID to uid,


                CAMPO_NOMBRES to nombres,


                CAMPO_APELLIDOS to apellidos,


                CAMPO_DNI to dni,


                CAMPO_CELULAR to celular,


                CAMPO_CORREO to correo,


                CAMPO_ROL to ROL_USUARIO,


                CAMPO_ESTADO to ESTADO_ACTIVO,


                CAMPO_FOTO_URL to fotoUrl,


                CAMPO_ULTIMA_EMPRESA to "",


                CAMPO_PROVEEDOR_AUTH to
                        PROVEEDOR_GOOGLE,


                CAMPO_FECHA_REGISTRO to
                        FieldValue.serverTimestamp()
            )


        firestore
            .collection(COLECCION_USUARIOS)
            .document(uid)

            /*
             * merge permite completar un documento
             * si por alguna razón ya existía.
             */
            .set(
                datosUsuario,
                SetOptions.merge()
            )

            .addOnSuccessListener {


                mostrarCargando(false)


                Toast.makeText(
                    this,
                    "Tu registro se completó correctamente.",
                    Toast.LENGTH_LONG
                ).show()


                abrirSeleccionEmpresa()
            }

            .addOnFailureListener { error ->


                mostrarCargando(false)


                Toast.makeText(
                    this,
                    "No se pudo guardar tu perfil: " +
                            (
                                    error.localizedMessage
                                        ?: "error desconocido"
                                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
    }


    // =========================================================
    // SEPARAR NOMBRE GOOGLE
    // =========================================================

    private fun separarNombreCompleto(
        nombreCompleto: String
    ): Pair<String, String> {


        val partes =
            nombreCompleto
                .trim()
                .split(
                    Regex("\\s+")
                )
                .filter {
                    it.isNotBlank()
                }


        /*
         * Ejemplos:
         *
         * Víctor Orccoapaza
         *
         * nombres:
         * Víctor
         *
         * apellidos:
         * Orccoapaza
         *
         *
         * Víctor Alfonso Orccoapaza López
         *
         * nombres:
         * Víctor Alfonso
         *
         * apellidos:
         * Orccoapaza López
         */


        return when {


            partes.size >= 4 -> {


                val nombres =
                    partes
                        .dropLast(2)
                        .joinToString(" ")


                val apellidos =
                    partes
                        .takeLast(2)
                        .joinToString(" ")


                Pair(
                    nombres,
                    apellidos
                )
            }


            partes.size == 3 -> {


                Pair(
                    partes.first(),
                    partes
                        .drop(1)
                        .joinToString(" ")
                )
            }


            partes.size == 2 -> {


                Pair(
                    partes[0],
                    partes[1]
                )
            }


            partes.size == 1 -> {


                Pair(
                    partes[0],
                    ""
                )
            }


            else -> {


                val correo =
                    auth.currentUser
                        ?.email
                        ?.substringBefore("@")
                        .orEmpty()


                Pair(
                    if (correo.isBlank()) {
                        "Usuario"
                    } else {
                        correo
                    },
                    ""
                )
            }
        }
    }


    // =========================================================
    // VALIDAR FORMULARIO
    // =========================================================

    private fun validarFormulario(
        dni: String,
        celular: String,
        aceptaTerminos: Boolean
    ): Boolean {


        var formularioValido =
            true


        // DNI

        if (dni.isBlank()) {


            binding.tilDni.error =
                "Ingresa tu DNI"


            formularioValido =
                false


        } else if (dni.length != 8) {


            binding.tilDni.error =
                "El DNI debe tener 8 dígitos"


            formularioValido =
                false
        }


        // CELULAR

        if (celular.isBlank()) {


            binding.tilCelular.error =
                "Ingresa tu número de celular"


            formularioValido =
                false


        } else if (celular.length != 9) {


            binding.tilCelular.error =
                "El celular debe tener 9 dígitos"


            formularioValido =
                false


        } else if (!celular.startsWith("9")) {


            binding.tilCelular.error =
                "Ingresa un número celular válido"


            formularioValido =
                false
        }


        // TÉRMINOS

        if (!aceptaTerminos) {


            binding.tvErrorTerminos.visibility =
                View.VISIBLE


            formularioValido =
                false
        }


        return formularioValido
    }


    // =========================================================
    // CARGANDO
    // =========================================================

    private fun mostrarCargando(
        cargando: Boolean
    ) {


        binding.btnCompletar.isEnabled =
            !cargando


        binding.tvCancelar.isEnabled =
            !cargando


        binding.etDni.isEnabled =
            !cargando


        binding.etCelular.isEnabled =
            !cargando


        binding.checkTerminos.isEnabled =
            !cargando


        binding.progressRegistro.visibility =

            if (cargando) {

                View.VISIBLE

            } else {

                View.GONE
            }


        binding.btnCompletar.text =

            if (cargando) {

                ""

            } else {

                "COMPLETAR REGISTRO"
            }
    }


    // =========================================================
    // LIMPIAR ERRORES
    // =========================================================

    private fun limpiarErrores() {

        binding.tilDni.error = null

        binding.tilCelular.error = null

        binding.tvErrorTerminos.visibility =
            View.GONE
    }


    // =========================================================
    // CANCELAR GOOGLE
    // =========================================================

    private fun cancelarRegistroGoogle() {


        /*
         * Si no quiere terminar el registro,
         * cerramos la sesión Google.
         */

        auth.signOut()


        volverAlLogin()
    }


    // =========================================================
    // VOLVER LOGIN
    // =========================================================

    private fun volverAlLogin() {


        val intent =
            Intent(
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


    // =========================================================
    // ABRIR PASAJESYA
    // =========================================================

    private fun abrirSeleccionEmpresa() {


        val intent =
            Intent(
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
    // CONSTANTES
    // =========================================================

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


        private const val CAMPO_PROVEEDOR_AUTH =
            "proveedorAuth"


        private const val ROL_USUARIO =
            "usuario"


        private const val ESTADO_ACTIVO =
            "activo"


        private const val PROVEEDOR_GOOGLE =
            "google"
    }
}