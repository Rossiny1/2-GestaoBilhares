package com.example.gestaobilhares.sync.handlers

import android.content.Context
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.data.database.AppDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

/**
 * ✅ FASE 12.14: Handler para pull de ciclos do Firestore
 * 
 * Nota: Este handler suporta múltiplas estruturas de coleção (ciclos, cicloacertos, ciclos aninhados por rota)
 */
class CicloPullHandler(
    appRepository: AppRepository,
    database: AppDatabase,
    firestore: FirebaseFirestore,
    context: Context
) : BasePullHandler(appRepository, database, firestore, context) {
    
    override suspend fun pull(empresaId: String) {
        try {
            logPullStart("ciclos", empresaId, "empresas/$empresaId/ciclos")
            
            // Tentar coleções compatíveis: primeiro "ciclos"; se vazio, tentar "cicloacertos"
            var snapshot = firestore
                .collection("empresas")
                .document(empresaId)
                .collection("ciclos")
                .get()
                .await()

            if (snapshot.isEmpty) {
                android.util.Log.w("CicloPullHandler", "Coleção 'ciclos' vazia. Tentando 'cicloacertos'...")
                snapshot = firestore
                    .collection("empresas")
                    .document(empresaId)
                    .collection("cicloacertos")
                    .get()
                    .await()
            }

            // Fallback 2: ciclos aninhados por rota: empresas/{empresaId}/rotas/{rotaId}/ciclos
            if (snapshot.isEmpty) {
                android.util.Log.w("CicloPullHandler", "Coleções 'ciclos' e 'cicloacertos' vazias. Tentando ciclos aninhados por rota...")
                val rotasImportadas = appRepository.obterTodasRotas().first()
                var insertedNested = 0
                for (rota in rotasImportadas) {
                    try {
                        val nested = firestore
                            .collection("empresas")
                            .document(empresaId)
                            .collection("rotas")
                            .document(rota.id.toString())
                            .collection("ciclos")
                            .get()
                            .await()

                        if (!nested.isEmpty) {
                            android.util.Log.d("CicloPullHandler", "NESTED rotaId=${rota.id} count=${nested.size()}")
                            for (doc in nested.documents) {
                                try {
                                    val data = doc.data ?: continue
                                    val numeroCiclo = (data["numeroCiclo"] as? Double)?.toInt() ?: (data["numeroCiclo"] as? Long)?.toInt()
                                    val statusStr = (data["status"] as? String) ?: "FINALIZADO"
                                    val ano = ((data["ano"] as? Double)?.toInt()) ?: ((data["ano"] as? Long)?.toInt()) ?: java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                                    val di = when (val v = data["dataInicio"]) { 
                                        is Number -> java.util.Date(v.toLong())
                                        is String -> if (v.isNotEmpty()) java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", java.util.Locale.ENGLISH).parse(v) else null
                                        else -> null 
                                    } ?: java.util.Date()
                                    val df = when (val v = data["dataFim"]) { 
                                        is Number -> java.util.Date(v.toLong())
                                        is String -> if (v.isNotEmpty()) java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", java.util.Locale.ENGLISH).parse(v) else null
                                        else -> null 
                                    } ?: java.util.Date()
                                    if (numeroCiclo != null) {
                                        val ciclo = com.example.gestaobilhares.data.entities.CicloAcertoEntity(
                                            rotaId = rota.id,
                                            numeroCiclo = numeroCiclo,
                                            ano = ano,
                                            dataInicio = di,
                                            dataFim = df,
                                            status = com.example.gestaobilhares.data.entities.StatusCicloAcerto.valueOf(statusStr),
                                            criadoPor = "Importado"
                                        )
                                        database.cicloAcertoDao().inserir(ciclo)
                                        insertedNested++
                                        android.util.Log.d("CicloPullHandler", "NESTED_INSERT rota=${rota.id} numero=${numeroCiclo} status=${statusStr}")
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.w("CicloPullHandler", "NESTED_ERROR rota=${rota.id} doc=${doc.id} error=${e.message}")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("CicloPullHandler", "NESTED_FETCH_ERROR rota=${rota.id} error=${e.message}")
                    }
                }
                android.util.Log.d("CicloPullHandler", "NESTED_SUMMARY inserted=$insertedNested")
            }
            
            android.util.Log.d("CicloPullHandler", "📊 Encontrados ${snapshot.size()} ciclos no Firestore")
            
            if (snapshot.isEmpty) {
                android.util.Log.w("CicloPullHandler", "⚠️ Nenhum ciclo encontrado no Firestore")
                return
            }
            
            var ciclosSincronizados = 0
            var ciclosExistentes = 0
            
            for (document in snapshot.documents) {
                try {
                    val data = document.data ?: continue
                    val roomId = (data["roomId"] as? Long) ?: (data["roomId"] as? Double)?.toLong()
                    val numeroCiclo = (data["numeroCiclo"] as? Double)?.toInt() ?: (data["numeroCiclo"] as? Long)?.toInt()
                    val rotaId = (data["rotaId"] as? Double)?.toLong() ?: (data["rotaId"] as? Long)
                    
                    android.util.Log.d("CicloPullHandler", "🔍 Processando ciclo: ${numeroCiclo}º (Room ID: $roomId, Rota ID: $rotaId)")
                    
                    if (roomId != null && numeroCiclo != null && rotaId != null) {
                        val cicloExistente = try {
                            val cicloDao = database.cicloAcertoDao()
                            cicloDao.buscarPorId(roomId)
                        } catch (e: Exception) {
                            null
                        }
                        
                        if (cicloExistente == null) {
                            val ciclo = com.example.gestaobilhares.data.entities.CicloAcertoEntity(
                                id = roomId,
                                rotaId = rotaId,
                                numeroCiclo = numeroCiclo,
                                ano = ((data["ano"] as? Double)?.toInt())
                                    ?: ((data["ano"] as? Long)?.toInt())
                                    ?: java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
                                dataInicio = try {
                                    when (val di = data["dataInicio"]) {
                                        is Number -> java.util.Date(di.toLong())
                                        is String -> if (di.isNotEmpty()) java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", java.util.Locale.ENGLISH).parse(di) else null
                                        else -> null
                                    } ?: java.util.Date()
                                } catch (e: Exception) { java.util.Date() },
                                dataFim = try {
                                    when (val df = data["dataFim"]) {
                                        is Number -> java.util.Date(df.toLong())
                                        is String -> if (df.isNotEmpty()) java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", java.util.Locale.ENGLISH).parse(df) else null
                                        else -> null
                                    } ?: java.util.Date()
                                } catch (e: Exception) { java.util.Date() },
                                status = com.example.gestaobilhares.data.entities.StatusCicloAcerto.valueOf(
                                    (data["status"] as? String) ?: "FINALIZADO"
                                ),
                                totalClientes = (data["totalClientes"] as? Double)?.toInt() ?: 0,
                                clientesAcertados = (data["clientesAcertados"] as? Double)?.toInt() ?: 0,
                                valorTotalAcertado = (data["valorTotalAcertado"] as? Double) ?: 0.0,
                                valorTotalDespesas = (data["valorTotalDespesas"] as? Double) ?: 0.0,
                                lucroLiquido = (data["lucroLiquido"] as? Double) ?: 0.0,
                                debitoTotal = (data["debitoTotal"] as? Double) ?: 0.0,
                                observacoes = data["observacoes"] as? String,
                                criadoPor = data["criadoPor"] as? String ?: "Sistema",
                                dataCriacao = try {
                                    val dataCriacaoStr = data["dataCriacao"] as? String
                                    if (dataCriacaoStr != null) {
                                        java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", java.util.Locale.ENGLISH).parse(dataCriacaoStr)
                                    } else {
                                        java.util.Date()
                                    }
                                } catch (e: Exception) {
                                    java.util.Date()
                                },
                                dataAtualizacao = java.util.Date()
                            )
                            
                            val cicloDao = database.cicloAcertoDao()
                            cicloDao.inserir(ciclo)
                            
                            ciclosSincronizados++
                            android.util.Log.d("CicloPullHandler", "✅ Ciclo sincronizado: ${ciclo.numeroCiclo}º (ID: $roomId)")
                        } else {
                            ciclosExistentes++
                            android.util.Log.d("CicloPullHandler", "⏭️ Ciclo já existe: ${cicloExistente.numeroCiclo}º (ID: $roomId)")
                        }
                    } else {
                        android.util.Log.w("CicloPullHandler", "⚠️ Ciclo sem roomId, numeroCiclo ou rotaId: ${document.id}")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("CicloPullHandler", "❌ Erro ao processar ciclo ${document.id}: ${e.message}")
                }
            }
            
            logPullSummary("Ciclos", ciclosSincronizados, ciclosExistentes)
            
        } catch (e: Exception) {
            logError("ciclos", e.message ?: "Erro desconhecido", e)
        }
    }
}

