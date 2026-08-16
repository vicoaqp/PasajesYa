package com.PasajesYa.pasajesya

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.PasajesYa.pasajesya.databinding.ActivityMisEncomiendasBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Locale

class MisEncomiendasActivity : AppCompatActivity() {

    private lateinit var binding:
            ActivityMisEncomiendasBinding

    private lateinit var auth:
            FirebaseAuth

    private lateinit var firestore:
            FirebaseFirestore

    private lateinit var adapter:
            EncomiendaAdapter

    private var listenerEncomiendas:
            ListenerRegistration? = null


    // =================================================
    // ON CREATE
    // =================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )


        binding =
            ActivityMisEncomiendasBinding
                .inflate(
                    layoutInflater
                )


        setContentView(
            binding.root
        )


        auth =
            FirebaseAuth.getInstance()


        firestore =
            FirebaseFirestore.getInstance()


        // ---------------------------------------------
        // VERIFICAR SESIÓN
        // ---------------------------------------------

        if (
            auth.currentUser == null
        ) {

            finish()

            return
        }


        configurarPantalla()

        configurarRecycler()

        configurarEventos()

        escucharEncomiendas()
    }


    // =================================================
    // PANTALLA
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
    // RECYCLER
    // =================================================

    private fun configurarRecycler() {

        adapter =
            EncomiendaAdapter {
                    encomienda ->


                abrirDetalleEncomienda(
                    encomienda
                )
            }


        binding.recyclerEncomiendas.apply {

            layoutManager =
                LinearLayoutManager(
                    this@MisEncomiendasActivity
                )


            adapter =
                this@MisEncomiendasActivity
                    .adapter


            setHasFixedSize(
                false
            )
        }
    }


    // =================================================
    // ABRIR DETALLE
    // =================================================

    private fun abrirDetalleEncomienda(
        encomienda: EncomiendaLocal
    ) {

        // ---------------------------------------------
        // VERIFICAR ID
        // ---------------------------------------------

        if (
            encomienda.id.isBlank()
        ) {

            Toast.makeText(
                this,
                "No se pudo identificar la encomienda.",
                Toast.LENGTH_SHORT
            ).show()


            return
        }


        // ---------------------------------------------
        // ABRIR DETALLE
        // ---------------------------------------------

        val intent =
            Intent(
                this,
                DetalleEncomiendaActivity::class.java
            )
                .apply {

                    putExtra(
                        DetalleEncomiendaActivity.EXTRA_SOLICITUD_ID,
                        encomienda.id
                    )
                }


        startActivity(
            intent
        )


        overridePendingTransition(
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
    }


    // =================================================
    // EVENTOS
    // =================================================

    private fun configurarEventos() {

        binding.btnVolver
            .setOnClickListener {

                finish()
            }
    }


    // =================================================
    // ESCUCHAR ENCOMIENDAS
    // =================================================

    private fun escucharEncomiendas() {

        val usuario =
            auth.currentUser
                ?: return


        mostrarCargando(
            true
        )


        listenerEncomiendas
            ?.remove()


        listenerEncomiendas =
            firestore
                .collection(
                    COLECCION_SOLICITUDES
                )
                .whereEqualTo(
                    CAMPO_USUARIO_ID,
                    usuario.uid
                )
                .addSnapshotListener(
                    this
                ) {
                        snapshot,
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

                        Toast.makeText(
                            this,
                            "No se pudieron cargar tus encomiendas: " +
                                    (
                                            error.localizedMessage
                                                ?: "Error desconocido"
                                            ),
                            Toast.LENGTH_LONG
                        ).show()


                        mostrarLista(
                            emptyList()
                        )


                        return@addSnapshotListener
                    }


                    // -------------------------------------
                    // CONVERTIR DOCUMENTOS
                    // -------------------------------------

                    val encomiendas =
                        snapshot
                            ?.documents
                            ?.mapNotNull {
                                    documento ->


                                documento
                                    .toObject(
                                        EncomiendaLocal::class.java
                                    )
                                    ?.copy(
                                        id =
                                            documento.id
                                    )
                            }
                            ?.filter {
                                    encomienda ->


                                encomienda
                                    .tipoSolicitud
                                    .trim()
                                    .lowercase(
                                        Locale.ROOT
                                    ) ==
                                        TIPO_ENCOMIENDA
                            }
                            ?.sortedWith(

                                compareByDescending<
                                        EncomiendaLocal
                                        > {

                                    it
                                        .fechaPublicacion
                                        ?.seconds
                                        ?: 0L
                                }
                            )
                            .orEmpty()


                    // -------------------------------------
                    // MOSTRAR
                    // -------------------------------------

                    mostrarLista(
                        encomiendas
                    )
                }
    }


    // =================================================
    // MOSTRAR LISTA
    // =================================================

    private fun mostrarLista(
        encomiendas:
        List<EncomiendaLocal>
    ) {

        adapter.actualizar(
            encomiendas
        )


        val cantidad =
            encomiendas.size


        // ---------------------------------------------
        // TEXTO RESUMEN
        // ---------------------------------------------

        binding.tvResumen.text =
            when (
                cantidad
            ) {

                0 -> {

                    "No tienes encomiendas publicadas."
                }


                1 -> {

                    "Tienes 1 encomienda registrada."
                }


                else -> {

                    "Tienes $cantidad encomiendas registradas."
                }
            }


        // ---------------------------------------------
        // SIN ENCOMIENDAS
        // ---------------------------------------------

        binding.contenedorSinEncomiendas.visibility =
            if (
                encomiendas.isEmpty()
            ) {

                View.VISIBLE

            } else {

                View.GONE
            }


        // ---------------------------------------------
        // RECYCLER
        // ---------------------------------------------

        binding.recyclerEncomiendas.visibility =
            if (
                encomiendas.isEmpty()
            ) {

                View.GONE

            } else {

                View.VISIBLE
            }
    }


    // =================================================
    // CARGANDO
    // =================================================

    private fun mostrarCargando(
        mostrar: Boolean
    ) {

        binding.progressEncomiendas.visibility =
            if (
                mostrar
            ) {

                View.VISIBLE

            } else {

                View.GONE
            }


        if (
            mostrar
        ) {

            binding.contenedorSinEncomiendas.visibility =
                View.GONE
        }
    }


    // =================================================
    // ON DESTROY
    // =================================================

    override fun onDestroy() {

        listenerEncomiendas
            ?.remove()


        listenerEncomiendas =
            null


        super.onDestroy()
    }


    // =================================================
    // CONSTANTES
    // =================================================

    companion object {

        private const val COLECCION_SOLICITUDES =
            "solicitudeslocales"


        private const val CAMPO_USUARIO_ID =
            "usuarioId"


        private const val TIPO_ENCOMIENDA =
            "encomienda"
    }
}