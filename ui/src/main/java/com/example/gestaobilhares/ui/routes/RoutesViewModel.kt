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
    
    // ✅ NOVO: Timestamp do último login verificado (para evitar verificações repetidas)
    private var lastCheckedLoginTimestamp: Long = 0L


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
     * ✅ CORRIGIDO: Verifica operações pendentes de sincronização
     * Verifica tanto dados para importar (nuvem) quanto para exportar (pendências locais)
     * ✅ CORREÇÃO: Não mostra diálogo se já foi fechado pelo usuário nesta sessão
     * ✅ SEGURANÇA: Só mostra diálogo se o app estiver online
     */
    /**
     * ✅ REFATORADO COMPLETO: Verifica operações pendentes de sincronização
     * Baseado na documentação oficial do Android para gerenciamento de estado
     * 
     * Condições para mostrar diálogo:
     * 1. É um NOVO login (timestamp de login mudou)
     * 2. App está online
     * 3. Há dados pendentes (para importar OU exportar)
     * 4. Diálogo ainda não foi mostrado nesta sessão de login
     */
    fun checkSyncPendencies(context: android.content.Context) {
        viewModelScope.launch {
            try {
                val currentLoginTimestamp = userSessionManager.getLoginTimestamp()
                
                // ✅ CONDIÇÃO CRÍTICA 1: Só verificar se é um NOVO login (timestamp mudou)
                // Isso previne que o diálogo apareça ao retornar de outras telas
                if (currentLoginTimestamp == lastCheckedLoginTimestamp && lastCheckedLoginTimestamp > 0L) {
                    android.util.Log.d("RoutesViewModel", "ℹ️ Mesmo login já verificado (timestamp: $currentLoginTimestamp) - ignorando verificação")
                    _syncDialogState.value = null
                    return@launch
                }
                
                // ✅ CONDIÇÃO CRÍTICA 2: Verificar se o diálogo já foi mostrado para este login
                if (hasSyncDialogBeenShown(context)) {
                    android.util.Log.d("RoutesViewModel", "ℹ️ Diálogo já foi mostrado para este login (timestamp: $currentLoginTimestamp) - não mostrando novamente")
                    lastCheckedLoginTimestamp = currentLoginTimestamp // Marcar como verificado
                    _syncDialogState.value = null
                    return@launch
                }
                
                // ✅ CONDIÇÃO 1: Verificar conectividade primeiro - só mostrar diálogo se estiver online
                val networkUtils = com.example.gestaobilhares.sync.utils.NetworkUtils(context)
                val isOnline = networkUtils.isConnected()
                
                android.util.Log.d("RoutesViewModel", "🔍 Verificando pendências de sincronização para NOVO login (timestamp: $currentLoginTimestamp)...")
                android.util.Log.d("RoutesViewModel", "📶 Status de conectividade: ${if (isOnline) "ONLINE" else "OFFLINE"}")
                
                // Se estiver offline, não mostrar diálogo
                if (!isOnline) {
                    android.util.Log.d("RoutesViewModel", "ℹ️ App offline - não mostrando diálogo de sincronização")
                    lastCheckedLoginTimestamp = currentLoginTimestamp // Marcar como verificado mesmo offline
                    _syncDialogState.value = null
                    return@launch
                }
                
                val syncRepository = com.example.gestaobilhares.sync.SyncRepository(context, appRepository)
                val lastGlobalSync = runCatching {
                    syncRepository.getGlobalLastSyncTimestamp()
                }.getOrDefault(0L).takeIf { it > 0L }
                android.util.Log.d("RoutesViewModel", "📅 Última sincronização: $lastGlobalSync")
                
                // ✅ CORREÇÃO: Verificar pendências locais (dados para exportar)
                val pending = appRepository.contarOperacoesSyncPendentes()
                android.util.Log.d("RoutesViewModel", "📡 Pendências de sincronização (exportar): $pending")
                
                // ✅ CORREÇÃO: Verificar se há dados na nuvem (dados para importar)
                val rotasLocais = appRepository.obterTodasRotas().first()
                android.util.Log.d("RoutesViewModel", "🗂️ Rotas locais: ${rotasLocais.size}")
                
                var hasDataInCloud = false
                if (rotasLocais.isEmpty() || pending == 0) {
                    // Se banco está vazio ou não há pendências, verificar se há dados na nuvem
                    android.util.Log.d("RoutesViewModel", "🔍 Verificando dados na nuvem...")
                    try {
                        hasDataInCloud = syncRepository.hasDataInCloud()
                        android.util.Log.d("RoutesViewModel", "📡 Dados na nuvem encontrados: $hasDataInCloud")
                    } catch (e: Exception) {
                        android.util.Log.e("RoutesViewModel", "❌ Erro ao verificar dados na nuvem: ${e.message}", e)
                        // Se banco está vazio e houve erro, assumir que pode haver dados
                        if (rotasLocais.isEmpty()) {
                            hasDataInCloud = true
                            android.util.Log.d("RoutesViewModel", "⚠️ Banco vazio e erro ao verificar nuvem - assumindo que pode haver dados")
                        }
                    }
                }
                
                // ✅ CONDIÇÃO 3: Verificar se há dados pendentes (para importar OU exportar)
                val needsSync = pending > 0 || hasDataInCloud
                
                if (needsSync) {
                    val pendingCount = if (pending > 0) pending else 1
                    
                    android.util.Log.d("RoutesViewModel", "✅ Todas as condições atendidas - mostrando diálogo de sincronização:")
                    android.util.Log.d("RoutesViewModel", "   ✓ Novo login detectado (timestamp: $currentLoginTimestamp)")
                    android.util.Log.d("RoutesViewModel", "   ✓ App online")
                    android.util.Log.d("RoutesViewModel", "   ✓ Diálogo ainda não foi mostrado")
                    android.util.Log.d("RoutesViewModel", "   ✓ Pendências para exportar: $pending")
                    android.util.Log.d("RoutesViewModel", "   ✓ Dados na nuvem para importar: $hasDataInCloud")
                    android.util.Log.d("RoutesViewModel", "   Total: $pendingCount")

                    _syncDialogState.value = SyncDialogState(
                        pendingCount = pendingCount,
                        isCloudData = hasDataInCloud,
                        hasLocalPending = pending > 0,
                        lastSyncTimestamp = lastGlobalSync
                    )
                    
                    // Marcar como verificado para este login
                    lastCheckedLoginTimestamp = currentLoginTimestamp
                } else {
                    android.util.Log.d("RoutesViewModel", "ℹ️ Nenhuma pendência de sincronização - não mostrando diálogo")
                    // Marcar como verificado mesmo sem pendências
                    lastCheckedLoginTimestamp = currentLoginTimestamp
                    _syncDialogState.value = null
                }
            } catch (e: Exception) {
                android.util.Log.e("RoutesViewModel", "Erro ao verificar pendências de sync: ${e.message}", e)
                // Em caso de erro, tentar mostrar diálogo se banco está vazio E estiver online
                try {
                    val networkUtils = com.example.gestaobilhares.sync.utils.NetworkUtils(context)
                    val isOnline = networkUtils.isConnected()
                    
                    if (isOnline) {
                        val rotasLocais = appRepository.obterTodasRotas().first()
                        // ✅ CORREÇÃO: Só mostrar diálogo fallback se ainda não foi mostrado
                        if (rotasLocais.isEmpty() && !hasSyncDialogBeenShown(context)) {
                            android.util.Log.d("RoutesViewModel", "⚠️ Erro na verificação, mas banco vazio e online - mostrando diálogo (fallback)")
                            _syncDialogState.value = SyncDialogState(
                                pendingCount = 1,
                                isCloudData = true,
                                hasLocalPending = false,
                                lastSyncTimestamp = null
                            )
                        }
                    } else {
                        android.util.Log.d("RoutesViewModel", "ℹ️ Erro na verificação, mas app offline - não mostrando diálogo")
                    }
                } catch (e2: Exception) {
                    android.util.Log.e("RoutesViewModel", "Erro ao verificar rotas locais: ${e2.message}", e2)
                }
            }
        }
    }

    /**
     * ✅ REFATORADO COMPLETO: Marca diálogo de sincronização como mostrado usando SharedPreferences
     * Baseado na documentação oficial do Android para persistência de estado
     * Armazena flag, userId e timestamp de login para detectar novos logins de forma confiável
     * Usa commit() para garantir salvamento imediato (recomendado pela documentação oficial)
     */
    fun dismissSyncDialog(context: android.content.Context) {
        val userId = userSessionManager.getCurrentUserId()
        if (userId != 0L) {
            val loginTimestamp = userSessionManager.getLoginTimestamp()
            val prefs = context.getSharedPreferences("sync_dialog_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().apply {
                putBoolean("sync_dialog_shown_$userId", true)
                putLong("sync_dialog_login_timestamp_$userId", loginTimestamp) // Armazenar timestamp de login
                commit() // ✅ CORREÇÃO: Usar commit() para garantir salvamento imediato (documentação oficial)
            }
            android.util.Log.d("RoutesViewModel", "🔒 Diálogo marcado como mostrado para usuário $userId (login timestamp: $loginTimestamp)")
        }
        _syncDialogState.value = null
    }

    /**
     * ✅ REFATORADO COMPLETO: Verifica se o diálogo já foi mostrado nesta sessão de login
     * Usa timestamp de login do UserSessionManager para detectar novo login de forma confiável
     * Baseado na documentação oficial do Android sobre persistência de estado
     */
    private fun hasSyncDialogBeenShown(context: android.content.Context): Boolean {
        val currentUserId = userSessionManager.getCurrentUserId()
        if (currentUserId == 0L) {
            android.util.Log.d("RoutesViewModel", "🔍 Usuário não logado - não mostrar diálogo")
            return true // Não mostrar se não estiver logado
        }
        
        val prefs = context.getSharedPreferences("sync_dialog_prefs", android.content.Context.MODE_PRIVATE)
        
        // ✅ NOVA ABORDAGEM: Usar timestamp de login para detectar novo login
        val currentLoginTimestamp = userSessionManager.getLoginTimestamp()
        val storedLoginTimestamp = prefs.getLong("sync_dialog_login_timestamp_$currentUserId", 0L)
        
        // Se o timestamp de login mudou, é um novo login - permitir que apareça
        if (currentLoginTimestamp != storedLoginTimestamp && currentLoginTimestamp > 0L) {
            android.util.Log.d("RoutesViewModel", "🔄 Novo login detectado (timestamp mudou de $storedLoginTimestamp para $currentLoginTimestamp) - permitindo diálogo")
            // Limpar flag antigo se existir
            prefs.edit().remove("sync_dialog_shown_$currentUserId").apply()
            return false // Permitir que apareça
        }
        
        // Verificar se o diálogo foi mostrado para este userId neste login
        val hasBeenShown = prefs.getBoolean("sync_dialog_shown_$currentUserId", false)
        android.util.Log.d("RoutesViewModel", "🔍 Diálogo já foi mostrado para usuário $currentUserId (login timestamp: $currentLoginTimestamp): $hasBeenShown")
        return hasBeenShown
    }

    /**
     * ✅ REFATORADO COMPLETO: Reseta flag de diálogo quando há novo login
     * Usa timestamp de login para detectar novo login de forma confiável
     * Baseado na documentação oficial do Android sobre detecção de novo login
     * Esta função NÃO é mais necessária - a detecção é feita automaticamente em hasSyncDialogBeenShown()
     * Mantida para compatibilidade, mas agora é uma função vazia (lógica movida para hasSyncDialogBeenShown)
     */
    fun resetSyncDialogFlag(context: android.content.Context) {
        // ✅ NOVA ABORDAGEM: A detecção de novo login é feita automaticamente em hasSyncDialogBeenShown()
        // usando o timestamp de login. Não precisamos mais resetar manualmente.
        android.util.Log.d("RoutesViewModel", "ℹ️ resetSyncDialogFlag chamado - detecção automática de novo login ativa")
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
    val isCloudData: Boolean = false, // Indica se há dados na nuvem para importar
    val hasLocalPending: Boolean = false, // Indica se há dados locais para exportar
    val lastSyncTimestamp: Long? = null
)

