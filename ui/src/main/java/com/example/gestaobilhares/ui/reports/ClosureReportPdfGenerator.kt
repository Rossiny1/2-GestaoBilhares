package com.example.gestaobilhares.ui.reports

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.graphics.Color
import com.github.mikephil.charting.utils.ColorTemplate
import com.example.gestaobilhares.ui.reports.ClosureReportViewModel
import com.example.gestaobilhares.core.utils.ChartGenerator
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.*
import com.itextpdf.layout.property.TextAlignment
import com.itextpdf.layout.property.UnitValue
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Gerador de relatórios PDF para fechamento (anual e por ciclo)
 * Baseado na estrutura existente do PdfReportGenerator
 */
class ClosureReportPdfGenerator(private val context: Context) {

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
    private val dateOnlyFormatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

    /**
     * Gera relatório de fechamento por acerto
     */
    fun generateAcertoClosureReport(
        ano: Int,
        numeroAcerto: Int,
        resumo: ClosureReportViewModel.Resumo,
        detalhes: List<ClosureReportViewModel.LinhaDetalhe>,
        totalMesas: Int,
        chartData: ClosureReportViewModel.ChartData? = null
    ): File {
        val fileName = "relatorio_fechamento_${ano}_acerto_${numeroAcerto}_${System.currentTimeMillis()}.pdf"
        val file = File(context.getExternalFilesDir(null), fileName)
        
        try {
            val writer = PdfWriter(FileOutputStream(file))
            val pdf = PdfDocument(writer)
            val document = Document(pdf)
            
            // Configurar margens para A4
            document.setMargins(50f, 50f, 50f, 50f)
            
                    // Adicionar cabeçalho
                    addHeader(document, "Relatório do ${numeroAcerto}º Acerto - $ano")
            
            // Adicionar resumo executivo
            addExecutiveSummary(document, resumo, totalMesas)
            
            // Adicionar gráficos se disponíveis
            chartData?.let { addChartsSection(document, it) }
            
            // Adicionar detalhamento por rota
            addRouteDetails(document, detalhes)
            
            // Adicionar análise de indicadores
            addManagementIndicators(document, resumo, totalMesas)
            
            // Adicionar resumo financeiro detalhado
            addDetailedFinancialSummary(document, resumo)
            
            document.close()
            
            Log.d("ClosureReportPdfGenerator", "Relatório de fechamento gerado com sucesso: ${file.absolutePath}")
            return file
            
        } catch (e: Exception) {
            Log.e("ClosureReportPdfGenerator", "Erro ao gerar relatório de fechamento", e)
            throw e
        }
    }

    /**
     * Gera relatório de fechamento anual
     */
    fun generateAnnualClosureReport(
        ano: Int,
        resumo: ClosureReportViewModel.Resumo,
        detalhes: List<ClosureReportViewModel.LinhaDetalhe>,
        totalMesas: Int,
        chartData: ClosureReportViewModel.ChartData? = null
    ): File {
        val fileName = "relatorio_fechamento_anual_${ano}_${System.currentTimeMillis()}.pdf"
        val file = File(context.getExternalFilesDir(null), fileName)
        
        try {
            val writer = PdfWriter(FileOutputStream(file))
            val pdf = PdfDocument(writer)
            val document = Document(pdf)
            
            // Configurar margens para A4
            document.setMargins(50f, 50f, 50f, 50f)
            
            // Adicionar cabeçalho
            addHeader(document, "Relatório de Fechamento Anual - $ano")
            
            // Adicionar resumo executivo
            addExecutiveSummary(document, resumo, totalMesas)
            
            // Adicionar gráficos se disponíveis
            chartData?.let { addChartsSection(document, it) }
            
            // Adicionar detalhamento por rota
            addRouteDetails(document, detalhes)
            
            // Adicionar análise de indicadores
            addManagementIndicators(document, resumo, totalMesas)
            
            // Adicionar resumo financeiro detalhado
            addDetailedFinancialSummary(document, resumo)
            
            // Adicionar análise anual
            addAnnualAnalysis(document, resumo, totalMesas)
            
            document.close()
            
            Log.d("ClosureReportPdfGenerator", "Relatório anual gerado com sucesso: ${file.absolutePath}")
            return file
            
        } catch (e: Exception) {
            Log.e("ClosureReportPdfGenerator", "Erro ao gerar relatório anual", e)
            throw e
        }
    }

