package com.example.gestaobilhares.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import com.example.gestaobilhares.R
import com.example.gestaobilhares.data.entities.*
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.*
import com.itextpdf.layout.property.HorizontalAlignment
import com.itextpdf.layout.property.TextAlignment
import com.itextpdf.layout.property.UnitValue
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Gerador de relatórios PDF para ciclos de acerto
 * ✅ FASE 9C: RELATÓRIOS DETALHADOS EM PDF - VERSÃO MELHORADA
 */
class PdfReportGenerator(private val context: Context) {

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
    private val dateOnlyFormatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    private val localDateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    /**
     * Gera relatório detalhado de um ciclo de acerto
     */
    fun generateCycleReport(
        ciclo: CicloAcertoEntity,
        rota: Rota,
        acertos: List<Acerto>,
        despesas: List<Despesa>,
        clientes: List<Cliente>
    ): File {
        val fileName = "relatorio_ciclo_${ciclo.ano}_${ciclo.numeroCiclo}_${System.currentTimeMillis()}.pdf"
        val file = File(context.getExternalFilesDir(null), fileName)
        
        try {
            val writer = PdfWriter(FileOutputStream(file))
            val pdf = PdfDocument(writer)
            val document = Document(pdf)
            
            // Configurar margens para A4
            document.setMargins(50f, 50f, 50f, 50f)
            
            // Adicionar cabeçalho
            addHeader(document, rota, ciclo)
            
            // Adicionar lista de recebimentos (MELHORADA)
            addReceiptsList(document, acertos, clientes)
            
            // ✅ NOVO: Adicionar resumo por modalidade de pagamento
            addPaymentMethodsSummary(document, acertos)
            
            // Adicionar lista de despesas
            addExpensesList(document, despesas)
            
            // Adicionar resumo final (COMPLETAMENTE REFORMULADO)
            addEnhancedFinalSummary(document, ciclo, acertos, despesas)
            
            document.close()
            
            Log.d("PdfReportGenerator", "Relatório gerado com sucesso: ${file.absolutePath}")
            return file
            
        } catch (e: Exception) {
            Log.e("PdfReportGenerator", "Erro ao gerar relatório", e)
            throw e
        }
    }

    /**
     * Adiciona cabeçalho com logo e informações da rota
     */
    private fun addHeader(document: Document, rota: Rota, ciclo: CicloAcertoEntity) {
        try {
            // Tabela para organizar logo e título na mesma linha
            val headerTable = Table(2)
                .setWidth(UnitValue.createPercentValue(100f))
                .setMarginBottom(20f)
            
            // Célula do logo
            val logoCell = Cell()
            try {
                val logoStream = context.resources.openRawResource(R.drawable.logo_globo1)
                val logoBytes = logoStream.readBytes()
                logoStream.close()
                
                val logo = Image(ImageDataFactory.create(logoBytes))
                logo.setWidth(80f)
                logoCell.add(logo)
            } catch (e: Exception) {
                logoCell.add(Paragraph("LOGO"))
            }
            logoCell.setBorder(null)
            headerTable.addCell(logoCell)
            
            // Célula do título
            val titleCell = Cell()
            titleCell.add(
                Paragraph("RELATÓRIO DE FECHAMENTO")
                    .setFontSize(16f)
                    .setBold()
                    .setTextAlignment(TextAlignment.RIGHT)
            )
            titleCell.add(
                Paragraph("${rota.nome} - Ciclo ${ciclo.ano} #${ciclo.numeroCiclo}")
                    .setFontSize(12f)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setMarginTop(5f)
            )
            titleCell.setBorder(null)
            headerTable.addCell(titleCell)
            
            document.add(headerTable)
            
            // Informações da rota
            val routeInfo = Table(2)
                .setWidth(UnitValue.createPercentValue(100f))
                .setMarginTop(20f)
                .setMarginBottom(20f)
            
            routeInfo.addCell(createCell("ROTA:", true))
            routeInfo.addCell(createCell(rota.nome, false))
            routeInfo.addCell(createCell("CICLO:", true))
            routeInfo.addCell(createCell("${ciclo.ano} - #${ciclo.numeroCiclo}", false))
            routeInfo.addCell(createCell("DATA INÍCIO:", true))
            routeInfo.addCell(createCell(dateFormatter.format(ciclo.dataInicio), false))
            routeInfo.addCell(createCell("DATA FIM:", true))
            routeInfo.addCell(createCell(ciclo.dataFim?.let { dateFormatter.format(it) } ?: "Em andamento", false))
            
            document.add(routeInfo)
            
        } catch (e: Exception) {
            Log.e("PdfReportGenerator", "Erro ao adicionar cabeçalho", e)
            // Continuar sem logo se houver erro
            val title = Paragraph("RELATÓRIO DETALHADO DE FECHAMENTO")
                .setFontSize(20f)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(20f)
                .setMarginBottom(20f)
            document.add(title)
        }
    }

