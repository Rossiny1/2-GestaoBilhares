package com.example.gestaobilhares.data.repository.internal

import com.example.gestaobilhares.data.dao.ColaboradorDao
import com.example.gestaobilhares.data.dao.SyncQueueDao
import com.example.gestaobilhares.data.entities.Colaborador
import com.example.gestaobilhares.data.entities.NivelAcesso
import com.example.gestaobilhares.core.utils.DataEncryption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import android.util.Log

/**
 * ✅ FASE 12.14 Etapa 2: Repository interno para operações de Colaborador
 * 
 * Extraído do AppRepository para melhorar modularidade e manutenibilidade.
 * Este repository é usado internamente pelo AppRepository.
 */
internal class ColaboradorRepositoryInternal(
    private val colaboradorDao: ColaboradorDao,
    private val syncQueueDao: SyncQueueDao
) {
    
    /**
     * Obtém todos os colaboradores com descriptografia de dados sensíveis
     */
    fun obterTodosColaboradores(): Flow<List<Colaborador>> = colaboradorDao.obterTodos().map { colaboradores ->
        colaboradores.map { decryptColaborador(it) ?: it }
    }
    
    /**
     * Obtém colaboradores ativos com descriptografia
     */
    fun obterColaboradoresAtivos(): Flow<List<Colaborador>> = colaboradorDao.obterAtivos().map { colaboradores ->
        colaboradores.map { decryptColaborador(it) ?: it }
    }
    
    /**
     * Obtém colaboradores aprovados com descriptografia
     */
    fun obterColaboradoresAprovados(): Flow<List<Colaborador>> = colaboradorDao.obterAprovados().map { colaboradores ->
        colaboradores.map { decryptColaborador(it) ?: it }
    }
    
    /**
     * Obtém colaboradores pendentes de aprovação com descriptografia
     */
    fun obterColaboradoresPendentesAprovacao(): Flow<List<Colaborador>> = 
        colaboradorDao.obterPendentesAprovacao().map { colaboradores ->
            colaboradores.map { decryptColaborador(it) ?: it }
        }
    
    /**
     * Obtém colaboradores por nível de acesso com descriptografia
     */
    fun obterColaboradoresPorNivelAcesso(nivelAcesso: NivelAcesso): Flow<List<Colaborador>> = 
        colaboradorDao.obterPorNivelAcesso(nivelAcesso).map { colaboradores ->
            colaboradores.map { decryptColaborador(it) ?: it }
        }
    
    /**
     * Obtém colaborador por ID com descriptografia
     */
    suspend fun obterColaboradorPorId(id: Long): Colaborador? = decryptColaborador(colaboradorDao.obterPorId(id))
    
    /**
     * Obtém colaborador por email com descriptografia
     */
    suspend fun obterColaboradorPorEmail(email: String): Colaborador? = 
        decryptColaborador(colaboradorDao.obterPorEmail(email))
    
    /**
     * Obtém colaborador por Firebase UID com descriptografia
     */
    suspend fun obterColaboradorPorFirebaseUid(firebaseUid: String): Colaborador? = 
        decryptColaborador(colaboradorDao.obterPorFirebaseUid(firebaseUid))
    
    /**
     * Obtém colaborador por Google ID com descriptografia
     */
    suspend fun obterColaboradorPorGoogleId(googleId: String): Colaborador? = 
        decryptColaborador(colaboradorDao.obterPorGoogleId(googleId))
    
    /**
     * Insere um novo colaborador com criptografia e sincronização
     */
    suspend fun inserirColaborador(
        colaborador: Colaborador,
        logDbInsertStart: (String, String) -> Unit,
        logDbInsertSuccess: (String, String) -> Unit,
        logDbInsertError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ): Long {
        logDbInsertStart("COLABORADOR", "Nome=${colaborador.nome}, Email=${colaborador.email}, Nivel=${colaborador.nivelAcesso}")
        return try {
            // ✅ FASE 12.3: Criptografar dados sensíveis antes de salvar
            val colaboradorEncrypted = encryptColaborador(colaborador)
            val id = colaboradorDao.inserir(colaboradorEncrypted)
            logDbInsertSuccess("COLABORADOR", "Email=${colaborador.email}, ID=$id")
            
            // ✅ FASE 3C: Adicionar à fila de sincronização
            try {
                val payload = """
                    {
                        "id": $id,
                        "nome": "${colaborador.nome}",
                        "email": "${colaborador.email}",
                        "nivelAcesso": "${colaborador.nivelAcesso}",
                        "ativo": ${colaborador.ativo},
                        "aprovado": ${colaborador.aprovado},
                        "dataCadastro": "${colaborador.dataCadastro}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("Colaborador", id, "CREATE", payload, 1)
                logarOperacaoSync("Colaborador", id, "CREATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("ColaboradorRepositoryInternal", "Erro ao adicionar colaborador à fila de sync: ${syncError.message}")
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("COLABORADOR", "Email=${colaborador.email}", e)
            throw e
        }
    }
    
    /**
     * Atualiza um colaborador com criptografia e sincronização
     */
    suspend fun atualizarColaborador(
        colaborador: Colaborador,
        logDbUpdateStart: (String, String) -> Unit,
        logDbUpdateSuccess: (String, String) -> Unit,
        logDbUpdateError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ) {
        logDbUpdateStart("COLABORADOR", "ID=${colaborador.id}, Nome=${colaborador.nome}")
        try {
            // ✅ FASE 12.3: Criptografar dados sensíveis antes de salvar
            val colaboradorEncrypted = encryptColaborador(colaborador)
            colaboradorDao.atualizar(colaboradorEncrypted)
            logDbUpdateSuccess("COLABORADOR", "ID=${colaborador.id}, Nome=${colaborador.nome}")
            
            // ✅ FASE 3C: Adicionar operação UPDATE à fila de sincronização
            try {
                val payload = """
                    {
                        "id": ${colaborador.id},
                        "nome": "${colaborador.nome}",
                        "email": "${colaborador.email}",
                        "telefone": "${colaborador.telefone ?: ""}",
                        "cpf": "${colaborador.cpf ?: ""}",
                        "endereco": "${colaborador.endereco ?: ""}",
                        "bairro": "${colaborador.bairro ?: ""}",
                        "cidade": "${colaborador.cidade ?: ""}",
                        "estado": "${colaborador.estado ?: ""}",
                        "cep": "${colaborador.cep ?: ""}",
                        "rg": "${colaborador.rg ?: ""}",
                        "orgaoEmissor": "${colaborador.orgaoEmissor ?: ""}",
                        "estadoCivil": "${colaborador.estadoCivil ?: ""}",
                        "nomeMae": "${colaborador.nomeMae ?: ""}",
                        "nomePai": "${colaborador.nomePai ?: ""}",
                        "fotoPerfil": "${colaborador.fotoPerfil ?: ""}",
                        "nivelAcesso": "${colaborador.nivelAcesso.name}",
                        "ativo": ${colaborador.ativo},
                        "aprovado": ${colaborador.aprovado},
                        "dataAprovacao": "${colaborador.dataAprovacao ?: ""}",
                        "aprovadoPor": "${colaborador.aprovadoPor ?: ""}",
                        "firebaseUid": "${colaborador.firebaseUid ?: ""}",
                        "googleId": "${colaborador.googleId ?: ""}",
                        "senhaTemporaria": "${colaborador.senhaTemporaria ?: ""}",
                        "emailAcesso": "${colaborador.emailAcesso ?: ""}",
                        "observacoes": "${colaborador.observacoes ?: ""}",
                        "dataCadastro": "${colaborador.dataCadastro}",
                        "dataUltimoAcesso": "${colaborador.dataUltimoAcesso ?: ""}",
                        "dataUltimaAtualizacao": "${colaborador.dataUltimaAtualizacao}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("Colaborador", colaborador.id, "UPDATE", payload, 1)
                logarOperacaoSync("Colaborador", colaborador.id, "UPDATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("ColaboradorRepositoryInternal", "Erro ao adicionar atualização de colaborador à fila de sync: ${syncError.message}")
            }
            
        } catch (e: Exception) {
            logDbUpdateError("COLABORADOR", "ID=${colaborador.id}", e)
            throw e
        }
    }
    
    /**
     * Deleta um colaborador
     */
    suspend fun deletarColaborador(colaborador: Colaborador) = colaboradorDao.deletar(colaborador)
    
    /**
     * Aprova um colaborador
     */
    suspend fun aprovarColaborador(colaboradorId: Long, dataAprovacao: java.util.Date, aprovadoPor: String) = 
        colaboradorDao.aprovarColaborador(colaboradorId, dataAprovacao, aprovadoPor)
    
    /**
     * Aprova colaborador com credenciais
     */
    suspend fun aprovarColaboradorComCredenciais(
        colaboradorId: Long,
        email: String,
        senha: String,
        nivelAcesso: NivelAcesso,
        observacoes: String,
        dataAprovacao: java.util.Date,
        aprovadoPor: String
    ) = colaboradorDao.aprovarColaboradorComCredenciais(
        colaboradorId, email, senha, nivelAcesso, observacoes, dataAprovacao, aprovadoPor
    )
    
    /**
     * Altera status do colaborador
     */
    suspend fun alterarStatusColaborador(colaboradorId: Long, ativo: Boolean) = 
        colaboradorDao.alterarStatus(colaboradorId, ativo)
    
    /**
     * Atualiza último acesso do colaborador
     */
    suspend fun atualizarUltimoAcessoColaborador(colaboradorId: Long, dataUltimoAcesso: java.util.Date) = 
        colaboradorDao.atualizarUltimoAcesso(colaboradorId, dataUltimoAcesso)
    
    /**
     * Conta colaboradores ativos
     */
    suspend fun contarColaboradoresAtivos(): Int = colaboradorDao.contarAtivos()
    
    /**
     * Conta colaboradores pendentes de aprovação
     */
    suspend fun contarColaboradoresPendentesAprovacao(): Int = colaboradorDao.contarPendentesAprovacao()
    
    /**
     * Descriptografa um colaborador (método público para uso externo)
     */
    fun descriptografarColaborador(colaborador: Colaborador?): Colaborador? = decryptColaborador(colaborador)
    
    // ==================== MÉTODOS PRIVADOS DE CRIPTOGRAFIA ====================
    
    /**
     * Criptografa dados sensíveis de um Colaborador antes de salvar
     */
    private fun encryptColaborador(colaborador: Colaborador): Colaborador {
        return colaborador.copy(
            cpf = colaborador.cpf?.let { DataEncryption.encrypt(it) ?: it }
            // senhaTemporaria já está como hash (Fase 12.1), não precisa criptografar novamente
        )
    }
    
    /**
     * Descriptografa dados sensíveis de um Colaborador após ler
     */
    private fun decryptColaborador(colaborador: Colaborador?): Colaborador? {
        return colaborador?.copy(
            cpf = colaborador.cpf?.let { DataEncryption.decrypt(it) ?: it }
        )
    }
}

