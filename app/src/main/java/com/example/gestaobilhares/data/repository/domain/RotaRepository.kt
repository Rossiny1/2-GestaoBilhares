package com.example.gestaobilhares.data.repository.domain

import com.example.gestaobilhares.data.dao.RotaDao
import com.example.gestaobilhares.data.dao.ClienteDao
import com.example.gestaobilhares.data.dao.AcertoDao
import com.example.gestaobilhares.data.dao.CicloAcertoDao
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.RotaResumo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import android.util.Log

/**
 * Repository especializado para operações relacionadas a rotas.
 * Segue arquitetura híbrida modular: AppRepository como Facade.
 */
class RotaRepository(
    private val rotaDao: RotaDao,
    private val clienteDao: ClienteDao,
    private val acertoDao: AcertoDao,
    private val cicloAcertoDao: CicloAcertoDao
) {
    
    fun obterTodas(): Flow<List<Rota>> = rotaDao.getAllRotas()
    fun obterAtivas(): Flow<List<Rota>> = rotaDao.getAllRotasAtivas()
    suspend fun obterPorId(rotaId: Long) = rotaDao.getRotaById(rotaId)
    
    /**
     * Obtém resumo de rotas com atualização em tempo real
     * Método complexo que combina dados de rotas, ciclos, clientes e acertos
     */
    fun getRotasResumoComAtualizacaoTempoReal(
        calcularClientesAtivos: (Long) -> Int,
        obterCicloAtualRota: (Long) -> Triple<Int, Long?, Long?>,
        calcularPendenciasReais: (Long) -> Int,
        calcularQuantidadeMesas: (Long) -> Int,
        calcularPercentualAcertados: (Long, Long?, Int) -> Int,
        calcularValorAcertado: (Long, Long?) -> Double,
        determinarStatus: (Long) -> com.example.gestaobilhares.data.entities.StatusRota,
        obterDatasCiclo: (Long) -> Pair<Long?, Long?>
    ): Flow<List<RotaResumo>> {
        return combine(
            rotaDao.getAllRotasAtivas(),
            cicloAcertoDao.listarTodos(),
            clienteDao.obterTodos() // ✅ NOVO: Incluir clientes para atualizar quando houver mudanças nos débitos
        ) { rotas, ciclos, clientes ->
            Log.d("RotaRepository", "🔄 Atualizando resumo de rotas: ${rotas.size} rotas, ${ciclos.size} ciclos, ${clientes.size} clientes")
            
            rotas.map { rota ->
                val clientesAtivos = calcularClientesAtivos(rota.id)
                val (cicloAtualNumero, cicloAtualId, _) = obterCicloAtualRota(rota.id)
                val pendencias = calcularPendenciasReais(rota.id)
                val quantidadeMesas = calcularQuantidadeMesas(rota.id)
                val percentualAcertados = calcularPercentualAcertados(rota.id, cicloAtualId, clientesAtivos)
                val valorAcertado = calcularValorAcertado(rota.id, cicloAtualId)
                val statusAtual = determinarStatus(rota.id)
                val (dataInicio, dataFim) = obterDatasCiclo(rota.id)
                
                RotaResumo(
                    rota = rota,
                    clientesAtivos = clientesAtivos,
                    pendencias = pendencias,
                    valorAcertado = valorAcertado,
                    quantidadeMesas = quantidadeMesas,
                    percentualAcertados = percentualAcertados,
                    status = statusAtual,
                    cicloAtual = cicloAtualNumero,
                    dataInicioCiclo = dataInicio,
                    dataFimCiclo = dataFim
                )
            }
        }
    }
}

