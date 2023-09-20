package com.example.proyectolistalugares

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

// Interfaz para interactuar con la base de datos ( Realizar acciones)
@Dao
interface LugarDao {
    @Query("SELECT * FROM Lugar WHERE lugar = :lugarId")
    fun obtenerLugarPorId(lugarId: Long): Lugar

    @Query("SELECT * FROM Lugar ORDER BY orden")
    fun obtenertodosloslugares(): List<Lugar>

    @Query("SELECT COUNT(*) FROM Lugar")
    fun contarRegistros(): Int

    @Insert
    suspend fun insertarlugar(lugar: Lugar): Long

    @Update
    suspend fun actualizarlugar(lugar: Lugar)

    @Delete
    suspend fun eliminarlugar(lugar: Lugar)
}

