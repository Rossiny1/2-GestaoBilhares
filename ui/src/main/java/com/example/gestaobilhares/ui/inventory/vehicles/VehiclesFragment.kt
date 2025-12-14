package com.example.gestaobilhares.ui.inventory.vehicles
import com.example.gestaobilhares.ui.R

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.ui.databinding.FragmentVehiclesBinding
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class VehiclesFragment : Fragment() {
    private var _binding: FragmentVehiclesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VehiclesViewModel by viewModels()
    private lateinit var adapter: VehiclesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVehiclesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Configurar toolbar com título "Veículo" e navegação back
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }
        adapter = VehiclesAdapter { vehicle ->
            // Navegar para tela de detalhes do veículo
            val bundle = Bundle().apply {
                putLong("vehicleId", vehicle.id)
            }
            findNavController().navigate(com.example.gestaobilhares.ui.R.id.vehicleDetailFragment, bundle)
        }
        binding.rvVehicles.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVehicles.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.vehicles.collect { list ->
                adapter.submitList(list)
            }
        }

        binding.fabAddVehicle.setOnClickListener {
            AddEditVehicleDialog().show(childFragmentManager, "add_vehicle")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

