package com.example.gestaobilhares.sync.handlers

import android.content.Context
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.data.database.AppDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

/**
 * ✅ FASE 12.14: Handler para pull de acertos do Firestore
 * 
 * Nota: Este handler é complexo pois processa também as mesas do acerto e faz download de fotos
 */
class AcertoPullHandler(
    appRepository: AppRepository,
    database: AppDatabase,
    firestore: FirebaseFirestore,
    context: Context
) : BasePullHandler(appRepository, database, firestore, context) {
    
    override suspend fun pull(empresaId: String) {
        try {
            logPullStart("acertos", empresaId, "empresas/$empresaId/acertos")
            
            val snapshot = firestore
                .collection("empresas")
                .document(empresaId)
                .collection("acertos")
                .get()
                .await()
            
            android.util.Log.d("AcertoPullHandler", "📊 Encontrados ${snapshot.size()} acertos no Firestore")
            
            if (snapshot.isEmpty) {
                android.util.Log.w("AcertoPullHandler", "⚠️ Nenhum acerto encontrado no Firestore")
                return
            }
            
            var acertosSincronizados = 0
            var acertosExistentes = 0
            
            for (document in snapshot.documents) {
                try {
                    val data = document.data ?: continue
                    val roomId = data["roomId"] as? Long
                    val valorRecebido = data["valorRecebido"] as? Double
                    
                    android.util.Log.d("AcertoPullHandler", "🔍 Processando acerto: Valor $valorRecebido (Room ID: $roomId)")
                    
                    if (roomId != null) {
                        // Verificar se já existe no Room
                        val acertoExistente = appRepository.obterAcertoPorId(roomId)
                        
                        if (acertoExistente == null) {
                            // ✅ CORREÇÃO CRÍTICA: Acertos sincronizados do Firestore devem ser FINALIZADOS
                            val statusFirestore = data["status"] as? String
                            val statusFinal = if (statusFirestore == "PENDENTE") {
                                android.util.Log.d("AcertoPullHandler", "🔄 Convertendo acerto PENDENTE para FINALIZADO (ID: $roomId)")
                                com.example.gestaobilhares.data.entities.StatusAcerto.FINALIZADO
                            } else {
                                com.example.gestaobilhares.data.entities.StatusAcerto.valueOf(statusFirestore ?: "FINALIZADO")
                            }
                            
                            // ✅ VALIDAÇÃO CRÍTICA: Verificar se já existe acerto FINALIZADO para este cliente e ciclo
                            val clienteId = (data["clienteId"] as? Double)?.toLong() ?: 0L
                            val cicloId = (data["cicloId"] as? Double)?.toLong() ?: 0L
                            
                            if (clienteId > 0 && cicloId > 0) {
                                val acertosExistentesList = appRepository.buscarAcertosPorCicloId(cicloId).first()
                                val acertoDuplicado = acertosExistentesList.any { acertoExistente -> 
                                    acertoExistente.clienteId == clienteId && 
                                    acertoExistente.status == com.example.gestaobilhares.data.entities.StatusAcerto.FINALIZADO &&
                                    acertoExistente.id != roomId
                                }
                                
                                if (acertoDuplicado) {
                                    android.util.Log.w("AcertoPullHandler", "⚠️ DUPLICATA DETECTADA: Cliente $clienteId já tem acerto FINALIZADO no ciclo $cicloId - PULANDO")
                                    continue
                                }
                            }
                            
                            // Criar acerto no Room baseado nos dados do Firestore
                            val acerto = com.example.gestaobilhares.data.entities.Acerto(
                                id = roomId,
                                clienteId = clienteId,
                                rotaId = (data["rotaId"] as? Double)?.toLong() ?: 0L,
                                periodoInicio = java.util.Date(),
                                periodoFim = java.util.Date(),
                                valorRecebido = valorRecebido ?: 0.0,
                                debitoAnterior = (data["debitoAnterior"] as? Double) ?: 0.0,
                                debitoAtual = (data["debitoAtual"] as? Double) ?: 0.0,
                                valorTotal = (data["valorTotal"] as? Double) ?: 0.0,
                                desconto = (data["desconto"] as? Double) ?: 0.0,
                                valorComDesconto = (data["valorComDesconto"] as? Double) ?: 0.0,
                                dataAcerto = java.util.Date(),
                                observacoes = data["observacoes"] as? String,
                                metodosPagamentoJson = data["metodosPagamentoJson"] as? String,
                                status = statusFinal,
                                representante = data["representante"] as? String ?: "",
                                tipoAcerto = data["tipoAcerto"] as? String ?: "Presencial",
                                panoTrocado = data["panoTrocado"] as? Boolean ?: false,
                                numeroPano = data["numeroPano"] as? String,
                                dadosExtrasJson = data["dadosExtrasJson"] as? String,
                                cicloId = cicloId,
                                totalMesas = (data["totalMesas"] as? Double) ?: 0.0
                            )
                            
                            // Inserir no Room (sem adicionar à fila de sync)
                            val acertoDao = database.acertoDao()
                            acertoDao.inserir(acerto)
                            
                            // ✅ CORREÇÃO CRÍTICA: Processar dados das mesas incluídos no payload
                            val acertoMesasData = data["acertoMesas"] as? List<Map<String, Any>>
                            if (acertoMesasData != null && acertoMesasData.isNotEmpty()) {
                                android.util.Log.d("AcertoPullHandler", "📋 Processando ${acertoMesasData.size} mesas do acerto $roomId")
                                
                                val acertoMesaDao = database.acertoMesaDao()
                                acertoMesasData.forEach { mesaData ->
                                    try {
                                        val acertoMesa = com.example.gestaobilhares.data.entities.AcertoMesa(
                                            id = (mesaData["id"] as? Double)?.toLong() ?: 0L,
                                            acertoId = roomId,
                                            mesaId = (mesaData["mesaId"] as? Double)?.toLong() ?: 0L,
                                            relogioInicial = (mesaData["relogioInicial"] as? Double)?.toInt() ?: 0,
                                            relogioFinal = (mesaData["relogioFinal"] as? Double)?.toInt() ?: 0,
                                            fichasJogadas = (mesaData["fichasJogadas"] as? Double)?.toInt() ?: 0,
                                            valorFixo = (mesaData["valorFixo"] as? Double) ?: 0.0,
                                            valorFicha = (mesaData["valorFicha"] as? Double) ?: 0.0,
                                            comissaoFicha = (mesaData["comissaoFicha"] as? Double) ?: 0.0,
                                            subtotal = (mesaData["subtotal"] as? Double) ?: 0.0,
                                            comDefeito = mesaData["comDefeito"] as? Boolean ?: false,
                                            relogioReiniciou = mesaData["relogioReiniciou"] as? Boolean ?: false,
                                            observacoes = mesaData["observacoes"] as? String,
                                            fotoRelogioFinal = null,
                                            dataFoto = null,
                                            dataCriacao = java.util.Date()
                                        )
                                        
                                        // ✅ NOVO: Download de foto do Firebase Storage se for URL
                                        val fotoUrlFirebaseMesa = mesaData["fotoRelogioFinal"] as? String
                                        val fotoRelogioLocalMesa = if (!fotoUrlFirebaseMesa.isNullOrBlank()) {
                                            try {
                                                val isUrlFirebase = com.example.gestaobilhares.core.utils.FirebaseStorageManager.isFirebaseStorageUrl(fotoUrlFirebaseMesa)
                                                if (isUrlFirebase) {
                                                    android.util.Log.d("AcertoPullHandler", "📥 Iniciando download de foto do Firebase Storage: $fotoUrlFirebaseMesa")
                                                    val caminhoLocal = com.example.gestaobilhares.core.utils.FirebaseStorageManager.downloadFoto(
                                                        context = context,
                                                        urlFirebase = fotoUrlFirebaseMesa,
                                                        tipoFoto = "relogio_final"
                                                    )
                                                    caminhoLocal
                                                } else {
                                                    null
                                                }
                                            } catch (e: Exception) {
                                                android.util.Log.e("AcertoPullHandler", "❌ Erro ao baixar foto de relógio final (mesa): ${e.message}", e)
                                                null
                                            }
                                        } else {
                                            null
                                        }
                                        
                                        // Processar dataFoto do timestamp
                                        val dataFotoTimestamp = (mesaData["dataFoto"] as? Number)?.toLong()
                                        
                                        // Atualizar acertoMesa com foto e dataFoto
                                        val acertoMesaComFoto = acertoMesa.copy(
                                            fotoRelogioFinal = fotoRelogioLocalMesa,
                                            dataFoto = dataFotoTimestamp?.let { java.util.Date(it) }
                                        )
                                        
                                        acertoMesaDao.inserir(acertoMesaComFoto)
                                        android.util.Log.d("AcertoPullHandler", "✅ Mesa ${acertoMesa.mesaId} sincronizada para acerto $roomId")
                                    } catch (e: Exception) {
                                        android.util.Log.w("AcertoPullHandler", "❌ Erro ao processar mesa do acerto: ${e.message}")
                                    }
                                }
                            }
                            
                            acertosSincronizados++
                            android.util.Log.d("AcertoPullHandler", "✅ Acerto sincronizado: Valor ${acerto.valorRecebido} (ID: $roomId)")
                        } else {
                            acertosExistentes++
                            android.util.Log.d("AcertoPullHandler", "⏭️ Acerto já existe: Valor ${acertoExistente.valorRecebido} (ID: $roomId)")
                        }
                    } else {
                        android.util.Log.w("AcertoPullHandler", "⚠️ Acerto sem roomId: ${document.id}")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("AcertoPullHandler", "❌ Erro ao processar acerto ${document.id}: ${e.message}")
                }
            }
            
            logPullSummary("Acertos", acertosSincronizados, acertosExistentes)
            
        } catch (e: Exception) {
            logError("acertos", e.message ?: "Erro desconhecido", e)
        }
    }
}

