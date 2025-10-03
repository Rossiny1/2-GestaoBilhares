package com.example.gestaobilhares.utils

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import com.example.gestaobilhares.R
import com.example.gestaobilhares.data.entities.AcertoMesa
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.entities.TipoMesa
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Helper para preencher o layout de recibo de impressão de forma consistente
 * entre SettlementSummaryDialog e SettlementDetailFragment
 */
object ReciboPrinterHelper {
    
    /**
     * Preenche o layout de recibo com os dados fornecidos (versão com informações completas das mesas)
     * @param context Contexto da aplicação
     * @param reciboView View do layout de recibo já inflado
     * @param clienteNome Nome do cliente
     * @param clienteCpf CPF do cliente (opcional)
     * @param mesasCompletas Lista de mesas completas com informações de tipo
     * @param debitoAnterior Débito anterior do cliente
     * @param valorTotalMesas Valor total das mesas
     * @param desconto Desconto aplicado
     * @param metodosPagamento Métodos de pagamento utilizados
     * @param debitoAtual Débito atual após o acerto
     * @param observacao Observações do acerto
     * @param valorFicha Valor da ficha do cliente
     * @param acertoId ID do acerto (opcional, para títulos)
     */
    fun preencherReciboImpressaoCompleto(
        context: Context,
        reciboView: View,
        clienteNome: String,
        clienteCpf: String? = null,
        mesasCompletas: List<Mesa>,
        debitoAnterior: Double,
        valorTotalMesas: Double,
        desconto: Double,
        metodosPagamento: Map<String, Double>,
        debitoAtual: Double,
        observacao: String?,
        valorFicha: Double,
        acertoId: Long? = null,
        numeroContrato: String? = null
    ) {
        // Referências dos elementos
        val txtTitulo = reciboView.findViewById<android.widget.TextView>(R.id.txtTituloRecibo)
        val txtClienteValor = reciboView.findViewById<android.widget.TextView>(R.id.txtClienteValor)
        val rowCpfCliente = reciboView.findViewById<android.widget.LinearLayout>(R.id.rowCpfCliente)
        val txtCpfCliente = reciboView.findViewById<android.widget.TextView>(R.id.txtCpfCliente)
        val rowNumeroContrato = reciboView.findViewById<android.widget.LinearLayout>(R.id.rowNumeroContrato)
        val txtNumeroContrato = reciboView.findViewById<android.widget.TextView>(R.id.txtNumeroContrato)
        val txtData = reciboView.findViewById<android.widget.TextView>(R.id.txtData)
        val rowValorFicha = reciboView.findViewById<android.widget.LinearLayout>(R.id.rowValorFicha)
        val txtValorFicha = reciboView.findViewById<android.widget.TextView>(R.id.txtValorFicha)
        val txtMesas = reciboView.findViewById<android.widget.TextView>(R.id.txtMesas)
        val txtFichasJogadas = reciboView.findViewById<android.widget.TextView>(R.id.txtFichasJogadas)
        val txtDebitoAnterior = reciboView.findViewById<android.widget.TextView>(R.id.txtDebitoAnterior)
        val txtSubtotalMesas = reciboView.findViewById<android.widget.TextView>(R.id.txtSubtotalMesas)
        val txtTotal = reciboView.findViewById<android.widget.TextView>(R.id.txtTotal)
        val txtDesconto = reciboView.findViewById<android.widget.TextView>(R.id.txtDesconto)
        val txtValorRecebido = reciboView.findViewById<android.widget.TextView>(R.id.txtValorRecebido)
        val txtDebitoAtual = reciboView.findViewById<android.widget.TextView>(R.id.txtDebitoAtual)
        val txtPagamentos = reciboView.findViewById<android.widget.TextView>(R.id.txtPagamentos)
        val txtObservacoes = reciboView.findViewById<android.widget.TextView>(R.id.txtObservacoes)

        // Formatação
        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val dataFormatada = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        // Título
        val titulo = if (acertoId != null) {
            "RECIBO DE ACERTO #${acertoId.toString().padStart(4, '0')}"
        } else {
            "RECIBO DE ACERTO"
        }
        txtTitulo.text = titulo

        // Cliente e CPF
        txtClienteValor.text = clienteNome
        
        // CPF do cliente
        if (!clienteCpf.isNullOrBlank()) {
            txtCpfCliente.text = clienteCpf
            rowCpfCliente.visibility = View.VISIBLE
        } else {
            rowCpfCliente.visibility = View.GONE
        }
        
        // Número do contrato
        if (!numeroContrato.isNullOrBlank()) {
            txtNumeroContrato.text = numeroContrato
            rowNumeroContrato.visibility = View.VISIBLE
        } else {
            rowNumeroContrato.visibility = View.GONE
        }
        
        // Data
        txtData.text = "Data: $dataFormatada"

        // Valor da ficha
        if (valorFicha > 0) {
            txtValorFicha.text = formatter.format(valorFicha)
            rowValorFicha.visibility = View.VISIBLE
        } else {
            rowValorFicha.visibility = View.GONE
        }

        // Mesas (formatação com tipo do equipamento e número real da mesa)
        val mesasFormatadas = StringBuilder()
        mesasCompletas.forEachIndexed { index, mesa ->
            val fichasJogadas = mesa.fichasFinal - mesa.fichasInicial
            val tipoEquipamento = getTipoEquipamentoNome(mesa.tipoMesa)
            // ✅ CORREÇÃO: Usar número real da mesa, não índice
            mesasFormatadas.append("$tipoEquipamento ${mesa.numero}\n${mesa.fichasInicial} → ${mesa.fichasFinal} (${fichasJogadas} fichas)")
            if (index < mesasCompletas.size - 1) mesasFormatadas.append("\n")
        }
        txtMesas.text = mesasFormatadas.toString()

        // Fichas jogadas
        val totalFichasJogadas = mesasCompletas.sumOf { it.fichasFinal - it.fichasInicial }
        txtFichasJogadas.text = totalFichasJogadas.toString()

        // Resumo Financeiro (sem duplicação e com rótulos únicos)
        txtDebitoAnterior.text = formatter.format(debitoAnterior)
        txtSubtotalMesas.text = formatter.format(valorTotalMesas)
        val valorTotal = valorTotalMesas + debitoAnterior
        txtTotal.text = formatter.format(valorTotal)
        txtDesconto.text = formatter.format(desconto)
        val valorRecebidoSum = metodosPagamento.values.sum()
        txtValorRecebido.text = formatter.format(valorRecebidoSum)
        txtDebitoAtual.text = formatter.format(debitoAtual)

        // Forma de pagamento (formatação limpa)
        val pagamentosFormatados = if (metodosPagamento.isNotEmpty()) {
            metodosPagamento.entries.joinToString("\n") { "${it.key}: ${formatter.format(it.value)}" }
        } else {
            "Não informado"
        }
        txtPagamentos.text = pagamentosFormatados

        // Observações
        if (observacao.isNullOrBlank()) {
            txtObservacoes.text = "-"
        } else {
            txtObservacoes.text = observacao
        }

        // Logo
        val imgLogo = reciboView.findViewById<android.widget.ImageView>(R.id.imgLogoRecibo)
        imgLogo.setImageResource(R.drawable.logo_globo1)

        // Ajustar estilos para títulos e valores principais
        txtTitulo.setTypeface(null, Typeface.BOLD)
        txtClienteValor.setTypeface(null, Typeface.BOLD)
        txtMesas.setTypeface(null, Typeface.BOLD)
        txtPagamentos.setTypeface(null, Typeface.BOLD)
        txtObservacoes.setTypeface(null, Typeface.BOLD)
    }
    
