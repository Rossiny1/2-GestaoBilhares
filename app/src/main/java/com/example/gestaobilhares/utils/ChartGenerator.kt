package com.example.gestaobilhares.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.Log
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
    fun generateRevenuePieChart(data: Map<String, Double>, title: String): Bitmap? {
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
                Log.w("ChartGenerator", "Nenhum dado de faturamento encontrado")
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
            pieChart.data = pieData

            // Configurar descrição
            pieChart.description.text = title
            pieChart.description.textSize = 14f
            pieChart.description.textColor = Color.BLACK

            // Configurar legenda
            val legend = pieChart.legend
            legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            legend.orientation = Legend.LegendOrientation.HORIZONTAL
            legend.setDrawInside(false)
            legend.textSize = 10f

            // Configurar animação
            pieChart.animateY(1000)

            // Converter para bitmap
            convertChartToBitmap(pieChart)

        } catch (e: Exception) {
            Log.e("ChartGenerator", "Erro ao gerar gráfico de faturamento: ${e.message}", e)
            null
        }
    }

    /**
     * Gera um gráfico de pizza para despesas por tipo
     */
    fun generateExpensesPieChart(data: Map<String, Double>, title: String): Bitmap? {
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
                Log.w("ChartGenerator", "Nenhum dado de despesas encontrado")
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
            pieChart.data = pieData

            // Configurar descrição
            pieChart.description.text = title
            pieChart.description.textSize = 14f
            pieChart.description.textColor = Color.BLACK

            // Configurar legenda
            val legend = pieChart.legend
            legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            legend.orientation = Legend.LegendOrientation.HORIZONTAL
            legend.setDrawInside(false)
            legend.textSize = 10f

            // Configurar animação
            pieChart.animateY(1000)

            // Converter para bitmap
            convertChartToBitmap(pieChart)

        } catch (e: Exception) {
            Log.e("ChartGenerator", "Erro ao gerar gráfico de despesas: ${e.message}", e)
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
        
        // Configurar tamanho
        pieChart.layoutParams = android.view.ViewGroup.LayoutParams(400, 400)
        pieChart.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(400, android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.makeMeasureSpec(400, android.view.View.MeasureSpec.EXACTLY)
        )
        pieChart.layout(0, 0, 400, 400)

        // CONFIGURAÇÕES BÁSICAS PARA PDF - SIMPLIFICADAS
        pieChart.setUsePercentValues(true)
        pieChart.setDrawEntryLabels(false)
        pieChart.setDrawHoleEnabled(true) // Manter buraco central
        pieChart.setHoleColor(Color.WHITE)
        pieChart.setTransparentCircleColor(Color.WHITE)
        pieChart.setTransparentCircleAlpha(110)
        pieChart.setHoleRadius(50f)
        pieChart.setTransparentCircleRadius(55f)
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
        android.util.Log.d("ChartGenerator", "Iniciando conversão do gráfico para Bitmap")
        
        // Preparar o Looper se necessário
        if (android.os.Looper.myLooper() == null) {
            android.os.Looper.prepare()
        }
        
        // SOLUÇÃO CRÍTICA: Desativar aceleração de hardware para evitar conflitos
        pieChart.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
        
        // Configurar tamanho do gráfico
        pieChart.layoutParams = android.view.ViewGroup.LayoutParams(400, 400)
        
        // Forçar layout
        pieChart.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(400, android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.makeMeasureSpec(400, android.view.View.MeasureSpec.EXACTLY)
        )
        pieChart.layout(0, 0, 400, 400)
        
        android.util.Log.d("ChartGenerator", "Gráfico configurado - largura: ${pieChart.width}, altura: ${pieChart.height}")
        android.util.Log.d("ChartGenerator", "Dados do gráfico: ${pieChart.data?.dataSet?.entryCount} entradas")
        
        // SOLUÇÃO ALTERNATIVA: Usar getDrawingCache() conforme fontes externas
        pieChart.setDrawingCacheEnabled(true)
        pieChart.buildDrawingCache()
        
        val bitmap: Bitmap = try {
            // Tentar usar o drawing cache primeiro
            val drawingCache = pieChart.drawingCache
            if (drawingCache != null) {
                android.util.Log.d("ChartGenerator", "Bitmap criado usando drawing cache")
                Bitmap.createBitmap(drawingCache)
            } else {
                // Fallback: criar bitmap manualmente
                android.util.Log.d("ChartGenerator", "Bitmap criado usando canvas manual")
                val fallbackBitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(fallbackBitmap)
                canvas.drawColor(Color.WHITE)
                pieChart.draw(canvas)
                fallbackBitmap
            }
        } catch (e: Exception) {
            android.util.Log.e("ChartGenerator", "Erro ao criar bitmap: ${e.message}", e)
            // Fallback final
            val fallbackBitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(fallbackBitmap)
            canvas.drawColor(Color.WHITE)
            pieChart.draw(canvas)
            fallbackBitmap
        } finally {
            pieChart.setDrawingCacheEnabled(false)
        }
        
        return bitmap
    }
}
