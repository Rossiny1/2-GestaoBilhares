package com.example.gestaobilhares.ui.cycles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.ui.common.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.Date
import java.time.ZoneId
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Extensão para converter LocalDateTime para Date
 */
fun java.time.LocalDateTime.toDate(): Date {
    return Date.from(this.atZone(ZoneId.systemDefault()).toInstant())
}

/**
 * Dados de despesa para exibição
 */
data class CycleExpenseItem(
    val id: Long,
    val descricao: String,
    val valor: Double,
    val categoria: String,
    val data: Date,
    val observacoes: String? = null,
    val fotoComprovante: String? = null,
    val dataFotoComprovante: Long? = null
)

/**
 * ViewModel para gerenciar despesas do ciclo
 * ✅ CORRIGIDO: Usa observação reativa como CycleReceiptsViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CycleExpensesViewModel @Inject constructor(
    private val appRepository: AppRepository
) : BaseViewModel() {

    // ✅ NOVO: Flow para cicloId atual para observação reativa (igual ao CycleReceiptsViewModel)
    private val _cicloIdFlow = MutableStateFlow<Long?>(null)

    private val _despesas = MutableStateFlow<List<CycleExpenseItem>>(emptyList())
    val despesas: StateFlow<List<CycleExpenseItem>> = _despesas.asStateFlow()

    // isLoading já existe na BaseViewModel

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ✅ NOVO: Evento para notificar mudanças nas despesas
    private val _despesaModificada = MutableStateFlow<Boolean>(false)
    val despesaModificada: StateFlow<Boolean> = _despesaModificada.asStateFlow()

    init {
        // ✅ CORRIGIDO: Observar mudanças em despesas para atualização automática (igual ao CycleReceiptsViewModel)
        viewModelScope.launch {
            _cicloIdFlow
                .flatMapLatest { cicloId ->
                    if (cicloId == null) {
                        return@flatMapLatest flowOf(emptyList<CycleExpenseItem>())
                    }
                    
                    // Observar despesas do ciclo (Flow reativo)
                    appRepository.buscarDespesasPorCicloId(cicloId)
                        .map { despesasReais ->
                            processarDespesasParaExpenseItems(despesasReais)
                        }
                }
                .collect { despesasDTO ->
                    _despesas.value = despesasDTO
                    android.util.Log.d("CycleExpensesViewModel", "✅ Despesas atualizadas: ${despesasDTO.size} itens")
                }
        }
    }

    /**
     * Carrega despesas do ciclo
     * ✅ CORRIGIDO: Agora apenas atualiza o cicloId, o init observa o Flow automaticamente (igual ao CycleReceiptsViewModel)
     */
    fun carregarDespesas(cicloId: Long) {
        _cicloIdFlow.value = cicloId
        android.util.Log.d("CycleExpensesViewModel", "🔄 Carregando despesas para ciclo: $cicloId")
    }

    /**
     * Processa lista de despesas e converte para CycleExpenseItem
     * ✅ NOVO: Método similar ao processarAcertosParaReceipts do CycleReceiptsViewModel
     */
    private suspend fun processarDespesasParaExpenseItems(despesasReais: List<com.example.gestaobilhares.data.entities.Despesa>): List<CycleExpenseItem> {
        return try {
            android.util.Log.d("CycleExpensesViewModel", "📊 Processando ${despesasReais.size} despesas")
            
            despesasReais.map { despesa ->
                CycleExpenseItem(
                    id = despesa.id,
                    descricao = despesa.descricao,
                    valor = despesa.valor,
                    categoria = despesa.categoria,
                    data = Date(despesa.dataHora),
                    observacoes = despesa.observacoes,
                    fotoComprovante = despesa.fotoComprovante,
                    dataFotoComprovante = despesa.dataFotoComprovante
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("CycleExpensesViewModel", "Erro ao processar despesas: ${e.message}", e)
            _errorMessage.value = "Erro ao processar despesas: ${e.message}"
            emptyList()
        }
    }

    /**
     * Remove uma despesa
     * ✅ CORREÇÃO: Persistir no banco de dados
     * ✅ CORRIGIDO: Não precisa recarregar manualmente, o Flow observa automaticamente
     */
    fun removerDespesa(despesaId: Long) {
        viewModelScope.launch {
            try {
                showLoading()
                _errorMessage.value = null

                // ✅ CORREÇÃO: Buscar despesa real no banco e remover
                // ✅ MIGRADO: Usa AppRepository
                val despesaExistente = appRepository.obterDespesaPorId(despesaId)
                
                if (despesaExistente != null) {
                    // Remover despesa do banco
                    appRepository.deletarDespesa(despesaExistente)
                    
                    // ✅ CORRIGIDO: Não precisa recarregar manualmente, o Flow observa automaticamente
                    // O init block já está observando appRepository.buscarDespesasPorCicloId()
                    // e atualizará automaticamente quando a despesa for removida
                    
                    android.util.Log.d("CycleExpensesViewModel", "✅ Despesa $despesaId removida com sucesso do banco")
                } else {
                    _errorMessage.value = "Despesa não encontrada"
                }

            } catch (e: Exception) {
                android.util.Log.e("CycleExpensesViewModel", "Erro ao remover despesa: ${e.message}")
                _errorMessage.value = "Erro ao remover despesa: ${e.message}"
            } finally {
                hideLoading()
            }
        }
    }

    /**
     * Edita uma despesa existente
     * ✅ CORREÇÃO: Persistir no banco de dados
     * ✅ CORRIGIDO: Não precisa recarregar manualmente, o Flow observa automaticamente
     */
    fun editarDespesa(despesaId: Long, descricao: String, valor: Double, categoria: String, observacoes: String) {
        viewModelScope.launch {
            try {
                showLoading()
                _errorMessage.value = null

                // ✅ CORREÇÃO: Buscar despesa real no banco e atualizar
                // ✅ MIGRADO: Usa AppRepository
                val despesaExistente = appRepository.obterDespesaPorId(despesaId)
                
                if (despesaExistente != null) {
                    // ✅ VALIDAÇÃO CRÍTICA: Verificar se o ciclo da despesa está finalizado
                    val cicloIdDespesa = despesaExistente.cicloId
                    if (cicloIdDespesa != null) {
                        val cicloDaDespesa = appRepository.buscarCicloPorId(cicloIdDespesa)
                        if (cicloDaDespesa != null && cicloDaDespesa.status != com.example.gestaobilhares.data.entities.StatusCicloAcerto.EM_ANDAMENTO) {
                            android.util.Log.e("CycleExpensesViewModel", "❌ ERRO: Tentativa de editar despesa em ciclo finalizado! Ciclo ID: ${cicloDaDespesa.id}, Status: ${cicloDaDespesa.status}")
                            _errorMessage.value = "Não é possível editar despesas de ciclos finalizados. O ciclo desta despesa está ${cicloDaDespesa.status.name.lowercase()}."
                            hideLoading()
                            return@launch
                        }
                    }
                    // Atualizar despesa no banco
                    val despesaAtualizada = despesaExistente.copy(
                        descricao = descricao,
                        valor = valor,
                        categoria = categoria,
                        observacoes = observacoes
                    )
                    
                    appRepository.atualizarDespesa(despesaAtualizada)
                    
                    // ✅ CORRIGIDO: Não precisa recarregar manualmente, o Flow observa automaticamente
                    // O init block já está observando appRepository.buscarDespesasPorCicloId()
                    // e atualizará automaticamente quando a despesa for atualizada
                    
                    android.util.Log.d("CycleExpensesViewModel", "✅ Despesa $despesaId editada com sucesso no banco")
                } else {
                    _errorMessage.value = "Despesa não encontrada"
                }

            } catch (e: Exception) {
                android.util.Log.e("CycleExpensesViewModel", "Erro ao editar despesa: ${e.message}")
                _errorMessage.value = "Erro ao editar despesa: ${e.message}"
            } finally {
                hideLoading()
            }
        }
    }

    /**
     * ✅ NOVO: Busca um ciclo por ID para verificar seu status
     */
    suspend fun buscarCicloPorId(cicloId: Long): com.example.gestaobilhares.data.entities.CicloAcertoEntity? {
        return try {
            // ✅ MIGRADO: Usa AppRepository - tipo explícito no retorno para evitar ambiguidade
            val resultado: com.example.gestaobilhares.data.entities.CicloAcertoEntity? = appRepository.buscarCicloPorId(cicloId)
            resultado
        } catch (e: Exception) {
            android.util.Log.e("CycleExpensesViewModel", "Erro ao buscar ciclo por ID: ${e.message}")
            null
        }
    }

    /**
     * Limpa mensagem de erro
     */
    fun limparErro() {
        _errorMessage.value = null
    }

    /**
     * ✅ NOVO: Limpa notificação de mudança
     */
    fun limparNotificacaoMudanca() {
        _despesaModificada.value = false
    }
}


