package com.example.gestaobilhares.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "veiculos")
data class Veiculo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String = "",
    val placa: String = "",
    val marca: String,
    val modelo: String,
    val anoModelo: Int,
    val kmAtual: Long = 0,
    val dataCompra: Long? = null,
    val observacoes: String? = null
)


