package com.PasajesYa.pasajesya

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.PasajesYa.pasajesya.databinding.ActivityOfertasMovilidadBinding
import com.PasajesYa.pasajesya.databinding.ItemOfertaMovilidadBinding
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Locale


class OfertasMovilidadActivity : AppCompatActivity() {


    // =========================================================
    // VIEW BINDING
    // =========================================================

    private lateinit var binding:
            ActivityOfertasMovilidadBinding


    // =========================================================
    // FIREBASE
    // =========================================================

    private lateinit var auth:
            FirebaseAuth

    private lateinit var firestore:
            FirebaseFirestore


    // =========================================================
    // ID DE SOLICITUD
    // =========================================================

    private var solicitudId =
        ""


    // =========================================================
    // LISTENERS FIRESTORE
    // =========================================================

    private var listenerSolicitud:
            ListenerRegistration? = null

    private var listenerOfertas:
            ListenerRegistration? = null


    // =========================================================
    // LISTA DE OFERTAS
    // =========================================================

    private val listaOfertas =
        mutableListOf<OfertaMovilidad>()


    private lateinit var adapter:
            OfertasAdapter


    // =========================================================
    // ESTADO DE SOLICITUD
    // =========================================================

    private var estadoSolicitud =
        ESTADO_BUSCANDO_OFERTAS


