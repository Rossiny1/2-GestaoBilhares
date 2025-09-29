package com.example.gestaobilhares.ui.metas

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
import com.example.gestaobilhares.R
import com.example.gestaobilhares.data.entities.TipoMeta
import com.example.gestaobilhares.databinding.FragmentMetasBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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
        setupToolbar()
        setupRecyclerView()
        setupFilters()
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

        binding.btnFilters.setOnClickListener {
            toggleFilters()
        }

        binding.btnHistory.setOnClickListener {
            viewModel.alternarHistorico()
        }
    }

    private fun setupRecyclerView() {
        metasAdapter = MetasAdapter { metaRota ->
            // Navegar para cadastro de metas com a rota pre-selecionada
            try {
                val bundle = Bundle().apply {
                    putLong("rota_id", metaRota.rota.id)
                }
                findNavController().navigate(R.id.metaCadastroFragment, bundle)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro ao abrir cadastro de metas: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        binding.recyclerViewMetas.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = metasAdapter
        }
    }

    private fun setupFilters() {
        // Configurar filtro por tipo de meta
        val tiposMeta = listOf("Todos") + TipoMeta.values().map { getTipoMetaFormatado(it) }
        val tipoMetaAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tiposMeta)
        binding.actvTipoMeta.setAdapter(tipoMetaAdapter)

        binding.actvTipoMeta.setOnItemClickListener { _, _, position, _ ->
            val tipoSelecionado = if (position == 0) null else TipoMeta.values()[position - 1]
            viewModel.aplicarFiltroTipoMeta(tipoSelecionado)
        }

        // Configurar filtro por status
        val statusOptions = listOf("Todos", "Próximas", "Atingidas", "Em Andamento", "Finalizadas")
        val statusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, statusOptions)
        binding.actvStatus.setAdapter(statusAdapter)

        binding.actvStatus.setOnItemClickListener { _, _, position, _ ->
            val statusSelecionado = when (position) {
                0 -> null
                1 -> StatusFiltroMeta.PROXIMAS
                2 -> StatusFiltroMeta.ATINGIDAS
                3 -> StatusFiltroMeta.EM_ANDAMENTO
                4 -> StatusFiltroMeta.FINALIZADAS
                else -> null
            }
            viewModel.aplicarFiltroStatus(statusSelecionado)
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

        // Observar histórico
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mostrarHistorico.collect { mostrarHistorico ->
                    // Atualizar ícone e título baseado no modo histórico
                    binding.btnHistory.setImageResource(
                        if (mostrarHistorico) R.drawable.ic_today else R.drawable.ic_history
                    )
                    binding.toolbar.title = if (mostrarHistorico) "Histórico de Metas" else "Metas por Rota"
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

    private fun toggleFilters() {
        val isVisible = binding.layoutFilters.visibility == View.VISIBLE
        binding.layoutFilters.visibility = if (isVisible) View.GONE else View.VISIBLE
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
