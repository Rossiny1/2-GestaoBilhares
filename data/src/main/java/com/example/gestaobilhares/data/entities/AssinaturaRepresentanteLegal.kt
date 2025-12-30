package com.example.gestaobilhares.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * Entidade para armazenar a assinatura digital do representante legal da empresa
 * Utilizada em todos os contratos e aditivos conforme Lei 14.063/2020
 */
@Entity(tableName = "assinatura_representante_legal")
data class AssinaturaRepresentanteLegal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Dados do representante legal
    val nomeRepresentante: String,
    val cpfRepresentante: String,
    val cargoRepresentante: String, // Ex: "Sócio-Administrador", "Diretor", etc.
    
    // Assinatura digital (Base64)
    val assinaturaBase64: String,
    
    // Metadados de segurança (conforme Cláusula 9.3 do contrato)
    val timestampCriacao: Long, // Timestamp da criação da assinatura
    val deviceId: String, // ID do dispositivo que capturou
    val hashIntegridade: String, // SHA-256 da assinatura para verificação
    val versaoSistema: String, // Versão do app quando foi criada
    
    // Dados de auditoria
    val dataCriacao: Long = System.currentTimeMillis(),
    val criadoPor: String, // Usuário que criou (ADM)
    val ativo: Boolean = true, // Se a assinatura está ativa
    
    // Procuração e delegação de poderes
    val numeroProcuração: String, // Número único da procuração
    val dataProcuração: Long,
    val poderesDelegados: String, // JSON com os poderes específicos
    val validadeProcuração: Long? = null, // Data de validade (opcional)
    
    // Logs de uso
    val totalUsos: Int = 0, // Quantas vezes foi utilizada
    val ultimoUso: Long? = null, // Data do último uso
    val contratosAssinados: String = "", // IDs dos contratos assinados (JSON)
    
    // Status de validação jurídica
    val validadaJuridicamente: Boolean = false,
    val dataValidacao: Long? = null,
    val validadoPor: String? = null // Advogado ou responsável legal
) : Serializable
