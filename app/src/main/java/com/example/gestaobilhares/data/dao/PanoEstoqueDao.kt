package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.PanoEstoque
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operações de PanoEstoque no banco de dados
 */
@Dao
interface PanoEstoqueDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(pano: PanoEstoque): Long

    @Query("SELECT * FROM panos_estoque WHERE disponivel = 1 ORDER BY numero ASC")
    fun listarDisponiveis(): Flow<List<PanoEstoque>>

    @Query("SELECT * FROM panos_estoque ORDER BY numero ASC")
    fun listarTodos(): Flow<List<PanoEstoque>>

    @Query("SELECT * FROM panos_estoque WHERE id = :id")
    suspend fun buscarPorId(id: Long): PanoEstoque?

    @Query("SELECT * FROM panos_estoque WHERE numero = :numero")
    suspend fun buscarPorNumero(numero: String): PanoEstoque?

    @Query("SELECT * FROM panos_estoque WHERE cor = :cor AND disponivel = 1 ORDER BY numero ASC")
    fun buscarPorCor(cor: String): Flow<List<PanoEstoque>>

    @Query("SELECT * FROM panos_estoque WHERE tamanho = :tamanho AND disponivel = 1 ORDER BY numero ASC")
    fun buscarPorTamanho(tamanho: String): Flow<List<PanoEstoque>>

    @Query("SELECT COUNT(*) FROM panos_estoque WHERE disponivel = 1")
    suspend fun contarDisponiveis(): Int

    @Update
    suspend fun atualizar(pano: PanoEstoque)

    @Delete
    suspend fun deletar(pano: PanoEstoque)

    @Query("DELETE FROM panos_estoque WHERE id = :id")
    suspend fun deletarPorId(id: Long)

    @Query("UPDATE panos_estoque SET disponivel = :disponivel WHERE id = :id")
    suspend fun atualizarDisponibilidade(id: Long, disponivel: Boolean)
}
