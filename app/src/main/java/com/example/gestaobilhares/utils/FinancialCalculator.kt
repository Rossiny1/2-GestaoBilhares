package com.example.gestaobilhares.utils

import com.example.gestaobilhares.data.entities.Acerto
import com.example.gestaobilhares.data.entities.Despesa
import com.example.gestaobilhares.data.entities.MetaColaborador
import com.example.gestaobilhares.data.entities.TipoMeta
import com.example.gestaobilhares.utils.AppLogger
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

/**
 * ✅ FASE 1: CALCULADORA FINANCEIRA CENTRALIZADA
 * 
 * Centraliza todos os cálculos financeiros do sistema para:
 * - Eliminar duplicação de código (~180 linhas)
 * - Garantir consistência de cálculos
 * - Facilitar manutenção e testes
 * - Padronizar regras de negócio
 */
object FinancialCalculator {
    
    // ==================== CÁLCULOS DE ACERTO ====================
    
    /**
     * Calcula o débito atual baseado nos parâmetros do acerto
     * FÓRMULA: débitoAnterior + valorTotal - desconto - valorRecebido
     */
    fun calcularDebitoAtual(
        debitoAnterior: Double,
        valorTotal: Double,
        desconto: Double,
        valorRecebido: Double
    ): Double {
        return debitoAnterior + valorTotal - desconto - valorRecebido
    }
    
    /**
     * Calcula o valor total das mesas baseado nas fichas jogadas e comissão
     */
    fun calcularValorTotalMesas(
        mesas: List<MesaAcertoCalculo>,
        comissaoFicha: Double
    ): Double {
        return mesas.sumOf { mesa ->
            if (mesa.valorFixo > 0) {
                mesa.valorFixo
            } else {
                val fichasJogadas = (mesa.relogioFinal - mesa.relogioInicial).coerceAtLeast(0)
                fichasJogadas * comissaoFicha
            }
        }
    }
    
    /**
     * Calcula o valor com desconto aplicado
     */
    fun calcularValorComDesconto(
        valorTotal: Double,
        desconto: Double
    ): Double {
        return maxOf(0.0, valorTotal - desconto)
    }
    
    /**
     * Calcula o valor recebido total dos métodos de pagamento
     */
    fun calcularValorRecebido(metodosPagamento: Map<String, Double>): Double {
        return metodosPagamento.values.sum()
    }
    
    // ==================== CÁLCULOS DE CICLO ====================
    
    /**
     * Calcula estatísticas financeiras do ciclo conforme especificação do PDF
     */
    fun calcularEstatisticasCiclo(
        acertos: List<Acerto>,
        despesas: List<Despesa>
    ): EstatisticasCiclo {
        // Calcular valores conforme especificação do PDF
        val totalRecebido = acertos.sumOf { it.valorRecebido }
        val despesasViagem = despesas.filter { it.categoria.equals("Viagem", ignoreCase = true) }.sumOf { it.valor }
        val subtotal = totalRecebido - despesasViagem
        val comissaoMotorista = subtotal * 0.03 // 3% do subtotal
        val comissaoIltair = totalRecebido * 0.02 // 2% do faturamento total

        // Calcular totais por modalidade
        val totaisPorModalidade = calcularTotaisPorModalidade(acertos)
        AppLogger.log("FinancialCalculator", "📊 Totais por modalidade calculados: $totaisPorModalidade")
        val somaPix = totaisPorModalidade["PIX"] ?: 0.0
        val somaCartao = totaisPorModalidade["Cartão"] ?: 0.0
        val totalCheques = totaisPorModalidade["Cheque"] ?: 0.0
        AppLogger.log("FinancialCalculator", "💰 Valores extraídos: PIX=$somaPix, Cartão=$somaCartao, Cheque=$totalCheques")

        // Soma despesas = Total geral das despesas - despesas de viagem
        val totalGeralDespesas = despesas.sumOf { it.valor }
        val somaDespesas = totalGeralDespesas - despesasViagem

        val totalGeral = subtotal - comissaoMotorista - comissaoIltair - somaPix - somaCartao - somaDespesas - totalCheques

        return EstatisticasCiclo(
            totalRecebido = totalRecebido,
            despesasViagem = despesasViagem,
            subtotal = subtotal,
            comissaoMotorista = comissaoMotorista,
            comissaoIltair = comissaoIltair,
            somaPix = somaPix,
            somaCartao = somaCartao,
            somaDespesas = somaDespesas,
            cheques = totalCheques,
            totalGeral = totalGeral
        )
    }
    
