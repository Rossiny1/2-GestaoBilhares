package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.MesaDao
import com.example.gestaobilhares.data.entities.Mesa
import kotlinx.coroutines.flow.Flow
/**
 * Repository para operações relacionadas a mesas
 * Implementa o padrão Repository para abstrair a camada de dados
 * e fornecer uma interface limpa para os ViewModels.
 */
class MesaRepository constructor(
    private val mesaDao: MesaDao,
    private val appRepository: AppRepository
) {
    fun obterMesasPorCliente(clienteId: Long): Flow<List<Mesa>> =
        mesaDao.obterMesasPorCliente(clienteId)

    fun obterMesasDisponiveis(): Flow<List<Mesa>> = mesaDao.obterMesasDisponiveis()

    suspend fun inserir(mesa: Mesa): Long {
        // ✅ CORREÇÃO CRÍTICA: Usar AppRepository para incluir sincronização
        return appRepository.inserirMesa(mesa)
    }

    suspend fun atualizar(mesa: Mesa) {
        // ✅ CORREÇÃO CRÍTICA: Usar AppRepository para incluir sincronização
        appRepository.atualizarMesa(mesa)
    }

    suspend fun deletar(mesa: Mesa) {
        // ✅ CORREÇÃO CRÍTICA: Usar AppRepository para incluir sincronização
        appRepository.deletarMesa(mesa)
    }

    suspend fun vincularMesa(mesaId: Long, clienteId: Long) {
        // ✅ Atualizar no banco local
        mesaDao.vincularMesa(mesaId, clienteId)
        
        // ✅ Adicionar à fila de sincronização para atualizar o Firestore
        try {
            val mesa = mesaDao.obterMesaPorId(mesaId)
            if (mesa != null) {
                val payload = """
                    {
                        "id": ${mesa.id},
                        "numero": "${mesa.numero}",
                        "clienteId": $clienteId,
                        "ativa": ${mesa.ativa},
                        "tipoMesa": "${mesa.tipoMesa}",
                        "tamanho": "${mesa.tamanho}",
                        "estadoConservacao": "${mesa.estadoConservacao}",
                        "valorFixo": ${mesa.valorFixo},
                        "relogioInicial": ${mesa.relogioInicial},
                        "relogioFinal": ${mesa.relogioFinal},
                        "dataInstalacao": "${mesa.dataInstalacao}",
                        "observacoes": "${mesa.observacoes ?: ""}"
                    }
                """.trimIndent()
                
                appRepository.adicionarOperacaoSync("Mesa", mesaId, "UPDATE", payload, priority = 1)
                android.util.Log.d("MesaRepository", "✅ Mesa $mesaId vinculada ao cliente $clienteId - adicionada à fila de sync")
                android.util.Log.d("MesaRepository", "📋 Payload enviado: $payload")
            }
        } catch (e: Exception) {
            android.util.Log.e("MesaRepository", "Erro ao adicionar vinculação à fila de sync: ${e.message}")
        }
    }

    suspend fun desvincularMesa(mesaId: Long) {
        // ✅ Atualizar no banco local
        mesaDao.desvincularMesa(mesaId)
        
        // ✅ Adicionar à fila de sincronização para atualizar o Firestore
        try {
            val mesa = mesaDao.obterMesaPorId(mesaId)
            if (mesa != null) {
                val payload = """
                    {
                        "id": ${mesa.id},
                        "numero": "${mesa.numero}",
                        "clienteId": null,
                        "ativa": ${mesa.ativa},
                        "tipoMesa": "${mesa.tipoMesa}",
                        "tamanho": "${mesa.tamanho}",
                        "estadoConservacao": "${mesa.estadoConservacao}",
                        "valorFixo": ${mesa.valorFixo},
                        "relogioInicial": ${mesa.relogioInicial},
                        "relogioFinal": ${mesa.relogioFinal},
                        "dataInstalacao": "${mesa.dataInstalacao}",
                        "observacoes": "${mesa.observacoes ?: ""}"
                    }
                """.trimIndent()
                
                appRepository.adicionarOperacaoSync("Mesa", mesaId, "UPDATE", payload, priority = 1)
                android.util.Log.d("MesaRepository", "✅ Mesa $mesaId desvinculada - adicionada à fila de sync")
                android.util.Log.d("MesaRepository", "📋 Payload enviado: $payload")
            }
        } catch (e: Exception) {
            android.util.Log.e("MesaRepository", "Erro ao adicionar desvinculação à fila de sync: ${e.message}")
        }
    }

    suspend fun vincularMesaComValorFixo(mesaId: Long, clienteId: Long, valorFixo: Double) {
        // ✅ Atualizar no banco local
        mesaDao.vincularMesaComValorFixo(mesaId, clienteId, valorFixo)
        
        // ✅ Adicionar à fila de sincronização para atualizar o Firestore
        try {
            val mesa = mesaDao.obterMesaPorId(mesaId)
            if (mesa != null) {
                val payload = """
                    {
                        "id": ${mesa.id},
                        "numero": "${mesa.numero}",
                        "clienteId": $clienteId,
                        "ativa": ${mesa.ativa},
                        "tipoMesa": "${mesa.tipoMesa}",
                        "tamanho": "${mesa.tamanho}",
                        "estadoConservacao": "${mesa.estadoConservacao}",
                        "valorFixo": $valorFixo,
                        "relogioInicial": ${mesa.relogioInicial},
                        "relogioFinal": ${mesa.relogioFinal},
                        "dataInstalacao": "${mesa.dataInstalacao}",
                        "observacoes": "${mesa.observacoes ?: ""}"
                    }
                """.trimIndent()
                
                appRepository.adicionarOperacaoSync("Mesa", mesaId, "UPDATE", payload, priority = 1)
                android.util.Log.d("MesaRepository", "✅ Mesa $mesaId vinculada ao cliente $clienteId com valor fixo $valorFixo - adicionada à fila de sync")
                android.util.Log.d("MesaRepository", "📋 Payload enviado: $payload")
            }
        } catch (e: Exception) {
            android.util.Log.e("MesaRepository", "Erro ao adicionar vinculação com valor fixo à fila de sync: ${e.message}")
        }
    }

    suspend fun retirarMesa(mesaId: Long) = mesaDao.retirarMesa(mesaId)

    suspend fun atualizarRelogioMesa(
        mesaId: Long, 
        relogioInicial: Int, 
        relogioFinal: Int
    ) = mesaDao.atualizarRelogioMesa(mesaId, relogioInicial, relogioFinal)

    /**
     * ✅ NOVO: Busca uma mesa específica por ID
     */
    suspend fun obterMesaPorId(mesaId: Long): Mesa? {
        android.util.Log.d("MesaRepository", "ObterMesaPorId: MesaId=$mesaId")
        val mesa = mesaDao.obterMesaPorId(mesaId)
        if (mesa != null) {
            android.util.Log.d("MesaRepository", "Mesa encontrada: ID=${mesa.id}, Número=${mesa.numero}, Tipo=${mesa.tipoMesa}, ClienteId=${mesa.clienteId}")
        } else {
            android.util.Log.w("MesaRepository", "Mesa não encontrada para ID: $mesaId")
        }
        return mesa
    }
    
    /**
     * ✅ NOVO: Obtém todas as mesas vinculadas a um cliente (versão síncrona)
     */
    suspend fun obterMesasPorClienteDireto(clienteId: Long): List<Mesa> {
        android.util.Log.d("MesaRepository", "ObterMesasPorClienteDireto: ClienteId=$clienteId")
        val mesas = mesaDao.obterMesasPorClienteDireto(clienteId)
        android.util.Log.d("MesaRepository", "Mesas encontradas: ${mesas.size}")
        mesas.forEach { mesa ->
            android.util.Log.d("MesaRepository", "Mesa: ID=${mesa.id}, Número=${mesa.numero}, Tipo=${mesa.tipoMesa}, ClienteId=${mesa.clienteId}")
        }
        return mesas
    }

    suspend fun atualizarRelogioFinal(mesaId: Long, relogioFinal: Int) = 
        mesaDao.atualizarRelogioFinal(mesaId, relogioFinal)
    
    /**
     * ✅ MÉTODO LEGADO: Mantido para compatibilidade com código existente
     */
    suspend fun buscarPorId(mesaId: Long): Mesa? = mesaDao.obterMesaPorId(mesaId)

    /**
     * ✅ NOVA FUNÇÃO: Obtém todas as mesas (disponíveis e em uso)
     */
    fun obterTodasMesas(): Flow<List<Mesa>> = mesaDao.obterTodasMesas()
    
    /**
     * ✅ NOVA FUNÇÃO: Busca contratos por cliente
     */
    fun buscarContratosPorCliente(_clienteId: Long): Flow<List<com.example.gestaobilhares.data.entities.ContratoLocacao>> {
        // Este método será implementado no AppRepository
        // Por enquanto, retorna um Flow vazio
        return kotlinx.coroutines.flow.flowOf(emptyList())
    }
} 
