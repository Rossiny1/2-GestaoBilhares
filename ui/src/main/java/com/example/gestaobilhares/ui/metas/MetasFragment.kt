package com.example.gestaobilhares.ui.metas
import com.example.gestaobilhares.ui.R

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.gestaobilhares.data.entities.TipoMeta
import com.example.gestaobilhares.ui.databinding.FragmentMetasBinding
import kotlinx.coroutines.launch

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MetasFragment : Fragment() {

    private var _binding: FragmentMetasBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MetasViewModel by viewModels()
    private lateinit var metasAdapter: MetasAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMetasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ViewModel initialized by Hilt
        
        setupToolbar()
        setupRecyclerView()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // Atualiza ao voltar de outra tela/diálogo
        viewModel.carregarMetasRotas()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        
        // ✅ CORREÇÃO: Configurar botão voltar do header se existir
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // ✅ ATUALIZADO: Botão histórico navega para tela de histórico global
        binding.btnHistory.setOnClickListener {
            findNavController().navigate(com.example.gestaobilhares.ui.R.id.action_metasFragment_to_metaHistoricoFragment)
        }
    }

    private fun setupRecyclerView() {
        metasAdapter = MetasAdapter(
            onDetailsClick = { metaRota ->
                // ✅ ATUALIZADO: Cards não navegam mais para histórico individual
                // O histórico agora é global e acessado pelo botão no toolbar
            },
            onCreateMetaClick = { metaRota ->
                // ✅ NOVO: Botão "Criar Metas" navega para cadastro
                try {
                    val bundle = Bundle().apply {
                        putLong("rota_id", metaRota.rota.id)
                    }
                    findNavController().navigate(com.example.gestaobilhares.ui.R.id.metaCadastroFragment, bundle)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Erro ao abrir cadastro de metas: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )

        binding.recyclerViewMetas.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = metasAdapter
        }
    }

    private fun observeViewModel() {
        // Recarregar imediatamente quando voltamos para este fragment
        // (garante que a lista reflita metas recém-criadas sem esperar auto-refresh)
        viewModel.carregarMetasRotas()

        // Observar metas filtradas
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getMetasFiltradas().collect { metas ->
                    metasAdapter.submitList(metas)
                    updateEmptyState(metas.isEmpty())
                }
            }
        }

        // Observar loading
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    binding.swipeRefresh.isRefreshing = isLoading
                }
            }
        }

        // Observar mensagens
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.message.collect { message ->
                    message?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                        viewModel.limparMensagem()
                    }
                }
            }
        }

        // Observar notificações
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Evitar toasts repetitivos: não exibir notificações automaticamente
                viewModel.notificacoes.collect { _ -> }
            }
        }

        // Configurar swipe refresh
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.carregarMetasRotas()
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.layoutEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerViewMetas.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun getTipoMetaFormatado(tipoMeta: TipoMeta): String {
        return when (tipoMeta) {
            TipoMeta.FATURAMENTO -> "Faturamento"
            TipoMeta.CLIENTES_ACERTADOS -> "Clientes Acertados"
            TipoMeta.MESAS_LOCADAS -> "Mesas Locadas"
            TipoMeta.TICKET_MEDIO -> "Ticket Médio"
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

