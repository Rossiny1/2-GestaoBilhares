package com.example.gestaobilhares.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.io.Serializable

/**
 * Entidade para documentação de procuração e delegação de poderes
 * Conforme legislação brasileira e boas práticas contratuais
 */
@Entity(tableName = "procuracoes_representantes")
data class ProcuraçãoRepresentante(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Dados da empresa outorgante
    val empresaNome: String = "BILHAR GLOBO R & A LTDA",
    val empresaCnpj: String = "34.994.884/0001-69",
    val empresaEndereco: String = "Rua João Pinheiro, nº 765, Bairro Centro, Montes Claros, MG",
    
    // Dados do representante legal outorgante
    val representanteLegalNome: String,
    val representanteLegalCpf: String,
    val representanteLegalCargo: String,
    
    // Dados do representante outorgado (funcionário)
    val representanteOutorgadoNome: String,
    val representanteOutorgadoCpf: String,
    val representanteOutorgadoCargo: String,
    val representanteOutorgadoUsuario: String, // Username no sistema
    
    // Dados da procuração
    val numeroProcuração: String, // Número único
    val dataProcuração: Date,
    val dataValidade: Date? = null, // Se não especificada, é por prazo indeterminado
    val tipoProcuração: String = "AD_NUTUM", // Ad nutum (revogável a qualquer tempo)
    
    // Poderes delegados (JSON com lista detalhada)
    val poderesDelegados: String, // JSON com os poderes específicos
    
    // Status
    val ativa: Boolean = true,
    val dataRevogacao: Date? = null,
    val motivoRevogacao: String? = null,
    
    // Assinaturas
    val assinaturaRepresentanteLegal: String? = null, // Base64
    val assinaturaRepresentanteOutorgado: String? = null, // Base64
    
    // Testemunhas (opcional)
    val testemunha1Nome: String? = null,
    val testemunha1Cpf: String? = null,
    val testemunha2Nome: String? = null,
    val testemunha2Cpf: String? = null,
    
    // Dados de auditoria
    val criadoPor: String, // Usuário que criou
    val dataCriacao: Date,
    val aprovadoPor: String? = null, // ADM que aprovou
    val dataAprovacao: Date? = null,
    
    // Validação jurídica
    val validadaJuridicamente: Boolean = false,
    val dataValidacaoJuridica: Date? = null,
    val validadaPor: String? = null, // Advogado responsável
    
    // Observações
    val observacoes: String? = null
) : Serializable
