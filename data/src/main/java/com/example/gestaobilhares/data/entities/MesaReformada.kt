package com.example.gestaobilhares.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * Entidade que representa uma Mesa Reformada no banco de dados.
 * Armazena informações sobre mesas que foram reformadas e o que foi feito.
 */
@Entity(tableName = "mesas_reformadas")
data class MesaReformada(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "mesa_id")
    val mesaId: Long, // ID da mesa que foi reformada
    
    @ColumnInfo(name = "numero_mesa")
    val numeroMesa: String, // Número da mesa reformada
    
    @ColumnInfo(name = "tipo_mesa")
    val tipoMesa: TipoMesa, // Tipo da mesa
    
    @ColumnInfo(name = "tamanho_mesa")
    val tamanhoMesa: TamanhoMesa, // Tamanho da mesa
    
    @ColumnInfo(name = "pintura")
    val pintura: Boolean = false, // Se foi feita pintura
    
    @ColumnInfo(name = "tabela")
    val tabela: Boolean = false, // Se foi reformada a tabela
    
    @ColumnInfo(name = "panos")
    val panos: Boolean = false, // Se foram trocados os panos
    
    @ColumnInfo(name = "numero_panos")
    val numeroPanos: String?, // Números dos panos utilizados (se aplicável)
    
    @ColumnInfo(name = "outros")
    val outros: Boolean = false, // Se foram feitas outras reformas
    
    @ColumnInfo(name = "observacoes")
    val observacoes: String?, // Observações sobre a reforma
    
    @ColumnInfo(name = "foto_reforma")
    val fotoReforma: String?, // Caminho da foto da mesa reformada
    
    @ColumnInfo(name = "data_reforma")
    val dataReforma: Long, // Data da reforma
    
    @ColumnInfo(name = "data_criacao")
    val dataCriacao: Long = System.currentTimeMillis() // Data de criação do registro
) : Serializable
