package com.example.gestaobilhares.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.example.gestaobilhares.BuildConfig
import com.example.gestaobilhares.data.dao.RotaDao
import com.example.gestaobilhares.data.dao.ClienteDao
import com.example.gestaobilhares.data.dao.DespesaDao
import com.example.gestaobilhares.data.dao.PanoMesaDao
import com.example.gestaobilhares.data.entities.*
import java.util.Date

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
        // ProcuraçãoRepresentante::class, // ✅ TEMPORARIAMENTE REMOVIDO: PROBLEMA DE ENCODING
        MesaVendida::class, // ✅ NOVO: MESAS VENDIDAS
        MesaReformada::class, // ✅ NOVO: MESAS REFORMADAS
        PanoEstoque::class, // ✅ NOVO: PANOS EM ESTOQUE
        HistoricoManutencaoMesa::class, // ✅ NOVO: HISTÓRICO DE MANUTENÇÃO DAS MESAS
        Veiculo::class, // ✅ NOVO: VEÍCULOS
        HistoricoManutencaoVeiculo::class, // ✅ NOVO: HISTÓRICO DE MANUTENÇÃO DE VEÍCULOS
        HistoricoCombustivelVeiculo::class, // ✅ NOVO: HISTÓRICO DE COMBUSTÍVEL DE VEÍCULOS
        PanoMesa::class, // ✅ NOVO: VINCULAÇÃO PANO-MESA
        com.example.gestaobilhares.data.entities.StockItem::class, // ✅ NOVO: ITENS GENÉRICOS DO ESTOQUE
        Equipment::class, // ✅ NOVO: EQUIPAMENTOS
        SyncLog::class, // ✅ FASE 3B: LOG DE SINCRONIZAÇÃO
        SyncQueue::class, // ✅ FASE 3B: FILA DE SINCRONIZAÇÃO
        SyncConfig::class // ✅ FASE 3B: CONFIGURAÇÕES DE SINCRONIZAÇÃO
    ],
    version = 44, // ✅ NOVO: Equipment adicionado
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
    // abstract fun procuraçãoRepresentanteDao(): com.example.gestaobilhares.data.dao.ProcuraçãoRepresentanteDao // ✅ TEMPORARIAMENTE REMOVIDO: PROBLEMA DE ENCODING
    
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
    
    // ✅ NOVO: DAO de vinculação pano-mesa
    abstract fun panoMesaDao(): PanoMesaDao
    
    // ✅ NOVO: DAO de itens genéricos do estoque
    abstract fun stockItemDao(): com.example.gestaobilhares.data.dao.StockItemDao
    
    // ✅ NOVO: DAO de equipamentos
    abstract fun equipmentDao(): com.example.gestaobilhares.data.dao.EquipmentDao
    
    // ✅ FASE 3B: DAOs de sincronização
    abstract fun syncLogDao(): com.example.gestaobilhares.data.dao.SyncLogDao
    abstract fun syncQueueDao(): com.example.gestaobilhares.data.dao.SyncQueueDao
    abstract fun syncConfigDao(): com.example.gestaobilhares.data.dao.SyncConfigDao

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
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("ALTER TABLE ciclos_acerto ADD COLUMN debito_total REAL NOT NULL DEFAULT 0.0")
                    }
                }
                
                val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // Limpar dados das mesas para evitar problemas com enums antigos
                        db.execSQL("DELETE FROM mesas")
                    }
                }
                
                val MIGRATION_13_14 = object : androidx.room.migration.Migration(13, 14) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // Recriar tabela clientes com schema correto
                        db.execSQL("""
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
                        db.execSQL("""
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
                        db.execSQL("DROP TABLE clientes")
                        db.execSQL("ALTER TABLE clientes_new RENAME TO clientes")
                        
                        // Recriar índice
                        db.execSQL("CREATE INDEX index_clientes_rota_id ON clientes (rota_id)")
                    }
                }
                
                val MIGRATION_14_15 = object : androidx.room.migration.Migration(14, 15) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // Adicionar campos de geolocalização na tabela clientes
                        db.execSQL("ALTER TABLE clientes ADD COLUMN latitude REAL")
                        db.execSQL("ALTER TABLE clientes ADD COLUMN longitude REAL")
                        db.execSQL("ALTER TABLE clientes ADD COLUMN precisao_gps REAL")
                        db.execSQL("ALTER TABLE clientes ADD COLUMN data_captura_gps INTEGER")
                    }
                }
                
                val MIGRATION_15_16 = object : androidx.room.migration.Migration(15, 16) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // Adicionar campos de foto do relógio na tabela acerto_mesas
                        db.execSQL("ALTER TABLE acerto_mesas ADD COLUMN foto_relogio_final TEXT")
                        db.execSQL("ALTER TABLE acerto_mesas ADD COLUMN data_foto INTEGER")
                    }
                }
                
                val MIGRATION_16_17 = object : androidx.room.migration.Migration(16, 17) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // Adicionar campos de foto do comprovante na tabela despesas
                        db.execSQL("ALTER TABLE despesas ADD COLUMN fotoComprovante TEXT")
                        db.execSQL("ALTER TABLE despesas ADD COLUMN dataFotoComprovante INTEGER")
                    }
                }
                
                val MIGRATION_17_18 = object : androidx.room.migration.Migration(17, 18) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // Migration para corrigir problemas de integridade do schema
                        // Recriar tabelas se necessário para garantir consistência
                        try {
                            // Verificar se a tabela clientes tem todos os campos necessários
                            db.execSQL("""
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
                            db.execSQL("""
                                INSERT OR IGNORE INTO clientes_temp 
                                SELECT * FROM clientes
                            """)
                            
                            // Substituir tabela original
                            db.execSQL("DROP TABLE IF EXISTS clientes")
                            db.execSQL("ALTER TABLE clientes_temp RENAME TO clientes")
                            
                            // Recriar índices
                            db.execSQL("CREATE INDEX IF NOT EXISTS index_clientes_rota_id ON clientes (rota_id)")
                            
                        } catch (e: Exception) {
                            // Se houver erro, apenas logar e continuar
                            android.util.Log.w("Migration", "Erro na migration 17_18: ${e.message}")
                        }
                    }
                }
                
                val MIGRATION_18_19 = object : androidx.room.migration.Migration(18, 19) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // Migration para corrigir os nomes das colunas de foto na tabela despesas
                        try {
                            // Verificar se as colunas com nomes incorretos existem
                            val cursor = db.query("PRAGMA table_info(despesas)")
                            val columnNames = mutableListOf<String>()
                            while (cursor.moveToNext()) {
                                columnNames.add(cursor.getString(1)) // nome da coluna
                            }
                            cursor.close()
                            
                            // Se existem colunas com nomes incorretos, corrigir
                            if (columnNames.contains("foto_comprovante") && !columnNames.contains("fotoComprovante")) {
                                // Renomear colunas incorretas para corretas
                                db.execSQL("ALTER TABLE despesas RENAME COLUMN foto_comprovante TO fotoComprovante")
                                db.execSQL("ALTER TABLE despesas RENAME COLUMN data_foto_comprovante TO dataFotoComprovante")
                                android.util.Log.d("Migration", "Colunas de foto renomeadas com sucesso")
                            }
                            
                        } catch (e: Exception) {
                            // Se houver erro, apenas logar e continuar
                            android.util.Log.w("Migration", "Erro na migration 18_19: ${e.message}")
                        }
                    }
                }
                
                val MIGRATION_19_20 = object : androidx.room.migration.Migration(19, 20) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // Migration para adicionar novas colunas na tabela colaboradores
                        try {
                            // Adicionar novas colunas na tabela colaboradores
                            db.execSQL("ALTER TABLE colaboradores ADD COLUMN data_nascimento INTEGER")
                            db.execSQL("ALTER TABLE colaboradores ADD COLUMN endereco TEXT")
                            db.execSQL("ALTER TABLE colaboradores ADD COLUMN bairro TEXT")
                            db.execSQL("ALTER TABLE colaboradores ADD COLUMN cidade TEXT")
                            db.execSQL("ALTER TABLE colaboradores ADD COLUMN estado TEXT")
                            db.execSQL("ALTER TABLE colaboradores ADD COLUMN cep TEXT")
                            db.execSQL("ALTER TABLE colaboradores ADD COLUMN rg TEXT")
                            db.execSQL("ALTER TABLE colaboradores ADD COLUMN orgao_emissor TEXT")
                            db.execSQL("ALTER TABLE colaboradores ADD COLUMN estado_civil TEXT")
                            db.execSQL("ALTER TABLE colaboradores ADD COLUMN nome_mae TEXT")
                            db.execSQL("ALTER TABLE colaboradores ADD COLUMN nome_pai TEXT")
                            db.execSQL("ALTER TABLE colaboradores ADD COLUMN foto_perfil TEXT")
                            db.execSQL("ALTER TABLE colaboradores ADD COLUMN aprovado INTEGER DEFAULT 0")
                            db.execSQL("ALTER TABLE colaboradores ADD COLUMN data_aprovacao INTEGER")
                            db.execSQL("ALTER TABLE colaboradores ADD COLUMN aprovado_por TEXT")
                            db.execSQL("ALTER TABLE colaboradores ADD COLUMN google_id TEXT")
                            db.execSQL("ALTER TABLE colaboradores ADD COLUMN senha_temporaria TEXT")
                            db.execSQL("ALTER TABLE colaboradores ADD COLUMN data_ultima_atualizacao INTEGER")
                            
                            // Criar tabela de metas dos colaboradores
                            db.execSQL("""
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
                            db.execSQL("""
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
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // Migration para corrigir valores padrão da tabela colaboradores
                        try {
                            // Recriar tabela colaboradores com valores padrão corretos
                            db.execSQL("""
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
                            db.execSQL("""
                                INSERT OR IGNORE INTO colaboradores_temp 
                                SELECT * FROM colaboradores
                            """)
                            
                            // Substituir tabela original
                            db.execSQL("DROP TABLE IF EXISTS colaboradores")
                            db.execSQL("ALTER TABLE colaboradores_temp RENAME TO colaboradores")
                            
                            android.util.Log.d("Migration", "Migration 20_21 executada com sucesso")
                            
                        } catch (e: Exception) {
                            // Se houver erro, apenas logar e continuar
                            android.util.Log.w("Migration", "Erro na migration 20_21: ${e.message}")
                        }
                    }
                }
                
                val MIGRATION_21_22 = object : androidx.room.migration.Migration(21, 22) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // Migration para adicionar coluna email_acesso na tabela colaboradores
                        try {
                            // Adicionar coluna email_acesso
                            db.execSQL("ALTER TABLE colaboradores ADD COLUMN email_acesso TEXT")
                            
                            android.util.Log.d("Migration", "Migration 21_22 executada com sucesso")
                            
                        } catch (e: Exception) {
                            // Se houver erro, apenas logar e continuar
                            android.util.Log.w("Migration", "Erro na migration 21_22: ${e.message}")
                        }
                    }
                }
                
                val MIGRATION_22_23 = object : androidx.room.migration.Migration(22, 23) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // Migration para adicionar coluna observacoes na tabela colaboradores
                        try {
                            // Adicionar coluna observacoes
                            db.execSQL("ALTER TABLE colaboradores ADD COLUMN observacoes TEXT")
                            
                            android.util.Log.d("Migration", "Migration 22_23 executada com sucesso")
                            
                        } catch (e: Exception) {
                            // Se houver erro, apenas logar e continuar
                            android.util.Log.w("Migration", "Erro na migration 22_23: ${e.message}")
                        }
                    }
                }
                
                val MIGRATION_23_24 = object : androidx.room.migration.Migration(23, 24) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // Migration para atualizar tabela metas_colaborador
                        try {
                            // Criar tabela temporária com nova estrutura
                            db.execSQL("""
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
                            db.execSQL("""
                                INSERT INTO metas_colaborador_temp (id, colaborador_id, tipo_meta, valor_meta, ciclo_id, rota_id, valor_atual, ativo, data_criacao)
                                SELECT id, colaborador_id, tipo_meta, valor_meta, 
                                       (SELECT id FROM ciclos_acerto WHERE ativo = 1 LIMIT 1) as ciclo_id,
                                       NULL as rota_id,
                                       valor_atual, ativo, data_criacao
                                FROM metas_colaborador
                            """)
                            
                            // Remover tabela antiga
                            db.execSQL("DROP TABLE metas_colaborador")
                            
                            // Renomear tabela temporária
                            db.execSQL("ALTER TABLE metas_colaborador_temp RENAME TO metas_colaborador")
                            
                            android.util.Log.d("Migration", "Migration 23_24 executada com sucesso")
                            
                        } catch (e: Exception) {
                            // Se houver erro, apenas logar
                            android.util.Log.e("Migration", "Erro na migration 23_24: ${e.message}")
                        }
                    }
                }
                
                val MIGRATION_24_25 = object : androidx.room.migration.Migration(24, 25) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // Migration para corrigir tabela metas_colaborador se a anterior falhou
                        try {
                            // Verificar se a tabela tem a estrutura correta
                            val cursor = db.query("PRAGMA table_info(metas_colaborador)")
                            val columns = mutableListOf<String>()
                            while (cursor.moveToNext()) {
                                columns.add(cursor.getString(1)) // nome da coluna
                            }
                            cursor.close()
                            
                            // Se ainda tem periodo_inicio e periodo_fim, corrigir
                            if (columns.contains("periodo_inicio") || columns.contains("periodo_fim")) {
                                android.util.Log.d("Migration", "Corrigindo estrutura da tabela metas_colaborador")
                                
                                // Criar tabela temporária com estrutura correta
                                db.execSQL("""
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
                                    db.execSQL("""
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
                                db.execSQL("DROP TABLE metas_colaborador")
                                
                                // Renomear tabela temporária
                                db.execSQL("ALTER TABLE metas_colaborador_temp RENAME TO metas_colaborador")
                            }
                            
                            android.util.Log.d("Migration", "Migration 24_25 executada com sucesso")
                            
                        } catch (e: Exception) {
                            android.util.Log.e("Migration", "Erro na migration 24_25: ${e.message}")
                        }
                    }
                }
                
                val MIGRATION_25_26 = object : androidx.room.migration.Migration(25, 26) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        try {
                            // Adicionar novas colunas
                            db.execSQL("ALTER TABLE despesas ADD COLUMN origemLancamento TEXT NOT NULL DEFAULT 'ROTA'")
                            db.execSQL("ALTER TABLE despesas ADD COLUMN cicloAno INTEGER")
                            db.execSQL("ALTER TABLE despesas ADD COLUMN cicloNumero INTEGER")
                            
                            // Criar índices para as novas colunas
                            db.execSQL("CREATE INDEX IF NOT EXISTS index_despesas_origemLancamento ON despesas (origemLancamento)")
                            db.execSQL("CREATE INDEX IF NOT EXISTS index_despesas_cicloAno ON despesas (cicloAno)")
                            db.execSQL("CREATE INDEX IF NOT EXISTS index_despesas_cicloNumero ON despesas (cicloNumero)")
                            db.execSQL("CREATE INDEX IF NOT EXISTS index_despesas_cicloAno_cicloNumero ON despesas (cicloAno, cicloNumero)")
                            
                            android.util.Log.d("Migration", "Migration 25_26 executada com sucesso")
                        } catch (e: Exception) {
                            android.util.Log.w("Migration", "Erro na migration 25_26: ${e.message}")
                        }
                    }
                }
                
                val MIGRATION_26_27 = object : androidx.room.migration.Migration(26, 27) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        try {
                            // Criar tabelas de contratos
                            db.execSQL("""
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
                            
                            db.execSQL("""
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
                            db.execSQL("CREATE INDEX IF NOT EXISTS index_contratos_locacao_clienteId ON contratos_locacao (clienteId)")
                            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_contratos_locacao_numeroContrato ON contratos_locacao (numeroContrato)")
                            db.execSQL("CREATE INDEX IF NOT EXISTS index_contrato_mesas_contratoId ON contrato_mesas (contratoId)")
                            db.execSQL("CREATE INDEX IF NOT EXISTS index_contrato_mesas_mesaId ON contrato_mesas (mesaId)")
                            
                            android.util.Log.d("Migration", "Migration 26_27 executada com sucesso")
                        } catch (e: Exception) {
                            android.util.Log.w("Migration", "Erro na migration 26_27: ${e.message}")
                        }
                    }
                }
                
                val MIGRATION_27_28 = object : androidx.room.migration.Migration(27, 28) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        try {
                            // Criar tabelas de aditivos
                            db.execSQL("""
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
                            
                            db.execSQL("""
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
                            db.execSQL("CREATE INDEX IF NOT EXISTS index_aditivos_contrato_contratoId ON aditivos_contrato (contratoId)")
                            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_aditivos_contrato_numeroAditivo ON aditivos_contrato (numeroAditivo)")
                            db.execSQL("CREATE INDEX IF NOT EXISTS index_aditivo_mesas_aditivoId ON aditivo_mesas (aditivoId)")
                            db.execSQL("CREATE INDEX IF NOT EXISTS index_aditivo_mesas_mesaId ON aditivo_mesas (mesaId)")
                            
                            android.util.Log.d("Migration", "Migration 27_28 executada com sucesso")
                        } catch (e: Exception) {
                            android.util.Log.w("Migration", "Erro na migration 27_28: ${e.message}")
                        }
                    }
                }

                val MIGRATION_28_29 = object : androidx.room.migration.Migration(28, 29) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        try {
                            // Adicionar dataEncerramento na tabela contratos_locacao
                            val cursor1 = db.query("PRAGMA table_info(contratos_locacao)")
                            val cols1 = mutableListOf<String>()
                            while (cursor1.moveToNext()) cols1.add(cursor1.getString(1))
                            cursor1.close()
                            if (!cols1.contains("dataEncerramento")) {
                                db.execSQL("ALTER TABLE contratos_locacao ADD COLUMN dataEncerramento INTEGER")
                            }

                            // Adicionar tipo no aditivos_contrato
                            val cursor2 = db.query("PRAGMA table_info(aditivos_contrato)")
                            val cols2 = mutableListOf<String>()
                            while (cursor2.moveToNext()) cols2.add(cursor2.getString(1))
                            cursor2.close()
                            if (!cols2.contains("tipo")) {
                                db.execSQL("ALTER TABLE aditivos_contrato ADD COLUMN tipo TEXT NOT NULL DEFAULT 'INCLUSAO'")
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("Migration", "Erro na migration 28_29: ${e.message}")
                        }
                    }
                }

                val MIGRATION_29_30 = object : androidx.room.migration.Migration(29, 30) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        try {
                            val cursor = db.query("PRAGMA table_info(contratos_locacao)")
                            val cols = mutableListOf<String>()
                            while (cursor.moveToNext()) cols.add(cursor.getString(1))
                            cursor.close()
                            if (!cols.contains("distratoAssinaturaLocador")) {
                                db.execSQL("ALTER TABLE contratos_locacao ADD COLUMN distratoAssinaturaLocador TEXT")
                            }
                            if (!cols.contains("distratoAssinaturaLocatario")) {
                                db.execSQL("ALTER TABLE contratos_locacao ADD COLUMN distratoAssinaturaLocatario TEXT")
                            }
                            if (!cols.contains("distratoDataAssinatura")) {
                                db.execSQL("ALTER TABLE contratos_locacao ADD COLUMN distratoDataAssinatura INTEGER")
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("Migration", "Erro na migration 29_30: ${e.message}")
                        }
                    }
                }

                // ✅ FASE 1: Migração para índices essenciais (baixo risco)
                val MIGRATION_39_40 = object : androidx.room.migration.Migration(39, 40) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        try {
                            android.util.Log.d("AppDatabase", "Migration 39→40: Criando índices essenciais para performance")
                            
                            // Criar índice para nome na tabela clientes (para ORDER BY nome)
                            db.execSQL("CREATE INDEX IF NOT EXISTS index_clientes_nome ON clientes (nome)")
                            
                            // Criar índice para data_acerto na tabela acertos (para ORDER BY data_acerto)
                            db.execSQL("CREATE INDEX IF NOT EXISTS index_acertos_data_acerto ON acertos (data_acerto)")
                            
                            android.util.Log.d("AppDatabase", "Migration 39→40: Índices essenciais criados com sucesso")
                        } catch (e: Exception) {
                            android.util.Log.w("Migration", "Erro na migration 39_40: ${e.message}")
                        }
                    }
                }

                /**
                 * ✅ FASE 2A: Migração para índice composto otimizado
                 */
                val MIGRATION_40_41 = object : androidx.room.migration.Migration(40, 41) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        try {
                            android.util.Log.d("AppDatabase", "Migration 40→41: Criando índice composto para query otimizada")
                            
                            // Criar índice composto para query otimizada (cliente_id + data_acerto)
                            db.execSQL("CREATE INDEX IF NOT EXISTS index_acertos_cliente_id_data_acerto ON acertos (cliente_id, data_acerto)")
                            
                            android.util.Log.d("AppDatabase", "Migration 40→41: Índice composto criado com sucesso")
                        } catch (e: Exception) {
                            android.util.Log.w("Migration", "Erro na migration 40_41: ${e.message}")
                        }
                    }
                }

                /**
                 * ✅ FASE 3A: Migração para campos de sincronização
                 */
                val MIGRATION_41_42 = object : androidx.room.migration.Migration(41, 42) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        try {
                            android.util.Log.d("AppDatabase", "Migration 41→42: Adicionando campos de sincronização")
                            
                            // Adicionar campos de sincronização na tabela clientes
                            db.execSQL("ALTER TABLE clientes ADD COLUMN sync_timestamp INTEGER NOT NULL DEFAULT 0")
                            db.execSQL("ALTER TABLE clientes ADD COLUMN sync_version INTEGER NOT NULL DEFAULT 1")
                            db.execSQL("ALTER TABLE clientes ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'PENDING'")
                            
                            // Adicionar campos de sincronização na tabela acertos
                            db.execSQL("ALTER TABLE acertos ADD COLUMN sync_timestamp INTEGER NOT NULL DEFAULT 0")
                            db.execSQL("ALTER TABLE acertos ADD COLUMN sync_version INTEGER NOT NULL DEFAULT 1")
                            db.execSQL("ALTER TABLE acertos ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'PENDING'")
                            
                            // Atualizar timestamps existentes
                            val currentTime = System.currentTimeMillis()
                            db.execSQL("UPDATE clientes SET sync_timestamp = $currentTime WHERE sync_timestamp = 0")
                            db.execSQL("UPDATE acertos SET sync_timestamp = $currentTime WHERE sync_timestamp = 0")
                            
                            android.util.Log.d("AppDatabase", "Migration 41→42: Campos de sincronização adicionados com sucesso")
                        } catch (e: Exception) {
                            android.util.Log.w("Migration", "Erro na migration 41_42: ${e.message}")
                        }
                    }
                }

                /**
                 * ✅ FASE 3B: Migração para entidades de sincronização
                 */
                val MIGRATION_42_43 = object : androidx.room.migration.Migration(42, 43) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        try {
                            android.util.Log.d("AppDatabase", "Migration 42→43: Criando entidades de sincronização")
                            
                            // Criar tabela sync_logs
                            db.execSQL("""
                                CREATE TABLE IF NOT EXISTS sync_logs (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                    entity_type TEXT NOT NULL,
                                    entity_id INTEGER NOT NULL,
                                    operation TEXT NOT NULL,
                                    timestamp INTEGER NOT NULL,
                                    sync_status TEXT NOT NULL,
                                    error_message TEXT,
                                    payload TEXT
                                )
                            """)
                            
                            // Criar índices para sync_logs
                            db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_logs_entity_type_entity_id ON sync_logs (entity_type, entity_id)")
                            db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_logs_sync_status ON sync_logs (sync_status)")
                            db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_logs_timestamp ON sync_logs (timestamp)")
                            db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_logs_operation ON sync_logs (operation)")
                            
                            // Criar tabela sync_queue
                            db.execSQL("""
                                CREATE TABLE IF NOT EXISTS sync_queue (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                    entity_type TEXT NOT NULL,
                                    entity_id INTEGER NOT NULL,
                                    operation TEXT NOT NULL,
                                    payload TEXT NOT NULL,
                                    created_at INTEGER NOT NULL,
                                    scheduled_for INTEGER NOT NULL,
                                    retry_count INTEGER NOT NULL DEFAULT 0,
                                    status TEXT NOT NULL DEFAULT 'PENDING',
                                    priority INTEGER NOT NULL DEFAULT 0
                                )
                            """)
                            
                            // Criar índices para sync_queue
                            db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_status_priority ON sync_queue (status, priority)")
                            db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_entity_type_entity_id ON sync_queue (entity_type, entity_id)")
                            db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_scheduled_for ON sync_queue (scheduled_for)")
                            db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_created_at ON sync_queue (created_at)")
                            db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_retry_count ON sync_queue (retry_count)")
                            
                            // Criar tabela sync_config
                            db.execSQL("""
                                CREATE TABLE IF NOT EXISTS sync_config (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                    key TEXT NOT NULL,
                                    value TEXT NOT NULL,
                                    last_updated INTEGER NOT NULL
                                )
                            """)
                            
                            // Criar índice único para sync_config
                            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sync_config_key ON sync_config (key)")
                            
                            android.util.Log.d("AppDatabase", "Migration 42→43: Entidades de sincronização criadas com sucesso")
                        } catch (e: Exception) {
                            android.util.Log.w("Migration", "Erro na migration 42_43: ${e.message}")
                        }
                    }
                }

                try {
                    val builder = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        DATABASE_NAME
                    )
                        .addMigrations(MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30, MIGRATION_39_40, MIGRATION_40_41, MIGRATION_41_42, MIGRATION_42_43)
                        .addCallback(object : RoomDatabase.Callback() {
                            override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                                super.onOpen(db)
                                android.util.Log.d("AppDatabase", "Banco de dados aberto com sucesso!")
                            }
                        })
                    
                    // Aplicar fallback destrutivo somente em builds de debug
                    val instance = if (BuildConfig.DEBUG) {
                        builder.fallbackToDestructiveMigration().build()
                    } else {
                        builder.build()
                    }
                    
                    // Banco de dados limpo - sem seed automático
                    // Os dados serão inseridos manualmente pelo usuário
                    
                    INSTANCE = instance
                    android.util.Log.d("AppDatabase", "✅ Banco de dados inicializado com sucesso")
                    instance
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "Erro crítico ao inicializar banco de dados: ${e.message}")
                    // Em caso de erro crítico, usar fallback destrutivo
                    val fallbackBuilder = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        DATABASE_NAME
                    ).fallbackToDestructiveMigration()
                    
                    val fallbackInstance = fallbackBuilder.build()
                    INSTANCE = fallbackInstance
                    android.util.Log.w("AppDatabase", "⚠️ Usando fallback destrutivo devido a erro")
                    fallbackInstance
                }
            }
        }

    }
} 