    /**
     * ✅ NOVA FUNÇÃO: Gera texto para WhatsApp usando a mesma lógica do recibo impresso
     * FONTE ÚNICA DE VERDADE - Mesmo conteúdo do recibo impresso
     */
    fun gerarTextoWhatsApp(
        clienteNome: String,
        clienteCpf: String? = null,
        mesasCompletas: List<Mesa>,
        debitoAnterior: Double,
        valorTotalMesas: Double,
        desconto: Double,
        metodosPagamento: Map<String, Double>,
        debitoAtual: Double,
        observacao: String?,
        valorFicha: Double,
        acertoId: Long? = null,
        numeroContrato: String? = null
    ): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val dataAtual = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val texto = StringBuilder()
        
        // ✅ TÍTULO (mesmo do recibo impresso)
        val titulo = if (acertoId != null) {
            "🎱 *RECIBO DE ACERTO #${acertoId.toString().padStart(4, '0')}*"
        } else {
            "🎱 *RECIBO DE ACERTO*"
        }
        texto.append("$titulo\n")
        texto.append("================================\n\n")
        
        // ✅ CLIENTE E CPF (mesmo do recibo impresso)
        texto.append("👤 *Cliente:* $clienteNome\n")
        if (!clienteCpf.isNullOrBlank()) {
            texto.append("📄 *CPF:* $clienteCpf\n")
        }
        if (!numeroContrato.isNullOrBlank()) {
            texto.append("📋 *Contrato:* $numeroContrato\n")
        }
        texto.append("📅 *Data:* $dataAtual\n\n")
        
