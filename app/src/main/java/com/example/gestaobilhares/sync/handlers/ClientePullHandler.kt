package com.example.gestaobilhares.sync.handlers

import android.content.Context
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.data.database.AppDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

/**
 * ✅ FASE 12.14: Handler para pull de clientes do Firestore
 */
class ClientePullHandler(
    appRepository: AppRepository,
    database: AppDatabase,
    firestore: FirebaseFirestore,
    context: Context
) : BasePullHandler(appRepository, database, firestore, context) {
    
    override suspend fun pull(empresaId: String) {
        try {
            logPullStart("clientes", empresaId, "empresas/$empresaId/clientes")
            
            val snapshot = firestore
                .collection("empresas")
                .document(empresaId)
                .collection("clientes")
                .get()
                .await()
            
            android.util.Log.d("ClientePullHandler", "📊 Encontrados ${snapshot.size()} clientes no Firestore")
            
            if (snapshot.isEmpty) {
                android.util.Log.w("ClientePullHandler", "⚠️ Nenhum cliente encontrado no Firestore")
                return
            }
            
            var clientesSincronizados = 0
            var clientesExistentes = 0
            
            for (document in snapshot.documents) {
                try {
                    val data = document.data ?: continue
                    val roomId = data["roomId"] as? Long
                    val nome = data["nome"] as? String
                    
                    android.util.Log.d("ClientePullHandler", "🔍 Processando cliente: $nome (Room ID: $roomId)")
                    
                    if (roomId != null && nome != null) {
                        // Verificar se já existe no Room
                        val clienteExistente = appRepository.obterClientePorId(roomId)
                        
                        if (clienteExistente == null) {
                            // Obter rotaId válido
                            val rotaIdCliente = (data["rotaId"] as? Double)?.toLong()
                            val rotaIdFinal = if (rotaIdCliente != null) {
                                // Verificar se a rota existe
                                val rotaExiste = appRepository.buscarRotaPorId(rotaIdCliente)
                                if (rotaExiste != null) {
                                    rotaIdCliente
                                } else {
                                    // Usar primeira rota disponível
                                    val rotas = appRepository.obterTodasRotas().first()
                                    if (rotas.isNotEmpty()) {
                                        android.util.Log.w("ClientePullHandler", "⚠️ Rota $rotaIdCliente não existe. Usando primeira rota disponível: ${rotas.first().id}")
                                        rotas.first().id
                                    } else {
                                        android.util.Log.w("ClientePullHandler", "⚠️ Nenhuma rota disponível. Usando ID 1")
                                        1L
                                    }
                                }
                            } else {
                                // Usar primeira rota disponível
                                val rotas = appRepository.obterTodasRotas().first()
                                if (rotas.isNotEmpty()) {
                                    android.util.Log.w("ClientePullHandler", "⚠️ Cliente sem rotaId. Usando primeira rota disponível: ${rotas.first().id}")
                                    rotas.first().id
                                } else {
                                    android.util.Log.w("ClientePullHandler", "⚠️ Nenhuma rota disponível. Usando ID 1")
                                    1L
                                }
                            }
                            
                            // Criar cliente no Room baseado nos dados do Firestore
                            val cliente = com.example.gestaobilhares.data.entities.Cliente(
                                id = roomId,
                                nome = nome,
                                telefone = data["telefone"] as? String,
                                endereco = data["endereco"] as? String ?: "",
                                rotaId = rotaIdFinal,
                                ativo = data["ativo"] as? Boolean ?: true,
                                dataCadastro = java.util.Date(), // Usar data atual como fallback
                                valorFicha = (data["valorFicha"] as? Double) ?: 0.0,
                                comissaoFicha = (data["comissaoFicha"] as? Double) ?: 0.0
                            )
                            
                            // Inserir no Room (sem adicionar à fila de sync)
                            val clienteDao = database.clienteDao()
                            clienteDao.inserir(cliente)
                            
                            clientesSincronizados++
                            android.util.Log.d("ClientePullHandler", "✅ Cliente sincronizado: ${cliente.nome} (ID: $roomId)")
                        } else {
                            clientesExistentes++
                            android.util.Log.d("ClientePullHandler", "⏭️ Cliente já existe: ${clienteExistente.nome} (ID: $roomId)")
                        }
                    } else {
                        android.util.Log.w("ClientePullHandler", "⚠️ Cliente sem roomId ou nome: ${document.id}")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("ClientePullHandler", "❌ Erro ao processar cliente ${document.id}: ${e.message}")
                }
            }
            
            logPullSummary("Clientes", clientesSincronizados, clientesExistentes)
            
        } catch (e: Exception) {
            logError("clientes", e.message ?: "Erro desconhecido", e)
        }
    }
}

