package com.example.gestaobilhares.ui.inventory.stock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.databinding.FragmentStockBinding
import com.example.gestaobilhares.ui.inventory.stock.AddPanosLoteDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StockFragment : Fragment() {
    private var _binding: FragmentStockBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StockViewModel by viewModels()
    private lateinit var adapter: StockAdapter
    private lateinit var panosAdapter: PanosEstoqueAdapter

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
        
        binding.rvStock.layoutManager = LinearLayoutManager(requireContext())
        binding.rvStock.adapter = adapter
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.stockItems.collect { items ->
                adapter.submitList(items)
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.panosEstoque.collect { panos ->
                if (panos.isNotEmpty()) {
                    // Se há panos, mostrar os panos no RecyclerView
                    binding.rvStock.adapter = panosAdapter
                    panosAdapter.submitList(panos)
                } else {
                    // Se não há panos, mostrar itens genéricos
                    binding.rvStock.adapter = adapter
                }
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
