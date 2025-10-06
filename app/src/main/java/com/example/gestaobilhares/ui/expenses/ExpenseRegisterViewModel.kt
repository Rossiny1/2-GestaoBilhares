package com.example.gestaobilhares.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.*
import com.example.gestaobilhares.data.repository.*
import com.example.gestaobilhares.ui.common.BaseViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel para o cadastro de despesas.
 * Gerencia o estado da tela de cadastro e integra com o ciclo de acertos.
 */
@HiltViewModel
class ExpenseRegisterViewModel @Inject constructor(
    private val despesaRepository: DespesaRepository,
    private val categoriaDespesaRepository: CategoriaDespesaRepository,
    private val tipoDespesaRepository: TipoDespesaRepository,
    private val cicloAcertoRepository: CicloAcertoRepository,
    private val historicoManutencaoRepository: HistoricoManutencaoVeiculoRepository,
    private val historicoCombustivelRepository: HistoricoCombustivelVeiculoRepository
) : BaseViewModel() {

    // Estado de carregamento - já existe na BaseViewModel

    // Mensagens de feedback - já existe na BaseViewModel

    // Estado de sucesso
    private val _success = MutableStateFlow(false)
    val success: StateFlow<Boolean> = _success.asStateFlow()

    // Data selecionada
    private val _selectedDate = MutableStateFlow(LocalDateTime.now())
    val selectedDate: StateFlow<LocalDateTime> = _selectedDate.asStateFlow()

    // Categoria selecionada
    private val _selectedCategory = MutableStateFlow<CategoriaDespesa?>(null)
    val selectedCategory: StateFlow<CategoriaDespesa?> = _selectedCategory.asStateFlow()

    // Tipo selecionado
    private val _selectedType = MutableStateFlow<TipoDespesa?>(null)
    val selectedType: StateFlow<TipoDespesa?> = _selectedType.asStateFlow()

    // Lista de categorias
    private val _categories = MutableStateFlow<List<CategoriaDespesa>>(emptyList())
    val categories: StateFlow<List<CategoriaDespesa>> = _categories.asStateFlow()

    // Lista de tipos filtrados por categoria
    private val _types = MutableStateFlow<List<TipoDespesa>>(emptyList())
    val types: StateFlow<List<TipoDespesa>> = _types.asStateFlow()

    // ✅ NOVO: seleção/listagem de ciclos para fluxo global sem rota
    private val _cycles = MutableStateFlow<List<CicloAcertoEntity>>(emptyList())
    val cycles: StateFlow<List<CicloAcertoEntity>> = _cycles.asStateFlow()
    private val _selectedCycle = MutableStateFlow<CicloAcertoEntity?>(null)
    val selectedCycle: StateFlow<CicloAcertoEntity?> = _selectedCycle.asStateFlow()

    // Formatador de data
    private val dateFormatter = DateTimeFormatter.ofPattern("dd 'de' MMM 'de' yyyy", Locale("pt", "BR"))

    init {
        loadCategories()
        // BLOQUEADO: Criação automática de categorias desabilitada
        // ensureViagemCategory()
    }

    /**
     * Carrega as categorias de despesas.
     */
    private fun loadCategories() {
        viewModelScope.launch {
            try {
                categoriaDespesaRepository.buscarAtivas().collect { categorias ->
                    _categories.value = categorias
                }
            } catch (e: Exception) {
                showMessage("Erro ao carregar categorias: ${e.message}")
            }
        }
    }

    /**
     * ✅ NOVO: Carregar todos os ciclos (todas as rotas) quando necessário
     */
    fun loadAllCycles() {
        viewModelScope.launch {
            try {
                cicloAcertoRepository.listarTodosCiclos().collect { lista ->
                    _cycles.value = lista
                }
            } catch (e: Exception) {
                showMessage("Erro ao carregar ciclos: ${e.message}")
            }
        }
    }

    fun setSelectedCycle(ciclo: CicloAcertoEntity?) {
        _selectedCycle.value = ciclo
    }

    /**
     * ✅ NOVO: Carrega despesa para edição
     */
    suspend fun carregarDespesaParaEdicao(despesaId: Long): com.example.gestaobilhares.data.entities.Despesa? {
        return try {
            despesaRepository.buscarPorId(despesaId)
        } catch (e: Exception) {
            android.util.Log.e("ExpenseRegisterViewModel", "Erro ao carregar despesa para edição: ${e.message}")
            null
        }
    }

    /**
     * ✅ NOVO: Define categoria selecionada por nome
     */
    fun setSelectedCategory(categoriaNome: String) {
        viewModelScope.launch {
            val categoria = categoriaDespesaRepository.buscarPorNome(categoriaNome)
            _selectedCategory.value = categoria
        }
    }

    /**
     * ✅ NOVO: Define tipo selecionado por nome
     */
    fun setSelectedType(tipoNome: String) {
        viewModelScope.launch {
            val tipo = tipoDespesaRepository.buscarPorNome(tipoNome)
            _selectedType.value = tipo
        }
    }

    /**
     * ✅ NOVO: Define data selecionada
     */
    fun setSelectedDate(data: java.time.LocalDateTime) {
        _selectedDate.value = data
    }

    /**
     * Define a categoria selecionada e carrega os tipos correspondentes.
     */
    fun setSelectedCategory(categoria: CategoriaDespesa?) {
        _selectedCategory.value = categoria
        _selectedType.value = null // Reset tipo quando categoria muda
        
        if (categoria != null) {
            loadTypesForCategory(categoria.id)
        } else {
            _types.value = emptyList()
        }
    }

    /**
     * Define o tipo selecionado.
     */
    fun setSelectedType(tipo: TipoDespesa?) {
        _selectedType.value = tipo
    }

    /**
     * ❌ BLOQUEADO: Criação automática de categorias desabilitada
     * A categoria "Viagem" e tipos "Combustível"/"Manutenção" devem ser criados manualmente
     */
    /*
    private fun ensureViagemCategory() {
        viewModelScope.launch {
            try {
                val existe = categoriaDespesaRepository.categoriaExiste("Viagem")
                if (!existe) {
                    val catId = categoriaDespesaRepository.criarCategoria(
                        NovaCategoriaDespesa(nome = "Viagem", descricao = "Despesas de viagem", criadoPor = "Sistema")
                    )
                    // Tipos fixos
                    if (!tipoDespesaRepository.tipoExiste("Combustível", catId)) {
                        tipoDespesaRepository.criarTipo(NovoTipoDespesa(categoriaId = catId, nome = "Combustível", descricao = "Gasto com combustível"))
                    }
                    if (!tipoDespesaRepository.tipoExiste("Manutenção", catId)) {
                        tipoDespesaRepository.criarTipo(NovoTipoDespesa(categoriaId = catId, nome = "Manutenção", descricao = "Manutenção de veículo"))
                    }
                    // Recarregar categorias/tipos
                    loadCategories()
                }
            } catch (e: Exception) {
                showMessage("Erro ao configurar categoria Viagem: ${e.message}")
            }
        }
    }
    */

    /**
     * Carrega os tipos de despesa para uma categoria específica.
     */
    private fun loadTypesForCategory(categoriaId: Long) {
        viewModelScope.launch {
            try {
                tipoDespesaRepository.buscarPorCategoria(categoriaId).collect { tipos ->
                    _types.value = tipos
                }
            } catch (e: Exception) {
                showMessage("Erro ao carregar tipos: ${e.message}")
            }
        }
    }

    /**
     * Cria uma nova categoria.
     */
    fun createCategory(nome: String, descricao: String = "") {
        viewModelScope.launch {
            try {
                showLoading()
                
                // Verificar se categoria já existe
                if (categoriaDespesaRepository.categoriaExiste(nome)) {
                    showMessage("Categoria '$nome' já existe")
                    return@launch
                }

                val novaCategoria = NovaCategoriaDespesa(
                    nome = nome,
                    descricao = descricao,
                    criadoPor = "Sistema" // TODO: Pegar usuário atual
                )

                val categoriaId = categoriaDespesaRepository.criarCategoria(novaCategoria)
                showMessage("Categoria criada com sucesso")
                
                // Recarregar categorias
                loadCategories()
                
            } catch (e: Exception) {
                showMessage("Erro ao criar categoria: ${e.message}")
            } finally {
                hideLoading()
            }
        }
    }

    /**
     * Cria um novo tipo de despesa.
     */
    fun createType(categoriaId: Long, nome: String, descricao: String = "") {
        viewModelScope.launch {
            try {
                showLoading()
                
                // Verificar se tipo já existe na categoria
                if (tipoDespesaRepository.tipoExiste(nome, categoriaId)) {
                    showMessage("Tipo '$nome' já existe nesta categoria")
                    return@launch
                }

                val novoTipo = NovoTipoDespesa(
                    categoriaId = categoriaId,
                    nome = nome,
                    descricao = descricao,
                    criadoPor = "Sistema" // TODO: Pegar usuário atual
                )

                val tipoId = tipoDespesaRepository.criarTipo(novoTipo)
                showMessage("Tipo criado com sucesso")
                
                // Recarregar tipos da categoria
                loadTypesForCategory(categoriaId)
                
            } catch (e: Exception) {
                showMessage("Erro ao criar tipo: ${e.message}")
            } finally {
                hideLoading()
            }
        }
    }

    /**
     * Salva a despesa.
     * ✅ CORREÇÃO: Suporta edição de despesas existentes
     */
    fun saveExpense(
        rotaId: Long,
        descricao: String,
        valor: Double,
        quantidade: Int = 1,
        observacoes: String = "",
        despesaId: Long = 0L,
        modoEdicao: Boolean = false,
        fotoComprovante: String? = null,
        dataFotoComprovante: Date? = null,
        veiculoId: Long? = null,
        kmRodado: Long? = null,
        litrosAbastecidos: Double? = null
    ) {
        viewModelScope.launch {
            try {
                showLoading()
                val cicloAtivo = if (rotaId == 0L) {
                    // Fluxo global: seleção do ciclo apenas para ler ano/número; não usamos cicloId na gravação
                    val selecionado = _selectedCycle.value
                    if (selecionado == null) {
                        showMessage("Selecione um ciclo para lançar a despesa.")
                        return@launch
                    }
                    selecionado
                } else {
                    // Fluxo normal por rota
                    val ativo = cicloAcertoRepository.buscarCicloAtivo(rotaId)
                    if (ativo == null || ativo.status != StatusCicloAcerto.EM_ANDAMENTO) {
                        showMessage("Não é possível cadastrar despesas. O ciclo de acerto não está em andamento.")
                        return@launch
                    }
                    ativo
                }

                // Validações
                if (descricao.isBlank()) {
                    showMessage("Descrição é obrigatória")
                    return@launch
                }

                if (valor <= 0) {
                    showMessage("Valor deve ser maior que zero")
                    return@launch
                }

                val categoria = _selectedCategory.value
                if (categoria == null) {
                    showMessage("Selecione uma categoria")
                    return@launch
                }

                // ✅ CORRIGIDO: Usar ciclo ativo real
                val cicloId = if (rotaId == 0L) null else cicloAtivo.id
                val rotaParaLancamento = if (rotaId == 0L) cicloAtivo.rotaId else rotaId
                val cicloAno = cicloAtivo.ano
                val cicloNumero = cicloAtivo.numeroCiclo

                if (modoEdicao && despesaId > 0) {
                    // ✅ NOVO: Modo de edição - atualizar despesa existente
                    val despesaExistente = despesaRepository.buscarPorId(despesaId)
                    if (despesaExistente != null) {
                        val despesaAtualizada = despesaExistente.copy(
                            descricao = descricao,
                            valor = valor * quantidade,
                            categoria = categoria.nome,
                            tipoDespesa = _selectedType.value?.nome ?: "",
                            dataHora = _selectedDate.value,
                            observacoes = observacoes,
                            cicloId = cicloId,
                            origemLancamento = if (rotaId == 0L) "GLOBAL" else "ROTA",
                            cicloAno = if (rotaId == 0L) cicloAno else despesaExistente.cicloAno,
                            cicloNumero = if (rotaId == 0L) cicloNumero else despesaExistente.cicloNumero,
                            rotaId = rotaParaLancamento,
                            fotoComprovante = fotoComprovante,
                            dataFotoComprovante = dataFotoComprovante,
                            veiculoId = veiculoId,
                            kmRodado = kmRodado,
                            litrosAbastecidos = litrosAbastecidos
                        )
                        
                        despesaRepository.atualizar(despesaAtualizada)
                        showMessage("Despesa atualizada com sucesso")
                        _success.value = true
                    } else {
                        showMessage("Despesa não encontrada")
                    }
                } else {
                    // ✅ NOVO: Modo de criação - criar nova despesa
                    val despesa = Despesa(
                        rotaId = rotaParaLancamento,
                        descricao = descricao,
                        valor = valor * quantidade,
                        categoria = categoria.nome,
                        tipoDespesa = _selectedType.value?.nome ?: "",
                        dataHora = _selectedDate.value,
                        observacoes = observacoes,
                        criadoPor = "Sistema", // TODO: Pegar usuário atual
                        cicloId = cicloId,
                        origemLancamento = if (rotaId == 0L) "GLOBAL" else "ROTA",
                        cicloAno = if (rotaId == 0L) cicloAno else null,
                        cicloNumero = if (rotaId == 0L) cicloNumero else null,
                        fotoComprovante = fotoComprovante,
                        dataFotoComprovante = dataFotoComprovante,
                        veiculoId = veiculoId,
                        kmRodado = kmRodado,
                        litrosAbastecidos = litrosAbastecidos
                    )

                    val novaDespesaId = despesaRepository.inserir(despesa)
                    android.util.Log.d("ExpenseRegisterViewModel", "Despesa salva com ID: $novaDespesaId")
                    
                    // ✅ NOVO: Salvar no histórico de veículos se for combustível ou manutenção
                    if (veiculoId != null) {
                        try {
                            salvarNoHistoricoVeiculo(despesa, veiculoId, kmRodado, litrosAbastecidos)
                            android.util.Log.d("ExpenseRegisterViewModel", "Histórico de veículo salvo com sucesso")
                        } catch (e: Exception) {
                            android.util.Log.e("ExpenseRegisterViewModel", "Erro ao salvar histórico de veículo: ${e.message}", e)
                            showMessage("Despesa salva, mas erro ao salvar no histórico do veículo: ${e.message}")
                        }
                    }
                    
                    showMessage("Despesa salva com sucesso")
                    _success.value = true
                }
                
            } catch (e: Exception) {
                showMessage("Erro ao salvar despesa: ${e.message}")
            } finally {
                hideLoading()
            }
        }
    }

    /**
     * Formata a data selecionada para exibição.
     */
    fun getFormattedDate(): String {
        return _selectedDate.value.format(dateFormatter)
    }

    /**
     * Limpa as mensagens.
     */
    // clearMessage já existe na BaseViewModel

    /**
     * Reseta o estado de sucesso.
     */
    fun resetSuccess() {
        _success.value = false
    }
    
    /**
     * ✅ NOVO: Salva no histórico de veículos baseado no tipo de despesa
     */
    private suspend fun salvarNoHistoricoVeiculo(
        despesa: Despesa,
        veiculoId: Long,
        kmRodado: Long?,
        litrosAbastecidos: Double?
    ) {
        try {
            val tipoDespesa = despesa.tipoDespesa.lowercase()
            val categoriaDespesa = despesa.categoria.lowercase()
            
            // ✅ CORREÇÃO: Conversão segura de LocalDateTime para Date
            val dataDespesa = try {
                Date.from(despesa.dataHora.atZone(java.time.ZoneId.systemDefault()).toInstant())
            } catch (e: Exception) {
                // Fallback para data atual se houver erro na conversão
                android.util.Log.w("ExpenseRegisterViewModel", "Erro na conversão de data: ${e.message}")
                Date()
            }
            
            when {
                isCombustivel(tipoDespesa, categoriaDespesa) -> {
                    // Salvar no histórico de combustível
                    if (litrosAbastecidos != null && litrosAbastecidos > 0) {
                        android.util.Log.d("ExpenseRegisterViewModel", "Salvando abastecimento: veiculoId=$veiculoId, litros=$litrosAbastecidos, valor=${despesa.valor}")
                        
                        val historicoCombustivel = HistoricoCombustivelVeiculo(
                            veiculoId = veiculoId,
                            dataAbastecimento = dataDespesa,
                            litros = litrosAbastecidos,
                            valor = despesa.valor,
                            kmVeiculo = kmRodado ?: 0L,
                            kmRodado = kmRodado?.toDouble() ?: 0.0,
                            posto = "Posto", // TODO: Permitir seleção de posto
                            observacoes = despesa.observacoes
                        )
                        
                        val idInserido = historicoCombustivelRepository.inserir(historicoCombustivel)
                        android.util.Log.d("ExpenseRegisterViewModel", "Abastecimento salvo com ID: $idInserido")
                    } else {
                        android.util.Log.w("ExpenseRegisterViewModel", "Litros não informados ou inválidos: $litrosAbastecidos")
                    }
                }
                isManutencao(tipoDespesa, categoriaDespesa) -> {
                    // Salvar no histórico de manutenção
                    val historicoManutencao = HistoricoManutencaoVeiculo(
                        veiculoId = veiculoId,
                        tipoManutencao = despesa.tipoDespesa,
                        descricao = despesa.descricao,
                        dataManutencao = dataDespesa,
                        valor = despesa.valor,
                        kmVeiculo = kmRodado ?: 0L,
                        responsavel = "Sistema", // TODO: Pegar usuário atual
                        observacoes = despesa.observacoes
                    )
                    historicoManutencaoRepository.inserir(historicoManutencao)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ExpenseRegisterViewModel", "Erro ao salvar no histórico de veículos: ${e.message}", e)
            // Não falhar o salvamento da despesa por causa do histórico
            // Mas vamos tentar salvar pelo menos a despesa principal
            throw e // Re-throw para que o erro seja capturado no saveExpense
        }
    }
    
    /**
     * ✅ NOVO: Verifica se é despesa de combustível
     */
    private fun isCombustivel(tipoDespesa: String, categoriaDespesa: String): Boolean {
        val combustivelKeywords = listOf(
            "combustível", "combustivel", "gasolina", "diesel", "etanol", "gnv", "gás"
        )
        return combustivelKeywords.any { keyword ->
            tipoDespesa.contains(keyword) || categoriaDespesa.contains(keyword)
        }
    }
    
    /**
     * ✅ NOVO: Verifica se é despesa de manutenção
     */
    private fun isManutencao(tipoDespesa: String, categoriaDespesa: String): Boolean {
        val manutencaoKeywords = listOf(
            "manutenção", "manutencao", "revisão", "revisao", "troca", "pneu", "óleo", "oleo",
            "filtro", "bateria", "freio", "suspensão", "suspensao", "motor", "transmissão"
        )
        return manutencaoKeywords.any { keyword ->
            tipoDespesa.contains(keyword) || categoriaDespesa.contains(keyword)
        }
    }
} 