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

/**
 * Gerador de relatórios PDF para ciclos de acerto
 * ✅ FASE 9C: RELATÓRIOS DETALHADOS EM PDF
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
            
            // Adicionar resumo executivo
            addExecutiveSummary(document, ciclo)
            
            // Adicionar lista de recebimentos
            addReceiptsList(document, acertos, clientes)
            
            // Adicionar resumo financeiro
            addFinancialSummary(document, ciclo)
            
            // Adicionar lista de despesas
            addExpensesList(document, despesas)
            
            // Adicionar resumo final
            addFinalSummary(document, ciclo)
            
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
            // Logo
            val logoStream = context.resources.openRawResource(R.drawable.logo_globo1)
            val logoBytes = logoStream.readBytes()
            logoStream.close()
            
            val logo = Image(ImageDataFactory.create(logoBytes))
            logo.setWidth(150f)
            logo.setHorizontalAlignment(HorizontalAlignment.CENTER)
            document.add(logo)
            
            // Título do relatório
            val title = Paragraph("RELATÓRIO DETALHADO DE FECHAMENTO")
                .setFontSize(20f)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(20f)
                .setMarginBottom(10f)
            document.add(title)
            
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
     * Adiciona lista detalhada de recebimentos
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
        val receiptsTable = Table(8)
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginBottom(20f)
        
        receiptsTable.addHeaderCell(createHeaderCell("Cliente"))
        receiptsTable.addHeaderCell(createHeaderCell("Data"))
        receiptsTable.addHeaderCell(createHeaderCell("Mesa"))
        receiptsTable.addHeaderCell(createHeaderCell("Relógio Inicial"))
        receiptsTable.addHeaderCell(createHeaderCell("Relógio Final"))
        receiptsTable.addHeaderCell(createHeaderCell("Fichas"))
        receiptsTable.addHeaderCell(createHeaderCell("Recebido"))
        receiptsTable.addHeaderCell(createHeaderCell("Débito"))
        
        // Dados dos acertos
        var totalRecebido = 0.0
        var totalDebito = 0.0
        var totalPix = 0.0
        var totalCheque = 0.0
        
        acertos.forEach { acerto ->
            val cliente = clientes.find { it.id == acerto.clienteId }
            val clienteNome = cliente?.nome ?: "Cliente não encontrado"
            
            receiptsTable.addCell(createCell(clienteNome, false))
            receiptsTable.addCell(createCell(dateOnlyFormatter.format(acerto.dataAcerto), false))
            receiptsTable.addCell(createCell("N/A", false)) // mesaId não existe na entidade Acerto
            receiptsTable.addCell(createCell("N/A", false)) // relogioInicial não existe na entidade Acerto
            receiptsTable.addCell(createCell("N/A", false)) // relogioFinal não existe na entidade Acerto
            receiptsTable.addCell(createCell("N/A", false)) // cálculo não possível
            receiptsTable.addCell(createCell(currencyFormatter.format(acerto.valorRecebido), false))
            receiptsTable.addCell(createCell(currencyFormatter.format(acerto.debitoAtual), false))
            
            totalRecebido += acerto.valorRecebido
            totalDebito += acerto.debitoAtual
            
            // Calcular totais por forma de pagamento
            acerto.metodosPagamentoJson?.let { metodosJson ->
                try {
                    // Aqui você pode implementar parsing do JSON se necessário
                    // Por enquanto, vamos assumir que não temos dados detalhados
                } catch (e: Exception) {
                    // Ignorar erro de parsing
                }
            }
        }
        
        document.add(receiptsTable)
        
        // Resumo dos recebimentos
        val receiptsSummary = Table(2)
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginBottom(20f)
        
        receiptsSummary.addCell(createCell("TOTAL RECEBIDO:", true))
        receiptsSummary.addCell(createCell(currencyFormatter.format(totalRecebido), false))
        receiptsSummary.addCell(createCell("TOTAL DÉBITO:", true))
        receiptsSummary.addCell(createCell(currencyFormatter.format(totalDebito), false))
        receiptsSummary.addCell(createCell("RECEBIDO VIA PIX:", true))
        receiptsSummary.addCell(createCell(currencyFormatter.format(totalPix), false))
        receiptsSummary.addCell(createCell("RECEBIDO VIA CHEQUE:", true))
        receiptsSummary.addCell(createCell(currencyFormatter.format(totalCheque), false))
        
        document.add(receiptsSummary)
    }

    /**
     * Adiciona resumo financeiro
     */
    private fun addFinancialSummary(document: Document, ciclo: CicloAcertoEntity) {
        val financialTitle = Paragraph("RESUMO FINANCEIRO")
            .setFontSize(16f)
            .setBold()
            .setMarginTop(30f)
            .setMarginBottom(15f)
        document.add(financialTitle)
        
        val financialTable = Table(2)
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginBottom(20f)
        
        financialTable.addCell(createCell("FATURAMENTO BRUTO:", true))
        financialTable.addCell(createCell(currencyFormatter.format(ciclo.valorTotalAcertado), false))
        financialTable.addCell(createCell("DÉBITOS PENDENTES:", true))
        financialTable.addCell(createCell(currencyFormatter.format(ciclo.debitoTotal), false))
        financialTable.addCell(createCell("FATURAMENTO LÍQUIDO:", true))
        financialTable.addCell(createCell(currencyFormatter.format(ciclo.valorTotalAcertado - ciclo.debitoTotal), false))
        
        document.add(financialTable)
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
                expensesTable.addCell(createCell(despesa.dataHora.format(localDateTimeFormatter), false))
                expensesTable.addCell(createCell(currencyFormatter.format(despesa.valor), false))
                expensesTable.addCell(createCell(despesa.observacoes, false))
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
     * Adiciona resumo final do fechamento
     */
    private fun addFinalSummary(document: Document, ciclo: CicloAcertoEntity) {
        val finalTitle = Paragraph("RESUMO DO FECHAMENTO")
            .setFontSize(16f)
            .setBold()
            .setMarginTop(30f)
            .setMarginBottom(15f)
        document.add(finalTitle)
        
        val finalTable = Table(2)
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginBottom(20f)
        
        finalTable.addCell(createCell("FATURAMENTO TOTAL:", true))
        finalTable.addCell(createCell(currencyFormatter.format(ciclo.valorTotalAcertado), false))
        finalTable.addCell(createCell("DESPESAS TOTAIS:", true))
        finalTable.addCell(createCell(currencyFormatter.format(ciclo.valorTotalDespesas), false))
        finalTable.addCell(createCell("LUCRO LÍQUIDO:", true))
        finalTable.addCell(createCell(currencyFormatter.format(ciclo.lucroLiquido), false))
        
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