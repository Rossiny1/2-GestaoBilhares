package com.example.gestaobilhares.ui.contracts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.AssinaturaRepresentanteLegal
import com.example.gestaobilhares.data.entities.LogAuditoriaAssinatura
import com.example.gestaobilhares.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.*
import javax.inject.Inject

/**
 * ViewModel para gerenciar a assinatura digital do representante legal
 * Implementa todos os requisitos de segurança da Cláusula 9.3 do contrato
 */
@HiltViewModel
class RepresentanteLegalSignatureViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {
    
    private val _assinaturaAtiva = MutableStateFlow<AssinaturaRepresentanteLegal?>(null)
    val assinaturaAtiva: StateFlow<AssinaturaRepresentanteLegal?> = _assinaturaAtiva.asStateFlow()
    
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _success = MutableStateFlow<String?>(null)
    val success: StateFlow<String?> = _success.asStateFlow()
    
    init {
        carregarAssinaturaAtiva()
    }
    
    /**
     * Carrega a assinatura ativa do representante legal
     */
    private fun carregarAssinaturaAtiva() {
        viewModelScope.launch {
            try {
                val assinatura = repository.obterAssinaturaRepresentanteLegalAtiva()
                _assinaturaAtiva.value = assinatura
            } catch (e: Exception) {
                _error.value = "Erro ao carregar assinatura: ${e.message}"
            }
        }
    }
    
    /**
     * Salva a assinatura digital do representante legal
     * Implementa todos os requisitos de segurança da Cláusula 9.3
     */
    fun salvarAssinaturaRepresentante(
        nomeRepresentante: String,
        cpfRepresentante: String,
        cargoRepresentante: String,
        assinaturaBase64: String,
        deviceId: String,
        versaoSistema: String,
        criadoPor: String,
        numeroProcuração: String,
        poderesDelegados: String
    ) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            
            try {
                // 1. Gerar hash SHA-256 da assinatura para integridade
                val hashIntegridade = gerarHashSHA256(assinaturaBase64)
                
                // 2. Obter timestamp atual
                val timestamp = System.currentTimeMillis()
                
                // 3. Criar entidade da assinatura
                val assinatura = AssinaturaRepresentanteLegal(
                    nomeRepresentante = nomeRepresentante,
                    cpfRepresentante = cpfRepresentante,
                    cargoRepresentante = cargoRepresentante,
                    assinaturaBase64 = assinaturaBase64,
                    timestampCriacao = timestamp,
                    deviceId = deviceId,
                    hashIntegridade = hashIntegridade,
                    versaoSistema = versaoSistema,
                    dataCriacao = Date(),
                    criadoPor = criadoPor,
                    numeroProcuração = numeroProcuração,
                    dataProcuração = Date(),
                    poderesDelegados = poderesDelegados
                )
                
                // 4. Salvar no banco de dados
                val idAssinatura = repository.inserirAssinaturaRepresentanteLegal(assinatura)
                
                // 5. Criar log de auditoria
                val logAuditoria = LogAuditoriaAssinatura(
                    tipoOperacao = "CRIACAO_ASSINATURA",
                    idAssinatura = idAssinatura,
                    usuarioExecutou = criadoPor,
                    cpfUsuario = cpfRepresentante,
                    cargoUsuario = cargoRepresentante,
                    timestamp = timestamp,
                    deviceId = deviceId,
                    versaoApp = versaoSistema,
                    hashDocumento = hashIntegridade,
                    hashAssinatura = hashIntegridade,
                    tipoDocumento = "ASSINATURA_REPRESENTANTE",
                    numeroDocumento = numeroProcuração,
                    dataOperacao = Date(),
                    observacoes = "Assinatura digital do representante legal criada com sucesso"
                )
                
                repository.inserirLogAuditoriaAssinatura(logAuditoria)
                
                // 6. Atualizar estado
                _assinaturaAtiva.value = assinatura.copy(id = idAssinatura)
                _success.value = "Assinatura do representante legal salva com sucesso!"
                
            } catch (e: Exception) {
                _error.value = "Erro ao salvar assinatura: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    /**
     * Registra o uso da assinatura em um contrato
     */
    fun registrarUsoAssinatura(
        idContrato: Long,
        tipoDocumento: String,
        numeroDocumento: String,
        valorContrato: Double?,
        usuarioExecutou: String,
        cpfUsuario: String,
        cargoUsuario: String
    ) {
        viewModelScope.launch {
            try {
                val assinatura = _assinaturaAtiva.value ?: return@launch
                
                // Incrementar contador de uso
                repository.incrementarUsoAssinatura(assinatura.id, Date())
                
                // Criar log de auditoria
                val logAuditoria = LogAuditoriaAssinatura(
                    tipoOperacao = "USO_CONTRATO",
                    idAssinatura = assinatura.id,
                    idContrato = idContrato,
                    usuarioExecutou = usuarioExecutou,
                    cpfUsuario = cpfUsuario,
                    cargoUsuario = cargoUsuario,
                    timestamp = System.currentTimeMillis(),
                    deviceId = assinatura.deviceId,
                    versaoApp = assinatura.versaoSistema,
                    hashDocumento = assinatura.hashIntegridade,
                    hashAssinatura = assinatura.hashIntegridade,
                    tipoDocumento = tipoDocumento,
                    numeroDocumento = numeroDocumento,
                    valorContrato = valorContrato,
                    dataOperacao = Date(),
                    observacoes = "Assinatura utilizada em $tipoDocumento"
                )
                
                repository.inserirLogAuditoriaAssinatura(logAuditoria)
                
            } catch (e: Exception) {
                _error.value = "Erro ao registrar uso da assinatura: ${e.message}"
            }
        }
    }
    
    /**
     * Valida juridicamente a assinatura
     */
    fun validarAssinaturaJuridicamente(validadoPor: String) {
        viewModelScope.launch {
            try {
                val assinatura = _assinaturaAtiva.value ?: return@launch
                
                val assinaturaAtualizada = assinatura.copy(
                    validadaJuridicamente = true,
                    dataValidacao = Date(),
                    validadoPor = validadoPor
                )
                
                repository.atualizarAssinaturaRepresentanteLegal(assinaturaAtualizada)
                _assinaturaAtiva.value = assinaturaAtualizada
                _success.value = "Assinatura validada juridicamente!"
                
            } catch (e: Exception) {
                _error.value = "Erro ao validar assinatura: ${e.message}"
            }
        }
    }
    
    /**
     * Desativa a assinatura atual
     */
    fun desativarAssinatura() {
        viewModelScope.launch {
            try {
                val assinatura = _assinaturaAtiva.value ?: return@launch
                repository.desativarAssinaturaRepresentanteLegal(assinatura.id)
                _assinaturaAtiva.value = null
                _success.value = "Assinatura desativada com sucesso!"
                
            } catch (e: Exception) {
                _error.value = "Erro ao desativar assinatura: ${e.message}"
            }
        }
    }
    
    /**
     * Gera hash SHA-256 para verificação de integridade
     */
    private fun gerarHashSHA256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Limpa mensagens de erro e sucesso
     */
    fun limparMensagens() {
        _error.value = null
        _success.value = null
    }
}
