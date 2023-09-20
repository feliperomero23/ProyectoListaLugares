package com.example.proyectolistalugares.APIDOLAR

import retrofit2.http.GET

interface TasaCambioApiService {
    @GET("dolar")
    suspend fun obtenerTasaCambio(): TasaCambioResponse
}