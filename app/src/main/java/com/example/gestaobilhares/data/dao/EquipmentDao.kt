package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.Equipment
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operações de Equipment no banco de dados
 */
@Dao
interface EquipmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(equipment: Equipment): Long

    @Update
    suspend fun atualizar(equipment: Equipment)

    @Delete
    suspend fun deletar(equipment: Equipment)

    @Query("SELECT * FROM equipments ORDER BY name ASC")
    fun listarTodos(): Flow<List<Equipment>>

    @Query("SELECT * FROM equipments WHERE id = :id")
    suspend fun buscarPorId(id: Long): Equipment?

    @Query("SELECT * FROM equipments WHERE name LIKE '%' || :search || '%' ORDER BY name ASC")
    fun buscarPorNome(search: String): Flow<List<Equipment>>

    @Query("SELECT * FROM equipments WHERE location = :location ORDER BY name ASC")
    fun buscarPorLocalizacao(location: String): Flow<List<Equipment>>

    @Query("DELETE FROM equipments WHERE id = :id")
    suspend fun deletarPorId(id: Long)
}

