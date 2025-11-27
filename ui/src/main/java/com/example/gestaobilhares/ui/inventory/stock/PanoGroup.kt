package com.example.gestaobilhares.ui.inventory.stock

import android.os.Parcelable
import com.example.gestaobilhares.data.entities.PanoEstoque
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

/**
 * Representa um grupo de panos com as mesmas características
 */
@Parcelize
data class PanoGroup(
    val cor: String,
    val tamanho: String,
    val material: String,
    val panos: @RawValue List<PanoEstoque>,
    val quantidadeDisponivel: Int,
    val quantidadeTotal: Int
) : Parcelable {
    val numeroInicial: String get() = panos.minByOrNull { it.numero }?.numero ?: ""
    val numeroFinal: String get() = panos.maxByOrNull { it.numero }?.numero ?: ""
    val observacoes: String? get() = panos.firstOrNull()?.observacoes
}

