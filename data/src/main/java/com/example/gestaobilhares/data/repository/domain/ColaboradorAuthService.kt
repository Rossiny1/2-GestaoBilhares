package com.example.gestaobilhares.data.repository.domain

import com.example.gestaobilhares.data.entities.Colaborador
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service especializado para coordenar o fluxo de autenticação de colaboradores.
 * 
 * Responsabilidades:
 * - Coordenar busca/criação de colaborador durante login
 * - Garantir preservação de status de aprovação
 * - Decidir qual fonte de dados usar (local vs Firestore)
 * - Sincronizar status quando necessário
 * 
 * Este service centraliza toda a lógica de decisão sobre colaboradores durante o login,
 * eliminando race conditions e garantindo uma única fonte da verdade.
 */
@Singleton
class ColaboradorAuthService @Inject constructor(
    private val colaboradorRepository: ColaboradorRepository,
    private val colaboradorFirestoreRepository: ColaboradorFirestoreRepository
) {
    
    /**
     * Processa o colaborador durante o login.
     * 
     * Fluxo:
     * 1. Verifica se existe localmente
     * 2. Se existe localmente e está aprovado, preserva e sincroniza para Firestore
     * 3. Se não existe localmente, busca do Firestore
     * 4. Se existe no Firestore mas não localmente, salva localmente
     * 5. Se não existe em nenhum lugar, cria pendente
     * 
     * @param empresaId ID da empresa
     * @param uid Firebase UID do usuário
     * @param email Email do usuário
     * @return Colaborador (criado ou existente)
     */
    suspend fun processarColaboradorNoLogin(
        empresaId: String,
        uid: String,
        email: String
    ): Colaborador {
        Timber.d("ColaboradorAuthService", "═══════════════════════════════════════")
        Timber.d("ColaboradorAuthService", "🔄 [LOGIN] Processando colaborador")
        Timber.d("ColaboradorAuthService", "   UID: $uid")
        Timber.d("ColaboradorAuthService", "   Email: $email")
        Timber.d("ColaboradorAuthService", "   Empresa: $empresaId")
        Timber.d("ColaboradorAuthService", "═══════════════════════════════════════")
        
        // ✅ PASSO 1: Verificar se existe localmente
        val colaboradorLocal = colaboradorRepository.obterPorFirebaseUid(uid) 
            ?: colaboradorRepository.obterPorEmail(email)
        
        if (colaboradorLocal != null) {
            Timber.d("ColaboradorAuthService", "✅ [LOGIN] Colaborador existe localmente")
            Timber.d("ColaboradorAuthService", "   ID: ${colaboradorLocal.id}")
            Timber.d("ColaboradorAuthService", "   Nome: ${colaboradorLocal.nome}")
            Timber.d("ColaboradorAuthService", "   Aprovado: ${colaboradorLocal.aprovado}")
            
            // Atualizar firebaseUid se necessário
            if (colaboradorLocal.firebaseUid == null || colaboradorLocal.firebaseUid != uid) {
                Timber.d("ColaboradorAuthService", "🔧 Atualizando firebaseUid")
                val atualizado = colaboradorLocal.copy(firebaseUid = uid)
                colaboradorRepository.atualizarColaborador(atualizado)
                
                // Se está aprovado, sincronizar para Firestore
                if (atualizado.aprovado) {
                    try {
                        colaboradorFirestoreRepository.sincronizarColaboradorCompleto(
                            atualizado,
                            empresaId,
                            uid,
                            preservarAprovado = true
                        )
                        Timber.d("ColaboradorAuthService", "✅ Status aprovado sincronizado para Firestore")
                    } catch (e: Exception) {
                        Timber.e(e, "❌ Erro ao sincronizar status aprovado: ${e.message}")
                    }
                }
                
                return atualizado
            }
            
            // ✅ CRÍTICO: Se está aprovado localmente, garantir que está no Firestore
            if (colaboradorLocal.aprovado) {
                try {
                    // Verificar se Firestore tem aprovado=false (conflito)
                    val colaboradorFirestore = colaboradorFirestoreRepository.getColaboradorByUid(empresaId, uid)
                    if (colaboradorFirestore != null && !colaboradorFirestore.aprovado) {
                        Timber.w("ColaboradorAuthService", "⚠️ CONFLITO: Local aprovado=true, Firestore aprovado=false")
                        Timber.w("ColaboradorAuthService", "   Sincronizando status aprovado para Firestore...")
                        colaboradorFirestoreRepository.atualizarStatusAprovacao(
                            empresaId,
                            uid,
                            aprovado = true,
                            dataAprovacao = colaboradorLocal.dataAprovacao,
                            aprovadoPor = colaboradorLocal.aprovadoPor
                        )
                    }
                } catch (e: Exception) {
                    Timber.e(e, "❌ Erro ao verificar/sincronizar Firestore: ${e.message}")
                }
            }
            
            return colaboradorLocal
        }
        
        // ✅ PASSO 2: Não existe localmente, buscar do Firestore
        Timber.d("ColaboradorAuthService", "🔍 Colaborador não existe localmente, buscando do Firestore...")
        
        val colaboradorFirestore = colaboradorFirestoreRepository.getColaboradorByUid(empresaId, uid)
        
        if (colaboradorFirestore != null) {
            Timber.d("ColaboradorAuthService", "✅ Colaborador encontrado no Firestore")
            Timber.d("ColaboradorAuthService", "   Nome: ${colaboradorFirestore.nome}")
            Timber.d("ColaboradorAuthService", "   Aprovado: ${colaboradorFirestore.aprovado}")
            
            // ✅ CRÍTICO: Verificar se existe por email (pode ter sido aprovado antes de ter UID)
            val colaboradorPorEmail = colaboradorRepository.obterPorEmail(email)
            if (colaboradorPorEmail != null && colaboradorPorEmail.aprovado && !colaboradorFirestore.aprovado) {
                Timber.w("ColaboradorAuthService", "⚠️ CONFLITO: Colaborador por email está APROVADO!")
                Timber.w("ColaboradorAuthService", "   Preservando status aprovado do local")
                
                // Usar o colaborador local aprovado
                val colaboradorPreservado = colaboradorPorEmail.copy(firebaseUid = uid)
                colaboradorRepository.atualizarColaborador(colaboradorPreservado)
                
                // Sincronizar status aprovado para Firestore
                try {
                    colaboradorFirestoreRepository.atualizarStatusAprovacao(
                        empresaId,
                        uid,
                        aprovado = true,
                        dataAprovacao = colaboradorPreservado.dataAprovacao,
                        aprovadoPor = colaboradorPreservado.aprovadoPor
                    )
                    Timber.d("ColaboradorAuthService", "✅ Status aprovado sincronizado para Firestore")
                } catch (e: Exception) {
                    Timber.e(e, "❌ Erro ao sincronizar status aprovado: ${e.message}")
                }
                
                return colaboradorPreservado
            }
            
            // Salvar do Firestore localmente
            val idLocal = colaboradorRepository.inserirColaborador(colaboradorFirestore)
            val colaboradorSalvo = colaboradorFirestore.copy(id = idLocal)
            
            Timber.d("ColaboradorAuthService", "✅ Colaborador do Firestore salvo localmente")
            return colaboradorSalvo
        }
        
        // ✅ PASSO 3: Não existe em nenhum lugar, criar pendente
        Timber.d("ColaboradorAuthService", "🔧 Colaborador não existe, criando pendente...")
        
        val colaboradorPendente = colaboradorRepository.criarColaboradorPendenteLocal(uid, email)
        
        // Criar no Firestore também
        try {
            colaboradorFirestoreRepository.criarColaboradorNoFirestore(
                colaboradorPendente,
                empresaId,
                uid
            )
            Timber.d("ColaboradorAuthService", "✅ Colaborador pendente criado no Firestore")
        } catch (e: Exception) {
            Timber.e(e, "❌ Erro ao criar no Firestore: ${e.message}")
            // Continuar mesmo se Firestore falhar (offline-first)
        }
        
        return colaboradorPendente
    }
}
