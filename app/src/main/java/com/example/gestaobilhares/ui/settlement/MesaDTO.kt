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
    val fichasInicial: Int,
    val fichasFinal: Int,
    val tipoMesa: String,
    val tamanho: String = "GRANDE",
    val estadoConservacao: String = "BOM",
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
            fichasInicial = fichasInicial,
            fichasFinal = fichasFinal,
            valorFixo = valorFixo,
            tipoMesa = when (tipoMesa) {
                "SINUCA" -> TipoMesa.SINUCA
                "MAQUINA_MUSICA" -> TipoMesa.MAQUINA_MUSICA
                "PEMBOLIM" -> TipoMesa.PEMBOLIM
                "SNOOKER" -> TipoMesa.SNOOKER
                "POOL" -> TipoMesa.POOL
                "BILHAR" -> TipoMesa.BILHAR
                "OUTROS" -> TipoMesa.OUTROS
                else -> TipoMesa.SINUCA
            },
            tamanho = when (tamanho) {
                "PEQUENA" -> TamanhoMesa.PEQUENA
                "GRANDE" -> TamanhoMesa.GRANDE
                else -> TamanhoMesa.GRANDE
            },
            estadoConservacao = when (estadoConservacao) {
                "OTIMO" -> EstadoConservacao.OTIMO
                "BOM" -> EstadoConservacao.BOM
                "RUIM" -> EstadoConservacao.RUIM
                else -> EstadoConservacao.BOM
            }
        )
    }
} 