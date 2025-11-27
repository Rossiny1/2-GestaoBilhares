package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.Equipment
import kotlinx.coroutines.flow.Flow

@Dao
interface EquipmentDao {
    @Query("SELECT * FROM equipments ORDER BY name")
    fun listar(): Flow<List<Equipment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(equipment: Equipment): Long

    @Update
    suspend fun atualizar(equipment: Equipment)

    @Delete
    suspend fun deletar(equipment: Equipment)
}