        // ✅ VALOR DA FICHA (mesmo do recibo impresso)
        if (valorFicha > 0) {
            texto.append("💰 *Preço da ficha:* ${formatter.format(valorFicha)}\n\n")
        }
        
        // ✅ MESAS (mesmo do recibo impresso - número real da mesa)
        texto.append("🎯 *MESAS ACERTADAS:*\n")
        var totalFichasJogadas = 0
        mesasCompletas.forEach { mesa ->
            val fichasJogadas = mesa.fichasFinal - mesa.fichasInicial
            totalFichasJogadas += fichasJogadas
            val tipoEquipamento = getTipoEquipamentoNome(mesa.tipoMesa)
            // ✅ CORREÇÃO: Usar número real da mesa, não índice
            texto.append("• *$tipoEquipamento ${mesa.numero}*: ${mesa.fichasInicial} → ${mesa.fichasFinal} (${fichasJogadas} fichas)\n")
        }
        if (totalFichasJogadas > 0) {
            texto.append("\n*Total de fichas jogadas: $totalFichasJogadas*\n\n")
        }
        
        // ✅ RESUMO FINANCEIRO (mesmo do recibo impresso)
        texto.append("💰 *RESUMO FINANCEIRO:*\n")
        if (debitoAnterior > 0) {
            texto.append("• Débito anterior: ${formatter.format(debitoAnterior)}\n")
        }
        texto.append("• Total das mesas: ${formatter.format(valorTotalMesas)}\n")
        if (valorFicha > 0) {
            texto.append("• Valor da ficha: ${formatter.format(valorFicha)}\n")
        }
        val valorTotal = valorTotalMesas + debitoAnterior
        texto.append("• Valor total: ${formatter.format(valorTotal)}\n")
        if (desconto > 0) {
            texto.append("• Desconto: ${formatter.format(desconto)}\n")
        }
        if (metodosPagamento.isNotEmpty()) {
            val valorRecebido = metodosPagamento.values.sum()
            texto.append("• Valor recebido: ${formatter.format(valorRecebido)}\n")
        }
        if (debitoAtual > 0) {
            texto.append("• Débito atual: ${formatter.format(debitoAtual)}\n")
        }
        texto.append("\n")
        
        // ✅ FORMA DE PAGAMENTO (mesmo do recibo impresso)
        if (metodosPagamento.isNotEmpty()) {
            texto.append("💳 *FORMA DE PAGAMENTO:*\n")
            metodosPagamento.forEach { (metodo, valor) ->
                texto.append("• $metodo: ${formatter.format(valor)}\n")
            }
            texto.append("\n")
        }
        
        // ✅ OBSERVAÇÕES (mesmo do recibo impresso)
        if (!observacao.isNullOrBlank()) {
            texto.append("📝 *Observações:* $observacao\n\n")
        }
        
