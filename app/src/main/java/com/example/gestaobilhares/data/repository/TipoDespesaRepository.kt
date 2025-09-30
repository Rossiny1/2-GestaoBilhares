package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.TipoDespesaDao
import com.example.gestaobilhares.data.entities.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository para gerenciar tipos de despesas.
 */
@Singleton
class TipoDespesaRepository @Inject constructor(
    private val tipoDespesaDao: TipoDespesaDao
) {
    
    
    /**
     * Busca todos os tipos ativos.
     */
    fun buscarAtivos(): Flow<List<TipoDespesa>> {
        return tipoDespesaDao.buscarAtivos()
    }
    
    /**
     * Busca todos os tipos.
     */
    fun buscarTodos(): Flow<List<TipoDespesa>> {
        return tipoDespesaDao.buscarTodos()
    }
    
    /**
     * Busca tipos por categoria.
     */
    fun buscarPorCategoria(categoriaId: Long): Flow<List<TipoDespesa>> {
        return tipoDespesaDao.buscarPorCategoria(categoriaId)
    }
    
    /**
     * Busca tipos ativos com informações da categoria.
     */
    fun buscarAtivosComCategoria(): Flow<List<TipoDespesaComCategoria>> {
        return tipoDespesaDao.buscarAtivosComCategoria()
    }
    
    /**
     * Busca tipos por categoria com informações da categoria.
     */
    fun buscarPorCategoriaComCategoria(categoriaId: Long): Flow<List<TipoDespesaComCategoria>> {
        return tipoDespesaDao.buscarPorCategoriaComCategoria(categoriaId)
    }
    
    /**
     * Busca um tipo por ID.
     */
    suspend fun buscarPorId(id: Long): TipoDespesa? {
        return tipoDespesaDao.buscarPorId(id)
    }

    /**
     * ✅ NOVO: Busca um tipo por nome
     */
    suspend fun buscarPorNome(nome: String): TipoDespesa? {
        return tipoDespesaDao.buscarPorNome(nome)
    }
    
    /**
     * Insere um novo tipo.
     */
    suspend fun inserir(tipo: TipoDespesa): Long {
        return tipoDespesaDao.inserir(tipo)
    }
    
    /**
     * Atualiza um tipo existente.
     */
    suspend fun atualizar(tipo: TipoDespesa) {
        tipoDespesaDao.atualizar(tipo)
    }
    
    /**
     * Deleta um tipo.
     */
    suspend fun deletar(tipo: TipoDespesa) {
        tipoDespesaDao.deletar(tipo)
    }
    
    /**
     * Verifica se um tipo com o nome já existe na categoria.
     */
    suspend fun tipoExiste(nome: String, categoriaId: Long): Boolean {
        return tipoDespesaDao.contarPorNomeECategoria(nome, categoriaId) > 0
    }
    
    /**
     * Conta quantos tipos ativos existem por categoria.
     */
    suspend fun contarAtivosPorCategoria(categoriaId: Long): Int {
        return tipoDespesaDao.contarAtivosPorCategoria(categoriaId)
    }
    
    /**
     * Cria um novo tipo a partir dos dados fornecidos.
     */
    suspend fun criarTipo(dados: NovoTipoDespesa): Long {
        val tipo = TipoDespesa(
            categoriaId = dados.categoriaId,
            nome = dados.nome.trim(),
            descricao = dados.descricao.trim(),
            criadoPor = dados.criadoPor
        )
        return inserir(tipo)
    }
    
    /**
     * Atualiza um tipo existente.
     */
    suspend fun editarTipo(dados: EdicaoTipoDespesa) {
        val tipoExistente = buscarPorId(dados.id)
        tipoExistente?.let {
            val tipoAtualizado = it.copy(
                categoriaId = dados.categoriaId,
                nome = dados.nome.trim(),
                descricao = dados.descricao.trim(),
                ativo = dados.ativo,
                dataAtualizacao = java.util.Date()
            )
            atualizar(tipoAtualizado)
        }
    }
    
    // ❌ REMOVIDO: Dados mock excluídos para evitar criação automática de dados
} 