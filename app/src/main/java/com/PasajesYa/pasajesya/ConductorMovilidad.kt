package com.PasajesYa.pasajesya

data class ConductorMovilidad(

    val id: String = "",

    val nombres: String = "",

    val apellidos: String = "",

    val nombreCompleto: String = "",

    val celular: String = "",

    val tipoServicio: String = "",

    val placa: String = "",

    val marca: String = "",

    val modelo: String = "",

    val color: String = "",

    val fotoPerfilUrl: String = "",

    val fotoUnidadUrl: String = "",

    val estado: String = "",

    val enLinea: Boolean = false,

    val disponible: Boolean = false,

    val estadoServicio: String = ""
) {

    fun obtenerNombre(): String {

        if (nombreCompleto.isNotBlank()) {

            return nombreCompleto
        }

        return "$nombres $apellidos"
            .trim()
            .ifBlank {

                "Conductor MovilYa"
            }
    }
}