    /**
     * Adiciona resumo executivo
     */
    private fun addExecutiveSummary(document: Document, ciclo: CicloAcertoEntity) {
        val summaryTitle = Paragraph("RESUMO EXECUTIVO")
            .setFontSize(16f)
            .setBold()
            .setMarginTop(30f)
            .setMarginBottom(15f)
        document.add(summaryTitle)
        
        val summaryTable = Table(2)
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginBottom(20f)
        
        summaryTable.addCell(createCell("CLIENTES ACERTADOS:", true))
        summaryTable.addCell(createCell("${ciclo.clientesAcertados}/${ciclo.totalClientes}", false))
        summaryTable.addCell(createCell("FATURAMENTO TOTAL:", true))
        summaryTable.addCell(createCell(currencyFormatter.format(ciclo.valorTotalAcertado), false))
        summaryTable.addCell(createCell("DESPESAS TOTAIS:", true))
        summaryTable.addCell(createCell(currencyFormatter.format(ciclo.valorTotalDespesas), false))
        summaryTable.addCell(createCell("LUCRO LÍQUIDO:", true))
        summaryTable.addCell(createCell(currencyFormatter.format(ciclo.lucroLiquido), false))
        summaryTable.addCell(createCell("DÉBITO TOTAL:", true))
        summaryTable.addCell(createCell(currencyFormatter.format(ciclo.debitoTotal), false))
        
        document.add(summaryTable)
    }

    /**
     * ✅ MELHORADA: Adiciona lista detalhada de recebimentos com métodos de pagamento reais
     */
    private fun addReceiptsList(document: Document, acertos: List<Acerto>, clientes: List<Cliente>) {
        val receiptsTitle = Paragraph("LISTA DE RECEBIMENTOS")
            .setFontSize(16f)
            .setBold()
            .setMarginTop(30f)
            .setMarginBottom(15f)
        document.add(receiptsTitle)
        
        if (acertos.isEmpty()) {
            val noData = Paragraph("Nenhum acerto encontrado neste ciclo.")
                .setItalic()
                .setMarginBottom(20f)
            document.add(noData)
            return
        }
        
        // Cabeçalho da tabela
        val receiptsTable = Table(5)
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginBottom(20f)
        
        receiptsTable.addHeaderCell(createHeaderCell("Cliente"))
        receiptsTable.addHeaderCell(createHeaderCell("Data"))
        receiptsTable.addHeaderCell(createHeaderCell("Tipo Pagamento"))
        receiptsTable.addHeaderCell(createHeaderCell("Recebido"))
        receiptsTable.addHeaderCell(createHeaderCell("Débito"))
        
        // Dados dos acertos
        var totalRecebido = 0.0
        var totalDebito = 0.0
        
        acertos.forEach { acerto ->
            val cliente = clientes.find { it.id == acerto.clienteId }
            val clienteNome = cliente?.nome ?: "Cliente #${acerto.clienteId}"
            
            // ✅ NOVA LÓGICA: Processar métodos de pagamento reais do JSON
            val metodosPagamento = processarMetodosPagamento(acerto.metodosPagamentoJson)
            val tipoPagamentoTexto = formatarTiposPagamento(metodosPagamento)
            
            receiptsTable.addCell(createCell(clienteNome, false))
            receiptsTable.addCell(createCell(dateOnlyFormatter.format(acerto.dataAcerto), false))
            receiptsTable.addCell(createCell(tipoPagamentoTexto, false))
            receiptsTable.addCell(createCell(currencyFormatter.format(acerto.valorRecebido), false))
            receiptsTable.addCell(createCell(currencyFormatter.format(acerto.debitoAtual), false))
            
            totalRecebido += acerto.valorRecebido
            totalDebito += acerto.debitoAtual
        }
        
        // Adicionar linha de totais
        receiptsTable.addCell(createCell("TOTAIS", true))
        receiptsTable.addCell(createCell("", true))
        receiptsTable.addCell(createCell("", true))
        receiptsTable.addCell(createCell(currencyFormatter.format(totalRecebido), true))
        receiptsTable.addCell(createCell(currencyFormatter.format(totalDebito), true))
        
        document.add(receiptsTable)
    }

