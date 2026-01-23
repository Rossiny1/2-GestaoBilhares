package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.PanoEstoque
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operações de PanoEstoque no banco de dados
 */
@Dao
abstract class PanoEstoqueDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun inserir(pano: PanoEstoque): Long

    @Query("SELECT * FROM panos_estoque WHERE disponivel = 1 ORDER BY numero ASC")
    abstract fun listarDisponiveis(): Flow<List<PanoEstoque>>

    @Query("SELECT * FROM panos_estoque ORDER BY numero ASC")
    abstract fun listarTodos(): Flow<List<PanoEstoque>>

    @Query("SELECT * FROM panos_estoque WHERE id = :id")
    abstract suspend fun buscarPorId(id: Long): PanoEstoque?

    @Query("SELECT * FROM panos_estoque WHERE numero = :numero")
    abstract suspend fun buscarPorNumero(numero: String): PanoEstoque?

    @Query("SELECT * FROM panos_estoque WHERE cor = :cor AND disponivel = 1 ORDER BY numero ASC")
    abstract fun buscarPorCor(cor: String): Flow<List<PanoEstoque>>

    @Query("SELECT * FROM panos_estoque WHERE tamanho = :tamanho AND disponivel = 1 ORDER BY numero ASC")
    abstract fun buscarPorTamanho(tamanho: String): Flow<List<PanoEstoque>>

    @Query("SELECT COUNT(*) FROM panos_estoque WHERE disponivel = 1")
    abstract suspend fun contarDisponiveis(): Int

    @Update
    abstract suspend fun atualizar(pano: PanoEstoque)

    @Delete
    abstract suspend fun deletar(pano: PanoEstoque)

    @Query("DELETE FROM panos_estoque WHERE id = :id")
    abstract suspend fun deletarPorId(id: Long)

    @Query("UPDATE panos_estoque SET disponivel = :disponivel WHERE id = :id")
    abstract suspend fun atualizarDisponibilidade(id: Long, disponivel: Boolean)
    
    /**
     * ✅ NOVO: Busca panos disponíveis por tamanho
     */
    @Query("SELECT * FROM panos_estoque WHERE tamanho = :tamanho AND disponivel = 1 ORDER BY numero ASC")
    abstract fun buscarDisponiveisPorTamanho(tamanho: String): Flow<List<PanoEstoque>>
    
    /**
     * ✅ NOVO: Busca panos disponíveis por tamanho e cor
     */
    @Query("SELECT * FROM panos_estoque WHERE tamanho = :tamanho AND cor = :cor AND disponivel = 1 ORDER BY numero ASC")
    abstract fun buscarDisponiveisPorTamanhoECor(tamanho: String, cor: String): Flow<List<PanoEstoque>>
    
    /**
     * ✅ CORRIGIDO V10: Insere panos em lote com @Transaction e inserções individuais
     * Cada inserção individual notifica o Flow corretamente
     */
    @Transaction
    open suspend fun inserirLote(panos: List<PanoEstoque>) {
        android.util.Log.d("PanoEstoqueDao", "=== INÍCIO inserirLote @Transaction ===")
        android.util.Log.d("PanoEstoqueDao", "Inserindo ${panos.size} panos individualmente...")
        panos.forEachIndexed { index, pano ->
            inserir(pano)
            android.util.Log.d("PanoEstoqueDao", "Pano ${index + 1}/${panos.size} inserido: ${pano.numero}")
        }
        android.util.Log.d("PanoEstoqueDao", "=== FIM inserirLote - ${panos.size} panos inseridos ===")
    }
    
    /**
     * ✅ NOVO: Busca o maior número de pano para uma cor e tamanho específicos
     */
    @Query("SELECT MAX(CAST(SUBSTR(numero, 2) AS INTEGER)) FROM panos_estoque WHERE cor = :cor AND tamanho = :tamanho")
    abstract suspend fun buscarMaiorNumeroPano(cor: String, tamanho: String): Int?
    
    /**
     * ✅ NOVO: Busca panos por intervalo de numeração
     */
    @Query("SELECT * FROM panos_estoque WHERE numero BETWEEN :numeroInicial AND :numeroFinal AND disponivel = 1 ORDER BY numero ASC")
    abstract fun buscarPorIntervalo(numeroInicial: String, numeroFinal: String): Flow<List<PanoEstoque>>
}
