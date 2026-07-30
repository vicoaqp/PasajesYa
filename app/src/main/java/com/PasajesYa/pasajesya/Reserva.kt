package com.PasajesYa.pasajesya

import com.google.firebase.Timestamp

data class Reserva(

    val id: String = "",
    val documentoPath: String = "",

    val empresaId: String = "",
    val empresaNombre: String = "",

    val viajeId: String = "",

    val usuarioId: String = "",
    val usuarioCorreo: String = "",

    val compradorDni: String = "",
    val compradorNombres: String = "",
    val compradorApellidos: String = "",
    val compradorCelular: String = "",

    val pasajeroDni: String = "",
    val pasajeroNombres: String = "",
    val pasajeroApellidos: String = "",
    val pasajeroCelular: String = "",

    val esTitular: Boolean = false,

    val datosPasajeroPendientes:
    Boolean = false,

    val tipoPasajero: String = "",

    val origen: String = "",
    val destino: String = "",

    val asiento: String = "",
    val precio: Double = 0.0,

    val estadoReserva: String = "",
    val estadoPago: String = "",

    val fechaReserva: Timestamp? = null,

    val fechaSalida: String = "",
    val horaSalida: String = "",
    val puntoEmbarque: String = "",

    val placa: String = "",
    val vehiculoDescripcion: String = ""

) {

    fun nombrePasajero(): String {

        if (datosPasajeroPendientes) {
            return "Datos por completar"
        }

        return listOf(
            pasajeroNombres.trim(),
            pasajeroApellidos.trim()
        )
            .filter { texto ->
                texto.isNotBlank()
            }
            .joinToString(" ")
            .ifBlank {
                "Pasajero no registrado"
            }
    }
}