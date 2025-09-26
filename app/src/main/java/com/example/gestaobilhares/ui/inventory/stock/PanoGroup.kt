package com.example.gestaobilhares.ui.inventory.stock

import com.example.gestaobilhares.data.entities.PanoEstoque

/**
 * Representa um grupo de panos com as mesmas características
 */
data class PanoGroup(
    val cor: String,
    val tamanho: String,
    val material: String,
    val panos: List<PanoEstoque>,
    val quantidadeDisponivel: Int,
    val quantidadeTotal: Int
) {
    val numeroInicial: String get() = panos.minByOrNull { it.numero }?.numero ?: ""
    val numeroFinal: String get() = panos.maxByOrNull { it.numero }?.numero ?: ""
    val observacoes: String? get() = panos.firstOrNull()?.observacoes
}