    private var aceptandoOferta =
        false


    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)


        binding =
            ActivityOfertasMovilidadBinding.inflate(
                layoutInflater
            )


        setContentView(
            binding.root
        )


        // Firebase

        auth =
            FirebaseAuth.getInstance()

        firestore =
            FirebaseFirestore.getInstance()


        // Obtener solicitudId

        solicitudId =
            intent.getStringExtra(
                EXTRA_SOLICITUD_ID
            ).orEmpty()


        if (solicitudId.isBlank()) {

            Toast.makeText(
                this,
                "No se encontró la solicitud de movilidad.",
                Toast.LENGTH_LONG
            ).show()

            finish()

            return
        }


        configurarRecycler()

        configurarEventos()

        escucharSolicitud()

        escucharOfertas()
    }


    // =========================================================
    // CONFIGURAR RECYCLER
    // =========================================================

    private fun configurarRecycler() {

        adapter =
            OfertasAdapter(
                listaOfertas
            ) { oferta ->

                confirmarAceptarOferta(
                    oferta
                )
            }


        binding.recyclerOfertas.layoutManager =
            LinearLayoutManager(this)


        binding.recyclerOfertas.adapter =
            adapter
    }


    // =========================================================
    // EVENTOS
    // =========================================================

    private fun configurarEventos() {


        // -----------------------------------------------------
        // VOLVER
        // -----------------------------------------------------

        binding.btnVolver.setOnClickListener {

            finish()
        }


        // -----------------------------------------------------
        // CANCELAR SOLICITUD
        // -----------------------------------------------------

        binding.btnCancelarSolicitud.setOnClickListener {

            confirmarCancelarSolicitud()
        }
    }


    // =========================================================
    // ESCUCHAR SOLICITUD PRINCIPAL
    // =========================================================

    private fun escucharSolicitud() {

        val usuario =
            auth.currentUser


        if (usuario == null) {

            Toast.makeText(
                this,
                "Tu sesión ha finalizado.",
                Toast.LENGTH_LONG
            ).show()

            finish()

            return
        }


        listenerSolicitud =
            firestore
                .collection(COLECCION_SOLICITUDES)
                .document(solicitudId)
                .addSnapshotListener { documento, error ->


                    if (error != null) {

                        Toast.makeText(
                            this,
                            "No se pudo actualizar la solicitud.",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@addSnapshotListener
                    }


                    if (
                        documento == null ||
                        !documento.exists()
                    ) {

                        Toast.makeText(
                            this,
                            "La solicitud ya no existe.",
                            Toast.LENGTH_LONG
                        ).show()

                        finish()

                        return@addSnapshotListener
                    }


                    // =================================================
                    // SEGURIDAD BÁSICA
                    // Verificamos que la solicitud sea del usuario
                    // =================================================

                    val usuarioId =
                        documento.getString(
                            "usuarioId"
                        ).orEmpty()


                    if (
                        usuarioId.isNotBlank() &&
                        usuarioId != usuario.uid
                    ) {

                        Toast.makeText(
                            this,
                            "No tienes acceso a esta solicitud.",
                            Toast.LENGTH_LONG
                        ).show()

                        finish()

                        return@addSnapshotListener
                    }


                    // =================================================
                    // RUTA
                    // =================================================

                    val origen =
                        documento.get(
                            "origen"
                        ) as? Map<*, *>


                    val destino =
                        documento.get(
                            "destino"
                        ) as? Map<*, *>


                    val direccionOrigen =
                        origen
                            ?.get("direccion")
                            ?.toString()
                            .orEmpty()


                    val direccionDestino =
                        destino
                            ?.get("direccion")
                            ?.toString()
                            .orEmpty()


                    binding.tvOrigen.text =

                        if (
                            direccionOrigen.isNotBlank()
                        ) {

                            direccionOrigen

                        } else {

                            "Origen seleccionado"
                        }


                    binding.tvDestino.text =

                        if (
                            direccionDestino.isNotBlank()
                        ) {

                            direccionDestino

                        } else {

                            "Destino seleccionado"
                        }


                    // =================================================
                    // ESTADO
                    // =================================================

                    estadoSolicitud =
                        documento.getString(
                            "estado"
                        ) ?: ESTADO_BUSCANDO_OFERTAS


                    actualizarEstadoPantalla(
                        estadoSolicitud
                    )
                }
    }


    // =========================================================
    // ESCUCHAR OFERTAS EN TIEMPO REAL
    // =========================================================

    private fun escucharOfertas() {

        listenerOfertas =
            firestore
                .collection(COLECCION_SOLICITUDES)
                .document(solicitudId)
                .collection(SUBCOLECCION_OFERTAS)
                .addSnapshotListener { snapshot, error ->


                    if (error != null) {

                        Toast.makeText(
                            this,
                            "No se pudieron cargar las ofertas.",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@addSnapshotListener
                    }


                    if (snapshot == null) {

                        return@addSnapshotListener
                    }


                    listaOfertas.clear()


                    for (documento in snapshot.documents) {


                        val precio =
                            (
                                    documento.get(
                                        "precio"
                                    ) as? Number
                                    )?.toDouble()
                                ?: 0.0


                        val oferta =
                            OfertaMovilidad(

                                ofertaId =
                                    documento.id,

                                conductorId =
                                    documento.getString(
                                        "conductorId"
                                    ).orEmpty(),

                                nombreConductor =
                                    documento.getString(
                                        "nombreConductor"
                                    ).orEmpty(),

                                telefono =
                                    documento.getString(
                                        "telefono"
                                    ).orEmpty(),

                                fotoConductor =
                                    documento.getString(
                                        "fotoConductor"
                                    ).orEmpty(),

                                vehiculo =
                                    documento.getString(
                                        "vehiculo"
                                    ).orEmpty(),

                                placa =
                                    documento.getString(
                                        "placa"
                                    ).orEmpty(),

                                precio =
                                    precio,

                                mensaje =
                                    documento.getString(
                                        "mensaje"
                                    ).orEmpty(),

                                estado =
                                    documento.getString(
                                        "estado"
                                    ) ?: ESTADO_OFERTA_PENDIENTE
                            )


                        // =================================================
                        // MOSTRAR OFERTAS VÁLIDAS
                        // =================================================

                        if (
                            oferta.estado ==
                            ESTADO_OFERTA_PENDIENTE ||
                            oferta.estado ==
                            ESTADO_OFERTA_ACEPTADA
                        ) {

                            listaOfertas.add(
                                oferta
                            )
                        }
                    }


                    // =================================================
                    // ORDENAR POR PRECIO
                    // MENOR PRECIO PRIMERO
                    // =================================================

                    listaOfertas.sortBy {
                        it.precio
                    }


                    adapter.notifyDataSetChanged()


                    actualizarCantidadOfertas()
                }
    }


    // =========================================================
    // CANTIDAD DE OFERTAS
    // =========================================================

    private fun actualizarCantidadOfertas() {

        val cantidad =
            listaOfertas.count {
                it.estado ==
                        ESTADO_OFERTA_PENDIENTE
            }


        if (
            estadoSolicitud !=
            ESTADO_BUSCANDO_OFERTAS
        ) {

            return
        }


        if (cantidad == 0) {

            binding.progressBuscando.visibility =
                View.VISIBLE


            binding.tvEstado.text =
                "Buscando ofertas..."


            binding.tvCantidadOfertas.text =
                "Los conductores disponibles podrán enviarte un precio"


        } else {


            binding.progressBuscando.visibility =
                View.VISIBLE


            binding.tvEstado.text =

                if (cantidad == 1) {

                    "¡Recibiste una oferta!"

                } else {

                    "¡Ya tienes ofertas!"
                }


            binding.tvCantidadOfertas.text =

                if (cantidad == 1) {

                    "1 conductor te envió un precio"

                } else {

                    "$cantidad conductores te enviaron precios"
                }
        }
    }


    // =========================================================
    // ACTUALIZAR ESTADO DE SOLICITUD
    // =========================================================

    private fun actualizarEstadoPantalla(
        estado: String
    ) {

        when (estado) {


            // -------------------------------------------------
            // BUSCANDO
            // -------------------------------------------------

            ESTADO_BUSCANDO_OFERTAS -> {

                binding.progressBuscando.visibility =
                    View.VISIBLE


                binding.btnCancelarSolicitud.visibility =
                    View.VISIBLE


                adapter.permitirAceptar =
                    true


                adapter.notifyDataSetChanged()


                actualizarCantidadOfertas()
            }


            // -------------------------------------------------
            // CONDUCTOR SELECCIONADO
            // -------------------------------------------------

            ESTADO_CONDUCTOR_SELECCIONADO -> {

                binding.progressBuscando.visibility =
                    View.GONE


                binding.tvEstado.text =
                    "Conductor seleccionado"


                binding.tvCantidadOfertas.text =
                    "Tu oferta fue aceptada. Espera a que el conductor inicie la ruta."


                binding.btnCancelarSolicitud.visibility =
                    View.GONE


                adapter.permitirAceptar =
                    false


                adapter.notifyDataSetChanged()
            }


            // -------------------------------------------------
            // EN RUTA
            // -------------------------------------------------

            ESTADO_EN_RUTA -> {

                binding.progressBuscando.visibility =
                    View.GONE


                binding.tvEstado.text =
                    "Tu conductor está en ruta"


                binding.tvCantidadOfertas.text =
                    "El conductor se dirige hacia tu ubicación."


                binding.btnCancelarSolicitud.visibility =
                    View.GONE


                adapter.permitirAceptar =
                    false


                adapter.notifyDataSetChanged()
            }


            // -------------------------------------------------
            // CONDUCTOR LLEGÓ
            // -------------------------------------------------

            ESTADO_CONDUCTOR_LLEGO -> {

                binding.progressBuscando.visibility =
                    View.GONE


                binding.tvEstado.text =
                    "Tu conductor llegó"


                binding.tvCantidadOfertas.text =
                    "El conductor está esperando en el punto de recojo."


                binding.btnCancelarSolicitud.visibility =
                    View.GONE
            }


            // -------------------------------------------------
            // EN VIAJE
            // -------------------------------------------------

            ESTADO_EN_VIAJE -> {

                binding.progressBuscando.visibility =
                    View.GONE


                binding.tvEstado.text =
                    "Viaje en curso"


                binding.tvCantidadOfertas.text =
                    "Tu viaje hacia el destino ha comenzado."


                binding.btnCancelarSolicitud.visibility =
                    View.GONE
            }


            // -------------------------------------------------
            // FINALIZADO
            // -------------------------------------------------

            ESTADO_FINALIZADO -> {

                binding.progressBuscando.visibility =
                    View.GONE


                binding.tvEstado.text =
                    "Viaje finalizado"


                binding.tvCantidadOfertas.text =
                    "El servicio fue completado."


                binding.btnCancelarSolicitud.visibility =
                    View.GONE
            }


            // -------------------------------------------------
            // CANCELADO
            // -------------------------------------------------

            ESTADO_CANCELADO_USUARIO,
            ESTADO_CANCELADO_CONDUCTOR -> {

                binding.progressBuscando.visibility =
                    View.GONE


                binding.tvEstado.text =
                    "Solicitud cancelada"


                binding.tvCantidadOfertas.text =
                    "Este servicio ya no está activo."


                binding.btnCancelarSolicitud.visibility =
                    View.GONE


                adapter.permitirAceptar =
                    false


                adapter.notifyDataSetChanged()
            }
        }
    }


    // =========================================================
    // CONFIRMAR ACEPTAR OFERTA
    // =========================================================

    private fun confirmarAceptarOferta(
        oferta: OfertaMovilidad
    ) {

        if (aceptandoOferta) {

            return
        }


        if (
            estadoSolicitud !=
            ESTADO_BUSCANDO_OFERTAS
        ) {

            Toast.makeText(
                this,
                "Esta solicitud ya no acepta ofertas.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        if (
            oferta.estado !=
            ESTADO_OFERTA_PENDIENTE
        ) {

            Toast.makeText(
                this,
                "Esta oferta ya no está disponible.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        val precioTexto =
            String.format(
                Locale.US,
                "S/ %.2f",
                oferta.precio
            )


        MaterialAlertDialogBuilder(this)
            .setTitle(
                "¿Aceptar esta oferta?"
            )
            .setMessage(
                "${oferta.nombreConductor}\n\n" +
                        "Precio ofrecido: $precioTexto\n\n" +
                        "Al aceptar, las demás ofertas dejarán de estar disponibles."
            )
            .setNegativeButton(
                "VOLVER",
                null
            )
            .setPositiveButton(
                "ACEPTAR"
            ) { _, _ ->

                aceptarOferta(
                    oferta
                )
            }
            .show()
    }


    // =========================================================
    // ACEPTAR OFERTA
    // =========================================================

    private fun aceptarOferta(
        oferta: OfertaMovilidad
    ) {

        if (aceptandoOferta) {

            return
        }


        aceptandoOferta =
            true


        adapter.permitirAceptar =
            false


        adapter.notifyDataSetChanged()


        val solicitudRef =
            firestore
                .collection(COLECCION_SOLICITUDES)
                .document(solicitudId)


        val ofertaRef =
            solicitudRef
                .collection(SUBCOLECCION_OFERTAS)
                .document(oferta.ofertaId)


        firestore
            .runTransaction { transaction ->


                // =================================================
                // LEER SOLICITUD
                // =================================================

                val solicitudSnapshot =
                    transaction.get(
                        solicitudRef
                    )


                val estadoActual =
                    solicitudSnapshot.getString(
                        "estado"
                    )


                if (
                    estadoActual !=
                    ESTADO_BUSCANDO_OFERTAS
                ) {

                    throw IllegalStateException(
                        "La solicitud ya no está disponible."
                    )
                }


                // =================================================
                // LEER OFERTA
                // =================================================

                val ofertaSnapshot =
                    transaction.get(
                        ofertaRef
                    )


                if (!ofertaSnapshot.exists()) {

                    throw IllegalStateException(
                        "La oferta ya no existe."
                    )
                }


                val estadoOferta =
                    ofertaSnapshot.getString(
                        "estado"
                    ) ?: ESTADO_OFERTA_PENDIENTE


                if (
                    estadoOferta !=
                    ESTADO_OFERTA_PENDIENTE
                ) {

                    throw IllegalStateException(
                        "La oferta ya no está disponible."
                    )
                }


                // =================================================
                // ACTUALIZAR SOLICITUD
                // =================================================

                transaction.update(
                    solicitudRef,
                    mapOf(

                        "estado" to
                                ESTADO_CONDUCTOR_SELECCIONADO,

                        "conductorSeleccionadoId" to
                                oferta.conductorId,

                        "ofertaSeleccionadaId" to
                                oferta.ofertaId,

                        "precioAceptado" to
                                oferta.precio,

                        "nombreConductor" to
                                oferta.nombreConductor,

                        "telefonoConductor" to
                                oferta.telefono,

                        "fotoConductor" to
                                oferta.fotoConductor,

                        "vehiculoConductor" to
                                oferta.vehiculo,

                        "placaConductor" to
                                oferta.placa,

                        "fechaAceptacion" to
                                FieldValue.serverTimestamp(),

                        "fechaActualizacion" to
                                FieldValue.serverTimestamp()
                    )
                )


                // =================================================
                // MARCAR OFERTA COMO ACEPTADA
                // =================================================

                transaction.update(
                    ofertaRef,
                    mapOf(

                        "estado" to
                                ESTADO_OFERTA_ACEPTADA,

                        "fechaAceptacion" to
                                FieldValue.serverTimestamp()
                    )
                )


                true
            }
            .addOnSuccessListener {


                aceptandoOferta =
                    false


                marcarOtrasOfertasComoNoSeleccionadas(
                    oferta.ofertaId
                )


                mostrarOfertaAceptada(
                    oferta
                )
            }
            .addOnFailureListener { error ->


                aceptandoOferta =
                    false


                adapter.permitirAceptar =
                    estadoSolicitud ==
                            ESTADO_BUSCANDO_OFERTAS


                adapter.notifyDataSetChanged()


                Toast.makeText(
                    this,
                    error.message
                        ?: "No se pudo aceptar la oferta.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }


    // =========================================================
    // DESCARTAR LAS DEMÁS OFERTAS
    // =========================================================

    private fun marcarOtrasOfertasComoNoSeleccionadas(
        ofertaAceptadaId: String
    ) {

        val ofertasRef =
            firestore
                .collection(COLECCION_SOLICITUDES)
                .document(solicitudId)
                .collection(SUBCOLECCION_OFERTAS)


        ofertasRef
            .whereEqualTo(
                "estado",
                ESTADO_OFERTA_PENDIENTE
            )
            .get()
            .addOnSuccessListener { documentos ->


                if (documentos.isEmpty) {

                    return@addOnSuccessListener
                }


                val batch =
                    firestore.batch()


                var cambios =
                    0


                for (documento in documentos.documents) {


                    if (
                        documento.id ==
                        ofertaAceptadaId
                    ) {

                        continue
                    }


                    batch.update(
                        documento.reference,
                        mapOf(

                            "estado" to
                                    ESTADO_OFERTA_NO_SELECCIONADA,

                            "fechaActualizacion" to
                                    FieldValue.serverTimestamp()
                        )
                    )


                    cambios++
                }


                if (cambios > 0) {

                    batch.commit()
                }
            }
    }


    // =========================================================
    // OFERTA ACEPTADA
    // =========================================================

    private fun mostrarOfertaAceptada(
        oferta: OfertaMovilidad
    ) {

        val precio =
            String.format(
                Locale.US,
                "S/ %.2f",
                oferta.precio
            )


        MaterialAlertDialogBuilder(this)
            .setTitle(
                "Oferta aceptada"
            )
            .setMessage(
                "Elegiste a ${oferta.nombreConductor} por $precio.\n\n" +
                        "El conductor recibirá la confirmación en MovilYa y podrá iniciar la ruta hacia tu ubicación."
            )
            .setPositiveButton(
                "ENTENDIDO",
                null
            )
            .show()


        /*
         * =====================================================
         * SIGUIENTE PASO
         * =====================================================
         *
         * Aquí luego abriremos la pantalla de seguimiento:
         *
         * SeguimientoMovilidadActivity
         *
         * donde aparecerán:
         *
         * - Nombre del conductor
         * - Vehículo
         * - Placa
         * - Precio acordado
         * - Estado
         * - LLAMAR
         * - WHATSAPP
         *
         * Por ahora NO la llamamos para evitar una clase roja
         * que todavía no hemos creado.
         */
    }


    // =========================================================
    // CONFIRMAR CANCELACIÓN
    // =========================================================

    private fun confirmarCancelarSolicitud() {

        if (
            estadoSolicitud !=
            ESTADO_BUSCANDO_OFERTAS
        ) {

            Toast.makeText(
                this,
                "La solicitud ya no puede cancelarse desde esta pantalla.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        MaterialAlertDialogBuilder(this)
            .setTitle(
                "Cancelar solicitud"
            )
            .setMessage(
                "¿Seguro que deseas cancelar la búsqueda de movilidad?"
            )
            .setNegativeButton(
                "NO",
                null
            )
            .setPositiveButton(
                "SÍ, CANCELAR"
            ) { _, _ ->

                cancelarSolicitud()
            }
            .show()
    }


    // =========================================================
    // CANCELAR
    // =========================================================

    private fun cancelarSolicitud() {

        binding.btnCancelarSolicitud.isEnabled =
            false


        firestore
            .collection(COLECCION_SOLICITUDES)
            .document(solicitudId)
            .update(

                mapOf(

                    "estado" to
                            ESTADO_CANCELADO_USUARIO,

                    "fechaCancelacion" to
                            FieldValue.serverTimestamp(),

                    "fechaActualizacion" to
                            FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener {


                cancelarOfertasPendientes()


                Toast.makeText(
                    this,
                    "Solicitud cancelada.",
                    Toast.LENGTH_SHORT
                ).show()


                finish()
            }
            .addOnFailureListener { error ->


                binding.btnCancelarSolicitud.isEnabled =
                    true


                Toast.makeText(
                    this,
                    "No se pudo cancelar: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }


    // =========================================================
    // CANCELAR OFERTAS PENDIENTES
    // =========================================================

    private fun cancelarOfertasPendientes() {

        firestore
            .collection(COLECCION_SOLICITUDES)
            .document(solicitudId)
            .collection(SUBCOLECCION_OFERTAS)
            .whereEqualTo(
                "estado",
                ESTADO_OFERTA_PENDIENTE
            )
            .get()
            .addOnSuccessListener { documentos ->


                if (documentos.isEmpty) {

                    return@addOnSuccessListener
                }


                val batch =
                    firestore.batch()


                for (documento in documentos.documents) {

                    batch.update(
                        documento.reference,
                        mapOf(

                            "estado" to
                                    ESTADO_OFERTA_CANCELADA,

                            "fechaActualizacion" to
                                    FieldValue.serverTimestamp()
                        )
                    )
                }


                batch.commit()
            }
    }


    // =========================================================
    // DESTRUIR LISTENERS
    // =========================================================

    override fun onDestroy() {

        listenerSolicitud?.remove()

        listenerOfertas?.remove()


        super.onDestroy()
    }


    // =========================================================
    // MODELO DE OFERTA
    // =========================================================

    data class OfertaMovilidad(

        val ofertaId: String,

        val conductorId: String,

        val nombreConductor: String,

        val telefono: String,

        val fotoConductor: String,

        val vehiculo: String,

        val placa: String,

        val precio: Double,

        val mensaje: String,

        val estado: String
    )


    // =========================================================
    // ADAPTER
    // =========================================================

    inner class OfertasAdapter(

        private val ofertas:
        List<OfertaMovilidad>,

        private val onAceptar:
            (OfertaMovilidad) -> Unit

    ) : RecyclerView.Adapter<OfertasAdapter.OfertaViewHolder>() {


        var permitirAceptar =
            true


        // =====================================================
        // VIEW HOLDER
        // =====================================================

        inner class OfertaViewHolder(

            val binding:
            ItemOfertaMovilidadBinding

        ) : RecyclerView.ViewHolder(
            binding.root
        )


        // =====================================================
        // CREAR VIEW
        // =====================================================

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): OfertaViewHolder {


            val itemBinding =
                ItemOfertaMovilidadBinding.inflate(

                    LayoutInflater.from(
                        parent.context
                    ),

                    parent,

                    false
                )


            return OfertaViewHolder(
                itemBinding
            )
        }


        // =====================================================
        // CANTIDAD
        // =====================================================

        override fun getItemCount():
                Int {

            return ofertas.size
        }


        // =====================================================
        // MOSTRAR OFERTA
        // =====================================================

        override fun onBindViewHolder(
            holder: OfertaViewHolder,
            position: Int
        ) {

            val oferta =
                ofertas[position]


            val item =
                holder.binding


            // -------------------------------------------------
            // NOMBRE
            // -------------------------------------------------

            item.tvNombreConductor.text =

                if (
                    oferta.nombreConductor.isNotBlank()
                ) {

                    oferta.nombreConductor

                } else {

                    "Conductor"
                }


            // -------------------------------------------------
            // VEHÍCULO
            // -------------------------------------------------

            item.tvVehiculo.text =

                if (
                    oferta.vehiculo.isNotBlank()
                ) {

                    oferta.vehiculo

                } else {

                    "Movilidad"
                }


            // -------------------------------------------------
            // PLACA
            // -------------------------------------------------

            item.tvPlaca.text =

                if (
                    oferta.placa.isNotBlank()
                ) {

                    oferta.placa

                } else {

                    "Placa no registrada"
                }


            // -------------------------------------------------
            // PRECIO
            // -------------------------------------------------

            item.tvPrecio.text =
                String.format(
                    Locale.US,
                    "S/ %.2f",
                    oferta.precio
                )


            // -------------------------------------------------
            // MENSAJE
            // -------------------------------------------------

            if (
                oferta.mensaje.isNotBlank()
            ) {

                item.tvMensajeConductor.visibility =
                    View.VISIBLE


                item.tvMensajeConductor.text =
                    oferta.mensaje

            } else {

                item.tvMensajeConductor.visibility =
                    View.GONE
            }


            // -------------------------------------------------
            // FOTO
            // -------------------------------------------------

            if (
                oferta.fotoConductor.isNotBlank()
            ) {

                Glide
                    .with(this@OfertasMovilidadActivity)
                    .load(oferta.fotoConductor)
                    .placeholder(
                        R.drawable.ic_persona_pasajes
                    )
                    .error(
                        R.drawable.ic_persona_pasajes
                    )
                    .into(
                        item.ivFotoConductor
                    )

            } else {

                item.ivFotoConductor.setImageResource(
                    R.drawable.ic_persona_pasajes
                )
            }


            // -------------------------------------------------
            // BOTÓN
            // -------------------------------------------------

            val ofertaPendiente =
                oferta.estado ==
                        ESTADO_OFERTA_PENDIENTE


            if (
                oferta.estado ==
                ESTADO_OFERTA_ACEPTADA
            ) {

                item.btnAceptarOferta.visibility =
                    View.VISIBLE


                item.btnAceptarOferta.isEnabled =
                    false


                item.btnAceptarOferta.text =
                    "OFERTA ACEPTADA"


            } else {


                item.btnAceptarOferta.visibility =
                    View.VISIBLE


                item.btnAceptarOferta.isEnabled =
                    permitirAceptar &&
                            ofertaPendiente


                item.btnAceptarOferta.text =
                    "ACEPTAR OFERTA"
            }


            item.btnAceptarOferta.setOnClickListener {

                if (
                    permitirAceptar &&
                    ofertaPendiente
                ) {

                    onAceptar(
                        oferta
                    )
                }
            }
        }
    }


    // =========================================================
    // CONSTANTES
    // =========================================================

    companion object {


        // =====================================================
        // EXTRA
        // =====================================================

        const val EXTRA_SOLICITUD_ID =
            "solicitudId"


        // =====================================================
        // FIRESTORE
        // =====================================================

        private const val COLECCION_SOLICITUDES =
            "solicitudesMovilidad"


        private const val SUBCOLECCION_OFERTAS =
            "ofertas"


        // =====================================================
        // ESTADOS SOLICITUD
        // =====================================================

        private const val ESTADO_BUSCANDO_OFERTAS =
            "BUSCANDO_OFERTAS"


        private const val ESTADO_CONDUCTOR_SELECCIONADO =
            "CONDUCTOR_SELECCIONADO"


        private const val ESTADO_EN_RUTA =
            "EN_RUTA"


        private const val ESTADO_CONDUCTOR_LLEGO =
            "CONDUCTOR_LLEGO"


        private const val ESTADO_EN_VIAJE =
            "EN_VIAJE"


        private const val ESTADO_FINALIZADO =
            "FINALIZADO"


        private const val ESTADO_CANCELADO_USUARIO =
            "CANCELADO_USUARIO"


        private const val ESTADO_CANCELADO_CONDUCTOR =
            "CANCELADO_CONDUCTOR"


        // =====================================================
        // ESTADOS OFERTA
        // =====================================================

        private const val ESTADO_OFERTA_PENDIENTE =
            "PENDIENTE"


        private const val ESTADO_OFERTA_ACEPTADA =
            "ACEPTADA"


        private const val ESTADO_OFERTA_NO_SELECCIONADA =
            "NO_SELECCIONADA"


        private const val ESTADO_OFERTA_CANCELADA =
            "CANCELADA"
    }
}