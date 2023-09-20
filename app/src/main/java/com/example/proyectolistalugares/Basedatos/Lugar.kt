package com.example.proyectolistalugares

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity // entidad en mi base de datos
data class Lugar(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    //Propiedades de mi bd
    val lugar: String,
    val imagendeReferencia: String,
    var latitud: Double,
    var longitud: Double,
    val orden: Int,
    val costoxAlojamiento: Double,
    val costoxTraslado: Double,
    val comentarios: String
)