    /**
     * Calcula totais por modalidade de pagamento dos acertos
     * ✅ CORREÇÃO: Normaliza nomes dos métodos de pagamento para agrupar corretamente
     */
    private fun calcularTotaisPorModalidade(acertos: List<Acerto>): Map<String, Double> {
        val totais = mutableMapOf<String, Double>()
        
        acertos.forEach { acerto ->
            val metodosPagamento = acerto.metodosPagamentoJson?.let { json ->
                try {
                    Gson().fromJson(json, object : TypeToken<Map<String, Double>>() {}.type) as? Map<String, Double>
                } catch (e: Exception) {
                    AppLogger.log("FinancialCalculator", "Erro ao parsear métodos de pagamento: ${e.message}")
                    emptyMap()
                }
            } ?: emptyMap()
            
            metodosPagamento.forEach { (metodo, valor) ->
                // ✅ CORREÇÃO: Normalizar nome do método para agrupar corretamente
                val metodoNormalizado = normalizarNomeMetodoPagamento(metodo)
                AppLogger.log("FinancialCalculator", "🔄 Método: '$metodo' -> Normalizado: '$metodoNormalizado' -> Valor: R$ $valor")
                totais[metodoNormalizado] = (totais[metodoNormalizado] ?: 0.0) + valor
            }
        }
        
        return totais
    }
    
    /**
     * ✅ CORREÇÃO: Normaliza nome do método de pagamento para garantir agrupamento correto
     * Independente de como está escrito no JSON (PIX, Pix, pix, etc.)
     * Agrupa "Cartão Débito" e "Cartão Crédito" como "Cartão"
     */
    private fun normalizarNomeMetodoPagamento(metodo: String): String {
        val metodoUpper = metodo.trim().uppercase()
        return when {
            metodoUpper.contains("PIX") -> "PIX"
            // ✅ CORREÇÃO: Agrupar "Cartão Débito" e "Cartão Crédito" como "Cartão"
            metodoUpper.contains("CART") || metodoUpper.contains("CARTAO") -> "Cartão"
            metodoUpper.contains("CHEQUE") -> "Cheque"
            metodoUpper.contains("DINHEIRO") || metodoUpper.contains("ESPECIE") -> "Dinheiro"
            else -> {
                // Se não reconhecer, tentar mapear por similaridade
                when {
                    metodoUpper.startsWith("P") -> "PIX"
                    metodoUpper.startsWith("C") -> "Cartão"
                    metodoUpper.startsWith("CH") -> "Cheque"
                    metodoUpper.startsWith("D") || metodoUpper.startsWith("E") -> "Dinheiro"
                    else -> metodo // Manter nome original se não conseguir normalizar
                }
            }
        }
    }
    
    // ==================== CÁLCULOS DE METAS ====================
    
    /**
     * Calcula o progresso de uma meta específica
     */
    fun calcularProgressoMeta(
        meta: MetaColaborador,
        dadosCiclo: DadosCiclo
    ): Double {
        return when (meta.tipoMeta) {
            TipoMeta.FATURAMENTO -> calcularFaturamentoAtual(dadosCiclo)
            TipoMeta.CLIENTES_ACERTADOS -> calcularClientesAcertados(dadosCiclo)
            TipoMeta.MESAS_LOCADAS -> calcularNovasMesasNoCiclo(dadosCiclo)
            TipoMeta.TICKET_MEDIO -> calcularTicketMedio(dadosCiclo)
        }
    }
    
    /**
     * Calcula o faturamento atual do ciclo
     */
    private fun calcularFaturamentoAtual(dadosCiclo: DadosCiclo): Double {
        return dadosCiclo.acertos.sumOf { it.valorRecebido }
    }
    
    /**
     * Calcula o número de clientes acertados no ciclo
     */
    private fun calcularClientesAcertados(dadosCiclo: DadosCiclo): Double {
        return dadosCiclo.acertos.distinctBy { it.clienteId }.size.toDouble()
    }
    
