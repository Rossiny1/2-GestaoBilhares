package com.example.gestaobilhares.ui.mesas

import com.example.gestaobilhares.data.entities.HistoricoManutencaoMesa
import com.example.gestaobilhares.data.entities.MesaReformada
import java.io.Serializable
import java.util.Date

/**
 * ✅ NOVO: Data class para agrupar reformas por mesa com histórico.
 * Movida para arquivo próprio para ser usada em navegação.
 */
data class MesaReformadaComHistorico(
    val numeroMesa: String,
    val mesaId: Long,
    val tipoMesa: String,
    val tamanhoMesa: String,
    val reformas: List<MesaReformada>,
    val historicoManutencoes: List<HistoricoManutencaoMesa>
) : Serializable {
    // Data da última reforma
    val dataUltimaReforma = reformas.maxByOrNull { it.dataReforma.time }?.dataReforma
    
    // ✅ NOVO: Data do último evento (reforma ou manutenção)
    val dataUltimoEvento = listOfNotNull(
        dataUltimaReforma,
        historicoManutencoes.maxByOrNull { it.dataManutencao.time }?.dataManutencao
    ).maxByOrNull { it.time }

    // Total de reformas
    val totalReformas = reformas.size

    /**
     * ✅ NOVO: Obtém o número do último pano trocado.
     * Busca na lista de reformas a mais recente que possui número do pano preenchido.
     */
    val numeroUltimoPano: String
        get() = reformas.sortedByDescending { it.dataReforma.time }
            .firstOrNull { it.panos && !it.numeroPanos.isNullOrBlank() }
            ?.numeroPanos ?: "Não informado"
}
