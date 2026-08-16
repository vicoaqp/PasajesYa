package com.PasajesYa.pasajesya

import com.google.firebase.Timestamp

data class EncomiendaLocal(

    val id: String = "",

    val tipoSolicitud: String = "",

    val usuarioId: String = "",

    val descripcion: String = "",

    val cantidad: Int = 0,

    val pesoAproximado: String = "",

    val tamano: String = "",

    val fotoUrl: String = "",

    val direccionRecojo: String = "",

    val referenciaRecojo: String = "",

    val direccionEntrega: String = "",

    val referenciaEntrega: String = "",

    val indicaciones: String = "",

    val tipoUnidad: String = "",

    val estado: String = "",

    val aceptaOfertas: Boolean = true,

    val cantidadOfertas: Int = 0,

    val conductorSeleccionadoId: String = "",

    val conductorSeleccionadoNombre: String = "",

    val conductorSeleccionadoCelular: String = "",

    val precioAceptado: Double = 0.0,

    val fechaPublicacion: Timestamp? = null,

    val fechaActualizacion: Timestamp? = null
)