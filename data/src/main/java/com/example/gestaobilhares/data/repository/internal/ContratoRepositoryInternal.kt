package com.example.gestaobilhares.data.repository.internal

import com.example.gestaobilhares.data.dao.*
import com.example.gestaobilhares.data.entities.*
import com.example.gestaobilhares.core.utils.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import android.util.Log

/**
 * ✅ FASE 12.14 Etapa 3: Repository interno para operações de Contratos
 * 
 * Extraído do AppRepository para melhorar modularidade e manutenibilidade.
 * Inclui: ContratoLocacao, ContratoMesa, AditivoContrato, AditivoMesa,
 * AssinaturaRepresentanteLegal, LogAuditoriaAssinatura
 */
internal class ContratoRepositoryInternal(
    private val contratoLocacaoDao: ContratoLocacaoDao,
    private val aditivoContratoDao: AditivoContratoDao,
    private val assinaturaRepresentanteLegalDao: AssinaturaRepresentanteLegalDao,
    private val logAuditoriaAssinaturaDao: LogAuditoriaAssinaturaDao,
    private val syncQueueDao: SyncQueueDao
) {
    
    // ==================== CONTRATO LOCAÇÃO ====================
    
    /**
     * Busca contratos por cliente com descriptografia
     */
    fun buscarContratosPorCliente(
        clienteId: Long,
        decryptContrato: (ContratoLocacao?) -> ContratoLocacao?
    ): Flow<List<ContratoLocacao>> = contratoLocacaoDao.buscarContratosPorCliente(clienteId).map { contratos ->
        contratos.map { decryptContrato(it) ?: it }
    }
    
    /**
     * Busca contrato por número com descriptografia
     */
    suspend fun buscarContratoPorNumero(
        numeroContrato: String,
        decryptContrato: (ContratoLocacao?) -> ContratoLocacao?
    ) = decryptContrato(contratoLocacaoDao.buscarContratoPorNumero(numeroContrato))
    
    /**
     * Busca contratos ativos com descriptografia
     */
    fun buscarContratosAtivos(
        decryptContrato: (ContratoLocacao?) -> ContratoLocacao?
    ): Flow<List<ContratoLocacao>> = contratoLocacaoDao.buscarContratosAtivos().map { contratos ->
        contratos.map { decryptContrato(it) ?: it }
    }
    
    /**
     * Busca todos os contratos com descriptografia
     */
    fun buscarTodosContratos(
        decryptContrato: (ContratoLocacao?) -> ContratoLocacao?
    ): Flow<List<ContratoLocacao>> = contratoLocacaoDao.buscarTodosContratos().map { contratos ->
        contratos.map { decryptContrato(it) ?: it }
    }
    
    /**
     * Conta contratos por ano
     */
    suspend fun contarContratosPorAno(ano: String): Int {
        val (inicioAno, fimAno) = DateUtils.calcularRangeAno(ano)
        return contratoLocacaoDao.contarContratosPorAno(inicioAno, fimAno)
    }
    
    suspend fun contarContratosGerados() = contratoLocacaoDao.contarContratosGerados()
    suspend fun contarContratosAssinados() = contratoLocacaoDao.contarContratosAssinados()
    
    /**
     * Obtém contratos assinados com descriptografia
     */
    suspend fun obterContratosAssinados(
        decryptContrato: (ContratoLocacao?) -> ContratoLocacao?
    ) = contratoLocacaoDao.obterContratosAssinados().map { contrato ->
        decryptContrato(contrato) ?: contrato
    }
    
    /**
     * Insere contrato com criptografia e sincronização
     */
    suspend fun inserirContrato(
        contrato: ContratoLocacao,
        encryptContrato: (ContratoLocacao) -> ContratoLocacao,
        obterAssinaturaAtiva: suspend () -> AssinaturaRepresentanteLegal?,
        logDbInsertStart: (String, String) -> Unit,
        logDbInsertSuccess: (String, String) -> Unit,
        logDbInsertError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ): Long {
        logDbInsertStart("CONTRATO", "Numero=${contrato.numeroContrato}, ClienteID=${contrato.clienteId}")
        return try {
            val contratoEncrypted = encryptContrato(contrato)
            val id = contratoLocacaoDao.inserirContrato(contratoEncrypted)
            logDbInsertSuccess("CONTRATO", "Numero=${contrato.numeroContrato}, ID=$id")
            
            // ✅ SELO 1: Garantir que a assinatura do representante (locador) vá junto no contrato
            val assinaturaAtiva = try { obterAssinaturaAtiva() } catch (_: Exception) { null }
            val assinaturaLocadorFinal = contrato.assinaturaLocador ?: assinaturaAtiva?.assinaturaBase64
            if (assinaturaLocadorFinal != null && contrato.assinaturaLocador == null) {
                val atualizado = contrato.copy(id = id, assinaturaLocador = assinaturaLocadorFinal, dataAtualizacao = java.util.Date())
                try { contratoLocacaoDao.atualizarContrato(encryptContrato(atualizado)) } catch (_: Exception) {}
            }
            
            // Sincronização
            try {
                val payload = criarPayloadContrato(contrato.copy(id = id, assinaturaLocador = assinaturaLocadorFinal ?: contrato.assinaturaLocador))
                adicionarOperacaoSync("ContratoLocacao", id, "CREATE", payload, 1)
                logarOperacaoSync("ContratoLocacao", id, "CREATE", "PENDING", null, payload)
            } catch (syncError: Exception) {
                Log.w("ContratoRepositoryInternal", "Erro ao enfileirar ContratoLocacao CREATE: ${syncError.message}")
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("CONTRATO", "Numero=${contrato.numeroContrato}", e)
            throw e
        }
    }
    
    /**
     * Atualiza contrato com criptografia e sincronização
     */
    suspend fun atualizarContrato(
        contrato: ContratoLocacao,
        encryptContrato: (ContratoLocacao) -> ContratoLocacao,
        obterAssinaturaAtiva: suspend () -> AssinaturaRepresentanteLegal?,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ) {
        try {
            Log.d("ContratoRepositoryInternal", "Atualizando contrato id=${contrato.id}")
            
            // ✅ SELO 2: Preencher assinatura do representante no contrato
            val assinaturaAtiva = try { obterAssinaturaAtiva() } catch (_: Exception) { null }
            val assinaturaLocadorFinal = contrato.assinaturaLocador ?: assinaturaAtiva?.assinaturaBase64
            val contratoComAssinatura = if (assinaturaLocadorFinal != null && contrato.assinaturaLocador == null) {
                contrato.copy(assinaturaLocador = assinaturaLocadorFinal, dataAtualizacao = java.util.Date())
            } else contrato
            
            val contratoParaSalvar = encryptContrato(contratoComAssinatura)
            contratoLocacaoDao.atualizarContrato(contratoParaSalvar)
            
            // Sincronização
            try {
                val payload = criarPayloadContrato(contratoComAssinatura)
                adicionarOperacaoSync("ContratoLocacao", contratoParaSalvar.id, "UPDATE", payload, 1)
                logarOperacaoSync("ContratoLocacao", contratoParaSalvar.id, "UPDATE", "PENDING", null, payload)
            } catch (syncError: Exception) {
                Log.w("ContratoRepositoryInternal", "Erro ao enfileirar ContratoLocacao UPDATE: ${syncError.message}")
            }
        } catch (e: Exception) {
            Log.e("ContratoRepositoryInternal", "Erro ao atualizar contrato id=${contrato.id}", e)
            throw e
        }
    }
    
    /**
     * Encerra contrato
     */
    suspend fun encerrarContrato(
        contratoId: Long,
        @Suppress("UNUSED_PARAMETER") clienteId: Long,
        status: String,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ) {
        val agora = java.util.Date()
        Log.d("ContratoRepositoryInternal", "Encerrar contrato id=$contratoId status=$status")
        contratoLocacaoDao.encerrarContrato(contratoId, status, agora, agora)
        
        // Sincronização
        try {
            val contrato = contratoLocacaoDao.buscarContratoPorId(contratoId)
            if (contrato != null) {
                val payload = criarPayloadContrato(contrato)
                adicionarOperacaoSync("ContratoLocacao", contrato.id, "UPDATE", payload, 1)
                logarOperacaoSync("ContratoLocacao", contrato.id, "UPDATE", "PENDING", null, payload)
            }
        } catch (e: Exception) {
            Log.w("ContratoRepositoryInternal", "Erro ao enfileirar UPDATE após encerrar: ${e.message}")
        }
    }
    
    /**
     * Exclui contrato
     */
    suspend fun excluirContrato(
        contrato: ContratoLocacao,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String?) -> Unit
    ) {
        contratoLocacaoDao.excluirContrato(contrato)
        try {
            adicionarOperacaoSync("ContratoLocacao", contrato.id, "DELETE", "{}", 1)
            logarOperacaoSync("ContratoLocacao", contrato.id, "DELETE", "PENDING", null, null)
        } catch (syncError: Exception) {
            Log.w("ContratoRepositoryInternal", "Erro ao enfileirar ContratoLocacao DELETE: ${syncError.message}")
        }
    }
    
    /**
     * Busca contrato por ID com descriptografia
     */
    suspend fun buscarContratoPorId(
        contratoId: Long,
        decryptContrato: (ContratoLocacao?) -> ContratoLocacao?
    ) = decryptContrato(contratoLocacaoDao.buscarContratoPorId(contratoId))
    
    /**
     * Busca contrato ativo por cliente com descriptografia
     */
    suspend fun buscarContratoAtivoPorCliente(
        clienteId: Long,
        decryptContrato: (ContratoLocacao?) -> ContratoLocacao?
    ) = decryptContrato(contratoLocacaoDao.buscarContratoAtivoPorCliente(clienteId))
    
    suspend fun buscarMesasPorContrato(contratoId: Long) = contratoLocacaoDao.buscarMesasPorContrato(contratoId)
    
    /**
     * Insere contrato mesa com sincronização
     */
    suspend fun inserirContratoMesa(
        contratoMesa: ContratoMesa,
        logDbInsertStart: (String, String) -> Unit,
        logDbInsertSuccess: (String, String) -> Unit,
        logDbInsertError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ): Long {
        logDbInsertStart("CONTRATO_MESA", "ContratoID=${contratoMesa.contratoId}, MesaID=${contratoMesa.mesaId}")
        return try {
            val id = contratoLocacaoDao.inserirContratoMesa(contratoMesa)
            logDbInsertSuccess("CONTRATO_MESA", "ContratoID=${contratoMesa.contratoId}, ID=$id")
            
            try {
                val payload = """
                    {
                        "id": $id,
                        "contratoId": ${contratoMesa.contratoId},
                        "mesaId": ${contratoMesa.mesaId},
                        "tipoEquipamento": "${contratoMesa.tipoEquipamento}",
                        "numeroSerie": "${contratoMesa.numeroSerie}",
                        "valorFicha": ${contratoMesa.valorFicha ?: 0.0},
                        "valorFixo": ${contratoMesa.valorFixo ?: 0.0}
                    }
                """.trimIndent()
                adicionarOperacaoSync("ContratoMesa", id, "CREATE", payload, 1)
                logarOperacaoSync("ContratoMesa", id, "CREATE", "PENDING", null, payload)
            } catch (e: Exception) {
                Log.w("ContratoRepositoryInternal", "Erro ao enfileirar ContratoMesa: ${e.message}")
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("CONTRATO_MESA", "ContratoID=${contratoMesa.contratoId}", e)
            throw e
        }
    }
    
    /**
     * Insere múltiplas mesas de contrato
     */
    suspend fun inserirContratoMesas(
        contratoMesas: List<ContratoMesa>,
        logDbInsertStart: (String, String) -> Unit,
        logDbInsertSuccess: (String, String) -> Unit,
        logDbInsertError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ): List<Long> {
        logDbInsertStart("CONTRATO_MESAS", "Quantidade=${contratoMesas.size}")
        return try {
            val ids = contratoLocacaoDao.inserirContratoMesas(contratoMesas)
            logDbInsertSuccess("CONTRATO_MESAS", "IDs=${ids.joinToString()}")
            
            try {
                contratoMesas.zip(ids).forEach { (cm, id) ->
                    val payload = """
                        {
                            "id": $id,
                            "contratoId": ${cm.contratoId},
                            "mesaId": ${cm.mesaId},
                            "tipoEquipamento": "${cm.tipoEquipamento}",
                            "numeroSerie": "${cm.numeroSerie}",
                            "valorFicha": ${cm.valorFicha ?: 0.0},
                            "valorFixo": ${cm.valorFixo ?: 0.0}
                        }
                    """.trimIndent()
                    adicionarOperacaoSync("ContratoMesa", id, "CREATE", payload, 1)
                    logarOperacaoSync("ContratoMesa", id, "CREATE", "PENDING", null, payload)
                }
            } catch (e: Exception) {
                Log.w("ContratoRepositoryInternal", "Erro ao enfileirar lote ContratoMesas: ${e.message}")
            }
            
            ids
        } catch (e: Exception) {
            logDbInsertError("CONTRATO_MESAS", "Quantidade=${contratoMesas.size}", e)
            throw e
        }
    }
    
    /**
     * Exclui contrato mesa
     */
    suspend fun excluirContratoMesa(
        contratoMesa: ContratoMesa,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ) {
        contratoLocacaoDao.excluirContratoMesa(contratoMesa)
        try {
            val payload = """{ "id": ${contratoMesa.id} }"""
            adicionarOperacaoSync("ContratoMesa", contratoMesa.id, "DELETE", payload, 1)
            logarOperacaoSync("ContratoMesa", contratoMesa.id, "DELETE", "PENDING", null, payload)
        } catch (e: Exception) {
            Log.w("ContratoRepositoryInternal", "Erro ao enfileirar DELETE ContratoMesa: ${e.message}")
        }
    }
    
    suspend fun excluirMesasPorContrato(contratoId: Long) = contratoLocacaoDao.excluirMesasPorContrato(contratoId)
    
    // ==================== ADITIVO CONTRATO ====================
    
    fun buscarAditivosPorContrato(contratoId: Long) = aditivoContratoDao.buscarAditivosPorContrato(contratoId)
    suspend fun buscarAditivoPorNumero(numeroAditivo: String) = aditivoContratoDao.buscarAditivoPorNumero(numeroAditivo)
    suspend fun buscarAditivoPorId(aditivoId: Long) = aditivoContratoDao.buscarAditivoPorId(aditivoId)
    fun buscarTodosAditivos() = aditivoContratoDao.buscarTodosAditivos()
    
    suspend fun contarAditivosPorAno(ano: String): Int {
        val (inicioAno, fimAno) = DateUtils.calcularRangeAno(ano)
        return aditivoContratoDao.contarAditivosPorAno(inicioAno, fimAno)
    }
    
    suspend fun contarAditivosGerados() = aditivoContratoDao.contarAditivosGerados()
    suspend fun contarAditivosAssinados() = aditivoContratoDao.contarAditivosAssinados()
    
    /**
     * Insere aditivo com sincronização
     */
    suspend fun inserirAditivo(
        aditivo: AditivoContrato,
        logDbInsertStart: (String, String) -> Unit,
        logDbInsertSuccess: (String, String) -> Unit,
        logDbInsertError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ): Long {
        logDbInsertStart("ADITIVO", "ContratoID=${aditivo.contratoId}, Numero=${aditivo.numeroAditivo}")
        return try {
            val id = aditivoContratoDao.inserirAditivo(aditivo)
            logDbInsertSuccess("ADITIVO", "ContratoID=${aditivo.contratoId}, ID=$id")
            
            try {
                val payload = """
                    {
                        "id": $id,
                        "numeroAditivo": "${aditivo.numeroAditivo}",
                        "contratoId": ${aditivo.contratoId},
                        "dataAditivo": ${aditivo.dataAditivo.time},
                        "observacoes": "${aditivo.observacoes ?: ""}",
                        "tipo": "${aditivo.tipo}",
                        "assinaturaLocador": ${aditivo.assinaturaLocador?.let { "\"$it\"" } ?: "null"},
                        "assinaturaLocatario": ${aditivo.assinaturaLocatario?.let { "\"$it\"" } ?: "null"},
                        "dataCriacao": ${aditivo.dataCriacao.time},
                        "dataAtualizacao": ${aditivo.dataAtualizacao.time}
                    }
                """.trimIndent()
                adicionarOperacaoSync("AditivoContrato", id, "CREATE", payload, 1)
                logarOperacaoSync("AditivoContrato", id, "CREATE", "PENDING", null, payload)
            } catch (syncError: Exception) {
                Log.w("ContratoRepositoryInternal", "Erro ao adicionar AditivoContrato à fila de sync: ${syncError.message}")
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("ADITIVO", "ContratoID=${aditivo.contratoId}", e)
            throw e
        }
    }
    
    /**
     * Insere mesas de aditivo
     */
    suspend fun inserirAditivoMesas(
        aditivoMesas: List<AditivoMesa>,
        logDbInsertStart: (String, String) -> Unit,
        logDbInsertSuccess: (String, String) -> Unit,
        logDbInsertError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ): List<Long> {
        logDbInsertStart("ADITIVO_MESAS", "Quantidade=${aditivoMesas.size}")
        return try {
            val ids = aditivoContratoDao.inserirAditivoMesas(aditivoMesas)
            logDbInsertSuccess("ADITIVO_MESAS", "IDs=${ids.joinToString()}")
            
            try {
                aditivoMesas.zip(ids).forEach { (mesa, id) ->
                    val payload = """
                        {
                            "id": $id,
                            "aditivoId": ${mesa.aditivoId},
                            "mesaId": ${mesa.mesaId},
                            "tipoEquipamento": "${mesa.tipoEquipamento}",
                            "numeroSerie": "${mesa.numeroSerie}",
                            "valorFicha": ${mesa.valorFicha ?: 0.0},
                            "valorFixo": ${mesa.valorFixo ?: 0.0}
                        }
                    """.trimIndent()
                    adicionarOperacaoSync("AditivoMesa", id, "CREATE", payload, 1)
                    logarOperacaoSync("AditivoMesa", id, "CREATE", "PENDING", null, payload)
                }
            } catch (syncError: Exception) {
                Log.w("ContratoRepositoryInternal", "Erro ao enfileirar AditivoMesa: ${syncError.message}")
            }
            
            ids
        } catch (e: Exception) {
            logDbInsertError("ADITIVO_MESAS", "Quantidade=${aditivoMesas.size}", e)
            throw e
        }
    }
    
    /**
     * Atualiza aditivo
     */
    suspend fun atualizarAditivo(
        aditivo: AditivoContrato,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ) {
        aditivoContratoDao.atualizarAditivo(aditivo)
        try {
            val payload = """
                {
                    "id": ${aditivo.id},
                    "numeroAditivo": "${aditivo.numeroAditivo}",
                    "contratoId": ${aditivo.contratoId},
                    "dataAditivo": ${aditivo.dataAditivo.time},
                    "observacoes": "${aditivo.observacoes ?: ""}",
                    "tipo": "${aditivo.tipo}",
                    "assinaturaLocador": ${aditivo.assinaturaLocador?.let { "\"$it\"" } ?: "null"},
                    "assinaturaLocatario": ${aditivo.assinaturaLocatario?.let { "\"$it\"" } ?: "null"},
                    "dataCriacao": ${aditivo.dataCriacao.time},
                    "dataAtualizacao": ${aditivo.dataAtualizacao.time}
                }
            """.trimIndent()
            adicionarOperacaoSync("AditivoContrato", aditivo.id, "UPDATE", payload, 1)
            logarOperacaoSync("AditivoContrato", aditivo.id, "UPDATE", "PENDING", null, payload)
        } catch (e: Exception) {
            Log.w("ContratoRepositoryInternal", "Erro ao enfileirar UPDATE AditivoContrato: ${e.message}")
        }
    }
    
    /**
     * Exclui aditivo
     */
    suspend fun excluirAditivo(
        aditivo: AditivoContrato,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ) {
        aditivoContratoDao.excluirAditivo(aditivo)
        try {
            val payload = """{ "id": ${aditivo.id} }"""
            adicionarOperacaoSync("AditivoContrato", aditivo.id, "DELETE", payload, 1)
            logarOperacaoSync("AditivoContrato", aditivo.id, "DELETE", "PENDING", null, payload)
        } catch (e: Exception) {
            Log.w("ContratoRepositoryInternal", "Erro ao enfileirar DELETE AditivoContrato: ${e.message}")
        }
    }
    
    suspend fun buscarMesasPorAditivo(aditivoId: Long) = aditivoContratoDao.buscarMesasPorAditivo(aditivoId)
    
    /**
     * Exclui aditivo mesa
     */
    suspend fun excluirAditivoMesa(
        aditivoMesa: AditivoMesa,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ) {
        aditivoContratoDao.excluirAditivoMesa(aditivoMesa)
        try {
            val payload = """{ "id": ${aditivoMesa.id} }"""
            adicionarOperacaoSync("AditivoMesa", aditivoMesa.id, "DELETE", payload, 1)
            logarOperacaoSync("AditivoMesa", aditivoMesa.id, "DELETE", "PENDING", null, payload)
        } catch (e: Exception) {
            Log.w("ContratoRepositoryInternal", "Erro ao enfileirar DELETE AditivoMesa: ${e.message}")
        }
    }
    
    /**
     * Exclui todas as mesas do aditivo
     */
    suspend fun excluirTodasMesasDoAditivo(
        aditivoId: Long,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ) {
        try {
            val mesas = aditivoContratoDao.buscarMesasPorAditivo(aditivoId)
            mesas.forEach { mesa ->
                val payload = """{ "id": ${mesa.id} }"""
                adicionarOperacaoSync("AditivoMesa", mesa.id, "DELETE", payload, 1)
                logarOperacaoSync("AditivoMesa", mesa.id, "DELETE", "PENDING", null, payload)
            }
        } catch (e: Exception) {
            Log.w("ContratoRepositoryInternal", "Erro ao preparar DELETE das AditivoMesas: ${e.message}")
        }
        aditivoContratoDao.excluirTodasMesasDoAditivo(aditivoId)
    }
    
    // ==================== ASSINATURA REPRESENTANTE LEGAL ====================
    
    /**
     * Obtém assinatura ativa com descriptografia
     */
    suspend fun obterAssinaturaRepresentanteLegalAtiva(
        decryptAssinatura: (AssinaturaRepresentanteLegal?) -> AssinaturaRepresentanteLegal?
    ) = decryptAssinatura(assinaturaRepresentanteLegalDao.obterAssinaturaAtiva())
    
    /**
     * Obtém assinatura ativa como Flow com descriptografia
     */
    fun obterAssinaturaRepresentanteLegalAtivaFlow(
        decryptAssinatura: (AssinaturaRepresentanteLegal?) -> AssinaturaRepresentanteLegal?
    ) = assinaturaRepresentanteLegalDao.obterAssinaturaAtivaFlow().map { assinatura ->
        decryptAssinatura(assinatura) ?: assinatura
    }
    
    /**
     * Obtém todas as assinaturas com descriptografia
     */
    suspend fun obterTodasAssinaturasRepresentanteLegal(
        decryptAssinatura: (AssinaturaRepresentanteLegal?) -> AssinaturaRepresentanteLegal?
    ) = assinaturaRepresentanteLegalDao.obterTodasAssinaturas().map { assinatura ->
        decryptAssinatura(assinatura) ?: assinatura
    }
    
    /**
     * Obtém todas as assinaturas como Flow com descriptografia
     */
    fun obterTodasAssinaturasRepresentanteLegalFlow(
        decryptAssinatura: (AssinaturaRepresentanteLegal?) -> AssinaturaRepresentanteLegal?
    ) = assinaturaRepresentanteLegalDao.obterTodasAssinaturasFlow().map { lista ->
        lista.map { decryptAssinatura(it) ?: it }
    }
    
    /**
     * Obtém assinatura por ID com descriptografia
     */
    suspend fun obterAssinaturaRepresentanteLegalPorId(
        id: Long,
        decryptAssinatura: (AssinaturaRepresentanteLegal?) -> AssinaturaRepresentanteLegal?
    ) = decryptAssinatura(assinaturaRepresentanteLegalDao.obterAssinaturaPorId(id))
    
    /**
     * Insere assinatura com criptografia e sincronização
     */
    suspend fun inserirAssinaturaRepresentanteLegal(
        assinatura: AssinaturaRepresentanteLegal,
        encryptAssinatura: (AssinaturaRepresentanteLegal) -> AssinaturaRepresentanteLegal,
        logDbInsertStart: (String, String) -> Unit,
        logDbInsertSuccess: (String, String) -> Unit,
        logDbInsertError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ): Long {
        logDbInsertStart(
            "ASSINATURA",
            "Representante=${assinatura.nomeRepresentante}, NumeroProcuração=${assinatura.numeroProcuração}"
        )
        return try {
            val assinaturaEncrypted = encryptAssinatura(assinatura)
            val id = assinaturaRepresentanteLegalDao.inserirAssinatura(assinaturaEncrypted)
            logDbInsertSuccess(
                "ASSINATURA",
                "Representante=${assinatura.nomeRepresentante}, ID=$id"
            )
            
            try {
                val payload = criarPayloadAssinatura(assinatura.copy(id = id))
                adicionarOperacaoSync("AssinaturaRepresentanteLegal", id, "CREATE", payload, 1)
                logarOperacaoSync("AssinaturaRepresentanteLegal", id, "CREATE", "PENDING", null, payload)
            } catch (syncError: Exception) {
                Log.w("ContratoRepositoryInternal", "Erro ao enfileirar AssinaturaRepresentanteLegal: ${syncError.message}")
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError(
                "ASSINATURA",
                "Representante=${assinatura.nomeRepresentante}",
                e
            )
            throw e
        }
    }
    
    /**
     * Atualiza assinatura com criptografia
     */
    suspend fun atualizarAssinaturaRepresentanteLegal(
        assinatura: AssinaturaRepresentanteLegal,
        encryptAssinatura: (AssinaturaRepresentanteLegal) -> AssinaturaRepresentanteLegal
    ) {
        val assinaturaEncrypted = encryptAssinatura(assinatura)
        assinaturaRepresentanteLegalDao.atualizarAssinatura(assinaturaEncrypted)
    }
    
    suspend fun desativarAssinaturaRepresentanteLegal(id: Long) = assinaturaRepresentanteLegalDao.desativarAssinatura(id)
    suspend fun incrementarUsoAssinatura(id: Long, dataUso: java.util.Date) = assinaturaRepresentanteLegalDao.incrementarUso(id, dataUso)
    suspend fun contarAssinaturasRepresentanteLegalAtivas() = assinaturaRepresentanteLegalDao.contarAssinaturasAtivas()
    
    /**
     * Obtém assinaturas validadas com descriptografia
     */
    suspend fun obterAssinaturasRepresentanteLegalValidadas(
        decryptAssinatura: (AssinaturaRepresentanteLegal?) -> AssinaturaRepresentanteLegal?
    ) = assinaturaRepresentanteLegalDao.obterAssinaturasValidadas().map { assinatura ->
        decryptAssinatura(assinatura) ?: assinatura
    }
    
    // ==================== LOGS DE AUDITORIA ====================
    
    /**
     * Insere log de auditoria com criptografia e sincronização
     */
    suspend fun inserirLogAuditoriaAssinatura(
        log: LogAuditoriaAssinatura,
        encryptLog: (LogAuditoriaAssinatura) -> LogAuditoriaAssinatura,
        logDbInsertStart: (String, String) -> Unit,
        logDbInsertSuccess: (String, String) -> Unit,
        logDbInsertError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ): Long {
        logDbInsertStart("LOG_AUDITORIA_ASSINATURA", "Tipo=${log.tipoOperacao}, Usuario=${log.usuarioExecutou}")
        return try {
            val logEncrypted = encryptLog(log)
            val id = logAuditoriaAssinaturaDao.inserirLog(logEncrypted)
            logDbInsertSuccess("LOG_AUDITORIA_ASSINATURA", "Tipo=${log.tipoOperacao}, ID=$id")
            
            try {
                val payload = criarPayloadLogAuditoria(log.copy(id = id))
                adicionarOperacaoSync("LogAuditoriaAssinatura", id, "CREATE", payload, 1)
                logarOperacaoSync("LogAuditoriaAssinatura", id, "CREATE", "PENDING", null, payload)
            } catch (syncError: Exception) {
                Log.w("ContratoRepositoryInternal", "Erro ao adicionar criação de log auditoria assinatura à fila de sync: ${syncError.message}")
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("LOG_AUDITORIA_ASSINATURA", "Tipo=${log.tipoOperacao}", e)
            throw e
        }
    }
    
    /**
     * Obtém todos os logs com descriptografia
     */
    suspend fun obterTodosLogsAuditoria(
        decryptLog: (LogAuditoriaAssinatura?) -> LogAuditoriaAssinatura?
    ) = logAuditoriaAssinaturaDao.obterTodosLogs().map { log ->
        decryptLog(log) ?: log
    }
    
    /**
     * Obtém todos os logs como Flow com descriptografia
     */
    fun obterTodosLogsAuditoriaFlow(
        decryptLog: (LogAuditoriaAssinatura?) -> LogAuditoriaAssinatura?
    ) = logAuditoriaAssinaturaDao.obterTodosLogsFlow().map { lista ->
        lista.map { decryptLog(it) ?: it }
    }
    
    suspend fun obterLogsAuditoriaPorAssinatura(
        idAssinatura: Long,
        decryptLog: (LogAuditoriaAssinatura?) -> LogAuditoriaAssinatura?
    ) = logAuditoriaAssinaturaDao.obterLogsPorAssinatura(idAssinatura).map { log ->
        decryptLog(log) ?: log
    }
    
    suspend fun obterLogsAuditoriaPorContrato(
        idContrato: Long,
        decryptLog: (LogAuditoriaAssinatura?) -> LogAuditoriaAssinatura?
    ) = logAuditoriaAssinaturaDao.obterLogsPorContrato(idContrato).map { log ->
        decryptLog(log) ?: log
    }
    
    suspend fun obterLogsAuditoriaPorTipoOperacao(
        tipoOperacao: String,
        decryptLog: (LogAuditoriaAssinatura?) -> LogAuditoriaAssinatura?
    ) = logAuditoriaAssinaturaDao.obterLogsPorTipoOperacao(tipoOperacao).map { log ->
        decryptLog(log) ?: log
    }
    
    suspend fun obterLogsAuditoriaPorPeriodo(
        dataInicio: java.util.Date,
        dataFim: java.util.Date,
        decryptLog: (LogAuditoriaAssinatura?) -> LogAuditoriaAssinatura?
    ) = logAuditoriaAssinaturaDao.obterLogsPorPeriodo(dataInicio, dataFim).map { log ->
        decryptLog(log) ?: log
    }
    
    suspend fun obterLogsAuditoriaPorUsuario(
        usuario: String,
        decryptLog: (LogAuditoriaAssinatura?) -> LogAuditoriaAssinatura?
    ) = logAuditoriaAssinaturaDao.obterLogsPorUsuario(usuario).map { log ->
        decryptLog(log) ?: log
    }
    
    suspend fun obterLogsAuditoriaComErro(
        decryptLog: (LogAuditoriaAssinatura?) -> LogAuditoriaAssinatura?
    ) = logAuditoriaAssinaturaDao.obterLogsComErro().map { log ->
        decryptLog(log) ?: log
    }
    
    suspend fun contarLogsAuditoriaDesde(dataInicio: java.util.Date) = logAuditoriaAssinaturaDao.contarLogsDesde(dataInicio)
    suspend fun contarUsosAssinaturaAuditoria(idAssinatura: Long) = logAuditoriaAssinaturaDao.contarUsosAssinatura(idAssinatura)
    
    suspend fun obterLogsAuditoriaNaoValidados(
        decryptLog: (LogAuditoriaAssinatura?) -> LogAuditoriaAssinatura?
    ) = logAuditoriaAssinaturaDao.obterLogsNaoValidados().map { log ->
        decryptLog(log) ?: log
    }
    
    suspend fun validarLogAuditoria(id: Long, dataValidacao: java.util.Date, validadoPor: String) = 
        logAuditoriaAssinaturaDao.validarLog(id, dataValidacao, validadoPor)
    
    // ==================== MÉTODOS AUXILIARES ====================
    
    /**
     * Cria payload JSON para sincronização de ContratoLocacao
     */
    private fun criarPayloadContrato(contrato: ContratoLocacao): String {
        return """
            {
                "id": ${contrato.id},
                "numeroContrato": "${contrato.numeroContrato}",
                "clienteId": ${contrato.clienteId},
                "locadorNome": "${contrato.locadorNome}",
                "locadorCnpj": "${contrato.locadorCnpj}",
                "locadorEndereco": "${contrato.locadorEndereco}",
                "locadorCep": "${contrato.locadorCep}",
                "locatarioNome": "${contrato.locatarioNome}",
                "locatarioCpf": "${contrato.locatarioCpf}",
                "locatarioEndereco": "${contrato.locatarioEndereco}",
                "locatarioTelefone": "${contrato.locatarioTelefone}",
                "locatarioEmail": "${contrato.locatarioEmail}",
                "valorMensal": ${contrato.valorMensal},
                "diaVencimento": ${contrato.diaVencimento},
                "tipoPagamento": "${contrato.tipoPagamento}",
                "percentualReceita": ${contrato.percentualReceita ?: 0.0},
                "dataContrato": ${contrato.dataContrato.time},
                "dataInicio": ${contrato.dataInicio.time},
                "status": "${contrato.status}",
                "dataEncerramento": ${contrato.dataEncerramento?.time ?: "null"},
                "assinaturaLocador": ${if (contrato.assinaturaLocador != null) "\"${contrato.assinaturaLocador}\"" else "null"},
                "assinaturaLocatario": ${if (contrato.assinaturaLocatario != null) "\"${contrato.assinaturaLocatario}\"" else "null"},
                "distratoAssinaturaLocador": ${if (contrato.distratoAssinaturaLocador != null) "\"${contrato.distratoAssinaturaLocador}\"" else "null"},
                "distratoAssinaturaLocatario": ${if (contrato.distratoAssinaturaLocatario != null) "\"${contrato.distratoAssinaturaLocatario}\"" else "null"},
                "distratoDataAssinatura": ${contrato.distratoDataAssinatura?.time ?: "null"},
                "dataCriacao": ${contrato.dataCriacao.time},
                "dataAtualizacao": ${contrato.dataAtualizacao.time},
                "locatarioAssinaturaHash": ${if (contrato.locatarioAssinaturaHash != null) "\"${contrato.locatarioAssinaturaHash}\"" else "null"},
                "locatarioAssinaturaDeviceId": ${if (contrato.locatarioAssinaturaDeviceId != null) "\"${contrato.locatarioAssinaturaDeviceId}\"" else "null"},
                "locatarioAssinaturaIpAddress": ${if (contrato.locatarioAssinaturaIpAddress != null) "\"${contrato.locatarioAssinaturaIpAddress}\"" else "null"},
                "locatarioAssinaturaTimestamp": ${contrato.locatarioAssinaturaTimestamp ?: "null"},
                "locatarioAssinaturaPressaoMedia": ${contrato.locatarioAssinaturaPressaoMedia ?: "null"},
                "locatarioAssinaturaVelocidadeMedia": ${contrato.locatarioAssinaturaVelocidadeMedia ?: "null"},
                "locatarioAssinaturaDuracao": ${contrato.locatarioAssinaturaDuracao ?: "null"},
                "locatarioAssinaturaTotalPontos": ${contrato.locatarioAssinaturaTotalPontos ?: "null"},
                "locadorAssinaturaHash": ${if (contrato.locadorAssinaturaHash != null) "\"${contrato.locadorAssinaturaHash}\"" else "null"},
                "locadorAssinaturaDeviceId": ${if (contrato.locadorAssinaturaDeviceId != null) "\"${contrato.locadorAssinaturaDeviceId}\"" else "null"},
                "locadorAssinaturaTimestamp": ${contrato.locadorAssinaturaTimestamp ?: "null"},
                "documentoHash": ${if (contrato.documentoHash != null) "\"${contrato.documentoHash}\"" else "null"},
                "presencaFisicaConfirmada": ${contrato.presencaFisicaConfirmada},
                "presencaFisicaConfirmadaPor": ${if (contrato.presencaFisicaConfirmadaPor != null) "\"${contrato.presencaFisicaConfirmadaPor}\"" else "null"},
                "presencaFisicaConfirmadaCpf": ${if (contrato.presencaFisicaConfirmadaCpf != null) "\"${contrato.presencaFisicaConfirmadaCpf}\"" else "null"},
                "presencaFisicaConfirmadaTimestamp": ${contrato.presencaFisicaConfirmadaTimestamp ?: "null"}
            }
        """.trimIndent()
    }
    
    /**
     * Cria payload JSON para sincronização de AssinaturaRepresentanteLegal
     */
    private fun criarPayloadAssinatura(assinatura: AssinaturaRepresentanteLegal): String {
        return """
            {
                "id": ${assinatura.id},
                "nomeRepresentante": "${assinatura.nomeRepresentante}",
                "cpfRepresentante": "${assinatura.cpfRepresentante}",
                "cargoRepresentante": "${assinatura.cargoRepresentante}",
                "assinaturaBase64": "${assinatura.assinaturaBase64}",
                "timestampCriacao": ${assinatura.timestampCriacao},
                "deviceId": "${assinatura.deviceId}",
                "hashIntegridade": "${assinatura.hashIntegridade}",
                "versaoSistema": "${assinatura.versaoSistema}",
                "dataCriacao": ${assinatura.dataCriacao.time},
                "criadoPor": "${assinatura.criadoPor}",
                "ativo": ${assinatura.ativo},
                "numeroProcuração": "${assinatura.numeroProcuração}",
                "dataProcuração": ${assinatura.dataProcuração.time},
                "poderesDelegados": "${assinatura.poderesDelegados}",
                "validadeProcuração": ${assinatura.validadeProcuração?.time ?: "null"},
                "totalUsos": ${assinatura.totalUsos},
                "ultimoUso": ${assinatura.ultimoUso?.time ?: "null"},
                "contratosAssinados": "${assinatura.contratosAssinados}",
                "validadaJuridicamente": ${assinatura.validadaJuridicamente},
                "dataValidacao": ${assinatura.dataValidacao?.time ?: "null"},
                "validadoPor": ${assinatura.validadoPor?.let { "\"$it\"" } ?: "null"}
            }
        """.trimIndent()
    }
    
    /**
     * Cria payload JSON para sincronização de LogAuditoriaAssinatura
     */
    private fun criarPayloadLogAuditoria(log: LogAuditoriaAssinatura): String {
        return """
            {
                "id": ${log.id},
                "tipoOperacao": "${log.tipoOperacao}",
                "idAssinatura": ${log.idAssinatura},
                "idContrato": ${log.idContrato ?: "null"},
                "idAditivo": ${log.idAditivo ?: "null"},
                "usuarioExecutou": "${log.usuarioExecutou}",
                "cpfUsuario": "${log.cpfUsuario}",
                "cargoUsuario": "${log.cargoUsuario}",
                "timestamp": ${log.timestamp},
                "deviceId": "${log.deviceId}",
                "versaoApp": "${log.versaoApp}",
                "hashDocumento": "${log.hashDocumento}",
                "hashAssinatura": "${log.hashAssinatura}",
                "latitude": ${log.latitude ?: "null"},
                "longitude": ${log.longitude ?: "null"},
                "endereco": "${log.endereco ?: ""}",
                "ipAddress": "${log.ipAddress ?: ""}",
                "userAgent": "${log.userAgent ?: ""}",
                "tipoDocumento": "${log.tipoDocumento}",
                "numeroDocumento": "${log.numeroDocumento}",
                "valorContrato": ${log.valorContrato ?: "null"},
                "sucesso": ${log.sucesso},
                "mensagemErro": "${log.mensagemErro ?: ""}",
                "dataOperacao": ${log.dataOperacao.time},
                "observacoes": "${log.observacoes ?: ""}",
                "validadoJuridicamente": ${log.validadoJuridicamente},
                "dataValidacao": ${log.dataValidacao?.time ?: "null"},
                "validadoPor": "${log.validadoPor ?: ""}"
            }
        """.trimIndent()
    }
}

