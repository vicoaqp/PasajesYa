package com.PasajesYa.pasajesya

import com.google.firebase.Timestamp

data class OfertaEncomienda(

    val conductorId: String = "",

    val conductorNombre: String = "",

    val conductorCelular: String = "",

    val tipoServicio: String = "",

    val placa: String = "",

    val marca: String = "",

    val modelo: String = "",

    val color: String = "",

    val fotoPerfilUrl: String = "",

    val fotoUnidadUrl: String = "",

    val precioServicio: Double = 0.0,

    val estadoOferta: String = "pendiente",

    val fechaOferta: Timestamp? = null
)