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
        Despesa::class,
        AcertoMesa::class,
        CicloAcertoEntity::class, // ✅ FASE 8A: NOVA ENTIDADE PARA HISTÓRICO DE CICLOS
        CategoriaDespesa::class, // ✅ NOVO: CATEGORIAS DE DESPESAS
        TipoDespesa::class // ✅ NOVO: TIPOS DE DESPESAS
    ],
    version = 19, // ✅ MIGRATION: Correção dos nomes das colunas de foto
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
    
    /**
     * DAO para operações com acerto-mesas.
     */
    abstract fun acertoMesaDao(): com.example.gestaobilhares.data.dao.AcertoMesaDao
    
    /**
     * DAO para operações com ciclos de acerto.
     * ✅ FASE 8A: NOVO DAO PARA HISTÓRICO DE CICLOS
     */
    abstract fun cicloAcertoDao(): com.example.gestaobilhares.data.dao.CicloAcertoDao
    
    /**
     * DAO para operações com categorias de despesas.
     * ✅ NOVO: CATEGORIAS DE DESPESAS
     */
    abstract fun categoriaDespesaDao(): com.example.gestaobilhares.data.dao.CategoriaDespesaDao
    
    /**
     * DAO para operações com tipos de despesas.
     * ✅ NOVO: TIPOS DE DESPESAS
     */
    abstract fun tipoDespesaDao(): com.example.gestaobilhares.data.dao.TipoDespesaDao

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
                val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
                    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                        database.execSQL("ALTER TABLE ciclos_acerto ADD COLUMN debito_total REAL NOT NULL DEFAULT 0.0")
                    }
                }
                
                val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
                    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // Limpar dados das mesas para evitar problemas com enums antigos
                        database.execSQL("DELETE FROM mesas")
                    }
                }
                
                val MIGRATION_13_14 = object : androidx.room.migration.Migration(13, 14) {
                    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // Recriar tabela clientes com schema correto
                        database.execSQL("""
                            CREATE TABLE clientes_new (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                nome TEXT NOT NULL,
                                nome_fantasia TEXT,
                                cpf_cnpj TEXT,
                                telefone TEXT,
                                telefone2 TEXT,
                                email TEXT,
                                endereco TEXT,
                                bairro TEXT,
                                cidade TEXT,
                                estado TEXT,
                                cep TEXT,
                                rota_id INTEGER NOT NULL,
                                valor_ficha REAL NOT NULL DEFAULT 0.0,
                                comissao_ficha REAL NOT NULL DEFAULT 0.0,
                                numero_contrato TEXT,
                                debito_anterior REAL NOT NULL DEFAULT 0.0,
                                debito_atual REAL NOT NULL DEFAULT 0.0,
                                ativo INTEGER NOT NULL DEFAULT 1,
                                observacoes TEXT,
                                data_cadastro INTEGER NOT NULL,
                                data_ultima_atualizacao INTEGER NOT NULL,
                                FOREIGN KEY (rota_id) REFERENCES rotas (id) ON DELETE CASCADE
                            )
                        """)
                        
                        // Copiar dados existentes
                        database.execSQL("""
                            INSERT INTO clientes_new (
                                id, nome, nome_fantasia, cpf_cnpj, telefone, email, 
                                endereco, cidade, estado, cep, rota_id, valor_ficha, 
                                comissao_ficha, numero_contrato, debito_anterior, 
                                debito_atual, ativo, observacoes, data_cadastro, 
                                data_ultima_atualizacao
                            )
                            SELECT 
                                id, nome, nome_fantasia, cnpj, telefone, email, 
                                endereco, cidade, estado, cep, rota_id, valor_ficha, 
                                comissao_ficha, numero_contrato, debito_anterior, 
                                debito_atual, ativo, observacoes, data_cadastro, 
                                data_ultima_atualizacao
                            FROM clientes
                        """)
                        
                        // Remover tabela antiga e renomear nova
                        database.execSQL("DROP TABLE clientes")
                        database.execSQL("ALTER TABLE clientes_new RENAME TO clientes")
                        
                        // Recriar índice
                        database.execSQL("CREATE INDEX index_clientes_rota_id ON clientes (rota_id)")
                    }
                }
                
                val MIGRATION_14_15 = object : androidx.room.migration.Migration(14, 15) {
                    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // Adicionar campos de geolocalização na tabela clientes
                        database.execSQL("ALTER TABLE clientes ADD COLUMN latitude REAL")
                        database.execSQL("ALTER TABLE clientes ADD COLUMN longitude REAL")
                        database.execSQL("ALTER TABLE clientes ADD COLUMN precisao_gps REAL")
                        database.execSQL("ALTER TABLE clientes ADD COLUMN data_captura_gps INTEGER")
                    }
                }
                
                val MIGRATION_15_16 = object : androidx.room.migration.Migration(15, 16) {
                    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // Adicionar campos de foto do relógio na tabela acerto_mesas
                        database.execSQL("ALTER TABLE acerto_mesas ADD COLUMN foto_relogio_final TEXT")
                        database.execSQL("ALTER TABLE acerto_mesas ADD COLUMN data_foto INTEGER")
                    }
                }
                
                val MIGRATION_16_17 = object : androidx.room.migration.Migration(16, 17) {
                    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // Adicionar campos de foto do comprovante na tabela despesas
                        database.execSQL("ALTER TABLE despesas ADD COLUMN fotoComprovante TEXT")
                        database.execSQL("ALTER TABLE despesas ADD COLUMN dataFotoComprovante INTEGER")
                    }
                }
                
                val MIGRATION_17_18 = object : androidx.room.migration.Migration(17, 18) {
                    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // Migration para corrigir problemas de integridade do schema
                        // Recriar tabelas se necessário para garantir consistência
                        try {
                            // Verificar se a tabela clientes tem todos os campos necessários
                            database.execSQL("""
                                CREATE TABLE IF NOT EXISTS clientes_temp (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                    nome TEXT NOT NULL,
                                    nome_fantasia TEXT,
                                    cpf_cnpj TEXT,
                                    telefone TEXT,
                                    telefone2 TEXT,
                                    email TEXT,
                                    endereco TEXT,
                                    bairro TEXT,
                                    cidade TEXT,
                                    estado TEXT,
                                    cep TEXT,
                                    rota_id INTEGER NOT NULL,
                                    valor_ficha REAL NOT NULL DEFAULT 0.0,
                                    comissao_ficha REAL NOT NULL DEFAULT 0.0,
                                    numero_contrato TEXT,
                                    debito_anterior REAL NOT NULL DEFAULT 0.0,
                                    debito_atual REAL NOT NULL DEFAULT 0.0,
                                    ativo INTEGER NOT NULL DEFAULT 1,
                                    observacoes TEXT,
                                    data_cadastro INTEGER NOT NULL,
                                    data_ultima_atualizacao INTEGER NOT NULL,
                                    latitude REAL,
                                    longitude REAL,
                                    precisao_gps REAL,
                                    data_captura_gps INTEGER,
                                    FOREIGN KEY (rota_id) REFERENCES rotas (id) ON DELETE CASCADE
                                )
                            """)
                            
                            // Copiar dados existentes se a tabela original existir
                            database.execSQL("""
                                INSERT OR IGNORE INTO clientes_temp 
                                SELECT * FROM clientes
                            """)
                            
                            // Substituir tabela original
                            database.execSQL("DROP TABLE IF EXISTS clientes")
                            database.execSQL("ALTER TABLE clientes_temp RENAME TO clientes")
                            
                            // Recriar índices
                            database.execSQL("CREATE INDEX IF NOT EXISTS index_clientes_rota_id ON clientes (rota_id)")
                            
                        } catch (e: Exception) {
                            // Se houver erro, apenas logar e continuar
                            android.util.Log.w("Migration", "Erro na migration 17_18: ${e.message}")
                        }
                    }
                }
                
                val MIGRATION_18_19 = object : androidx.room.migration.Migration(18, 19) {
                    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // Migration para corrigir os nomes das colunas de foto na tabela despesas
                        try {
                            // Verificar se as colunas com nomes incorretos existem
                            val cursor = database.query("PRAGMA table_info(despesas)")
                            val columnNames = mutableListOf<String>()
                            while (cursor.moveToNext()) {
                                columnNames.add(cursor.getString(1)) // nome da coluna
                            }
                            cursor.close()
                            
                            // Se existem colunas com nomes incorretos, corrigir
                            if (columnNames.contains("foto_comprovante") && !columnNames.contains("fotoComprovante")) {
                                // Renomear colunas incorretas para corretas
                                database.execSQL("ALTER TABLE despesas RENAME COLUMN foto_comprovante TO fotoComprovante")
                                database.execSQL("ALTER TABLE despesas RENAME COLUMN data_foto_comprovante TO dataFotoComprovante")
                                android.util.Log.d("Migration", "Colunas de foto renomeadas com sucesso")
                            }
                            
                        } catch (e: Exception) {
                            // Se houver erro, apenas logar e continuar
                            android.util.Log.w("Migration", "Erro na migration 18_19: ${e.message}")
                        }
                    }
                }
                
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19)
                    .fallbackToDestructiveMigration() // ✅ NOVO: Permite recriar banco em caso de erro de migration
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 