    /**
     * Adiciona cabeçalho do relatório
     */
    private fun addHeader(document: Document, title: String) {
        // Título
        val titleText = Paragraph(title)
            .setFontSize(18f)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20f)
        document.add(titleText)
        
        // Data de geração
        val dateText = Paragraph("Gerado em: ${dateFormatter.format(Date())}")
            .setFontSize(10f)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(30f)
        document.add(dateText)
    }

    /**
     * Adiciona resumo executivo
     */
    private fun addExecutiveSummary(document: Document, resumo: ClosureReportViewModel.Resumo, totalMesas: Int) {
        val sectionTitle = Paragraph("RESUMO EXECUTIVO")
            .setFontSize(14f)
            .setBold()
            .setMarginBottom(15f)
        document.add(sectionTitle)
        
        val summaryTable = Table(2)
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginBottom(20f)
        
        // Indicadores principais
        summaryTable.addCell(createCell("Faturamento Total:", true))
        summaryTable.addCell(createCell(currencyFormatter.format(resumo.faturamentoTotal), false))
        
        summaryTable.addCell(createCell("Total de Despesas:", true))
        summaryTable.addCell(createCell(currencyFormatter.format(resumo.despesasTotal), false))
        
        summaryTable.addCell(createCell("Lucro Líquido:", true))
        summaryTable.addCell(createCell(currencyFormatter.format(resumo.lucroLiquido), false))
        
        summaryTable.addCell(createCell("Total de Mesas:", true))
        summaryTable.addCell(createCell(totalMesas.toString(), false))
        
        // Indicadores calculados
        val faturamentoPorMesa = if (totalMesas > 0) resumo.faturamentoTotal / totalMesas else 0.0
        val lucroPorMesa = if (totalMesas > 0) resumo.lucroLiquido / totalMesas else 0.0
        val margemLiquida = if (resumo.faturamentoTotal > 0) (resumo.lucroLiquido / resumo.faturamentoTotal) * 100.0 else 0.0
        
        summaryTable.addCell(createCell("Faturamento por Mesa:", true))
        summaryTable.addCell(createCell(currencyFormatter.format(faturamentoPorMesa), false))
        
        summaryTable.addCell(createCell("Lucro por Mesa:", true))
        summaryTable.addCell(createCell(currencyFormatter.format(lucroPorMesa), false))
        
        summaryTable.addCell(createCell("Margem Líquida:", true))
        summaryTable.addCell(createCell("${String.format(Locale("pt", "BR"), "%.1f%%", margemLiquida)}", false))
        
        document.add(summaryTable)
    }

    /**
     * Adiciona detalhamento por rota
     */
    private fun addRouteDetails(document: Document, detalhes: List<ClosureReportViewModel.LinhaDetalhe>) {
        val sectionTitle = Paragraph("DETALHAMENTO POR ROTA")
            .setFontSize(14f)
            .setBold()
            .setMarginBottom(15f)
        document.add(sectionTitle)
        
        val detailsTable = Table(4)
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginBottom(20f)
        
        // Cabeçalho da tabela
        detailsTable.addCell(createHeaderCell("Rota"))
        detailsTable.addCell(createHeaderCell("Faturamento"))
        detailsTable.addCell(createHeaderCell("Despesas"))
        detailsTable.addCell(createHeaderCell("Lucro"))
        
        // Dados das rotas
        detalhes.forEach { detalhe ->
            detailsTable.addCell(createCell(detalhe.rota, false))
            detailsTable.addCell(createCell(currencyFormatter.format(detalhe.faturamento), false))
            detailsTable.addCell(createCell(currencyFormatter.format(detalhe.despesas), false))
            detailsTable.addCell(createCell(currencyFormatter.format(detalhe.lucro), false))
        }
        
        document.add(detailsTable)
    }

    /**
     * Adiciona indicadores gerenciais
     */
    private fun addManagementIndicators(document: Document, resumo: ClosureReportViewModel.Resumo, totalMesas: Int) {
        val sectionTitle = Paragraph("INDICADORES GERENCIAIS")
            .setFontSize(14f)
            .setBold()
            .setMarginBottom(15f)
        document.add(sectionTitle)
        
        val indicatorsTable = Table(2)
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginBottom(20f)
        
        // Eficiência operacional
        val eficienciaOperacional = if (resumo.faturamentoTotal > 0) (resumo.lucroLiquido / resumo.faturamentoTotal) * 100.0 else 0.0
        indicatorsTable.addCell(createCell("Eficiência Operacional:", true))
        indicatorsTable.addCell(createCell("${String.format(Locale("pt", "BR"), "%.1f%%", eficienciaOperacional)}", false))
        
        // Produtividade por mesa
        val produtividadeMesa = if (totalMesas > 0) resumo.faturamentoTotal / totalMesas else 0.0
        indicatorsTable.addCell(createCell("Produtividade por Mesa:", true))
        indicatorsTable.addCell(createCell(currencyFormatter.format(produtividadeMesa), false))
        
        // Rentabilidade
        val rentabilidade = if (resumo.despesasTotal > 0) (resumo.lucroLiquido / resumo.despesasTotal) * 100.0 else 0.0
        indicatorsTable.addCell(createCell("Rentabilidade:", true))
        indicatorsTable.addCell(createCell("${String.format(Locale("pt", "BR"), "%.1f%%", rentabilidade)}", false))
        
        // Custo por mesa
        val custoPorMesa = if (totalMesas > 0) resumo.despesasTotal / totalMesas else 0.0
        indicatorsTable.addCell(createCell("Custo por Mesa:", true))
        indicatorsTable.addCell(createCell(currencyFormatter.format(custoPorMesa), false))
        
        document.add(indicatorsTable)
    }

    /**
     * Adiciona resumo financeiro detalhado
     */
    private fun addDetailedFinancialSummary(document: Document, resumo: ClosureReportViewModel.Resumo) {
        val sectionTitle = Paragraph("RESUMO FINANCEIRO DETALHADO")
            .setFontSize(14f)
            .setBold()
            .setMarginBottom(15f)
        document.add(sectionTitle)
        
        val financialTable = Table(2)
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginBottom(20f)
        
        // Receitas
        financialTable.addCell(createCell("FATURAMENTO TOTAL:", true))
        financialTable.addCell(createCell(currencyFormatter.format(resumo.faturamentoTotal), false))
        
        // Despesas discriminadas
        financialTable.addCell(createCell("Despesas de Rotas:", true))
        financialTable.addCell(createCell(currencyFormatter.format(resumo.despesasRotas), false))
        
        financialTable.addCell(createCell("Despesas Globais:", true))
        financialTable.addCell(createCell(currencyFormatter.format(resumo.despesasGlobais), false))
        
        financialTable.addCell(createCell("Comissão Motorista (3%):", true))
        financialTable.addCell(createCell(currencyFormatter.format(resumo.comissaoMotorista), false))
        
        financialTable.addCell(createCell("Comissão Iltair (2%):", true))
        financialTable.addCell(createCell(currencyFormatter.format(resumo.comissaoIltair), false))
        
        // Linha separadora
        financialTable.addCell(createCell("", true))
        financialTable.addCell(createCell("", true))
        
        financialTable.addCell(createCell("TOTAL DESPESAS:", true))
        financialTable.addCell(createCell(currencyFormatter.format(resumo.despesasTotal), false))
        
        // Lucro líquido
        financialTable.addCell(createTotalCell("LUCRO LÍQUIDO:"))
        financialTable.addCell(createTotalCell(currencyFormatter.format(resumo.lucroLiquido)))
        
        // Distribuição do lucro
        financialTable.addCell(createCell("", true))
        financialTable.addCell(createCell("", true))
        
        financialTable.addCell(createCell("Lucro Rossiny (60%):", true))
        financialTable.addCell(createCell(currencyFormatter.format(resumo.lucroRossiny), false))
        
        financialTable.addCell(createCell("Lucro Petrina (40%):", true))
        financialTable.addCell(createCell(currencyFormatter.format(resumo.lucroPetrina), false))
        
        document.add(financialTable)
    }

    /**
     * Adiciona análise anual (apenas para relatórios anuais)
     */
    private fun addAnnualAnalysis(document: Document, resumo: ClosureReportViewModel.Resumo, totalMesas: Int) {
        val sectionTitle = Paragraph("ANÁLISE ANUAL")
            .setFontSize(14f)
            .setBold()
            .setMarginBottom(15f)
        document.add(sectionTitle)
        
        val analysisText = Paragraph(
            "Este relatório apresenta o consolidado anual de todas as operações. " +
            "Os indicadores mostram a performance geral do negócio, incluindo " +
            "faturamento total de ${currencyFormatter.format(resumo.faturamentoTotal)}, " +
            "com margem líquida de ${String.format(Locale("pt", "BR"), "%.1f%%", if (resumo.faturamentoTotal > 0) (resumo.lucroLiquido / resumo.faturamentoTotal) * 100.0 else 0.0)}. " +
            "A produtividade média por mesa foi de ${currencyFormatter.format(if (totalMesas > 0) resumo.faturamentoTotal / totalMesas else 0.0)}."
        )
            .setFontSize(10f)
            .setMarginBottom(20f)
        
        document.add(analysisText)
    }

    /**
     * Cria célula normal
     */
    private fun createCell(text: String, isBold: Boolean = false): Cell {
        val paragraph = if (isBold) {
            Paragraph(text).setBold().setFontSize(10f)
        } else {
            Paragraph(text).setFontSize(10f)
        }
        return Cell().add(paragraph).setPadding(8f)
    }

    /**
     * Cria célula de cabeçalho
     */
    private fun createHeaderCell(text: String): Cell {
        return Cell()
            .add(Paragraph(text).setBold().setFontSize(10f))
            .setBackgroundColor(DeviceRgb(200, 200, 200))
            .setPadding(8f)
    }

    /**
     * Cria célula de total (com fundo cinza)
     */
    private fun createTotalCell(text: String): Cell {
        return Cell()
            .add(Paragraph(text).setBold().setFontSize(10f))
            .setBackgroundColor(DeviceRgb(220, 220, 220))
            .setPadding(8f)
    }

    /**
     * Adiciona seção de gráficos ao documento
     */
    private fun addChartsSection(document: Document, chartData: ClosureReportViewModel.ChartData) {
        try {
            val chartGenerator = ChartGenerator(context)
            
            // Título da seção
            document.add(
                Paragraph("Análise Gráfica")
                    .setBold()
                    .setFontSize(18f)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(20f)
                    .setMarginBottom(15f)
            )
            
            // Ordenar dados por valor para manter correspondência de cores entre gráfico e legenda
            val revenuePairs = chartData.faturamentoPorRota.entries.sortedByDescending { it.value }
            val orderedRevenueMap = java.util.LinkedHashMap<String, Double>().apply {
                revenuePairs.forEach { put(it.key, it.value) }
            }
            val expensesPairs = chartData.despesasPorTipo.entries.sortedByDescending { it.value }
            val orderedExpensesMap = java.util.LinkedHashMap<String, Double>().apply {
                expensesPairs.forEach { put(it.key, it.value) }
            }

            // Gerar gráficos com mapas ordenados
            val revenueChart = chartGenerator.generateRevenuePieChart(orderedRevenueMap, "Faturamento por Rota")
            val expensesChart = chartGenerator.generateExpensesPieChart(orderedExpensesMap, "Despesas por Tipo")
            
            // Adicionar gráficos ao documento
            // Tabela de 2 colunas: cada célula terá imagem grande e uma legenda textual abaixo
            val table = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
                .setWidth(UnitValue.createPercentValue(100f))
                .setMarginTop(10f)
                .setMarginBottom(10f)
            
            // Célula do gráfico de faturamento
            val revenueCell = Cell()
            if (revenueChart != null) {
                val revenueImage = Image(ImageDataFactory.create(bitmapToByteArray(revenueChart)))
                revenueImage.scaleToFit(360f, 360f)
                revenueImage.setAutoScale(true)
                // Título acima do gráfico
                revenueCell.add(
                    Paragraph("Faturamento por Rota")
                        .setBold()
                        .setFontSize(11f)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(6f)
                )
                revenueCell.add(revenueImage)
            } else {
                revenueCell.add(Paragraph("Sem dados de faturamento").setTextAlignment(TextAlignment.CENTER))
            }
            table.addCell(revenueCell)
            
            // Célula do gráfico de despesas
            val expensesCell = Cell()
            if (expensesChart != null) {
                val expensesImage = Image(ImageDataFactory.create(bitmapToByteArray(expensesChart)))
                expensesImage.scaleToFit(360f, 360f)
                expensesImage.setAutoScale(true)
                expensesCell.add(
                    Paragraph("Despesas por Tipo")
                        .setBold()
                        .setFontSize(11f)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(6f)
                )
                expensesCell.add(expensesImage)
            } else {
                expensesCell.add(Paragraph("Sem dados de despesas").setTextAlignment(TextAlignment.CENTER))
            }
            table.addCell(expensesCell)
            
            document.add(table)

            // Legendas completas abaixo (fora das imagens) com marcadores coloridos
            val legendsTable = Table(2)
                .setWidth(UnitValue.createPercentValue(100f))
                .setMarginTop(6f)
                .setMarginBottom(16f)

            legendsTable.addCell(
                Cell().add(
                    createLegendRow(revenuePairs.map { it.key })
                )
            )
            legendsTable.addCell(
                Cell().add(
                    createLegendRow(expensesPairs.map { it.key })
                )
            )

            document.add(legendsTable)
            
        } catch (e: Exception) {
            Log.e("ClosureReportPdfGenerator", "Erro ao adicionar gráficos: ${e.message}", e)
            // Adicionar mensagem de erro em vez de falhar completamente
            document.add(
                Paragraph("Erro ao gerar gráficos: ${e.message}")
                    .setFontSize(10f)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(10f)
            )
        }
    }

    /**
     * Converte Bitmap para ByteArray
     */
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    /**
     * Cria um parágrafo de legenda com marcadores coloridos
     */
    private fun createLegendRow(labelsInOrder: List<String>): Paragraph {
        val p = Paragraph().setFontSize(10f)
        val colors = ColorTemplate.MATERIAL_COLORS
        labelsInOrder.forEachIndexed { index, label ->
            val color = colors[index % colors.size]
            val swatch = createColorSwatch(color)
            p.add(swatch)
            p.add(com.itextpdf.layout.element.Text(" "))
            p.add(com.itextpdf.layout.element.Text(label).setFontSize(10f))
            if (index < labelsInOrder.size - 1) p.add(com.itextpdf.layout.element.Text("    "))
        }
        return p
    }

    private fun createColorSwatch(colorInt: Int): Image {
        val bmp = Bitmap.createBitmap(12, 12, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        canvas.drawColor(colorInt)
        val img = Image(ImageDataFactory.create(bitmapToByteArray(bmp)))
        img.scaleAbsolute(8f, 8f)
        img.setMarginRight(2f)
        img.setAutoScale(false)
        return img
    }
}
