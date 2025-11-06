package com.example.gestaobilhares.ui.mesas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.R
import com.example.gestaobilhares.data.entities.MesaReformada
import com.example.gestaobilhares.databinding.FragmentMesasReformadasBinding
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.repository.MesaReformadaRepository

/**
 * Fragment que exibe a lista de mesas reformadas.
 * Permite visualizar o histórico de reformas e adicionar novas reformas.
 */
class MesasReformadasFragment : Fragment() {

    private var _binding: FragmentMesasReformadasBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MesasReformadasViewModel
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
        
        // ✅ CORREÇÃO: Inicializar ViewModel manualmente
        val database = AppDatabase.getDatabase(requireContext())
        val mesaReformadaRepository = MesaReformadaRepository(database.mesaReformadaDao())
        val appRepository = com.example.gestaobilhares.data.factory.RepositoryFactory.getAppRepository(requireContext())
        viewModel = MesasReformadasViewModel(mesaReformadaRepository, appRepository)
        
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        
        // Carregar dados
        viewModel.carregarMesasReformadas()
    }

    private fun setupRecyclerView() {
        adapter = MesasReformadasAdapter { mesaComHistorico ->
            // Mostrar detalhes da primeira reforma (ou criar um diálogo específico)
            if (mesaComHistorico.reformas.isNotEmpty()) {
                mostrarDetalhesMesaReformada(mesaComHistorico.reformas.first())
            }
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
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mesasReformadas.collect { mesas ->
                    adapter.submitList(mesas)
                    
                    // Mostrar/ocultar estado vazio
                    binding.emptyStateLayout.visibility = if (mesas.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    // TODO: Implementar loading state se necessário
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorMessage.collect { message ->
                    message?.let {
                        // TODO: Mostrar erro
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    private fun mostrarDetalhesMesaReformada(mesaReformada: MesaReformada) {
        try {
            val dialog = DetalhesMesaReformadaDialog.newInstance(mesaReformada)
            dialog.show(parentFragmentManager, "DetalhesMesaReformadaDialog")
        } catch (e: Exception) {
            // Log do erro se necessário
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

