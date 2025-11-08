package com.example.gestaobilhares.data.database

import androidx.room.TypeConverter
import com.example.gestaobilhares.data.entities.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Classe com conversores de tipos para o Room Database.
 * Converte tipos não suportados nativamente pelo Room.
 */
class Converters {
    
    /**
     * Conversores para Date
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    /**
     * Conversores para TipoMesa enum
     */
    @TypeConverter
    fun fromTipoMesa(value: TipoMesa): String {
        return value.name
    }

    @TypeConverter
    fun toTipoMesa(value: String): TipoMesa {
        return TipoMesa.valueOf(value)
    }
    
    /**
     * Conversores para TamanhoMesa enum
     */
    @TypeConverter
    fun fromTamanhoMesa(value: TamanhoMesa): String {
        return value.name
    }

    @TypeConverter
    fun toTamanhoMesa(value: String): TamanhoMesa {
        return TamanhoMesa.valueOf(value)
    }
    
    /**
     * Conversores para EstadoConservacao enum
     */
    @TypeConverter
    fun fromEstadoConservacao(value: EstadoConservacao): String {
        return value.name
    }

    @TypeConverter
    fun toEstadoConservacao(value: String): EstadoConservacao {
        return EstadoConservacao.valueOf(value)
    }
    
    /**
     * Conversores para NivelAcesso enum
     */
    @TypeConverter
    fun fromNivelAcesso(value: NivelAcesso): String {
        return value.name
    }

    @TypeConverter
    fun toNivelAcesso(value: String): NivelAcesso {
        return NivelAcesso.valueOf(value)
    }
    
    /**
     * Conversores para StatusAcerto enum
     */
    @TypeConverter
    fun fromStatusAcerto(value: StatusAcerto): String {
        return value.name
    }

    @TypeConverter
    fun toStatusAcerto(value: String): StatusAcerto {
        return StatusAcerto.valueOf(value)
    }
    
    // Conversores para CategoriaDespesa enum removidos - agora usando entidades dinâmicas
    
    /**
     * Conversores para LocalDateTime
     */
    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? {
        return value?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }
    }

    @TypeConverter
    fun fromPagamentoMap(value: Map<String, Double>?): String? {
        return value?.let { Gson().toJson(it) }
    }

    @TypeConverter
    fun toPagamentoMap(value: String?): Map<String, Double>? {
        return value?.let {
            val type = object : TypeToken<Map<String, Double>>() {}.type
            Gson().fromJson<Map<String, Double>>(it, type)
        }
    }
    
    /**
     * ✅ FASE 12.3: NOTA - Criptografia de dados sensíveis
     * 
     * A criptografia é feita manualmente no AppRepository antes de salvar
     * e depois de ler, não através de TypeConverters, pois o Room não permite
     * múltiplos TypeConverters com a mesma assinatura (String? -> String?).
     * 
     * Os métodos de criptografia estão em:
     * - utils/DataEncryption.kt (utilitário de criptografia)
     * - data/repository/AppRepository.kt (métodos encryptCliente, decryptCliente, etc.)
     */
} 
