package com.example.gestaobilhares.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import timber.log.Timber
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.NumberFormat
import java.util.*

/**
 * Utilitário para gerar gráficos de pizza como bitmaps
 * Baseado na biblioteca MPAndroidChart
 */
class ChartGenerator(private val context: Context) {

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    /**
     * Gera um gráfico de pizza para faturamento por rota
     */
    fun generateRevenuePieChart(data: Map<String, Double>, @Suppress("UNUSED_PARAMETER") title: String): Bitmap? {
        return try {
            val pieChart = createPieChart()
            val entries = mutableListOf<PieEntry>()
            val labels = mutableListOf<String>()

            // Converter dados para entradas do gráfico
            data.forEach { (label, value) ->
                if (value > 0) {
                    entries.add(PieEntry(value.toFloat(), label))
                    labels.add(label)
                }
            }

            if (entries.isEmpty()) {
                Timber.tag("ChartGenerator").w("Nenhum dado de faturamento encontrado")
                return null
            }

            // Configurar dataset - CORRIGIDAS CORES PARA MELHOR VISIBILIDADE
            val dataSet = PieDataSet(entries, "")
            
            // Usar cores padrão do Material Design - mais confiáveis
            dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
            
            dataSet.valueTextSize = 12f
            dataSet.valueTextColor = Color.BLACK
            dataSet.sliceSpace = 2f

            // Configurar dados
            val pieData = PieData(dataSet)
            pieData.setValueFormatter(PercentFormatter(pieChart))
            pieData.setValueTextSize(12f)
            pieData.setValueTextColor(Color.BLACK)
            pieChart.data = pieData

            // Garantir atualização sem animação (offscreen)
            pieChart.highlightValues(null)
            pieChart.notifyDataSetChanged()
            pieChart.invalidate()

            // Remover descrição para evitar título duplicado no gráfico
            pieChart.description.isEnabled = false

            // Configurar legenda
            val legend = pieChart.legend
            // Desativar a legenda dentro do gráfico para maximizar o tamanho do donut.
            legend.isEnabled = false
            // Espaços menores agora que a legenda não está no chart
            pieChart.setExtraOffsets(8f, 8f, 8f, 8f)

            // Converter para bitmap
            convertChartToBitmap(pieChart)

        } catch (e: Exception) {
            Timber.tag("ChartGenerator").e(e, "Erro ao gerar gráfico de faturamento: ${e.message}")
            null
        }
    }

    /**
     * Gera um gráfico de pizza para despesas por tipo
     */
    fun generateExpensesPieChart(data: Map<String, Double>, @Suppress("UNUSED_PARAMETER") title: String): Bitmap? {
        return try {
            val pieChart = createPieChart()
            val entries = mutableListOf<PieEntry>()
            val labels = mutableListOf<String>()

            // Converter dados para entradas do gráfico
            data.forEach { (label, value) ->
                if (value > 0) {
                    entries.add(PieEntry(value.toFloat(), label))
                    labels.add(label)
                }
            }

            if (entries.isEmpty()) {
                Timber.tag("ChartGenerator").w("Nenhum dado de despesas encontrado")
                return null
            }

            // Configurar dataset - CORRIGIDAS CORES PARA MELHOR VISIBILIDADE
            val dataSet = PieDataSet(entries, "")
            
            // Usar cores padrão do Material Design - mais confiáveis
            dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
            
            dataSet.valueTextSize = 12f
            dataSet.valueTextColor = Color.BLACK
            dataSet.sliceSpace = 2f

            // Configurar dados
            val pieData = PieData(dataSet)
            pieData.setValueFormatter(PercentFormatter(pieChart))
            pieData.setValueTextSize(12f)
            pieData.setValueTextColor(Color.BLACK)
            pieChart.data = pieData

            // Garantir atualização sem animação (offscreen)
            pieChart.highlightValues(null)
            pieChart.notifyDataSetChanged()
            pieChart.invalidate()

            // Remover descrição para evitar título duplicado no gráfico
            pieChart.description.isEnabled = false

            // Configurar legenda
            val legend = pieChart.legend
            legend.isEnabled = false
            pieChart.setExtraOffsets(8f, 8f, 8f, 8f)

            // Converter para bitmap
            convertChartToBitmap(pieChart)

        } catch (e: Exception) {
            Timber.tag("ChartGenerator").e(e, "Erro ao gerar gráfico de despesas: ${e.message}")
            null
        }
    }

