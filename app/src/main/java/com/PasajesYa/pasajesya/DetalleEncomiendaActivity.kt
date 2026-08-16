package com.PasajesYa.pasajesya

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.PasajesYa.pasajesya.databinding.ActivityDetalleEncomiendaBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Locale

class DetalleEncomiendaActivity :
    AppCompatActivity() {

    private lateinit var binding:
            ActivityDetalleEncomiendaBinding

    private lateinit var auth:
            FirebaseAuth

    private lateinit var firestore:
            FirebaseFirestore

    private lateinit var adapter:
            OfertaEncomiendaAdapter


    private var listenerSolicitud:
            ListenerRegistration? = null

    private var listenerOfertas:
            ListenerRegistration? = null


    private var solicitudId =
        ""

    private var encomiendaActual:
            EncomiendaLocal? = null

    private var ofertasActuales =
        emptyList<OfertaEncomienda>()


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )


        binding =
            ActivityDetalleEncomiendaBinding
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


        solicitudId =
            intent
                .getStringExtra(
                    EXTRA_SOLICITUD_ID
                )
                .orEmpty()


        if (
            auth.currentUser == null ||
            solicitudId.isBlank()
        ) {

            finish()

            return
        }


        configurarPantalla()

        configurarRecycler()

        configurarEventos()

        escucharSolicitud()

        escucharOfertas()
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
            OfertaEncomiendaAdapter {
                    oferta ->


                confirmarAceptarOferta(
                    oferta
                )
            }


        binding.recyclerOfertas.apply {

            layoutManager =
                LinearLayoutManager(
                    this@DetalleEncomiendaActivity
                )


            adapter =
                this@DetalleEncomiendaActivity
                    .adapter


            isNestedScrollingEnabled =
                false
        }
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
    // ESCUCHAR SOLICITUD
    // =================================================

    private fun escucharSolicitud() {

        val usuario =
            auth.currentUser
                ?: return


        listenerSolicitud
            ?.remove()


        listenerSolicitud =
            firestore
                .collection(
                    COLECCION_SOLICITUDES
                )
                .document(
                    solicitudId
                )
                .addSnapshotListener(
                    this
                ) {
                        documento,
                        error ->


                    if (
                        error != null
                    ) {

                        Toast.makeText(
                            this,
                            "No se pudo cargar la encomienda.",
                            Toast.LENGTH_LONG
                        ).show()

                        return@addSnapshotListener
                    }


                    if (
                        documento == null ||
                        !documento.exists()
                    ) {

                        finish()

                        return@addSnapshotListener
                    }


                    val encomienda =
                        documento
                            .toObject(
                                EncomiendaLocal::class.java
                            )
                            ?.copy(
                                id =
                                    documento.id
                            )
                            ?: return@addSnapshotListener


                    // SEGURIDAD BÁSICA EN CLIENTE

                    if (
                        encomienda.usuarioId !=
                        usuario.uid
                    ) {

                        finish()

                        return@addSnapshotListener
                    }


                    encomiendaActual =
                        encomienda


                    mostrarEncomienda(
                        encomienda
                    )


                    adapter.actualizar(
                        ofertasActuales,
                        encomienda.aceptaOfertas
                    )
                }
    }


    // =================================================
    // MOSTRAR ENCOMIENDA
    // =================================================

    private fun mostrarEncomienda(
        encomienda: EncomiendaLocal
    ) {

        binding.tvDescripcion.text =
            "📦 " +
                    encomienda
                        .descripcion
                        .ifBlank {

                            "Encomienda local"
                        }


        binding.tvCantidad.text =
            "Cantidad: ${encomienda.cantidad}"


        binding.tvPeso.text =
            "Peso: " +
                    encomienda
                        .pesoAproximado
                        .ifBlank {

                            "No especificado"
                        }


        binding.tvTamano.text =
            "Tamaño: " +
                    textoTamano(
                        encomienda.tamano
                    )


        binding.tvUnidad.text =
            "Unidad: " +
                    textoUnidad(
                        encomienda.tipoUnidad
                    )


        binding.tvRecojo.text =
            "📍 Recojo: " +
                    encomienda.direccionRecojo


        binding.tvReferenciaRecojo.text =
            if (
                encomienda
                    .referenciaRecojo
                    .isBlank()
            ) {

                "Sin referencia"

            } else {

                "Referencia: " +
                        encomienda
                            .referenciaRecojo
            }


        binding.tvEntrega.text =
            "🏁 Entrega: " +
                    encomienda.direccionEntrega


        binding.tvReferenciaEntrega.text =
            if (
                encomienda
                    .referenciaEntrega
                    .isBlank()
            ) {

                "Sin referencia"

            } else {

                "Referencia: " +
                        encomienda
                            .referenciaEntrega
            }


        binding.tvEstado.text =
            textoEstado(
                encomienda.estado
            )


        val asignada =
            encomienda
                .conductorSeleccionadoId
                .isNotBlank()


        binding.cardConductorSeleccionado.visibility =
            if (
                asignada
            ) {

                View.VISIBLE

            } else {

                View.GONE
            }


        if (
            asignada
        ) {

            binding.tvConductorSeleccionado.text =
                encomienda
                    .conductorSeleccionadoNombre
                    .ifBlank {

                        "Conductor seleccionado"
                    }


            binding.tvPrecioAceptado.text =
                String.format(
                    Locale.US,
                    "Precio acordado: S/ %.2f",
                    encomienda.precioAceptado
                )
        }
    }


    // =================================================
    // ESCUCHAR OFERTAS
    // =================================================

    private fun escucharOfertas() {

        binding.progressOfertas.visibility =
            View.VISIBLE


        listenerOfertas
            ?.remove()


        listenerOfertas =
            firestore
                .collection(
                    COLECCION_SOLICITUDES
                )
                .document(
                    solicitudId
                )
                .collection(
                    COLECCION_OFERTAS
                )
                .addSnapshotListener(
                    this
                ) {
                        snapshot,
                        error ->


                    binding.progressOfertas.visibility =
                        View.GONE


                    if (
                        error != null
                    ) {

                        Toast.makeText(
                            this,
                            "No se pudieron cargar las ofertas.",
                            Toast.LENGTH_LONG
                        ).show()


                        mostrarOfertas(
                            emptyList()
                        )


                        return@addSnapshotListener
                    }


                    val ofertas =
                        snapshot
                            ?.documents
                            ?.mapNotNull {
                                    documento ->


                                documento
                                    .toObject(
                                        OfertaEncomienda::class.java
                                    )
                                    ?.copy(

                                        conductorId =
                                            documento.id
                                    )
                            }
                            ?.sortedBy {
                                it.precioServicio
                            }
                            .orEmpty()


                    ofertasActuales =
                        ofertas


                    mostrarOfertas(
                        ofertas
                    )
                }
    }


    // =================================================
    // MOSTRAR OFERTAS
    // =================================================

    private fun mostrarOfertas(
        ofertas:
        List<OfertaEncomienda>
    ) {

        val acepta =
            encomiendaActual
                ?.aceptaOfertas
                ?: false


        adapter.actualizar(
            ofertas,
            acepta
        )


        binding.tvSinOfertas.visibility =
            if (
                ofertas.isEmpty()
            ) {

                View.VISIBLE

            } else {

                View.GONE
            }


        binding.recyclerOfertas.visibility =
            if (
                ofertas.isEmpty()
            ) {

                View.GONE

            } else {

                View.VISIBLE
            }


        binding.tvResumenOfertas.text =
            when (
                ofertas.size
            ) {

                0 ->
                    "Esperando ofertas de conductores..."

                1 ->
                    "Has recibido 1 oferta."

                else ->
                    "Has recibido ${ofertas.size} ofertas."
            }
    }


    // =================================================
    // CONFIRMAR OFERTA
    // =================================================

    private fun confirmarAceptarOferta(
        oferta: OfertaEncomienda
    ) {

        val encomienda =
            encomiendaActual
                ?: return


        if (
            !encomienda.aceptaOfertas
        ) {

            Toast.makeText(
                this,
                "Esta encomienda ya tiene un conductor seleccionado.",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        MaterialAlertDialogBuilder(
            this
        )
            .setTitle(
                "Aceptar oferta"
            )
            .setMessage(

                "Conductor:\n" +
                        oferta.conductorNombre +
                        "\n\nPrecio del servicio:\n" +
                        String.format(
                            Locale.US,
                            "S/ %.2f",
                            oferta.precioServicio
                        ) +
                        "\n\n¿Deseas seleccionar esta oferta?"
            )
            .setNegativeButton(
                "CANCELAR",
                null
            )
            .setPositiveButton(
                "ACEPTAR"
            ) {
                    _, _ ->


                aceptarOferta(
                    oferta
                )
            }
            .show()
    }


    // =================================================
    // ACEPTAR OFERTA
    // =================================================

    private fun aceptarOferta(
        oferta: OfertaEncomienda
    ) {

        val usuario =
            auth.currentUser
                ?: return


        val encomienda =
            encomiendaActual
                ?: return


        if (
            encomienda.usuarioId !=
            usuario.uid
        ) {

            return
        }


        if (
            !encomienda.aceptaOfertas
        ) {

            Toast.makeText(
                this,
                "Ya seleccionaste una oferta.",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        val referenciaSolicitud =
            firestore
                .collection(
                    COLECCION_SOLICITUDES
                )
                .document(
                    solicitudId
                )


        val batch =
            firestore.batch()


        // ---------------------------------------------
        // ACTUALIZAR SOLICITUD
        // ---------------------------------------------

        batch.update(

            referenciaSolicitud,

            mapOf(

                "estado" to
                        ESTADO_ASIGNADO,

                "aceptaOfertas" to
                        false,

                "conductorSeleccionadoId" to
                        oferta.conductorId,

                "conductorSeleccionadoNombre" to
                        oferta.conductorNombre,

                "conductorSeleccionadoCelular" to
                        oferta.conductorCelular,

                "precioAceptado" to
                        oferta.precioServicio,

                "fechaAsignacion" to
                        FieldValue.serverTimestamp(),

                "fechaActualizacion" to
                        FieldValue.serverTimestamp()
            )
        )


        // ---------------------------------------------
        // MARCAR OFERTAS
        // ---------------------------------------------

        ofertasActuales.forEach {
                ofertaActual ->


            val referenciaOferta =
                referenciaSolicitud
                    .collection(
                        COLECCION_OFERTAS
                    )
                    .document(
                        ofertaActual.conductorId
                    )


            val nuevoEstado =
                if (
                    ofertaActual.conductorId ==
                    oferta.conductorId
                ) {

                    ESTADO_OFERTA_ACEPTADA

                } else {

                    ESTADO_OFERTA_RECHAZADA
                }


            batch.update(
                referenciaOferta,
                mapOf(

                    "estadoOferta" to
                            nuevoEstado,

                    "fechaActualizacion" to
                            FieldValue.serverTimestamp()
                )
            )
        }


        batch
            .commit()
            .addOnSuccessListener {


                Toast.makeText(
                    this,
                    "Oferta aceptada correctamente.",
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnFailureListener {
                    error ->


                Toast.makeText(
                    this,
                    "No se pudo aceptar la oferta: " +
                            (
                                    error.localizedMessage
                                        ?: "Error desconocido"
                                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
    }


    // =================================================
    // TEXTOS
    // =================================================

    private fun textoUnidad(
        tipo: String
    ): String {

        return when (
            tipo
                .trim()
                .lowercase()
        ) {

            "mototaxi" ->
                "🛺 Mototaxi"

            "taxi" ->
                "🚕 Taxi / vehículo"

            else ->
                "🚘 Cualquier unidad"
        }
    }


    private fun textoTamano(
        tamano: String
    ): String {

        return when (
            tamano
                .trim()
                .lowercase()
        ) {

            "grande" ->
                "Grande"

            "mediano" ->
                "Mediano"

            else ->
                "Pequeño"
        }
    }


    private fun textoEstado(
        estado: String
    ): String {

        return when (
            estado
                .trim()
                .lowercase()
        ) {

            "asignado" ->
                "🟡 Conductor seleccionado"

            "recogido" ->
                "📦 Encomienda recogida"

            "en_camino" ->
                "🚗 En camino"

            "entregado" ->
                "🟢 Entregada"

            "cancelado" ->
                "🔴 Cancelada"

            else -> {

                if (
                    ofertasActuales.isEmpty()
                ) {

                    "🔵 Buscando ofertas"

                } else {

                    "🔵 Ofertas recibidas"
                }
            }
        }
    }


    // =================================================
    // DESTROY
    // =================================================

    override fun onDestroy() {

        listenerSolicitud
            ?.remove()


        listenerOfertas
            ?.remove()


        listenerSolicitud =
            null


        listenerOfertas =
            null


        super.onDestroy()
    }


    companion object {

        const val EXTRA_SOLICITUD_ID =
            "extra_solicitud_id"


        private const val COLECCION_SOLICITUDES =
            "solicitudeslocales"


        private const val COLECCION_OFERTAS =
            "ofertas"


        private const val ESTADO_ASIGNADO =
            "asignado"


        private const val ESTADO_OFERTA_ACEPTADA =
            "aceptada"


        private const val ESTADO_OFERTA_RECHAZADA =
            "rechazada"
    }
}