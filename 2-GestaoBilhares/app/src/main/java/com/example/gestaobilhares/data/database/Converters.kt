package com.example.gestaobilhares.data.database

import androidx.room.TypeConverter
import com.example.gestaobilhares.data.entities.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date

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
    
    /**
     * Conversores para CategoriaDespesa enum
     */
    @TypeConverter
    fun fromCategoriaDespesa(value: CategoriaDespesa): String {
        return value.name
    }

    @TypeConverter
    fun toCategoriaDespesa(value: String): CategoriaDespesa {
        return CategoriaDespesa.valueOf(value)
    }
    
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
} 
