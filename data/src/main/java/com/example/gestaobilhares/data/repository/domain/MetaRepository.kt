package com.example.gestaobilhares.data.repository.domain

import com.example.gestaobilhares.data.dao.MetaDao
import com.example.gestaobilhares.data.entities.MetaEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Repository especializado para operações relacionadas a metas.
 * Segue arquitetura híbrida modular: AppRepository como Facade.
 */
class MetaRepository(
    private val metaDao: MetaDao?
) {
    
    fun obterTodas(): Flow<List<MetaEntity>> = metaDao?.obterTodas() ?: flowOf(emptyList())
    suspend fun inserir(meta: MetaEntity): Long = metaDao?.inserir(meta) ?: 0L
    suspend fun atualizar(meta: MetaEntity) = metaDao?.atualizar(meta)
    suspend fun obterPorId(id: Long) = metaDao?.obterPorId(id)
}

