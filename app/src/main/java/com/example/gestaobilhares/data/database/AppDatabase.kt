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
        MetaColaborador::class, // ✅ NOVO: METAS DOS COLABORADORES
        ColaboradorRota::class, // ✅ NOVO: VINCULAÇÃO COLABORADOR-ROTA
        Acerto::class,
        Despesa::class,
        AcertoMesa::class,
        CicloAcertoEntity::class, // ✅ FASE 8A: NOVA ENTIDADE PARA HISTÓRICO DE CICLOS
        CategoriaDespesa::class, // ✅ NOVO: CATEGORIAS DE DESPESAS
        TipoDespesa::class, // ✅ NOVO: TIPOS DE DESPESAS
        ContratoLocacao::class, // ✅ NOVO: CONTRATOS DE LOCAÇÃO
        ContratoMesa::class, // ✅ NOVO: VINCULAÇÃO CONTRATO-MESAS
        AditivoContrato::class, // ✅ NOVO: ADITIVOS DE CONTRATO
        AditivoMesa::class, // ✅ NOVO: VINCULAÇÃO ADITIVO-MESAS
        AssinaturaRepresentanteLegal::class, // ✅ NOVO: ASSINATURA DIGITAL DO REPRESENTANTE
        LogAuditoriaAssinatura::class, // ✅ NOVO: LOGS DE AUDITORIA
        ProcuraçãoRepresentante::class, // ✅ NOVO: PROCURAÇÕES E DELEGAÇÃO DE PODERES
        MesaVendida::class, // ✅ NOVO: MESAS VENDIDAS
        MesaReformada::class, // ✅ NOVO: MESAS REFORMADAS
        PanoEstoque::class, // ✅ NOVO: PANOS EM ESTOQUE
        HistoricoManutencaoMesa::class, // ✅ NOVO: HISTÓRICO DE MANUTENÇÃO DAS MESAS
        Veiculo::class, // ✅ NOVO: VEÍCULOS
        HistoricoManutencaoVeiculo::class, // ✅ NOVO: HISTÓRICO DE MANUTENÇÃO DE VEÍCULOS
        HistoricoCombustivelVeiculo::class // ✅ NOVO: HISTÓRICO DE COMBUSTÍVEL DE VEÍCULOS
    ],
    version = 37, // ✅ MIGRATION: inclusão de histórico de veículos
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
     * ✅ IMPLEMENTADO: ColaboradorDao com metas e rotas
     */
    abstract fun colaboradorDao(): com.example.gestaobilhares.data.dao.ColaboradorDao
    
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
    
    /**
     * DAO para operações com contratos de locação.
     * ✅ NOVO: CONTRATOS DE LOCAÇÃO
     */
    abstract fun contratoLocacaoDao(): com.example.gestaobilhares.data.dao.ContratoLocacaoDao
    
    /**
     * DAO para operações com aditivos de contrato.
     * ✅ NOVO: ADITIVOS DE CONTRATO
     */
    abstract fun aditivoContratoDao(): com.example.gestaobilhares.data.dao.AditivoContratoDao
    
    /**
     * DAO para operações com assinaturas do representante legal.
     * ✅ NOVO: ASSINATURA DIGITAL DO REPRESENTANTE
     */
    abstract fun assinaturaRepresentanteLegalDao(): com.example.gestaobilhares.data.dao.AssinaturaRepresentanteLegalDao
    
    /**
     * DAO para operações com logs de auditoria de assinaturas.
     * ✅ NOVO: LOGS DE AUDITORIA
     */
    abstract fun logAuditoriaAssinaturaDao(): com.example.gestaobilhares.data.dao.LogAuditoriaAssinaturaDao
    
    /**
     * DAO para operações com procurações de representantes.
     * ✅ NOVO: PROCURAÇÕES E DELEGAÇÃO DE PODERES
     */
    abstract fun procuraçãoRepresentanteDao(): com.example.gestaobilhares.data.dao.ProcuraçãoRepresentanteDao
    
    /**
     * DAO para operações com mesas vendidas.
     * ✅ NOVO: MESAS VENDIDAS
     */
    abstract fun mesaVendidaDao(): com.example.gestaobilhares.data.dao.MesaVendidaDao
    
    /**
     * DAO para operações com mesas reformadas.
     * ✅ NOVO: MESAS REFORMADAS
     */
    abstract fun mesaReformadaDao(): com.example.gestaobilhares.data.dao.MesaReformadaDao
    
    /**
     * DAO para operações com panos em estoque.
     * ✅ NOVO: PANOS EM ESTOQUE
     */
    abstract fun panoEstoqueDao(): com.example.gestaobilhares.data.dao.PanoEstoqueDao
    
    /**
     * DAO para operações com histórico de manutenção das mesas.
     * ✅ NOVO: HISTÓRICO DE MANUTENÇÃO DAS MESAS
     */
    abstract fun historicoManutencaoMesaDao(): com.example.gestaobilhares.data.dao.HistoricoManutencaoMesaDao

    // ✅ NOVO: DAO de veículos
    abstract fun veiculoDao(): com.example.gestaobilhares.data.dao.VeiculoDao
    
    // ✅ NOVO: DAO de histórico de manutenção de veículos
    abstract fun historicoManutencaoVeiculoDao(): com.example.gestaobilhares.data.dao.HistoricoManutencaoVeiculoDao
    
    // ✅ NOVO: DAO de histórico de combustível de veículos
    abstract fun historicoCombustivelVeiculoDao(): com.example.gestaobilhares.data.dao.HistoricoCombustivelVeiculoDao

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
                
                val MIGRATION_19_20 = object : androidx.room.migration.Migration(19, 20) {
                    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // Migration para adicionar novas colunas na tabela colaboradores
                        try {
                            // Adicionar novas colunas na tabela colaboradores
                            database.execSQL("ALTER TABLE colaboradores ADD COLUMN data_nascimento INTEGER")
                            database.execSQL("ALTER TABLE colaboradores ADD COLUMN endereco TEXT")
                            database.execSQL("ALTER TABLE colaboradores ADD COLUMN bairro TEXT")
                            database.execSQL("ALTER TABLE colaboradores ADD COLUMN cidade TEXT")
                            database.execSQL("ALTER TABLE colaboradores ADD COLUMN estado TEXT")
                            database.execSQL("ALTER TABLE colaboradores ADD COLUMN cep TEXT")
                            database.execSQL("ALTER TABLE colaboradores ADD COLUMN rg TEXT")
                            database.execSQL("ALTER TABLE colaboradores ADD COLUMN orgao_emissor TEXT")
                            database.execSQL("ALTER TABLE colaboradores ADD COLUMN estado_civil TEXT")
                            database.execSQL("ALTER TABLE colaboradores ADD COLUMN nome_mae TEXT")
                            database.execSQL("ALTER TABLE colaboradores ADD COLUMN nome_pai TEXT")
                            database.execSQL("ALTER TABLE colaboradores ADD COLUMN foto_perfil TEXT")
                            database.execSQL("ALTER TABLE colaboradores ADD COLUMN aprovado INTEGER DEFAULT 0")
                            database.execSQL("ALTER TABLE colaboradores ADD COLUMN data_aprovacao INTEGER")
                            database.execSQL("ALTER TABLE colaboradores ADD COLUMN aprovado_por TEXT")
                            database.execSQL("ALTER TABLE colaboradores ADD COLUMN google_id TEXT")
                            database.execSQL("ALTER TABLE colaboradores ADD COLUMN senha_temporaria TEXT")
                            database.execSQL("ALTER TABLE colaboradores ADD COLUMN data_ultima_atualizacao INTEGER")
                            
                            // Criar tabela de metas dos colaboradores
                            database.execSQL("""
                                CREATE TABLE IF NOT EXISTS metas_colaborador (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                    colaborador_id INTEGER NOT NULL,
                                    tipo_meta TEXT NOT NULL,
                                    valor_meta REAL NOT NULL,
                                    periodo_inicio INTEGER NOT NULL,
                                    periodo_fim INTEGER NOT NULL,
                                    valor_atual REAL NOT NULL DEFAULT 0.0,
                                    ativo INTEGER NOT NULL DEFAULT 1,
                                    data_criacao INTEGER NOT NULL
                                )
                            """)
                            
                            // Criar tabela de vinculação colaborador-rota
                            database.execSQL("""
                                CREATE TABLE IF NOT EXISTS colaborador_rotas (
                                    colaborador_id INTEGER NOT NULL,
                                    rota_id INTEGER NOT NULL,
                                    responsavel_principal INTEGER NOT NULL DEFAULT 0,
                                    data_vinculacao INTEGER NOT NULL,
                                    PRIMARY KEY(colaborador_id, rota_id)
                                )
                            """)
                            
                            android.util.Log.d("Migration", "Migration 19_20 executada com sucesso")
                            
                        } catch (e: Exception) {
                            // Se houver erro, apenas logar e continuar
                            android.util.Log.w("Migration", "Erro na migration 19_20: ${e.message}")
                        }
                    }
                }
                
                val MIGRATION_20_21 = object : androidx.room.migration.Migration(20, 21) {
                    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // Migration para corrigir valores padrão da tabela colaboradores
                        try {
                            // Recriar tabela colaboradores com valores padrão corretos
                            database.execSQL("""
                                CREATE TABLE IF NOT EXISTS colaboradores_temp (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                    nome TEXT NOT NULL,
                                    email TEXT NOT NULL,
                                    telefone TEXT,
                                    cpf TEXT,
                                    nivel_acesso TEXT NOT NULL DEFAULT 'USER',
                                    ativo INTEGER NOT NULL DEFAULT 1,
                                    firebase_uid TEXT,
                                    data_cadastro INTEGER NOT NULL,
                                    data_ultimo_acesso INTEGER,
                                    data_nascimento INTEGER,
                                    endereco TEXT,
                                    bairro TEXT,
                                    cidade TEXT,
                                    estado TEXT,
                                    cep TEXT,
                                    rg TEXT,
                                    orgao_emissor TEXT,
                                    estado_civil TEXT,
                                    nome_mae TEXT,
                                    nome_pai TEXT,
                                    foto_perfil TEXT,
                                    aprovado INTEGER NOT NULL DEFAULT 0,
                                    data_aprovacao INTEGER,
                                    aprovado_por TEXT,
                                    google_id TEXT,
                                    senha_temporaria TEXT,
                                    data_ultima_atualizacao INTEGER NOT NULL DEFAULT 0
                                )
                            """)
                            
                            // Copiar dados existentes
                            database.execSQL("""
                                INSERT OR IGNORE INTO colaboradores_temp 
                                SELECT * FROM colaboradores
                            """)
                            
                            // Substituir tabela original
                            database.execSQL("DROP TABLE IF EXISTS colaboradores")
                            database.execSQL("ALTER TABLE colaboradores_temp RENAME TO colaboradores")
                            
                            android.util.Log.d("Migration", "Migration 20_21 executada com sucesso")
                            
                        } catch (e: Exception) {
                            // Se houver erro, apenas logar e continuar
                            android.util.Log.w("Migration", "Erro na migration 20_21: ${e.message}")
                        }
                    }
                }
                
                val MIGRATION_21_22 = object : androidx.room.migration.Migration(21, 22) {
                    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // Migration para adicionar coluna email_acesso na tabela colaboradores
                        try {
                            // Adicionar coluna email_acesso
                            database.execSQL("ALTER TABLE colaboradores ADD COLUMN email_acesso TEXT")
                            
                            android.util.Log.d("Migration", "Migration 21_22 executada com sucesso")
                            
                        } catch (e: Exception) {
                            // Se houver erro, apenas logar e continuar
                            android.util.Log.w("Migration", "Erro na migration 21_22: ${e.message}")
                        }
                    }
                }
                
                val MIGRATION_22_23 = object : androidx.room.migration.Migration(22, 23) {
                    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // Migration para adicionar coluna observacoes na tabela colaboradores
                        try {
                            // Adicionar coluna observacoes
                            database.execSQL("ALTER TABLE colaboradores ADD COLUMN observacoes TEXT")
                            
                            android.util.Log.d("Migration", "Migration 22_23 executada com sucesso")
                            
                        } catch (e: Exception) {
                            // Se houver erro, apenas logar e continuar
                            android.util.Log.w("Migration", "Erro na migration 22_23: ${e.message}")
                        }
                    }
                }
                
                val MIGRATION_23_24 = object : androidx.room.migration.Migration(23, 24) {
                    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // Migration para atualizar tabela metas_colaborador
                        try {
                            // Criar tabela temporária com nova estrutura
                            database.execSQL("""
                                CREATE TABLE metas_colaborador_temp (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                    colaborador_id INTEGER NOT NULL,
                                    tipo_meta TEXT NOT NULL,
                                    valor_meta REAL NOT NULL,
                                    ciclo_id INTEGER NOT NULL,
                                    rota_id INTEGER,
                                    valor_atual REAL NOT NULL DEFAULT 0.0,
                                    ativo INTEGER NOT NULL DEFAULT 1,
                                    data_criacao INTEGER NOT NULL
                                )
                            """)
                            
                            // Copiar dados existentes (se houver)
                            database.execSQL("""
                                INSERT INTO metas_colaborador_temp (id, colaborador_id, tipo_meta, valor_meta, ciclo_id, rota_id, valor_atual, ativo, data_criacao)
                                SELECT id, colaborador_id, tipo_meta, valor_meta, 
                                       (SELECT id FROM ciclos_acerto WHERE ativo = 1 LIMIT 1) as ciclo_id,
                                       NULL as rota_id,
                                       valor_atual, ativo, data_criacao
                                FROM metas_colaborador
                            """)
                            
                            // Remover tabela antiga
                            database.execSQL("DROP TABLE metas_colaborador")
                            
                            // Renomear tabela temporária
                            database.execSQL("ALTER TABLE metas_colaborador_temp RENAME TO metas_colaborador")
                            
                            android.util.Log.d("Migration", "Migration 23_24 executada com sucesso")
                            
                        } catch (e: Exception) {
                            // Se houver erro, apenas logar
                            android.util.Log.e("Migration", "Erro na migration 23_24: ${e.message}")
                        }
                    }
                }
                
                val MIGRATION_24_25 = object : androidx.room.migration.Migration(24, 25) {
                    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // Migration para corrigir tabela metas_colaborador se a anterior falhou
                        try {
                            // Verificar se a tabela tem a estrutura correta
                            val cursor = database.query("PRAGMA table_info(metas_colaborador)")
                            val columns = mutableListOf<String>()
                            while (cursor.moveToNext()) {
                                columns.add(cursor.getString(1)) // nome da coluna
                            }
                            cursor.close()
                            
                            // Se ainda tem periodo_inicio e periodo_fim, corrigir
                            if (columns.contains("periodo_inicio") || columns.contains("periodo_fim")) {
                                android.util.Log.d("Migration", "Corrigindo estrutura da tabela metas_colaborador")
                                
                                // Criar tabela temporária com estrutura correta
                                database.execSQL("""
                                    CREATE TABLE metas_colaborador_temp (
                                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                        colaborador_id INTEGER NOT NULL,
                                        tipo_meta TEXT NOT NULL,
                                        valor_meta REAL NOT NULL,
                                        ciclo_id INTEGER NOT NULL,
                                        rota_id INTEGER,
                                        valor_atual REAL NOT NULL DEFAULT 0.0,
                                        ativo INTEGER NOT NULL DEFAULT 1,
                                        data_criacao INTEGER NOT NULL
                                    )
                                """)
                                
                                // Tentar copiar dados existentes
                                try {
                                    database.execSQL("""
                                        INSERT INTO metas_colaborador_temp (id, colaborador_id, tipo_meta, valor_meta, ciclo_id, rota_id, valor_atual, ativo, data_criacao)
                                        SELECT id, colaborador_id, tipo_meta, valor_meta, 
                                               (SELECT id FROM ciclos_acerto WHERE ativo = 1 LIMIT 1) as ciclo_id,
                                               NULL as rota_id,
                                               valor_atual, ativo, data_criacao
                                        FROM metas_colaborador
                                    """)
                                } catch (e: Exception) {
                                    android.util.Log.w("Migration", "Não foi possível copiar dados existentes: ${e.message}")
                                }
                                
                                // Remover tabela antiga
                                database.execSQL("DROP TABLE metas_colaborador")
                                
                                // Renomear tabela temporária
                                database.execSQL("ALTER TABLE metas_colaborador_temp RENAME TO metas_colaborador")
                            }
                            
                            android.util.Log.d("Migration", "Migration 24_25 executada com sucesso")
                            
                        } catch (e: Exception) {
                            android.util.Log.e("Migration", "Erro na migration 24_25: ${e.message}")
                        }
                    }
                }
                
                val MIGRATION_25_26 = object : androidx.room.migration.Migration(25, 26) {
                    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                        try {
                            // Adicionar novas colunas
                            database.execSQL("ALTER TABLE despesas ADD COLUMN origemLancamento TEXT NOT NULL DEFAULT 'ROTA'")
                            database.execSQL("ALTER TABLE despesas ADD COLUMN cicloAno INTEGER")
                            database.execSQL("ALTER TABLE despesas ADD COLUMN cicloNumero INTEGER")
                            
                            // Criar índices para as novas colunas
                            database.execSQL("CREATE INDEX IF NOT EXISTS index_despesas_origemLancamento ON despesas (origemLancamento)")
                            database.execSQL("CREATE INDEX IF NOT EXISTS index_despesas_cicloAno ON despesas (cicloAno)")
                            database.execSQL("CREATE INDEX IF NOT EXISTS index_despesas_cicloNumero ON despesas (cicloNumero)")
                            database.execSQL("CREATE INDEX IF NOT EXISTS index_despesas_cicloAno_cicloNumero ON despesas (cicloAno, cicloNumero)")
                            
                            android.util.Log.d("Migration", "Migration 25_26 executada com sucesso")
                        } catch (e: Exception) {
                            android.util.Log.w("Migration", "Erro na migration 25_26: ${e.message}")
                        }
                    }
                }
                
                val MIGRATION_26_27 = object : androidx.room.migration.Migration(26, 27) {
                    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                        try {
                            // Criar tabelas de contratos
                            database.execSQL("""
                                CREATE TABLE IF NOT EXISTS contratos_locacao (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                    numeroContrato TEXT NOT NULL,
                                    clienteId INTEGER NOT NULL,
                                    locadorNome TEXT NOT NULL DEFAULT 'BILHAR GLOBO R & A LTDA',
                                    locadorCnpj TEXT NOT NULL DEFAULT '34.994.884/0001-69',
                                    locadorEndereco TEXT NOT NULL DEFAULT 'Rua João Pinheiro, nº 765, Bairro Centro, Montes Claros, MG',
                                    locadorCep TEXT NOT NULL DEFAULT '39.400-093',
                                    locatarioNome TEXT NOT NULL,
                                    locatarioCpf TEXT NOT NULL,
                                    locatarioEndereco TEXT NOT NULL,
                                    locatarioTelefone TEXT NOT NULL,
                                    locatarioEmail TEXT NOT NULL,
                                    valorMensal REAL NOT NULL,
                                    diaVencimento INTEGER NOT NULL,
                                    tipoPagamento TEXT NOT NULL,
                                    percentualReceita REAL,
                                    dataContrato INTEGER NOT NULL,
                                    dataInicio INTEGER NOT NULL,
                                    status TEXT NOT NULL DEFAULT 'ATIVO',
                                    assinaturaLocador TEXT,
                                    assinaturaLocatario TEXT,
                                    dataCriacao INTEGER NOT NULL,
                                    dataAtualizacao INTEGER NOT NULL,
                                    FOREIGN KEY (clienteId) REFERENCES clientes (id) ON DELETE CASCADE
                                )
                            """)
                            
                            database.execSQL("""
                                CREATE TABLE IF NOT EXISTS contrato_mesas (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                    contratoId INTEGER NOT NULL,
                                    mesaId INTEGER NOT NULL,
                                    tipoEquipamento TEXT NOT NULL,
                                    numeroSerie TEXT NOT NULL,
                                    valorFicha REAL,
                                    valorFixo REAL,
                                    FOREIGN KEY (contratoId) REFERENCES contratos_locacao (id) ON DELETE CASCADE,
                                    FOREIGN KEY (mesaId) REFERENCES mesas (id) ON DELETE CASCADE
                                )
                            """)
                            
                            // Criar índices
                            database.execSQL("CREATE INDEX IF NOT EXISTS index_contratos_locacao_clienteId ON contratos_locacao (clienteId)")
                            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_contratos_locacao_numeroContrato ON contratos_locacao (numeroContrato)")
                            database.execSQL("CREATE INDEX IF NOT EXISTS index_contrato_mesas_contratoId ON contrato_mesas (contratoId)")
                            database.execSQL("CREATE INDEX IF NOT EXISTS index_contrato_mesas_mesaId ON contrato_mesas (mesaId)")
                            
                            android.util.Log.d("Migration", "Migration 26_27 executada com sucesso")
                        } catch (e: Exception) {
                            android.util.Log.w("Migration", "Erro na migration 26_27: ${e.message}")
                        }
                    }
                }
                
                val MIGRATION_27_28 = object : androidx.room.migration.Migration(27, 28) {
                    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                        try {
                            // Criar tabelas de aditivos
                            database.execSQL("""
                                CREATE TABLE IF NOT EXISTS aditivos_contrato (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                    numeroAditivo TEXT NOT NULL,
                                    contratoId INTEGER NOT NULL,
                                    dataAditivo INTEGER NOT NULL,
                                    observacoes TEXT,
                                    assinaturaLocador TEXT,
                                    assinaturaLocatario TEXT,
                                    dataCriacao INTEGER NOT NULL,
                                    dataAtualizacao INTEGER NOT NULL,
                                    FOREIGN KEY (contratoId) REFERENCES contratos_locacao (id) ON DELETE CASCADE
                                )
                            """)
                            
                            database.execSQL("""
                                CREATE TABLE IF NOT EXISTS aditivo_mesas (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                    aditivoId INTEGER NOT NULL,
                                    mesaId INTEGER NOT NULL,
                                    tipoEquipamento TEXT NOT NULL,
                                    numeroSerie TEXT NOT NULL,
                                    valorFicha REAL,
                                    valorFixo REAL,
                                    FOREIGN KEY (aditivoId) REFERENCES aditivos_contrato (id) ON DELETE CASCADE,
                                    FOREIGN KEY (mesaId) REFERENCES mesas (id) ON DELETE CASCADE
                                )
                            """)
                            
                            // Criar índices
                            database.execSQL("CREATE INDEX IF NOT EXISTS index_aditivos_contrato_contratoId ON aditivos_contrato (contratoId)")
                            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_aditivos_contrato_numeroAditivo ON aditivos_contrato (numeroAditivo)")
                            database.execSQL("CREATE INDEX IF NOT EXISTS index_aditivo_mesas_aditivoId ON aditivo_mesas (aditivoId)")
                            database.execSQL("CREATE INDEX IF NOT EXISTS index_aditivo_mesas_mesaId ON aditivo_mesas (mesaId)")
                            
                            android.util.Log.d("Migration", "Migration 27_28 executada com sucesso")
                        } catch (e: Exception) {
                            android.util.Log.w("Migration", "Erro na migration 27_28: ${e.message}")
                        }
                    }
                }

                val MIGRATION_28_29 = object : androidx.room.migration.Migration(28, 29) {
                    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                        try {
                            // Adicionar dataEncerramento na tabela contratos_locacao
                            val cursor1 = database.query("PRAGMA table_info(contratos_locacao)")
                            val cols1 = mutableListOf<String>()
                            while (cursor1.moveToNext()) cols1.add(cursor1.getString(1))
                            cursor1.close()
                            if (!cols1.contains("dataEncerramento")) {
                                database.execSQL("ALTER TABLE contratos_locacao ADD COLUMN dataEncerramento INTEGER")
                            }

                            // Adicionar tipo no aditivos_contrato
                            val cursor2 = database.query("PRAGMA table_info(aditivos_contrato)")
                            val cols2 = mutableListOf<String>()
                            while (cursor2.moveToNext()) cols2.add(cursor2.getString(1))
                            cursor2.close()
                            if (!cols2.contains("tipo")) {
                                database.execSQL("ALTER TABLE aditivos_contrato ADD COLUMN tipo TEXT NOT NULL DEFAULT 'INCLUSAO'")
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("Migration", "Erro na migration 28_29: ${e.message}")
                        }
                    }
                }

                val MIGRATION_29_30 = object : androidx.room.migration.Migration(29, 30) {
                    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                        try {
                            val cursor = database.query("PRAGMA table_info(contratos_locacao)")
                            val cols = mutableListOf<String>()
                            while (cursor.moveToNext()) cols.add(cursor.getString(1))
                            cursor.close()
                            if (!cols.contains("distratoAssinaturaLocador")) {
                                database.execSQL("ALTER TABLE contratos_locacao ADD COLUMN distratoAssinaturaLocador TEXT")
                            }
                            if (!cols.contains("distratoAssinaturaLocatario")) {
                                database.execSQL("ALTER TABLE contratos_locacao ADD COLUMN distratoAssinaturaLocatario TEXT")
                            }
                            if (!cols.contains("distratoDataAssinatura")) {
                                database.execSQL("ALTER TABLE contratos_locacao ADD COLUMN distratoDataAssinatura INTEGER")
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("Migration", "Erro na migration 29_30: ${e.message}")
                        }
                    }
                }
                
                
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30)
                    .fallbackToDestructiveMigration() // ✅ NOVO: Permite recriar banco em caso de erro de migration
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            super.onOpen(db)
                            // Popular dados de demonstração apenas uma vez (debug/dev)
                            try {
                                val appCtx = context.applicationContext
                                val prefs = appCtx.getSharedPreferences("gestao_bilhares_seed", android.content.Context.MODE_PRIVATE)
                                val alreadySeeded = prefs.getBoolean("db_seeded_v1", false)
                                if (!alreadySeeded) {
                                    // Rodar em thread separada para não bloquear a inicialização
                                    Thread {
                                        try {
                                            seedDatabaseIfNeeded(appCtx)
                                            prefs.edit().putBoolean("db_seeded_v1", true).apply()
                                        } catch (e: Exception) {
                                            android.util.Log.e("AppDatabase", "Erro ao popular seed: ${e.message}", e)
                                        }
                                    }.start()
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("AppDatabase", "Falha ao verificar seed: ${e.message}", e)
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private fun seedDatabaseIfNeeded(context: Context) {
            // Inserções de exemplo: 4 rotas, clientes, mesas, ciclos, acertos, acerto_mesas, categorias/tipos de despesa e algumas despesas
            val db = getDatabase(context)
            // Usar uma única transação para consistência
            db.runInTransaction {
                // 1) Rotas
                val rotaDao = db.rotaDao()
                val rotaCentroId = kotlinx.coroutines.runBlocking { rotaDao.insertRota(com.example.gestaobilhares.data.entities.Rota(nome = "Centro")) }
                val rotaNorteId = kotlinx.coroutines.runBlocking { rotaDao.insertRota(com.example.gestaobilhares.data.entities.Rota(nome = "Norte")) }
                val rotaSulId = kotlinx.coroutines.runBlocking { rotaDao.insertRota(com.example.gestaobilhares.data.entities.Rota(nome = "Sul")) }
                val rotaLesteId = kotlinx.coroutines.runBlocking { rotaDao.insertRota(com.example.gestaobilhares.data.entities.Rota(nome = "Leste")) }

                // 2) Clientes (alguns com débitos e valores de ficha)
                val clienteDao = db.clienteDao()
                val c1Id = kotlinx.coroutines.runBlocking { clienteDao.inserir(com.example.gestaobilhares.data.entities.Cliente(nome = "Bar do João", rotaId = rotaCentroId, valorFicha = 1.5, comissaoFicha = 0.0)) }
                val c2Id = kotlinx.coroutines.runBlocking { clienteDao.inserir(com.example.gestaobilhares.data.entities.Cliente(nome = "Boteco da Ana", rotaId = rotaCentroId, valorFicha = 2.0, comissaoFicha = 0.0)) }
                val c3Id = kotlinx.coroutines.runBlocking { clienteDao.inserir(com.example.gestaobilhares.data.entities.Cliente(nome = "Bar do Zé", rotaId = rotaNorteId, valorFicha = 1.75, comissaoFicha = 0.0)) }
                val c4Id = kotlinx.coroutines.runBlocking { clienteDao.inserir(com.example.gestaobilhares.data.entities.Cliente(nome = "Quiosque da Praia", rotaId = rotaSulId, valorFicha = 2.0, comissaoFicha = 0.0)) }
                val c5Id = kotlinx.coroutines.runBlocking { clienteDao.inserir(com.example.gestaobilhares.data.entities.Cliente(nome = "Bar da Esquina", rotaId = rotaLesteId, valorFicha = 1.5, comissaoFicha = 0.0)) }

                // 3) Mesas por cliente
                val mesaDao = db.mesaDao()
                val m1Id = kotlinx.coroutines.runBlocking { mesaDao.inserir(com.example.gestaobilhares.data.entities.Mesa(numero = "101", clienteId = c1Id, relogioInicial = 100, relogioFinal = 200)) }
                val m2Id = kotlinx.coroutines.runBlocking { mesaDao.inserir(com.example.gestaobilhares.data.entities.Mesa(numero = "102", clienteId = c1Id, relogioInicial = 50, relogioFinal = 120)) }
                val m3Id = kotlinx.coroutines.runBlocking { mesaDao.inserir(com.example.gestaobilhares.data.entities.Mesa(numero = "201", clienteId = c2Id, relogioInicial = 300, relogioFinal = 430)) }
                val m4Id = kotlinx.coroutines.runBlocking { mesaDao.inserir(com.example.gestaobilhares.data.entities.Mesa(numero = "301", clienteId = c3Id, relogioInicial = 0, relogioFinal = 90)) }
                val m5Id = kotlinx.coroutines.runBlocking { mesaDao.inserir(com.example.gestaobilhares.data.entities.Mesa(numero = "401", clienteId = c4Id, relogioInicial = 10, relogioFinal = 10)) }
                val m6Id = kotlinx.coroutines.runBlocking { mesaDao.inserir(com.example.gestaobilhares.data.entities.Mesa(numero = "501", clienteId = c5Id, relogioInicial = 5, relogioFinal = 65)) }

                // 4) Ciclos de acerto (2 ciclos para ano atual em algumas rotas)
                val anoAtual = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                val cicloDao = db.cicloAcertoDao()
                val cInicio1 = java.util.Calendar.getInstance().apply { set(anoAtual, 0, 1) }.time
                val cFim1 = java.util.Calendar.getInstance().apply { set(anoAtual, 0, 15) }.time
                val cInicio2 = java.util.Calendar.getInstance().apply { set(anoAtual, 0, 16) }.time
                val cFim2 = java.util.Calendar.getInstance().apply { set(anoAtual, 0, 31) }.time
                val cicloCentro1 = kotlinx.coroutines.runBlocking { cicloDao.inserir(com.example.gestaobilhares.data.entities.CicloAcertoEntity(rotaId = rotaCentroId, numeroCiclo = 1, ano = anoAtual, dataInicio = cInicio1, dataFim = cFim1)) }
                val cicloCentro2 = kotlinx.coroutines.runBlocking { cicloDao.inserir(com.example.gestaobilhares.data.entities.CicloAcertoEntity(rotaId = rotaCentroId, numeroCiclo = 2, ano = anoAtual, dataInicio = cInicio2, dataFim = cFim2)) }
                val cicloNorte1 = kotlinx.coroutines.runBlocking { cicloDao.inserir(com.example.gestaobilhares.data.entities.CicloAcertoEntity(rotaId = rotaNorteId, numeroCiclo = 1, ano = anoAtual, dataInicio = cInicio1, dataFim = cFim1)) }

                // 5) Acertos para alguns clientes
                val acertoDao = db.acertoDao()
                val acerto1Id = kotlinx.coroutines.runBlocking {
                    acertoDao.inserir(
                        com.example.gestaobilhares.data.entities.Acerto(
                            clienteId = c1Id,
                            periodoInicio = cInicio1,
                            periodoFim = cFim1,
                            totalMesas = 0.0,
                            debitoAnterior = 0.0,
                            valorTotal = 0.0,
                            desconto = 0.0,
                            valorComDesconto = 0.0,
                            valorRecebido = 500.0,
                            debitoAtual = 0.0,
                            status = com.example.gestaobilhares.data.entities.StatusAcerto.FINALIZADO,
                            observacoes = "Acerto inicial Centro",
                            rotaId = rotaCentroId,
                            cicloId = cicloCentro1
                        )
                    )
                }
                val acerto2Id = kotlinx.coroutines.runBlocking {
                    acertoDao.inserir(
                        com.example.gestaobilhares.data.entities.Acerto(
                            clienteId = c2Id,
                            periodoInicio = cInicio1,
                            periodoFim = cFim1,
                            totalMesas = 0.0,
                            debitoAnterior = 50.0,
                            valorTotal = 0.0,
                            desconto = 0.0,
                            valorComDesconto = 0.0,
                            valorRecebido = 350.0,
                            debitoAtual = 0.0,
                            status = com.example.gestaobilhares.data.entities.StatusAcerto.FINALIZADO,
                            observacoes = "Acerto ciclo 1",
                            rotaId = rotaCentroId,
                            cicloId = cicloCentro1
                        )
                    )
                }
                val acerto3Id = kotlinx.coroutines.runBlocking {
                    acertoDao.inserir(
                        com.example.gestaobilhares.data.entities.Acerto(
                            clienteId = c3Id,
                            periodoInicio = cInicio2,
                            periodoFim = cFim2,
                            totalMesas = 0.0,
                            debitoAnterior = 0.0,
                            valorTotal = 0.0,
                            desconto = 0.0,
                            valorComDesconto = 0.0,
                            valorRecebido = 420.0,
                            debitoAtual = 0.0,
                            status = com.example.gestaobilhares.data.entities.StatusAcerto.FINALIZADO,
                            observacoes = "Acerto Norte",
                            rotaId = rotaNorteId,
                            cicloId = cicloNorte1
                        )
                    )
                }

                // 6) AcertoMesas (fichas/relogios)
                val acertoMesaDao = db.acertoMesaDao()
                kotlinx.coroutines.runBlocking {
                    acertoMesaDao.inserir(com.example.gestaobilhares.data.entities.AcertoMesa(acertoId = acerto1Id, mesaId = m1Id, relogioInicial = 100, relogioFinal = 180, fichasJogadas = 80, valorFixo = 0.0, valorFicha = 1.5, comissaoFicha = 0.0, subtotal = 120.0))
                    acertoMesaDao.inserir(com.example.gestaobilhares.data.entities.AcertoMesa(acertoId = acerto1Id, mesaId = m2Id, relogioInicial = 50, relogioFinal = 100, fichasJogadas = 50, valorFixo = 0.0, valorFicha = 1.5, comissaoFicha = 0.0, subtotal = 75.0))
                    acertoMesaDao.inserir(com.example.gestaobilhares.data.entities.AcertoMesa(acertoId = acerto2Id, mesaId = m3Id, relogioInicial = 300, relogioFinal = 420, fichasJogadas = 120, valorFixo = 0.0, valorFicha = 2.0, comissaoFicha = 0.0, subtotal = 240.0))
                    acertoMesaDao.inserir(com.example.gestaobilhares.data.entities.AcertoMesa(acertoId = acerto3Id, mesaId = m4Id, relogioInicial = 0, relogioFinal = 80, fichasJogadas = 80, valorFixo = 0.0, valorFicha = 1.75, comissaoFicha = 0.0, subtotal = 140.0))
                }

                // 7) Categorias e Tipos de Despesa
                val catDao = db.categoriaDespesaDao()
                val catCombId = kotlinx.coroutines.runBlocking { catDao.inserir(com.example.gestaobilhares.data.entities.CategoriaDespesa(nome = "Combustível")) }
                val catAlimId = kotlinx.coroutines.runBlocking { catDao.inserir(com.example.gestaobilhares.data.entities.CategoriaDespesa(nome = "Alimentação")) }
                val catManuId = kotlinx.coroutines.runBlocking { catDao.inserir(com.example.gestaobilhares.data.entities.CategoriaDespesa(nome = "Manutenção")) }

                val tipoDao = db.tipoDespesaDao()
                kotlinx.coroutines.runBlocking {
                    tipoDao.inserir(com.example.gestaobilhares.data.entities.TipoDespesa(categoriaId = catCombId, nome = "Gasolina"))
                    tipoDao.inserir(com.example.gestaobilhares.data.entities.TipoDespesa(categoriaId = catAlimId, nome = "Almoço"))
                    tipoDao.inserir(com.example.gestaobilhares.data.entities.TipoDespesa(categoriaId = catManuId, nome = "Troca de pano"))
                }

                // 8) Despesas em ciclos diferentes
                val despesaDao = db.despesaDao()
                val agora = java.time.LocalDateTime.now()
                kotlinx.coroutines.runBlocking {
                    despesaDao.inserir(com.example.gestaobilhares.data.entities.Despesa(rotaId = rotaCentroId, descricao = "Gasolina rota Centro", valor = 150.0, categoria = "Combustível", tipoDespesa = "Gasolina", dataHora = agora.minusDays(20), observacoes = "", criadoPor = "seed", cicloId = cicloCentro1, origemLancamento = "ROTA", cicloAno = anoAtual, cicloNumero = 1))
                    despesaDao.inserir(com.example.gestaobilhares.data.entities.Despesa(rotaId = rotaCentroId, descricao = "Almoço equipe", valor = 80.0, categoria = "Alimentação", tipoDespesa = "Almoço", dataHora = agora.minusDays(19), observacoes = "", criadoPor = "seed", cicloId = cicloCentro1, origemLancamento = "ROTA", cicloAno = anoAtual, cicloNumero = 1))
                    despesaDao.inserir(com.example.gestaobilhares.data.entities.Despesa(rotaId = rotaNorteId, descricao = "Troca de pano mesa 301", valor = 120.0, categoria = "Manutenção", tipoDespesa = "Troca de pano", dataHora = agora.minusDays(5), observacoes = "", criadoPor = "seed", cicloId = cicloNorte1, origemLancamento = "ROTA", cicloAno = anoAtual, cicloNumero = 2))
                    // Despesa global (sem rota efetiva no cálculo por rota, mas marcada pelo ano/ciclo)
                    despesaDao.inserir(com.example.gestaobilhares.data.entities.Despesa(rotaId = rotaCentroId, descricao = "Despesa global escritório", valor = 200.0, categoria = "Outros", tipoDespesa = "Administrativo", dataHora = agora.minusDays(2), observacoes = "Global", criadoPor = "seed", cicloId = null, origemLancamento = "GLOBAL", cicloAno = anoAtual, cicloNumero = 2))
                }
            }
        }
    }
} 
