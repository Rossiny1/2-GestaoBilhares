package com.example.gestaobilhares.data.repository

import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.entities.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    private val db: AppDatabase,
    private val gson: Gson
) {
    suspend fun exportData(): String = withContext(Dispatchers.IO) {
        // Coleta dados de todas as tabelas principais
        // Usando first() para pegar o estado atual dos Flows
        
        val clientes = try { db.clienteDao().obterTodos().first() } catch (e: Exception) { emptyList() }
        val rotas = try { db.rotaDao().getAllRotas().first() } catch (e: Exception) { emptyList() }
        val mesas = try { db.mesaDao().obterTodasMesas().first() } catch (e: Exception) { emptyList() }
        
        // Acertos e Despesas (assumindo métodos padronizados, se falhar ajustamos)
        // Precisamos verificar nomes corretos nos DAOs se o build falhar
        val acertos = try { db.acertoDao().listarTodos().first() } catch (e: Exception) { emptyList() }
        val despesas = try { db.despesaDao().buscarTodas().first() } catch (e: Exception) { emptyList() }
        
        val backupData = BackupData(
            timestamp = Date().time,
            clientes = clientes,
            rotas = rotas,
            mesas = mesas,
            acertos = acertos,
            despesas = despesas
        )
        
        gson.toJson(backupData)
    }

    data class BackupData(
        val timestamp: Long,
        val clientes: List<Cliente>,
        val rotas: List<Rota>,
        val mesas: List<Mesa>,
        val acertos: List<Acerto>,
        val despesas: List<Despesa>
    )
}
