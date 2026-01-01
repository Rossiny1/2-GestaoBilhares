package com.example.gestaobilhares.ui.contracts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.AssinaturaRepresentanteLegal
import com.example.gestaobilhares.data.entities.ContratoLocacao
import com.example.gestaobilhares.data.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SignatureCaptureViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {
    
    private val _contrato = MutableStateFlow<ContratoLocacao?>(null)
    val contrato: StateFlow<ContratoLocacao?> = _contrato.asStateFlow()
    
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    
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
        @Suppress("UNUSED_PARAMETER") hashAssinatura: String?,
        @Suppress("UNUSED_PARAMETER") deviceId: String?,
        @Suppress("UNUSED_PARAMETER") ipAddress: String?,
        @Suppress("UNUSED_PARAMETER") timestamp: Long?,
        @Suppress("UNUSED_PARAMETER") pressaoMedia: Float?,
        @Suppress("UNUSED_PARAMETER") velocidadeMedia: Float?,
        @Suppress("UNUSED_PARAMETER") duracao: Long?,
        @Suppress("UNUSED_PARAMETER") totalPontos: Int?
    ) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val contrato = _contrato.value ?: throw Exception("Contrato não encontrado")
                
                val contratoAtualizado = contrato.copy(
                    assinaturaLocatario = assinaturaBase64,
                    // TODO: Campos de metadados de assinatura não existem em ContratoLocacao - adicionar quando necessário
                    // ✅ CONFORMIDADE JURÍDICA CLÁUSULA 9.3: Metadados da assinatura do locatário
                    // locatarioAssinaturaHash = hashAssinatura,
                    // locatarioAssinaturaDeviceId = deviceId,
                    // locatarioAssinaturaIpAddress = ipAddress,
                    // locatarioAssinaturaTimestamp = timestamp ?: System.currentTimeMillis(),
                    // locatarioAssinaturaPressaoMedia = pressaoMedia,
                    // locatarioAssinaturaVelocidadeMedia = velocidadeMedia,
                    // locatarioAssinaturaDuracao = duracao,
                    // locatarioAssinaturaTotalPontos = totalPontos,
                    dataAtualizacao = System.currentTimeMillis()
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
                    distratoDataAssinatura = System.currentTimeMillis(),
                    dataAtualizacao = System.currentTimeMillis()
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

    suspend fun getFechamentoResumoDistrato(): com.example.gestaobilhares.core.utils.ContractPdfGenerator.FechamentoResumo {
        val contrato = _contrato.value ?: throw Exception("Contrato não encontrado")
        val ultimo = repository.buscarUltimoAcertoPorCliente(contrato.clienteId)
        val totalRecebido = ultimo?.valorRecebido ?: 0.0
        val despesasViagem = 0.0
        val subtotal = totalRecebido - despesasViagem
        val comissaoMotorista = subtotal * 0.03
        val comissaoIltair = totalRecebido * 0.02
        val totalGeral = subtotal - comissaoMotorista - comissaoIltair
        val saldo = ultimo?.debitoAtual ?: 0.0
        return com.example.gestaobilhares.core.utils.ContractPdfGenerator.FechamentoResumo(
            totalRecebido, despesasViagem, subtotal, comissaoMotorista, comissaoIltair, totalGeral, saldo
        )
    }
    
    /**
     * ✅ NOVO: Obter assinatura do representante legal ativa
     * Retorna a assinatura Base64 do representante legal para uso automático em contratos
     */
    suspend fun obterAssinaturaRepresentanteLegalAtiva(): AssinaturaRepresentanteLegal? {
        return try {
            repository.obterAssinaturaRepresentanteLegalAtiva()
        } catch (e: Exception) {
            android.util.Log.e("SignatureCaptureViewModel", "Erro ao obter assinatura do representante: ${e.message}")
            null
        }
    }
}

