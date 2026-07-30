package com.PasajesYa.pasajesya

data class Empresa(
    val id: String = "",
    val nombre: String = "",
    val nombreCorto: String = "",
    val descripcion: String = "",
    val rutasResumen: String = "",
    val telefono: String = "",
    val logoUrl: String = "",
    val colorPrincipal: String = "#1261A6",
    val colorSecundario: String = "#FFB300",
    val estado: String = "",
    val visibleUsuarios: Boolean = false,
    val orden: Long = 0L
)