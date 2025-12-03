package com.example.gestaobilhares.data.dao

import androidx.room.*
import com.example.gestaobilhares.data.entities.*
import kotlinx.coroutines.flow.Flow

/**
 * ✅ DAO EXPANDIDO - ColaboradorDao
 * Inclui operações para metas e vinculação de rotas
 */
@Dao
interface ColaboradorDao {
    
    // ==================== COLABORADOR ====================
    
    @Query("SELECT * FROM colaboradores ORDER BY nome ASC")
    fun obterTodos(): Flow<List<Colaborador>>
    
    @Query("SELECT * FROM colaboradores WHERE ativo = 1 ORDER BY nome ASC")
    fun obterAtivos(): Flow<List<Colaborador>>
    
    @Query("SELECT * FROM colaboradores WHERE aprovado = 1 AND ativo = 1 ORDER BY nome ASC")
    fun obterAprovados(): Flow<List<Colaborador>>
    
    @Query("SELECT * FROM colaboradores WHERE aprovado = 0 ORDER BY data_cadastro DESC")
    fun obterPendentesAprovacao(): Flow<List<Colaborador>>
    
    @Query("SELECT * FROM colaboradores WHERE id = :id")
    suspend fun obterPorId(id: Long): Colaborador?
    
    @Query("SELECT * FROM colaboradores WHERE email = :email")
    suspend fun obterPorEmail(email: String): Colaborador?
    
    @Query("SELECT * FROM colaboradores WHERE firebase_uid = :firebaseUid")
    suspend fun obterPorFirebaseUid(firebaseUid: String): Colaborador?
    
    @Query("SELECT * FROM colaboradores WHERE nivel_acesso = :nivelAcesso ORDER BY nome ASC")
    fun obterPorNivelAcesso(nivelAcesso: NivelAcesso): Flow<List<Colaborador>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(colaborador: Colaborador): Long
    
    @Update
    suspend fun atualizar(colaborador: Colaborador)
    
    @Delete
    suspend fun deletar(colaborador: Colaborador)
    
    @Query("UPDATE colaboradores SET aprovado = 1, data_aprovacao = :dataAprovacao, aprovado_por = :aprovadoPor WHERE id = :colaboradorId")
    suspend fun aprovarColaborador(colaboradorId: Long, dataAprovacao: java.util.Date, aprovadoPor: String)
    
    @Query("UPDATE colaboradores SET aprovado = 1, data_aprovacao = :dataAprovacao, aprovado_por = :aprovadoPor, email_acesso = :email, senha_temporaria = :senha, nivel_acesso = :nivelAcesso, observacoes = :observacoes, primeiro_acesso = 1, firebase_uid = :firebaseUid WHERE id = :colaboradorId")
    suspend fun aprovarColaboradorComCredenciais(
        colaboradorId: Long,
        email: String,
        senha: String,
        nivelAcesso: NivelAcesso,
        observacoes: String,
        dataAprovacao: java.util.Date,
        aprovadoPor: String,
        firebaseUid: String? = null
    )
    
    @Query("UPDATE colaboradores SET primeiro_acesso = 0, senha_temporaria = NULL, senha_hash = :senhaHash WHERE id = :colaboradorId")
    suspend fun marcarPrimeiroAcessoConcluido(colaboradorId: Long, senhaHash: String)
    
    @Query("UPDATE colaboradores SET ativo = :ativo WHERE id = :colaboradorId")
    suspend fun alterarStatus(colaboradorId: Long, ativo: Boolean)
    
    @Query("UPDATE colaboradores SET data_ultimo_acesso = :dataUltimoAcesso WHERE id = :colaboradorId")
    suspend fun atualizarUltimoAcesso(colaboradorId: Long, dataUltimoAcesso: java.util.Date)
    
    @Query("SELECT COUNT(*) FROM colaboradores WHERE ativo = 1")
    suspend fun contarAtivos(): Int
    
    @Query("SELECT COUNT(*) FROM colaboradores WHERE aprovado = 0")
    suspend fun contarPendentesAprovacao(): Int
    
    // ==================== META COLABORADOR ====================
    
    @Query("SELECT * FROM metas_colaborador ORDER BY data_criacao DESC")
    fun obterTodasMetaColaborador(): Flow<List<MetaColaborador>>
    
    @Query("SELECT * FROM metas_colaborador WHERE colaborador_id = :colaboradorId AND ativo = 1 ORDER BY data_criacao DESC")
    fun obterMetasPorColaborador(colaboradorId: Long): Flow<List<MetaColaborador>>
    
    @Query("SELECT * FROM metas_colaborador WHERE colaborador_id = :colaboradorId AND tipo_meta = :tipoMeta AND ativo = 1 ORDER BY data_criacao DESC LIMIT 1")
    suspend fun obterMetaAtual(colaboradorId: Long, tipoMeta: TipoMeta): MetaColaborador?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirMeta(meta: MetaColaborador): Long
    
    @Update
    suspend fun atualizarMeta(meta: MetaColaborador)
    
    @Delete
    suspend fun deletarMeta(meta: MetaColaborador)
    
    
    @Query("SELECT * FROM metas_colaborador WHERE colaborador_id = :colaboradorId AND ciclo_id = :cicloId AND ativo = 1")
    fun obterMetasPorCiclo(colaboradorId: Long, cicloId: Long): Flow<List<MetaColaborador>>
    
