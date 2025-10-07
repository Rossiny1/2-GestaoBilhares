package com.example.gestaobilhares.ui.inventory.others

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.databinding.FragmentOthersInventoryBinding
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.repository.StockItemRepository
import kotlinx.coroutines.launch

class OthersInventoryFragment : Fragment() {
    private var _binding: FragmentOthersInventoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: OthersInventoryViewModel
    private lateinit var adapter: OthersInventoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOthersInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // ✅ CORREÇÃO: Inicializar ViewModel manualmente
        viewModel = OthersInventoryViewModel() // Construtor padrão
        
        setupRecyclerView()
        observeData()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        adapter = OthersInventoryAdapter { otherItem ->
            // TODO: Implementar navegação para detalhes do item
        }
        binding.rvOthersInventory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOthersInventory.adapter = adapter
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.othersItems.collect { items ->
                adapter.submitList(items)
            }
        }
    }

    private fun setupClickListeners() {
        binding.fabAddOtherItem.setOnClickListener {
            AddEditOtherItemDialog().show(childFragmentManager, "add_other_item")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

