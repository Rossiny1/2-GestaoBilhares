package com.example.gestaobilhares.ui.mesas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.databinding.FragmentGerenciarMesasBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GerenciarMesasFragment : Fragment() {

    private var _binding: FragmentGerenciarMesasBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: GerenciarMesasViewModel by viewModels()
    private lateinit var adapter: RotaMesasAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGerenciarMesasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = RotaMesasAdapter { rotaComMesas ->
            // Navegar para a tela de mesas da rota específica
            val action = GerenciarMesasFragmentDirections.actionGerenciarMesasFragmentToRotaMesasFragment(
                rotaId = rotaComMesas.rota.id,
                rotaNome = rotaComMesas.rota.nome
            )
            findNavController().navigate(action)
        }
        
        binding.rvRotas.adapter = adapter
        binding.rvRotas.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.cardDeposito.setOnClickListener {
            // Navegar para a tela de mesas do depósito indicando que veio do Gerenciar Mesas
            val action = GerenciarMesasFragmentDirections.actionGerenciarMesasFragmentToMesasDepositoFragment(
                clienteId = 0L, // Não há cliente específico
                isFromGerenciarMesas = true
            )
            findNavController().navigate(action)
        }

        // ✅ NOVO: FAB expandível para venda de mesas
        binding.fabMain.setOnClickListener {
            toggleFabMenu()
        }

        binding.fabVendaMesa.setOnClickListener {
            // Navegar para histórico de mesas vendidas
            val action = GerenciarMesasFragmentDirections.actionGerenciarMesasFragmentToHistoricoMesasVendidasFragment()
            findNavController().navigate(action)
            hideFabMenu()
        }
    }

    private var isFabMenuExpanded = false

    private fun toggleFabMenu() {
        if (isFabMenuExpanded) {
            hideFabMenu()
        } else {
            showFabMenu()
        }
    }

    private fun showFabMenu() {
        isFabMenuExpanded = true
        binding.fabVendaMesa.visibility = View.VISIBLE
        binding.fabVendaMesa.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(200)
            .start()
    }

    private fun hideFabMenu() {
        isFabMenuExpanded = false
        binding.fabVendaMesa.animate()
            .scaleX(0f)
            .scaleY(0f)
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                binding.fabVendaMesa.visibility = View.GONE
            }
            .start()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.estatisticasGerais.collect { stats ->
                binding.tvTotalSinuca.text = stats.totalSinuca.toString()
                binding.tvTotalJukebox.text = stats.totalJukebox.toString()
                binding.tvTotalPembolim.text = stats.totalPembolim.toString()
                
                binding.tvDepositoSinuca.text = "S: ${stats.depositoSinuca}"
                binding.tvDepositoJukebox.text = "J: ${stats.depositoJukebox}"
                binding.tvDepositoPembolim.text = "P: ${stats.depositoPembolim}"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.rotasComMesas.collect { rotas ->
                android.util.Log.d("GerenciarMesasFragment", "=== RECEBENDO ROTAS COM MESAS ===")
                android.util.Log.d("GerenciarMesasFragment", "Total rotas recebidas: ${rotas.size}")

                rotas.forEach { rotaComMesas ->
                    android.util.Log.d("GerenciarMesasFragment", "Rota: ${rotaComMesas.rota.nome}")
                    android.util.Log.d("GerenciarMesasFragment", "Sinuca: ${rotaComMesas.sinuca}, Jukebox: ${rotaComMesas.jukebox}, Pembolim: ${rotaComMesas.pembolim}")
                }

                adapter.updateData(rotas)

                // Mostrar/ocultar estado vazio
                if (rotas.isEmpty()) {
                    binding.emptyStateLayout.visibility = View.VISIBLE
                    binding.rvRotas.visibility = View.GONE
                } else {
                    binding.emptyStateLayout.visibility = View.GONE
                    binding.rvRotas.visibility = View.VISIBLE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loading.collect { loading ->
                // TODO: Implementar loading indicator se necessário
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
