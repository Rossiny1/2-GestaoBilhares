package com.example.gestaobilhares.ui.inventory.others

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.databinding.FragmentOthersInventoryBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OthersInventoryFragment : Fragment() {
    private var _binding: FragmentOthersInventoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OthersInventoryViewModel by viewModels()
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
