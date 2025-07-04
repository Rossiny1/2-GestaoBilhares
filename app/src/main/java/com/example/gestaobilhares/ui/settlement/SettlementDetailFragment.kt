package com.example.gestaobilhares.ui.settlement

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.gestaobilhares.databinding.FragmentSettlementDetailBinding
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.repository.AcertoRepository
import com.example.gestaobilhares.data.repository.AcertoMesaRepository
import java.text.NumberFormat
import java.util.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager

/**
 * Fragment para exibir detalhes de um acerto específico.
 * FASE 4B+ - Detalhes e edição de acertos
 */
class SettlementDetailFragment : Fragment() {

    private var _binding: FragmentSettlementDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SettlementDetailViewModel
    private val args: SettlementDetailFragmentArgs by navArgs()

    private var mesaDetailAdapter: AcertoMesaDetailAdapter? = null

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
        
        // Inicializar ViewModel aqui onde o contexto está disponível
        viewModel = SettlementDetailViewModel(
            AcertoRepository(AppDatabase.getDatabase(requireContext()).acertoDao()),
            AcertoMesaRepository(AppDatabase.getDatabase(requireContext()).acertoMesaDao())
        )
        
        setupUI()
        observeViewModel()
        
        // Carregar detalhes do acerto
        viewModel.loadSettlementDetails(args.acertoId)
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

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.settlementDetails.collect { settlement ->
                settlement?.let {
                    Log.d("SettlementDetailFragment", "Detalhes carregados: $it")
                    updateUI(it)
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                // TODO: Mostrar loading se necessário
                if (isLoading) {
                    Log.d("SettlementDetailFragment", "Carregando detalhes...")
                }
            }
        }
    }

    private fun updateUI(settlement: SettlementDetailViewModel.SettlementDetail) {
        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

        binding.apply {
            // Informações básicas
            tvSettlementId.text = "#${settlement.id.toString().padStart(4, '0')}"
            tvSettlementDate.text = settlement.date
            
            // Status com cor
            tvSettlementStatus.text = settlement.status.uppercase()
            val statusColor = when (settlement.status.lowercase()) {
                "finalizado" -> android.R.color.holo_green_dark
                "pendente" -> android.R.color.holo_orange_dark
                "atrasado" -> android.R.color.holo_red_dark
                else -> android.R.color.darker_gray
            }
            tvSettlementStatus.setTextColor(
                ContextCompat.getColor(requireContext(), statusColor)
            )
            
            // Valores financeiros corretos
            tvInitialChips.text = formatter.format(settlement.debitoAnterior) // Débito anterior
            tvFinalChips.text = formatter.format(settlement.valorTotal) // Valor total do acerto
            tvPlayedChips.text = formatter.format(settlement.valorRecebido) // Valor recebido
            tvChipValue.text = formatter.format(settlement.desconto) // Desconto aplicado
            tvTotalValue.text = formatter.format(settlement.debitoAtual) // Débito atual após acerto
            
            // Observações
            tvObservations.text = settlement.observacoes

            // Configurar RecyclerView das mesas do acerto
            mesaDetailAdapter = AcertoMesaDetailAdapter(settlement.acertoMesas)
            rvMesasDetalhe.adapter = mesaDetailAdapter
            rvMesasDetalhe.layoutManager = LinearLayoutManager(requireContext())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 