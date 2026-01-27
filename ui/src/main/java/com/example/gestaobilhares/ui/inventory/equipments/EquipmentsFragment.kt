package com.example.gestaobilhares.ui.inventory.equipments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.ui.databinding.FragmentEquipmentsBinding
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.viewModels
import javax.inject.Inject

@AndroidEntryPoint
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
        adapter = EquipmentsAdapter(
            onEquipmentClick = { equipment ->
                // TODO: Implementar navegação para detalhes do equipamento
            },
            onEditClick = { equipment ->
                // Abrir dialog de edição com dados preenchidos
                val dialog = AddEditEquipmentDialog.newInstance(equipment)
                dialog.show(parentFragmentManager, "EditEquipmentDialog")
            }
        )
        binding.rvEquipments.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEquipments.adapter = adapter
    }

    private fun observeData() {
        // ✅ CORRIGIDO: Observar StateFlow com repeatOnLifecycle (igual outras telas)
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.equipments.collect { equipments ->
                    android.util.Log.d("EquipmentsFragment", "Equipamentos atualizados: ${equipments.size}")
                    adapter.submitList(equipments)
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.fabAddEquipment.setOnClickListener {
            // ✅ CORRIGIDO: O companion object do ViewModel compartilha a lista entre instâncias
            // Então o dialog pode criar sua própria instância e ainda compartilhar os dados
            AddEditEquipmentDialog.newInstance().show(childFragmentManager, "add_equipment")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

