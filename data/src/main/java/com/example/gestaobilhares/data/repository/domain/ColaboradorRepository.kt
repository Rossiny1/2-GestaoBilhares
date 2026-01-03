package com.example.gestaobilhares.data.repository.domain

import com.example.gestaobilhares.data.dao.ColaboradorDao
import com.example.gestaobilhares.data.entities.Colaborador
import com.example.gestaobilhares.data.entities.ColaboradorRota
import com.example.gestaobilhares.data.entities.NivelAcesso
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject

/**
 * Repository especializado para operações relacionadas a colaboradores (local).
 * Segue arquitetura híbrida modular: AppRepository como Facade.
 * 
 * Responsabilidades:
 * - Operações locais (Room)
 * - Busca de colaboradores
 * - Criação de colaboradores pendentes
 * - Vinculações colaborador-rota
 */
class ColaboradorRepository @Inject constructor(
    private val colaboradorDao: ColaboradorDao
) {
    
    /**
     * Obtém todos os ColaboradorRota
     * Para sincronização: busca todos os colaboradores e depois todas as rotas de cada um
     */
    suspend fun obterTodosColaboradorRotas(): List<ColaboradorRota> {
        val colaboradores = colaboradorDao.obterTodos().first()
        return colaboradores.flatMap { colaborador ->
            colaboradorDao.obterRotasPorColaborador(colaborador.id).first()
        }
    }
    
    /**
     * Obtém colaborador por Firebase UID
     */
    suspend fun obterPorFirebaseUid(firebaseUid: String): Colaborador? {
        return colaboradorDao.obterPorFirebaseUid(firebaseUid)
    }
    
    /**
     * Obtém colaborador por email
     */
    suspend fun obterPorEmail(email: String): Colaborador? {
        return colaboradorDao.obterPorEmail(email)
    }
    
    /**
     * Obtém colaborador por ID local
     */
    suspend fun obterPorId(id: Long): Colaborador? {
        return colaboradorDao.obterPorId(id)
    }
    
    /**
     * Insere colaborador no banco local (verifica duplicação antes)
     */
    suspend fun inserirColaborador(colaborador: Colaborador): Long {
        // ✅ Verificar se já existe antes de inserir (evita duplicação)
        val existente = colaborador.firebaseUid?.let { 
            obterPorFirebaseUid(it) 
        } ?: obterPorEmail(colaborador.email)
        
        if (existente != null) {
            Timber.d("ColaboradorRepository", "⚠️ Colaborador já existe localmente (ID: ${existente.id})")
            return existente.id
        }
        
        return colaboradorDao.inserir(colaborador)
    }
    
    /**
     * Atualiza colaborador no banco local
     */
    suspend fun atualizarColaborador(colaborador: Colaborador) {
        colaboradorDao.atualizar(colaborador)
    }
    
    /**
     * Cria colaborador pendente localmente
     * 
     * @param uid Firebase UID
     * @param email Email do colaborador
     * @param nome Nome do colaborador (opcional, usa email se não fornecido)
     * @return Colaborador criado
     */
    suspend fun criarColaboradorPendenteLocal(
        uid: String,
        email: String,
        nome: String? = null
    ): Colaborador {
        val agora = System.currentTimeMillis()
        val nomeFinal = nome ?: email.split("@")[0]
        val isSuperAdmin = email == "rossinys@gmail.com"
        
        val colaborador = if (isSuperAdmin) {
            // ✅ SUPERADMIN: sempre ADMIN, aprovado, sem primeiro acesso
            Colaborador(
                id = 0L,
                nome = nomeFinal,
                email = email,
                firebaseUid = uid,
                nivelAcesso = NivelAcesso.ADMIN,
                aprovado = true,
                ativo = true,
                primeiroAcesso = false,
                dataCadastro = agora,
                dataUltimaAtualizacao = agora,
                dataAprovacao = agora,
                aprovadoPor = "Sistema (Superadmin)"
            )
        } else {
            // ✅ NOVOS USUÁRIOS: aprovado=false, ativo=true (padrão)
            Colaborador(
                id = 0L,
                nome = nomeFinal,
                email = email,
                firebaseUid = uid,
                nivelAcesso = NivelAcesso.USER,
                aprovado = false,
                ativo = true,
                primeiroAcesso = true,
                dataCadastro = agora,
                dataUltimaAtualizacao = agora
            )
        }
        
        val idLocal = inserirColaborador(colaborador)
        return colaborador.copy(id = idLocal)
    }
    
    /**
     * Verifica se colaborador existe localmente (por UID ou email)
     */
    suspend fun existeLocalmente(uid: String?, email: String): Boolean {
        if (uid != null) {
            val porUid = obterPorFirebaseUid(uid)
            if (porUid != null) return true
        }
        val porEmail = obterPorEmail(email)
        return porEmail != null
    }
}

