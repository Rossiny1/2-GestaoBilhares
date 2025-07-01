package com.example.gestaobilhares.ui.settlement

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.entities.TipoMesa

@Parcelize
data class MesaDTO(
    val id: Long,
    val numero: String,
    val fichasInicial: Int,
    val fichasFinal: Int,
    val tipoMesa: String,
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
                "SNOOKER" -> TipoMesa.SNOOKER
                "POOL" -> TipoMesa.POOL
                "BILHAR" -> TipoMesa.BILHAR
                else -> TipoMesa.SINUCA
            }
        )
    }
} 