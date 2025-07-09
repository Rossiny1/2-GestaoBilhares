package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.CicloAcertoDao
import com.example.gestaobilhares.data.entities.CicloAcertoEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Repository para operações relacionadas a ciclos de acerto
 * Abstrai o acesso ao CicloAcertoDao para uso nos ViewModels
 */
class CicloAcertoRepository(
    private val cicloAcertoDao: CicloAcertoDao
) {
    fun listarPorRota(rotaId: Long): Flow<List<CicloAcertoEntity>> =
        cicloAcertoDao.listarPorRota(rotaId)

    suspend fun buscarUltimoCicloPorRota(rotaId: Long): CicloAcertoEntity? =
        cicloAcertoDao.buscarUltimoCicloPorRota(rotaId)

    suspend fun buscarCicloEmAndamento(rotaId: Long): CicloAcertoEntity? =
        cicloAcertoDao.buscarCicloEmAndamento(rotaId)

    suspend fun buscarProximoNumeroCiclo(rotaId: Long, ano: Int): Int =
        cicloAcertoDao.buscarProximoNumeroCiclo(rotaId, ano)

    suspend fun inserir(ciclo: CicloAcertoEntity): Long =
        cicloAcertoDao.inserir(ciclo)

    suspend fun atualizar(ciclo: CicloAcertoEntity) =
        cicloAcertoDao.atualizar(ciclo)

    suspend fun deletar(ciclo: CicloAcertoEntity) =
        cicloAcertoDao.deletar(ciclo)
} 