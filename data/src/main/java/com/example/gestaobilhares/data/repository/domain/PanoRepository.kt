package com.example.gestaobilhares.data.repository.domain

import com.example.gestaobilhares.data.dao.PanoEstoqueDao
import com.example.gestaobilhares.data.entities.PanoEstoque
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Repository especializado para operações relacionadas a panos de estoque.
 * Segue arquitetura híbrida modular: AppRepository como Facade.
 */
class PanoRepository(
    private val panoEstoqueDao: PanoEstoqueDao?
) {
    
    fun obterDisponiveis(): Flow<List<PanoEstoque>> = panoEstoqueDao?.listarDisponiveis() ?: flowOf(emptyList())
    fun obterTodos(): Flow<List<PanoEstoque>> = panoEstoqueDao?.listarTodos() ?: flowOf(emptyList())
    suspend fun buscarPorNumero(numero: String) = panoEstoqueDao?.buscarPorNumero(numero)
    suspend fun obterPorId(id: Long) = panoEstoqueDao?.buscarPorId(id)
    suspend fun inserir(pano: PanoEstoque): Long = panoEstoqueDao?.inserir(pano) ?: 0L
    /**
     * ✅ V10: Logs detalhados para rastreamento
     */
    suspend fun inserirLote(panos: List<PanoEstoque>) {
        android.util.Log.d("PanoRepository", "=== INÍCIO inserirLote ===")
        android.util.Log.d("PanoRepository", "Recebidos ${panos.size} panos para inserir no DAO")
        panoEstoqueDao?.inserirLote(panos)
        android.util.Log.d("PanoRepository", "=== FIM inserirLote - DAO concluído ===")
    }
    
    suspend fun marcarComoUsado(id: Long) = panoEstoqueDao?.atualizarDisponibilidade(id, false)
    suspend fun marcarComoUsadoPorNumero(numero: String) {
        val pano = buscarPorNumero(numero)
        pano?.let { marcarComoUsado(it.id) }
    }

    suspend fun atualizar(pano: PanoEstoque) = panoEstoqueDao?.atualizar(pano)
}

