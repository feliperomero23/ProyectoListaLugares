package com.example.proyectolistalugares

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Establece una estructura de base de datos utilizando Room, proporcionando un punto de acceso para interactuar con la base de datos.

@Database(entities = [Lugar::class], version = 1 )
abstract class AppDatabase : RoomDatabase() {
    abstract fun lugarDao():LugarDao

    companion object {
        @Volatile private var BASE_DATOS : AppDatabase? = null
        fun getInstance(contexto: Context):AppDatabase {
            return BASE_DATOS ?: synchronized(this) {
            Room.databaseBuilder(
                contexto.applicationContext,
                AppDatabase::class.java,
                "Lugaresbd.bd"
            )
                .fallbackToDestructiveMigration()
                .build()
                .also { BASE_DATOS = it } } }
        }
    }
