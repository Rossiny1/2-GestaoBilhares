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
    suspend fun marcarComoUsado(id: Long) = panoEstoqueDao?.atualizarDisponibilidade(id, false)
    suspend fun marcarComoUsadoPorNumero(numero: String) {
        val pano = buscarPorNumero(numero)
        pano?.let { marcarComoUsado(it.id) }
    }
}

