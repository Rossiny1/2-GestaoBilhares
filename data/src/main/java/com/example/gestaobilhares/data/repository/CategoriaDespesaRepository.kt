package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.CategoriaDespesaDao
import com.example.gestaobilhares.data.entities.*
import kotlinx.coroutines.flow.Flow
/**
 * Repository para gerenciar categorias de despesas.
 * 
 * @deprecated Use AppRepository ao invés deste repository individual.
 * Este repository será removido em versão futura. Migre para AppRepository
 * obtido via RepositoryFactory.getAppRepository(context).
 */
@Deprecated(
    message = "Use AppRepository ao invés de CategoriaDespesaRepository. " +
            "Obtenha via RepositoryFactory.getAppRepository(context).",
    replaceWith = ReplaceWith(
        "RepositoryFactory.getAppRepository(context)",
        "com.example.gestaobilhares.data.factory.RepositoryFactory"
    ),
    level = DeprecationLevel.WARNING
)
class CategoriaDespesaRepository constructor(
    private val categoriaDespesaDao: CategoriaDespesaDao
) {
    
    
    /**
     * Busca todas as categorias ativas.
     */
    fun buscarAtivas(): Flow<List<CategoriaDespesa>> {
        return categoriaDespesaDao.buscarAtivas()
    }
    
    /**
     * Busca todas as categorias.
     */
    fun buscarTodas(): Flow<List<CategoriaDespesa>> {
        return categoriaDespesaDao.buscarTodas()
    }
    
    /**
     * Busca uma categoria por ID.
     */
    suspend fun buscarPorId(id: Long): CategoriaDespesa? {
        return categoriaDespesaDao.buscarPorId(id)
    }
    
    /**
     * Busca uma categoria por nome.
     */
    suspend fun buscarPorNome(nome: String): CategoriaDespesa? {
        return categoriaDespesaDao.buscarPorNome(nome)
    }
    
    /**
     * Insere uma nova categoria.
     */
    suspend fun inserir(categoria: CategoriaDespesa): Long {
        return categoriaDespesaDao.inserir(categoria)
    }
    
    /**
     * Atualiza uma categoria existente.
     */
    suspend fun atualizar(categoria: CategoriaDespesa) {
        categoriaDespesaDao.atualizar(categoria)
    }
    
    /**
     * Deleta uma categoria.
     */
    suspend fun deletar(categoria: CategoriaDespesa) {
        categoriaDespesaDao.deletar(categoria)
    }
    
    /**
     * Verifica se uma categoria com o nome já existe.
     */
    suspend fun categoriaExiste(nome: String): Boolean {
        return categoriaDespesaDao.contarPorNome(nome) > 0
    }
    
    /**
     * Conta quantas categorias ativas existem.
     */
    suspend fun contarAtivas(): Int {
        return categoriaDespesaDao.contarAtivas()
    }
    
    /**
     * Cria uma nova categoria a partir dos dados fornecidos.
     */
    suspend fun criarCategoria(dados: NovaCategoriaDespesa): Long {
        val categoria = CategoriaDespesa(
            nome = dados.nome.trim(),
            descricao = dados.descricao.trim(),
            criadoPor = dados.criadoPor
        )
        return inserir(categoria)
    }
    
    /**
     * Atualiza uma categoria existente.
     */
    suspend fun editarCategoria(dados: EdicaoCategoriaDespesa) {
        val categoriaExistente = buscarPorId(dados.id)
        categoriaExistente?.let {
            val categoriaAtualizada = it.copy(
                nome = dados.nome.trim(),
                descricao = dados.descricao.trim(),
                ativa = dados.ativa,
                dataAtualizacao = System.currentTimeMillis()
            )
            atualizar(categoriaAtualizada)
        }
    }
    
    // ❌ REMOVIDO: Dados mock excluídos para evitar criação automática de dados
} 
