package com.example.gestaobilhares.ui.inventory.vehicles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.databinding.FragmentVehicleDetailBinding
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.repository.VeiculoRepository
import com.example.gestaobilhares.data.repository.HistoricoManutencaoVeiculoRepository
import com.example.gestaobilhares.data.repository.HistoricoCombustivelVeiculoRepository
import kotlinx.coroutines.launch
import java.util.Calendar

class VehicleDetailFragment : Fragment() {
    private var _binding: FragmentVehicleDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: VehicleDetailViewModel
    private lateinit var maintenanceAdapter: MaintenanceHistoryAdapter
    private lateinit var fuelAdapter: FuelHistoryAdapter

    private var vehicleId: Long = 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVehicleDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        vehicleId = arguments?.getLong("vehicleId", 0L) ?: 0L
        
        // ✅ CORREÇÃO: Inicializar ViewModel manualmente
        val appRepository = com.example.gestaobilhares.data.factory.RepositoryFactory.getAppRepository(requireContext())
        viewModel = VehicleDetailViewModel(appRepository)
        
        setupUI()
        setupRecyclerViews()
        setupClickListeners()
        observeData()
        loadVehicleData()
    }

    private fun setupUI() {
        // Configurar toolbar
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }
        
        // Configurar spinner de anos
        setupYearSpinner()
        
        // Configurar tabs
        setupTabs()
    }

    private fun setupYearSpinner() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = mutableListOf<String>()
        years.add("Todos")
        years.addAll((currentYear - 5..currentYear).toList().reversed().map { it.toString() })
        
        val yearAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, years)
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerYear.adapter = yearAdapter
        
        // Selecionar ano atual por padrão
        val currentYearIndex = years.indexOf(currentYear.toString())
        if (currentYearIndex >= 0) {
            binding.spinnerYear.setSelection(currentYearIndex)
        } else {
            binding.spinnerYear.setSelection(0) // Todos
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Manutenção"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Abastecimento"))
        
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showMaintenanceHistory()
                    1 -> showFuelHistory()
                }
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    private fun setupRecyclerViews() {
        // Adapter de manutenção
        maintenanceAdapter = MaintenanceHistoryAdapter { maintenance ->
            // TODO: Implementar navegação para detalhes da manutenção
        }
        binding.rvMaintenanceHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMaintenanceHistory.adapter = maintenanceAdapter

        // Adapter de abastecimento
        fuelAdapter = FuelHistoryAdapter(
            onFuelClick = { fuel ->
                // TODO: Implementar navegação para detalhes do abastecimento
            },
            vehicleInitialMileage = viewModel.vehicle.value?.mileage ?: 0.0
        )
        binding.rvFuelHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFuelHistory.adapter = fuelAdapter
    }

    private fun setupClickListeners() {
        binding.spinnerYear.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val value = parent?.getItemAtPosition(position)?.toString()
                val selectedYear: Int? = if (value == null || value == "Todos") null else value.toIntOrNull()
                viewModel.filterByYear(selectedYear)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.vehicle.collect { vehicle ->
                vehicle?.let {
                    binding.tvVehicleName.text = it.name
                    binding.tvVehiclePlate.text = it.plate
                    binding.tvVehicleModel.text = it.model
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.maintenanceHistory.collect { maintenanceList ->
                maintenanceAdapter.submitList(maintenanceList)
                updateMaintenanceSummary()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.fuelHistory.collect { fuelList ->
                fuelAdapter.submitList(fuelList)
                // Atualizar km inicial do adapter quando veículo estiver carregado
                viewModel.vehicle.value?.let { vehicle ->
                    fuelAdapter.updateVehicleInitialMileage(vehicle.mileage)
                }
                updateFuelSummary()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.summaryData.collect { summary ->
                updateSummaryCards(summary)
            }
        }
    }

    private fun loadVehicleData() {
        viewModel.loadVehicle(vehicleId)
    }
    
    override fun onResume() {
        super.onResume()
        // ✅ NOVO: Recarregar dados quando o fragment voltar ao foco
        viewModel.refreshData()
        
        // ✅ DEBUG: Carregar todos os dados para debug
        viewModel.loadAllDataForDebug()
    }

    private fun showMaintenanceHistory() {
        binding.rvMaintenanceHistory.visibility = View.VISIBLE
        binding.rvFuelHistory.visibility = View.GONE
    }

    private fun showFuelHistory() {
        binding.rvMaintenanceHistory.visibility = View.GONE
        binding.rvFuelHistory.visibility = View.VISIBLE
    }

    private fun updateMaintenanceSummary() {
        val maintenanceList = viewModel.maintenanceHistory.value
        val totalMaintenance = maintenanceList.sumOf { it.value }
        binding.tvTotalMaintenance.text = "R$ ${String.format("%.2f", totalMaintenance)}"
    }

    private fun updateFuelSummary() {
        val fuelList = viewModel.fuelHistory.value
        val totalFuel = fuelList.sumOf { it.value }
        val totalKmReal = calculateRealKmDriven(fuelList)
        val averageKmPerLiter = if (fuelList.isNotEmpty() && totalKmReal > 0) {
            val totalLitros = fuelList.sumOf { it.liters }
            if (totalLitros > 0) totalKmReal / totalLitros else 0.0
        } else 0.0
        
        // ✅ NOVO: Obter km atual (último abastecimento)
        val kmAtual = getCurrentMileage(fuelList)
        
        binding.tvTotalFuel.text = "R$ ${String.format("%.2f", totalFuel)}"
        binding.tvTotalKm.text = "${String.format("%.0f", kmAtual)} km"
        binding.tvAverageKmPerLiter.text = "${String.format("%.1f", averageKmPerLiter)} km/l"
    }
    
    /**
     * Obtém o km atual (último abastecimento) do veículo.
     * Implementa a mesma lógica do ViewModel para consistência.
     */
    private fun getCurrentMileage(fuelList: List<FuelRecord>): Double {
        if (fuelList.isEmpty()) {
            val vehicle = viewModel.vehicle.value
            return vehicle?.mileage ?: 0.0
        }
        
        // Ordenar por data e pegar o mais recente
        val ultimoAbastecimento = fuelList.maxByOrNull { it.date }
        return ultimoAbastecimento?.km ?: (viewModel.vehicle.value?.mileage ?: 0.0)
    }
    
    /**
     * Calcula o km real rodado considerando o km inicial do veículo.
     * Implementa a mesma lógica do ViewModel para consistência.
     */
    private fun calculateRealKmDriven(fuelList: List<FuelRecord>): Double {
        if (fuelList.isEmpty()) return 0.0
        
        // Obter km inicial do veículo do ViewModel
        val vehicle = viewModel.vehicle.value
        val kmInicial = vehicle?.mileage ?: 0.0
        
        // Ordenar abastecimentos por data (mais antigo primeiro)
        val sortedFuelList = fuelList.sortedBy { it.date }
        
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

    private fun updateSummaryCards(summary: VehicleSummary) {
        binding.tvTotalMaintenance.text = "R$ ${String.format("%.2f", summary.totalMaintenance)}"
        binding.tvTotalFuel.text = "R$ ${String.format("%.2f", summary.totalFuel)}"
        binding.tvTotalKm.text = "${String.format("%.0f", summary.totalKm)} km"
        binding.tvAverageKmPerLiter.text = "${String.format("%.1f", summary.averageKmPerLiter)} km/l"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

