package com.example.gestaobilhares.data.repository.domain

import com.example.gestaobilhares.data.dao.MetaDao
import com.example.gestaobilhares.data.entities.Meta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Repository especializado para operações relacionadas a metas.
 * Segue arquitetura híbrida modular: AppRepository como Facade.
 */
class MetaRepository(
    private val metaDao: MetaDao?
) {
    
    fun obterTodas(): Flow<List<Meta>> = metaDao?.getAllMetas() ?: flowOf(emptyList())
    suspend fun inserir(meta: Meta): Long = metaDao?.insert(meta) ?: 0L
    suspend fun atualizar(meta: Meta) = metaDao?.update(meta)
    suspend fun obterPorId(id: Long) = metaDao?.getMetaById(id)
}

