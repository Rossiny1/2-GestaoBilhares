package com.example.gestaobilhares.ui.settlement

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MesaDTO(
    val id: Long,
    val numero: String,
    val fichasInicial: Int,
    val fichasFinal: Int,
    val tipoMesa: String,
    val ativa: Boolean
) : Parcelable 