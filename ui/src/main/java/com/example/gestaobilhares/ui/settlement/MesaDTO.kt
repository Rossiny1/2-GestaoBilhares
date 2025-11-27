package com.example.gestaobilhares.ui.settlement

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.entities.TipoMesa
import com.example.gestaobilhares.data.entities.TamanhoMesa
import com.example.gestaobilhares.data.entities.EstadoConservacao

@Parcelize
data class MesaDTO(
    val id: Long,
    val numero: String,
    val relogioInicial: Int,
    val relogioFinal: Int,
    val tipoMesa: TipoMesa,
    val tamanho: TamanhoMesa = TamanhoMesa.GRANDE,
    val estadoConservacao: EstadoConservacao = EstadoConservacao.BOM,
    val ativa: Boolean,
    val valorFixo: Double = 0.0,
    val valorFicha: Double = 0.0,
    val comissaoFicha: Double = 0.0
) : Parcelable {
    
    /**
     * Converte MesaDTO para Mesa
     */
    fun toMesa(): Mesa {
        return Mesa(
            id = id,
            numero = numero,
            relogioInicial = relogioInicial,
            relogioFinal = relogioFinal,
            valorFixo = valorFixo,
            tipoMesa = tipoMesa,
            tamanho = tamanho,
            estadoConservacao = estadoConservacao
        )
    }
} 
