package com.example.gestaobilhares.sync.handlers

import android.content.Context
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.data.database.AppDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * ✅ FASE 12.14: Handler para pull de mesas do Firestore
 */
class MesaPullHandler(
    appRepository: AppRepository,
    database: AppDatabase,
    firestore: FirebaseFirestore,
    context: Context
) : BasePullHandler(appRepository, database, firestore, context) {
    
    override suspend fun pull(empresaId: String) {
        try {
            logPullStart("mesas", empresaId, "empresas/$empresaId/mesas")
            
            val snapshot = firestore
                .collection("empresas")
                .document(empresaId)
                .collection("mesas")
                .get()
                .await()
            
            android.util.Log.d("MesaPullHandler", "📊 Encontradas ${snapshot.size()} mesas no Firestore")
            
            if (snapshot.isEmpty) {
                android.util.Log.w("MesaPullHandler", "⚠️ Nenhuma mesa encontrada no Firestore")
                return
            }
            
            var mesasSincronizadas = 0
            var mesasExistentes = 0
            
            for (document in snapshot.documents) {
                try {
                    val data = document.data ?: continue
                    val roomId = data["roomId"] as? Long
                    val numero = data["numero"] as? String
                    
                    android.util.Log.d("MesaPullHandler", "🔍 Processando mesa: $numero (Room ID: $roomId)")
                    
                    if (roomId != null && numero != null) {
                        // Verificar se já existe no Room
                        val mesaExistente = appRepository.obterMesaPorId(roomId)
                        
                        if (mesaExistente == null) {
                            // Criar mesa no Room baseado nos dados do Firestore
                            val mesa = com.example.gestaobilhares.data.entities.Mesa(
                                id = roomId,
                                numero = numero,
                                clienteId = (data["clienteId"] as? Double)?.toLong(),
                                ativa = data["ativa"] as? Boolean ?: true,
                                tipoMesa = com.example.gestaobilhares.data.entities.TipoMesa.valueOf(
                                    (data["tipoMesa"] as? String) ?: "SINUCA"
                                ),
                                tamanho = com.example.gestaobilhares.data.entities.TamanhoMesa.valueOf(
                                    (data["tamanho"] as? String) ?: "PEQUENA"
                                ),
                                estadoConservacao = com.example.gestaobilhares.data.entities.EstadoConservacao.valueOf(
                                    (data["estadoConservacao"] as? String) ?: "OTIMO"
                                ),
                                valorFixo = (data["valorFixo"] as? Double) ?: 0.0,
                                relogioInicial = (data["relogioInicial"] as? Double)?.toInt() ?: 0,
                                relogioFinal = (data["relogioFinal"] as? Double)?.toInt() ?: 0,
                                dataInstalacao = try {
                                    val dataInstalacaoStr = data["dataInstalacao"] as? String
                                    if (dataInstalacaoStr != null) {
                                        java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", java.util.Locale.ENGLISH).parse(dataInstalacaoStr)
                                    } else {
                                        java.util.Date()
                                    }
                                } catch (e: Exception) {
                                    java.util.Date()
                                },
                                observacoes = data["observacoes"] as? String,
                                panoAtualId = (data["panoAtualId"] as? Double)?.toLong(),
                                dataUltimaTrocaPano = try {
                                    val dataTrocaStr = data["dataUltimaTrocaPano"] as? String
                                    if (dataTrocaStr != null && dataTrocaStr.isNotEmpty()) {
                                        java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", java.util.Locale.ENGLISH).parse(dataTrocaStr)
                                    } else {
                                        null
                                    }
                                } catch (e: Exception) {
                                    null
                                }
                            )
                            
                            // Inserir no Room (sem adicionar à fila de sync)
                            val mesaDao = database.mesaDao()
                            mesaDao.inserir(mesa)
                            
                            mesasSincronizadas++
                            android.util.Log.d("MesaPullHandler", "✅ Mesa sincronizada: ${mesa.numero} (ID: $roomId)")
                        } else {
                            mesasExistentes++
                            android.util.Log.d("MesaPullHandler", "⏭️ Mesa já existe: ${mesaExistente.numero} (ID: $roomId)")
                        }
                    } else {
                        android.util.Log.w("MesaPullHandler", "⚠️ Mesa sem roomId ou numero: ${document.id}")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("MesaPullHandler", "❌ Erro ao processar mesa ${document.id}: ${e.message}")
                }
            }
            
            logPullSummary("Mesas", mesasSincronizadas, mesasExistentes)
            
        } catch (e: Exception) {
            logError("mesas", e.message ?: "Erro desconhecido", e)
        }
    }
}