    /**
     * Cria uma instância configurada do PieChart
     */
    private fun createPieChart(): PieChart {
        // Preparar o Looper para criar componentes de UI em thread de background
        if (android.os.Looper.myLooper() == null) {
            android.os.Looper.prepare()
        }
        
        val pieChart = PieChart(context)
        
        // Configurar tamanho (maior para melhor definição)
        val size = 500
        pieChart.layoutParams = android.view.ViewGroup.LayoutParams(size, size)
        pieChart.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(size, android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.makeMeasureSpec(size, android.view.View.MeasureSpec.EXACTLY)
        )
        pieChart.layout(0, 0, size, size)

        // CONFIGURAÇÕES BÁSICAS PARA PDF - SIMPLIFICADAS
        pieChart.setUsePercentValues(true)
        pieChart.setDrawEntryLabels(false)
        pieChart.setDrawHoleEnabled(true) // Manter buraco central
        pieChart.setHoleColor(Color.WHITE)
        pieChart.setTransparentCircleColor(Color.WHITE)
        pieChart.setTransparentCircleAlpha(110)
        pieChart.setHoleRadius(40f)
        pieChart.setTransparentCircleRadius(45f)
        pieChart.setBackgroundColor(Color.WHITE)
        
        // Configurar legenda
        val legend = pieChart.legend
        legend.isEnabled = true
        legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
        legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
        legend.orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false)
        legend.xEntrySpace = 10f
        legend.yEntrySpace = 5f
        legend.textSize = 10f
        legend.textColor = Color.BLACK

        return pieChart
    }

    /**
     * Converte um PieChart em Bitmap
     */
    private fun convertChartToBitmap(pieChart: PieChart): Bitmap {
        Timber.tag("ChartGenerator").d("Iniciando conversão do gráfico para Bitmap")
        
        // Preparar o Looper se necessário
        if (android.os.Looper.myLooper() == null) {
            android.os.Looper.prepare()
        }
        
        // Desativar aceleração de hardware para evitar conflitos
        pieChart.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
        
        // Configurar tamanho do gráfico
        val size2 = 500
        pieChart.layoutParams = android.view.ViewGroup.LayoutParams(size2, size2)
        
        // Forçar layout
        pieChart.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(size2, android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.makeMeasureSpec(size2, android.view.View.MeasureSpec.EXACTLY)
        )
        pieChart.layout(0, 0, size2, size2)
        
        Timber.tag("ChartGenerator").d("Gráfico configurado - largura: ${pieChart.width}, altura: ${pieChart.height}")
        Timber.tag("ChartGenerator").d("Dados do gráfico: ${pieChart.data?.dataSet?.entryCount} entradas")
        
        // Tentar obter o bitmap nativo do chart (recomendado pelo MPAndroidChart)
        val bitmap: Bitmap = try {
            val native = pieChart.chartBitmap
            if (native != null) {
                Timber.tag("ChartGenerator").d("Bitmap criado via chartBitmap")
                native
            } else {
                Timber.tag("ChartGenerator").d("chartBitmap null, desenhando manualmente")
                val fb = Bitmap.createBitmap(size2, size2, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(fb)
                canvas.drawColor(Color.WHITE)
                pieChart.draw(canvas)
                fb
            }
        } catch (e: Exception) {
            Timber.tag("ChartGenerator").e(e, "Erro ao criar bitmap: ${e.message}")
            val fb = Bitmap.createBitmap(size2, size2, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(fb)
            canvas.drawColor(Color.WHITE)
            pieChart.draw(canvas)
            fb
        }
        
        return bitmap
    }
}
