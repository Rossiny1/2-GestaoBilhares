package com.example.gestaobilhares.ui.mesas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.entities.TipoMesa
import com.example.gestaobilhares.data.entities.TamanhoMesa
import com.example.gestaobilhares.data.repository.MesaRepository
import com.example.gestaobilhares.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class EstatisticasDeposito(
    val totalMesas: Int = 0,
    val mesasSinuca: Int = 0,
    val mesasMaquina: Int = 0,
    val mesasPembolim: Int = 0,
    val mesasOutros: Int = 0,
    val mesasPequenas: Int = 0,
    val mesasMedias: Int = 0,
    val mesasGrandes: Int = 0
)

class MesasDepositoViewModel(
    private val mesaRepository: MesaRepository,
    private val appRepository: AppRepository
) : ViewModel() {
    private val _mesasDisponiveis = MutableStateFlow<List<Mesa>>(emptyList())
    val mesasDisponiveis: StateFlow<List<Mesa>> = _mesasDisponiveis.asStateFlow()

    private val _estatisticas = MutableStateFlow(EstatisticasDeposito())
    val estatisticas: StateFlow<EstatisticasDeposito> = _estatisticas.asStateFlow()

    // ✅ ESTADO DE BUSCA POR NÚMERO
    private val _queryNumero = MutableStateFlow("")
    val queryNumero: StateFlow<String> = _queryNumero.asStateFlow()

    // ✅ LISTA FILTRADA REATIVA
    val mesasFiltradas: StateFlow<List<Mesa>> = combine(_mesasDisponiveis, _queryNumero) { mesas, query ->
        val q = query.trim()
        if (q.isEmpty()) mesas else mesas.filter { it.numero.contains(q, ignoreCase = true) }
    }.let { flow ->
        // Converter para StateFlow mantendo último valor
        val state = MutableStateFlow<List<Mesa>>(emptyList())
        viewModelScope.launch { flow.collect { state.value = it } }
        state.asStateFlow()
    }

    fun loadMesasDisponiveis() {
        viewModelScope.launch {
            android.util.Log.d("MesasDepositoViewModel", "=== CARREGANDO MESAS DISPONÍVEIS ===")
            mesaRepository.obterMesasDisponiveis().collect { mesas ->
                android.util.Log.d("MesasDepositoViewModel", "📊 Mesas recebidas do repositório: ${mesas.size}")
                mesas.forEach { mesa ->
                    android.util.Log.d("MesasDepositoViewModel", "Mesa: ${mesa.numero} | ID: ${mesa.id} | Ativa: ${mesa.ativa} | ClienteId: ${mesa.clienteId}")
                }
                _mesasDisponiveis.value = mesas
                android.util.Log.d("MesasDepositoViewModel", "✅ Lista atualizada no StateFlow: ${_mesasDisponiveis.value.size} mesas")
                calcularEstatisticas(mesas)
            }
        }
    }

    // ✅ ATUALIZA A QUERY DE BUSCA
    fun atualizarBuscaNumero(query: String) {
        _queryNumero.value = query
    }

    private fun calcularEstatisticas(mesas: List<Mesa>) {
        val stats = EstatisticasDeposito(
            totalMesas = mesas.size,
            mesasSinuca = mesas.count { it.tipoMesa == TipoMesa.SINUCA },
            mesasMaquina = mesas.count { it.tipoMesa == TipoMesa.JUKEBOX },
            mesasPembolim = mesas.count { it.tipoMesa == TipoMesa.PEMBOLIM },
            mesasOutros = mesas.count { it.tipoMesa == TipoMesa.OUTROS },
            mesasPequenas = mesas.count { it.tamanho == TamanhoMesa.PEQUENA },
            mesasMedias = mesas.count { it.tamanho == TamanhoMesa.MEDIA },
            mesasGrandes = mesas.count { it.tamanho == TamanhoMesa.GRANDE }
        )
        _estatisticas.value = stats
    }

    fun vincularMesaAoCliente(mesaId: Long, clienteId: Long, tipoFixo: Boolean, valorFixo: Double?) {
        android.util.Log.d("MesasDepositoViewModel", "=== VINCULANDO MESA ===")
        android.util.Log.d("MesasDepositoViewModel", "MesaId: $mesaId, ClienteId: $clienteId")
        android.util.Log.d("MesasDepositoViewModel", "TipoFixo: $tipoFixo, ValorFixo: $valorFixo")
        
        viewModelScope.launch {
            try {
                if (tipoFixo && valorFixo != null) {
                    // Vincular mesa com valor fixo
                    android.util.Log.d("MesasDepositoViewModel", "Vinculando mesa com valor fixo: $valorFixo")
                    mesaRepository.vincularMesaComValorFixo(mesaId, clienteId, valorFixo)
                } else {
                    // Vincular mesa normal (fichas jogadas)
                    android.util.Log.d("MesasDepositoViewModel", "Vinculando mesa com fichas jogadas")
                    mesaRepository.vincularMesa(mesaId, clienteId)
                }
                android.util.Log.d("MesasDepositoViewModel", "Mesa vinculada com sucesso")
                loadMesasDisponiveis()
            } catch (e: Exception) {
                android.util.Log.e("MesasDepositoViewModel", "Erro ao vincular mesa: ${e.message}", e)
            }
        }
    }
    
    /**
     * ✅ NOVO: Obtém todas as mesas já vinculadas a um cliente específico
     */
    suspend fun obterTodasMesasVinculadasAoCliente(clienteId: Long): List<Mesa> {
        return try {
            android.util.Log.d("MesasDepositoViewModel", "Buscando mesas vinculadas ao cliente: $clienteId")
            val mesas = mesaRepository.obterMesasPorClienteDireto(clienteId)
            android.util.Log.d("MesasDepositoViewModel", "Mesas encontradas: ${mesas.size}")
            mesas.forEach { mesa ->
                android.util.Log.d("MesasDepositoViewModel", "Mesa: ID=${mesa.id}, Número=${mesa.numero}, Tipo=${mesa.tipoMesa}, ClienteId=${mesa.clienteId}")
            }
            mesas
        } catch (e: Exception) {
            android.util.Log.e("MesasDepositoViewModel", "Erro ao buscar mesas vinculadas ao cliente $clienteId", e)
            emptyList()
        }
    }
    
    /**
     * ✅ NOVO: Verifica se o cliente possui contrato ativo
     */
    suspend fun verificarContratoAtivo(clienteId: Long): com.example.gestaobilhares.data.entities.ContratoLocacao? {
        return try {
            android.util.Log.d("MesasDepositoViewModel", "Verificando contrato ATIVO para cliente: $clienteId")
            
            suspend fun pickLatestContrato(): com.example.gestaobilhares.data.entities.ContratoLocacao? {
                val contratos = appRepository.buscarContratosPorCliente(clienteId).first()
                val latest = contratos.maxByOrNull { c ->
                    (c.dataEncerramento?.time ?: c.dataAtualizacao?.time ?: c.dataCriacao.time)
                }
                if (latest != null) {
                    val ts = latest.dataEncerramento ?: latest.dataAtualizacao ?: latest.dataCriacao
                    android.util.Log.d(
                        "MesasDepositoViewModel",
                        "Mais recente -> id=${latest.id}, num=${latest.numeroContrato}, status=${latest.status}, data=${ts}"
                    )
                } else {
                    android.util.Log.d("MesasDepositoViewModel", "Nenhum contrato encontrado para cliente $clienteId")
                }
                // Regra: só considerar ATIVO se o documento mais recente estiver ATIVO
                return if (latest != null && latest.status.equals("ATIVO", ignoreCase = true)) latest else null
            }

            // Tentativa 1
            var ativo = pickLatestContrato()
            if (ativo == null) {
                // Retry rápido para casos logo após distrato/atualização
                android.util.Log.d("MesasDepositoViewModel", "Retry rápido (150ms) pós-distrato para cliente $clienteId")
                kotlinx.coroutines.delay(150)
                ativo = pickLatestContrato()
            }

            if (ativo != null) {
                android.util.Log.d("MesasDepositoViewModel", "Contrato ATIVO confirmado (mais recente): ${ativo.numeroContrato}")
            } else {
                android.util.Log.d("MesasDepositoViewModel", "Sem contrato ATIVO vigente (mais recente não é ATIVO) -> gerar NOVO CONTRATO")
            }
            ativo
        } catch (e: Exception) {
            android.util.Log.e("MesasDepositoViewModel", "Erro ao verificar contrato para cliente $clienteId", e)
            null
        }
    }
} 