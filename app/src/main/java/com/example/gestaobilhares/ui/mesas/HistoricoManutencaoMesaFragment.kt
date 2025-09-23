package com.example.gestaobilhares.ui.mesas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.databinding.FragmentHistoricoManutencaoMesaBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Fragment que exibe o histórico completo de manutenção de uma mesa.
 */
@AndroidEntryPoint
class HistoricoManutencaoMesaFragment : Fragment() {

    private var _binding: FragmentHistoricoManutencaoMesaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoricoManutencaoMesaViewModel by viewModels()
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
        viewModel.mesa.observe(viewLifecycleOwner) { mesa ->
            mesa?.let {
                binding.tvNumeroMesa.text = "Mesa ${it.numero}"
                binding.tvTipoTamanhoMesa.text = "${it.tipoMesa} - ${it.tamanho}"
            }
        }

        viewModel.historicoManutencao.observe(viewLifecycleOwner) { historico ->
            adapter.submitList(historico)

            // Mostrar/ocultar estado vazio
            binding.emptyStateLayout.visibility = if (historico.isEmpty()) View.VISIBLE else View.GONE
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
