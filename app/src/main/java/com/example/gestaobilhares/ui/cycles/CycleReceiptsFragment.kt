package com.example.gestaobilhares.ui.cycles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.databinding.FragmentCycleReceiptsBinding
import com.example.gestaobilhares.data.entities.Acerto
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.data.entities.StatusCicloAcerto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Fragment para exibir lista de recebimentos (acertos) do ciclo
 * Similar ao que está no relatório PDF
 */
class CycleReceiptsFragment : Fragment() {

    private var _binding: FragmentCycleReceiptsBinding? = null
    private val binding get() = _binding!!
    
    private var cicloId: Long = 0L
    private var rotaId: Long = 0L
    private var isCicloFinalizado: Boolean = false
    
    private lateinit var receiptsAdapter: CycleReceiptsAdapter
    
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
        
        setupRecyclerView()
        loadReceipts()
    }

    private fun setupRecyclerView() {
        receiptsAdapter = CycleReceiptsAdapter(
            isCicloFinalizado = isCicloFinalizado,
            onItemClick = { acerto ->
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

    private fun loadReceipts() {
        lifecycleScope.launch {
            try {
                // Buscar acertos do ciclo
                val parentFragment = parentFragment as? CycleManagementFragment
                val acertos = parentFragment?.viewModel?.buscarAcertosPorCiclo(cicloId) ?: emptyList()
                val clientes = parentFragment?.viewModel?.buscarClientesPorRota(rotaId) ?: emptyList()
                
                // Mapear para DTOs
                val receiptsItems = acertos.map { acerto ->
                    val cliente = clientes.find { it.id == acerto.clienteId }
                    val metodosPagamento = processarMetodosPagamento(acerto.metodosPagamentoJson)
                    val tipoPagamentoTexto = formatarTiposPagamento(metodosPagamento)
                    
                    CycleReceiptItem(
                        id = acerto.id,
                        clienteNome = cliente?.nome ?: "Cliente #${acerto.clienteId}",
                        dataAcerto = dateFormatter.format(acerto.dataAcerto),
                        tipoPagamento = tipoPagamentoTexto,
                        valorRecebido = acerto.valorRecebido,
                        debitoAtual = acerto.debitoAtual,
                        metodosPagamento = metodosPagamento
                    )
                }
                
                receiptsAdapter.submitList(receiptsItems)
                
                // Atualizar estatísticas
                updateStatistics(acertos)
                
            } catch (e: Exception) {
                android.util.Log.e("CycleReceiptsFragment", "Erro ao carregar recebimentos: ${e.message}")
            }
        }
    }

    private fun updateStatistics(acertos: List<Acerto>) {
        val totalRecebido = acertos.sumOf { it.valorRecebido }
        val totalDebito = acertos.sumOf { it.debitoAtual }
        
        binding.tvTotalRecebido.text = currencyFormatter.format(totalRecebido)
        binding.tvTotalDebito.text = currencyFormatter.format(totalDebito)
        binding.tvQuantidadeAcertos.text = "${acertos.size} acertos"
    }

    /**
     * Processa JSON dos métodos de pagamento
     */
    private fun processarMetodosPagamento(metodosPagamentoJson: String?): Map<String, Double> {
        return try {
            if (metodosPagamentoJson.isNullOrBlank()) {
                mapOf("Dinheiro" to 0.0)
            } else {
                val tipo = object : TypeToken<Map<String, Double>>() {}.type
                Gson().fromJson(metodosPagamentoJson, tipo) ?: mapOf("Dinheiro" to 0.0)
            }
        } catch (e: Exception) {
            android.util.Log.e("CycleReceiptsFragment", "Erro ao processar métodos de pagamento: ${e.message}")
            mapOf("Dinheiro" to 0.0)
        }
    }

    /**
     * Formata tipos de pagamento para exibição
     */
    private fun formatarTiposPagamento(metodosPagamento: Map<String, Double>): String {
        return if (metodosPagamento.size == 1) {
            metodosPagamento.keys.first()
        } else {
            metodosPagamento.entries
                .filter { it.value > 0 }
                .joinToString(", ") { "${it.key}: ${currencyFormatter.format(it.value)}" }
                .ifEmpty { "Não informado" }
        }
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