package com.example.proyectolistalugares.APIDOLAR

data class TasaCambioResponse(
    val dolar: TasaCambioDolar
)

data class TasaCambioDolar(
    val valor: Double
)
