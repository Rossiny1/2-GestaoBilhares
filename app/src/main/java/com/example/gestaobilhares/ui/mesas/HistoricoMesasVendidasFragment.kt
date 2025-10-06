package com.example.gestaobilhares.ui.mesas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.databinding.FragmentHistoricoMesasVendidasBinding
import com.example.gestaobilhares.ui.mesas.adapter.MesasVendidasAdapter
import kotlinx.coroutines.launch

/**
 * Fragment para exibir o histórico de mesas vendidas
 * ✅ NOVO: SISTEMA DE VENDA DE MESAS
 */
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
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch {
                    viewModel.mesasVendidas.collect { mesas ->
                        adapter.updateData(mesas)
                        if (mesas.isEmpty()) {
                            binding.emptyStateLayout.visibility = View.VISIBLE
                            binding.rvMesasVendidas.visibility = View.GONE
                        } else {
                            binding.emptyStateLayout.visibility = View.GONE
                            binding.rvMesasVendidas.visibility = View.VISIBLE
                        }
                    }
                }
                // Loading spinner removido do layout e da observação
                launch {
                    viewModel.errorMessage.collect { error ->
                        error?.let {
                            try {
                                com.google.android.material.snackbar.Snackbar.make(binding.root, it, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
                            } catch (_: Exception) {
                                android.widget.Toast.makeText(requireContext(), it, android.widget.Toast.LENGTH_SHORT).show()
                            }
                            viewModel.limparErro()
                        }
                    }
                }
                launch {
                    viewModel.showVendaDialog.collect { show ->
                        if (show) {
                            abrirDialogVendaMesa()
                            viewModel.dialogVendaAberto()
                        }
                    }
                }
            }
        }
    }

    private fun abrirDialogVendaMesa() {
        val dialog = VendaMesaDialog.newInstance { mesaVendida ->
            // Recarregar dados após venda
            viewModel.recarregarDados()

            // Mostrar snackbar de sucesso
            try {
                com.google.android.material.snackbar.Snackbar.make(binding.root, "Mesa vendida com sucesso!", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
            } catch (e: Exception) {
                // Fallback para Toast se Snackbar falhar
                Toast.makeText(requireContext(), "Mesa vendida com sucesso!", Toast.LENGTH_SHORT).show()
            }
        }

        try {
            dialog.show(parentFragmentManager, "VendaMesaDialog")
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Erro ao abrir dialog de venda", Toast.LENGTH_SHORT).show()
        }
    }

    private fun abrirDetalhesMesaVendida(mesaVendida: com.example.gestaobilhares.data.entities.MesaVendida) {
        val dialog = DetalhesMesaVendidaDialog.newInstance(mesaVendida)
        dialog.show(parentFragmentManager, "DetalhesMesaVendidaDialog")
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d("HistoricoMesasVendidasFragment", "🔄 Voltando para a tela - recarregando dados...")
        // ✅ CORREÇÃO: Recarregar mesas vendidas sempre que voltar para a tela
        viewModel.carregarMesasVendidas()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

