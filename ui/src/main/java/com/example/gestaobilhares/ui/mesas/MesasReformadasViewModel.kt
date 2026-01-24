package com.example.gestaobilhares.ui.mesas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

import com.example.gestaobilhares.ui.common.BaseViewModel

@HiltViewModel
class MesasReformadasViewModel @Inject constructor(
    private val appRepository: AppRepository
) : BaseViewModel() {

    private val _cards = MutableStateFlow<List<ReformaCard>>(emptyList())
    val cards: StateFlow<List<ReformaCard>> = _cards.asStateFlow()

    private val _filtroNumeroMesa = MutableStateFlow<String?>(null)
    val filtroNumeroMesa: StateFlow<String?> = _filtroNumeroMesa.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun carregarMesasReformadas() {
        viewModelScope.launch {
            try {
                Log.d("DEBUG_CARDS", "")
                Log.d("DEBUG_CARDS", "┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓")
                Log.d("DEBUG_CARDS", "┃  CARREGANDO CARDS - Reforma de Mesas  ┃")
                Log.d("DEBUG_CARDS", "┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛")

                combine(
                    appRepository.obterTodasMesasReformadas(),
                    appRepository.obterTodosHistoricoManutencaoMesa(),
                    appRepository.obterTodasMesas(),
                    _filtroNumeroMesa
                ) { reformas, historico, todasMesas, filtro ->

                    Log.d("DEBUG_CARDS", "📊 Dados recebidos:")
                    Log.d("DEBUG_CARDS", "   - Total MesasReformadas: ${reformas.size}")
                    Log.d("DEBUG_CARDS", "   - Total HistoricoManutencaoMesa: ${historico.size}")
                    Log.d("DEBUG_CARDS", "   - Total Mesas: ${todasMesas.size}")

                    // 1. Filtrar reformas manuais (Nova Reforma)
                    val reformasManuais = reformas.filter { reforma ->
                        // Reformas que não são do Acerto (compatibilidade com dados antigos)
                        reforma.observacoes?.let { obs ->
                            val contemAcerto = obs.contains("acerto", ignoreCase = true)
                            !contemAcerto // Inverte: pega as que NÃO são do acerto
                        } ?: true // Se não tem observação, considera manual
                    }

                    Log.d("DEBUG_CARDS", "🔍 Reformas MANUAIS (Nova Reforma): ${reformasManuais.size}")

                    // 2. Filtrar históricos do ACERTO (novo fluxo estruturado)
                    val historicosAcerto = historico.filter { historico ->
                        historico.tipoManutencao == com.example.gestaobilhares.data.entities.TipoManutencao.TROCA_PANO
                        // Removida verificação de responsavel (agora é usuário real para rastreabilidade)
                    }

                    Log.d("DEBUG_CARDS", "🔍 Históricos do ACERTO (estruturado): ${historicosAcerto.size}")
                    historicosAcerto.forEach {
                        Log.d("DEBUG_CARDS", "   - Mesa ${it.numeroMesa}: ${it.descricao}")
                    }

                    // 3. Fallback: reformas antigas do Acerto (compatibilidade)
                    val reformasAcertoLegacy = reformas.filter { reforma ->
                        reforma.observacoes?.let { obs ->
                            val contemAcerto = obs.contains("acerto", ignoreCase = true)
                            val contemContexto = obs.contains("durante", ignoreCase = true) ||
                                                  obs.contains("via acerto", ignoreCase = true) ||
                                                  obs.contains("realizada", ignoreCase = true)
                            contemAcerto && contemContexto
                        } == true
                    }

                    Log.d("DEBUG_CARDS", "🔍 Reformas do ACERTO (legacy/texto): ${reformasAcertoLegacy.size}")

                    // ✅ NOVO: AGRUPAR TODOS OS ITENS POR MESA
                    val cardsAgrupados = mutableMapOf<Long, MutableList<ReformaCard>>()

                    // Adicionar reformas manuais ao agrupamento
                    reformasManuais.forEach { reforma ->
                        val mesa = todasMesas.find { it.id == reforma.mesaId }
                        val card = ReformaCard(
                            id = reforma.id,
                            mesaId = reforma.mesaId,
                            numeroMesa = reforma.numeroMesa.toIntOrNull() ?: 0,
                            descricao = "Reforma manual - Panos: ${reforma.numeroPanos}",
                            data = reforma.dataReforma,
                            origem = "NOVA_REFORMA",
                            responsavel = null,  // ✅ ADICIONAR (reformas antigas não têm responsável)
                            observacoes = reforma.observacoes
                        )
                        cardsAgrupados.getOrPut(reforma.mesaId) { mutableListOf() }.add(card)
                    }

                    // Adicionar históricos do Acerto ao agrupamento
                    historicosAcerto.forEach { historico ->
                        val mesa = todasMesas.find { it.id == historico.mesaId }
                        val card = ReformaCard(
                            id = historico.id,
                            mesaId = historico.mesaId,
                            numeroMesa = historico.numeroMesa.toIntOrNull() ?: 0,
                            descricao = historico.descricao ?: "Troca de pano realizada durante acerto",
                            data = historico.dataManutencao,
                            origem = "ACERTO",
                            responsavel = historico.responsavel,  // ✅ ADICIONAR ESTA LINHA
                            observacoes = historico.observacoes
                        )
                        cardsAgrupados.getOrPut(historico.mesaId) { mutableListOf() }.add(card)
                    }

                    // Adicionar reformas do Acerto legacy ao agrupamento
                    reformasAcertoLegacy.forEach { reforma ->
                        val mesa = todasMesas.find { it.id == reforma.mesaId }
                        val card = ReformaCard(
                            id = reforma.id,
                            mesaId = reforma.mesaId,
                            numeroMesa = reforma.numeroMesa.toIntOrNull() ?: 0,
                            descricao = "Troca via Acerto (legacy) - Panos: ${reforma.numeroPanos}",
                            data = reforma.dataReforma,
                            origem = "ACERTO_LEGACY",
                            responsavel = null,  // ✅ ADICIONAR (legacy não tem responsável estruturado)
                            observacoes = reforma.observacoes
                        )
                        cardsAgrupados.getOrPut(reforma.mesaId) { mutableListOf() }.add(card)
                    }

                    // ✅ NOVO: ORDENAR CARDS DENTRO DE CADA MESA E DEPOIS AS MESAS
                    val cardsFinais = cardsAgrupados.map { (mesaId, cardsDaMesa) ->
                        // Ordenar cards da mesa por data (mais recente primeiro)
                        val cardsOrdenados = cardsDaMesa.sortedByDescending { it.data }

                        // Adicionar header da mesa (APENAS o header - 1 card por mesa)
                        val mesa = todasMesas.find { it.id == mesaId }
                        ReformaCard(
                            id = -mesaId, // ID negativo para identificar como header
                            mesaId = mesaId,
                            numeroMesa = mesa?.numero?.toIntOrNull() ?: 0,
                            descricao = "🏓 Mesa ${mesa?.numero} - ${cardsOrdenados.size} manutenção(ões)",
                            data = cardsOrdenados.firstOrNull()?.data ?: 0L,
                            origem = "HEADER_MESA",
                            responsavel = null,  // ✅ ADICIONADO (header não tem responsável)
                            observacoes = null
                        )
                        // ✅ CORRIGIDO: Retorna apenas o header, não expande todos os cards
                    }.sortedByDescending { it.data }

                    Log.d("DEBUG_CARDS", "")
                    Log.d("DEBUG_CARDS", "📊 Resumo final AGRUPADO:")
                    Log.d("DEBUG_CARDS", "   - Mesas com reformas: ${cardsAgrupados.size}")
                    Log.d("DEBUG_CARDS", "   - Total de cards gerados: ${cardsFinais.size}")
                    Log.d("DEBUG_CARDS", "┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛")

                    // Aplicar filtro por número da mesa se necessário
                    val cardsFiltrados = if (!filtro.isNullOrBlank()) {
                        cardsFinais.filter { it.numeroMesa.toString().contains(filtro, ignoreCase = true) }
                    } else {
                        cardsFinais
                    }

                    // Emitir para UI
                    cardsFiltrados

                }.catch { e ->
                    Log.e("DEBUG_CARDS", "❌ Erro ao carregar cards", e)
                    _cards.value = emptyList()
                }.collect { cardsFiltrados ->
                    _cards.value = cardsFiltrados
                }

            } catch (e: Exception) {
                Log.e("DEBUG_CARDS", "❌ Erro ao carregar cards:", e)
                _errorMessage.value = "Erro ao carregar mesas reformadas: ${e.message}"
                hideLoading()
            }
        }
    }

    /**
     * Define o filtro por número da mesa
     */
    fun filtrarPorNumero(numero: String?) {
        _filtroNumeroMesa.value = numero?.trim()?.takeIf { it.isNotEmpty() }
    }

    /**
     * Remove o filtro
     */
    fun removerFiltro() {
        _filtroNumeroMesa.value = null
    }
    
    /**
     * ✅ NOVO: Obtém dados completos da mesa com histórico para navegação
     */
    suspend fun obterMesaComHistorico(mesaId: Long): MesaReformadaComHistorico {
        val reformas = appRepository.obterTodasMesasReformadas().first()
            .filter { it.mesaId == mesaId }
        
        val historico = appRepository.obterTodosHistoricoManutencaoMesa().first()
            .filter { it.mesaId == mesaId }
        
        val todasMesas = appRepository.obterTodasMesas().first()
        val mesa = todasMesas.find { it.id == mesaId }
        
        return MesaReformadaComHistorico(
            numeroMesa = mesa?.numero ?: "Não informado",
            mesaId = mesaId,
            tipoMesa = mesa?.tipoMesa?.name ?: "Não informado",
            tamanhoMesa = mesa?.tamanho?.name ?: "Não informado",
            reformas = reformas,
            historicoManutencoes = historico
        )
    }
}

// Data class para o card
data class ReformaCard(
    val id: Long,
    val mesaId: Long,
    val numeroMesa: Int,
    val descricao: String,
    val data: Long,
    val origem: String, // "NOVA_REFORMA", "ACERTO", "ACERTO_LEGACY", "HEADER_MESA"
    val responsavel: String? = null,  // ✅ ADICIONADO - Nome do responsável pela manutenção
    val observacoes: String?
)
