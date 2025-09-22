package com.example.gestaobilhares.ui.mesas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.R
import com.example.gestaobilhares.databinding.FragmentMesasReformadasBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Fragment que exibe a lista de mesas reformadas.
 * Permite visualizar o histórico de reformas e adicionar novas reformas.
 */
@AndroidEntryPoint
class MesasReformadasFragment : Fragment() {

    private var _binding: FragmentMesasReformadasBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MesasReformadasViewModel by viewModels()
    private lateinit var adapter: MesasReformadasAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMesasReformadasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        
        // Carregar dados
        viewModel.carregarMesasReformadas()
    }

    private fun setupRecyclerView() {
        adapter = MesasReformadasAdapter { mesaReformada ->
            // TODO: Implementar navegação para detalhes da reforma
        }

        binding.rvMesasReformadas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MesasReformadasFragment.adapter
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.fabNovaReforma.setOnClickListener {
            // Navegar para a tela de nova reforma
            findNavController().navigate(R.id.novaReformaFragment)
        }
    }

    private fun observeViewModel() {
        viewModel.mesasReformadas.observe(viewLifecycleOwner) { mesas ->
            adapter.submitList(mesas)
            
            // Mostrar/ocultar estado vazio
            binding.emptyStateLayout.visibility = if (mesas.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // TODO: Implementar loading state se necessário
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                // TODO: Mostrar erro
                viewModel.clearError()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
