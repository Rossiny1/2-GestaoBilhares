package com.example.gestaobilhares.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.*
import com.example.gestaobilhares.data.repository.*
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
    private val cicloAcertoRepository: CicloAcertoRepository
) : ViewModel() {

    // Estado de carregamento
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Mensagens de feedback
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

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
                _message.value = "Erro ao carregar categorias: ${e.message}"
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
                _message.value = "Erro ao carregar ciclos: ${e.message}"
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
     * Carrega os tipos de despesa para uma categoria específica.
     */
    private fun loadTypesForCategory(categoriaId: Long) {
        viewModelScope.launch {
            try {
                tipoDespesaRepository.buscarPorCategoria(categoriaId).collect { tipos ->
                    _types.value = tipos
                }
            } catch (e: Exception) {
                _message.value = "Erro ao carregar tipos: ${e.message}"
            }
        }
    }

    /**
     * Cria uma nova categoria.
     */
    fun createCategory(nome: String, descricao: String = "") {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Verificar se categoria já existe
                if (categoriaDespesaRepository.categoriaExiste(nome)) {
                    _message.value = "Categoria '$nome' já existe"
                    return@launch
                }

                val novaCategoria = NovaCategoriaDespesa(
                    nome = nome,
                    descricao = descricao,
                    criadoPor = "Sistema" // TODO: Pegar usuário atual
                )

                val categoriaId = categoriaDespesaRepository.criarCategoria(novaCategoria)
                _message.value = "Categoria criada com sucesso"
                
                // Recarregar categorias
                loadCategories()
                
            } catch (e: Exception) {
                _message.value = "Erro ao criar categoria: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Cria um novo tipo de despesa.
     */
    fun createType(categoriaId: Long, nome: String, descricao: String = "") {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Verificar se tipo já existe na categoria
                if (tipoDespesaRepository.tipoExiste(nome, categoriaId)) {
                    _message.value = "Tipo '$nome' já existe nesta categoria"
                    return@launch
                }

                val novoTipo = NovoTipoDespesa(
                    categoriaId = categoriaId,
                    nome = nome,
                    descricao = descricao,
                    criadoPor = "Sistema" // TODO: Pegar usuário atual
                )

                val tipoId = tipoDespesaRepository.criarTipo(novoTipo)
                _message.value = "Tipo criado com sucesso"
                
                // Recarregar tipos da categoria
                loadTypesForCategory(categoriaId)
                
            } catch (e: Exception) {
                _message.value = "Erro ao criar tipo: ${e.message}"
            } finally {
                _isLoading.value = false
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
        dataFotoComprovante: Date? = null
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val cicloAtivo = if (rotaId == 0L) {
                    // Fluxo global: seleção do ciclo apenas para ler ano/número; não usamos cicloId na gravação
                    val selecionado = _selectedCycle.value
                    if (selecionado == null) {
                        _message.value = "Selecione um ciclo para lançar a despesa."
                        return@launch
                    }
                    selecionado
                } else {
                    // Fluxo normal por rota
                    val ativo = cicloAcertoRepository.buscarCicloAtivo(rotaId)
                    if (ativo == null || ativo.status != StatusCicloAcerto.EM_ANDAMENTO) {
                        _message.value = "Não é possível cadastrar despesas. O ciclo de acerto não está em andamento."
                        return@launch
                    }
                    ativo
                }

                // Validações
                if (descricao.isBlank()) {
                    _message.value = "Descrição é obrigatória"
                    return@launch
                }

                if (valor <= 0) {
                    _message.value = "Valor deve ser maior que zero"
                    return@launch
                }

                val categoria = _selectedCategory.value
                if (categoria == null) {
                    _message.value = "Selecione uma categoria"
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
                            dataFotoComprovante = dataFotoComprovante
                        )
                        
                        despesaRepository.atualizar(despesaAtualizada)
                        _message.value = "Despesa atualizada com sucesso"
                        _success.value = true
                    } else {
                        _message.value = "Despesa não encontrada"
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
                        dataFotoComprovante = dataFotoComprovante
                    )

                    val novaDespesaId = despesaRepository.inserir(despesa)
                    _message.value = "Despesa salva com sucesso"
                    _success.value = true
                }
                
            } catch (e: Exception) {
                _message.value = "Erro ao salvar despesa: ${e.message}"
            } finally {
                _isLoading.value = false
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
    fun clearMessage() {
        _message.value = null
    }

    /**
     * Reseta o estado de sucesso.
     */
    fun resetSuccess() {
        _success.value = false
    }
} 