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
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.databinding.FragmentHistoricoManutencaoMesaBinding
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.repository.HistoricoManutencaoMesaRepository

/**
 * Fragment que exibe o histórico completo de manutenção de uma mesa.
 */
class HistoricoManutencaoMesaFragment : Fragment() {

    private var _binding: FragmentHistoricoManutencaoMesaBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HistoricoManutencaoMesaViewModel
    private val args: HistoricoManutencaoMesaFragmentArgs by navArgs()
    private lateinit var adapter: HistoricoManutencaoMesaAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoricoManutencaoMesaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ CORREÇÃO: Inicializar ViewModel manualmente
        val appRepository = com.example.gestaobilhares.data.factory.RepositoryFactory.getAppRepository(requireContext())
        viewModel = HistoricoManutencaoMesaViewModel(appRepository)

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()

        // Carregar dados da mesa
        viewModel.carregarHistoricoMesa(args.mesaId)
    }

    private fun setupRecyclerView() {
        adapter = HistoricoManutencaoMesaAdapter()

        binding.rvHistoricoManutencao.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HistoricoManutencaoMesaFragment.adapter
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mesa.collect { mesa ->
                    mesa?.let {
                        binding.tvNumeroMesa.text = "Mesa ${it.numero}"
                        binding.tvTipoTamanhoMesa.text = "${it.tipoMesa} - ${it.tamanho}"
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.historicoManutencao.collect { historico ->
                    adapter.submitList(historico)

                    // Mostrar/ocultar estado vazio
                    binding.emptyStateLayout.visibility = if (historico.isEmpty()) View.VISIBLE else View.GONE
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

