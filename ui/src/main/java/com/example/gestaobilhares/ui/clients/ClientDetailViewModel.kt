package com.example.gestaobilhares.ui.clients

import android.os.Parcelable
import timber.log.Timber
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.core.utils.UserSessionManager
import com.example.gestaobilhares.data.entities.Acerto
import com.example.gestaobilhares.data.entities.CicloAcertoEntity
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.data.entities.ContratoLocacao
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.ui.common.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.example.gestaobilhares.sync.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel responsável pelos dados da tela de detalhes do cliente.
 * Mantém o histórico de acertos, mesas vinculadas e informações auxiliares
 * necessárias para o fluxo offline-first.
 */
@HiltViewModel
class ClientDetailViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val userSessionManager: UserSessionManager,
    private val syncRepository: SyncRepository
) : BaseViewModel() {

    companion object {
        private const val TAG = "ClientDetailViewModel"
        private const val DEFAULT_OBSERVACAO = "Nenhuma observação registrada."
        private const val DEFAULT_ENDERECO = "Endereço não informado"
        private const val DEFAULT_TELEFONE = "Telefone não informado"
    }

    private val debugLogsEnabled = true
    private val defaultHistoryLimit = 3

    private val _clientDetails = MutableStateFlow<ClienteResumo?>(null)
    val clientDetails: StateFlow<ClienteResumo?> = _clientDetails.asStateFlow()

    private val _settlementHistory = MutableStateFlow<List<AcertoResumo>>(emptyList())
    val settlementHistory: StateFlow<List<AcertoResumo>> = _settlementHistory.asStateFlow()

    private val _historyFilter = MutableStateFlow<HistoryFilterState>(HistoryFilterState.Recent(defaultHistoryLimit))
    val historyFilter: StateFlow<HistoryFilterState> = _historyFilter.asStateFlow()

    private val _historyLoading = MutableStateFlow(false)
    val historyLoading: StateFlow<Boolean> = _historyLoading.asStateFlow()

    private val _historyError = MutableStateFlow<String?>(null)
    val historyError: StateFlow<String?> = _historyError.asStateFlow()

    private val _mesasCliente = MutableStateFlow<List<Mesa>>(emptyList())
    val mesasCliente: StateFlow<List<Mesa>> = _mesasCliente.asStateFlow()

    private val _mesasDisponiveis = MutableStateFlow<List<Mesa>>(emptyList())
    val mesasDisponiveis: StateFlow<List<Mesa>> = _mesasDisponiveis.asStateFlow()

    private val _temContratoAtivo = MutableStateFlow(false)
    val temContratoAtivo: StateFlow<Boolean> = _temContratoAtivo.asStateFlow()

    private val _cliente = MutableStateFlow<Cliente?>(null)
    val cliente: StateFlow<Cliente?> = _cliente.asStateFlow()

    private val _pendenciasCliente = MutableStateFlow<List<String>>(emptyList())
    val pendenciasCliente: StateFlow<List<String>> = _pendenciasCliente.asStateFlow()

    private var settlementHistoryJob: Job? = null

    init {
        logDebug("ViewModel inicializado")
    }

    fun loadClientDetails(clienteId: Long) {
        viewModelScope.launch {
            showLoading()
            try {
                logDebug("Carregando cliente $clienteId")

                val cliente = appRepository.obterClientePorId(clienteId) ?: run {
                    showError("Cliente não encontrado.")
                    return@launch
                }

                _cliente.value = cliente

                val ultimoAcerto = appRepository.buscarUltimoAcertoPorCliente(clienteId)
                val ultimaVisita = ultimoAcerto?.let { calcularTempoRelativoReal(it.dataAcerto) } ?: "Nunca visitado"

                val observacaoUltimoAcerto = appRepository.buscarObservacaoUltimoAcerto(clienteId)
                val observacaoExibir = observacaoUltimoAcerto ?: DEFAULT_OBSERVACAO

                val enderecoExibir = cliente.endereco?.takeIf { it.isNotBlank() }?.trim() ?: DEFAULT_ENDERECO
                val telefoneExibir = cliente.telefone?.takeIf { it.isNotBlank() }?.trim() ?: DEFAULT_TELEFONE

                val diasSemAcerto = ultimoAcerto?.let {
                    val hoje = LocalDate.now()
                    val dataUltimoAcerto = it.dataAcerto.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    ChronoUnit.DAYS.between(dataUltimoAcerto, hoje).toInt()
                } ?: 0

                val debitoAtualReal = ultimoAcerto?.debitoAtual ?: 0.0
                
                // ✅ NOVO: Calcular mensagem de pendência (nunca acertado ou meses sem acerto)
                val mensagemPendencia = calcularMensagemPendencia(ultimoAcerto)

                _clientDetails.value = ClienteResumo(
                    id = cliente.id,
                    nome = cliente.nome,
                    endereco = enderecoExibir,
                    telefone = telefoneExibir,
                    valorFicha = cliente.valorFicha,
                    comissaoFicha = cliente.comissaoFicha,
                    mesasAtivas = _mesasCliente.value.size,
                    ultimaVisita = ultimaVisita,
                    observacoes = observacaoExibir,
                    debitoAtual = debitoAtualReal,
                    latitude = cliente.latitude,
                    longitude = cliente.longitude,
                    diasSemAcerto = diasSemAcerto,
                    mensagemPendencia = mensagemPendencia
                )

                loadSettlementHistory(clienteId)
                observeMesasDoCliente(clienteId)
                
                // ✅ Verificar pendências do cliente após carregar os dados
                verificarPendenciasCliente(cliente)
            } catch (e: Exception) {
                Timber.e(TAG, "Erro ao carregar detalhes do cliente", e)
                showError("Erro ao carregar detalhes do cliente: ${e.message}", e)
            } finally {
                hideLoading()
            }
        }
    }
    
    /**
     * ✅ NOVO: Verifica pendências do cliente e armazena em StateFlow
     * Verifica: dados faltantes (CPF, Telefone, Contrato), débito alto, sem acerto há mais de 4 meses
     * O diálogo só aparece se houver pelo menos uma pendência
     */
    private fun verificarPendenciasCliente(cliente: Cliente) {
        viewModelScope.launch {
            try {
                val pendencias = mutableListOf<String>()
                
                // 1. Verificar dados faltantes
                val dadosFaltantes = mutableListOf<String>()
                if (cliente.cpfCnpj.isNullOrBlank()) {
                    dadosFaltantes.add("CPF")
                }
                if (cliente.telefone.isNullOrBlank()) {
                    dadosFaltantes.add("Telefone")
                }
                
                // Verificar se tem contrato ativo
                val contratos = appRepository.buscarContratosPorCliente(cliente.id).first()
                val temContratoAtivo = contratos.any { contrato: ContratoLocacao ->
                    contrato.status.equals("ATIVO", ignoreCase = true)
                }
                
                if (!temContratoAtivo) {
                    dadosFaltantes.add("Contrato")
                }
                
                if (dadosFaltantes.isNotEmpty()) {
                    pendencias.add("Dados faltantes: ${dadosFaltantes.joinToString(", ")}")
                }
                
                // 2. Verificar débito alto (> R$ 300)
                val ultimoAcerto = appRepository.buscarUltimoAcertoPorCliente(cliente.id)
                val debitoAtual = ultimoAcerto?.debitoAtual ?: cliente.debitoAtual
                
                if (debitoAtual > 300.0) {
                    val debitoFormatado = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("pt", "BR")).format(debitoAtual)
                    pendencias.add("Débito alto: $debitoFormatado")
                }
                
                // 3. Verificar se não acerta há mais de 4 meses
                val hoje = Calendar.getInstance().time
                val quatroMesesAtras = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -4)
                }.time
                
                val semAcertoRecente = when {
                    ultimoAcerto == null -> true // Nunca foi acertado
                    else -> ultimoAcerto.dataAcerto.before(quatroMesesAtras)
                }
                
                if (semAcertoRecente) {
                    val mesesSemAcerto = if (ultimoAcerto == null) {
                        "Nunca foi acertado"
                    } else {
                        val diffMeses = ((hoje.time - ultimoAcerto.dataAcerto.time) / (1000L * 60 * 60 * 24 * 30)).toInt()
                        "Não acerta há $diffMeses mês(es)"
                    }
                    pendencias.add("Sem acerto recente: $mesesSemAcerto")
                }
                
                // ✅ Só armazenar se houver pendências
                _pendenciasCliente.value = pendencias
                
                if (pendencias.isNotEmpty()) {
                    logDebug("Pendências detectadas: ${pendencias.size} pendência(s) encontrada(s)")
                } else {
                    logDebug("Nenhuma pendência detectada")
                }
            } catch (e: Exception) {
                Timber.e(TAG, "Erro ao verificar pendências do cliente", e)
                _pendenciasCliente.value = emptyList()
            }
        }
    }

    private fun observeMesasDoCliente(clienteId: Long) {
        viewModelScope.launch {
            try {
                appRepository.obterMesasPorCliente(clienteId).collect { mesas ->
                    _mesasCliente.value = mesas
                    _clientDetails.value = _clientDetails.value?.copy(mesasAtivas = mesas.size)
                    logDebug("Mesas atualizadas: ${mesas.size}")
                }
            } catch (e: Exception) {
                Timber.e(TAG, "Erro ao observar mesas do cliente", e)
            }
        }
    }

    fun adicionarMesaAoCliente(mesaId: Long, clienteId: Long) {
        viewModelScope.launch {
            try {
                appRepository.vincularMesaACliente(mesaId, clienteId)
                loadClientDetails(clienteId)
            } catch (e: Exception) {
                Timber.e(TAG, "Erro ao adicionar mesa ao cliente", e)
            }
        }
    }

    suspend fun verificarSeRetiradaEPermitida(mesaId: Long, _clienteId: Long): RetiradaStatus {
        return try {
            val ultimoAcertoMesa = appRepository.buscarUltimoAcertoPorMesa(mesaId)
            
            if (ultimoAcertoMesa == null) {
                return RetiradaStatus.PRECISA_ACERTO
            }
            
            // Verificar se o acerto foi feito hoje (mesma data)
            val hoje = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            val amanha = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_MONTH, 1)
            }
            
            val dataAcerto = Calendar.getInstance().apply {
                time = ultimoAcertoMesa.dataAcerto
            }
            
            // Verificar se o acerto foi feito hoje (entre hoje 00:00:00 e amanhã 00:00:00)
            val acertoFoiHoje = dataAcerto.timeInMillis >= hoje.timeInMillis && 
                               dataAcerto.timeInMillis < amanha.timeInMillis

            if (acertoFoiHoje) {
                RetiradaStatus.PODE_RETIRAR
            } else {
                RetiradaStatus.PRECISA_ACERTO
            }
        } catch (e: Exception) {
            Timber.e(TAG, "Erro ao verificar status de retirada", e)
            RetiradaStatus.PRECISA_ACERTO
        }
    }

    fun retirarMesaDoCliente(mesaId: Long, clienteId: Long, relogioFinal: Int, valorRecebido: Double) {
        viewModelScope.launch {
            try {
                appRepository.atualizarRelogioFinal(mesaId, relogioFinal)
                appRepository.retirarMesa(mesaId)
                logDebug("Mesa $mesaId retirada. Relógio final: $relogioFinal, valor recebido: $valorRecebido")
                loadClientDetails(clienteId)
            } catch (e: Exception) {
                Timber.e(TAG, "Erro ao retirar mesa", e)
            }
        }
    }

    fun loadMesasDisponiveis() {
        viewModelScope.launch {
            try {
                appRepository.obterMesasDisponiveis().collect { mesas ->
                    _mesasDisponiveis.value = mesas
                }
            } catch (e: Exception) {
                Timber.e(TAG, "Erro ao carregar mesas disponíveis", e)
            }
        }
    }

    fun isAdminUser(): Boolean = userSessionManager?.isAdmin() ?: false

    fun salvarObservacaoCliente(clienteId: Long, observacao: String) {
        viewModelScope.launch {
            try {
                logDebug("Solicitação de salvar observação para cliente $clienteId: $observacao")
                loadClientDetails(clienteId)
            } catch (e: Exception) {
                Timber.e(TAG, "Erro ao salvar observação do cliente", e)
            }
        }
    }

    fun adicionarAcertoNoHistorico(novoAcerto: AcertoResumo) {
        val listaAtual = _settlementHistory.value.toMutableList()
        listaAtual.add(0, novoAcerto)
        _settlementHistory.value = listaAtual
        _clientDetails.value?.id?.let { loadSettlementHistory(it) }
    }

    fun loadSettlementHistory(clienteId: Long) {
        settlementHistoryJob?.cancel()
        when (val filter = _historyFilter.value) {
            is HistoryFilterState.Recent -> observeRecentHistory(clienteId)
            is HistoryFilterState.CustomLimit -> carregarHistoricoPorQuantidade(clienteId, filter.limit, updateFilter = false)
        }
    }

    fun mostrarHistoricoRecentes(clienteId: Long) {
        if (_historyFilter.value is HistoryFilterState.Recent) return
        _historyFilter.value = HistoryFilterState.Recent(defaultHistoryLimit)
        loadSettlementHistory(clienteId)
    }

    fun carregarHistoricoPorQuantidade(clienteId: Long, quantidadeSolicitada: Int, updateFilter: Boolean = true) {
        val quantidade = quantidadeSolicitada.coerceIn(defaultHistoryLimit, 100)
        if (quantidade <= defaultHistoryLimit && updateFilter) {
            mostrarHistoricoRecentes(clienteId)
            return
        }

        settlementHistoryJob?.cancel()
        settlementHistoryJob = viewModelScope.launch {
            _historyLoading.value = true
            try {
                // 1. Se quantidade > 3, sempre tentar buscar do Firestore primeiro
                var acertos: List<Acerto>? = null
                var buscaRemotaFalhou = false
                
                try {
                    acertos = syncRepository?.fetchUltimosAcertos(clienteId, quantidade)
                    if (acertos.isNullOrEmpty()) {
                        buscaRemotaFalhou = true
                        Timber.d(TAG, "Busca remota retornou vazio, tentando fallback local...")
                    } else {
                        Timber.d(TAG, "Histórico remoto carregado: ${acertos.size} itens")
                    }
                } catch (e: Exception) {
                    buscaRemotaFalhou = true
                    Timber.w(TAG, "Busca remota falhou (possível falta de índice Firestore), tentando fallback local: ${e.message}")
                }
                
                // 2. Se remoto falhou, tentar buscar localmente como fallback temporário
                // (pode ter mais que 3 se ainda não foi feita a limpeza)
                if (buscaRemotaFalhou) {
                    Timber.d(TAG, "Busca remota falhou, tentando buscar localmente como fallback...")
                    val acertosLocais = appRepository.obterAcertosRecentesPorCliente(clienteId, quantidade).first()
                    
                    if (acertosLocais.isEmpty()) {
                        _historyError.value = "Não encontramos acertos para este cliente. Verifique sua conexão e sincronize novamente."
                        return@launch
                    } else if (acertosLocais.size < quantidade) {
                        // Encontrou menos que o solicitado localmente
                        // Isso pode acontecer se a política de retenção já removeu os mais antigos
                        acertos = acertosLocais
                        Timber.d(TAG, "Histórico local carregado: ${acertosLocais.size} itens (menos que os ${quantidade} solicitados)")
                        _historyError.value = "Encontrados apenas ${acertosLocais.size} acertos armazenados localmente. A política de retenção mantém apenas os últimos 3 acertos. Para ver mais, sincronize novamente."
                    } else {
                        // Encontrou quantidade suficiente localmente (ainda não foi limpo)
                        acertos = acertosLocais
                        Timber.d(TAG, "Histórico local carregado: ${acertosLocais.size} itens (fallback temporário)")
                    }
                }
                
                if (acertos == null || acertos.isEmpty()) {
                    _historyError.value = "Não encontramos acertos para este cliente."
                    return@launch
                }
                
                if (updateFilter) {
                    _historyFilter.value = HistoryFilterState.CustomLimit(acertos.size)
                }
                _settlementHistory.value = mapAcertosParaResumo(acertos)
            } catch (e: Exception) {
                Timber.e(TAG, "Erro ao carregar histórico adicional", e)
                _historyError.value = "Erro ao buscar acertos adicionais: ${e.message}"
            } finally {
                _historyLoading.value = false
            }
        }
    }

    private fun observeRecentHistory(clienteId: Long) {
        settlementHistoryJob = viewModelScope.launch {
            try {
                appRepository.obterAcertosRecentesPorCliente(clienteId, defaultHistoryLimit).collect { acertos ->
                    _settlementHistory.value = mapAcertosParaResumo(acertos)
                    logDebug("Histórico recente atualizado: ${_settlementHistory.value.size} itens")
                }
            } catch (e: Exception) {
                Timber.e(TAG, "Erro ao carregar histórico recente de acertos", e)
            }
        }
    }

    fun obterLimiteHistoricoAtual(): Int {
        return when (val state = _historyFilter.value) {
            is HistoryFilterState.Recent -> state.limit
            is HistoryFilterState.CustomLimit -> state.limit
        }
    }

    fun buscarDataUltimoAcerto(clienteId: Long, callback: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val ultimoAcerto = appRepository.buscarUltimoAcertoPorCliente(clienteId)
                val formatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
                callback(ultimoAcerto?.let { formatter.format(it.dataAcerto) } ?: "Nunca")
            } catch (e: Exception) {
                Timber.e(TAG, "Erro ao buscar último acerto", e)
                callback("Nunca")
            }
        }
    }

    private fun calcularTempoRelativoReal(data: Date): String {
        val agora = Calendar.getInstance().time
        val diffMillis = agora.time - data.time
        val diffDias = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
        return when {
            diffDias < 1 -> "Hoje"
            diffDias == 1 -> "Há 1 dia"
            diffDias < 7 -> "Há $diffDias dias"
            diffDias < 30 -> "Há ${diffDias / 7} semana(s)"
            diffDias < 365 -> "Há ${diffDias / 30} mês(es)"
            else -> "Há ${diffDias / 365} ano(s)"
        }
    }

    suspend fun buscarRelogioFinalUltimoAcerto(@Suppress("UNUSED_PARAMETER") mesaId: Long): Int? = try {
        null
    } catch (e: Exception) {
        Timber.e(TAG, "Erro ao buscar relógio final do último acerto", e)
        null
    }

    suspend fun buscarCicloIdPorAcerto(@Suppress("UNUSED_PARAMETER") acertoId: Long): Long? = try {
        null
    } catch (e: Exception) {
        Timber.e(TAG, "Erro ao buscar ciclo do acerto", e)
        null
    }

    suspend fun buscarRotaIdPorCliente(clienteId: Long): Long? = try {
        appRepository.buscarRotaIdPorCliente(clienteId)
    } catch (e: Exception) {
        Timber.e(TAG, "Erro ao buscar rota do cliente", e)
        null
    }

    suspend fun buscarUltimoAcerto(clienteId: Long): Acerto? = try {
        appRepository.buscarUltimoAcertoPorCliente(clienteId)
    } catch (e: Exception) {
        Timber.e(TAG, "Erro ao buscar último acerto do cliente", e)
        null
    }

    suspend fun buscarCicloAtualPorRota(rotaId: Long): CicloAcertoEntity? = try {
        appRepository.buscarCicloAtualPorRota(rotaId)
    } catch (e: Exception) {
        Timber.e(TAG, "Erro ao buscar ciclo atual por rota", e)
        null
    }

    fun carregarClienteCompleto(clienteId: Long) {
        viewModelScope.launch {
            try {
                _cliente.value = appRepository.obterClientePorId(clienteId)
            } catch (e: Exception) {
                Timber.e(TAG, "Erro ao carregar cliente completo", e)
                _cliente.value = null
            }
        }
    }

    fun verificarContratoAtivo(clienteId: Long) {
        viewModelScope.launch {
            try {
                val contratos = appRepository.buscarContratosPorCliente(clienteId).first()
                val temAtivo = contratos.any { contrato: ContratoLocacao ->
                    contrato.status.equals("ATIVO", ignoreCase = true)
                }
                _temContratoAtivo.value = temAtivo
            } catch (e: Exception) {
                Timber.e(TAG, "Erro ao verificar contratos do cliente", e)
                _temContratoAtivo.value = false
            }
        }
    }

    private fun logDebug(message: String) {
        if (debugLogsEnabled) {
            Timber.d(TAG, message)
        }
    }

    fun consumirHistoryError() {
        _historyError.value = null
    }

    private fun mapAcertosParaResumo(acertos: List<Acerto>): List<AcertoResumo> {
        return acertos.map { acerto ->
            val observacaoExibir = acerto.observacoes?.takeIf { it.isNotBlank() }?.trim()
                ?: "Sem observações"
            AcertoResumo(
                id = acerto.id,
                data = android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", acerto.dataAcerto).toString(),
                valorTotal = acerto.valorRecebido,
                status = acerto.status.name,
                mesasAcertadas = acerto.totalMesas.toInt(),
                debitoAtual = acerto.debitoAtual,
                observacao = observacaoExibir
            )
        }
    }
    
    /**
     * ✅ NOVO: Calcula a mensagem de pendência baseada no histórico de acertos
     * Retorna:
     * - "Nunca acertado" se o cliente nunca foi acertado
     * - "Não acerta há X meses" se não acerta há 3 ou mais meses
     * - null se não há pendência
     */
    private fun calcularMensagemPendencia(ultimoAcerto: Acerto?): String? {
        return try {
            when {
                // Se nunca foi acertado
                ultimoAcerto == null -> "Nunca acertado"
                
                // Se tem acerto, calcular meses sem acerto de forma mais precisa
                else -> {
                    val hoje = Calendar.getInstance()
                    val dataUltimoAcerto = Calendar.getInstance().apply {
                        time = ultimoAcerto.dataAcerto
                    }
                    
                    // Calcular diferença em meses de forma mais precisa
                    var diffMeses = (hoje.get(Calendar.YEAR) - dataUltimoAcerto.get(Calendar.YEAR)) * 12
                    diffMeses += hoje.get(Calendar.MONTH) - dataUltimoAcerto.get(Calendar.MONTH)
                    
                    // Ajustar se o dia do mês ainda não passou
                    if (hoje.get(Calendar.DAY_OF_MONTH) < dataUltimoAcerto.get(Calendar.DAY_OF_MONTH)) {
                        diffMeses--
                    }
                    
                    // Se não acerta há 3 ou mais meses, mostrar mensagem
                    if (diffMeses >= 3) {
                        "Não acerta há $diffMeses meses"
                    } else {
                        null // Sem pendência se menos de 3 meses
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(TAG, "Erro ao calcular mensagem de pendência: ${e.message}", e)
            null
        }
    }
}

data class ClienteResumo(
    val id: Long,
    val nome: String,
    val endereco: String,
    val telefone: String,
    val valorFicha: Double,
    val comissaoFicha: Double,
    val mesasAtivas: Int,
    val ultimaVisita: String,
    val observacoes: String,
    val debitoAtual: Double = 0.0,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val diasSemAcerto: Int = 0,
    val mensagemPendencia: String? = null // ✅ NOVO: Mensagem de pendência (Nunca acertado ou Não acerta há X meses)
)

@Parcelize
data class AcertoResumo(
    val id: Long,
    val data: String,
    val valorTotal: Double,
    val status: String,
    val mesasAcertadas: Int,
    val debitoAtual: Double = 0.0,
    val observacao: String? = null
) : Parcelable

enum class RetiradaStatus {
    PODE_RETIRAR,
    PRECISA_ACERTO
}

sealed class HistoryFilterState {
    data class Recent(val limit: Int) : HistoryFilterState()
    data class CustomLimit(val limit: Int) : HistoryFilterState()
}

