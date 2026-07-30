package com.PasajesYa.pasajesya

data class DatosPasajero(
    val dni: String = "",
    val nombres: String = "",
    val apellidos: String = "",
    val celular: String = "",
    val esTitular: Boolean = false,
    val datosPendientes: Boolean = false
) {

    fun nombreCompleto(): String {

        return listOf(
            nombres.trim(),
            apellidos.trim()
        )
            .filter { texto ->
                texto.isNotBlank()
            }
            .joinToString(" ")
    }
}