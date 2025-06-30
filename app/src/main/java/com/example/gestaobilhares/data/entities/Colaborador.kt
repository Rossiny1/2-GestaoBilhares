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
    
    @ColumnInfo(name = "nome")
    val nome: String,
    
    @ColumnInfo(name = "email")
    val email: String,
    
    @ColumnInfo(name = "telefone")
    val telefone: String? = null,
    
    @ColumnInfo(name = "cpf")
    val cpf: String? = null,
    
    @ColumnInfo(name = "nivel_acesso")
    val nivelAcesso: NivelAcesso = NivelAcesso.USER,
    
    @ColumnInfo(name = "ativo")
    val ativo: Boolean = true,
    
    @ColumnInfo(name = "firebase_uid")
    val firebaseUid: String? = null,
    
    @ColumnInfo(name = "data_cadastro")
    val dataCadastro: Date = Date(),
    
    @ColumnInfo(name = "data_ultimo_acesso")
    val dataUltimoAcesso: Date? = null
)

/**
 * Enum para níveis de acesso dos colaboradores
 */
enum class NivelAcesso {
    ADMIN,  // Acesso total ao sistema
    USER    // Acesso limitado - não pode cadastrar rotas, mesas, colaboradores
} 
