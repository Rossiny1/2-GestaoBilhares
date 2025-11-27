package com.example.gestaobilhares.ui.cycles
import com.example.gestaobilhares.ui.R

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.ui.databinding.FragmentCycleClientsBinding
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.repository.CicloAcertoRepository
import com.example.gestaobilhares.data.repository.AcertoRepository
import com.example.gestaobilhares.data.repository.ClienteRepository
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.factory.RepositoryFactory
import com.example.gestaobilhares.ui.cycles.adapter.CycleClientsAdapter
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/**
 * Fragment para listar clientes acertados do ciclo
 */
class CycleClientsFragment : Fragment() {

    private var _binding: FragmentCycleClientsBinding? = null
    private val binding get() = _binding!!
    
    private var cicloId: Long = 0L
    private var rotaId: Long = 0L
    
    private lateinit var viewModel: CycleClientsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cicloId = arguments?.getLong("cicloId", 0L) ?: 0L
        rotaId = arguments?.getLong("rotaId", 0L) ?: 0L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCycleClientsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // ✅ CORREÇÃO: Inicializar ViewModel manualmente
        val database = AppDatabase.getDatabase(requireContext())
        val appRepository = RepositoryFactory.getAppRepository(requireContext())
        val cicloAcertoRepository = CicloAcertoRepository(
            database.cicloAcertoDao(),
            database.despesaDao(), // ✅ CORRIGIDO: Passar DespesaDao diretamente
            AcertoRepository(database.acertoDao(), database.clienteDao()),
            ClienteRepository(database.clienteDao(), appRepository),
            database.rotaDao()
        )
        viewModel = CycleClientsViewModel(cicloAcertoRepository)
        
        setupRecyclerView()
        setupObservers()
        
        // Carregar clientes
        viewModel.carregarClientes(cicloId, rotaId)
    }

    private fun setupRecyclerView() {
        binding.rvClients.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = CycleClientsAdapter(
                onClientClick = { _ ->
                    // TODO: Implementar navegação para detalhes do cliente
                    mostrarFeedback("Detalhes do cliente serão implementados em breve", Snackbar.LENGTH_SHORT)
                }
            )
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.clientes.collect { clientes ->
                (binding.rvClients.adapter as? CycleClientsAdapter)?.submitList(clientes)
                atualizarEmptyState(clientes.isEmpty())
            }
        }
        
        lifecycleScope.launch {
            viewModel.isLoading.collect { carregando ->
                binding.progressBar.visibility = if (carregando) View.VISIBLE else View.GONE
            }
        }
        
        lifecycleScope.launch {
            viewModel.errorMessage.collect { mensagem ->
                mensagem?.let {
                    mostrarFeedback("Erro: $it", Snackbar.LENGTH_LONG)
                    viewModel.limparErro()
                }
            }
        }
    }

    private fun atualizarEmptyState(mostrar: Boolean) {
        binding.apply {
            if (mostrar) {
                emptyStateLayout.visibility = View.VISIBLE
                rvClients.visibility = View.GONE
            } else {
                emptyStateLayout.visibility = View.GONE
                rvClients.visibility = View.VISIBLE
            }
        }
    }

    private fun mostrarFeedback(mensagem: String, duracao: Int) {
        Snackbar.make(binding.root, mensagem, duracao)
            .setBackgroundTint(requireContext().getColor(com.example.gestaobilhares.ui.R.color.purple_600))
            .setTextColor(requireContext().getColor(com.example.gestaobilhares.ui.R.color.white))
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(cicloId: Long, rotaId: Long): CycleClientsFragment {
            val fragment = CycleClientsFragment()
            val args = Bundle()
            args.putLong("cicloId", cicloId)
            args.putLong("rotaId", rotaId)
            fragment.arguments = args
            return fragment
        }
    }
}

