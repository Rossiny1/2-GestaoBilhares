package com.example.gestaobilhares.ui.contracts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.*
import com.example.gestaobilhares.data.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ContractGenerationViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {
    
    private val _cliente = MutableStateFlow<Cliente?>(null)
    val cliente: StateFlow<Cliente?> = _cliente.asStateFlow()
    
    private val _mesasVinculadas = MutableStateFlow<List<Mesa>>(emptyList())
    val mesasVinculadas: StateFlow<List<Mesa>> = _mesasVinculadas.asStateFlow()
    
    private val _contrato = MutableStateFlow<ContratoLocacao?>(null)
    val contrato: StateFlow<ContratoLocacao?> = _contrato.asStateFlow()
    
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Dados do tipo de acerto
    private var tipoFixo: Boolean = false
    private var valorFixo: Double = 0.0
    
    // ✅ Guarda idempotente: evita múltiplas gerações
    @Volatile private var generating: Boolean = false
    
    // Getters para as informações do tipo de acerto
    fun isTipoFixo(): Boolean = tipoFixo
    fun getValorFixo(): Double = valorFixo
    
    fun carregarDados(clienteId: Long, mesasIds: List<Long>, tipoFixo: Boolean, valorFixo: Double) {
        this.tipoFixo = tipoFixo
        this.valorFixo = valorFixo
        
        android.util.Log.d("ContractGenerationViewModel", "=== CARREGANDO DADOS ===")
        android.util.Log.d("ContractGenerationViewModel", "ClienteId: $clienteId")
        android.util.Log.d("ContractGenerationViewModel", "MesasIds recebidas: $mesasIds")
        android.util.Log.d("ContractGenerationViewModel", "TipoFixo: $tipoFixo, ValorFixo: $valorFixo")
        
        viewModelScope.launch {
            _loading.value = true
            try {
                // Carregar dados do cliente
                val clienteData = repository.obterClientePorId(clienteId)
                _cliente.value = clienteData
                android.util.Log.d("ContractGenerationViewModel", "Cliente carregado: ${clienteData?.nome}")
                
                // Carregar mesas vinculadas
                val mesas = mutableListOf<Mesa>()
                mesasIds.forEach { mesaId ->
                    android.util.Log.d("ContractGenerationViewModel", "Buscando mesa com ID: $mesaId")
                    val mesa = repository.obterMesaPorId(mesaId)
                    if (mesa != null) {
                        android.util.Log.d("ContractGenerationViewModel", "Mesa encontrada: ${mesa.numero} (${mesa.tipoMesa})")
                        mesas.add(mesa)
                    } else {
                        android.util.Log.w("ContractGenerationViewModel", "Mesa não encontrada para ID: $mesaId")
                    }
                }
                android.util.Log.d("ContractGenerationViewModel", "Total de mesas carregadas: ${mesas.size}")
                _mesasVinculadas.value = mesas
                
            } catch (e: Exception) {
                android.util.Log.e("ContractGenerationViewModel", "Erro ao carregar dados", e)
                _error.value = "Erro ao carregar dados: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun gerarContrato(
        valorMensal: Double,
        diaVencimento: Int,
        tipoPagamento: String,
        percentualReceita: Double? = null
    ) {
        // ✅ Early return se já estiver gerando/gerado nesta sessão do ViewModel
        if (generating) {
            android.util.Log.d("ContractGenerationVM", "Geração ignorada: já em andamento ou concluída")
            return
        }
        generating = true
        
        viewModelScope.launch {
            _loading.value = true
            try {
                val cliente = _cliente.value ?: throw Exception("Cliente não encontrado")
                val mesas = _mesasVinculadas.value
                
                if (mesas.isEmpty()) {
                    throw Exception("Nenhuma mesa vinculada")
                }
                
                // Gerar número do contrato
                val numeroContrato = gerarNumeroContrato()
                
                // Criar contrato
                val contrato = ContratoLocacao(
                    numeroContrato = numeroContrato,
                    clienteId = cliente.id,
                    locatarioNome = cliente.nome,
                    locatarioCpf = cliente.cpfCnpj ?: "",
                    locatarioEndereco = cliente.endereco ?: "",
                    locatarioTelefone = cliente.telefone ?: "",
                    locatarioEmail = cliente.email ?: "",
                    valorMensal = valorMensal,
                    diaVencimento = diaVencimento,
                    tipoPagamento = tipoPagamento,
                    percentualReceita = percentualReceita,
                    dataContrato = System.currentTimeMillis(),
                    dataInicio = System.currentTimeMillis()
                )
                
                // Salvar contrato
                val contratoId = repository.inserirContrato(contrato)
                
                // Vincular mesas ao contrato
                val contratoMesas = mesas.map { mesa ->
                    ContratoMesa(
                        contratoId = contratoId,
                        mesaId = mesa.id,
                        tipoEquipamento = mesa.tipoMesa.name,
                        numeroSerie = mesa.numero.toString(),
                        valorFicha = 0.0, // Mesa não tem valorFicha, usar 0.0
                        valorFixo = mesa.valorFixo
                    )
                }
                
                repository.inserirContratoMesas(contratoMesas)
                
                // Atualizar mesas para vincular ao cliente e atualizar timestamps para sync
                mesas.forEach { mesa ->
                    val agora = System.currentTimeMillis()
                    val mesaAtualizada = mesa.copy(
                        clienteId = cliente.id,
                        dataInstalacao = agora,
                        dataUltimaLeitura = agora
                    )
                    repository.atualizarMesa(mesaAtualizada)
                }
                
                _contrato.value = contrato.copy(id = contratoId)
                
            } catch (e: Exception) {
                _error.value = "Erro ao gerar contrato: ${e.message}"
                generating = false // ✅ Permitir nova tentativa apenas em erro
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun adicionarAssinaturaLocatario(assinaturaBase64: String) {
        viewModelScope.launch {
            try {
                val contrato = _contrato.value ?: return@launch
                val contratoAtualizado = contrato.copy(
                    assinaturaLocatario = assinaturaBase64,
                    dataAtualizacao = System.currentTimeMillis()
                )
                
                repository.atualizarContrato(contratoAtualizado)
                _contrato.value = contratoAtualizado
                
            } catch (e: Exception) {
                _error.value = "Erro ao salvar assinatura: ${e.message}"
            }
        }
    }
    
    private suspend fun gerarNumeroContrato(): String {
        val ano = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
        val numeroSequencial = repository.contarContratosPorAno(ano) + 1
        return "$ano-${String.format("%04d", numeroSequencial)}"
    }
    
    /**
     * ✅ NOVO: Obter assinatura do representante legal ativa
     * Retorna a assinatura Base64 do representante legal para uso automático em contratos
     */
    suspend fun obterAssinaturaRepresentanteLegalAtiva(): AssinaturaRepresentanteLegal? {
        return try {
            repository.obterAssinaturaRepresentanteLegalAtiva()
        } catch (e: Exception) {
            android.util.Log.e("ContractGenerationViewModel", "Erro ao obter assinatura do representante: ${e.message}")
            null
        }
    }
}

