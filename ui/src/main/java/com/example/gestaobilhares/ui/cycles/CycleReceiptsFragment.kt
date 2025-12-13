package com.example.gestaobilhares.ui.cycles
import com.example.gestaobilhares.ui.R

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.ui.databinding.FragmentCycleReceiptsBinding
import com.example.gestaobilhares.data.entities.Acerto
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.data.entities.StatusCicloAcerto
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.repository.CicloAcertoRepository
import com.example.gestaobilhares.data.repository.AppRepository


import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.viewModels
import javax.inject.Inject

/**
 * Fragment para exibir lista de recebimentos (acertos) do ciclo
 * Similar ao que está no relatório PDF
 */
@AndroidEntryPoint
class CycleReceiptsFragment : Fragment() {

    private var _binding: FragmentCycleReceiptsBinding? = null
    private val binding get() = _binding!!
    
    private var cicloId: Long = 0L
    private var rotaId: Long = 0L
    private var isCicloFinalizado: Boolean = false
    
    private lateinit var receiptsAdapter: CycleReceiptsAdapter
    private val viewModel: CycleReceiptsViewModel by viewModels()
    
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

    companion object {
        fun newInstance(cicloId: Long, rotaId: Long, isCicloFinalizado: Boolean): CycleReceiptsFragment {
            return CycleReceiptsFragment().apply {
                arguments = Bundle().apply {
                    putLong("cicloId", cicloId)
                    putLong("rotaId", rotaId)
                    putBoolean("isCicloFinalizado", isCicloFinalizado)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            cicloId = it.getLong("cicloId", 0L)
            rotaId = it.getLong("rotaId", 0L)
            isCicloFinalizado = it.getBoolean("isCicloFinalizado", false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCycleReceiptsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // ✅ CORREÇÃO: Inicializar ViewModel -> Hilt
        // viewModel injetado via by viewModels()
        
        setupRecyclerView()
        setupObservers()
        
        // Carregar recebimentos
        viewModel.carregarRecebimentos(cicloId)
    }

    private fun setupRecyclerView() {
        receiptsAdapter = CycleReceiptsAdapter(
            isCicloFinalizado = isCicloFinalizado,
            onItemClick = { _ ->
                // Para ciclos finalizados, não permitir edição
                if (!isCicloFinalizado) {
                    // TODO: Implementar edição de acerto se necessário
                }
            }
        )
        
        binding.rvReceipts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = receiptsAdapter
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.receipts.collect { receipts: List<CycleReceiptItem> ->
                receiptsAdapter.submitList(receipts)
                atualizarEmptyState(receipts.isEmpty())
                updateStatistics(receipts)
            }
        }
        
        lifecycleScope.launch {
            viewModel.isLoading.collect { _ ->
                // TODO: Adicionar progress bar se necessário
            }
        }
        
        lifecycleScope.launch {
            viewModel.errorMessage.collect { mensagem: String? ->
                mensagem?.let {
                    mostrarFeedback("Erro: $it", Snackbar.LENGTH_LONG)
                    viewModel.limparErro()
                }
            }
        }
    }

    private fun updateStatistics(receipts: List<CycleReceiptItem>) {
        val totalRecebido = receipts.sumOf { it.valorRecebido }
        val totalDebito = receipts.sumOf { it.debitoAtual }
        
        binding.tvTotalRecebido.text = currencyFormatter.format(totalRecebido)
        binding.tvTotalDebito.text = currencyFormatter.format(totalDebito)
        binding.tvQuantidadeAcertos.text = "${receipts.size} acertos"
    }

    private fun atualizarEmptyState(mostrar: Boolean) {
        binding.apply {
            if (mostrar) {
                llEmptyState.visibility = View.VISIBLE
                rvReceipts.visibility = View.GONE
            } else {
                llEmptyState.visibility = View.GONE
                rvReceipts.visibility = View.VISIBLE
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
}

/**
 * DTO para item de recebimento
 */
data class CycleReceiptItem(
    val id: Long,
    val clienteNome: String,
    val dataAcerto: String,
    val tipoPagamento: String,
    val valorRecebido: Double,
    val debitoAtual: Double,
    val metodosPagamento: Map<String, Double>
) 
