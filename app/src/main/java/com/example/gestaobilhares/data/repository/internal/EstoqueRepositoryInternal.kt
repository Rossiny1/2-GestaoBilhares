package com.example.gestaobilhares.data.repository.internal

import com.example.gestaobilhares.data.dao.*
import com.example.gestaobilhares.data.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import android.util.Log
import com.google.gson.Gson

/**
 * ✅ FASE 12.14 Etapa 5: Repository interno para operações de Estoque
 * 
 * Extraído do AppRepository para melhorar modularidade e manutenibilidade.
 * Inclui: PanoEstoque, PanoMesa, StockItem, Equipment, MesaVendida, MesaReformada, HistoricoManutencaoMesa
 */
internal class EstoqueRepositoryInternal(
    private val panoEstoqueDao: PanoEstoqueDao,
    private val panoMesaDao: PanoMesaDao,
    private val stockItemDao: StockItemDao,
    private val equipmentDao: EquipmentDao,
    private val mesaVendidaDao: MesaVendidaDao,
    private val mesaReformadaDao: MesaReformadaDao,
    private val historicoManutencaoMesaDao: HistoricoManutencaoMesaDao
) {
    
    // ==================== PANO ESTOQUE ====================
    
    fun obterTodosPanosEstoque() = panoEstoqueDao.listarTodos()
    fun obterPanosDisponiveis() = panoEstoqueDao.listarDisponiveis()
    suspend fun obterPanoEstoquePorId(id: Long) = panoEstoqueDao.buscarPorId(id)
    suspend fun buscarPorNumero(numeroPano: String) = panoEstoqueDao.buscarPorNumero(numeroPano)
    suspend fun marcarPanoComoUsadoPorNumero(numeroPano: String, @Suppress("UNUSED_PARAMETER") motivo: String) {
        val pano = panoEstoqueDao.buscarPorNumero(numeroPano)
        if (pano != null) {
            panoEstoqueDao.atualizarDisponibilidade(pano.id, false)
        }
    }
    suspend fun marcarPanoComoUsado(panoId: Long, @Suppress("UNUSED_PARAMETER") motivo: String) = 
        panoEstoqueDao.atualizarDisponibilidade(panoId, false)
    
    suspend fun inserirPanoEstoque(
        pano: PanoEstoque,
        logDbInsertStart: (String, String) -> Unit,
        logDbInsertSuccess: (String, String) -> Unit,
        logDbInsertError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ): Long {
        logDbInsertStart("PANOESTOQUE", "Numero=${pano.numero}, Cor=${pano.cor}")
        return try {
            val id = panoEstoqueDao.inserir(pano)
            logDbInsertSuccess("PANOESTOQUE", "Numero=${pano.numero}, ID=$id")
            
            try {
                val payload = """
                    {
                        "id": $id,
                        "numero": "${pano.numero}",
                        "cor": "${pano.cor}",
                        "tamanho": "${pano.tamanho}",
                        "material": "${pano.material}",
                        "disponivel": ${pano.disponivel},
                        "observacoes": "${pano.observacoes ?: ""}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("PanoEstoque", id, "CREATE", payload, 1)
                logarOperacaoSync("PanoEstoque", id, "CREATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("EstoqueRepositoryInternal", "Erro ao adicionar pano estoque à fila de sync: ${syncError.message}")
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("PANOESTOQUE", "Numero=${pano.numero}", e)
            throw e
        }
    }
    
    suspend fun atualizarPanoEstoque(
        pano: PanoEstoque,
        logDbUpdateStart: (String, String) -> Unit,
        logDbUpdateSuccess: (String, String) -> Unit,
        logDbUpdateError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ) {
        logDbUpdateStart("PANOESTOQUE", "ID=${pano.id}, Numero=${pano.numero}")
        try {
            panoEstoqueDao.atualizar(pano)
            logDbUpdateSuccess("PANOESTOQUE", "ID=${pano.id}")
            
            try {
                val payload = """
                    {
                        "id": ${pano.id},
                        "numero": "${pano.numero}",
                        "cor": "${pano.cor}",
                        "tamanho": "${pano.tamanho}",
                        "material": "${pano.material}",
                        "disponivel": ${pano.disponivel},
                        "observacoes": "${pano.observacoes ?: ""}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("PanoEstoque", pano.id, "UPDATE", payload, 1)
                logarOperacaoSync("PanoEstoque", pano.id, "UPDATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("EstoqueRepositoryInternal", "Erro ao adicionar atualização de pano estoque à fila de sync: ${syncError.message}")
            }
            
        } catch (e: Exception) {
            logDbUpdateError("PANOESTOQUE", "ID=${pano.id}", e)
            throw e
        }
    }
    
    suspend fun deletarPanoEstoque(pano: PanoEstoque) = panoEstoqueDao.deletar(pano)
    suspend fun atualizarDisponibilidadePano(id: Long, disponivel: Boolean) = 
        panoEstoqueDao.atualizarDisponibilidade(id, disponivel)
    
    // ==================== PANO MESA ====================
    
    fun obterPanoMesaPorMesa(mesaId: Long) = panoMesaDao.buscarPorMesa(mesaId)
    suspend fun obterPanoAtualMesa(mesaId: Long) = panoMesaDao.buscarPanoAtualMesa(mesaId)
    fun obterPanoMesaPorPano(panoId: Long) = panoMesaDao.buscarPorPano(panoId)
    suspend fun obterUltimaTrocaMesa(mesaId: Long) = panoMesaDao.buscarUltimaTrocaMesa(mesaId)
    
    suspend fun inserirPanoMesa(
        panoMesa: PanoMesa,
        logDbInsertStart: (String, String) -> Unit,
        logDbInsertSuccess: (String, String) -> Unit,
        logDbInsertError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ): Long {
        logDbInsertStart("PANOMESA", "MesaID=${panoMesa.mesaId}, PanoID=${panoMesa.panoId}")
        return try {
            val id = panoMesaDao.inserir(panoMesa)
            logDbInsertSuccess("PANOMESA", "MesaID=${panoMesa.mesaId}, PanoID=${panoMesa.panoId}, ID=$id")
            
            try {
                val payload = """
                    {
                        "id": $id,
                        "mesaId": ${panoMesa.mesaId},
                        "panoId": ${panoMesa.panoId},
                        "dataTroca": ${panoMesa.dataTroca.time},
                        "ativo": ${panoMesa.ativo},
                        "observacoes": "${panoMesa.observacoes ?: ""}",
                        "dataCriacao": ${panoMesa.dataCriacao.time}
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("PanoMesa", id, "CREATE", payload, 1)
                logarOperacaoSync("PanoMesa", id, "CREATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("EstoqueRepositoryInternal", "Erro ao adicionar PanoMesa à fila de sync: ${syncError.message}")
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("PANOMESA", "MesaID=${panoMesa.mesaId}, PanoID=${panoMesa.panoId}", e)
            throw e
        }
    }
    
    suspend fun atualizarPanoMesa(
        panoMesa: PanoMesa,
        logDbUpdateStart: (String, String) -> Unit,
        logDbUpdateSuccess: (String, String) -> Unit,
        logDbUpdateError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ) {
        logDbUpdateStart("PANOMESA", "ID=${panoMesa.id}, MesaID=${panoMesa.mesaId}")
        try {
            panoMesaDao.atualizar(panoMesa)
            logDbUpdateSuccess("PANOMESA", "ID=${panoMesa.id}, MesaID=${panoMesa.mesaId}")
            
            try {
                val payload = """
                    {
                        "id": ${panoMesa.id},
                        "mesaId": ${panoMesa.mesaId},
                        "panoId": ${panoMesa.panoId},
                        "dataTroca": ${panoMesa.dataTroca.time},
                        "ativo": ${panoMesa.ativo},
                        "observacoes": "${panoMesa.observacoes ?: ""}",
                        "dataCriacao": ${panoMesa.dataCriacao.time}
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("PanoMesa", panoMesa.id, "UPDATE", payload, 1)
                logarOperacaoSync("PanoMesa", panoMesa.id, "UPDATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("EstoqueRepositoryInternal", "Erro ao adicionar atualização de PanoMesa à fila de sync: ${syncError.message}")
            }
            
        } catch (e: Exception) {
            logDbUpdateError("PANOMESA", "ID=${panoMesa.id}", e)
            throw e
        }
    }
    
    suspend fun deletarPanoMesa(
        panoMesa: PanoMesa,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ) {
        panoMesaDao.deletar(panoMesa)
        try {
            val payload = """{ "id": ${panoMesa.id} }"""
            adicionarOperacaoSync("PanoMesa", panoMesa.id, "DELETE", payload, 1)
            logarOperacaoSync("PanoMesa", panoMesa.id, "DELETE", "PENDING", null, payload)
        } catch (syncError: Exception) {
            Log.w("EstoqueRepositoryInternal", "Erro ao enfileirar PanoMesa DELETE: ${syncError.message}")
        }
    }
    
    suspend fun desativarPanoAtualMesa(mesaId: Long) = panoMesaDao.desativarPanoAtualMesa(mesaId)
    suspend fun ativarPanoMesa(mesaId: Long, panoId: Long) = panoMesaDao.ativarPanoMesa(mesaId, panoId)
    fun buscarHistoricoTrocasMesa(mesaId: Long) = panoMesaDao.buscarHistoricoTrocasMesa(mesaId)
    
    // ==================== MESA VENDIDA ====================
    
    fun obterTodasMesasVendidas(
        decryptMesaVendida: (MesaVendida?) -> MesaVendida?
    ) = mesaVendidaDao.listarTodas().map { mesas ->
        mesas.map { decryptMesaVendida(it) ?: it }
    }
    
    suspend fun obterMesaVendidaPorId(
        id: Long,
        decryptMesaVendida: (MesaVendida?) -> MesaVendida?
    ) = decryptMesaVendida(mesaVendidaDao.buscarPorId(id))
    
    suspend fun inserirMesaVendida(
        mesaVendida: MesaVendida,
        encryptMesaVendida: (MesaVendida) -> MesaVendida,
        logDbInsertStart: (String, String) -> Unit,
        logDbInsertSuccess: (String, String) -> Unit,
        logDbInsertError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ): Long {
        logDbInsertStart("MESAVENDIDA", "Numero=${mesaVendida.numeroMesa}, Comprador=${mesaVendida.nomeComprador}")
        return try {
            val mesaVendidaEncrypted = encryptMesaVendida(mesaVendida)
            val id = mesaVendidaDao.inserir(mesaVendidaEncrypted)
            logDbInsertSuccess("MESAVENDIDA", "Numero=${mesaVendida.numeroMesa}, ID=$id")
            
            try {
                val payload = """
                    {
                        "id": $id,
                        "mesaIdOriginal": ${mesaVendida.mesaIdOriginal},
                        "numeroMesa": "${mesaVendida.numeroMesa}",
                        "tipoMesa": "${mesaVendida.tipoMesa}",
                        "tamanhoMesa": "${mesaVendida.tamanhoMesa}",
                        "estadoConservacao": "${mesaVendida.estadoConservacao}",
                        "nomeComprador": "${mesaVendida.nomeComprador}",
                        "telefoneComprador": "${mesaVendida.telefoneComprador ?: ""}",
                        "cpfCnpjComprador": "${mesaVendida.cpfCnpjComprador ?: ""}",
                        "enderecoComprador": "${mesaVendida.enderecoComprador ?: ""}",
                        "valorVenda": ${mesaVendida.valorVenda},
                        "dataVenda": "${mesaVendida.dataVenda}",
                        "observacoes": "${mesaVendida.observacoes ?: ""}",
                        "dataCriacao": "${mesaVendida.dataCriacao}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("MesaVendida", id, "CREATE", payload, 1)
                logarOperacaoSync("MesaVendida", id, "CREATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("EstoqueRepositoryInternal", "Erro ao adicionar mesa vendida à fila de sync: ${syncError.message}")
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("MESAVENDIDA", "Numero=${mesaVendida.numeroMesa}", e)
            throw e
        }
    }
    
    suspend fun atualizarMesaVendida(
        mesaVendida: MesaVendida,
        encryptMesaVendida: (MesaVendida) -> MesaVendida,
        logDbUpdateStart: (String, String) -> Unit,
        logDbUpdateSuccess: (String, String) -> Unit,
        logDbUpdateError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ) {
        logDbUpdateStart("MESAVENDIDA", "ID=${mesaVendida.id}, Numero=${mesaVendida.numeroMesa}")
        try {
            val mesaVendidaEncrypted = encryptMesaVendida(mesaVendida)
            mesaVendidaDao.atualizar(mesaVendidaEncrypted)
            logDbUpdateSuccess("MESAVENDIDA", "ID=${mesaVendida.id}")
            
            try {
                val payload = """
                    {
                        "id": ${mesaVendida.id},
                        "mesaIdOriginal": ${mesaVendida.mesaIdOriginal},
                        "numeroMesa": "${mesaVendida.numeroMesa}",
                        "tipoMesa": "${mesaVendida.tipoMesa}",
                        "tamanhoMesa": "${mesaVendida.tamanhoMesa}",
                        "estadoConservacao": "${mesaVendida.estadoConservacao}",
                        "nomeComprador": "${mesaVendida.nomeComprador}",
                        "telefoneComprador": "${mesaVendida.telefoneComprador ?: ""}",
                        "cpfCnpjComprador": "${mesaVendida.cpfCnpjComprador ?: ""}",
                        "enderecoComprador": "${mesaVendida.enderecoComprador ?: ""}",
                        "valorVenda": ${mesaVendida.valorVenda},
                        "dataVenda": "${mesaVendida.dataVenda}",
                        "observacoes": "${mesaVendida.observacoes ?: ""}",
                        "dataCriacao": "${mesaVendida.dataCriacao}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("MesaVendida", mesaVendida.id, "UPDATE", payload, 1)
                logarOperacaoSync("MesaVendida", mesaVendida.id, "UPDATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("EstoqueRepositoryInternal", "Erro ao adicionar atualização de mesa vendida à fila de sync: ${syncError.message}")
            }
            
        } catch (e: Exception) {
            logDbUpdateError("MESAVENDIDA", "ID=${mesaVendida.id}", e)
            throw e
        }
    }
    
    suspend fun deletarMesaVendida(mesaVendida: MesaVendida) = mesaVendidaDao.deletar(mesaVendida)
    
    // ==================== MESA REFORMADA ====================
    
    suspend fun inserirMesaReformada(
        mesaReformada: MesaReformada,
        obterEmpresaId: suspend () -> String,
        uploadFotoSeNecessario: suspend (String?, String, String, Long, Long?) -> String?,
        logDbInsertStart: (String, String) -> Unit,
        logDbInsertSuccess: (String, String) -> Unit,
        logDbInsertError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ): Long {
        logDbInsertStart("MESAREFORMADA", "Mesa=${mesaReformada.numeroMesa}, Data=${mesaReformada.dataReforma}")
        return try {
            val id = mesaReformadaDao.inserir(mesaReformada)
            logDbInsertSuccess("MESAREFORMADA", "Mesa=${mesaReformada.numeroMesa}, ID=$id")
            
            val empresaId = obterEmpresaId()
            Log.d("EstoqueRepositoryInternal", "📷 Processando foto de reforma para MesaReformada $id: foto='${mesaReformada.fotoReforma}'")
            
            val fotoUrl = uploadFotoSeNecessario(
                mesaReformada.fotoReforma,
                "foto_reforma",
                empresaId,
                id,
                null
            )
            
            Log.d("EstoqueRepositoryInternal", "📷 Resultado upload MesaReformada: fotoUrl='$fotoUrl' (original: '${mesaReformada.fotoReforma}')")
            
            if (fotoUrl != null) {
                delay(500)
                Log.d("EstoqueRepositoryInternal", "📷 Aguardou 500ms após upload bem-sucedido")
            }
            
            val mesaReformadaAtualizada = mesaReformada.copy(id = id)
            
            if (fotoUrl == null && !mesaReformada.fotoReforma.isNullOrBlank()) {
                Log.w("EstoqueRepositoryInternal", "⚠️ Upload falhou - removendo foto do banco para não sincronizar caminho inválido")
                val mesaSemFoto = mesaReformadaAtualizada.copy(fotoReforma = null)
                mesaReformadaDao.atualizar(mesaSemFoto)
            }
            
            try {
                val fotoUrlParaPayload = if (fotoUrl != null && com.example.gestaobilhares.utils.FirebaseStorageManager.isFirebaseStorageUrl(fotoUrl)) {
                    fotoUrl
                } else {
                    ""
                }
                
                val payload = """
                    {
                        "id": $id,
                        "mesaId": ${mesaReformadaAtualizada.mesaId},
                        "numeroMesa": "${mesaReformadaAtualizada.numeroMesa}",
                        "tipoMesa": "${mesaReformadaAtualizada.tipoMesa}",
                        "tamanhoMesa": "${mesaReformadaAtualizada.tamanhoMesa}",
                        "pintura": ${mesaReformadaAtualizada.pintura},
                        "tabela": ${mesaReformadaAtualizada.tabela},
                        "panos": ${mesaReformadaAtualizada.panos},
                        "numeroPanos": "${mesaReformadaAtualizada.numeroPanos ?: ""}",
                        "outros": ${mesaReformadaAtualizada.outros},
                        "observacoes": "${mesaReformadaAtualizada.observacoes ?: ""}",
                        "fotoReforma": "$fotoUrlParaPayload",
                        "dataReforma": "${mesaReformadaAtualizada.dataReforma.time}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("mesareformada", id, "CREATE", payload, 1)
                logarOperacaoSync("MESAREFORMADA", id, "CREATE", "Adicionado à fila de sync", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("EstoqueRepositoryInternal", "Erro ao adicionar mesa reformada à fila de sync: ${syncError.message}")
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("MESAREFORMADA", "Mesa=${mesaReformada.numeroMesa}", e)
            throw e
        }
    }
    
    suspend fun atualizarMesaReformada(
        mesaReformada: MesaReformada,
        obterEmpresaId: suspend () -> String,
        uploadFotoSeNecessario: suspend (String?, String, String, Long, Long?) -> String?,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit
    ) {
        mesaReformadaDao.atualizar(mesaReformada)
        
        try {
            val empresaId = obterEmpresaId()
            Log.d("EstoqueRepositoryInternal", "📷 Processando foto de reforma para MesaReformada ${mesaReformada.id} (UPDATE): foto='${mesaReformada.fotoReforma}'")
            
            val fotoUrl = uploadFotoSeNecessario(
                mesaReformada.fotoReforma,
                "foto_reforma",
                empresaId,
                mesaReformada.id,
                null
            )
            
            Log.d("EstoqueRepositoryInternal", "📷 Resultado upload MesaReformada (UPDATE): fotoUrl='$fotoUrl' (original: '${mesaReformada.fotoReforma}')")
            
            var mesaReformadaAtualizada = mesaReformada
            
            if (fotoUrl == null && !mesaReformada.fotoReforma.isNullOrBlank()) {
                Log.w("EstoqueRepositoryInternal", "⚠️ Upload falhou - removendo foto do banco para não sincronizar caminho inválido")
                val mesaSemFoto = mesaReformadaAtualizada.copy(fotoReforma = null)
                mesaReformadaDao.atualizar(mesaSemFoto)
            }
            
            val fotoUrlParaPayload = if (fotoUrl != null && com.example.gestaobilhares.utils.FirebaseStorageManager.isFirebaseStorageUrl(fotoUrl)) {
                fotoUrl
            } else {
                ""
            }
            
            val payloadMap = mutableMapOf<String, Any?>(
                "id" to mesaReformadaAtualizada.id,
                "mesaId" to mesaReformadaAtualizada.mesaId,
                "numeroMesa" to mesaReformadaAtualizada.numeroMesa,
                "tipoMesa" to mesaReformadaAtualizada.tipoMesa,
                "tamanhoMesa" to mesaReformadaAtualizada.tamanhoMesa,
                "pintura" to mesaReformadaAtualizada.pintura,
                "tabela" to mesaReformadaAtualizada.tabela,
                "panos" to mesaReformadaAtualizada.panos,
                "numeroPanos" to (mesaReformadaAtualizada.numeroPanos ?: ""),
                "outros" to mesaReformadaAtualizada.outros,
                "observacoes" to (mesaReformadaAtualizada.observacoes ?: ""),
                "fotoReforma" to fotoUrlParaPayload,
                "dataReforma" to mesaReformadaAtualizada.dataReforma.time
            )
            
            adicionarOperacaoSync("MESAREFORMADA", mesaReformadaAtualizada.id, "UPDATE", com.google.gson.Gson().toJson(payloadMap), 1)
            
        } catch (syncError: Exception) {
            Log.w("EstoqueRepositoryInternal", "Erro ao adicionar atualização de mesa reformada à fila de sync: ${syncError.message}")
        }
    }
    
    // ==================== STOCK ITEM ====================
    
    fun obterTodosStockItems() = stockItemDao.listarTodos()
    suspend fun obterStockItemPorId(id: Long) = stockItemDao.buscarPorId(id)
    
    suspend fun inserirStockItem(
        stockItem: StockItem,
        logDbInsertStart: (String, String) -> Unit,
        logDbInsertSuccess: (String, String) -> Unit,
        logDbInsertError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ): Long {
        logDbInsertStart("STOCKITEM", "Nome=${stockItem.name}, Categoria=${stockItem.category}")
        return try {
            val id = stockItemDao.inserir(stockItem)
            logDbInsertSuccess("STOCKITEM", "Nome=${stockItem.name}, ID=$id")
            
            try {
                val payload = """
                    {
                        "id": $id,
                        "name": "${stockItem.name}",
                        "category": "${stockItem.category}",
                        "quantity": ${stockItem.quantity},
                        "unitPrice": ${stockItem.unitPrice},
                        "supplier": "${stockItem.supplier}",
                        "description": "${stockItem.description ?: ""}",
                        "createdAt": "${stockItem.createdAt.time}",
                        "updatedAt": "${stockItem.updatedAt.time}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("StockItem", id, "CREATE", payload, 1)
                logarOperacaoSync("StockItem", id, "CREATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("EstoqueRepositoryInternal", "Erro ao adicionar criação de stock item à fila de sync: ${syncError.message}")
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("STOCKITEM", "Nome=${stockItem.name}", e)
            throw e
        }
    }
    
    suspend fun atualizarStockItem(
        stockItem: StockItem,
        logDbUpdateStart: (String, String) -> Unit,
        logDbUpdateSuccess: (String, String) -> Unit,
        logDbUpdateError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ) {
        logDbUpdateStart("STOCKITEM", "ID=${stockItem.id}, Nome=${stockItem.name}")
        try {
            stockItemDao.atualizar(stockItem)
            logDbUpdateSuccess("STOCKITEM", "ID=${stockItem.id}")
            
            try {
                val payload = """
                    {
                        "id": ${stockItem.id},
                        "name": "${stockItem.name}",
                        "category": "${stockItem.category}",
                        "quantity": ${stockItem.quantity},
                        "unitPrice": ${stockItem.unitPrice},
                        "supplier": "${stockItem.supplier}",
                        "description": "${stockItem.description ?: ""}",
                        "createdAt": "${stockItem.createdAt.time}",
                        "updatedAt": "${stockItem.updatedAt.time}"
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("StockItem", stockItem.id, "UPDATE", payload, 1)
                logarOperacaoSync("StockItem", stockItem.id, "UPDATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("EstoqueRepositoryInternal", "Erro ao adicionar atualização de stock item à fila de sync: ${syncError.message}")
            }
            
        } catch (e: Exception) {
            logDbUpdateError("STOCKITEM", "ID=${stockItem.id}", e)
            throw e
        }
    }
    
    suspend fun deletarStockItem(stockItem: StockItem) = stockItemDao.deletar(stockItem)
    suspend fun atualizarQuantidadeStockItem(id: Long, newQuantity: Int, updatedAt: java.util.Date) = 
        stockItemDao.atualizarQuantidade(id, newQuantity, updatedAt)
    
    // ==================== EQUIPMENT ====================
    
    fun obterTodosEquipments() = equipmentDao.listarTodos()
    suspend fun obterEquipmentPorId(id: Long) = equipmentDao.buscarPorId(id)
    
    suspend fun inserirEquipment(
        equipment: Equipment,
        logDbInsertStart: (String, String) -> Unit,
        logDbInsertSuccess: (String, String) -> Unit,
        logDbInsertError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ): Long {
        logDbInsertStart("EQUIPMENT", "Nome=${equipment.name}, Quantidade=${equipment.quantity}")
        return try {
            val id = equipmentDao.inserir(equipment)
            logDbInsertSuccess("EQUIPMENT", "Nome=${equipment.name}, ID=$id")
            
            try {
                val payload = """
                    {
                        "id": $id,
                        "name": "${equipment.name}",
                        "description": "${equipment.description ?: ""}",
                        "quantity": ${equipment.quantity},
                        "location": "${equipment.location ?: ""}",
                        "createdAt": ${equipment.createdAt.time},
                        "updatedAt": ${equipment.updatedAt.time}
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("Equipment", id, "CREATE", payload, 1)
                logarOperacaoSync("Equipment", id, "CREATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("EstoqueRepositoryInternal", "Erro ao adicionar criação de equipment à fila de sync: ${syncError.message}")
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("EQUIPMENT", "Nome=${equipment.name}", e)
            throw e
        }
    }
    
    suspend fun atualizarEquipment(
        equipment: Equipment,
        logDbUpdateStart: (String, String) -> Unit,
        logDbUpdateSuccess: (String, String) -> Unit,
        logDbUpdateError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ) {
        logDbUpdateStart("EQUIPMENT", "ID=${equipment.id}, Nome=${equipment.name}")
        try {
            equipmentDao.atualizar(equipment)
            logDbUpdateSuccess("EQUIPMENT", "ID=${equipment.id}")
            
            try {
                val payload = """
                    {
                        "id": ${equipment.id},
                        "name": "${equipment.name}",
                        "description": "${equipment.description ?: ""}",
                        "quantity": ${equipment.quantity},
                        "location": "${equipment.location ?: ""}",
                        "createdAt": ${equipment.createdAt.time},
                        "updatedAt": ${equipment.updatedAt.time}
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("Equipment", equipment.id, "UPDATE", payload, 1)
                logarOperacaoSync("Equipment", equipment.id, "UPDATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("EstoqueRepositoryInternal", "Erro ao adicionar atualização de equipment à fila de sync: ${syncError.message}")
            }
            
        } catch (e: Exception) {
            logDbUpdateError("EQUIPMENT", "ID=${equipment.id}", e)
            throw e
        }
    }
    
    suspend fun deletarEquipment(equipment: Equipment) = equipmentDao.deletar(equipment)
    
    // ==================== HISTORICO MANUTENCAO MESA ====================
    
    fun obterTodosHistoricoManutencaoMesa() = historicoManutencaoMesaDao.listarTodos()
    suspend fun obterHistoricoManutencaoMesaPorId(id: Long) = historicoManutencaoMesaDao.buscarPorId(id)
    
    suspend fun inserirHistoricoManutencaoMesa(
        historico: HistoricoManutencaoMesa,
        obterEmpresaId: suspend () -> String,
        uploadFotoSeNecessario: suspend (String?, String, String, Long, Long?) -> String?,
        logDbInsertStart: (String, String) -> Unit,
        logDbInsertSuccess: (String, String) -> Unit,
        logDbInsertError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ): Long {
        logDbInsertStart("HISTORICO_MANUTENCAO_MESA", "Mesa=${historico.numeroMesa}, Tipo=${historico.tipoManutencao}")
        return try {
            val id = historicoManutencaoMesaDao.inserir(historico)
            logDbInsertSuccess("HISTORICO_MANUTENCAO_MESA", "Mesa=${historico.numeroMesa}, ID=$id")
            
            val empresaId = obterEmpresaId()
            Log.d("EstoqueRepositoryInternal", "📷 Processando fotos de manutenção para histórico $id")
            
            val fotoAntesUrl = uploadFotoSeNecessario(
                historico.fotoAntes,
                "foto_antes",
                empresaId,
                id,
                null
            )
            val fotoDepoisUrl = uploadFotoSeNecessario(
                historico.fotoDepois,
                "foto_depois",
                empresaId,
                id,
                null
            )
            
            Log.d("EstoqueRepositoryInternal", "📷 Resultado uploads: fotoAntes='$fotoAntesUrl', fotoDepois='$fotoDepoisUrl'")
            
            if (fotoAntesUrl != null || fotoDepoisUrl != null) {
                delay(500)
                Log.d("EstoqueRepositoryInternal", "📷 Aguardou 500ms após upload(s) bem-sucedido(s)")
            }
            
            var historicoAtualizado = historico.copy(id = id)
            
            if (fotoAntesUrl == null && !historico.fotoAntes.isNullOrBlank()) {
                historicoAtualizado = historicoAtualizado.copy(fotoAntes = null)
                historicoManutencaoMesaDao.atualizar(historicoAtualizado)
            }
            if (fotoDepoisUrl == null && !historico.fotoDepois.isNullOrBlank()) {
                historicoAtualizado = historicoAtualizado.copy(fotoDepois = null)
                historicoManutencaoMesaDao.atualizar(historicoAtualizado)
            }
            
            try {
                val fotoAntesUrlParaPayload = if (fotoAntesUrl != null && com.example.gestaobilhares.utils.FirebaseStorageManager.isFirebaseStorageUrl(fotoAntesUrl)) {
                    fotoAntesUrl
                } else {
                    ""
                }
                val fotoDepoisUrlParaPayload = if (fotoDepoisUrl != null && com.example.gestaobilhares.utils.FirebaseStorageManager.isFirebaseStorageUrl(fotoDepoisUrl)) {
                    fotoDepoisUrl
                } else {
                    ""
                }
                
                val payload = """
                    {
                        "id": $id,
                        "mesaId": ${historicoAtualizado.mesaId},
                        "numeroMesa": "${historicoAtualizado.numeroMesa}",
                        "tipoManutencao": "${historicoAtualizado.tipoManutencao}",
                        "descricao": "${historicoAtualizado.descricao ?: ""}",
                        "dataManutencao": ${historicoAtualizado.dataManutencao.time},
                        "responsavel": "${historicoAtualizado.responsavel ?: ""}",
                        "observacoes": "${historicoAtualizado.observacoes ?: ""}",
                        "custo": ${historicoAtualizado.custo ?: "null"},
                        "fotoAntes": "$fotoAntesUrlParaPayload",
                        "fotoDepois": "$fotoDepoisUrlParaPayload",
                        "dataCriacao": ${historicoAtualizado.dataCriacao.time}
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("HistoricoManutencaoMesa", id, "CREATE", payload, 1)
                logarOperacaoSync("HistoricoManutencaoMesa", id, "CREATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("EstoqueRepositoryInternal", "Erro ao adicionar criação de histórico manutenção mesa à fila de sync: ${syncError.message}")
            }
            
            id
        } catch (e: Exception) {
            logDbInsertError("HISTORICO_MANUTENCAO_MESA", "Mesa=${historico.numeroMesa}", e)
            throw e
        }
    }
    
    suspend fun atualizarHistoricoManutencaoMesa(
        historico: HistoricoManutencaoMesa,
        obterEmpresaId: suspend () -> String,
        uploadFotoSeNecessario: suspend (String?, String, String, Long, Long?) -> String?,
        logDbUpdateStart: (String, String) -> Unit,
        logDbUpdateSuccess: (String, String) -> Unit,
        logDbUpdateError: (String, String, Throwable) -> Unit,
        adicionarOperacaoSync: suspend (String, Long, String, String, Int) -> Unit,
        logarOperacaoSync: suspend (String, Long, String, String, String?, String) -> Unit
    ) {
        logDbUpdateStart("HISTORICO_MANUTENCAO_MESA", "ID=${historico.id}, Mesa=${historico.numeroMesa}")
        try {
            historicoManutencaoMesaDao.atualizar(historico)
            logDbUpdateSuccess("HISTORICO_MANUTENCAO_MESA", "ID=${historico.id}")
            
            val empresaId = obterEmpresaId()
            Log.d("EstoqueRepositoryInternal", "📷 Processando fotos de manutenção para histórico ${historico.id} (UPDATE)")
            
            val fotoAntesUrl = uploadFotoSeNecessario(
                historico.fotoAntes,
                "foto_antes",
                empresaId,
                historico.id,
                null
            )
            val fotoDepoisUrl = uploadFotoSeNecessario(
                historico.fotoDepois,
                "foto_depois",
                empresaId,
                historico.id,
                null
            )
            
            Log.d("EstoqueRepositoryInternal", "📷 Resultado uploads: fotoAntes='$fotoAntesUrl', fotoDepois='$fotoDepoisUrl'")
            
            var historicoAtualizado = historico
            
            if (fotoAntesUrl == null && !historico.fotoAntes.isNullOrBlank()) {
                historicoAtualizado = historicoAtualizado.copy(fotoAntes = null)
                historicoManutencaoMesaDao.atualizar(historicoAtualizado)
            }
            if (fotoDepoisUrl == null && !historico.fotoDepois.isNullOrBlank()) {
                historicoAtualizado = historicoAtualizado.copy(fotoDepois = null)
                historicoManutencaoMesaDao.atualizar(historicoAtualizado)
            }
            
            try {
                val fotoAntesUrlParaPayload = if (fotoAntesUrl != null && com.example.gestaobilhares.utils.FirebaseStorageManager.isFirebaseStorageUrl(fotoAntesUrl)) {
                    fotoAntesUrl
                } else {
                    ""
                }
                val fotoDepoisUrlParaPayload = if (fotoDepoisUrl != null && com.example.gestaobilhares.utils.FirebaseStorageManager.isFirebaseStorageUrl(fotoDepoisUrl)) {
                    fotoDepoisUrl
                } else {
                    ""
                }
                
                val payload = """
                    {
                        "id": ${historicoAtualizado.id},
                        "mesaId": ${historicoAtualizado.mesaId},
                        "numeroMesa": "${historicoAtualizado.numeroMesa}",
                        "tipoManutencao": "${historicoAtualizado.tipoManutencao}",
                        "descricao": "${historicoAtualizado.descricao ?: ""}",
                        "dataManutencao": ${historicoAtualizado.dataManutencao.time},
                        "responsavel": "${historicoAtualizado.responsavel ?: ""}",
                        "observacoes": "${historicoAtualizado.observacoes ?: ""}",
                        "custo": ${historicoAtualizado.custo ?: "null"},
                        "fotoAntes": "$fotoAntesUrlParaPayload",
                        "fotoDepois": "$fotoDepoisUrlParaPayload",
                        "dataCriacao": ${historicoAtualizado.dataCriacao.time}
                    }
                """.trimIndent()
                
                adicionarOperacaoSync("HistoricoManutencaoMesa", historicoAtualizado.id, "UPDATE", payload, 1)
                logarOperacaoSync("HistoricoManutencaoMesa", historicoAtualizado.id, "UPDATE", "PENDING", null, payload)
                
            } catch (syncError: Exception) {
                Log.w("EstoqueRepositoryInternal", "Erro ao adicionar atualização de histórico manutenção mesa à fila de sync: ${syncError.message}")
            }
            
        } catch (e: Exception) {
            logDbUpdateError("HISTORICO_MANUTENCAO_MESA", "ID=${historico.id}", e)
            throw e
        }
    }
    
    suspend fun deletarHistoricoManutencaoMesa(historico: HistoricoManutencaoMesa) = 
        historicoManutencaoMesaDao.deletar(historico)
}

