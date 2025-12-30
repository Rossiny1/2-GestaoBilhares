package com.example.gestaobilhares.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidade que representa uma Mesa Vendida no banco de dados.
 * Armazena informações sobre mesas que foram vendidas e removidas do depósito.
 */
@Entity(tableName = "mesas_vendidas")
data class MesaVendida(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "mesa_id_original")
    val mesaIdOriginal: Long, // ID da mesa original que foi vendida
    
    @ColumnInfo(name = "numero_mesa")
    val numeroMesa: String, // Número da mesa vendida
    
    @ColumnInfo(name = "tipo_mesa")
    val tipoMesa: TipoMesa, // Tipo da mesa (Sinuca, Pembolim, etc.)
    
    @ColumnInfo(name = "tamanho_mesa")
    val tamanhoMesa: TamanhoMesa, // Tamanho da mesa
    
    @ColumnInfo(name = "estado_conservacao")
    val estadoConservacao: EstadoConservacao, // Estado de conservação
    
    @ColumnInfo(name = "nome_comprador")
    val nomeComprador: String, // Nome do comprador
    
    @ColumnInfo(name = "telefone_comprador")
    val telefoneComprador: String?, // Telefone do comprador (opcional)
    
    @ColumnInfo(name = "cpf_cnpj_comprador")
    val cpfCnpjComprador: String?, // CPF/CNPJ do comprador (opcional)

    @ColumnInfo(name = "endereco_comprador")
    val enderecoComprador: String?, // Endereço do comprador (opcional)
    
    @ColumnInfo(name = "valor_venda")
    val valorVenda: Double, // Valor da venda
    
    @ColumnInfo(name = "data_venda")
    val dataVenda: Long, // Data da venda
    
    @ColumnInfo(name = "observacoes")
    val observacoes: String?, // Observações sobre a venda
    
    @ColumnInfo(name = "data_criacao")
    val dataCriacao: Long = System.currentTimeMillis() // Data de criação do registro
)
