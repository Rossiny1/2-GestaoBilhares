package com.example.gestaobilhares.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entidade que representa um Colaborador no banco de dados.
 * Colaboradores são usuários do sistema com diferentes níveis de acesso.
 */
@Entity(tableName = "colaboradores")
data class Colaborador(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Dados básicos
    @ColumnInfo(name = "nome")
    val nome: String,
    
    @ColumnInfo(name = "email")
    val email: String,
    
    @ColumnInfo(name = "telefone")
    val telefone: String? = null,
    
    @ColumnInfo(name = "cpf")
    val cpf: String? = null,
    
    // Dados pessoais completos
    @ColumnInfo(name = "data_nascimento")
    val dataNascimento: Date? = null,
    
    @ColumnInfo(name = "endereco")
    val endereco: String? = null,
    
    @ColumnInfo(name = "bairro")
    val bairro: String? = null,
    
    @ColumnInfo(name = "cidade")
    val cidade: String? = null,
    
    @ColumnInfo(name = "estado")
    val estado: String? = null,
    
    @ColumnInfo(name = "cep")
    val cep: String? = null,
    
    @ColumnInfo(name = "rg")
    val rg: String? = null,
    
    @ColumnInfo(name = "orgao_emissor")
    val orgaoEmissor: String? = null,
    
    @ColumnInfo(name = "estado_civil")
    val estadoCivil: String? = null,
    
    @ColumnInfo(name = "nome_mae")
    val nomeMae: String? = null,
    
    @ColumnInfo(name = "nome_pai")
    val nomePai: String? = null,
    
    @ColumnInfo(name = "foto_perfil")
    val fotoPerfil: String? = null,
    
    // Sistema de acesso
    @ColumnInfo(name = "nivel_acesso")
    val nivelAcesso: NivelAcesso = NivelAcesso.USER,
    
    @ColumnInfo(name = "ativo")
    val ativo: Boolean = true,
    
    @ColumnInfo(name = "aprovado")
    val aprovado: Boolean = false,
    
    @ColumnInfo(name = "data_aprovacao")
    val dataAprovacao: Date? = null,
    
    @ColumnInfo(name = "aprovado_por")
    val aprovadoPor: String? = null,
    
    // Autenticação
    @ColumnInfo(name = "firebase_uid")
    val firebaseUid: String? = null,
    
    @ColumnInfo(name = "google_id")
    val googleId: String? = null,
    
    @ColumnInfo(name = "senha_temporaria")
    val senhaTemporaria: String? = null,
    
    // Datas
    @ColumnInfo(name = "data_cadastro")
    val dataCadastro: Date = Date(),
    
    @ColumnInfo(name = "data_ultimo_acesso")
    val dataUltimoAcesso: Date? = null,
    
    @ColumnInfo(name = "data_ultima_atualizacao")
    val dataUltimaAtualizacao: Date = Date()
)

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
    val id: Long = 0,
    
    @ColumnInfo(name = "colaborador_id")
    val colaboradorId: Long,
    
    @ColumnInfo(name = "tipo_meta")
    val tipoMeta: TipoMeta,
    
    @ColumnInfo(name = "valor_meta")
    val valorMeta: Double,
    
    @ColumnInfo(name = "periodo_inicio")
    val periodoInicio: Date,
    
    @ColumnInfo(name = "periodo_fim")
    val periodoFim: Date,
    
    @ColumnInfo(name = "valor_atual")
    val valorAtual: Double = 0.0,
    
    @ColumnInfo(name = "ativo")
    val ativo: Boolean = true,
    
    @ColumnInfo(name = "data_criacao")
    val dataCriacao: Date = Date()
)

/**
 * Enum para tipos de metas
 */
enum class TipoMeta {
    FATURAMENTO,           // Meta de faturamento em R$
    CLIENTES_ACERTADOS,    // Meta de quantidade de clientes acertados
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
    val colaboradorId: Long,
    
    @ColumnInfo(name = "rota_id")
    val rotaId: Long,
    
    @ColumnInfo(name = "responsavel_principal")
    val responsavelPrincipal: Boolean = false,
    
    @ColumnInfo(name = "data_vinculacao")
    val dataVinculacao: Date = Date()
) 
