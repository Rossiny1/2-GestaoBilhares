package com.example.gestaobilhares.ui.contracts

import androidx.lifecycle.ViewModel
import com.example.gestaobilhares.ui.common.BaseViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import com.example.gestaobilhares.data.entities.ContratoLocacao
import com.example.gestaobilhares.data.entities.AditivoContrato
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.repository.AppRepository
// import dagger.hilt.android.lifecycle.HiltViewModel // REMOVIDO: Hilt nao e mais usado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
// import javax.inject.Inject // REMOVIDO: Hilt nao e mais usado

/**
 * ViewModel para gerenciamento de contratos
 */
class ContractManagementViewModel constructor(
    private val repository: AppRepository
) : BaseViewModel() {

    private val _statistics = MutableStateFlow<ContractStatistics>(ContractStatistics(0, 0, 0))
    val statistics: StateFlow<ContractStatistics> = _statistics.asStateFlow()

    private val _contracts = MutableStateFlow<List<ContractItem>>(emptyList())
    val contracts: StateFlow<List<ContractItem>> = _contracts.asStateFlow()

    // isLoading já existe na BaseViewModel

    private var currentFilter = ContractFilter.WITH_CONTRACT

    /**
     * Carrega dados dos contratos e estatísticas
     */
    fun loadContractData() {
        viewModelScope.launch {
            showLoading()
            try {
                loadStatistics()
                loadContracts()
            } catch (e: Exception) {
                // Tratar erro
            } finally {
                hideLoading()
            }
        }
    }

    /**
     * Carrega estatísticas dos contratos
     */
    private suspend fun loadStatistics() {
        try {
            val totalClientes = repository.obterTodosClientes().first().size
            val contratosGerados = repository.contarContratosGerados()
            val contratosAssinados = repository.contarContratosAssinados()

            _statistics.value = ContractStatistics(
                totalClientes = totalClientes,
                contratosGerados = contratosGerados,
                contratosAssinados = contratosAssinados
            )
        } catch (e: Exception) {
            // Tratar erro
            _statistics.value = ContractStatistics(0, 0, 0)
        }
    }

    /**
     * Carrega lista de contratos baseada no filtro atual
     */
    private suspend fun loadContracts() {
        try {
            val contractItems = when (currentFilter) {
                ContractFilter.WITH_CONTRACT -> loadContractsWithContract()
                ContractFilter.WITHOUT_CONTRACT -> loadClientsWithoutContract()
            }
            _contracts.value = contractItems
        } catch (e: Exception) {
            // Tratar erro
            _contracts.value = emptyList()
        }
    }

    /**
     * Carrega todos os contratos
     */
    private suspend fun loadAllContracts(): List<ContractItem> {
        val contratos = repository.buscarTodosContratos().first()
        return contratos.map { contrato ->
            val cliente = repository.obterClientePorId(contrato.clienteId)
            val rota = cliente?.let { repository.obterRotaPorId(it.rotaId) }
            val mesas = repository.obterMesasPorCliente(contrato.clienteId).first()
            val aditivos = repository.buscarAditivosPorContrato(contrato.id).first()
            val aditivosRetirada = aditivos.filter { it.tipo.equals("RETIRADA", ignoreCase = true) }
            val hasDistrato = !contrato.status.equals("ATIVO", ignoreCase = true)
            val statusLabel = mapStatusLabel(contrato)
            
            ContractItem(
                contrato = contrato,
                cliente = cliente,
                rota = rota,
                mesas = mesas,
                aditivos = aditivos,
                aditivosRetiradaCount = aditivosRetirada.size,
                hasDistrato = hasDistrato,
                status = statusLabel
            )
        }
    }

    /**
     * Carrega clientes com contrato
     */
    private suspend fun loadContractsWithContract(): List<ContractItem> {
        val contratos = repository.buscarTodosContratos().first()
        
        // ✅ NOVO: Agrupar contratos por cliente
        val contratosPorCliente = contratos.groupBy { it.clienteId }
        
        return contratosPorCliente.map { (clienteId, contratosCliente) ->
            val cliente = repository.obterClientePorId(clienteId)
            val rota = cliente?.let { repository.obterRotaPorId(it.rotaId) }
            
            // Buscar o documento mais recente (contrato ou distrato)
            val documentoMaisRecente = contratosCliente.maxByOrNull { contrato ->
                // Se for distrato, usar dataEncerramento; senão, dataCriacao
                (contrato.dataEncerramento ?: contrato.dataCriacao).time
            } ?: contratosCliente.first()
            
            // Coletar todas as mesas de todos os contratos do cliente
            val todasMesas = mutableListOf<Mesa>()
            val todosAditivos = mutableListOf<AditivoContrato>()
            var totalAditivosRetirada = 0
            
            contratosCliente.forEach { contrato ->
                val mesas = repository.obterMesasPorCliente(contrato.clienteId).first()
                todasMesas.addAll(mesas)
                
                val aditivos = repository.buscarAditivosPorContrato(contrato.id).first()
                todosAditivos.addAll(aditivos)
                totalAditivosRetirada += aditivos.count { it.tipo.equals("RETIRADA", ignoreCase = true) }
            }
            
            // Status baseado no documento mais recente
            val baseStatusLabel = mapStatusLabel(documentoMaisRecente)
            val hasDistrato = !documentoMaisRecente.status.equals("ATIVO", ignoreCase = true)

            // ✅ Regra de negócio adicional: se o cliente não possui mesas, considerar Inativo
            val finalStatusLabel = if (todasMesas.isEmpty()) {
                if (hasDistrato) baseStatusLabel else "Inativo (Sem mesas)"
            } else {
                baseStatusLabel
            }

            ContractItem(
                contrato = documentoMaisRecente,
                cliente = cliente,
                rota = rota,
                mesas = todasMesas.distinctBy { it.id },
                aditivos = todosAditivos.sortedByDescending { it.dataCriacao },
                aditivosRetiradaCount = totalAditivosRetirada,
                hasDistrato = hasDistrato,
                status = finalStatusLabel
            )
        }
    }

    /**
     * Carrega clientes sem contrato
     */
    private suspend fun loadClientsWithoutContract(): List<ContractItem> {
        val clientes = repository.obterTodosClientes().first()
        val clientesComContrato = repository.buscarTodosContratos().first().map { it.clienteId }
        
        return clientes.filter { cliente -> !clientesComContrato.contains(cliente.id) }.map { cliente ->
            val rota = repository.obterRotaPorId(cliente.rotaId)
            val mesas = repository.obterMesasPorCliente(cliente.id).first()
            
            ContractItem(
                contrato = null,
                cliente = cliente,
                rota = rota,
                mesas = mesas,
                aditivos = emptyList(),
                status = "Sem Contrato"
            )
        }
    }

    /**
     * Carrega contratos assinados
     */
    private suspend fun loadSignedContracts(): List<ContractItem> {
        val contratos = repository.obterContratosAssinados()
        return contratos.map { contrato ->
            val cliente = repository.obterClientePorId(contrato.clienteId)
            val rota = cliente?.let { repository.obterRotaPorId(it.rotaId) }
            val mesas = repository.obterMesasPorCliente(contrato.clienteId).first()
            val aditivos = repository.buscarAditivosPorContrato(contrato.id).first()
            val aditivosRetirada = aditivos.filter { it.tipo.equals("RETIRADA", ignoreCase = true) }
            val hasDistrato = !contrato.status.equals("ATIVO", ignoreCase = true)
            val statusLabel = mapStatusLabel(contrato)
            
            ContractItem(
                contrato = contrato,
                cliente = cliente,
                rota = rota,
                mesas = mesas,
                aditivos = aditivos,
                aditivosRetiradaCount = aditivosRetirada.size,
                hasDistrato = hasDistrato,
                status = statusLabel
            )
        }
    }

    /**
     * Define filtro atual e recarrega dados
     */
    fun setFilter(filter: ContractFilter) {
        currentFilter = filter
        loadContractData()
    }


    
    /**
     * Busca contratos por texto
     */
    fun searchContracts(query: String) {
        viewModelScope.launch {
            showLoading()
            try {
                val allContracts = repository.buscarTodosContratos().first()
                val allClients = repository.obterTodosClientes().first()
                
                val filteredContracts = allContracts.filter { contrato ->
                    val cliente = allClients.find { it.id == contrato.clienteId }
                    cliente?.nome?.contains(query, ignoreCase = true) == true ||
                    contrato.numeroContrato?.contains(query, ignoreCase = true) == true
                }
                
                val contractItems = filteredContracts.map { contrato ->
                    val cliente = allClients.find { it.id == contrato.clienteId }
                    val rota = cliente?.let { repository.obterRotaPorId(it.rotaId) }
                    val mesas = cliente?.let { repository.obterMesasPorCliente(it.id).first() } ?: emptyList()
                    val aditivos = repository.buscarAditivosPorContrato(contrato.id).first()
                    val aditivosRetirada = aditivos.filter { it.tipo.equals("RETIRADA", ignoreCase = true) }
                    val hasDistrato = !contrato.status.equals("ATIVO", ignoreCase = true)
                    val statusLabel = mapStatusLabel(contrato)
                    
                    ContractItem(
                        contrato = contrato,
                        cliente = cliente,
                        rota = rota,
                        mesas = mesas,
                        aditivos = aditivos,
                        aditivosRetiradaCount = aditivosRetirada.size,
                        hasDistrato = hasDistrato,
                        status = statusLabel
                    )
                }
                
                _contracts.value = contractItems
            } catch (e: Exception) {
                _contracts.value = emptyList()
            }
            hideLoading()
        }
    }
    
    /**
     * Filtra contratos por rota
     */
    fun setFilterByRoute(routeId: Long) {
        viewModelScope.launch {
            showLoading()
            try {
                val allContracts = repository.buscarTodosContratos().first()
                val allClients = repository.obterTodosClientes().first()
                
                val filteredContracts = allContracts.filter { contrato ->
                    val cliente = allClients.find { it.id == contrato.clienteId }
                    cliente?.rotaId == routeId
                }
                
                val contractItems = filteredContracts.map { contrato ->
                    val cliente = allClients.find { it.id == contrato.clienteId }
                    val rota = cliente?.let { repository.obterRotaPorId(it.rotaId) }
                    val mesas = cliente?.let { repository.obterMesasPorCliente(it.id).first() } ?: emptyList()
                    val aditivos = repository.buscarAditivosPorContrato(contrato.id).first()
                    val aditivosRetirada = aditivos.filter { it.tipo.equals("RETIRADA", ignoreCase = true) }
                    val hasDistrato = !contrato.status.equals("ATIVO", ignoreCase = true)
                    val statusLabel = mapStatusLabel(contrato)
                    
                    ContractItem(
                        contrato = contrato,
                        cliente = cliente,
                        rota = rota,
                        mesas = mesas,
                        aditivos = aditivos,
                        aditivosRetiradaCount = aditivosRetirada.size,
                        hasDistrato = hasDistrato,
                        status = statusLabel
                    )
                }
                
                _contracts.value = contractItems
            } catch (e: Exception) {
                _contracts.value = emptyList()
            }
            hideLoading()
        }
    }

    private fun mapStatusLabel(contrato: ContratoLocacao): String {
        return when (contrato.status.uppercase()) {
            "ATIVO" -> if (contrato.assinaturaLocatario != null) "Ativo (Assinado)" else "Ativo (Gerado)"
            "ENCERRADO_QUITADO" -> "Inativo (Distrato)"
            "RESCINDIDO_COM_DIVIDA" -> "Inativo (Distrato com dívida)"
            "RESCINDIDO" -> "Inativo (Rescindido)"
            else -> contrato.status
        }
    }
    
    /**
     * Obtém todas as rotas
     */
    suspend fun getAllRoutes(): List<Rota> {
        return repository.obterTodasRotas().first()
    }
    
    /**
     * Obtém mesas por cliente
     */
    suspend fun getMesasPorCliente(clienteId: Long): List<Mesa> {
        return repository.obterMesasPorCliente(clienteId).first()
    }

    /**
     * Data class para estatísticas de contratos
     */
    data class ContractStatistics(
        val totalClientes: Int,
        val contratosGerados: Int,
        val contratosAssinados: Int
    )

    /**
     * Data class para item de contrato na lista
     */
    data class ContractItem(
        val contrato: ContratoLocacao?,
        val cliente: Cliente?,
        val rota: Rota?,
        val mesas: List<Mesa>,
        val aditivos: List<AditivoContrato>,
        val aditivosRetiradaCount: Int = 0,
        val hasDistrato: Boolean = false,
        val status: String
    )

    /**
     * ✅ NOVO: Obter assinatura do representante legal ativa
     * Retorna a assinatura Base64 do representante legal para uso automático em contratos
     */
    suspend fun obterAssinaturaRepresentanteLegalAtiva(): String? {
        return try {
            val assinatura = repository.obterAssinaturaRepresentanteLegalAtiva()
            assinatura?.assinaturaBase64
        } catch (e: Exception) {
            android.util.Log.e("ContractManagementViewModel", "Erro ao obter assinatura do representante: ${e.message}")
            null
        }
    }
    
    /**
     * Enum para filtros de contrato
     */
    enum class ContractFilter {
        WITH_CONTRACT,
        WITHOUT_CONTRACT
    }
}