    /**
     * ✅ NOVA: Seção de resumo por modalidade de pagamento
     */
    private fun addPaymentMethodsSummary(document: Document, acertos: List<Acerto>) {
        val summaryTitle = Paragraph("RESUMO POR MODALIDADE DE PAGAMENTO")
            .setFontSize(16f)
            .setBold()
            .setMarginTop(30f)
            .setMarginBottom(15f)
        document.add(summaryTitle)
        
        // Calcular totais por modalidade
        val totaisPorModalidade = calcularTotaisPorModalidade(acertos)
        
        val summaryTable = Table(2)
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginBottom(20f)
        
        // Adicionar cada modalidade
        summaryTable.addCell(createCell("PIX:", true))
        summaryTable.addCell(createCell(currencyFormatter.format(totaisPorModalidade["PIX"] ?: 0.0), false))
        
        summaryTable.addCell(createCell("CARTÃO:", true))
        summaryTable.addCell(createCell(currencyFormatter.format(totaisPorModalidade["Cartão"] ?: 0.0), false))
        
        summaryTable.addCell(createCell("CHEQUE:", true))
        summaryTable.addCell(createCell(currencyFormatter.format(totaisPorModalidade["Cheque"] ?: 0.0), false))
        
        summaryTable.addCell(createCell("DINHEIRO:", true))
        summaryTable.addCell(createCell(currencyFormatter.format(totaisPorModalidade["Dinheiro"] ?: 0.0), false))
        
        val totalRecebido = totaisPorModalidade.values.sum()
        summaryTable.addCell(createCell("TOTAL RECEBIDO:", true))
        summaryTable.addCell(createCell(currencyFormatter.format(totalRecebido), false))
        
        document.add(summaryTable)
    }

    /**
     * Adiciona lista de despesas por categoria
     */
    private fun addExpensesList(document: Document, despesas: List<Despesa>) {
        val expensesTitle = Paragraph("LISTA DE DESPESAS")
            .setFontSize(16f)
            .setBold()
            .setMarginTop(30f)
            .setMarginBottom(15f)
        document.add(expensesTitle)
        
        if (despesas.isEmpty()) {
            val noData = Paragraph("Nenhuma despesa encontrada neste ciclo.")
                .setItalic()
                .setMarginBottom(20f)
            document.add(noData)
            return
        }
        
        // Agrupar despesas por categoria
        val despesasPorCategoria = despesas.groupBy { it.categoria }
        
        despesasPorCategoria.forEach { (categoria, despesasCategoria) ->
            val categoriaTitle = Paragraph("Categoria: $categoria")
                .setFontSize(14f)
                .setBold()
                .setMarginTop(20f)
                .setMarginBottom(10f)
            document.add(categoriaTitle)
            
            val expensesTable = Table(4)
                .setWidth(UnitValue.createPercentValue(100f))
                .setMarginBottom(15f)
            
            expensesTable.addHeaderCell(createHeaderCell("Descrição"))
            expensesTable.addHeaderCell(createHeaderCell("Data"))
            expensesTable.addHeaderCell(createHeaderCell("Valor"))
            expensesTable.addHeaderCell(createHeaderCell("Observação"))
            
            despesasCategoria.forEach { despesa ->
                expensesTable.addCell(createCell(despesa.descricao, false))
                expensesTable.addCell(createCell(try { despesa.dataHora.format(localDateTimeFormatter) } catch (e: Exception) { "Data inválida" }, false))
                expensesTable.addCell(createCell(currencyFormatter.format(despesa.valor), false))
                expensesTable.addCell(createCell(despesa.observacoes ?: "", false))
            }
            
            document.add(expensesTable)
            
            // Total da categoria
            val totalCategoria = despesasCategoria.sumOf { it.valor }
            val totalCategoriaText = Paragraph("Total $categoria: ${currencyFormatter.format(totalCategoria)}")
                .setBold()
                .setMarginBottom(10f)
            document.add(totalCategoriaText)
        }
        
        // Total geral das despesas
        val totalGeral = despesas.sumOf { it.valor }
        val totalGeralText = Paragraph("TOTAL GERAL DAS DESPESAS: ${currencyFormatter.format(totalGeral)}")
            .setFontSize(14f)
            .setBold()
            .setMarginTop(20f)
            .setMarginBottom(20f)
        document.add(totalGeralText)
    }

