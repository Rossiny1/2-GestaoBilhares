package com.example.gestaobilhares.sync.handlers

import android.content.Context
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.data.database.AppDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

/**
 * ✅ FASE 12.14: Handler para pull de rotas do Firestore
 */
class RotaPullHandler(
    appRepository: AppRepository,
    database: AppDatabase,
    firestore: FirebaseFirestore,
    context: Context
) : BasePullHandler(appRepository, database, firestore, context) {
    
    override suspend fun pull(empresaId: String) {
        try {
            logPullStart("rotas", empresaId, "empresas/$empresaId/rotas")
            
            val snapshot = firestore
                .collection("empresas")
                .document(empresaId)
                .collection("rotas")
                .get()
                .await()
            
            android.util.Log.d("RotaPullHandler", "📊 Encontradas ${snapshot.size()} rotas no Firestore")
            
            if (snapshot.isEmpty) {
                android.util.Log.w("RotaPullHandler", "⚠️ Nenhuma rota encontrada no Firestore")
                return
            }
            
            var rotasSincronizadas = 0
            var rotasExistentes = 0
            
            for (document in snapshot.documents) {
                try {
                    val data = document.data ?: continue
                    val roomId = data["roomId"] as? Long
                    val nome = data["nome"] as? String
                    
                    android.util.Log.d("RotaPullHandler", "🔍 Processando rota: $nome (Room ID: $roomId)")
                    
                    if (roomId != null && nome != null) {
                        // Verificar se já existe no Room
                        val rotaExistente = appRepository.buscarRotaPorId(roomId)
                        
                        if (rotaExistente == null) {
                            // Criar rota no Room baseado nos dados do Firestore
                            val rota = com.example.gestaobilhares.data.entities.Rota(
                                id = roomId,
                                nome = nome,
                                descricao = data["descricao"] as? String ?: "",
                                ativa = data["ativa"] as? Boolean ?: true,
                                dataCriacao = System.currentTimeMillis()
                            )
                            
                            // Inserir no Room (sem adicionar à fila de sync)
                            val rotaDao = database.rotaDao()
                            rotaDao.insertRota(rota)
                            
                            rotasSincronizadas++
                            android.util.Log.d("RotaPullHandler", "✅ Rota sincronizada: ${rota.nome} (ID: $roomId)")
                        } else {
                            rotasExistentes++
                            android.util.Log.d("RotaPullHandler", "⏭️ Rota já existe: ${rotaExistente.nome} (ID: $roomId)")
                        }
                    } else {
                        android.util.Log.w("RotaPullHandler", "⚠️ Rota sem roomId ou nome: ${document.id}")
                        android.util.Log.w("RotaPullHandler", "   Dados disponíveis: ${data.keys}")
                        
                        // Tentar criar rota com dados mínimos se não tiver roomId
                        if (roomId == null && nome != null) {
                            android.util.Log.d("RotaPullHandler", "🔄 Tentando criar rota sem roomId: $nome")
                            try {
                                val rota = com.example.gestaobilhares.data.entities.Rota(
                                    nome = nome,
                                    descricao = data["descricao"] as? String ?: "",
                                    ativa = data["ativa"] as? Boolean ?: true,
                                    dataCriacao = System.currentTimeMillis()
                                )
                                
                                val rotaDao = database.rotaDao()
                                val novoId = rotaDao.insertRota(rota)
                                
                                rotasSincronizadas++
                                android.util.Log.d("RotaPullHandler", "✅ Rota criada sem roomId: ${rota.nome} (Novo ID: $novoId)")
                            } catch (e: Exception) {
                                android.util.Log.e("RotaPullHandler", "❌ Erro ao criar rota sem roomId: ${e.message}")
                            }
                        } else if (roomId == null && nome == null) {
                            // Rota completamente vazia - verificar se já existe uma rota com nome similar
                            android.util.Log.d("RotaPullHandler", "🔄 Rota completamente vazia. Verificando se já existe rota similar...")
                            
                            // Verificar se já existe uma rota com nome baseado no ID do documento
                            val nomeExtraido = document.id.takeIf { it.isNotBlank() } ?: "Rota Importada"
                            val rotasExistentesList = appRepository.obterTodasRotas().first()
                            val rotaSimilar = rotasExistentesList.find { it.nome.contains(nomeExtraido) || nomeExtraido.contains(it.nome) }
                            
                            if (rotaSimilar == null) {
                                try {
                                    val rota = com.example.gestaobilhares.data.entities.Rota(
                                        nome = nomeExtraido,
                                        descricao = "Rota importada do Firestore",
                                        ativa = true,
                                        dataCriacao = System.currentTimeMillis()
                                    )
                                    
                                    val rotaDao = database.rotaDao()
                                    val novoId = rotaDao.insertRota(rota)
                                    
                                    rotasSincronizadas++
                                    android.util.Log.d("RotaPullHandler", "✅ Rota criada com nome extraído: ${rota.nome} (Novo ID: $novoId)")
                                } catch (e: Exception) {
                                    android.util.Log.e("RotaPullHandler", "❌ Erro ao criar rota com nome extraído: ${e.message}")
                                }
                            } else {
                                android.util.Log.d("RotaPullHandler", "⏭️ Rota similar já existe: ${rotaSimilar.nome} (ID: ${rotaSimilar.id})")
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("RotaPullHandler", "❌ Erro ao processar rota ${document.id}: ${e.message}")
                }
            }
            
            logPullSummary("Rotas", rotasSincronizadas, rotasExistentes)
            
        } catch (e: Exception) {
            logError("rotas", e.message ?: "Erro desconhecido", e)
        }
    }
}

