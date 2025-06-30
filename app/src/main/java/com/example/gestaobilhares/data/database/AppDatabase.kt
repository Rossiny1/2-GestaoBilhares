package com.example.gestaobilhares.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.example.gestaobilhares.data.dao.RotaDao
import com.example.gestaobilhares.data.dao.ClienteDao
import com.example.gestaobilhares.data.dao.DespesaDao
import com.example.gestaobilhares.data.entities.*

/**
 * Banco de dados principal da aplicação usando Room.
 * Centraliza todas as entidades e DAOs da aplicação.
 */
@Database(
    entities = [
        Rota::class,
        Cliente::class,
        Mesa::class,
        Colaborador::class,
        Acerto::class,
        Despesa::class
    ],
    version = 6, // Incrementado para incluir novos campos nas entidades Cliente e Mesa
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    /**
     * DAO para operações com rotas.
     */
    abstract fun rotaDao(): RotaDao
    
    /**
     * DAO para operações com clientes.
     */
    abstract fun clienteDao(): ClienteDao
    
    /**
     * DAO para operações with mesas.
     */
    abstract fun mesaDao(): com.example.gestaobilhares.data.dao.MesaDao
    
    /**
     * DAO para operações com colaboradores.
     * TODO: Implementar ColaboradorDao na próxima fase
     */
    // abstract fun colaboradorDao(): ColaboradorDao
    
    /**
     * DAO para operações com acertos.
     */
    abstract fun acertoDao(): com.example.gestaobilhares.data.dao.AcertoDao
    
    /**
     * DAO para operações com despesas.
     */
    abstract fun despesaDao(): DespesaDao

    companion object {
        
        // Nome do banco de dados
        private const val DATABASE_NAME = "gestao_bilhares_database"
        
        // Instância singleton do banco de dados
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Obtém a instância singleton do banco de dados.
         * Usa o padrão Double-Checked Locking para thread safety.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    // Para produção, remover allowMainThreadQueries()
                    // .allowMainThreadQueries() // Apenas para desenvolvimento/testes
                    .fallbackToDestructiveMigration() // Para desenvolvimento inicial
                    .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
} 
