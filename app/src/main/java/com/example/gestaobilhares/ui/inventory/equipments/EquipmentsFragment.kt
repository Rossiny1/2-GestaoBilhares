package com.example.gestaobilhares.ui.inventory.equipments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.databinding.FragmentEquipmentsBinding
import kotlinx.coroutines.launch

class EquipmentsFragment : Fragment() {
    private var _binding: FragmentEquipmentsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EquipmentsViewModel by viewModels()
    private lateinit var adapter: EquipmentsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEquipmentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeData()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        adapter = EquipmentsAdapter { equipment ->
            // TODO: Implementar navegação para detalhes do equipamento
        }
        binding.rvEquipments.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEquipments.adapter = adapter
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.equipments.collect { equipments ->
                adapter.submitList(equipments)
            }
        }
    }

    private fun setupClickListeners() {
        binding.fabAddEquipment.setOnClickListener {
            AddEditEquipmentDialog().show(childFragmentManager, "add_equipment")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