    /**
     * ✅ NOVA: Resumo final completamente reformulado conforme especificação
     */
    private fun addEnhancedFinalSummary(document: Document, ciclo: CicloAcertoEntity, acertos: List<Acerto>, despesas: List<Despesa>) {
        val finalTitle = Paragraph("RESUMO DO FECHAMENTO")
            .setFontSize(16f)
            .setBold()
            .setMarginTop(30f)
            .setMarginBottom(15f)
        document.add(finalTitle)
        
        // Calcular valores conforme especificação
        val totalRecebido = acertos.sumOf { it.valorRecebido }
        val despesasViagem = despesas.filter { it.categoria.equals("Viagem", ignoreCase = true) }.sumOf { it.valor }
        val subtotal = totalRecebido - despesasViagem
        val comissaoMotorista = subtotal * 0.03 // 3% do subtotal
        val comissaoIltair = totalRecebido * 0.02 // 2% do faturamento total
        
        val totaisPorModalidade = calcularTotaisPorModalidade(acertos)
        val somaPix = totaisPorModalidade["PIX"] ?: 0.0
        val somaCartao = totaisPorModalidade["Cartão"] ?: 0.0
        val totalCheques = totaisPorModalidade["Cheque"] ?: 0.0
        val somaDespesas = despesas.sumOf { it.valor }
        
        val totalGeral = subtotal - comissaoMotorista - comissaoIltair - somaPix - somaCartao - somaDespesas - totalCheques
        
        val finalTable = Table(2)
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginBottom(20f)
        
        // Adicionar todos os campos conforme especificação
        finalTable.addCell(createCell("TOTAL RECEBIDO (Faturamento Total):", true))
        finalTable.addCell(createCell(currencyFormatter.format(totalRecebido), false))
        
        finalTable.addCell(createCell("DESPESAS DE VIAGEM:", true))
        finalTable.addCell(createCell(currencyFormatter.format(despesasViagem), false))
        
        finalTable.addCell(createCell("SUBTOTAL:", true))
        finalTable.addCell(createCell(currencyFormatter.format(subtotal), false))
        
        finalTable.addCell(createCell("COMISSÃO DO MOTORISTA (3%):", true))
        finalTable.addCell(createCell(currencyFormatter.format(comissaoMotorista), false))
        
        finalTable.addCell(createCell("COMISSÃO ILTAIR (2%):", true))
        finalTable.addCell(createCell(currencyFormatter.format(comissaoIltair), false))
        
        finalTable.addCell(createCell("SOMA PIX:", true))
        finalTable.addCell(createCell(currencyFormatter.format(somaPix), false))
        
        finalTable.addCell(createCell("SOMA DESPESAS:", true))
        finalTable.addCell(createCell(currencyFormatter.format(somaDespesas), false))
        
        finalTable.addCell(createCell("CHEQUES:", true))
        finalTable.addCell(createCell(currencyFormatter.format(totalCheques), false))
        
        // Linha separadora
        finalTable.addCell(createCell("", true))
        finalTable.addCell(createCell("", true))
        
        finalTable.addCell(createCell("TOTAL GERAL:", true))
        finalTable.addCell(createCell(currencyFormatter.format(totalGeral), true))
        
        document.add(finalTable)
        
        // Rodapé
        val footer = Paragraph("Relatório gerado em ${dateFormatter.format(Date())}")
            .setFontSize(10f)
            .setItalic()
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(30f)
        document.add(footer)
    }