    /**
     * Calcula o número de novas mesas no ciclo
     */
    private fun calcularNovasMesasNoCiclo(@Suppress("UNUSED_PARAMETER") dadosCiclo: DadosCiclo): Double {
        // Implementar lógica específica para novas mesas
        // Por enquanto, retornar 0 - implementar conforme regra de negócio
        return 0.0
    }
    
    /**
     * Calcula o ticket médio do ciclo
     */
    private fun calcularTicketMedio(dadosCiclo: DadosCiclo): Double {
        val acertos = dadosCiclo.acertos
        return if (acertos.isNotEmpty()) {
            acertos.sumOf { it.valorRecebido } / acertos.size
        } else {
            0.0
        }
    }
    
    // ==================== VALIDAÇÕES FINANCEIRAS ====================
    
    /**
     * ✅ FASE 2: Valida se os valores do acerto estão corretos usando DataValidator
     */
    fun validarValoresAcerto(
        debitoAnterior: Double,
        valorTotal: Double,
        desconto: Double,
        valorRecebido: Double
    ): ResultadoValidacao {
        val erros = mutableListOf<String>()
        
        // ✅ FASE 2: Usar DataValidator centralizado
        val validacaoDebitoAnterior = com.example.gestaobilhares.utils.DataValidator.validarValorNaoNegativo(debitoAnterior, "Débito anterior")
        if (validacaoDebitoAnterior.isErro()) {
            erros.addAll((validacaoDebitoAnterior as com.example.gestaobilhares.utils.DataValidator.ResultadoValidacao.Erro).mensagens)
        }
        
        val validacaoValorTotal = com.example.gestaobilhares.utils.DataValidator.validarValorNaoNegativo(valorTotal, "Valor total")
        if (validacaoValorTotal.isErro()) {
            erros.addAll((validacaoValorTotal as com.example.gestaobilhares.utils.DataValidator.ResultadoValidacao.Erro).mensagens)
        }
        
        val validacaoDesconto = com.example.gestaobilhares.utils.DataValidator.validarValorNaoNegativo(desconto, "Desconto")
        if (validacaoDesconto.isErro()) {
            erros.addAll((validacaoDesconto as com.example.gestaobilhares.utils.DataValidator.ResultadoValidacao.Erro).mensagens)
        }
        
        val validacaoValorRecebido = com.example.gestaobilhares.utils.DataValidator.validarValorNaoNegativo(valorRecebido, "Valor recebido")
        if (validacaoValorRecebido.isErro()) {
            erros.addAll((validacaoValorRecebido as com.example.gestaobilhares.utils.DataValidator.ResultadoValidacao.Erro).mensagens)
        }
        
        // Validação específica: desconto não pode ser maior que valor total
        if (desconto > valorTotal) {
            erros.add("Desconto não pode ser maior que o valor total")
        }
        
        // Validação de consistência
        val debitoAtual = calcularDebitoAtual(debitoAnterior, valorTotal, desconto, valorRecebido)
        if (debitoAtual < 0 && Math.abs(debitoAtual) > valorRecebido) {
            erros.add("Valor recebido insuficiente para cobrir o débito")
        }
        
        return if (erros.isEmpty()) {
            ResultadoValidacao.Sucesso
        } else {
            ResultadoValidacao.Erro(erros)
        }
    }
    
    // ==================== CLASSES DE DADOS ====================
    
    /**
     * Dados para cálculo de mesa
     */
    data class MesaAcertoCalculo(
        val relogioInicial: Int,
        val relogioFinal: Int,
        val valorFixo: Double
    )
    
    /**
     * Estatísticas financeiras do ciclo
     */
    data class EstatisticasCiclo(
        val totalRecebido: Double,
        val despesasViagem: Double,
        val subtotal: Double,
        val comissaoMotorista: Double,
        val comissaoIltair: Double,
        val somaPix: Double,
        val somaCartao: Double,
        val somaDespesas: Double,
        val cheques: Double,
        val totalGeral: Double
    )
    
    /**
     * Dados do ciclo para cálculos
     */
    data class DadosCiclo(
        val acertos: List<Acerto>,
        val despesas: List<Despesa>,
        val rotaId: Long,
        val cicloId: Long
    )
    
    /**
     * Resultado de validação
     */
    sealed class ResultadoValidacao {
        object Sucesso : ResultadoValidacao()
        data class Erro(val mensagens: List<String>) : ResultadoValidacao()
    }
}
