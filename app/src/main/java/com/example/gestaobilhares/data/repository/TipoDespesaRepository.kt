package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.TipoDespesaDao
import com.example.gestaobilhares.data.entities.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository para gerenciar tipos de despesas.
 */
class TipoDespesaRepository(
    private val tipoDespesaDao: TipoDespesaDao
) {
    
    // Flag para usar dados mock durante desenvolvimento
    private val usarDadosMock = true
    
    /**
     * Busca todos os tipos ativos.
     */
    fun buscarAtivos(): Flow<List<TipoDespesa>> {
        return if (usarDadosMock) {
            kotlinx.coroutines.flow.flowOf(obterTiposMock())
        } else {
            tipoDespesaDao.buscarAtivos()
        }
    }
    
    /**
     * Busca todos os tipos.
     */
    fun buscarTodos(): Flow<List<TipoDespesa>> {
        return if (usarDadosMock) {
            kotlinx.coroutines.flow.flowOf(obterTiposMock())
        } else {
            tipoDespesaDao.buscarTodos()
        }
    }
    
    /**
     * Busca tipos por categoria.
     */
    fun buscarPorCategoria(categoriaId: Long): Flow<List<TipoDespesa>> {
        return if (usarDadosMock) {
            kotlinx.coroutines.flow.flowOf(obterTiposMock().filter { it.categoriaId == categoriaId })
        } else {
            tipoDespesaDao.buscarPorCategoria(categoriaId)
        }
    }
    
    /**
     * Busca tipos ativos com informações da categoria.
     */
    fun buscarAtivosComCategoria(): Flow<List<TipoDespesaComCategoria>> {
        return if (usarDadosMock) {
            kotlinx.coroutines.flow.flowOf(obterTiposComCategoriaMock())
        } else {
            tipoDespesaDao.buscarAtivosComCategoria()
        }
    }
    
    /**
     * Busca tipos por categoria com informações da categoria.
     */
    fun buscarPorCategoriaComCategoria(categoriaId: Long): Flow<List<TipoDespesaComCategoria>> {
        return if (usarDadosMock) {
            kotlinx.coroutines.flow.flowOf(obterTiposComCategoriaMock().filter { it.categoriaId == categoriaId })
        } else {
            tipoDespesaDao.buscarPorCategoriaComCategoria(categoriaId)
        }
    }
    
    /**
     * Busca um tipo por ID.
     */
    suspend fun buscarPorId(id: Long): TipoDespesa? {
        return if (usarDadosMock) {
            obterTiposMock().find { it.id == id }
        } else {
            tipoDespesaDao.buscarPorId(id)
        }
    }
    
    /**
     * Insere um novo tipo.
     */
    suspend fun inserir(tipo: TipoDespesa): Long {
        return if (usarDadosMock) {
            // Em modo mock, retorna um ID simulado
            System.currentTimeMillis()
        } else {
            tipoDespesaDao.inserir(tipo)
        }
    }
    
    /**
     * Atualiza um tipo existente.
     */
    suspend fun atualizar(tipo: TipoDespesa) {
        if (!usarDadosMock) {
            tipoDespesaDao.atualizar(tipo)
        }
    }
    
    /**
     * Deleta um tipo.
     */
    suspend fun deletar(tipo: TipoDespesa) {
        if (!usarDadosMock) {
            tipoDespesaDao.deletar(tipo)
        }
    }
    
    /**
     * Verifica se um tipo com o nome já existe na categoria.
     */
    suspend fun tipoExiste(nome: String, categoriaId: Long): Boolean {
        return if (usarDadosMock) {
            obterTiposMock().any { 
                it.nome.equals(nome, ignoreCase = true) && it.categoriaId == categoriaId 
            }
        } else {
            tipoDespesaDao.contarPorNomeECategoria(nome, categoriaId) > 0
        }
    }
    
    /**
     * Conta quantos tipos ativos existem por categoria.
     */
    suspend fun contarAtivosPorCategoria(categoriaId: Long): Int {
        return if (usarDadosMock) {
            obterTiposMock().count { it.categoriaId == categoriaId && it.ativo }
        } else {
            tipoDespesaDao.contarAtivosPorCategoria(categoriaId)
        }
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
    
    /**
     * Dados mock para desenvolvimento e testes.
     */
    private fun obterTiposMock(): List<TipoDespesa> {
        return listOf(
            // Funcionários (ID: 1)
            TipoDespesa(id = 1, categoriaId = 1, nome = "Salário", descricao = "Pagamento de salários", ativo = true, criadoPor = "Sistema"),
            TipoDespesa(id = 2, categoriaId = 1, nome = "Vale Refeição", descricao = "Vale refeição para funcionários", ativo = true, criadoPor = "Sistema"),
            TipoDespesa(id = 3, categoriaId = 1, nome = "Vale Transporte", descricao = "Vale transporte para funcionários", ativo = true, criadoPor = "Sistema"),
            
            // Materiais Sinuca (ID: 2)
            TipoDespesa(id = 4, categoriaId = 2, nome = "Tacos", descricao = "Tacos de sinuca", ativo = true, criadoPor = "Sistema"),
            TipoDespesa(id = 5, categoriaId = 2, nome = "Bolas", descricao = "Bolas de sinuca", ativo = true, criadoPor = "Sistema"),
            TipoDespesa(id = 6, categoriaId = 2, nome = "Pano de Mesa", descricao = "Pano para mesas de sinuca", ativo = true, criadoPor = "Sistema"),
            
            // Impostos e Taxas (ID: 3)
            TipoDespesa(id = 7, categoriaId = 3, nome = "IPTU", descricao = "Imposto Predial e Territorial Urbano", ativo = true, criadoPor = "Sistema"),
            TipoDespesa(id = 8, categoriaId = 3, nome = "ISS", descricao = "Imposto Sobre Serviços", ativo = true, criadoPor = "Sistema"),
            TipoDespesa(id = 9, categoriaId = 3, nome = "Alvará", descricao = "Alvará de funcionamento", ativo = true, criadoPor = "Sistema"),
            
            // Veículo (ID: 4)
            TipoDespesa(id = 10, categoriaId = 4, nome = "Combustível", descricao = "Combustível para veículos", ativo = true, criadoPor = "Sistema"),
            TipoDespesa(id = 11, categoriaId = 4, nome = "Manutenção", descricao = "Manutenção de veículos", ativo = true, criadoPor = "Sistema"),
            TipoDespesa(id = 12, categoriaId = 4, nome = "Seguro", descricao = "Seguro de veículos", ativo = true, criadoPor = "Sistema"),
            
            // Copiadora (ID: 5)
            TipoDespesa(id = 13, categoriaId = 5, nome = "Aluguel", descricao = "Aluguel de copiadora", ativo = true, criadoPor = "Sistema"),
            TipoDespesa(id = 14, categoriaId = 5, nome = "Manutenção", descricao = "Manutenção de copiadora", ativo = true, criadoPor = "Sistema"),
            TipoDespesa(id = 15, categoriaId = 5, nome = "Toner", descricao = "Toner para copiadora", ativo = true, criadoPor = "Sistema"),
            
            // Transportadora (ID: 6)
            TipoDespesa(id = 16, categoriaId = 6, nome = "Frete", descricao = "Custos de frete", ativo = true, criadoPor = "Sistema"),
            TipoDespesa(id = 17, categoriaId = 6, nome = "Entrega", descricao = "Custos de entrega", ativo = true, criadoPor = "Sistema"),
            
            // Material Limpeza (ID: 7)
            TipoDespesa(id = 18, categoriaId = 7, nome = "Detergente", descricao = "Detergente para limpeza", ativo = true, criadoPor = "Sistema"),
            TipoDespesa(id = 19, categoriaId = 7, nome = "Desinfetante", descricao = "Desinfetante", ativo = true, criadoPor = "Sistema"),
            TipoDespesa(id = 20, categoriaId = 7, nome = "Papel Higiênico", descricao = "Papel higiênico", ativo = true, criadoPor = "Sistema"),
            
            // Padaria (ID: 8)
            TipoDespesa(id = 21, categoriaId = 8, nome = "Pães", descricao = "Pães para funcionários", ativo = true, criadoPor = "Sistema"),
            TipoDespesa(id = 22, categoriaId = 8, nome = "Café", descricao = "Café para funcionários", ativo = true, criadoPor = "Sistema"),
            
            // Energia / Luz / Telefone (ID: 9)
            TipoDespesa(id = 23, categoriaId = 9, nome = "Energia Elétrica", descricao = "Conta de energia elétrica", ativo = true, criadoPor = "Sistema"),
            TipoDespesa(id = 24, categoriaId = 9, nome = "Telefone", descricao = "Conta de telefone", ativo = true, criadoPor = "Sistema"),
            TipoDespesa(id = 25, categoriaId = 9, nome = "Internet", descricao = "Conta de internet", ativo = true, criadoPor = "Sistema"),
            
            // Viagem (ID: 10)
            TipoDespesa(id = 26, categoriaId = 10, nome = "Hospedagem", descricao = "Custos de hospedagem", ativo = true, criadoPor = "Sistema"),
            TipoDespesa(id = 27, categoriaId = 10, nome = "Alimentação", descricao = "Custos de alimentação em viagem", ativo = true, criadoPor = "Sistema"),
            TipoDespesa(id = 28, categoriaId = 10, nome = "Transporte", descricao = "Custos de transporte em viagem", ativo = true, criadoPor = "Sistema")
        )
    }
    
    /**
     * Dados mock para tipos com categoria.
     */
    private fun obterTiposComCategoriaMock(): List<TipoDespesaComCategoria> {
        val categorias = mapOf(
            1L to "Funcionários",
            2L to "Materiais Sinuca",
            3L to "Impostos e Taxas",
            4L to "Veículo",
            5L to "Copiadora",
            6L to "Transportadora",
            7L to "Material Limpeza",
            8L to "Padaria",
            9L to "Energia / Luz / Telefone",
            10L to "Viagem"
        )
        
        return obterTiposMock().map { tipo ->
            TipoDespesaComCategoria(
                id = tipo.id,
                categoriaId = tipo.categoriaId,
                nome = tipo.nome,
                descricao = tipo.descricao,
                ativo = tipo.ativo,
                categoriaNome = categorias[tipo.categoriaId] ?: "Categoria Desconhecida"
            )
        }
    }
} 