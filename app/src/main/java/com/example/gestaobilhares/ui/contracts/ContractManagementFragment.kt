package com.example.gestaobilhares.ui.contracts

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.R
import com.example.gestaobilhares.databinding.FragmentContractManagementBinding
import com.example.gestaobilhares.data.entities.ContratoLocacao
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.ui.contracts.ContractManagementAdapter
import com.example.gestaobilhares.ui.contracts.ContractManagementViewModel
import com.example.gestaobilhares.utils.ContractPdfGenerator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.repository.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Fragment para gerenciamento de contratos
 * Permite visualizar, filtrar e gerenciar todos os contratos de locação
 */
@AndroidEntryPoint
class ContractManagementFragment : Fragment() {

    private var _binding: FragmentContractManagementBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ContractManagementViewModel by viewModels()
    private lateinit var contractAdapter: ContractManagementAdapter

    @Inject
    lateinit var database: AppDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContractManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupClickListeners()
        setupFilters()
        observeViewModel()
        
        // Carregar dados iniciais
        viewModel.loadContractData()
    }

    private fun setupRecyclerView() {
        contractAdapter = ContractManagementAdapter(
            onContractClick = { contrato ->
                // Navegar para detalhes do contrato
                navigateToContractDetails(contrato)
            },
            onViewClick = { contrato ->
                // Visualizar contrato
                viewContract(contrato)
            },
            onShareClick = { contrato ->
                // Compartilhar contrato
                shareContract(contrato)
            }
        )

        binding.rvContracts.apply {
            adapter = contractAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupClickListeners() {
        // Botão voltar
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Botão buscar
        binding.fabSearch.setOnClickListener {
            showSearchDialog()
        }

        // Botão filtrar
        binding.fabFilter.setOnClickListener {
            showFilterDialog()
        }

        // FAB expandível
        setupFabMenu()
    }

    private fun setupFabMenu() {
        var isExpanded = false

        // FAB Principal
        binding.fabMain.setOnClickListener {
            if (isExpanded) {
                collapseFabMenu()
                isExpanded = false
            } else {
                expandFabMenu()
                isExpanded = true
            }
        }

        // FAB Gerar Contrato
        binding.fabGenerateContractContainer.setOnClickListener {
            // Navegar para seleção de cliente para gerar contrato
            findNavController().navigate(R.id.clientListFragment)
            collapseFabMenu()
            isExpanded = false
        }
    }

    private fun expandFabMenu() {
        binding.fabExpandedContainer.visibility = View.VISIBLE

        // Animar FAB principal
        binding.fabMain.animate()
            .rotation(45f)
            .setDuration(200)
            .start()

        // Animar FABs expandidos
        binding.fabGenerateContractContainer.animate()
            .alpha(1f)
            .translationY(-16f)
            .setDuration(200)
            .start()
    }

    private fun collapseFabMenu() {
        // Animar FAB principal
        binding.fabMain.animate()
            .rotation(0f)
            .setDuration(200)
            .start()

        // Animar FABs expandidos
        binding.fabGenerateContractContainer.animate()
            .alpha(0f)
            .translationY(0f)
            .setDuration(200)
            .withEndAction {
                if (_binding != null) {
                    binding.fabExpandedContainer.visibility = View.GONE
                }
            }
            .start()
    }

    private fun setupFilters() {
        // Configurar chips de filtro
        binding.chipTodos.setOnClickListener {
            viewModel.setFilter(ContractManagementViewModel.ContractFilter.ALL)
            updateFilterChips(ContractManagementViewModel.ContractFilter.ALL)
        }

        binding.chipComContrato.setOnClickListener {
            viewModel.setFilter(ContractManagementViewModel.ContractFilter.WITH_CONTRACT)
            updateFilterChips(ContractManagementViewModel.ContractFilter.WITH_CONTRACT)
        }

        binding.chipSemContrato.setOnClickListener {
            viewModel.setFilter(ContractManagementViewModel.ContractFilter.WITHOUT_CONTRACT)
            updateFilterChips(ContractManagementViewModel.ContractFilter.WITHOUT_CONTRACT)
        }

        binding.chipAssinados.setOnClickListener {
            viewModel.setFilter(ContractManagementViewModel.ContractFilter.SIGNED)
            updateFilterChips(ContractManagementViewModel.ContractFilter.SIGNED)
        }
    }

    private fun updateFilterChips(filter: ContractManagementViewModel.ContractFilter) {
        binding.chipTodos.isChecked = filter == ContractManagementViewModel.ContractFilter.ALL
        binding.chipComContrato.isChecked = filter == ContractManagementViewModel.ContractFilter.WITH_CONTRACT
        binding.chipSemContrato.isChecked = filter == ContractManagementViewModel.ContractFilter.WITHOUT_CONTRACT
        binding.chipAssinados.isChecked = filter == ContractManagementViewModel.ContractFilter.SIGNED
    }

    private fun observeViewModel() {
        // Observar estatísticas
        viewModel.statistics.observe(viewLifecycleOwner) { stats ->
            binding.tvTotalClientes.text = stats.totalClientes.toString()
            binding.tvContratosGerados.text = stats.contratosGerados.toString()
            binding.tvContratosAssinados.text = stats.contratosAssinados.toString()
        }

        // Observar lista de contratos
        viewModel.contracts.observe(viewLifecycleOwner) { contracts ->
            contractAdapter.submitList(contracts)
            
            // Mostrar/ocultar estado vazio
            if (contracts.isEmpty()) {
                binding.layoutEmpty.visibility = View.VISIBLE
                binding.rvContracts.visibility = View.GONE
            } else {
                binding.layoutEmpty.visibility = View.GONE
                binding.rvContracts.visibility = View.VISIBLE
            }
        }

        // Observar loading
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Implementar loading se necessário
        }
    }

    private fun navigateToContractDetails(contrato: ContratoLocacao?) {
        // Navegar para detalhes do contrato
        // TODO: Implementar navegação para detalhes
        Toast.makeText(requireContext(), "Detalhes do contrato ${contrato?.numeroContrato}", Toast.LENGTH_SHORT).show()
    }

    private fun viewContract(contrato: ContratoLocacao?) {
        // Visualizar contrato
        lifecycleScope.launch {
            try {
                if (contrato == null) {
                    Toast.makeText(requireContext(), "Contrato não encontrado", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // Gerar PDF do contrato para visualização
                val contractPdfGenerator = ContractPdfGenerator(requireContext())
                val mesas = contrato?.let { 
                    viewModel.getMesasPorCliente(it.clienteId)
                } ?: emptyList()
                val pdfFile = contractPdfGenerator.generateContractPdf(contrato, mesas)
                
                if (pdfFile.exists()) {
                    // Abrir PDF com visualizador padrão
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(FileProvider.getUriForFile(
                            requireContext(),
                            "${requireContext().packageName}.fileprovider",
                            pdfFile
                        ), "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    
                    if (intent.resolveActivity(requireContext().packageManager) != null) {
                        startActivity(intent)
                    } else {
                        Toast.makeText(requireContext(), "Nenhum visualizador de PDF encontrado", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Erro ao gerar PDF do contrato", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro ao visualizar contrato: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun shareContract(contrato: ContratoLocacao?) {
        // Compartilhar contrato
        lifecycleScope.launch {
            try {
                if (contrato == null) {
                    Toast.makeText(requireContext(), "Contrato não encontrado", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // Gerar PDF do contrato para compartilhamento
                val contractPdfGenerator = ContractPdfGenerator(requireContext())
                val mesas = contrato?.let { 
                    viewModel.getMesasPorCliente(it.clienteId)
                } ?: emptyList()
                val pdfFile = contractPdfGenerator.generateContractPdf(contrato, mesas)
                
                if (pdfFile.exists()) {
                    // Compartilhar PDF via WhatsApp ou outros apps
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                            requireContext(),
                            "${requireContext().packageName}.fileprovider",
                            pdfFile
                        ))
                        putExtra(Intent.EXTRA_SUBJECT, "Contrato ${contrato.numeroContrato}")
                        putExtra(Intent.EXTRA_TEXT, "Contrato de locação - ${contrato.numeroContrato}")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    
                    val chooserIntent = Intent.createChooser(shareIntent, "Compartilhar contrato")
                    if (shareIntent.resolveActivity(requireContext().packageManager) != null) {
                        startActivity(chooserIntent)
                    } else {
                        Toast.makeText(requireContext(), "Nenhum app de compartilhamento encontrado", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Erro ao gerar PDF do contrato", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro ao compartilhar contrato: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showSearchDialog() {
        // Implementar diálogo de busca
        val editText = TextInputEditText(requireContext()).apply {
            hint = "Digite o nome do cliente ou número do contrato"
            setPadding(50, 30, 50, 30)
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Buscar Contratos")
            .setView(editText)
            .setPositiveButton("Buscar") { _, _ ->
                val searchQuery = editText.text?.toString()?.trim()
                if (!searchQuery.isNullOrEmpty()) {
                    viewModel.searchContracts(searchQuery)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showFilterDialog() {
        // Implementar diálogo de filtros avançados
        val filterOptions = arrayOf("Todos", "Com Contrato", "Sem Contrato", "Assinados", "Por Rota")
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Filtros Avançados")
            .setItems(filterOptions) { _, which ->
                when (which) {
                    0 -> viewModel.setFilter(ContractManagementViewModel.ContractFilter.ALL)
                    1 -> viewModel.setFilter(ContractManagementViewModel.ContractFilter.WITH_CONTRACT)
                    2 -> viewModel.setFilter(ContractManagementViewModel.ContractFilter.WITHOUT_CONTRACT)
                    3 -> viewModel.setFilter(ContractManagementViewModel.ContractFilter.SIGNED)
                    4 -> showRouteFilterDialog()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun showRouteFilterDialog() {
        // Implementar filtro por rota
        lifecycleScope.launch {
            try {
                val rotas = viewModel.getAllRoutes()
                val routeNames = rotas.map { it.nome }.toTypedArray()
                
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Filtrar por Rota")
                    .setItems(routeNames) { _, which ->
                        val rotaSelecionada = rotas[which]
                        viewModel.setFilterByRoute(rotaSelecionada.id)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro ao carregar rotas: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
