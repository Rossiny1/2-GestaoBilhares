package com.example.gestaobilhares.ui.viewmodel

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data class para representar o estado de uma mesa durante o processo de acerto.
 * Isso desacopla a lógica da UI da entidade do banco de dados.
 */
@Parcelize
data class MesaAcertoState(
    val mesaId: Long,
    var relogioInicial: Int = 0,
    var relogioFinal: Int = 0,
    var valorFixo: Double = 0.0,
    var subtotal: Double = 0.0,
    var comDefeito: Boolean = false
) : Parcelable 