package com.example.gestaobilhares.ui.inventory.vehicles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestaobilhares.data.entities.Veiculo
import com.example.gestaobilhares.data.entities.HistoricoManutencaoVeiculo
import com.example.gestaobilhares.data.entities.HistoricoCombustivelVeiculo
import com.example.gestaobilhares.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Calendar
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class VehicleDetailViewModel constructor(
    private val appRepository: AppRepository
) : ViewModel() {
    
    // ✅ NOVO: Flow para vehicleId e currentYear para observação reativa
    private val _vehicleIdFlow = MutableStateFlow<Long?>(null)
    private val _currentYearFlow = MutableStateFlow<Int?>(Calendar.getInstance().get(Calendar.YEAR))
    
    private val _vehicle = MutableStateFlow<Vehicle?>(null)
    val vehicle: StateFlow<Vehicle?> = _vehicle.asStateFlow()
    
    private val _maintenanceHistory = MutableStateFlow<List<MaintenanceRecord>>(emptyList())
    val maintenanceHistory: StateFlow<List<MaintenanceRecord>> = _maintenanceHistory.asStateFlow()
    
    private val _fuelHistory = MutableStateFlow<List<FuelRecord>>(emptyList())
    val fuelHistory: StateFlow<List<FuelRecord>> = _fuelHistory.asStateFlow()
    
    private val _summaryData = MutableStateFlow(VehicleSummary())
    val summaryData: StateFlow<VehicleSummary> = _summaryData.asStateFlow()
    
    private var loadJob: Job? = null

    init {
        // ✅ CORRIGIDO: Usar a mesma abordagem do código antigo que funcionava
        // Observar TODOS os históricos e filtrar manualmente por vehicleId e currentYear
        // Usar combine para reagir às mudanças em vehicleId e currentYear
        viewModelScope.launch {
            // Coletar manutenção e combustível em paralelo (como no código antigo)
            launch {
                combine(
                    appRepository.obterTodosHistoricoManutencaoVeiculoFlow(),
                    _vehicleIdFlow,
                    _currentYearFlow
                ) { todasManutencoes, vehicleId, currentYear ->
                    Triple(todasManutencoes, vehicleId, currentYear)
                }.collect { (todasManutencoes, vehicleId, currentYear) ->
                    if (vehicleId == null) {
                        _maintenanceHistory.value = emptyList()
                        updateSummary(_maintenanceHistory.value, _fuelHistory.value)
                        return@collect
                    }
                    
                    // Filtrar por veículo (como no código antigo)
                    val manutencoesFiltradas = todasManutencoes.filter { it.veiculoId == vehicleId }
                    
                    // Filtrar por ano se necessário
                    val manutencoesPorAno = if (currentYear == null) {
                        manutencoesFiltradas
                    } else {
                        manutencoesFiltradas.filter { manutencao ->
                            val anoManutencao = manutencao.dataManutencao.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().year
                            anoManutencao == currentYear
                        }
                    }
                    
                    android.util.Log.d("VehicleDetailViewModel", "Manutenções encontradas: ${manutencoesPorAno.size} (filtradas de ${manutencoesFiltradas.size} total)")
                    
                    val maintenanceList = manutencoesPorAno.map { manutencao ->
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
                combine(
                    appRepository.obterTodosHistoricoCombustivelVeiculoFlow(),
                    _vehicleIdFlow,
                    _currentYearFlow
                ) { todosCombustiveis, vehicleId, currentYear ->
                    Triple(todosCombustiveis, vehicleId, currentYear)
                }.collect { (todosCombustiveis, vehicleId, currentYear) ->
                    if (vehicleId == null) {
                        _fuelHistory.value = emptyList()
                        updateSummary(_maintenanceHistory.value, _fuelHistory.value)
                        return@collect
                    }
                    
                    // Filtrar por veículo (como no código antigo)
                    val combustiveisPorVeiculo = todosCombustiveis.filter { it.veiculoId == vehicleId }
                    
                    android.util.Log.d("VehicleDetailViewModel", "TOTAL de abastecimentos no banco: ${combustiveisPorVeiculo.size}")
                    
                    // Filtrar por ano se necessário
                    val combustiveisFiltrados = if (currentYear == null) {
                        combustiveisPorVeiculo
                    } else {
                        combustiveisPorVeiculo.filter { combustivel ->
                            val anoAbastecimento = combustivel.dataAbastecimento.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().year
                            anoAbastecimento == currentYear
                        }
                    }
                    
                    android.util.Log.d("VehicleDetailViewModel", "Abastecimentos encontrados: ${combustiveisFiltrados.size} (filtrados de ${combustiveisPorVeiculo.size} total)")
                    
                    // Construir lista de abastecimentos com hodômetro absoluto consistente (como no código antigo)
                    val kmInicial = _vehicle.value?.mileage ?: 0.0
                    var ultimoHodometro = kmInicial
                    val ordenadosPorData = combustiveisFiltrados.sortedBy { it.dataAbastecimento }
                    val fuelList = ordenadosPorData.map { combustivel ->
                        val hodometroAbsoluto = when {
                            combustivel.kmVeiculo > 0L -> combustivel.kmVeiculo.toDouble()
                            combustivel.kmRodado > 0.0 -> ultimoHodometro + combustivel.kmRodado
                            else -> ultimoHodometro
                        }
                        ultimoHodometro = hodometroAbsoluto
                        FuelRecord(
                            id = combustivel.id,
                            date = combustivel.dataAbastecimento.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                            liters = combustivel.litros,
                            value = combustivel.valor,
                            km = hodometroAbsoluto,
                            gasStation = combustivel.posto ?: "N/A"
                        )
                    }
                    _fuelHistory.value = fuelList
                    updateSummary(_maintenanceHistory.value, _fuelHistory.value)
                }
            }
        }
        
        // ✅ NOVO: Observar veículos para carregar dados do veículo atual
        viewModelScope.launch {
            _vehicleIdFlow
                .flatMapLatest { vehicleId ->
                    if (vehicleId == null) {
                        return@flatMapLatest flowOf(null)
                    }
                    appRepository.obterTodosVeiculos()
                        .map { veiculos ->
                            veiculos.find { it.id == vehicleId }
                        }
                }
                .collect { veiculo ->
                    veiculo?.let {
                        _vehicle.value = Vehicle(
                            id = it.id,
                            name = it.nome.ifEmpty { "${it.marca} ${it.modelo}" },
                            plate = it.placa,
                            model = "${it.marca} ${it.modelo}",
                            year = it.anoModelo,
                            color = "N/A", // Veiculo não tem campo cor
                            mileage = it.kmAtual.toDouble()
                        )
                    }
                }
        }
    }

    fun loadVehicle(vehicleId: Long) {
        _vehicleIdFlow.value = vehicleId
        android.util.Log.d("VehicleDetailViewModel", "Carregando veículo: $vehicleId")
    }

    fun filterByYear(year: Int?) {
        _currentYearFlow.value = year
        android.util.Log.d("VehicleDetailViewModel", "Filtrando por ano: $year")
    }
    
    // ✅ NOVO: Método para forçar recarregamento dos dados
    fun refreshData() {
        android.util.Log.d("VehicleDetailViewModel", "Forçando recarregamento dos dados")
        val vehicleId = _vehicleIdFlow.value
        if (vehicleId != null) {
            _vehicleIdFlow.value = null
            _vehicleIdFlow.value = vehicleId
        }
    }

    private fun updateSummary(maintenanceList: List<MaintenanceRecord>, fuelList: List<FuelRecord>) {
        val totalMaintenance = maintenanceList.sumOf { it.value }
        val totalFuel = fuelList.sumOf { it.value }
        
        // ✅ CORREÇÃO: Calcular km real rodado considerando km inicial do veículo
        val totalKm = calculateRealKmDriven(fuelList)
        val averageKmPerLiter = if (fuelList.isNotEmpty() && totalKm > 0) {
            val totalLitros = fuelList.sumOf { it.liters }
            if (totalLitros > 0) totalKm / totalLitros else 0.0
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
     * Para os demais: kmAtual - kmAnterior
     */
    private fun calculateRealKmDriven(fuelList: List<FuelRecord>): Double {
        if (fuelList.isEmpty()) return 0.0
        
        // Obter km inicial do veículo
        val kmInicial = _vehicle.value?.mileage ?: 0.0
        
        // Ordenar por hodômetro (km) crescente para evitar problemas com datas iguais
        val sortedFuelList = fuelList.sortedBy { it.km }
        
        var totalKmReal = 0.0
        
        for (i in sortedFuelList.indices) {
            val fuel = sortedFuelList[i]
            
            val kmRodadoNesteAbastecimento = if (i == 0) {
                // Primeiro abastecimento: subtrair km inicial
                fuel.km - kmInicial
            } else {
                // Demais abastecimentos: km atual - km do abastecimento anterior
                val abastecimentoAnterior = sortedFuelList[i - 1]
                fuel.km - abastecimentoAnterior.km
            }
            
            totalKmReal += kmRodadoNesteAbastecimento
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
        
        // Ordenar por hodômetro (km) crescente para evitar problemas com datas iguais
        val sortedCombustiveis = combustiveis.sortedBy { it.kmVeiculo }
        
        var totalKmReal = 0.0
        
        for (i in sortedCombustiveis.indices) {
            val combustivel = sortedCombustiveis[i]
            
            val kmRodadoNesteAbastecimento = if (i == 0) {
                // Primeiro abastecimento: subtrair km inicial
                combustivel.kmRodado - kmInicial
            } else {
                // Demais abastecimentos: km atual - km do abastecimento anterior
                val abastecimentoAnterior = sortedCombustiveis[i - 1]
                combustivel.kmRodado - abastecimentoAnterior.kmRodado
            }
            
            totalKmReal += kmRodadoNesteAbastecimento
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

