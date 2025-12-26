package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.Veiculo
import kotlinx.coroutines.flow.Flow

@Dao
interface VeiculoDao {
    @Query("SELECT * FROM veiculos ORDER BY marca, modelo")
    fun listar(): Flow<List<Veiculo>>

    @Query("SELECT * FROM veiculos WHERE id = :id")
    suspend fun buscarPorId(id: Long): Veiculo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(veiculo: Veiculo): Long

    @Update
    suspend fun atualizar(veiculo: Veiculo)

    @Delete
    suspend fun deletar(veiculo: Veiculo)
}


