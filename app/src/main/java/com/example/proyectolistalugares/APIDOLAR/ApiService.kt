package com.example.proyectolistalugares.APIDOLAR

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiService {
    companion object {
        private const val BASE_URL = "https://mindicador.cl/api/"

        fun create(): TasaCambioApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(TasaCambioApiService::class.java)
        }
    }
}