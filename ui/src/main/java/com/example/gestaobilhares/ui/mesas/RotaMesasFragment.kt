package com.example.gestaobilhares.ui.mesas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.example.gestaobilhares.ui.databinding.FragmentRotaMesasBinding
import kotlinx.coroutines.launch

class RotaMesasFragment : Fragment() {

    private var _binding: FragmentRotaMesasBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: RotaMesasViewModel
    private lateinit var adapter: RotaMesasListAdapter
    private val args: RotaMesasFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRotaMesasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // ✅ CORREÇÃO: Inicializar ViewModel manualmente
        val appRepository = com.example.gestaobilhares.factory.RepositoryFactory.getAppRepository(requireContext())
        viewModel = RotaMesasViewModel(appRepository)

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()

        // Carregar dados da rota
        viewModel.loadMesasRota(args.rotaId)

        // Atualizar título
        binding.tvTituloRota.text = "🎱 ${args.rotaNome}"
    }

    private fun setupRecyclerView() {
        adapter = RotaMesasListAdapter(
            mesas = emptyList(),
            onMesaClick = { mesa ->
                // Navegar para a tela de edição da mesa
                val action = RotaMesasFragmentDirections.actionRotaMesasFragmentToEditMesaFragment(
                    mesaId = mesa.id
                )
                findNavController().navigate(action)
            }
        )

        binding.rvMesasRota.adapter = adapter
        binding.rvMesasRota.layoutManager = GridLayoutManager(requireContext(), 2)
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.mesasRota.collect { mesas ->
                adapter.updateData(mesas)

                // Mostrar/ocultar estado vazio
                if (mesas.isEmpty()) {
                    binding.emptyStateLayout.visibility = View.VISIBLE
                    binding.rvMesasRota.visibility = View.GONE
                } else {
                    binding.emptyStateLayout.visibility = View.GONE
                    binding.rvMesasRota.visibility = View.VISIBLE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.estatisticas.collect { stats ->
                binding.tvTotalSinuca.text = stats.totalSinuca.toString()
                binding.tvTotalJukebox.text = stats.totalJukebox.toString()
                binding.tvTotalPembolim.text = stats.totalPembolim.toString()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loading.collect { _ ->
                // TODO: Implementar loading indicator se necessário
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
