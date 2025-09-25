package com.example.gestaobilhares.data.repository

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
        panoEstoqueDao.atualizarDisponibilidade(panoId, false)
    }
    
    /**
     * ✅ NOVO: Marca um pano como usado pelo número
     */
    suspend fun marcarPanoComoUsadoPorNumero(numero: String, motivo: String = "Usado em reforma/acerto") {
        val pano = panoEstoqueDao.buscarPorNumero(numero)
        pano?.let {
            panoEstoqueDao.atualizarDisponibilidade(it.id, false)
        }
    }
}
