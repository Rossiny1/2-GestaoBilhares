package com.example.gestaobilhares.ui.settlement

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.gestaobilhares.databinding.FragmentSettlementDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.*

/**
 * Fragment para exibir detalhes completos de um acerto
 * FASE 4B+ - Detalhes e edição de acertos
 */
@AndroidEntryPoint
class SettlementDetailFragment : Fragment() {

    private var _binding: FragmentSettlementDetailBinding? = null
    private val binding get() = _binding!!

    private val args: SettlementDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettlementDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        loadSettlementDetails()
    }

    private fun setupUI() {
        // Botão voltar
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        
        // Botão editar
        binding.btnEdit.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("✏️ Editar Acerto")
                .setMessage("Funcionalidade de edição será implementada na próxima fase!\n\n🚀 Em breve você poderá:\n• Editar valores e fichas\n• Alterar status de pagamento\n• Adicionar observações")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun loadSettlementDetails() {
        // Simular carregamento de dados (TODO: Implementar com ViewModel real)
        val settlementId = args.acertoId
        
        // Dados mockados para demonstração
        val mockData = generateMockSettlementDetail(settlementId)
        updateUI(mockData)
    }

    private fun generateMockSettlementDetail(id: Long): SettlementDetail {
        return SettlementDetail(
            id = id,
            date = "15/12/2024 14:30",
            status = "Pago",
            initialChips = 1250,
            finalChips = 1890,
            chipValue = 0.50,
            observations = "Cliente pagou em dinheiro. Mesa estava funcionando perfeitamente durante todo o período."
        )
    }

    private fun updateUI(settlement: SettlementDetail) {
        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val playedChips = settlement.finalChips - settlement.initialChips
        val totalValue = playedChips * settlement.chipValue

        binding.apply {
            // Informações básicas
            tvSettlementId.text = "#${settlement.id.toString().padStart(4, '0')}"
            tvSettlementDate.text = settlement.date
            
            // Status com cor
            tvSettlementStatus.text = settlement.status.uppercase()
            val statusColor = when (settlement.status.lowercase()) {
                "pago" -> android.R.color.holo_green_dark
                "pendente" -> android.R.color.holo_orange_dark
                "atrasado" -> android.R.color.holo_red_dark
                else -> android.R.color.darker_gray
            }
            tvSettlementStatus.setTextColor(
                ContextCompat.getColor(requireContext(), statusColor)
            )
            
            // Valores
            tvInitialChips.text = NumberFormat.getNumberInstance(Locale("pt", "BR"))
                .format(settlement.initialChips)
            tvFinalChips.text = NumberFormat.getNumberInstance(Locale("pt", "BR"))
                .format(settlement.finalChips)
            tvPlayedChips.text = NumberFormat.getNumberInstance(Locale("pt", "BR"))
                .format(playedChips)
            tvChipValue.text = formatter.format(settlement.chipValue)
            tvTotalValue.text = formatter.format(totalValue)
            
            // Observações
            tvObservations.text = settlement.observations.ifEmpty { "Nenhuma observação registrada." }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Data class para representar os detalhes do acerto
    data class SettlementDetail(
        val id: Long,
        val date: String,
        val status: String,
        val initialChips: Int,
        val finalChips: Int,
        val chipValue: Double,
        val observations: String
    )
} 