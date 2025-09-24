package com.example.gestaobilhares.data.entities

import java.io.Serializable
import java.util.Date

/**
 * Data class para representar o resumo de metas de uma rota
 * Inclui informações do ciclo atual, colaborador responsável e progresso das metas
 */
data class MetaRotaResumo(
    val rota: Rota,
    val cicloAtual: Int,
    val anoCiclo: Int,
    val statusCiclo: StatusCicloAcerto,
    val colaboradorResponsavel: Colaborador?,
    val metas: List<MetaColaborador>,
    val progressoGeral: Double = 0.0, // 0-100%
    val dataInicioCiclo: Date? = null,
    val dataFimCiclo: Date? = null,
    val ultimaAtualizacao: Date = Date()
) : Serializable {
    
    /**
     * Calcula o progresso geral baseado na média ponderada das metas
     */
    fun calcularProgressoGeral(): Double {
        if (metas.isEmpty()) return 0.0
        
        val progressoTotal = metas.sumOf { meta ->
            when (meta.tipoMeta) {
                TipoMeta.FATURAMENTO -> calcularProgressoFaturamento(meta)
                TipoMeta.CLIENTES_ACERTADOS -> calcularProgressoClientesAcertados(meta)
                TipoMeta.MESAS_LOCADAS -> calcularProgressoMesasLocadas(meta)
                TipoMeta.TICKET_MEDIO -> calcularProgressoTicketMedio(meta)
            }
        }
        
        return progressoTotal / metas.size
    }
    
    private fun calcularProgressoFaturamento(meta: MetaColaborador): Double {
        return if (meta.valorMeta > 0) {
            (meta.valorAtual / meta.valorMeta * 100).coerceAtMost(100.0)
        } else 0.0
    }
    
    private fun calcularProgressoClientesAcertados(meta: MetaColaborador): Double {
        return if (meta.valorMeta > 0) {
            (meta.valorAtual / meta.valorMeta * 100).coerceAtMost(100.0)
        } else 0.0
    }
    
    private fun calcularProgressoMesasLocadas(meta: MetaColaborador): Double {
        return if (meta.valorMeta > 0) {
            (meta.valorAtual / meta.valorMeta * 100).coerceAtMost(100.0)
        } else 0.0
    }
    
    private fun calcularProgressoTicketMedio(meta: MetaColaborador): Double {
        return if (meta.valorMeta > 0) {
            (meta.valorAtual / meta.valorMeta * 100).coerceAtMost(100.0)
        } else 0.0
    }
    
    /**
     * Retorna o status formatado do ciclo
     */
    fun getStatusCicloFormatado(): String {
        return when (statusCiclo) {
            StatusCicloAcerto.EM_ANDAMENTO -> "Em Andamento"
            StatusCicloAcerto.FINALIZADO -> "Finalizado"
            StatusCicloAcerto.CANCELADO -> "Cancelado"
        }
    }
    
    /**
     * Retorna o período do ciclo formatado
     */
    fun getPeriodoCicloFormatado(): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("pt", "BR"))
        
        val dataInicioFormatada = if (dataInicioCiclo != null) {
            sdf.format(dataInicioCiclo)
        } else {
            "Data não definida"
        }
        
        val dataFimFormatada = if (dataFimCiclo != null) {
            sdf.format(dataFimCiclo)
        } else {
            "Em andamento"
        }
        
        return "$dataInicioFormatada a $dataFimFormatada"
    }
    
    /**
     * Retorna o nome do colaborador responsável ou "Não definido"
     */
    fun getNomeColaboradorResponsavel(): String {
        return colaboradorResponsavel?.nome ?: "Não definido"
    }
    
    /**
     * Retorna a quantidade de metas ativas
     */
    fun getQuantidadeMetasAtivas(): Int {
        return metas.count { it.ativo }
    }
    
    /**
     * Retorna se há metas próximas de serem atingidas (80% ou mais)
     */
    fun temMetasProximas(): Boolean {
        return metas.any { meta ->
            val progresso = when (meta.tipoMeta) {
                TipoMeta.FATURAMENTO -> calcularProgressoFaturamento(meta)
                TipoMeta.CLIENTES_ACERTADOS -> calcularProgressoClientesAcertados(meta)
                TipoMeta.MESAS_LOCADAS -> calcularProgressoMesasLocadas(meta)
                TipoMeta.TICKET_MEDIO -> calcularProgressoTicketMedio(meta)
            }
            progresso >= 80.0 && progresso < 100.0
        }
    }
    
    /**
     * Retorna se há metas atingidas (100%)
     */
    fun temMetasAtingidas(): Boolean {
        return metas.any { meta ->
            val progresso = when (meta.tipoMeta) {
                TipoMeta.FATURAMENTO -> calcularProgressoFaturamento(meta)
                TipoMeta.CLIENTES_ACERTADOS -> calcularProgressoClientesAcertados(meta)
                TipoMeta.MESAS_LOCADAS -> calcularProgressoMesasLocadas(meta)
                TipoMeta.TICKET_MEDIO -> calcularProgressoTicketMedio(meta)
            }
            progresso >= 100.0
        }
    }
}
