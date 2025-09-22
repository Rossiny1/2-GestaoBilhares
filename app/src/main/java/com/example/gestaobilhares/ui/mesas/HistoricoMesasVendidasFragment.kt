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
import com.example.gestaobilhares.databinding.FragmentHistoricoMesasVendidasBinding
import com.example.gestaobilhares.ui.mesas.adapter.MesasVendidasAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Fragment para exibir o histórico de mesas vendidas
 * ✅ NOVO: SISTEMA DE VENDA DE MESAS
 */
@AndroidEntryPoint
class HistoricoMesasVendidasFragment : Fragment() {

    private var _binding: FragmentHistoricoMesasVendidasBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: HistoricoMesasVendidasViewModel by viewModels()
    private lateinit var adapter: MesasVendidasAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoricoMesasVendidasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        android.util.Log.d("HistoricoMesasVendidasFragment", "🚀 Inicializando fragment de histórico de mesas vendidas...")
        
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        
        // Carregar dados
        android.util.Log.d("HistoricoMesasVendidasFragment", "📋 Carregando mesas vendidas...")
        viewModel.carregarMesasVendidas()
    }

    private fun setupRecyclerView() {
        adapter = MesasVendidasAdapter { mesaVendida ->
            android.util.Log.d("HistoricoMesasVendidasFragment", "Mesa vendida clicada: ${mesaVendida.numeroMesa}")
            abrirDetalhesMesaVendida(mesaVendida)
        }
        
        binding.rvMesasVendidas.adapter = adapter
        binding.rvMesasVendidas.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.fabVendaMesa.setOnClickListener {
            // Abrir dialog de venda de mesa
            viewModel.abrirDialogVendaMesa()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.mesasVendidas.collect { mesas ->
                android.util.Log.d("HistoricoMesasVendidasFragment", "📊 Mesas vendidas recebidas: ${mesas.size}")
                adapter.updateData(mesas)
                
                // Mostrar/ocultar estado vazio
                if (mesas.isEmpty()) {
                    binding.emptyStateLayout.visibility = View.VISIBLE
                    binding.rvMesasVendidas.visibility = View.GONE
                    android.util.Log.d("HistoricoMesasVendidasFragment", "📭 Exibindo estado vazio")
                } else {
                    binding.emptyStateLayout.visibility = View.GONE
                    binding.rvMesasVendidas.visibility = View.VISIBLE
                    android.util.Log.d("HistoricoMesasVendidasFragment", "📋 Exibindo lista com ${mesas.size} mesas")
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                error?.let {
                    // TODO: Mostrar snackbar com erro
                    android.util.Log.e("HistoricoMesasVendidasFragment", "Erro: $it")
                    viewModel.limparErro()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.showVendaDialog.collect { show ->
                if (show) {
                    abrirDialogVendaMesa()
                    viewModel.dialogVendaAberto()
                }
            }
        }
    }

    private fun abrirDialogVendaMesa() {
        val dialog = VendaMesaDialog.newInstance { mesaVendida ->
            // Recarregar dados após venda
            viewModel.recarregarDados()
            android.util.Log.d("HistoricoMesasVendidasFragment", "Mesa vendida: ${mesaVendida.numeroMesa}")
            // Mostrar snackbar de sucesso
            com.google.android.material.snackbar.Snackbar.make(binding.root, "Mesa vendida com sucesso!", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
        }
        dialog.show(parentFragmentManager, "VendaMesaDialog")
    }

    private fun abrirDetalhesMesaVendida(mesaVendida: com.example.gestaobilhares.data.entities.MesaVendida) {
        val dialog = DetalhesMesaVendidaDialog.newInstance(mesaVendida)
        dialog.show(parentFragmentManager, "DetalhesMesaVendidaDialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
