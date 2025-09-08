package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.CategoriaDespesaDao
import com.example.gestaobilhares.data.entities.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository para gerenciar categorias de despesas.
 */
@Singleton
class CategoriaDespesaRepository @Inject constructor(
    private val categoriaDespesaDao: CategoriaDespesaDao
) {
    
    // Flag para usar dados mock durante desenvolvimento
    private val usarDadosMock = false
    
    /**
     * Busca todas as categorias ativas.
     */
    fun buscarAtivas(): Flow<List<CategoriaDespesa>> {
        return if (usarDadosMock) {
            kotlinx.coroutines.flow.flowOf(obterCategoriasMock())
        } else {
            categoriaDespesaDao.buscarAtivas()
        }
    }
    
    /**
     * Busca todas as categorias.
     */
    fun buscarTodas(): Flow<List<CategoriaDespesa>> {
        return if (usarDadosMock) {
            kotlinx.coroutines.flow.flowOf(obterCategoriasMock())
        } else {
            categoriaDespesaDao.buscarTodas()
        }
    }
    
    /**
     * Busca uma categoria por ID.
     */
    suspend fun buscarPorId(id: Long): CategoriaDespesa? {
        return if (usarDadosMock) {
            obterCategoriasMock().find { it.id == id }
        } else {
            categoriaDespesaDao.buscarPorId(id)
        }
    }
    
    /**
     * Busca uma categoria por nome.
     */
    suspend fun buscarPorNome(nome: String): CategoriaDespesa? {
        return if (usarDadosMock) {
            obterCategoriasMock().find { it.nome.equals(nome, ignoreCase = true) }
        } else {
            categoriaDespesaDao.buscarPorNome(nome)
        }
    }
    
    /**
     * Insere uma nova categoria.
     */
    suspend fun inserir(categoria: CategoriaDespesa): Long {
        return if (usarDadosMock) {
            // Em modo mock, retorna um ID simulado
            System.currentTimeMillis()
        } else {
            categoriaDespesaDao.inserir(categoria)
        }
    }
    
    /**
     * Atualiza uma categoria existente.
     */
    suspend fun atualizar(categoria: CategoriaDespesa) {
        if (!usarDadosMock) {
            categoriaDespesaDao.atualizar(categoria)
        }
    }
    
    /**
     * Deleta uma categoria.
     */
    suspend fun deletar(categoria: CategoriaDespesa) {
        if (!usarDadosMock) {
            categoriaDespesaDao.deletar(categoria)
        }
    }
    
    /**
     * Verifica se uma categoria com o nome já existe.
     */
    suspend fun categoriaExiste(nome: String): Boolean {
        return if (usarDadosMock) {
            obterCategoriasMock().any { it.nome.equals(nome, ignoreCase = true) }
        } else {
            categoriaDespesaDao.contarPorNome(nome) > 0
        }
    }
    
    /**
     * Conta quantas categorias ativas existem.
     */
    suspend fun contarAtivas(): Int {
        return if (usarDadosMock) {
            obterCategoriasMock().size
        } else {
            categoriaDespesaDao.contarAtivas()
        }
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
                dataAtualizacao = java.util.Date()
            )
            atualizar(categoriaAtualizada)
        }
    }
    
    /**
     * Dados mock para desenvolvimento e testes.
     */
    private fun obterCategoriasMock(): List<CategoriaDespesa> {
        return listOf(
            CategoriaDespesa(
                id = 1,
                nome = "Funcionários",
                descricao = "Despesas relacionadas a funcionários",
                ativa = true,
                criadoPor = "Sistema"
            ),
            CategoriaDespesa(
                id = 2,
                nome = "Materiais Sinuca",
                descricao = "Materiais específicos para sinuca",
                ativa = true,
                criadoPor = "Sistema"
            ),
            CategoriaDespesa(
                id = 3,
                nome = "Impostos e Taxas",
                descricao = "Impostos e taxas governamentais",
                ativa = true,
                criadoPor = "Sistema"
            ),
            CategoriaDespesa(
                id = 4,
                nome = "Veículo",
                descricao = "Despesas com veículos",
                ativa = true,
                criadoPor = "Sistema"
            ),
            CategoriaDespesa(
                id = 5,
                nome = "Copiadora",
                descricao = "Despesas com copiadora",
                ativa = true,
                criadoPor = "Sistema"
            ),
            CategoriaDespesa(
                id = 6,
                nome = "Transportadora",
                descricao = "Despesas com transportadora",
                ativa = true,
                criadoPor = "Sistema"
            ),
            CategoriaDespesa(
                id = 7,
                nome = "Material Limpeza",
                descricao = "Materiais de limpeza",
                ativa = true,
                criadoPor = "Sistema"
            ),
            CategoriaDespesa(
                id = 8,
                nome = "Padaria",
                descricao = "Despesas com padaria",
                ativa = true,
                criadoPor = "Sistema"
            ),
            CategoriaDespesa(
                id = 9,
                nome = "Energia / Luz / Telefone",
                descricao = "Despesas com energia, luz e telefone",
                ativa = true,
                criadoPor = "Sistema"
            ),
            CategoriaDespesa(
                id = 10,
                nome = "Viagem",
                descricao = "Despesas com viagens",
                ativa = true,
                criadoPor = "Sistema"
            )
        )
    }
} 