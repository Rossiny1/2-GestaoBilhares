package com.example.gestaobilhares.ui.metas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.CicloAcertoEntity
import com.example.gestaobilhares.data.entities.MetaColaborador
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import timber.log.Timber

/**
 * ViewModel para cadastro de metas
 */
@HiltViewModel
class MetaCadastroViewModel @Inject constructor(
    private val appRepository: AppRepository
) : ViewModel() {

    private val _rotas = MutableStateFlow<List<Rota>>(emptyList())
    val rotas: StateFlow<List<Rota>> = _rotas.asStateFlow()

    private val _ciclos = MutableStateFlow<List<CicloAcertoEntity>>(emptyList())
    val ciclos: StateFlow<List<CicloAcertoEntity>> = _ciclos.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _metaSalva = MutableStateFlow(false)
    val metaSalva: StateFlow<Boolean> = _metaSalva.asStateFlow()

    private val _cicloCriado = MutableStateFlow(false)
    val cicloCriado: StateFlow<Boolean> = _cicloCriado.asStateFlow()

    /**
     * Carrega todas as rotas ativas
     */
    fun carregarRotas() {
        viewModelScope.launch {
            try {
                val rotasAtivas = appRepository.obterTodasRotas().first().filter { rota -> rota.ativa }
                _rotas.value = rotasAtivas
                Timber.d("Rotas carregadas: %d", rotasAtivas.size)
            } catch (e: Exception) {
                Timber.e(e, "Erro ao carregar rotas: %s", e.message)
                _message.value = "Erro ao carregar rotas: ${e.message}"
            }
        }
    }

    /**
     * Carrega uma rota específica por ID
     */
    fun carregarRotaPorId(rotaId: Long) {
        viewModelScope.launch {
            try {
                val rota = appRepository.buscarRotaPorId(rotaId)
                if (rota != null) {
                    _rotas.value = listOf(rota)
                    Timber.d("Rota carregada: %s", rota.nome)
                    // Carregar ciclos da rota automaticamente
                    carregarCiclosPorRota(rotaId)
                } else {
                    _message.value = "Rota não encontrada"
                }
            } catch (e: Exception) {
                Timber.e(e, "Erro ao carregar rota: %s", e.message)
                _message.value = "Erro ao carregar rota: ${e.message}"
            }
        }
    }

    /**
     * Carrega ciclos para uma rota específica (apenas ciclos relevantes para metas)
     */
    fun carregarCiclosPorRota(rotaId: Long) {
        viewModelScope.launch {
            try {
                Timber.d("Carregando ciclos para rota %d", rotaId)
                
                // Buscar TODOS os ciclos da rota primeiro
                val todosCiclos = appRepository.buscarCiclosPorRota(rotaId)
                Timber.d("Total de ciclos encontrados: %d", todosCiclos.size)
                
                // Filtrar apenas ciclos que podem ter metas (EM_ANDAMENTO ou PLANEJADO)
                val ciclosParaMetas = todosCiclos.filter { ciclo ->
                    ciclo.status == com.example.gestaobilhares.data.entities.StatusCicloAcerto.EM_ANDAMENTO ||
                    ciclo.status == com.example.gestaobilhares.data.entities.StatusCicloAcerto.PLANEJADO
                }
                
                Timber.d("Ciclos filtrados para metas: %d", ciclosParaMetas.size)
                
                _ciclos.value = ciclosParaMetas
                
                if (ciclosParaMetas.isEmpty()) {
                    Timber.w("Nenhum ciclo disponível para metas. Total de ciclos: %d", todosCiclos.size)
                    _message.value = "Nenhum ciclo disponível para metas. Crie um ciclo primeiro."
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Erro ao carregar ciclos: %s", e.message)
                _message.value = "Erro ao carregar ciclos: ${e.message}"
            }
        }
    }

    /**
     * Salva uma nova meta (sem necessidade de colaborador)
     */
    fun salvarMeta(meta: MetaColaborador) {
        viewModelScope.launch {
            try {
                Timber.d("Salvando meta: %s para rota %d, ciclo %d", meta.tipoMeta, meta.rotaId ?: 0L, meta.cicloId)
                
                // Impedir meta duplicada do mesmo tipo para mesma rota e ciclo
                val rotaId = meta.rotaId ?: 0L
                val duplicada = appRepository.existeMetaDuplicada(rotaId, meta.cicloId, meta.tipoMeta)
                if (duplicada) {
                    _message.value = "Já existe uma meta de ${meta.tipoMeta} para este ciclo."
                    return@launch
                }

                // Buscar colaborador responsável pela rota (opcional)
                val colaboradorResponsavel = appRepository.buscarColaboradorResponsavelPrincipal(meta.rotaId ?: 0L)
                
                // Usar colaborador existente ou ID padrão (0 = sem colaborador específico)
                val colaboradorId = colaboradorResponsavel?.id ?: 0L
                
                if (colaboradorId == 0L) {
                    Timber.d("Meta será salva sem colaborador específico (colaboradorId = 0)")
                } else {
                    Timber.d("Meta será salva com colaborador ID: %d", colaboradorId)
                }
                
                // Definir o colaborador na meta
                val metaComColaborador = meta.copy(colaboradorId = colaboradorId)
                
                // Salvar a meta
                val metaId = appRepository.inserirMeta(metaComColaborador)
                
                Timber.d("Meta salva com ID: %d", metaId)
                _metaSalva.value = true
                
                // Reset rápido do flag para permitir nova criação imediata se necessário
                _metaSalva.value = false
                _metaSalva.value = true
                
            } catch (e: Exception) {
                Timber.e(e, "Erro ao salvar meta: %s", e.message)
                _message.value = "Erro ao salvar meta: ${e.message}"
            }
        }
    }

    /**
     * Limpa a mensagem
     */
    fun limparMensagem() {
        _message.value = null
    }

    /**
     * Cria um novo ciclo para uma rota (em andamento)
     */
    fun criarCicloParaRota(rotaId: Long) {
        viewModelScope.launch {
            try {
                Timber.d("Criando ciclo para rota %d", rotaId)
                
                // Usar ano atual dinâmico
                val anoAtual = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                
                // Buscar próximo número de ciclo
                val proximoNumero = appRepository.buscarProximoNumeroCiclo(rotaId, anoAtual)
                
                // Reset anual: se próximo número for 1, significa que estamos começando novo ciclo anual
                val numeroCicloFinal = if (proximoNumero == 1) {
                    Timber.d("Reset anual detectado para rota %d no ano %d", rotaId, anoAtual)
                    1 // Começar do 1 para novo ano
                } else {
                    proximoNumero
                }
                
                val novoCiclo = CicloAcertoEntity(
                    rotaId = rotaId,
                    numeroCiclo = numeroCicloFinal,
                    ano = anoAtual,
                    dataInicio = System.currentTimeMillis(),
                    dataFim = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000), // 30 dias
                    status = com.example.gestaobilhares.data.entities.StatusCicloAcerto.EM_ANDAMENTO,
                    debitoTotal = 0.0
                )
                
                val cicloId = appRepository.inserirCicloAcerto(novoCiclo)
                Timber.d("Ciclo criado com ID: %d (ano: %d, número: %d)", cicloId, anoAtual, numeroCicloFinal)
                
                _cicloCriado.value = true
                _message.value = "Ciclo criado com sucesso! Recarregando..."
                
                // Recarregar ciclos após criação
                carregarCiclosPorRota(rotaId)
                
            } catch (e: Exception) {
                Timber.e(e, "Erro ao criar ciclo: %s", e.message)
                _message.value = "Erro ao criar ciclo: ${e.message}"
            }
        }
    }

    /**
     * Cria um novo ciclo futuro (planejado) para uma rota
     */
    fun criarCicloFuturoParaRota(rotaId: Long) {
        viewModelScope.launch {
            try {
                Timber.d("Criando ciclo futuro para rota %d", rotaId)
                
                // Usar ano atual dinâmico
                val anoAtual = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                
                // Buscar próximo número de ciclo
                val proximoNumero = appRepository.buscarProximoNumeroCiclo(rotaId, anoAtual)
                
                // Reset anual: se próximo número for 1, significa que estamos começando novo ciclo anual
                val numeroCicloFinal = if (proximoNumero == 1) {
                    Timber.d("Reset anual detectado para ciclo futuro da rota %d no ano %d", rotaId, anoAtual)
                    1 // Começar do 1 para novo ano
                } else {
                    proximoNumero
                }
                
                val novoCiclo = CicloAcertoEntity(
                    rotaId = rotaId,
                    numeroCiclo = numeroCicloFinal,
                    ano = anoAtual,
                    dataInicio = System.currentTimeMillis(),
                    dataFim = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000), // 30 dias
                    status = com.example.gestaobilhares.data.entities.StatusCicloAcerto.PLANEJADO,
                    debitoTotal = 0.0
                )
                
                val cicloId = appRepository.inserirCicloAcerto(novoCiclo)
                Timber.d("Ciclo futuro criado com ID: %d (ano: %d, número: %d)", cicloId, anoAtual, numeroCicloFinal)
                
                _cicloCriado.value = true
                _message.value = "Ciclo futuro criado com sucesso! Recarregando..."
                
                // Recarregar ciclos após criação
                carregarCiclosPorRota(rotaId)
                
            } catch (e: Exception) {
                Timber.e(e, "Erro ao criar ciclo futuro: %s", e.message)
                _message.value = "Erro ao criar ciclo futuro: ${e.message}"
            }
        }
    }

    /**
     * Reseta o estado de meta salva
     */
    fun resetarMetaSalva() {
        _metaSalva.value = false
    }

    /**
     * Reseta o estado de ciclo criado
     */
    fun resetarCicloCriado() {
        _cicloCriado.value = false
    }
}

