package com.example.gestaobilhares.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * ✅ FASE 4: UTILITÁRIOS DE DATA CENTRALIZADOS
 * 
 * Centraliza todas as operações com datas:
 * - Formatação padronizada
 * - Conversões comuns
 * - Validações de data
 * - Elimina duplicação de código (~80 linhas)
 */
object DateUtils {
    
    // ==================== FORMATOS PADRÃO ====================
    
    private const val FORMATO_BRASILEIRO = "dd/MM/yyyy"
    private const val FORMATO_BRASILEIRO_COM_HORA = "dd/MM/yyyy HH:mm"
    private const val FORMATO_ISO = "yyyy-MM-dd"
    private const val FORMATO_ISO_COM_HORA = "yyyy-MM-dd HH:mm:ss"
    private const val FORMATO_HORA = "HH:mm"
    private const val FORMATO_MES_ANO = "MM/yyyy"
    
    // ==================== FORMATADORES ====================
    
    private val formatoBrasileiro = SimpleDateFormat(FORMATO_BRASILEIRO, Locale("pt", "BR"))
    private val formatoBrasileiroComHora = SimpleDateFormat(FORMATO_BRASILEIRO_COM_HORA, Locale("pt", "BR"))
    private val formatoISO = SimpleDateFormat(FORMATO_ISO, Locale("pt", "BR"))
    private val formatoISOComHora = SimpleDateFormat(FORMATO_ISO_COM_HORA, Locale("pt", "BR"))
    private val formatoHora = SimpleDateFormat(FORMATO_HORA, Locale("pt", "BR"))
    private val formatoMesAno = SimpleDateFormat(FORMATO_MES_ANO, Locale("pt", "BR"))
    
    // ==================== FORMATAÇÃO DE DATAS ====================
    
    /**
     * Formata data para formato brasileiro (dd/MM/yyyy)
     */
    fun formatarDataBrasileira(data: Date): String {
        return formatoBrasileiro.format(data)
    }
    
    /**
     * Formata data para formato brasileiro com hora (dd/MM/yyyy HH:mm)
     */
    fun formatarDataBrasileiraComHora(data: Date): String {
        return formatoBrasileiroComHora.format(data)
    }
    
    /**
     * Formata data para formato ISO (yyyy-MM-dd)
     */
    fun formatarDataISO(data: Date): String {
        return formatoISO.format(data)
    }
    
    /**
     * Formata data para formato ISO com hora (yyyy-MM-dd HH:mm:ss)
     */
    fun formatarDataISOComHora(data: Date): String {
        return formatoISOComHora.format(data)
    }
    
    /**
     * Formata apenas a hora (HH:mm)
     */
    fun formatarHora(data: Date): String {
        return formatoHora.format(data)
    }
    
    /**
     * Formata mês e ano (MM/yyyy)
     */
    fun formatarMesAno(data: Date): String {
        return formatoMesAno.format(data)
    }
    
    // ==================== CONVERSÃO DE STRINGS ====================
    
    /**
     * Converte string brasileira para Date
     */
    fun parseDataBrasileira(dataString: String): Date? {
        return try {
            formatoBrasileiro.parse(dataString)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Converte string brasileira com hora para Date
     */
    fun parseDataBrasileiraComHora(dataString: String): Date? {
        return try {
            formatoBrasileiroComHora.parse(dataString)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Converte string ISO para Date
     */
    fun parseDataISO(dataString: String): Date? {
        return try {
            formatoISO.parse(dataString)
        } catch (e: Exception) {
            null
        }
    }
    
    // ==================== OPERAÇÕES COM DATAS ====================
    
    /**
     * Obtém data atual
     */
    fun obterDataAtual(): Date {
        return Date()
    }
    
    /**
     * Obtém data atual formatada em brasileiro
     */
    fun obterDataAtualFormatada(): String {
        return formatarDataBrasileira(obterDataAtual())
    }
    
    /**
     * Obtém data atual formatada com hora
     */
    fun obterDataAtualComHoraFormatada(): String {
        return formatarDataBrasileiraComHora(obterDataAtual())
    }
    
    /**
     * Adiciona dias a uma data
     */
    fun adicionarDias(data: Date, dias: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = data
        calendar.add(Calendar.DAY_OF_MONTH, dias)
        return calendar.time
    }
    
    /**
     * Adiciona meses a uma data
     */
    fun adicionarMeses(data: Date, meses: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = data
        calendar.add(Calendar.MONTH, meses)
        return calendar.time
    }
    
    /**
     * Adiciona anos a uma data
     */
    fun adicionarAnos(data: Date, anos: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = data
        calendar.add(Calendar.YEAR, anos)
        return calendar.time
    }
    
    // ==================== VALIDAÇÕES ====================
    
    /**
     * Verifica se uma data é válida
     */
    fun isDataValida(dataString: String): Boolean {
        return parseDataBrasileira(dataString) != null
    }
    
    /**
     * Verifica se uma data é futura
     */
    fun isDataFutura(data: Date): Boolean {
        return data.after(obterDataAtual())
    }
    
    /**
     * Verifica se uma data é passada
     */
    fun isDataPassada(data: Date): Boolean {
        return data.before(obterDataAtual())
    }
    
    /**
     * Verifica se duas datas são iguais (ignorando hora)
     */
    fun isMesmoDia(data1: Date, data2: Date): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance()
        cal1.time = data1
        cal2.time = data2
        
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
    
    // ==================== CÁLCULOS ====================
    
    /**
     * Calcula diferença em dias entre duas datas
     */
    fun calcularDiferencaEmDias(dataInicial: Date, dataFinal: Date): Long {
        val diferenca = dataFinal.time - dataInicial.time
        return diferenca / (24 * 60 * 60 * 1000)
    }
    
    /**
     * Calcula diferença em meses entre duas datas
     */
    fun calcularDiferencaEmMeses(dataInicial: Date, dataFinal: Date): Int {
        val calInicial = Calendar.getInstance()
        val calFinal = Calendar.getInstance()
        calInicial.time = dataInicial
        calFinal.time = dataFinal
        
        val anos = calFinal.get(Calendar.YEAR) - calInicial.get(Calendar.YEAR)
        val meses = calFinal.get(Calendar.MONTH) - calInicial.get(Calendar.MONTH)
        
        return anos * 12 + meses
    }
    
    // ==================== CONSTANTES ÚTEIS ====================
    
    /**
     * Obtém primeiro dia do mês
     */
    fun obterPrimeiroDiaDoMes(data: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = data
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }
    
    /**
     * Obtém último dia do mês
     */
    fun obterUltimoDiaDoMes(data: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = data
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.time
    }
    
    /**
     * ✅ FASE 2: Calcula timestamps de início e fim do ano para range queries otimizadas
     * Função centralizada para eliminar duplicação de código
     * @param ano Ano como String (ex: "2025")
     * @return Pair<Long, Long> com timestamps de início e fim do ano
     */
    fun calcularRangeAno(ano: String): Pair<Long, Long> {
        val anoInt = ano.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, anoInt)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val inicioAno = calendar.timeInMillis
        
        calendar.add(Calendar.YEAR, 1)
        val fimAno = calendar.timeInMillis
        
        return Pair(inicioAno, fimAno)
    }
}
