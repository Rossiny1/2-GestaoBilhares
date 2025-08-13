package com.example.gestaobilhares.ui.reports.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.data.repository.AppRepository.CicloInfo
import com.example.gestaobilhares.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.text.NumberFormat
import java.util.*

@HiltViewModel
class RelatorioClientesViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _estatisticas = MutableLiveData<EstatisticasClientes>()
    val estatisticas: LiveData<EstatisticasClientes> = _estatisticas

    private val _clientes = MutableLiveData<List<ClienteRelatorio>>()
    val clientes: LiveData<List<ClienteRelatorio>> = _clientes

    private val _ciclos = MutableLiveData<List<CicloInfo>>()
    val ciclos: LiveData<List<CicloInfo>> = _ciclos

    private val _rotas = MutableLiveData<List<Rota>>()
    val rotas: LiveData<List<Rota>> = _rotas

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private var cicloSelecionado: Long = 0
    private var rotaSelecionada: Long = 0

    init {
        carregarDadosIniciais()
    }

    private fun carregarDadosIniciais() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Carregar ciclos e rotas
                val ciclosData = repository.getCiclos()
                val rotasData = repository.getRotas()
                
                _ciclos.value = ciclosData
                _rotas.value = rotasData
                
                // Selecionar primeiro ciclo e rota por padrão
                if (ciclosData.isNotEmpty()) {
                    selecionarCiclo(ciclosData.first().numero.toLong())
                }
                if (rotasData.isNotEmpty()) {
                    selecionarRota(rotasData.first().id)
                }
                
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Erro ao carregar dados: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun selecionarCiclo(cicloId: Long) {
        cicloSelecionado = cicloId
        atualizarRelatorio()
    }

    fun selecionarRota(rotaId: Long) {
        rotaSelecionada = rotaId
        atualizarRelatorio()
    }

    private fun atualizarRelatorio() {
        if (cicloSelecionado == 0L || rotaSelecionada == 0L) return
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Carregar clientes da rota
                val clientesRota = repository.obterClientesPorRota(rotaSelecionada).first()
                val clienteIds = clientesRota.map { it.id }
                // Pré-carregar mesas por cliente para contagem (consulta em lote)
                val counts = repository.contarMesasAtivasPorClientes(clienteIds)
                val mesasPorCliente: Map<Long, Int> = counts.associate { it.clienteId to it.total }
                // Último acerto por cliente (consulta em lote)
                val dateFmt = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("pt", "BR"))
                val ultimos = repository.buscarUltimosAcertosPorClientes(clienteIds)
                val ultimoAcertoPorCliente: Map<Long, String?> = ultimos.associate { ac -> ac.clienteId to dateFmt.format(ac.dataAcerto) }
                
                // Calcular estatísticas
                val estatisticas = calcularEstatisticas(clientesRota)
                _estatisticas.value = estatisticas
                
                                 // Preparar lista de clientes para relatório
                 val clientesRelatorio = clientesRota.map { cliente ->
                     ClienteRelatorio(
                         id = cliente.id,
                         nome = cliente.nome,
                         endereco = cliente.endereco ?: "",
                         telefone = cliente.telefone ?: "",
                         mesasLocadas = mesasPorCliente[cliente.id] ?: 0,
                         debitoTotal = cliente.debitoAtual,
                         ultimoAcerto = ultimoAcertoPorCliente[cliente.id],
                         status = determinarStatusCliente(cliente)
                     )
                 }
                _clientes.value = clientesRelatorio
                
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Erro ao atualizar relatório: ${e.message}"
                _isLoading.value = false
            }
        }
    }

         private fun calcularEstatisticas(clientes: List<Cliente>): EstatisticasClientes {
         val totalClientes = clientes.size
         val clientesAtivos = clientes.count { it.debitoAtual > 0 || it.ativo }
         val clientesDebito = clientes.count { it.debitoAtual > 0 }
         val totalMesasLocadas = 0
         val ticketMedio = if (clientesAtivos > 0) {
             clientes.filter { it.debitoAtual > 0 || it.ativo }
                 .map { it.debitoAtual }
                 .average()
         } else 0.0
         
         val taxaConversao = if (totalClientes > 0) {
             (clientesAtivos.toDouble() / totalClientes) * 100
         } else 0.0

         return EstatisticasClientes(
             totalClientes = totalClientes,
             clientesAtivos = clientesAtivos,
             clientesDebito = clientesDebito,
             mesasLocadas = totalMesasLocadas,
             ticketMedio = ticketMedio,
             taxaConversao = taxaConversao
         )
     }

         private fun determinarStatusCliente(cliente: Cliente): StatusCliente {
         return when {
             cliente.debitoAtual > 300 -> StatusCliente.EM_DEBITO_ALTO
             cliente.debitoAtual > 0 -> StatusCliente.EM_DEBITO
             cliente.ativo -> StatusCliente.ATIVO
             else -> StatusCliente.INATIVO
         }
     }

    fun exportarRelatorio(): String {
        val estatisticas = _estatisticas.value ?: return ""
        val clientes = _clientes.value ?: emptyList()
        
        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        
        return buildString {
            appendLine("RELATÓRIO DE CLIENTES")
            appendLine("=".repeat(50))
            appendLine()
            
            appendLine("ESTATÍSTICAS GERAIS:")
            appendLine("Total de Clientes: ${estatisticas.totalClientes}")
            appendLine("Clientes Ativos: ${estatisticas.clientesAtivos}")
            appendLine("Clientes em Débito: ${estatisticas.clientesDebito}")
            appendLine("Mesas Locadas: ${estatisticas.mesasLocadas}")
            appendLine("Ticket Médio: ${formatter.format(estatisticas.ticketMedio)}")
            appendLine("Taxa de Conversão: ${String.format("%.1f", estatisticas.taxaConversao)}%")
            appendLine()
            
            appendLine("LISTA DE CLIENTES:")
            appendLine("-".repeat(50))
            clientes.forEach { cliente ->
                appendLine("${cliente.nome} - ${cliente.endereco}")
                appendLine("  Mesas: ${cliente.mesasLocadas} | Débito: ${formatter.format(cliente.debitoTotal)}")
                appendLine("  Status: ${cliente.status.descricao}")
                appendLine()
            }
        }
    }

    data class EstatisticasClientes(
        val totalClientes: Int,
        val clientesAtivos: Int,
        val clientesDebito: Int,
        val mesasLocadas: Int,
        val ticketMedio: Double,
        val taxaConversao: Double
    )

    data class ClienteRelatorio(
        val id: Long,
        val nome: String,
        val endereco: String,
        val telefone: String,
        val mesasLocadas: Int,
        val debitoTotal: Double,
        val ultimoAcerto: String?,
        val status: StatusCliente
    )

    enum class StatusCliente(val descricao: String, val cor: String) {
        ATIVO("Ativo", "#4CAF50"),
        EM_DEBITO("Em Débito", "#FF9800"),
        EM_DEBITO_ALTO("Débito Alto", "#F44336"),
        INATIVO("Inativo", "#9E9E9E")
    }
}

// Factory removida (Hilt injeta o ViewModel)
