package com.example.gestaobilhares.ui.inventory.vehicles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.Veiculo
import com.example.gestaobilhares.data.entities.HistoricoManutencaoVeiculo
import com.example.gestaobilhares.data.entities.HistoricoCombustivelVeiculo
import com.example.gestaobilhares.data.repository.VeiculoRepository
import com.example.gestaobilhares.data.repository.HistoricoManutencaoVeiculoRepository
import com.example.gestaobilhares.data.repository.HistoricoCombustivelVeiculoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Calendar
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class VehicleDetailViewModel @Inject constructor(
    private val veiculoRepository: VeiculoRepository,
    private val historicoManutencaoRepository: HistoricoManutencaoVeiculoRepository,
    private val historicoCombustivelRepository: HistoricoCombustivelVeiculoRepository
) : ViewModel() {
    
    private val _vehicle = MutableStateFlow<Vehicle?>(null)
    val vehicle: StateFlow<Vehicle?> = _vehicle.asStateFlow()
    
    private val _maintenanceHistory = MutableStateFlow<List<MaintenanceRecord>>(emptyList())
    val maintenanceHistory: StateFlow<List<MaintenanceRecord>> = _maintenanceHistory.asStateFlow()
    
    private val _fuelHistory = MutableStateFlow<List<FuelRecord>>(emptyList())
    val fuelHistory: StateFlow<List<FuelRecord>> = _fuelHistory.asStateFlow()
    
    private val _summaryData = MutableStateFlow(VehicleSummary())
    val summaryData: StateFlow<VehicleSummary> = _summaryData.asStateFlow()
    
    private var currentYear: Int? = Calendar.getInstance().get(Calendar.YEAR)
    private var vehicleId: Long = 0L
    private var loadJob: Job? = null

    fun loadVehicle(vehicleId: Long) {
        this.vehicleId = vehicleId
        viewModelScope.launch {
            // Carregar veículo do banco de dados
            veiculoRepository.listar().collect { veiculos ->
                val veiculo = veiculos.find { it.id == vehicleId }
                veiculo?.let {
                    _vehicle.value = Vehicle(
                        id = it.id,
                        name = it.nome.ifEmpty { "${it.marca} ${it.modelo}" },
                        plate = it.placa,
                        model = "${it.marca} ${it.modelo}",
                        year = it.anoModelo,
                        color = "N/A",
                        mileage = it.kmAtual.toDouble()
                    )
                }
            }
            loadHistoryData()
        }
    }

    fun filterByYear(year: Int?) {
        currentYear = year
        loadHistoryData()
    }
    
    // ✅ NOVO: Método para forçar recarregamento dos dados
    fun refreshData() {
        android.util.Log.d("VehicleDetailViewModel", "Forçando recarregamento dos dados")
        loadHistoryData()
    }
    
    // ✅ NOVO: Método para debug - carregar TODOS os dados sem filtro
    fun loadAllDataForDebug() {
        viewModelScope.launch {
            android.util.Log.d("VehicleDetailViewModel", "=== DEBUG: Carregando TODOS os dados sem filtro ===")
            
            historicoCombustivelRepository.listarPorVeiculo(vehicleId).collect { todosCombustiveis ->
                android.util.Log.d("VehicleDetailViewModel", "DEBUG: Total de abastecimentos: ${todosCombustiveis.size}")
                todosCombustiveis.forEach { combustivel ->
                    val dataLocal = combustivel.dataAbastecimento.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    android.util.Log.d("VehicleDetailViewModel", "DEBUG: ID=${combustivel.id}, Data=${combustivel.dataAbastecimento}, DataLocal=${dataLocal}, Ano=${dataLocal.year}")
                }
            }
        }
    }

    private fun loadHistoryData() {
        // Cancelar coleta anterior para evitar coletores duplicados
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            // ✅ LOGS DE DEBUG: Adicionar logs para rastrear o carregamento
            android.util.Log.d("VehicleDetailViewModel", "Carregando histórico para veículo $vehicleId, ano ${currentYear ?: "TODOS"}")
            
            // Coletar manutenção e combustível em paralelo
            launch {
                historicoManutencaoRepository.listarPorVeiculo(vehicleId).collect { todasManutencoes ->
                    val manutencoesFiltradas = if (currentYear == null) {
                        todasManutencoes
                    } else {
                        todasManutencoes.filter { manutencao ->
                            val anoManutencao = manutencao.dataManutencao.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().year
                            anoManutencao == currentYear
                        }
                    }
                    android.util.Log.d("VehicleDetailViewModel", "Manutenções encontradas: ${manutencoesFiltradas.size} (filtradas de ${todasManutencoes.size} total)")
                    val maintenanceList = manutencoesFiltradas.map { manutencao ->
                        MaintenanceRecord(
                            id = manutencao.id,
                            date = manutencao.dataManutencao.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                            description = manutencao.descricao,
                            value = manutencao.valor,
                            mileage = manutencao.kmVeiculo.toDouble(),
                            type = manutencao.tipoManutencao
                        )
                    }
                    _maintenanceHistory.value = maintenanceList
                    updateSummary(_maintenanceHistory.value, _fuelHistory.value)
                }
            }

            launch {
                historicoCombustivelRepository.listarPorVeiculo(vehicleId).collect { todosCombustiveis ->
                    android.util.Log.d("VehicleDetailViewModel", "TOTAL de abastecimentos no banco: ${todosCombustiveis.size}")
                    todosCombustiveis.forEach { combustivel ->
                        val anoAbastecimento = combustivel.dataAbastecimento.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().year
                        android.util.Log.d("VehicleDetailViewModel", "Abastecimento ID=${combustivel.id}: Data=${combustivel.dataAbastecimento}, Ano=${anoAbastecimento}, Filtro=${currentYear ?: "TODOS"}, Match=${currentYear == null || anoAbastecimento == currentYear}")
                    }
                    val combustiveisFiltrados = if (currentYear == null) {
                        todosCombustiveis
                    } else {
                        todosCombustiveis.filter { combustivel ->
                            val anoAbastecimento = combustivel.dataAbastecimento.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().year
                            anoAbastecimento == currentYear
                        }
                    }
                    android.util.Log.d("VehicleDetailViewModel", "Abastecimentos encontrados: ${combustiveisFiltrados.size} (filtrados de ${todosCombustiveis.size} total)")
                    combustiveisFiltrados.forEach { combustivel ->
                        android.util.Log.d("VehicleDetailViewModel", "Abastecimento FILTRADO: ID=${combustivel.id}, Data=${combustivel.dataAbastecimento}, Litros=${combustivel.litros}, Valor=${combustivel.valor}")
                    }
                    val fuelList = combustiveisFiltrados.map { combustivel ->
                        FuelRecord(
                            id = combustivel.id,
                            date = combustivel.dataAbastecimento.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                            liters = combustivel.litros,
                            value = combustivel.valor,
                            km = combustivel.kmRodado,
                            gasStation = combustivel.posto ?: "N/A"
                        )
                    }
                    _fuelHistory.value = fuelList
                    updateSummary(_maintenanceHistory.value, _fuelHistory.value)
                }
            }
        }
    }

    private fun updateSummary(maintenanceList: List<MaintenanceRecord>, fuelList: List<FuelRecord>) {
        val totalMaintenance = maintenanceList.sumOf { it.value }
        val totalFuel = fuelList.sumOf { it.value }
        
        // ✅ CORREÇÃO: Calcular km real rodado considerando km inicial do veículo
        val totalKm = calculateRealKmDriven(fuelList)
        val averageKmPerLiter = if (fuelList.isNotEmpty() && totalKm > 0) {
            totalKm / fuelList.sumOf { it.liters }
        } else 0.0
        
        // ✅ NOVO: Obter km atual (último abastecimento)
        val kmAtual = getCurrentMileage(fuelList)
        
        _summaryData.value = VehicleSummary(
            totalMaintenance = totalMaintenance,
            totalFuel = totalFuel,
            totalKm = kmAtual, // Agora mostra km atual em vez de total
            averageKmPerLiter = averageKmPerLiter
        )
    }
    
    /**
     * Obtém o km atual (último abastecimento) do veículo.
     * Se não há abastecimentos, retorna o km inicial do veículo.
     */
    private fun getCurrentMileage(fuelList: List<FuelRecord>): Double {
        if (fuelList.isEmpty()) {
            return _vehicle.value?.mileage ?: 0.0
        }
        
        // Ordenar por data e pegar o mais recente
        val ultimoAbastecimento = fuelList.maxByOrNull { it.date }
        return ultimoAbastecimento?.km ?: (_vehicle.value?.mileage ?: 0.0)
    }
    
    /**
     * Calcula o km real rodado considerando o km inicial do veículo.
     * Para o primeiro abastecimento: kmAtual - kmInicial
     * Para os demais: diferença entre abastecimentos consecutivos
     */
    private fun calculateRealKmDriven(fuelList: List<FuelRecord>): Double {
        if (fuelList.isEmpty()) return 0.0
        
        // Obter km inicial do veículo
        val kmInicial = _vehicle.value?.mileage ?: 0.0
        
        // Ordenar abastecimentos por data (mais antigo primeiro)
        val sortedFuelList = fuelList.sortedBy { it.date }
        
        var totalKmReal = 0.0
        var kmAnterior = kmInicial
        
        for (fuel in sortedFuelList) {
            // fuel.km já contém o km rodado desde o último abastecimento
            // Para o primeiro abastecimento: subtrair km inicial
            // Para os demais: usar o km rodado diretamente
            val kmRodadoNesteAbastecimento = if (kmAnterior == kmInicial) {
                // Primeiro abastecimento: subtrair km inicial
                fuel.km - kmInicial
            } else {
                // Demais abastecimentos: usar km rodado diretamente
                fuel.km
            }
            
            totalKmReal += kmRodadoNesteAbastecimento
            kmAnterior += kmRodadoNesteAbastecimento
        }
        
        return totalKmReal
    }
    
    /**
     * Calcula o km real rodado usando dados diretos do banco de dados.
     * Considera o km inicial do veículo e calcula diferenças entre abastecimentos.
     */
    private fun calculateRealKmDrivenFromDatabase(combustiveis: List<HistoricoCombustivelVeiculo>): Double {
        if (combustiveis.isEmpty()) return 0.0
        
        // Obter km inicial do veículo
        val kmInicial = _vehicle.value?.mileage ?: 0.0
        
        // Ordenar abastecimentos por data (mais antigo primeiro)
        val sortedCombustiveis = combustiveis.sortedBy { it.dataAbastecimento }
        
        var totalKmReal = 0.0
        var kmAnterior = kmInicial
        
        for (combustivel in sortedCombustiveis) {
            // Para o primeiro abastecimento: subtrair km inicial
            // Para os demais: usar o km rodado diretamente
            val kmRodadoNesteAbastecimento = if (kmAnterior == kmInicial) {
                // Primeiro abastecimento: subtrair km inicial
                combustivel.kmRodado - kmInicial
            } else {
                // Demais abastecimentos: usar km rodado diretamente
                combustivel.kmRodado
            }
            
            totalKmReal += kmRodadoNesteAbastecimento
            kmAnterior += kmRodadoNesteAbastecimento
        }
        
        return totalKmReal
    }
    
    /**
     * Obtém o km atual (último abastecimento) usando dados diretos do banco.
     * Se não há abastecimentos, retorna o km inicial do veículo.
     */
    private fun getCurrentMileageFromDatabase(combustiveis: List<HistoricoCombustivelVeiculo>): Double {
        if (combustiveis.isEmpty()) {
            return _vehicle.value?.mileage ?: 0.0
        }
        
        // Ordenar por data e pegar o mais recente
        val ultimoAbastecimento = combustiveis.maxByOrNull { it.dataAbastecimento }
        return ultimoAbastecimento?.kmVeiculo?.toDouble() ?: (_vehicle.value?.mileage ?: 0.0)
    }
    
    private fun updateSummaryFromRealData() {
        viewModelScope.launch {
            // ✅ CORREÇÃO: Calcular totais manualmente para garantir precisão
            val todasManutencoes = historicoManutencaoRepository.listarPorVeiculo(vehicleId)
            val todosCombustiveis = historicoCombustivelRepository.listarPorVeiculo(vehicleId)
            
            var totalMaintenance = 0.0
            var totalFuel = 0.0
            var totalKm = 0.0
            var totalLitros = 0.0
            
            todasManutencoes.collect { manutencoes ->
                totalMaintenance = if (currentYear == null) {
                    manutencoes.sumOf { it.valor }
                } else {
                    manutencoes.filter { manutencao ->
                        val anoManutencao = manutencao.dataManutencao.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().year
                        anoManutencao == currentYear
                    }.sumOf { it.valor }
                }
            }
            
            todosCombustiveis.collect { combustiveis ->
                val combustiveisFiltrados = if (currentYear == null) {
                    combustiveis
                } else {
                    combustiveis.filter { combustivel ->
                        val anoAbastecimento = combustivel.dataAbastecimento.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().year
                        anoAbastecimento == currentYear
                    }
                }
                
                totalFuel = combustiveisFiltrados.sumOf { it.valor }
                val totalKmReal = calculateRealKmDrivenFromDatabase(combustiveisFiltrados)
                totalLitros = combustiveisFiltrados.sumOf { it.litros }
                
                val averageKmPerLiter = if (totalLitros > 0) totalKmReal / totalLitros else 0.0
                
                // ✅ NOVO: Obter km atual (último abastecimento)
                val kmAtual = getCurrentMileageFromDatabase(combustiveisFiltrados)

                _summaryData.value = VehicleSummary(
                    totalMaintenance = totalMaintenance,
                    totalFuel = totalFuel,
                    totalKm = kmAtual, // Agora mostra km atual em vez de total
                    averageKmPerLiter = averageKmPerLiter
                )
            }
        }
    }

}

data class Vehicle(
    val id: Long,
    val name: String,
    val plate: String,
    val model: String,
    val year: Int,
    val color: String,
    val mileage: Double
)

data class MaintenanceRecord(
    val id: Long,
    val date: java.time.LocalDate,
    val description: String,
    val value: Double,
    val mileage: Double,
    val type: String
)

data class FuelRecord(
    val id: Long,
    val date: java.time.LocalDate,
    val liters: Double,
    val value: Double,
    val km: Double,
    val gasStation: String
)

data class VehicleSummary(
    val totalMaintenance: Double = 0.0,
    val totalFuel: Double = 0.0,
    val totalKm: Double = 0.0,
    val averageKmPerLiter: Double = 0.0
)
