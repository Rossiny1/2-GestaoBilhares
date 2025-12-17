package com.example.gestaobilhares.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.example.gestaobilhares.data.dao.RotaDao
import com.example.gestaobilhares.data.dao.ClienteDao
import com.example.gestaobilhares.data.dao.DespesaDao
import com.example.gestaobilhares.data.dao.PanoMesaDao
import com.example.gestaobilhares.data.dao.MetaDao // Importar o novo DAO
import com.example.gestaobilhares.data.dao.SyncOperationDao
import com.example.gestaobilhares.data.dao.SyncMetadataDao
import com.example.gestaobilhares.data.entities.*
import java.util.Date
import timber.log.Timber

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
        MetaColaborador::class, // ? NOVO: METAS DOS COLABORADORES
        ColaboradorRota::class, // ? NOVO: VINCULAÇÃO COLABORADOR-ROTA
        Acerto::class,
        Despesa::class,
        AcertoMesa::class,
        CicloAcertoEntity::class, // ? FASE 8A: NOVA ENTIDADE PARA HISTÓRICO DE CICLOS
        CategoriaDespesa::class, // ? NOVO: CATEGORIAS DE DESPESAS
        TipoDespesa::class, // ? NOVO: TIPOS DE DESPESAS
        ContratoLocacao::class, // ? NOVO: CONTRATOS DE LOCAÇÃO
        ContratoMesa::class, // ? NOVO: VINCULAÇÃO CONTRATO-MESAS
        AditivoContrato::class, // ? NOVO: ADITIVOS DE CONTRATO
        AditivoMesa::class, // ? NOVO: VINCULAÇÃO ADITIVO-MESAS
        AssinaturaRepresentanteLegal::class, // ? NOVO: ASSINATURA DIGITAL DO REPRESENTANTE
        LogAuditoriaAssinatura::class, // ? NOVO: LOGS DE AUDITORIA
        // ProcuraçãoRepresentante::class, // ? TEMPORARIAMENTE REMOVIDO: PROBLEMA DE ENCODING
        MesaVendida::class, // ? NOVO: MESAS VENDIDAS
        MesaReformada::class, // ? NOVO: MESAS REFORMADAS
        PanoEstoque::class, // ? NOVO: PANOS EM ESTOQUE
        HistoricoManutencaoMesa::class, // ? NOVO: HISTÓRICO DE MANUTENÇÃO DAS MESAS
        Veiculo::class, // ? NOVO: VEÍCULOS
        HistoricoManutencaoVeiculo::class, // ? NOVO: HISTÓRICO DE MANUTENÇÃO DE VEÍCULOS
        HistoricoCombustivelVeiculo::class, // ? NOVO: HISTÓRICO DE COMBUSTÍVEL DE VEÍCULOS
        PanoMesa::class, // ? NOVO: VINCULAÇÃO PANO-MESA
        com.example.gestaobilhares.data.entities.StockItem::class, // ? NOVO: ITENS GENÉRICOS DO ESTOQUE
        Meta::class, // Adicionar a nova entidade
        com.example.gestaobilhares.data.entities.Equipment::class, // ? NOVO: EQUIPAMENTOS
        SyncOperationEntity::class, // ? NOVO: FILA DE SINCRONIZAÇÃO OFFLINE-FIRST
        SyncMetadata::class // ? NOVO (2025): METADATA DE SINCRONIZAÇÃO INCREMENTAL
    ],
    version = 5, // ? MIGRATION: inclusão de metadata de sincronização incremental
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
     * ? IMPLEMENTADO: ColaboradorDao com metas e rotas
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
     * ? FASE 8A: NOVO DAO PARA HISTÓRICO DE CICLOS
     */
    abstract fun cicloAcertoDao(): com.example.gestaobilhares.data.dao.CicloAcertoDao
    
    /**
     * DAO para operações com categorias de despesas.
     * ? NOVO: CATEGORIAS DE DESPESAS
     */
    abstract fun categoriaDespesaDao(): com.example.gestaobilhares.data.dao.CategoriaDespesaDao
    
    /**
     * DAO para operações com tipos de despesas.
     * ? NOVO: TIPOS DE DESPESAS
     */
    abstract fun tipoDespesaDao(): com.example.gestaobilhares.data.dao.TipoDespesaDao
    
    /**
     * DAO para operações com contratos de locação.
     * ? NOVO: CONTRATOS DE LOCAÇÃO
     */
    abstract fun contratoLocacaoDao(): com.example.gestaobilhares.data.dao.ContratoLocacaoDao
    
    /**
     * DAO para operações com aditivos de contrato.
     * ? NOVO: ADITIVOS DE CONTRATO
     */
    abstract fun aditivoContratoDao(): com.example.gestaobilhares.data.dao.AditivoContratoDao
    
    /**
     * DAO para operações com assinaturas do representante legal.
     * ? NOVO: ASSINATURA DIGITAL DO REPRESENTANTE
     */
    abstract fun assinaturaRepresentanteLegalDao(): com.example.gestaobilhares.data.dao.AssinaturaRepresentanteLegalDao
    
    /**
     * DAO para operações com logs de auditoria de assinaturas.
     * ? NOVO: LOGS DE AUDITORIA
     */
    abstract fun logAuditoriaAssinaturaDao(): com.example.gestaobilhares.data.dao.LogAuditoriaAssinaturaDao
    
    /**
     * DAO para operações com procurações de representantes.
     * ? NOVO: PROCURAÇÕES E DELEGAÇÃO DE PODERES
     */
    // abstract fun procuraçãoRepresentanteDao(): com.example.gestaobilhares.data.dao.ProcuraçãoRepresentanteDao // ? TEMPORARIAMENTE REMOVIDO: PROBLEMA DE ENCODING
    
    /**
     * DAO para operações com mesas vendidas.
     * ? NOVO: MESAS VENDIDAS
     */
    abstract fun mesaVendidaDao(): com.example.gestaobilhares.data.dao.MesaVendidaDao
    
    /**
     * DAO para operações com mesas reformadas.
     * ? NOVO: MESAS REFORMADAS
     */
    abstract fun mesaReformadaDao(): com.example.gestaobilhares.data.dao.MesaReformadaDao
    
    /**
     * DAO para operações com panos em estoque.
     * ? NOVO: PANOS EM ESTOQUE
     */
    abstract fun panoEstoqueDao(): com.example.gestaobilhares.data.dao.PanoEstoqueDao
    
    /**
     * DAO para operações com histórico de manutenção das mesas.
     * ? NOVO: HISTÓRICO DE MANUTENÇÃO DAS MESAS
     */
    abstract fun historicoManutencaoMesaDao(): com.example.gestaobilhares.data.dao.HistoricoManutencaoMesaDao

    // ? NOVO: DAO de veículos
    abstract fun veiculoDao(): com.example.gestaobilhares.data.dao.VeiculoDao
    
    // ? NOVO: DAO de histórico de manutenção de veículos
    abstract fun historicoManutencaoVeiculoDao(): com.example.gestaobilhares.data.dao.HistoricoManutencaoVeiculoDao
    
    // ? NOVO: DAO de histórico de combustível de veículos
    abstract fun historicoCombustivelVeiculoDao(): com.example.gestaobilhares.data.dao.HistoricoCombustivelVeiculoDao
    
    // ? NOVO: DAO de vinculação pano-mesa
    abstract fun panoMesaDao(): PanoMesaDao
    
    // ? NOVO: DAO de itens genéricos do estoque
    abstract fun stockItemDao(): com.example.gestaobilhares.data.dao.StockItemDao

    // ? NOVO: DAO de metas
    abstract fun metaDao(): MetaDao
    
    // ? NOVO: DAO de equipamentos
    abstract fun equipmentDao(): com.example.gestaobilhares.data.dao.EquipmentDao
    
    // ? NOVO: DAO de fila de sincronização
    abstract fun syncOperationDao(): SyncOperationDao
    
    // ? NOVO (2025): DAO de metadata de sincronização incremental
    abstract fun syncMetadataDao(): SyncMetadataDao

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
                            Timber.w("Erro na migration 17_18: ${e.message}")
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
                                Timber.d("Colunas de foto renomeadas com sucesso")
                            }
                            
                        } catch (e: Exception) {
                            // Se houver erro, apenas logar e continuar
                            Timber.w("Erro na migration 18_19: ${e.message}")
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
                            
                            Timber.d("Migration 19_20 executada com sucesso")
                            
                        } catch (e: Exception) {
                            // Se houver erro, apenas logar e continuar
                            Timber.w("Erro na migration 19_20: ${e.message}")
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
                            
                            Timber.d("Migration 20_21 executada com sucesso")
                            
                        } catch (e: Exception) {
                            // Se houver erro, apenas logar e continuar
                            Timber.w("Erro na migration 20_21: ${e.message}")
                        }
                    }
                }
                
                val MIGRATION_21_22 = object : androidx.room.migration.Migration(21, 22) {
                    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // Migration para adicionar coluna email_acesso na tabela colaboradores
                        try {
                            // Adicionar coluna email_acesso
                            database.execSQL("ALTER TABLE colaboradores ADD COLUMN email_acesso TEXT")
                            
                            Timber.d("Migration 21_22 executada com sucesso")
                            
                        } catch (e: Exception) {
                            // Se houver erro, apenas logar e continuar
                            Timber.w("Erro na migration 21_22: ${e.message}")
                        }
                    }
                }
                
                val MIGRATION_22_23 = object : androidx.room.migration.Migration(22, 23) {
                    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                        // Migration para adicionar coluna observacoes na tabela colaboradores
                        try {
                            // Adicionar coluna observacoes
                            database.execSQL("ALTER TABLE colaboradores ADD COLUMN observacoes TEXT")
                            
                            Timber.d("Migration 22_23 executada com sucesso")
                            
                        } catch (e: Exception) {
                            // Se houver erro, apenas logar e continuar
                            Timber.w("Erro na migration 22_23: ${e.message}")
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
                            
                            Timber.d("Migration 23_24 executada com sucesso")
                            
                        } catch (e: Exception) {
                            // Se houver erro, apenas logar
                            Timber.e("Erro na migration 23_24: ${e.message}")
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
                                Timber.d("Corrigindo estrutura da tabela metas_colaborador")
                                
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
                                    Timber.w("Não foi possível copiar dados existentes: ${e.message}")
                                }
                                
                                // Remover tabela antiga
                                database.execSQL("DROP TABLE metas_colaborador")
                                
                                // Renomear tabela temporária
                                database.execSQL("ALTER TABLE metas_colaborador_temp RENAME TO metas_colaborador")
                            }
                            
                            Timber.d("Migration 24_25 executada com sucesso")
                            
                        } catch (e: Exception) {
                            Timber.e("Erro na migration 24_25: ${e.message}")
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
                            
                            Timber.d("Migration 25_26 executada com sucesso")
                        } catch (e: Exception) {
                            Timber.w("Erro na migration 25_26: ${e.message}")
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
                            
                            Timber.d("Migration 26_27 executada com sucesso")
                        } catch (e: Exception) {
                            Timber.w("Erro na migration 26_27: ${e.message}")
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
                            
                            Timber.d("Migration 27_28 executada com sucesso")
                        } catch (e: Exception) {
                            Timber.w("Erro na migration 27_28: ${e.message}")
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
                            Timber.d("Migration 28_29 executada com sucesso")
                        } catch (e: Exception) {
                            Timber.w("Erro na migration 28_29: ${e.message}")
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
                            Timber.w("Erro na migration 29_30: ${e.message}")
                        }
                    }
                }
                
                val MIGRATION_30_31 = object : androidx.room.migration.Migration(30, 31) {
                    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                        try {
                            // Criar tabela de fila de sincronização
                            database.execSQL("""
                                CREATE TABLE IF NOT EXISTS sync_operations (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                    operation_type TEXT NOT NULL,
                                    entity_type TEXT NOT NULL,
                                    entity_id TEXT NOT NULL,
                                    entity_data TEXT NOT NULL,
                                    timestamp INTEGER NOT NULL,
                                    retry_count INTEGER NOT NULL DEFAULT 0,
                                    max_retries INTEGER NOT NULL DEFAULT 3,
                                    status TEXT NOT NULL DEFAULT 'PENDING'
                                )
                            """)
                            
                            // Criar índice para melhor performance
                            database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_operations_status ON sync_operations (status)")
                            database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_operations_timestamp ON sync_operations (timestamp)")
                            
                            Timber.d("Migration 30_31 executada com sucesso - Tabela sync_operations criada")
                        } catch (e: Exception) {
                            Timber.w("Erro na migration 30_31: ${e.message}")
                        }
                    }
                }
                
                // Migration para versão 3 -> 4 (pular versões intermediárias se necessário)
                val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
                    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                        try {
                            // Criar tabela de fila de sincronização
                            database.execSQL("""
                                CREATE TABLE IF NOT EXISTS sync_operations (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                    operation_type TEXT NOT NULL,
                                    entity_type TEXT NOT NULL,
                                    entity_id TEXT NOT NULL,
                                    entity_data TEXT NOT NULL,
                                    timestamp INTEGER NOT NULL,
                                    retry_count INTEGER NOT NULL DEFAULT 0,
                                    max_retries INTEGER NOT NULL DEFAULT 3,
                                    status TEXT NOT NULL DEFAULT 'PENDING'
                                )
                            """)
                            
                            // Criar índices para melhor performance
                            database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_operations_status ON sync_operations (status)")
                            database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_operations_timestamp ON sync_operations (timestamp)")
                            
                            Timber.d("Migration 3_4 executada com sucesso - Tabela sync_operations criada")
                        } catch (e: Exception) {
                            Timber.w("Erro na migration 3_4: ${e.message}")
                        }
                    }
                }
                
                // ? NOVO (2025): Migration para versão 4 -> 5 (Metadata de sincronização incremental)
                val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
                    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                        try {
                            // Criar tabela de metadata de sincronização
                            database.execSQL("""
                                CREATE TABLE IF NOT EXISTS sync_metadata (
                                    entity_type TEXT PRIMARY KEY NOT NULL,
                                    last_sync_timestamp INTEGER NOT NULL DEFAULT 0,
                                    last_sync_count INTEGER NOT NULL DEFAULT 0,
                                    last_sync_duration_ms INTEGER NOT NULL DEFAULT 0,
                                    last_sync_bytes_downloaded INTEGER NOT NULL DEFAULT 0,
                                    last_sync_bytes_uploaded INTEGER NOT NULL DEFAULT 0,
                                    last_error TEXT,
                                    updated_at INTEGER NOT NULL DEFAULT 0
                                )
                            """)
                            
                            // Criar índice para melhor performance
                            database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_metadata_entity_type ON sync_metadata (entity_type)")
                            
                            Timber.d("Migration 4_5 executada com sucesso - Tabela sync_metadata criada")
                        } catch (e: Exception) {
                            Timber.w("Erro na migration 4_5: ${e.message}", e)
                        }
                    }
                }
                
                try {
                    val builder = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        DATABASE_NAME
                    )
                        .addMigrations(MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30, MIGRATION_30_31, MIGRATION_3_4, MIGRATION_4_5)
                        .addCallback(object : RoomDatabase.Callback() {
                            override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                                super.onOpen(db)
                                Timber.d("Banco de dados aberto com sucesso!")
                            }
                        })
                    
                    // Aplicar fallback destrutivo somente em builds de debug
                    // Nota: BuildConfig não está disponível em módulos de biblioteca
                    // Usar fallback destrutivo sempre para desenvolvimento
                    val instance = builder.fallbackToDestructiveMigration().build()
                    
                    // Banco de dados limpo - sem seed automático
                    // Os dados serão inseridos manualmente pelo usuário
                    
                    INSTANCE = instance
                    Timber.d("? Banco de dados inicializado com sucesso")
                    instance
                } catch (e: Exception) {
                    Timber.e("Erro crítico ao inicializar banco de dados: ${e.message}")
                    // Em caso de erro crítico, usar fallback destrutivo
                    val fallbackBuilder = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        DATABASE_NAME
                    ).fallbackToDestructiveMigration()
                    
                    val fallbackInstance = fallbackBuilder.build()
                    INSTANCE = fallbackInstance
                    Timber.w("?? Usando fallback destrutivo devido a erro")
                    fallbackInstance
                }
            }
        }

    }
} 
