package com.example.gestaobilhares.data.repository

import android.util.Log
import com.example.gestaobilhares.data.dao.PanoEstoqueDao
import com.example.gestaobilhares.data.entities.PanoEstoque
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository para operações de PanoEstoque
 */
@Singleton
class PanoEstoqueRepository @Inject constructor(
    private val panoEstoqueDao: PanoEstoqueDao
) {
    
    fun listarDisponiveis(): Flow<List<PanoEstoque>> = panoEstoqueDao.listarDisponiveis()
    
    fun listarTodos(): Flow<List<PanoEstoque>> = panoEstoqueDao.listarTodos()
    
    suspend fun buscarPorId(id: Long): PanoEstoque? = panoEstoqueDao.buscarPorId(id)
    
    suspend fun buscarPorNumero(numero: String): PanoEstoque? = panoEstoqueDao.buscarPorNumero(numero)
    
    fun buscarPorCor(cor: String): Flow<List<PanoEstoque>> = panoEstoqueDao.buscarPorCor(cor)
    
    fun buscarPorTamanho(tamanho: String): Flow<List<PanoEstoque>> = panoEstoqueDao.buscarPorTamanho(tamanho)
    
    fun buscarDisponiveisPorTamanho(tamanho: String): Flow<List<PanoEstoque>> = 
        panoEstoqueDao.buscarDisponiveisPorTamanho(tamanho)
    
    fun buscarDisponiveisPorTamanhoECor(tamanho: String, cor: String): Flow<List<PanoEstoque>> = 
        panoEstoqueDao.buscarDisponiveisPorTamanhoECor(tamanho, cor)
    
    suspend fun inserir(pano: PanoEstoque): Long = panoEstoqueDao.inserir(pano)
    
    suspend fun inserirLote(panos: List<PanoEstoque>) = panoEstoqueDao.inserirLote(panos)
    
    suspend fun atualizar(pano: PanoEstoque) = panoEstoqueDao.atualizar(pano)
    
    suspend fun deletar(pano: PanoEstoque) = panoEstoqueDao.deletar(pano)
    
    suspend fun deletarPorId(id: Long) = panoEstoqueDao.deletarPorId(id)
    
    suspend fun atualizarDisponibilidade(id: Long, disponivel: Boolean) = 
        panoEstoqueDao.atualizarDisponibilidade(id, disponivel)
    
    suspend fun contarDisponiveis(): Int = panoEstoqueDao.contarDisponiveis()
    
    suspend fun buscarMaiorNumeroPano(cor: String, tamanho: String): Int? = 
        panoEstoqueDao.buscarMaiorNumeroPano(cor, tamanho)
    
    fun buscarPorIntervalo(numeroInicial: String, numeroFinal: String): Flow<List<PanoEstoque>> = 
        panoEstoqueDao.buscarPorIntervalo(numeroInicial, numeroFinal)
    
    /**
     * ✅ NOVO: Marca um pano como usado (indisponível) no estoque
     */
    suspend fun marcarPanoComoUsado(panoId: Long, motivo: String = "Usado em reforma/acerto") {
        try {
            Log.d("PanoEstoqueRepository", "Marcando pano $panoId como usado: $motivo")
            panoEstoqueDao.atualizarDisponibilidade(panoId, false)
            Log.d("PanoEstoqueRepository", "Pano $panoId marcado como usado com sucesso")
        } catch (e: Exception) {
            Log.e("PanoEstoqueRepository", "Erro ao marcar pano como usado: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * ✅ NOVO: Marca um pano como usado pelo número
     */
    suspend fun marcarPanoComoUsadoPorNumero(numero: String, motivo: String = "Usado em reforma/acerto") {
        try {
            Log.d("PanoEstoqueRepository", "Marcando pano $numero como usado: $motivo")
            val pano = panoEstoqueDao.buscarPorNumero(numero)
            if (pano != null) {
                panoEstoqueDao.atualizarDisponibilidade(pano.id, false)
                Log.d("PanoEstoqueRepository", "Pano $numero (ID: ${pano.id}) marcado como usado com sucesso")
            } else {
                Log.e("PanoEstoqueRepository", "Pano $numero não encontrado no estoque")
            }
        } catch (e: Exception) {
            Log.e("PanoEstoqueRepository", "Erro ao marcar pano como usado: ${e.message}", e)
            throw e
        }
    }
}
