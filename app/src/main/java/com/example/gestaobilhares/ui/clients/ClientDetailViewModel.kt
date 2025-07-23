package com.example.gestaobilhares.ui.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// Data classes definidas no final do arquivo
// Hilt removido - usando instanciação direta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.repository.MesaRepository
import com.example.gestaobilhares.data.repository.ClienteRepository
import kotlinx.coroutines.flow.collect
import kotlinx.parcelize.Parcelize
import android.os.Parcelable
import com.example.gestaobilhares.data.repository.AcertoRepository
import com.example.gestaobilhares.data.entities.Acerto
import com.example.gestaobilhares.data.entities.TipoMesa
import android.util.Log
import com.example.gestaobilhares.data.repository.AcertoMesaRepository

/**
 * ViewModel para ClientDetailFragment
 * FASE 4A - Implementação crítica com dados mock
 */
class ClientDetailViewModel(
    private val clienteRepository: com.example.gestaobilhares.data.repository.ClienteRepository,
    private val mesaRepository: MesaRepository,
    private val acertoRepository: AcertoRepository,
    private val acertoMesaRepository: AcertoMesaRepository
) : ViewModel() {

    private val _clientDetails = MutableStateFlow<ClienteResumo?>(null)
    val clientDetails: StateFlow<ClienteResumo?> = _clientDetails.asStateFlow()

    private val _settlementHistory = MutableStateFlow<List<AcertoResumo>>(emptyList())
    val settlementHistory: StateFlow<List<AcertoResumo>> = _settlementHistory.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _mesasCliente = MutableStateFlow<List<Mesa>>(emptyList())
    val mesasCliente: StateFlow<List<Mesa>> = _mesasCliente.asStateFlow()

    private val _mesasDisponiveis = MutableStateFlow<List<Mesa>>(emptyList())
    val mesasDisponiveis: StateFlow<List<Mesa>> = _mesasDisponiveis.asStateFlow()

    init {
        // Removido dados mock - agora carrega do banco de dados real
    }

    fun loadClientDetails(clienteId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            Log.d("ClientDetailViewModel", "=== CARREGANDO DETALHES DO CLIENTE $clienteId ===")
            try {
                val cliente = clienteRepository.obterPorId(clienteId)
                cliente?.let {
                    Log.d("ClientDetailViewModel", "Cliente encontrado: ${it.nome}")
                    Log.d("ClientDetailViewModel", "Endereço: ${it.endereco}")
                    Log.d("ClientDetailViewModel", "Telefone: ${it.telefone}")
                    Log.d("ClientDetailViewModel", "Email: ${it.email}")
                    Log.d("ClientDetailViewModel", "Data última atualização: ${it.dataUltimaAtualizacao}")

                    // Buscar data do último acerto REAL
                    val ultimoAcerto = acertoRepository.buscarUltimoAcertoPorCliente(clienteId)
                    val ultimaVisita = if (ultimoAcerto != null) {
                        Log.d("ClientDetailViewModel", "Último acerto encontrado em: ${ultimoAcerto.dataAcerto}")
                        
                        // ✅ NOVO: Calcular dias sem acerto
                        val hoje = java.time.LocalDate.now()
                        val dataUltimoAcerto = ultimoAcerto.dataAcerto.toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        val diasSemAcerto = java.time.temporal.ChronoUnit.DAYS.between(dataUltimoAcerto, hoje).toInt()
                        
                        Log.d("ClientDetailViewModel", "Dias sem acerto: $diasSemAcerto")
                        
                        calcularTempoRelativoReal(ultimoAcerto.dataAcerto)
                    } else {
                        Log.d("ClientDetailViewModel", "Nenhum acerto encontrado - cliente nunca visitado")
                        "Nunca visitado"
                    }

                    Log.d("ClientDetailViewModel", "Última visita calculada: $ultimaVisita")

                    // ✅ CORREÇÃO: Buscar observação do último acerto em vez da observação do cliente
                    val observacaoUltimoAcerto = acertoRepository.buscarObservacaoUltimoAcerto(clienteId)
                    val observacaoExibir = observacaoUltimoAcerto ?: "Nenhuma observação registrada."
                    Log.d("ClientDetailViewModel", "Observação do último acerto: $observacaoExibir")

                    // ✅ CORREÇÃO: Logs detalhados para debug dos dados do cliente
                    Log.d("ClientDetailViewModel", "=== DADOS DO CLIENTE ===")
                    Log.d("ClientDetailViewModel", "Nome: '${it.nome}'")
                    Log.d("ClientDetailViewModel", "Endereço no banco: '${it.endereco}'")
                    Log.d("ClientDetailViewModel", "Telefone no banco: '${it.telefone}'")
                    Log.d("ClientDetailViewModel", "Valor ficha: ${it.valorFicha}")
                    Log.d("ClientDetailViewModel", "Comissão ficha: ${it.comissaoFicha}")
                    
                    // ✅ CORREÇÃO: Garantir que campos do cliente sejam exibidos
                    val enderecoExibir = when {
                        it.endereco.isNullOrBlank() -> "Endereço não informado"
                        else -> it.endereco.trim()
                    }
                    
                    val telefoneExibir = when {
                        it.telefone.isNullOrBlank() -> "Telefone não informado"
                        else -> it.telefone.trim()
                    }
                    
                    Log.d("ClientDetailViewModel", "Endereço que será exibido: '$enderecoExibir'")
                    Log.d("ClientDetailViewModel", "Telefone que será exibido: '$telefoneExibir'")

                    // ✅ CORREÇÃO CRÍTICA: Buscar débito atual do ÚLTIMO ACERTO como fonte da verdade
                    val debitoAtualReal = ultimoAcerto?.debitoAtual ?: 0.0
                    Log.d("ClientDetailViewModel", "Débito atual REAL (do último acerto): R$ $debitoAtualReal")
                    
                    _clientDetails.value = ClienteResumo(
                        id = it.id,
                        nome = it.nome,
                        endereco = enderecoExibir,
                        telefone = telefoneExibir,
                        valorFicha = it.valorFicha,
                        comissaoFicha = it.comissaoFicha,
                        mesasAtivas = 0, // Atualizado abaixo
                        ultimaVisita = ultimaVisita,
                        observacoes = observacaoExibir,
                        debitoAtual = debitoAtualReal, // ✅ CORREÇÃO: Usar débito atual do último acerto
                        diasSemAcerto = if (ultimoAcerto != null) {
                            val hoje = java.time.LocalDate.now()
                            val dataUltimoAcerto = ultimoAcerto.dataAcerto.toInstant()
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                            java.time.temporal.ChronoUnit.DAYS.between(dataUltimoAcerto, hoje).toInt()
                        } else {
                            0 // Se nunca acertou, considera 0 dias
                        }
                    )

                    Log.d("ClientDetailViewModel", "ClienteResumo criado: ${_clientDetails.value?.nome}")

                    // Tentar carregar mesas reais do banco
                    mesaRepository.obterMesasPorCliente(clienteId).collect { mesas ->
                        Log.d("ClientDetailViewModel", "Mesas reais encontradas: ${mesas.size}")
                        _mesasCliente.value = mesas
                        _clientDetails.value = _clientDetails.value?.copy(mesasAtivas = mesas.size)
                        Log.d("ClientDetailViewModel", "Mesas carregadas: ${mesas.size}")
                        mesas.forEachIndexed { index, mesa ->
                            Log.d("ClientDetailViewModel", "Mesa $index: ${mesa.numero} (ID: ${mesa.id}, Tipo: ${mesa.tipoMesa})")
                        }
                    }

                    // Buscar histórico real
                    loadSettlementHistory(clienteId)
                }
            } catch (e: Exception) {
                Log.e("ClientDetailViewModel", "Erro ao carregar detalhes do cliente", e)
                e.printStackTrace()
            } finally {
                _isLoading.value = false
                Log.d("ClientDetailViewModel", "=== CARREGAMENTO CONCLUÍDO ===")
            }
        }
    }

    fun adicionarMesaAoCliente(mesaId: Long, clienteId: Long) {
        viewModelScope.launch {
            mesaRepository.vincularMesa(mesaId, clienteId)
            loadClientDetails(clienteId)
        }
    }

    /**
     * ✅ NOVO FLUXO: Verifica se mesa pode ser retirada ou precisa de acerto
     */
    suspend fun verificarSeRetiradaEPermitida(mesaId: Long, clienteId: Long): RetiradaStatus {
        return try {
            // Buscar último acerto da mesa
            val ultimoAcertoMesa = acertoRepository.buscarUltimoAcertoMesa(mesaId)
            val hoje = java.util.Calendar.getInstance()
            hoje.set(java.util.Calendar.HOUR_OF_DAY, 0)
            hoje.set(java.util.Calendar.MINUTE, 0)
            hoje.set(java.util.Calendar.SECOND, 0)
            hoje.set(java.util.Calendar.MILLISECOND, 0)
            val inicioHoje = hoje.time
            
            if (ultimoAcertoMesa != null) {
                // Verificar se foi acertada hoje
                val dataAcerto = ultimoAcertoMesa.dataAcerto
                if (dataAcerto.after(inicioHoje)) {
                    // Mesa foi acertada hoje - pode retirar
                    RetiradaStatus.PODE_RETIRAR
                } else {
                    // Mesa não foi acertada hoje - precisa acertar primeiro
                    RetiradaStatus.PRECISA_ACERTO
                }
            } else {
                // Nunca foi acertada - precisa acertar primeiro
                RetiradaStatus.PRECISA_ACERTO
            }
        } catch (e: Exception) {
            Log.e("ClientDetailViewModel", "Erro ao verificar status de retirada: ${e.message}")
            RetiradaStatus.PRECISA_ACERTO
        }
    }

    fun retirarMesaDoCliente(mesaId: Long, clienteId: Long, relogioFinal: Int, valorRecebido: Double) {
        viewModelScope.launch {
            try {
                // ✅ NOVO: Atualizar mesa com relógio final informado pelo usuário
                mesaRepository.atualizarRelogioFinal(mesaId, relogioFinal)
                
                // Retirar mesa (volta para depósito com relógio final como inicial)
                mesaRepository.retirarMesa(mesaId)
                
                Log.d("ClientDetailViewModel", "Mesa $mesaId retirada do cliente $clienteId")
                Log.d("ClientDetailViewModel", "Relógio final: $relogioFinal, Valor recebido: R$ $valorRecebido")
                Log.d("ClientDetailViewModel", "Mesa retornou ao depósito com relógio inicial = $relogioFinal")
                
                // Recarregar dados do cliente
                loadClientDetails(clienteId)
            } catch (e: Exception) {
                Log.e("ClientDetailViewModel", "Erro ao retirar mesa", e)
            }
        }
    }

    fun loadMesasDisponiveis() {
        viewModelScope.launch {
            mesaRepository.obterMesasDisponiveis().collect { mesas ->
                _mesasDisponiveis.value = mesas
            }
        }
    }

    fun isAdminUser(): Boolean {
        // TODO: Implementar checagem real de permissão do usuário logado
        // Exemplo mock: return true para admin, false para user comum
        return true // Trocar para lógica real
    }

    fun salvarObservacaoCliente(clienteId: Long, observacao: String) {
        viewModelScope.launch {
            try {
                clienteRepository.atualizarObservacao(clienteId, observacao)
                loadClientDetails(clienteId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Adiciona um novo acerto ao histórico do cliente e recarrega do banco de dados.
     */
    fun adicionarAcertoNoHistorico(novoAcerto: AcertoResumo) {
        Log.d("ClientDetailViewModel", "Adicionando acerto ao histórico: $novoAcerto")
        
        // Recarregar histórico do banco de dados para garantir sincronização
        // Usar o clienteId atual do _clientDetails
        _clientDetails.value?.let { cliente ->
            loadSettlementHistory(cliente.id)
        }
        
        // Também adicionar à lista atual para feedback imediato
        val listaAtual = _settlementHistory.value.toMutableList()
        listaAtual.add(0, novoAcerto) // Adiciona no topo (mais recente)
        _settlementHistory.value = listaAtual
    }

    fun loadSettlementHistory(clienteId: Long) {
        viewModelScope.launch {
            Log.d("ClientDetailViewModel", "Carregando histórico de acertos para cliente: $clienteId")
            try {
                acertoRepository.buscarPorCliente(clienteId).collect { acertos ->
                    Log.d("ClientDetailViewModel", "=== CARREGANDO HISTÓRICO - DEBUG OBSERVAÇÕES ===")
                    Log.d("ClientDetailViewModel", "Acertos encontrados no banco: ${acertos.size}")
                    
                    val acertosResumo = acertos.map { acerto ->
                        Log.d("ClientDetailViewModel", "🔍 Acerto ID ${acerto.id}:")
                        Log.d("ClientDetailViewModel", "  - Observação no banco: '${acerto.observacoes}'")
                        Log.d("ClientDetailViewModel", "  - Observação é nula? ${acerto.observacoes == null}")
                        Log.d("ClientDetailViewModel", "  - Observação é vazia? ${acerto.observacoes?.isEmpty()}")
                        
                        // ✅ CORREÇÃO: Garantir que observação seja exibida corretamente
                        val observacaoExibir = when {
                            acerto.observacoes.isNullOrBlank() -> "Sem observações"
                            else -> acerto.observacoes.trim()
                        }
                        
                        Log.d("ClientDetailViewModel", "  - Observação que será exibida: '$observacaoExibir'")
                        
                        AcertoResumo(
                            id = acerto.id,
                            data = android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", acerto.dataAcerto).toString(),
                            valorTotal = acerto.valorRecebido,
                            status = acerto.status.name,
                            mesasAcertadas = acerto.totalMesas.toInt(),
                            debitoAtual = acerto.debitoAtual,
                            observacao = observacaoExibir // ✅ CORREÇÃO: Usar observação garantida
                        )
                    }
                    _settlementHistory.value = acertosResumo
                    Log.d("ClientDetailViewModel", "✅ Histórico atualizado: ${_settlementHistory.value.size} acertos")
                    
                    // ✅ CORREÇÃO: Log detalhado do que foi salvo no histórico
                    acertosResumo.forEach { resumo ->
                        Log.d("ClientDetailViewModel", "📋 Resumo ID ${resumo.id}: observação = '${resumo.observacao}'")
                    }
                }
            } catch (e: Exception) {
                Log.e("ClientDetailViewModel", "Erro ao carregar histórico de acertos", e)
                // Manter dados existentes em caso de erro
                if (_settlementHistory.value.isEmpty()) {
                    Log.d("ClientDetailViewModel", "Mantendo dados existentes devido a erro")
                }
            }
        }
    }

    /**
     * Busca a data do último acerto do cliente para exibir como "última visita"
     */
    fun buscarDataUltimoAcerto(clienteId: Long, callback: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val ultimoAcerto = acertoRepository.buscarUltimoAcertoPorCliente(clienteId)
                if (ultimoAcerto != null) {
                    val formatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("pt", "BR"))
                    val dataFormatada = formatter.format(ultimoAcerto.dataAcerto)
                    callback(dataFormatada)
                } else {
                    callback("Nunca")
                }
            } catch (e: Exception) {
                Log.e("ClientDetailViewModel", "Erro ao buscar último acerto: ${e.message}")
                callback("Nunca")
            }
        }
    }

    /**
     * Calcula a diferença de tempo entre a data passada e hoje, retornando string amigável
     */
    private fun calcularTempoRelativoReal(data: java.util.Date): String {
        val agora = java.util.Calendar.getInstance().time
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

    /**
     * ✅ NOVO: Busca o relógio final do último acerto de uma mesa
     */
    suspend fun buscarRelogioFinalUltimoAcerto(mesaId: Long): Int? {
        return try {
            acertoMesaRepository.buscarRelogioFinalUltimoAcerto(mesaId)
        } catch (e: Exception) {
            Log.e("ClientDetailViewModel", "Erro ao buscar relógio final do último acerto: ${e.message}")
            null
        }
    }
}

// Data classes auxiliares - FASE 4A
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
    val debitoAtual: Double = 0.0, // ✅ ADICIONADO: Campo para débito atual sincronizado
    val diasSemAcerto: Int = 0 // ✅ ADICIONADO: Campo para dias sem acerto
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

/**
 * ✅ NOVO: Status para verificação de retirada de mesa
 */
enum class RetiradaStatus {
    PODE_RETIRAR,      // Mesa foi acertada hoje, pode ser retirada
    PRECISA_ACERTO     // Mesa precisa ser acertada antes de retirar
} 
