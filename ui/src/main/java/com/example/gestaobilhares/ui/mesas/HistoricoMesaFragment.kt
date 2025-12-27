package com.example.gestaobilhares.ui.mesas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.core.utils.UserSessionManager
import com.example.gestaobilhares.data.entities.MesaReformada
import com.example.gestaobilhares.data.entities.HistoricoManutencaoMesa
import com.example.gestaobilhares.ui.databinding.FragmentHistoricoMesaBinding
import com.example.gestaobilhares.ui.R
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment que exibe o histórico detalhado de uma mesa.
 * Substitui o antigo diálogo por uma tela dedicada.
 */
@AndroidEntryPoint
class HistoricoMesaFragment : Fragment() {

    private var _binding: FragmentHistoricoMesaBinding? = null
    private val binding get() = _binding!!

    private val args: HistoricoMesaFragmentArgs by navArgs()
    
    private lateinit var adapter: ReformaAgrupadaAdapter
    private lateinit var userSessionManager: UserSessionManager
    
    private val dateFormat = SimpleDateFormat("dd/MM/yy", Locale("pt", "BR"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoricoMesaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        userSessionManager = UserSessionManager.getInstance(requireContext())
        
        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        val mesaComHistorico = args.mesaComHistorico
        
        // 1. Preencher Card de Resumo
        binding.tvSummaryNumeroMesa.text = "Mesa ${mesaComHistorico.numeroMesa}"
        binding.tvSummaryPano.text = mesaComHistorico.numeroUltimoPano
        binding.tvSummaryTamanho.text = mesaComHistorico.tamanhoMesa
        binding.tvSummaryTotalReformas.text = mesaComHistorico.totalReformas.toString()
        binding.tvSummaryTipo.text = mesaComHistorico.tipoMesa
        
        // 2. Configurar Lista de Histórico Agrupado
        setupHistoricoAgrupado(mesaComHistorico)
    }

    private fun setupHistoricoAgrupado(mesaComHistorico: MesaReformadaComHistorico) {
        val nomeUsuarioLogado = userSessionManager.getCurrentUserName()
        
        // Agrupar por data (usando apenas a data, sem hora)
        val mapaPorData = mutableMapOf<String, ReformaAgrupada>()
        
        // Processar reformas
        mesaComHistorico.reformas.forEach { reforma ->
            val dataKey = dateFormat.format(reforma.dataReforma)
            if (!mapaPorData.containsKey(dataKey)) {
                mapaPorData[dataKey] = ReformaAgrupada(
                    dataReforma = reforma.dataReforma,
                    reforma = reforma,
                    manutencoes = emptyList()
                )
            } else {
                val existente = mapaPorData[dataKey]!!
                mapaPorData[dataKey] = existente.copy(reforma = existente.reforma ?: reforma)
            }
        }
        
        // Processar manutenções
        mesaComHistorico.historicoManutencoes.forEach { manutencao ->
            val dataKey = dateFormat.format(manutencao.dataManutencao)
            if (!mapaPorData.containsKey(dataKey)) {
                mapaPorData[dataKey] = ReformaAgrupada(
                    dataReforma = manutencao.dataManutencao,
                    reforma = null,
                    manutencoes = listOf(manutencao)
                )
            } else {
                val existente = mapaPorData[dataKey]!!
                mapaPorData[dataKey] = existente.copy(
                    manutencoes = existente.manutencoes + listOf(manutencao)
                )
            }
        }
        
        // Converter para lista e ordenar (mais recente primeiro)
        val reformasAgrupadas = mapaPorData.values.sortedByDescending { it.dataReforma.time }
        
        binding.rvHistoricoCompleto.layoutManager = LinearLayoutManager(requireContext())
        adapter = ReformaAgrupadaAdapter(reformasAgrupadas, nomeUsuarioLogado)
        binding.rvHistoricoCompleto.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
