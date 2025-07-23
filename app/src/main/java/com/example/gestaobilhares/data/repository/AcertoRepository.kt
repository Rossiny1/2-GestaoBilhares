package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.dao.AcertoDao
import com.example.gestaobilhares.data.dao.ClienteDao
import com.example.gestaobilhares.data.entities.Acerto
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class AcertoRepository(private val acertoDao: AcertoDao, private val clienteDao: ClienteDao) {

    suspend fun salvarAcerto(acerto: Acerto): Long {
        AppLogger.log("AcertoRepo", "Tentando salvar acerto para clienteId: ${acerto.clienteId}, ciclo: ${acerto.cicloId}, valorRecebido: ${acerto.valorRecebido}")
        android.util.Log.d("DEBUG_DIAG", "[ACERTO] Salvando acerto: clienteId=${acerto.clienteId}, cicloId=${acerto.cicloId}, valorRecebido=${acerto.valorRecebido}")
        return try {
            // Verificar se o cliente existe
            val cliente = clienteDao.obterPorId(acerto.clienteId)
            if (cliente == null) {
                val msg = "ERRO: Cliente com id ${acerto.clienteId} não existe. Não é possível salvar acerto."
                AppLogger.log("AcertoRepo", msg)
                android.util.Log.e("DEBUG_DIAG", msg)
                throw IllegalStateException(msg)
            }
            
            // Log para verificar o cicloId antes de salvar
            if (acerto.cicloId == null || acerto.cicloId == 0L) {
                 android.util.Log.w("DEBUG_DIAG", "[ACERTO] AVISO: cicloId é nulo ou 0. O acerto será salvo sem vínculo de ciclo.")
            }

            val id = acertoDao.inserir(acerto)
            AppLogger.log("AcertoRepo", "Acerto salvo com sucesso! ID: $id")
            android.util.Log.d("DEBUG_DIAG", "[ACERTO] Acerto salvo com sucesso! ID: $id, cicloId=${acerto.cicloId}")

            // Atualizar o débito atual do cliente
            withContext(Dispatchers.IO) {
                val clienteAtual = clienteDao.obterPorId(acerto.clienteId)
                clienteAtual?.let {
                    // A lógica de atualização de débito já é tratada no ViewModel. 
                    // Removido para evitar dupla atualização.
                }
            }
            id
        } catch (e: Exception) {
            AppLogger.log("AcertoRepo", "ERRO ao salvar acerto: ${e.message}")
            android.util.Log.e("DEBUG_DIAG", "[ACERTO] ERRO ao salvar acerto: ${e.message}")
            -1L // Retorna um ID inválido em caso de erro
        }
    }

    suspend fun getNumeroClientesAcertados(rotaId: Long, cicloId: Long): Int {
        AppLogger.log("AcertoRepo", "Buscando número de clientes acertados para rotaId: $rotaId, cicloId: $cicloId")
        val acertos = acertoDao.buscarPorRotaECicloId(rotaId, cicloId).first()
        val count = acertos.map { it.clienteId }.distinct().count()
        AppLogger.log("AcertoRepo", "Clientes acertados encontrados: $count")
        return count
    }

    suspend fun getReceitaTotal(rotaId: Long, cicloId: Long): Double {
        AppLogger.log("AcertoRepo", "Buscando receita total para rotaId: $rotaId, cicloId: $cicloId")
        val acertos = acertoDao.buscarPorRotaECicloId(rotaId, cicloId).first()
        val receita = acertos.sumOf { it.valorRecebido }
        AppLogger.log("AcertoRepo", "Receita encontrada: R$$receita")
        return receita
    }

    suspend fun getAcertosDoCliente(clienteId: Long): List<Acerto> {
        AppLogger.log("AcertoRepo", "Buscando histórico de acertos para clienteId: $clienteId")
        return acertoDao.buscarPorCliente(clienteId).first()
    }

    fun buscarPorCliente(clienteId: Long): Flow<List<Acerto>> = acertoDao.buscarPorCliente(clienteId)
    fun listarTodos(): Flow<List<Acerto>> = acertoDao.listarTodos()
    suspend fun buscarPorId(id: Long): Acerto? {
        return acertoDao.buscarPorId(id)
    }
    suspend fun atualizar(acerto: Acerto) {
        acertoDao.atualizar(acerto)
    }
    suspend fun deletar(acerto: Acerto) = acertoDao.deletar(acerto)
    
    /**
     * Busca o último acerto de uma mesa específica
     * @param mesaId ID da mesa
     * @return Último acerto da mesa, ou null se não houver
     */
    suspend fun buscarUltimoAcertoMesa(mesaId: Long): Acerto? {
        return try {
            acertoDao.buscarUltimoAcertoPorMesa(mesaId)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun buscarUltimoAcertoPorCliente(clienteId: Long): Acerto? {
        return acertoDao.buscarUltimoAcertoPorCliente(clienteId)
    }

    /**
     * ✅ NOVO: Busca a observação do último acerto de um cliente
     * @param clienteId ID do cliente
     * @return Observação do último acerto, ou null se não houver
     */
    suspend fun buscarObservacaoUltimoAcerto(clienteId: Long): String? {
        return try {
            acertoDao.buscarObservacaoUltimoAcerto(clienteId)
        } catch (e: Exception) {
            null
        }
    }

    fun buscarPorCicloId(cicloId: Long) = acertoDao.buscarPorCicloId(cicloId)
    fun buscarPorRotaECicloId(rotaId: Long, cicloId: Long) = acertoDao.buscarPorRotaECicloId(rotaId, cicloId)
    fun buscarPorClienteECicloId(clienteId: Long, cicloId: Long) = acertoDao.buscarPorClienteECicloId(clienteId, cicloId)
} 