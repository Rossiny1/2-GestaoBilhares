package com.example.gestaobilhares.ui.cycles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.gestaobilhares.databinding.FragmentCycleSummaryBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/**
 * Fragment para exibir resumo do ciclo (modalidade de pagamento e fechamento)
 */
class CycleSummaryFragment : Fragment() {

    private var _binding: FragmentCycleSummaryBinding? = null
    private val binding get() = _binding!!
    
    private var cicloId: Long = 0L
    private var rotaId: Long = 0L
    private var isCicloFinalizado: Boolean = false
    
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    companion object {
        fun newInstance(cicloId: Long, rotaId: Long, isCicloFinalizado: Boolean): CycleSummaryFragment {
            return CycleSummaryFragment().apply {
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
        _binding = FragmentCycleSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupObservers()
        loadSummaryData()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            // TODO: Implementar observação de estatísticas do parent fragment
            // Observar estatísticas por modalidade do parent fragment
            // val parentFragment = parentFragment as? CycleManagementFragment
            // parentFragment?.viewModel?.estatisticasModalidade?.collect { stats ->
            //     updatePaymentMethodStats(stats)
            // }
        }
        
        lifecycleScope.launch {
            // TODO: Implementar observação de estatísticas do parent fragment
            // Observar estatísticas financeiras do parent fragment
            // val parentFragment = parentFragment as? CycleManagementFragment
            // parentFragment?.viewModel?.estatisticas?.collect { stats ->
            //     updateFinancialStats(stats)
            // }
        }
    }

    private fun loadSummaryData() {
        // Os dados serão carregados automaticamente pelos observers
        // que observam os StateFlows do parent fragment
    }

    private fun updatePaymentMethodStats(stats: PaymentMethodStats) {
        binding.apply {
            tvPix.text = currencyFormatter.format(stats.pix)
            tvCartao.text = currencyFormatter.format(stats.cartao)
            tvCheque.text = currencyFormatter.format(stats.cheque)
            tvDinheiro.text = currencyFormatter.format(stats.dinheiro)
            tvTotalRecebidoModalidade.text = currencyFormatter.format(stats.totalRecebido)
        }
    }

    private fun updateFinancialStats(stats: CycleFinancialStats) {
        binding.apply {
            tvTotalRecebido.text = currencyFormatter.format(stats.totalRecebido)
            tvDespesasViagem.text = currencyFormatter.format(stats.despesasViagem)
            tvSubtotal.text = currencyFormatter.format(stats.subtotal)
            tvComissaoMotorista.text = currencyFormatter.format(stats.comissaoMotorista)
            tvComissaoIltair.text = currencyFormatter.format(stats.comissaoIltair)
            tvSomaPix.text = currencyFormatter.format(stats.somaPix)
            tvSomaDespesas.text = currencyFormatter.format(stats.somaDespesas)
            tvCheques.text = currencyFormatter.format(stats.cheques)
            tvTotalGeral.text = currencyFormatter.format(stats.totalGeral)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 