    /**
     * ✅ NOVA: Processa JSON dos métodos de pagamento
     */
    private fun processarMetodosPagamento(metodosPagamentoJson: String?): Map<String, Double> {
        return try {
            if (metodosPagamentoJson.isNullOrBlank()) {
                mapOf("Dinheiro" to 0.0) // Default se não houver informação
            } else {
                val tipo = object : TypeToken<Map<String, Double>>() {}.type
                Gson().fromJson(metodosPagamentoJson, tipo) ?: mapOf("Dinheiro" to 0.0)
            }
        } catch (e: Exception) {
            Log.e("PdfReportGenerator", "Erro ao processar métodos de pagamento: ${e.message}")
            mapOf("Dinheiro" to 0.0)
        }
    }

    /**
     * ✅ NOVA: Formata tipos de pagamento para exibição
     */
    private fun formatarTiposPagamento(metodosPagamento: Map<String, Double>): String {
        return if (metodosPagamento.size == 1) {
            // Se for apenas um método, exibir só o nome
            metodosPagamento.keys.first()
        } else {
            // Se forem múltiplos métodos, discriminar todos com valores
            metodosPagamento.entries
                .filter { it.value > 0 }
                .joinToString(", ") { "${it.key}: ${currencyFormatter.format(it.value)}" }
                .ifEmpty { "Não informado" }
        }
    }

    /**
     * ✅ NOVA: Calcula totais por modalidade de pagamento
     */
    private fun calcularTotaisPorModalidade(acertos: List<Acerto>): Map<String, Double> {
        val totais = mutableMapOf(
            "PIX" to 0.0,
            "Cartão" to 0.0,
            "Cheque" to 0.0,
            "Dinheiro" to 0.0
        )
        
        acertos.forEach { acerto ->
            val metodos = processarMetodosPagamento(acerto.metodosPagamentoJson)
            metodos.forEach { (metodo, valor) ->
                when (metodo.uppercase()) {
                    "PIX" -> totais["PIX"] = (totais["PIX"] ?: 0.0) + valor
                    "CARTÃO", "CARTAO" -> totais["Cartão"] = (totais["Cartão"] ?: 0.0) + valor
                    "CHEQUE" -> totais["Cheque"] = (totais["Cheque"] ?: 0.0) + valor
                    "DINHEIRO" -> totais["Dinheiro"] = (totais["Dinheiro"] ?: 0.0) + valor
                    else -> {
                        // Para métodos não mapeados, adicionar como dinheiro
                        totais["Dinheiro"] = (totais["Dinheiro"] ?: 0.0) + valor
                    }
                }
            }
        }
        
        return totais
    }

    /**
     * Cria célula de cabeçalho
     */
    private fun createHeaderCell(text: String): Cell {
        return Cell()
            .add(Paragraph(text))
            .setBold()
            .setBackgroundColor(DeviceRgb(200, 200, 200))
            .setTextAlignment(TextAlignment.CENTER)
            .setFontSize(10f)
    }

    /**
     * Cria célula normal
     */
    private fun createCell(text: String, isHeader: Boolean): Cell {
        val cell = Cell()
            .add(Paragraph(text))
            .setTextAlignment(if (isHeader) TextAlignment.LEFT else TextAlignment.CENTER)
            .setFontSize(if (isHeader) 10f else 9f)
        
        if (isHeader) {
            cell.setBold()
        }
        
        return cell
    }
} 