    @Query("SELECT * FROM metas_colaborador WHERE colaborador_id = :colaboradorId AND rota_id = :rotaId AND ativo = 1")
    fun obterMetasPorRota(colaboradorId: Long, rotaId: Long): Flow<List<MetaColaborador>>
    
    @Query("SELECT * FROM metas_colaborador WHERE colaborador_id = :colaboradorId AND ciclo_id = :cicloId AND rota_id = :rotaId AND ativo = 1")
    fun obterMetasPorCicloERota(colaboradorId: Long, cicloId: Long, rotaId: Long): Flow<List<MetaColaborador>>
    
    // ==================== COLABORADOR ROTA ====================
    
    @Query("SELECT * FROM colaborador_rotas WHERE colaborador_id = :colaboradorId")
    fun obterRotasPorColaborador(colaboradorId: Long): Flow<List<ColaboradorRota>>
    
    @Query("SELECT * FROM colaborador_rotas WHERE rota_id = :rotaId")
    fun obterColaboradoresPorRota(rotaId: Long): Flow<List<ColaboradorRota>>
    
    @Query("SELECT * FROM colaborador_rotas WHERE colaborador_id = :colaboradorId AND responsavel_principal = 1")
    suspend fun obterRotaPrincipal(colaboradorId: Long): ColaboradorRota?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirColaboradorRota(colaboradorRota: ColaboradorRota): Long
    
    @Delete
    suspend fun deletarColaboradorRota(colaboradorRota: ColaboradorRota)
    
    @Query("DELETE FROM colaborador_rotas WHERE colaborador_id = :colaboradorId")
    suspend fun deletarTodasRotasColaborador(colaboradorId: Long)
    
    @Query("UPDATE colaborador_rotas SET responsavel_principal = 0 WHERE colaborador_id = :colaboradorId")
    suspend fun removerResponsavelPrincipal(colaboradorId: Long)
    
    @Query("UPDATE colaborador_rotas SET responsavel_principal = 1 WHERE colaborador_id = :colaboradorId AND rota_id = :rotaId")
    suspend fun definirResponsavelPrincipal(colaboradorId: Long, rotaId: Long)

    @Query("SELECT COUNT(*) FROM colaborador_rotas WHERE colaborador_id = :colaboradorId")
    suspend fun contarTotalRotasColaborador(colaboradorId: Long): Int
    
    // ==================== QUERIES PARA METAS ====================
    
    /**
     * Busca metas ativas de um colaborador para um ciclo específico
     */
    @Query("SELECT * FROM metas_colaborador WHERE colaborador_id = :colaboradorId AND ciclo_id = :cicloId AND ativo = 1")
    suspend fun buscarMetasPorColaboradorECiclo(colaboradorId: Long, cicloId: Long): List<MetaColaborador>
    
    /**
     * Busca metas ativas de uma rota para um ciclo específico
     */
    @Query("""
        SELECT DISTINCT mc.* FROM metas_colaborador mc
        LEFT JOIN colaborador_rotas cr ON mc.colaborador_id = cr.colaborador_id
        WHERE mc.ciclo_id = :cicloId
          AND mc.ativo = 1
          AND (
                cr.rota_id = :rotaId
             OR mc.rota_id = :rotaId
          )
    """)
    suspend fun buscarMetasPorRotaECiclo(rotaId: Long, cicloId: Long): List<MetaColaborador>

    /**
     * Verifica se já existe meta do mesmo tipo para a mesma rota e ciclo
     */
    @Query("""
        SELECT COUNT(*) FROM metas_colaborador
        WHERE rota_id = :rotaId AND ciclo_id = :cicloId AND tipo_meta = :tipoMeta AND ativo = 1
    """)
    suspend fun contarMetasPorRotaCicloETipo(rotaId: Long, cicloId: Long, tipoMeta: TipoMeta): Int
    
    /**
     * Busca colaborador responsável principal de uma rota
     */
    @Query("""
        SELECT c.* FROM colaboradores c
        INNER JOIN colaborador_rotas cr ON c.id = cr.colaborador_id
        WHERE cr.rota_id = :rotaId AND cr.responsavel_principal = 1
        LIMIT 1
    """)
    suspend fun buscarColaboradorResponsavelPrincipal(rotaId: Long): Colaborador?
    
    /**
     * Busca todas as metas ativas de um colaborador
     */
    @Query("SELECT * FROM metas_colaborador WHERE colaborador_id = :colaboradorId AND ativo = 1 ORDER BY data_criacao DESC")
    fun buscarMetasAtivasPorColaborador(colaboradorId: Long): Flow<List<MetaColaborador>>
    
    /**
     * Busca metas por tipo e ciclo
     */
    @Query("SELECT * FROM metas_colaborador WHERE tipo_meta = :tipoMeta AND ciclo_id = :cicloId AND ativo = 1")
    suspend fun buscarMetasPorTipoECiclo(tipoMeta: TipoMeta, cicloId: Long): List<MetaColaborador>
    
    /**
     * Atualiza o valor atual de uma meta
     */
    @Query("UPDATE metas_colaborador SET valor_atual = :valorAtual WHERE id = :metaId")
    suspend fun atualizarValorAtualMeta(metaId: Long, valorAtual: Double)
    
    /**
     * Desativa todas as metas de um colaborador
     */
    @Query("UPDATE metas_colaborador SET ativo = 0 WHERE colaborador_id = :colaboradorId")
    suspend fun desativarMetasColaborador(colaboradorId: Long)
} 