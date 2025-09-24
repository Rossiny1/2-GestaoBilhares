package com.example.gestaobilhares.ui.inventory.vehicles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class VehicleDetailViewModel @Inject constructor() : ViewModel() {
    
    private val _vehicle = MutableStateFlow<Vehicle?>(null)
    val vehicle: StateFlow<Vehicle?> = _vehicle.asStateFlow()
    
    private val _maintenanceHistory = MutableStateFlow<List<MaintenanceRecord>>(emptyList())
    val maintenanceHistory: StateFlow<List<MaintenanceRecord>> = _maintenanceHistory.asStateFlow()
    
    private val _fuelHistory = MutableStateFlow<List<FuelRecord>>(emptyList())
    val fuelHistory: StateFlow<List<FuelRecord>> = _fuelHistory.asStateFlow()
    
    private val _summaryData = MutableStateFlow(VehicleSummary())
    val summaryData: StateFlow<VehicleSummary> = _summaryData.asStateFlow()
    
    private var currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private var vehicleId: Long = 0L

    fun loadVehicle(vehicleId: Long) {
        this.vehicleId = vehicleId
        viewModelScope.launch {
            // TODO: Implementar carregamento do veículo do banco de dados
            _vehicle.value = getSampleVehicle(vehicleId)
            loadHistoryData()
        }
    }

    fun filterByYear(year: Int) {
        currentYear = year
        loadHistoryData()
    }

    private fun loadHistoryData() {
        viewModelScope.launch {
            // TODO: Implementar carregamento do histórico do banco de dados
            val maintenanceList = getSampleMaintenanceHistory().filter { 
                it.date.year == currentYear 
            }
            val fuelList = getSampleFuelHistory().filter { 
                it.date.year == currentYear 
            }
            
            _maintenanceHistory.value = maintenanceList
            _fuelHistory.value = fuelList
            
            updateSummary(maintenanceList, fuelList)
        }
    }

    private fun updateSummary(maintenanceList: List<MaintenanceRecord>, fuelList: List<FuelRecord>) {
        val totalMaintenance = maintenanceList.sumOf { it.value }
        val totalFuel = fuelList.sumOf { it.value }
        val totalKm = fuelList.sumOf { it.km }
        val averageKmPerLiter = if (fuelList.isNotEmpty()) {
            fuelList.sumOf { it.km } / fuelList.sumOf { it.liters }
        } else 0.0
        
        _summaryData.value = VehicleSummary(
            totalMaintenance = totalMaintenance,
            totalFuel = totalFuel,
            totalKm = totalKm,
            averageKmPerLiter = averageKmPerLiter
        )
    }

    private fun getSampleVehicle(id: Long): Vehicle {
        return Vehicle(
            id = id,
            name = "Veículo ${id}",
            plate = "ABC-1234",
            model = "Ford Transit",
            year = 2020,
            color = "Branco",
            mileage = 50000.0
        )
    }

    private fun getSampleMaintenanceHistory(): List<MaintenanceRecord> {
        return listOf(
            MaintenanceRecord(
                id = 1L,
                date = java.time.LocalDate.of(2024, 1, 15),
                description = "Troca de óleo",
                value = 150.0,
                mileage = 45000.0,
                type = "Preventiva"
            ),
            MaintenanceRecord(
                id = 2L,
                date = java.time.LocalDate.of(2024, 3, 10),
                description = "Troca de pneus",
                value = 800.0,
                mileage = 47000.0,
                type = "Preventiva"
            ),
            MaintenanceRecord(
                id = 3L,
                date = java.time.LocalDate.of(2024, 6, 5),
                description = "Revisão geral",
                value = 300.0,
                mileage = 49000.0,
                type = "Preventiva"
            )
        )
    }

    private fun getSampleFuelHistory(): List<FuelRecord> {
        return listOf(
            FuelRecord(
                id = 1L,
                date = java.time.LocalDate.of(2024, 1, 10),
                liters = 50.0,
                value = 250.0,
                km = 500.0,
                gasStation = "Posto Shell"
            ),
            FuelRecord(
                id = 2L,
                date = java.time.LocalDate.of(2024, 2, 15),
                liters = 45.0,
                value = 225.0,
                km = 450.0,
                gasStation = "Posto Ipiranga"
            ),
            FuelRecord(
                id = 3L,
                date = java.time.LocalDate.of(2024, 3, 20),
                liters = 48.0,
                value = 240.0,
                km = 480.0,
                gasStation = "Posto Shell"
            )
        )
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
