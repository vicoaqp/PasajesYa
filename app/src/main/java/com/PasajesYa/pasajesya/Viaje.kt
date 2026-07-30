package com.PasajesYa.pasajesya

data class Viaje(
    val id: String = "",
    val rutaId: String = "",
    val origen: String = "",
    val destino: String = "",
    val fechaSalida: String = "",
    val horaSalida: String = "",
    val puntoEmbarque: String = "",
    val choferNombre: String = "",
    val vehiculoDescripcion: String = "",
    val placa: String = "",
    val capacidad: Long = 4L,
    val asientosOcupados: Long = 0L,
    val precio: Double = 0.0,
    val estado: String = "",
    val orden: Long = 0L
) {

    fun asientosDisponibles(): Long {
        return (capacidad - asientosOcupados).coerceAtLeast(0L)
    }
}