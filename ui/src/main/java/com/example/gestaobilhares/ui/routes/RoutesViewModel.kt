package com.example.gestaobilhares.ui.routes

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.RotaResumo
import com.example.gestaobilhares.data.entities.MetaColaborador
import com.example.gestaobilhares.data.entities.TipoMeta
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.core.utils.UserSessionManager
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
/**
 * ViewModel para a tela de rotas.
 * Gerencia o estado da UI e coordena com o Repository para obter dados.
 * Segue o padrão MVVM para separar a lógica de negócio da UI.
 * 
 * FASE 3: Inclui controle de acesso administrativo e cálculo de valores acertados.
 * ✅ NOVO: Controle de acesso baseado em nível de usuário e rotas responsáveis.
 */
class RoutesViewModel constructor(
    private val appRepository: AppRepository,
    private val userSessionManager: UserSessionManager
) : ViewModel() {

    // ✅ MODERNIZADO: StateFlow para controlar o estado de loading
    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ✅ MODERNIZADO: StateFlow para mensagens de erro
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ✅ MODERNIZADO: StateFlow para mensagens de sucesso
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // ✅ MODERNIZADO: StateFlow para controlar a navegação
    private val _navigateToClients = MutableStateFlow<Long?>(null)
    val navigateToClients: StateFlow<Long?> = _navigateToClients.asStateFlow()

    // ✅ NOVO: StateFlow para diálogo de sincronização pendente
    private val _syncDialogState = MutableStateFlow<SyncDialogState?>(null)
    val syncDialogState: StateFlow<SyncDialogState?> = _syncDialogState.asStateFlow()
    private var syncDialogDismissed = false


    // ✅ MODERNIZADO: Rotas filtradas baseado no acesso do usuário
    private val _rotasResumoFiltradas = MutableStateFlow<List<RotaResumo>>(emptyList())
    val rotasResumo: StateFlow<List<RotaResumo>> = _rotasResumoFiltradas.asStateFlow()
    
    // ✅ MODERNIZADO: Observa as rotas resumo do repository e aplica filtro de acesso
    // ✅ CORREÇÃO: Carregar dados imediatamente ao inicializar para evitar delay
    private val rotasResumoOriginal: StateFlow<List<RotaResumo>> = run {
        val initialValue = kotlinx.coroutines.runBlocking {
            try {
                appRepository.getRotasResumoComAtualizacaoTempoReal().first()
            } catch (e: Exception) {
                android.util.Log.w("RoutesViewModel", "Erro ao carregar rotas inicialmente: ${e.message}")
                emptyList()
            }
        }
        appRepository.getRotasResumoComAtualizacaoTempoReal().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = initialValue
        )
    }

    // ✅ CORREÇÃO: Estatísticas gerais calculadas a partir das rotas FILTRADAS (baseado no acesso do usuário)
    val estatisticas: StateFlow<EstatisticasGerais> = _rotasResumoFiltradas.map { rotas ->
        calcularEstatisticas(rotas)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000), // ✅ CORREÇÃO: Usar WhileSubscribed para melhor performance
        initialValue = EstatisticasGerais(0, 0, 0, 0.0, 0.0)
    )

    init {
        // ✅ MODERNIZADO: Observar mudanças nas rotas originais e aplicar filtro de acesso
        viewModelScope.launch {
            rotasResumoOriginal.collect { rotas ->
                aplicarFiltroAcesso(rotas)
            }
        }
        
        // Banco de dados limpo - sem inserção automática de dados
    }

    /**
     * ✅ NOVO: Verifica operações pendentes de sincronização
     * Também verifica se há dados na nuvem quando o banco local está vazio
     */
    fun checkSyncPendencies(context: android.content.Context) {
        if (syncDialogDismissed) return
        
        viewModelScope.launch {
            try {
                android.util.Log.d("RoutesViewModel", "🔍 Verificando pendências de sincronização...")
                
                val syncRepository = com.example.gestaobilhares.sync.SyncRepository(context, appRepository)
                val lastGlobalSync = runCatching {
                    syncRepository.getGlobalLastSyncTimestamp()
                }.getOrDefault(0L).takeIf { it > 0L }
                android.util.Log.d("RoutesViewModel", "📅 Última sincronização: $lastGlobalSync")
                
                val pending = appRepository.contarOperacoesSyncPendentes()
                android.util.Log.d("RoutesViewModel", "📡 Pendências de sincronização: $pending")
                android.util.Log.d("RoutesViewModel", "🔍 syncDialogDismissed: $syncDialogDismissed")
                
                // Se não há pendências locais, verificar se há dados na nuvem quando banco local está vazio
                if (pending == 0) {
                    val rotasLocais = appRepository.obterTodasRotas().first()
                    android.util.Log.d("RoutesViewModel", "🗂️ Rotas locais: ${rotasLocais.size}")
                    
                    if (rotasLocais.isEmpty()) {
                        android.util.Log.d("RoutesViewModel", "🔍 Banco local vazio - verificando dados na nuvem...")
                        // Criar SyncRepository para verificar se há dados na nuvem
                        try {
                            val hasDataInCloud = syncRepository.hasDataInCloud()
                            android.util.Log.d("RoutesViewModel", "📡 Dados na nuvem encontrados: $hasDataInCloud")
                            
                            if (hasDataInCloud && !syncDialogDismissed) {
                                // Mostrar diálogo perguntando se quer sincronizar
                                android.util.Log.d("RoutesViewModel", "✅ Mostrando diálogo de sincronização - dados encontrados na nuvem")
                                _syncDialogState.value = SyncDialogState(1, isCloudData = true, lastSyncTimestamp = lastGlobalSync)
                                return@launch
                            } else {
                                android.util.Log.d("RoutesViewModel", "⚠️ Não mostrando diálogo: hasDataInCloud=$hasDataInCloud, syncDialogDismissed=$syncDialogDismissed")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("RoutesViewModel", "❌ Erro ao verificar dados na nuvem: ${e.message}", e)
                            android.util.Log.e("RoutesViewModel", "Stack trace: ${e.stackTraceToString()}")
                            // ✅ CORREÇÃO: Mesmo com erro, mostrar diálogo se banco está vazio
                            // O usuário pode querer tentar sincronizar mesmo assim
                            if (!syncDialogDismissed) {
                                android.util.Log.d("RoutesViewModel", "⚠️ Erro ao verificar nuvem, mas mostrando diálogo mesmo assim (banco vazio)")
                                _syncDialogState.value = SyncDialogState(1, isCloudData = true, lastSyncTimestamp = lastGlobalSync)
                                return@launch
                            }
                        }
                        return@launch
                    } else {
                        android.util.Log.d("RoutesViewModel", "ℹ️ Banco local não está vazio (${rotasLocais.size} rotas) - não verificar nuvem")
                    }
                }
                
                if (pending > 0 && !syncDialogDismissed) {
                    android.util.Log.d("RoutesViewModel", "✅ Mostrando diálogo de sincronização - $pending pendências")
                    _syncDialogState.value = SyncDialogState(pending, lastSyncTimestamp = lastGlobalSync)
                } else {
                    // Quando zerar pendências, resetar supressão
                    if (pending == 0) {
                        syncDialogDismissed = false
                    }
                    android.util.Log.d("RoutesViewModel", "ℹ️ Não mostrando diálogo: pending=$pending, syncDialogDismissed=$syncDialogDismissed")
                    _syncDialogState.value = null
                }
            } catch (e: Exception) {
                android.util.Log.e("RoutesViewModel", "Erro ao verificar pendências de sync: ${e.message}", e)
            }
        }
    }

    /**
     * ✅ NOVO: Marca diálogo de sincronização como manipulado
     */
    fun dismissSyncDialog(permanently: Boolean = true) {
        if (permanently) {
            syncDialogDismissed = true
        }
        _syncDialogState.value = null
    }

    /**
     * ✅ NOVO: Aplica filtro de acesso às rotas baseado no nível do usuário
     */
    private fun aplicarFiltroAcesso(rotas: List<RotaResumo>) {
        val isAdmin = userSessionManager.isAdmin()
        val userName = userSessionManager.getCurrentUserName()
        val userEmail = userSessionManager.getCurrentUserEmail()
        val userId = userSessionManager.getCurrentUserId()
        
        android.util.Log.d("RoutesViewModel", "🔍 Aplicando filtro de rotas:")
        android.util.Log.d("RoutesViewModel", "   Usuário: $userName")
        android.util.Log.d("RoutesViewModel", "   Email: $userEmail")
        android.util.Log.d("RoutesViewModel", "   ID: $userId")
        android.util.Log.d("RoutesViewModel", "   É Admin: $isAdmin")
        android.util.Log.d("RoutesViewModel", "   Total de rotas: ${rotas.size}")
        
        if (isAdmin) {
            // Admin vê todas as rotas
            _rotasResumoFiltradas.value = rotas
            android.util.Log.d("RoutesViewModel", "✅ ADMIN - Mostrando todas as ${rotas.size} rotas")
        } else {
            // ✅ IMPLEMENTADO: USER vê apenas rotas onde é responsável
            viewModelScope.launch {
                try {
                    // Buscar rotas onde o usuário é responsável
                    val rotasResponsavel = appRepository.obterRotasPorColaborador(userId).first()
                    
                    android.util.Log.d("RoutesViewModel", "🔍 Buscando rotas responsável para usuário $userId")
                    
                    // Filtrar apenas as rotas onde o usuário é responsável
                    val rotasFiltradas = rotas.filter { rotaResumo ->
                        rotasResponsavel.any { colaboradorRota ->
                            colaboradorRota.rotaId == rotaResumo.rota.id
                        }
                    }
                    
                    android.util.Log.d("RoutesViewModel", "✅ USER - Mostrando ${rotasFiltradas.size} rotas responsável:")
                    rotasFiltradas.forEach { rotaResumo ->
                        android.util.Log.d("RoutesViewModel", "   - ${rotaResumo.rota.nome}")
                    }
                    
                    _rotasResumoFiltradas.value = rotasFiltradas
                    
                } catch (e: Exception) {
                    android.util.Log.e("RoutesViewModel", "Erro ao filtrar rotas por responsabilidade: ${e.message}", e)
                    // Em caso de erro, mostrar todas as rotas (fallback)
                    _rotasResumoFiltradas.value = rotas
                }
            }
        }
    }
    
    /**
     * ✅ NOVO: Aplica filtro de acesso de forma síncrona para o refresh
     */
    private suspend fun aplicarFiltroAcessoCompleto(rotas: List<RotaResumo>) {
        val isAdmin = userSessionManager.isAdmin()
        val userId = userSessionManager.getCurrentUserId()
        
        android.util.Log.d("RoutesViewModel", "🔍 Aplicando filtro completo de rotas:")
        android.util.Log.d("RoutesViewModel", "   É Admin: $isAdmin")
        android.util.Log.d("RoutesViewModel", "   Total de rotas: ${rotas.size}")
        
        if (isAdmin) {
            // Admin vê todas as rotas
            _rotasResumoFiltradas.value = rotas
            android.util.Log.d("RoutesViewModel", "✅ ADMIN - Mostrando todas as ${rotas.size} rotas")
        } else {
            try {
                // Buscar rotas onde o usuário é responsável
                val rotasResponsavel = appRepository.obterRotasPorColaborador(userId).first()
                
                android.util.Log.d("RoutesViewModel", "🔍 Buscando rotas responsável para usuário $userId")
                
                // Filtrar apenas as rotas onde o usuário é responsável
                val rotasFiltradas = rotas.filter { rotaResumo ->
                    rotasResponsavel.any { colaboradorRota ->
                        colaboradorRota.rotaId == rotaResumo.rota.id
                    }
                }
                
                android.util.Log.d("RoutesViewModel", "✅ USER - Mostrando ${rotasFiltradas.size} rotas responsável:")
                rotasFiltradas.forEach { rotaResumo ->
                    android.util.Log.d("RoutesViewModel", "   - ${rotaResumo.rota.nome} (Ciclo: ${rotaResumo.cicloAtual}, Status: ${rotaResumo.status})")
                }
                
                _rotasResumoFiltradas.value = rotasFiltradas
                
            } catch (e: Exception) {
                android.util.Log.e("RoutesViewModel", "Erro ao filtrar rotas por responsabilidade: ${e.message}", e)
                // Em caso de erro, mostrar todas as rotas
                _rotasResumoFiltradas.value = rotas
            }
        }
    }
    
    /**
     * FASE 3: Calcula estatísticas gerais das rotas incluindo valores acertados não finalizados.
     */
    private fun calcularEstatisticas(rotas: List<RotaResumo>): EstatisticasGerais {
        return EstatisticasGerais(
            totalClientesAtivos = rotas.sumOf { it.clientesAtivos },
            totalPendencias = rotas.sumOf { it.pendencias },
            totalMesas = rotas.sumOf { it.quantidadeMesas },
            valorTotalAcertado = rotas.sumOf { it.valorAcertado },
            // FASE 3: Calcular apenas valores não finalizados
            valorAcertadoNaoFinalizado = calcularValorAcertadoNaoFinalizado(rotas)
        )
    }

    /**
     * FASE 3: Calcula a somatória dos valores acertados que ainda não foram finalizados.
     */
    private fun calcularValorAcertadoNaoFinalizado(rotas: List<RotaResumo>): Double {
        // TODO: Implementar cálculo real quando houver dados de acertos
        // Por enquanto, simula valores baseados nas rotas
        return rotas.sumOf { rotaResumo ->
            // Simula que 70% dos valores ainda não foram finalizados
            rotaResumo.valorAcertado * 0.7
        }
    }

    /**
     * Navega para a lista de clientes de uma rota específica.
     */
    fun navigateToClients(rotaResumo: RotaResumo) {
        _navigateToClients.value = rotaResumo.rota.id
    }

    /**
     * Limpa o estado de navegação após navegar.
     */
    fun navigationToClientsCompleted() {
        _navigateToClients.value = null
    }

    /**
     * Obter o AppRepository para operações de sincronização.
     */
    fun getAppRepository(): AppRepository {
        return appRepository
    }


    /**
     * Limpa mensagens de erro e sucesso.
     */
    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    /**
     * Recarrega os dados das rotas.
     * ✅ CORREÇÃO: Método mais agressivo para forçar atualização após sincronização
     */
    fun refresh() {
        android.util.Log.d("RoutesViewModel", "🔄 Forçando refresh dos dados das rotas")
        viewModelScope.launch {
            try {
                // ✅ CORREÇÃO: Forçar recálculo imediato das estatísticas
                val rotasAtuais = appRepository.getRotasResumoComAtualizacaoTempoReal().first()
                android.util.Log.d("RoutesViewModel", "📊 Dados atualizados: ${rotasAtuais.size} rotas")
                
                // ✅ CORREÇÃO: Aplicar filtro de acesso imediatamente
                aplicarFiltroAcessoCompleto(rotasAtuais)
                
                // ✅ NOVO: Forçar atualização das estatísticas também
                val estatisticasAtuais = calcularEstatisticas(rotasAtuais)
                android.util.Log.d("RoutesViewModel", "📈 Estatísticas recalculadas: ${estatisticasAtuais.totalClientesAtivos} clientes, ${estatisticasAtuais.totalMesas} mesas")
                
            } catch (e: Exception) {
                android.util.Log.e("RoutesViewModel", "Erro ao fazer refresh: ${e.message}", e)
            }
        }
    }

    /**
     * Carrega metas para uma rota específica
     */
    suspend fun carregarMetasPorRota(rotaId: Long): List<MetaColaborador> {
        return try {
            appRepository.obterMetasPorRota(rotaId).first()
        } catch (e: Exception) {
            android.util.Log.e("RoutesViewModel", "Erro ao carregar metas da rota $rotaId: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Calcula o progresso de uma meta baseado no valor atual vs valor meta
     */
    fun calcularProgressoMeta(meta: MetaColaborador): Double {
        return if (meta.valorMeta > 0) {
            (meta.valorAtual / meta.valorMeta * 100).coerceAtMost(100.0)
        } else 0.0
    }

    /**
     * Formata o texto da meta para exibição
     */
    fun formatarTextoMeta(meta: MetaColaborador): String {
        val progresso = calcularProgressoMeta(meta)
        return when (meta.tipoMeta) {
            TipoMeta.FATURAMENTO -> "Faturamento: ${String.format("%.0f", meta.valorAtual)}/${String.format("%.0f", meta.valorMeta)} (${String.format("%.1f", progresso)}%)"
            TipoMeta.CLIENTES_ACERTADOS -> "Clientes: ${String.format("%.0f", meta.valorAtual)}/${String.format("%.0f", meta.valorMeta)} (${String.format("%.1f", progresso)}%)"
            TipoMeta.MESAS_LOCADAS -> "Mesas: ${String.format("%.0f", meta.valorAtual)}/${String.format("%.0f", meta.valorMeta)} (${String.format("%.1f", progresso)}%)"
            TipoMeta.TICKET_MEDIO -> "Ticket Médio: ${String.format("%.2f", meta.valorAtual)}/${String.format("%.2f", meta.valorMeta)} (${String.format("%.1f", progresso)}%)"
        }
    }


    /**
     * FASE 3: Data class para estatísticas gerais incluindo valores não finalizados.
     */
    data class EstatisticasGerais(
        val totalClientesAtivos: Int,
        val totalPendencias: Int,
        val totalMesas: Int,
        val valorTotalAcertado: Double,
        val valorAcertadoNaoFinalizado: Double // FASE 3: Valores não finalizados
    )
}

data class SyncDialogState(
    val pendingCount: Int,
    val isCloudData: Boolean = false, // Indica se o diálogo é para dados na nuvem
    val lastSyncTimestamp: Long? = null
)

