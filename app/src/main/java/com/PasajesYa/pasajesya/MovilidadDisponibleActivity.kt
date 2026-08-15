package com.PasajesYa.pasajesya

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.PasajesYa.pasajesya.databinding.ActivityMovilidadDisponibleBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MovilidadDisponibleActivity :
    AppCompatActivity() {

    private lateinit var binding:
            ActivityMovilidadDisponibleBinding

    private lateinit var firestore:
            FirebaseFirestore

    private lateinit var adapter:
            ConductorMovilidadAdapter

    private var listenerConductores:
            ListenerRegistration? = null

    private val conductoresDisponibles =
        mutableListOf<ConductorMovilidad>()

    private var filtroActual =
        FILTRO_TODOS


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        binding =
            ActivityMovilidadDisponibleBinding
                .inflate(
                    layoutInflater
                )

        setContentView(
            binding.root
        )

        firestore =
            FirebaseFirestore.getInstance()

        configurarPantalla()

        configurarRecycler()

        configurarEventos()

        actualizarBotonesFiltro()

        escucharConductores()
    }


    // -------------------------------------------------
    // PANTALLA
    // -------------------------------------------------

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


    // -------------------------------------------------
    // RECYCLER
    // -------------------------------------------------

    private fun configurarRecycler() {

        adapter =
            ConductorMovilidadAdapter {
                    conductor ->

                llamarConductor(
                    conductor
                )
            }


        binding.recyclerMovilidad.apply {

            layoutManager =
                LinearLayoutManager(
                    this@MovilidadDisponibleActivity
                )

            adapter =
                this@MovilidadDisponibleActivity
                    .adapter

            setHasFixedSize(
                false
            )
        }
    }


    // -------------------------------------------------
    // EVENTOS
    // -------------------------------------------------

    private fun configurarEventos() {

        binding.btnVolver
            .setOnClickListener {

                finish()
            }


        binding.btnTodos
            .setOnClickListener {

                filtroActual =
                    FILTRO_TODOS

                actualizarBotonesFiltro()

                aplicarFiltro()
            }


        binding.btnTaxis
            .setOnClickListener {

                filtroActual =
                    FILTRO_TAXI

                actualizarBotonesFiltro()

                aplicarFiltro()
            }


        binding.btnMototaxis
            .setOnClickListener {

                filtroActual =
                    FILTRO_MOTOTAXI

                actualizarBotonesFiltro()

                aplicarFiltro()
            }
    }


    // -------------------------------------------------
    // FIRESTORE
    // -------------------------------------------------

    private fun escucharConductores() {

        mostrarCargando(
            true
        )


        listenerConductores
            ?.remove()


        listenerConductores =
            firestore
                .collection(
                    COLECCION_CONDUCTORES
                )
                .whereEqualTo(
                    CAMPO_ESTADO,
                    ESTADO_ACTIVO
                )
                .addSnapshotListener(
                    this
                ) {
                        snapshot,
                        error ->


                    mostrarCargando(
                        false
                    )


                    if (error != null) {

                        Toast.makeText(
                            this,
                            "No se pudieron cargar las unidades disponibles.",
                            Toast.LENGTH_LONG
                        ).show()

                        return@addSnapshotListener
                    }


                    if (snapshot == null) {

                        return@addSnapshotListener
                    }


                    val nuevaLista =
                        snapshot.documents
                            .map {
                                    documento ->


                                ConductorMovilidad(

                                    id =
                                        documento.id,

                                    nombres =
                                        documento
                                            .getString(
                                                "nombres"
                                            )
                                            .orEmpty(),

                                    apellidos =
                                        documento
                                            .getString(
                                                "apellidos"
                                            )
                                            .orEmpty(),

                                    nombreCompleto =
                                        documento
                                            .getString(
                                                "nombreCompleto"
                                            )
                                            .orEmpty(),

                                    celular =
                                        documento
                                            .getString(
                                                "celular"
                                            )
                                            .orEmpty(),

                                    tipoServicio =
                                        documento
                                            .getString(
                                                "tipoServicio"
                                            )
                                            .orEmpty()
                                            .trim()
                                            .lowercase(),

                                    placa =
                                        documento
                                            .getString(
                                                "placa"
                                            )
                                            .orEmpty(),

                                    marca =
                                        documento
                                            .getString(
                                                "marca"
                                            )
                                            .orEmpty(),

                                    modelo =
                                        documento
                                            .getString(
                                                "modelo"
                                            )
                                            .orEmpty(),

                                    color =
                                        documento
                                            .getString(
                                                "color"
                                            )
                                            .orEmpty(),

                                    fotoPerfilUrl =
                                        documento
                                            .getString(
                                                "fotoPerfilUrl"
                                            )
                                            .orEmpty(),

                                    fotoUnidadUrl =
                                        documento
                                            .getString(
                                                "fotoUnidadUrl"
                                            )
                                            .orEmpty(),

                                    estado =
                                        documento
                                            .getString(
                                                CAMPO_ESTADO
                                            )
                                            .orEmpty(),

                                    enLinea =
                                        documento
                                            .getBoolean(
                                                CAMPO_EN_LINEA
                                            )
                                            ?: false,

                                    disponible =
                                        documento
                                            .getBoolean(
                                                CAMPO_DISPONIBLE
                                            )
                                            ?: false,

                                    estadoServicio =
                                        documento
                                            .getString(
                                                CAMPO_ESTADO_SERVICIO
                                            )
                                            .orEmpty()
                                            .trim()
                                            .lowercase()
                                )
                            }
                            .filter {
                                    conductor ->


                                conductor.enLinea &&

                                        conductor.disponible &&

                                        conductor.estadoServicio ==
                                        ESTADO_SERVICIO_DISPONIBLE &&

                                        (
                                                conductor.tipoServicio ==
                                                        TIPO_TAXI ||

                                                        conductor.tipoServicio ==
                                                        TIPO_MOTOTAXI
                                                )
                            }
                            .sortedBy {
                                    conductor ->

                                conductor.obtenerNombre()
                                    .lowercase()
                            }


                    conductoresDisponibles.clear()

                    conductoresDisponibles.addAll(
                        nuevaLista
                    )


                    actualizarContadores()

                    aplicarFiltro()
                }
    }


    // -------------------------------------------------
    // CONTADORES
    // -------------------------------------------------

    private fun actualizarContadores() {

        val taxis =
            conductoresDisponibles.count {

                it.tipoServicio ==
                        TIPO_TAXI
            }


        val mototaxis =
            conductoresDisponibles.count {

                it.tipoServicio ==
                        TIPO_MOTOTAXI
            }


        binding.tvCantidadTaxis.text =
            taxis.toString()


        binding.tvCantidadMototaxis.text =
            mototaxis.toString()
    }


    // -------------------------------------------------
    // FILTRAR
    // -------------------------------------------------

    private fun aplicarFiltro() {

        val lista =
            when (
                filtroActual
            ) {

                FILTRO_TAXI -> {

                    conductoresDisponibles
                        .filter {

                            it.tipoServicio ==
                                    TIPO_TAXI
                        }
                }


                FILTRO_MOTOTAXI -> {

                    conductoresDisponibles
                        .filter {

                            it.tipoServicio ==
                                    TIPO_MOTOTAXI
                        }
                }


                else -> {

                    conductoresDisponibles
                }
            }


        adapter.actualizarConductores(
            lista
        )


        binding.recyclerMovilidad.visibility =
            if (lista.isEmpty()) {

                View.GONE

            } else {

                View.VISIBLE
            }


        binding.contenedorSinUnidades.visibility =
            if (lista.isEmpty()) {

                View.VISIBLE

            } else {

                View.GONE
            }


        binding.tvMensajeSinUnidades.text =
            when (
                filtroActual
            ) {

                FILTRO_TAXI -> {

                    "En este momento no hay taxis disponibles."
                }


                FILTRO_MOTOTAXI -> {

                    "En este momento no hay mototaxis disponibles."
                }


                else -> {

                    "En este momento no hay taxis ni mototaxis disponibles."
                }
            }
    }


    // -------------------------------------------------
    // ESTILO FILTROS
    // -------------------------------------------------

    private fun actualizarBotonesFiltro() {

        configurarBotonFiltro(
            seleccionado =
                filtroActual ==
                        FILTRO_TODOS,

            boton =
                binding.btnTodos
        )


        configurarBotonFiltro(
            seleccionado =
                filtroActual ==
                        FILTRO_TAXI,

            boton =
                binding.btnTaxis
        )


        configurarBotonFiltro(
            seleccionado =
                filtroActual ==
                        FILTRO_MOTOTAXI,

            boton =
                binding.btnMototaxis
        )
    }


    private fun configurarBotonFiltro(
        seleccionado: Boolean,
        boton:
        com.google.android.material.button.MaterialButton
    ) {

        if (seleccionado) {

            boton.backgroundTintList =
                ColorStateList.valueOf(
                    getColor(
                        R.color.pasajes_primary
                    )
                )

            boton.setTextColor(
                Color.WHITE
            )

        } else {

            boton.backgroundTintList =
                ColorStateList.valueOf(
                    getColor(
                        R.color.pasajes_surface
                    )
                )

            boton.setTextColor(
                getColor(
                    R.color.pasajes_primary
                )
            )

            boton.strokeColor =
                ColorStateList.valueOf(
                    getColor(
                        R.color.pasajes_border
                    )
                )

            boton.strokeWidth =
                1
        }
    }


    // -------------------------------------------------
    // LLAMAR
    // -------------------------------------------------

    private fun llamarConductor(
        conductor:
        ConductorMovilidad
    ) {

        val celular =
            conductor.celular
                .trim()


        if (celular.isBlank()) {

            Toast.makeText(
                this,
                "El conductor no tiene un número disponible.",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        try {

            val intent =
                Intent(
                    Intent.ACTION_DIAL
                ).apply {

                    data =
                        Uri.parse(
                            "tel:$celular"
                        )
                }


            startActivity(
                intent
            )


        } catch (_: Exception) {

            Toast.makeText(
                this,
                "No se pudo abrir el teléfono.",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    // -------------------------------------------------
    // CARGANDO
    // -------------------------------------------------

    private fun mostrarCargando(
        mostrar: Boolean
    ) {

        binding.progressMovilidad.visibility =
            if (mostrar) {

                View.VISIBLE

            } else {

                View.GONE
            }
    }


    override fun onDestroy() {

        listenerConductores
            ?.remove()

        listenerConductores =
            null

        super.onDestroy()
    }


    companion object {

        private const val
                COLECCION_CONDUCTORES =
            "conductoresmovilidad"

        private const val
                CAMPO_ESTADO =
            "estado"

        private const val
                CAMPO_EN_LINEA =
            "enLinea"

        private const val
                CAMPO_DISPONIBLE =
            "disponible"

        private const val
                CAMPO_ESTADO_SERVICIO =
            "estadoServicio"

        private const val
                ESTADO_ACTIVO =
            "activo"

        private const val
                ESTADO_SERVICIO_DISPONIBLE =
            "disponible"

        private const val
                TIPO_TAXI =
            "taxi"

        private const val
                TIPO_MOTOTAXI =
            "mototaxi"

        private const val
                FILTRO_TODOS =
            "todos"

        private const val
                FILTRO_TAXI =
            "taxi"

        private const val
                FILTRO_MOTOTAXI =
            "mototaxi"
    }
}