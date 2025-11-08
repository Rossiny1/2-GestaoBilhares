package com.example.gestaobilhares.ui.contracts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.ContratoLocacao
import com.example.gestaobilhares.data.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
class SignatureCaptureViewModel : ViewModel() {
    
    // ✅ CORREÇÃO: Repository como lateinit para inicialização posterior
    private lateinit var repository: AppRepository
    
    private val _contrato = MutableStateFlow<ContratoLocacao?>(null)
    val contrato: StateFlow<ContratoLocacao?> = _contrato.asStateFlow()
    
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    /**
     * ✅ CORREÇÃO: Inicializar repository
     */
    fun initializeRepository(repository: AppRepository) {
        this.repository = repository
    }
    
    private val _assinaturaSalva = MutableStateFlow(false)
    val assinaturaSalva: StateFlow<Boolean> = _assinaturaSalva.asStateFlow()
    
    suspend fun getMesasVinculadas(): List<com.example.gestaobilhares.data.entities.Mesa> {
        val contratoId = _contrato.value?.id ?: 0L
        val contratoMesas = repository.buscarMesasPorContrato(contratoId)
        val mesas = mutableListOf<com.example.gestaobilhares.data.entities.Mesa>()
        
        contratoMesas.forEach { contratoMesa ->
            val mesa = repository.obterMesaPorId(contratoMesa.mesaId)
            if (mesa != null) {
                mesas.add(mesa)
            }
        }
        
        return mesas
    }
    
    fun carregarContrato(contratoId: Long) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val contrato = repository.buscarContratoPorId(contratoId)
                _contrato.value = contrato
            } catch (e: Exception) {
                _error.value = "Erro ao carregar contrato: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun salvarAssinatura(assinaturaBase64: String) {
        salvarAssinaturaComMetadados(
            assinaturaBase64 = assinaturaBase64,
            hashAssinatura = null,
            deviceId = null,
            ipAddress = null,
            timestamp = null,
            pressaoMedia = null,
            velocidadeMedia = null,
            duracao = null,
            totalPontos = null
        )
    }
    
    /**
     * ✅ CONFORMIDADE JURÍDICA CLÁUSULA 9.3: Salva assinatura com metadados completos
     */
    fun salvarAssinaturaComMetadados(
        assinaturaBase64: String,
        hashAssinatura: String?,
        deviceId: String?,
        ipAddress: String?,
        timestamp: Long?,
        pressaoMedia: Float?,
        velocidadeMedia: Float?,
        duracao: Long?,
        totalPontos: Int?
    ) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val contrato = _contrato.value ?: throw Exception("Contrato não encontrado")
                
                val contratoAtualizado = contrato.copy(
                    assinaturaLocatario = assinaturaBase64,
                    // ✅ CONFORMIDADE JURÍDICA CLÁUSULA 9.3: Metadados da assinatura do locatário
                    locatarioAssinaturaHash = hashAssinatura,
                    locatarioAssinaturaDeviceId = deviceId,
                    locatarioAssinaturaIpAddress = ipAddress,
                    locatarioAssinaturaTimestamp = timestamp ?: System.currentTimeMillis(),
                    locatarioAssinaturaPressaoMedia = pressaoMedia,
                    locatarioAssinaturaVelocidadeMedia = velocidadeMedia,
                    locatarioAssinaturaDuracao = duracao,
                    locatarioAssinaturaTotalPontos = totalPontos,
                    dataAtualizacao = Date()
                )
                
                repository.atualizarContrato(contratoAtualizado)
                _contrato.value = contratoAtualizado
                _assinaturaSalva.value = true
                
            } catch (e: Exception) {
                _error.value = "Erro ao salvar assinatura: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun salvarAssinaturaDistrato(assinaturaBase64: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val contrato = _contrato.value ?: throw Exception("Contrato não encontrado")
                val contratoAtualizado = contrato.copy(
                    distratoAssinaturaLocatario = assinaturaBase64,
                    distratoDataAssinatura = Date(),
                    dataAtualizacao = Date()
                )
                repository.atualizarContrato(contratoAtualizado)
                _contrato.value = contratoAtualizado
                _assinaturaSalva.value = true
            } catch (e: Exception) {
                _error.value = "Erro ao salvar assinatura do distrato: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    suspend fun getMesasParaDistrato(): List<com.example.gestaobilhares.data.entities.Mesa> {
        val contratoId = _contrato.value?.id ?: 0L
        val contratoMesas = repository.buscarMesasPorContrato(contratoId)
        val mesas = mutableListOf<com.example.gestaobilhares.data.entities.Mesa>()
        contratoMesas.forEach { cm ->
            val mesa = repository.obterMesaPorId(cm.mesaId)
            if (mesa != null) mesas.add(mesa)
        }
        return mesas
    }

    suspend fun getFechamentoResumoDistrato(): com.example.gestaobilhares.utils.ContractPdfGenerator.FechamentoResumo {
        val contrato = _contrato.value ?: throw Exception("Contrato não encontrado")
        val ultimo = repository.buscarUltimoAcertoPorCliente(contrato.clienteId)
        val totalRecebido = ultimo?.valorRecebido ?: 0.0
        val despesasViagem = 0.0
        val subtotal = totalRecebido - despesasViagem
        val comissaoMotorista = subtotal * 0.03
        val comissaoIltair = totalRecebido * 0.02
        val totalGeral = subtotal - comissaoMotorista - comissaoIltair
        val saldo = ultimo?.debitoAtual ?: 0.0
        return com.example.gestaobilhares.utils.ContractPdfGenerator.FechamentoResumo(
            totalRecebido, despesasViagem, subtotal, comissaoMotorista, comissaoIltair, totalGeral, saldo
        )
    }
    
    /**
     * ✅ NOVO: Obter assinatura do representante legal ativa
     * Retorna a assinatura Base64 do representante legal para uso automático em contratos
     */
    suspend fun obterAssinaturaRepresentanteLegalAtiva(): String? {
        return try {
            val assinatura = repository.obterAssinaturaRepresentanteLegalAtiva()
            assinatura?.assinaturaBase64
        } catch (e: Exception) {
            android.util.Log.e("SignatureCaptureViewModel", "Erro ao obter assinatura do representante: ${e.message}")
            null
        }
    }
}

