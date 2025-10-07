package com.example.gestaobilhares.ui.inventory.stock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.databinding.FragmentStockBinding
import com.example.gestaobilhares.ui.inventory.stock.AddPanosLoteDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class StockFragment : Fragment() {
    private var _binding: FragmentStockBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: StockViewModel
    private lateinit var adapter: StockAdapter
    private lateinit var panosAdapter: PanosEstoqueAdapter
    private lateinit var panoGroupAdapter: PanoGroupAdapter
    private lateinit var concatAdapter: ConcatAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // ✅ CORREÇÃO: Inicializar ViewModel manualmente
        val database = com.example.gestaobilhares.data.database.AppDatabase.getDatabase(requireContext())
        val appRepository = com.example.gestaobilhares.data.repository.AppRepository(
            database.clienteDao(),
            database.acertoDao(),
            database.mesaDao(),
            database.rotaDao(),
            database.despesaDao(),
            database.colaboradorDao(),
            database.cicloAcertoDao(),
            database.acertoMesaDao(),
            database.contratoLocacaoDao(),
            database.aditivoContratoDao(),
            database.assinaturaRepresentanteLegalDao(),
            database.logAuditoriaAssinaturaDao()
        )
        val panoEstoqueRepository = com.example.gestaobilhares.data.repository.PanoEstoqueRepository(database.panoEstoqueDao())
        val stockItemRepository = com.example.gestaobilhares.data.repository.StockItemRepository(database.stockItemDao())
        viewModel = StockViewModel(panoEstoqueRepository, stockItemRepository)
        
        setupRecyclerView()
        observeData()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        adapter = StockAdapter { stockItem ->
            // TODO: Implementar navegação para detalhes do item
        }
        
        panosAdapter = PanosEstoqueAdapter { pano ->
            // TODO: Implementar navegação para detalhes do pano
        }
        
        panoGroupAdapter = PanoGroupAdapter { panoGroup ->
            // Mostrar detalhes do grupo de panos
            PanoDetailsDialog.newInstance(panoGroup).show(childFragmentManager, "pano_details")
        }
        
        binding.rvStock.layoutManager = LinearLayoutManager(requireContext())
        // Mostrar panos (agrupados) e itens genéricos simultaneamente
        concatAdapter = ConcatAdapter(panoGroupAdapter, adapter)
        binding.rvStock.adapter = concatAdapter
        android.util.Log.d("StockFragment", "RecyclerView configurado com ConcatAdapter (panos + itens)")
    }

    private fun observeData() {
        // Observar itens genéricos do estoque
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.stockItems.collect { items ->
                android.util.Log.d("StockFragment", "Itens genéricos recebidos: ${items.size}")
                adapter.submitList(items)
            }
        }

        // Observar grupos de panos (agrupados)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.panoGroups.collect { panoGroups ->
                android.util.Log.d("StockFragment", "Grupos de panos recebidos: ${panoGroups.size}")
                panoGroupAdapter.submitList(panoGroups)
            }
        }
    }

    private fun setupClickListeners() {
        binding.fabAddStockItem.setOnClickListener {
            showAddItemDialog()
        }
    }
    
    private fun showAddItemDialog() {
        val options = arrayOf("Adicionar Item Genérico", "Adicionar Panos em Lote")
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Adicionar ao Estoque")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> AddEditStockItemDialog().show(childFragmentManager, "add_stock_item")
                    1 -> AddPanosLoteDialog().show(childFragmentManager, "add_panos_lote")
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