        texto.append("--------------------------------\n")
        texto.append("✅ Acerto realizado via GestaoBilhares")
        return texto.toString()
    }

    /**
     * Preenche o layout de recibo com os dados fornecidos (versão compatível com AcertoMesa)
     * @param context Contexto da aplicação
     * @param reciboView View do layout de recibo já inflado
     * @param clienteNome Nome do cliente
     * @param mesas Lista de mesas do acerto
     * @param debitoAnterior Débito anterior do cliente
     * @param valorTotalMesas Valor total das mesas
     * @param desconto Desconto aplicado
     * @param metodosPagamento Métodos de pagamento utilizados
     * @param debitoAtual Débito atual após o acerto
     * @param observacao Observações do acerto
     * @param valorFicha Valor da ficha do cliente
     * @param acertoId ID do acerto (opcional, para títulos)
     */
    fun preencherReciboImpressao(
        context: Context,
        reciboView: View,
        clienteNome: String,
        mesas: List<AcertoMesa>,
        debitoAnterior: Double,
        valorTotalMesas: Double,
        desconto: Double,
        metodosPagamento: Map<String, Double>,
        debitoAtual: Double,
        observacao: String?,
        valorFicha: Double,
        acertoId: Long? = null
    ) {
        // Referências dos elementos
        val txtTitulo = reciboView.findViewById<android.widget.TextView>(R.id.txtTituloRecibo)
        val txtClienteValor = reciboView.findViewById<android.widget.TextView>(R.id.txtClienteValor)
        val txtData = reciboView.findViewById<android.widget.TextView>(R.id.txtData)
        val rowValorFicha = reciboView.findViewById<android.widget.LinearLayout>(R.id.rowValorFicha)
        val txtValorFicha = reciboView.findViewById<android.widget.TextView>(R.id.txtValorFicha)
        val txtMesas = reciboView.findViewById<android.widget.TextView>(R.id.txtMesas)
        val txtFichasJogadas = reciboView.findViewById<android.widget.TextView>(R.id.txtFichasJogadas)
        val txtDebitoAnterior = reciboView.findViewById<android.widget.TextView>(R.id.txtDebitoAnterior)
        val txtSubtotalMesas = reciboView.findViewById<android.widget.TextView>(R.id.txtSubtotalMesas)
        val txtTotal = reciboView.findViewById<android.widget.TextView>(R.id.txtTotal)
        val txtDesconto = reciboView.findViewById<android.widget.TextView>(R.id.txtDesconto)
        val txtValorRecebido = reciboView.findViewById<android.widget.TextView>(R.id.txtValorRecebido)
        val txtDebitoAtual = reciboView.findViewById<android.widget.TextView>(R.id.txtDebitoAtual)
        val txtPagamentos = reciboView.findViewById<android.widget.TextView>(R.id.txtPagamentos)
        val txtObservacoes = reciboView.findViewById<android.widget.TextView>(R.id.txtObservacoes)

        // Formatação
        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val dataFormatada = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        // Título
        val titulo = if (acertoId != null) {
            "RECIBO DE ACERTO #${acertoId.toString().padStart(4, '0')}"
        } else {
            "RECIBO DE ACERTO"
        }
        txtTitulo.text = titulo

        // Cliente e data
        txtClienteValor.text = clienteNome
        txtData.text = "Data: $dataFormatada"

        // Valor da ficha
        if (valorFicha > 0) {
            txtValorFicha.text = formatter.format(valorFicha)
            rowValorFicha.visibility = View.VISIBLE
        } else {
            rowValorFicha.visibility = View.GONE
        }

        // Mesas (formatação limpa sem quebras extras)
        val mesasFormatadas = StringBuilder()
        mesas.forEachIndexed { index, mesa ->
            val fichasJogadas = mesa.fichasJogadas
            // ✅ CORREÇÃO: Usar número real da mesa, não mesaId
            mesasFormatadas.append("Mesa ${index + 1}\n${mesa.relogioInicial} → ${mesa.relogioFinal} (${fichasJogadas} fichas)")
            if (index < mesas.size - 1) mesasFormatadas.append("\n")
        }
        txtMesas.text = mesasFormatadas.toString()

        // Fichas jogadas
        val totalFichasJogadas = mesas.sumOf { it.fichasJogadas }
        txtFichasJogadas.text = totalFichasJogadas.toString()

        // Resumo Financeiro (sem duplicação e com rótulos únicos)
        txtDebitoAnterior.text = formatter.format(debitoAnterior)
        txtSubtotalMesas.text = formatter.format(valorTotalMesas)
        val valorTotal = valorTotalMesas + debitoAnterior
        txtTotal.text = formatter.format(valorTotal)
        txtDesconto.text = formatter.format(desconto)
        val valorRecebidoSum = metodosPagamento.values.sum()
        txtValorRecebido.text = formatter.format(valorRecebidoSum)
        txtDebitoAtual.text = formatter.format(debitoAtual)

        // Forma de pagamento (formatação limpa)
        val pagamentosFormatados = if (metodosPagamento.isNotEmpty()) {
            metodosPagamento.entries.joinToString("\n") { "${it.key}: ${formatter.format(it.value)}" }
        } else {
            "Não informado"
        }
        txtPagamentos.text = pagamentosFormatados

        // Observações
        if (observacao.isNullOrBlank()) {
            txtObservacoes.text = "-"
        } else {
            txtObservacoes.text = observacao
        }

        // Logo
        val imgLogo = reciboView.findViewById<android.widget.ImageView>(R.id.imgLogoRecibo)
        imgLogo.setImageResource(R.drawable.logo_globo1)

        // Ajustar estilos para títulos e valores principais
        txtTitulo.setTypeface(null, Typeface.BOLD)
        txtClienteValor.setTypeface(null, Typeface.BOLD)
        txtMesas.setTypeface(null, Typeface.BOLD)
        txtPagamentos.setTypeface(null, Typeface.BOLD)
        txtObservacoes.setTypeface(null, Typeface.BOLD)
    }

    /**
     * Retorna o nome do tipo do equipamento para exibição
     */
    private fun getTipoEquipamentoNome(tipoMesa: TipoMesa): String {
        return when (tipoMesa) {
            TipoMesa.SINUCA -> "Sinuca"
            TipoMesa.PEMBOLIM -> "Pembolim"
            TipoMesa.JUKEBOX -> "Jukebox"
            TipoMesa.OUTROS -> "Equipamento"
        }
    }
}
