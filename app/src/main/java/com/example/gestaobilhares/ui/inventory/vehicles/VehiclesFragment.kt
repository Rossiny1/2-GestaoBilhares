package com.example.gestaobilhares.ui.inventory.vehicles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.R
import com.example.gestaobilhares.databinding.FragmentVehiclesBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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
        adapter = VehiclesAdapter { vehicle ->
            // Navegar para tela de detalhes do veículo
            val bundle = Bundle().apply {
                putLong("vehicleId", vehicle.id)
            }
            findNavController().navigate(R.id.vehicleDetailFragment, bundle)
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


