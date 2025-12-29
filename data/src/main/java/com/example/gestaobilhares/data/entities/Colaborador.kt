package com.example.gestaobilhares.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.Date

/**
 * Entidade que representa um Colaborador no banco de dados.
 * Colaboradores são usuários do sistema com diferentes níveis de acesso.
 */
@Entity(tableName = "colaboradores")
data class Colaborador(
    @PrimaryKey(autoGenerate = true)
    @SerializedName("id")
    val id: Long = 0,
    
    // Dados básicos
    @ColumnInfo(name = "nome")
    @SerializedName("nome")
    val nome: String,
    
    @ColumnInfo(name = "email")
    @SerializedName("email")
    val email: String,
    
    @ColumnInfo(name = "telefone")
    @SerializedName("telefone")
    val telefone: String? = null,
    
    @ColumnInfo(name = "cpf")
    @SerializedName("cpf")
    val cpf: String? = null,
    
    // Dados pessoais completos
    @ColumnInfo(name = "data_nascimento")
    @SerializedName("data_nascimento")
    val dataNascimento: Date? = null,
    
    @ColumnInfo(name = "endereco")
    @SerializedName("endereco")
    val endereco: String? = null,
    
    @ColumnInfo(name = "bairro")
    @SerializedName("bairro")
    val bairro: String? = null,
    
    @ColumnInfo(name = "cidade")
    @SerializedName("cidade")
    val cidade: String? = null,
    
    @ColumnInfo(name = "estado")
    @SerializedName("estado")
    val estado: String? = null,
    
    @ColumnInfo(name = "cep")
    @SerializedName("cep")
    val cep: String? = null,
    
    @ColumnInfo(name = "rg")
    @SerializedName("rg")
    val rg: String? = null,
    
    @ColumnInfo(name = "orgao_emissor")
    @SerializedName("orgao_emissor")
    val orgaoEmissor: String? = null,
    
    @ColumnInfo(name = "estado_civil")
    @SerializedName("estado_civil")
    val estadoCivil: String? = null,
    
    @ColumnInfo(name = "nome_mae")
    @SerializedName("nome_mae")
    val nomeMae: String? = null,
    
    @ColumnInfo(name = "nome_pai")
    @SerializedName("nome_pai")
    val nomePai: String? = null,
    
    @ColumnInfo(name = "foto_perfil")
    @SerializedName("foto_perfil")
    val fotoPerfil: String? = null,
    
    // Sistema de acesso
    @ColumnInfo(name = "nivel_acesso")
    @SerializedName("nivel_acesso")
    val nivelAcesso: NivelAcesso = NivelAcesso.USER,
    
    @ColumnInfo(name = "ativo")
    @SerializedName("ativo")
    val ativo: Boolean = true,
    
    @ColumnInfo(name = "aprovado")
    @SerializedName("aprovado")
    val aprovado: Boolean = false,
    
    @ColumnInfo(name = "data_aprovacao")
    @SerializedName("data_aprovacao")
    val dataAprovacao: Date? = null,
    
    @ColumnInfo(name = "aprovado_por")
    @SerializedName("aprovado_por")
    val aprovadoPor: String? = null,
    
    // Autenticação
    @ColumnInfo(name = "firebase_uid")
    @SerializedName("firebase_uid")
    val firebaseUid: String? = null,
    
    @ColumnInfo(name = "senha_temporaria")
    @SerializedName("senha_temporaria")
    val senhaTemporaria: String? = null,
    
    @ColumnInfo(name = "senha_hash")
    @SerializedName("senha_hash")
    val senhaHash: String? = null, // Hash da senha pessoal (para login offline)
    
    @ColumnInfo(name = "primeiro_acesso")
    @SerializedName("primeiro_acesso")
    val primeiroAcesso: Boolean = true,
    
    @ColumnInfo(name = "email_acesso")
    @SerializedName("email_acesso")
    val emailAcesso: String? = null,
    
    @ColumnInfo(name = "observacoes")
    @SerializedName("observacoes")
    val observacoes: String? = null,
    
    // Datas
    @ColumnInfo(name = "data_cadastro")
    @SerializedName("data_cadastro")
    val dataCadastro: Date = Date(),
    
    @ColumnInfo(name = "data_ultimo_acesso")
    @SerializedName("data_ultimo_acesso")
    val dataUltimoAcesso: Date? = null,
    
    @ColumnInfo(name = "data_ultima_atualizacao")
    @SerializedName("data_ultima_atualizacao")
    val dataUltimaAtualizacao: Date = Date()
) : Serializable

/**
 * Enum para níveis de acesso dos colaboradores
 */
enum class NivelAcesso {
    ADMIN,  // Acesso total ao sistema
    USER    // Acesso limitado - não pode cadastrar rotas, mesas, colaboradores
}

/**
 * Entidade para metas dos colaboradores
 */
@Entity(tableName = "metas_colaborador")
data class MetaColaborador(
    @PrimaryKey(autoGenerate = true)
    @SerializedName("id")
    val id: Long = 0,
    
    @ColumnInfo(name = "colaborador_id")
    @SerializedName("colaborador_id")
    val colaboradorId: Long,
    
    @ColumnInfo(name = "tipo_meta")
    @SerializedName("tipo_meta")
    val tipoMeta: TipoMeta,
    
    @ColumnInfo(name = "valor_meta")
    @SerializedName("valor_meta")
    val valorMeta: Double,
    
    @ColumnInfo(name = "ciclo_id")
    @SerializedName("ciclo_id")
    val cicloId: Long, // ID do ciclo de acerto
    
    @ColumnInfo(name = "rota_id")
    @SerializedName("rota_id")
    val rotaId: Long? = null, // Rota específica (opcional - null = todas as rotas)
    
    @ColumnInfo(name = "valor_atual")
    @SerializedName("valor_atual")
    val valorAtual: Double = 0.0,
    
    @ColumnInfo(name = "ativo")
    @SerializedName("ativo")
    val ativo: Boolean = true,
    
    @ColumnInfo(name = "data_criacao")
    @SerializedName("data_criacao")
    val dataCriacao: Date = Date()
)

/**
 * Enum para tipos de metas
 */
enum class TipoMeta {
    FATURAMENTO,           // Meta de faturamento em R$
    CLIENTES_ACERTADOS,    // Meta de percentual de clientes acertados (%)
    MESAS_LOCADAS,         // Meta de quantidade de mesas locadas
    TICKET_MEDIO           // Meta de ticket médio por mesa
}

/**
 * Entidade para vinculação de colaboradores com rotas
 */
@Entity(
    tableName = "colaborador_rotas",
    primaryKeys = ["colaborador_id", "rota_id"]
)
data class ColaboradorRota(
    @ColumnInfo(name = "colaborador_id")
    @SerializedName("colaborador_id")
    val colaboradorId: Long,
    
    @ColumnInfo(name = "rota_id")
    @SerializedName("rota_id")
    val rotaId: Long,
    
    @ColumnInfo(name = "responsavel_principal")
    @SerializedName("responsavel_principal")
    val responsavelPrincipal: Boolean = false,
    
    @ColumnInfo(name = "data_vinculacao")
    @SerializedName("data_vinculacao")
    val dataVinculacao: Date = Date()
) 
