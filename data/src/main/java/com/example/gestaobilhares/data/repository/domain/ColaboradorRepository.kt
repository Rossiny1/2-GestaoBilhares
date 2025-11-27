package com.example.gestaobilhares.data.repository.domain

import com.example.gestaobilhares.data.dao.ColaboradorDao
import com.example.gestaobilhares.data.entities.ColaboradorRota
import kotlinx.coroutines.flow.first

/**
 * Repository especializado para operações relacionadas a colaboradores.
 * Segue arquitetura híbrida modular: AppRepository como Facade.
 * 
 * Responsabilidades:
 * - Vinculações colaborador-rota
 */
class ColaboradorRepository(
    private val colaboradorDao: ColaboradorDao
) {
    
    /**
     * Obtém todos os ColaboradorRota
     * Para sincronização: busca todos os colaboradores e depois todas as rotas de cada um
     */
    suspend fun obterTodosColaboradorRotas(): List<ColaboradorRota> {
        val colaboradores = colaboradorDao.obterTodos().first()
        return colaboradores.flatMap { colaborador ->
            colaboradorDao.obterRotasPorColaborador(colaborador.id).first()
        }
    }
}

