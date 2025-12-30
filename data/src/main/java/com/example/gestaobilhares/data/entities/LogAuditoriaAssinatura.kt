package com.example.gestaobilhares.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * Entidade para logs de auditoria das assinaturas digitais
 * Conforme requisitos da Cláusula 9.3 do contrato
 */
@Entity(tableName = "logs_auditoria_assinatura")
data class LogAuditoriaAssinatura(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Identificação da operação
    val tipoOperacao: String, // "CRIACAO_ASSINATURA", "USO_CONTRATO", "USO_ADITIVO", "USO_DISTRATO"
    val idAssinatura: Long, // ID da assinatura utilizada
    val idContrato: Long? = null, // ID do contrato (se aplicável)
    val idAditivo: Long? = null, // ID do aditivo (se aplicável)
    
    // Dados do usuário que executou a operação
    val usuarioExecutou: String, // Nome do usuário
    val cpfUsuario: String, // CPF do usuário
    val cargoUsuario: String, // Cargo do usuário
    
    // Metadados técnicos
    val timestamp: Long, // Timestamp da operação
    val deviceId: String, // ID do dispositivo
    val versaoApp: String, // Versão do aplicativo
    val hashDocumento: String, // Hash do documento assinado
    val hashAssinatura: String, // Hash da assinatura aplicada
    
    // Dados de localização (se disponível)
    val latitude: Double? = null,
    val longitude: Double? = null,
    val endereco: String? = null,
    
    // Dados de rede
    val ipAddress: String? = null,
    val userAgent: String? = null,
    
    // Dados do documento
    val tipoDocumento: String, // "CONTRATO", "ADITIVO", "DISTRATO"
    val numeroDocumento: String, // Número do contrato/aditivo
    val valorContrato: Double? = null, // Valor do contrato (se aplicável)
    
    // Status da operação
    val sucesso: Boolean = true,
    val mensagemErro: String? = null,
    
    // Dados de auditoria
    val dataOperacao: Long = System.currentTimeMillis(),
    val observacoes: String? = null,
    
    // Validação jurídica
    val validadoJuridicamente: Boolean = false,
    val dataValidacao: Long? = null,
    val validadoPor: String? = null
) : Serializable
