package com.example.gestaobilhares.ui.mesas

import android.app.Dialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.ui.R
import com.example.gestaobilhares.data.entities.MesaReformada
import com.example.gestaobilhares.data.entities.HistoricoManutencaoMesa
import com.example.gestaobilhares.data.entities.TipoManutencao
import com.example.gestaobilhares.ui.databinding.DialogDetalhesMesaReformadaComHistoricoBinding
import com.example.gestaobilhares.core.utils.UserSessionManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog para exibir os detalhes completos de uma mesa reformada, incluindo:
 * - Todas as reformas realizadas
 * - Histórico completo de manutenções
 * - Fotos das reformas
 */
class DetalhesMesaReformadaComHistoricoDialog : DialogFragment() {

    private var _binding: DialogDetalhesMesaReformadaComHistoricoBinding? = null
    private val binding get() = _binding!!

    private lateinit var mesaComHistorico: MesaReformadaComHistorico
    private lateinit var userSessionManager: UserSessionManager
    private lateinit var adapter: ReformaAgrupadaAdapter

    private val dateFormat = SimpleDateFormat("dd-MM-yy", Locale("pt", "BR"))
    private val dateTimeFormat = SimpleDateFormat("dd-MM-yy HH:mm", Locale("pt", "BR"))

    companion object {
        fun newInstance(mesaComHistorico: MesaReformadaComHistorico): DetalhesMesaReformadaComHistoricoDialog {
            val args = Bundle()
            // Passar os dados necessários (enums já são strings no MesaReformadaComHistorico)
            args.putString("numeroMesa", mesaComHistorico.numeroMesa)
            args.putLong("mesaId", mesaComHistorico.mesaId)
            args.putString("tipoMesa", mesaComHistorico.tipoMesa)
            args.putString("tamanhoMesa", mesaComHistorico.tamanhoMesa)
            // MesaReformada e HistoricoManutencaoMesa são Serializable
            args.putSerializable("reformas", ArrayList(mesaComHistorico.reformas))
            args.putSerializable("historicoManutencoes", ArrayList(mesaComHistorico.historicoManutencoes))
            
            val fragment = DetalhesMesaReformadaComHistoricoDialog()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog)
        
        arguments?.let {
            val numeroMesa = it.getString("numeroMesa", "")
            val mesaId = it.getLong("mesaId", 0L)
            val tipoMesa = it.getString("tipoMesa", "")
            val tamanhoMesa = it.getString("tamanhoMesa", "")
            @Suppress("UNCHECKED_CAST")
            val reformas = (it.getSerializable("reformas") as? ArrayList<MesaReformada>) ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val historicoManutencoes = (it.getSerializable("historicoManutencoes") as? ArrayList<HistoricoManutencaoMesa>) ?: emptyList()
            
            mesaComHistorico = MesaReformadaComHistorico(
                numeroMesa = numeroMesa,
                mesaId = mesaId,
                tipoMesa = tipoMesa,
                tamanhoMesa = tamanhoMesa,
                reformas = reformas,
                historicoManutencoes = historicoManutencoes
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogDetalhesMesaReformadaComHistoricoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Inicializar UserSessionManager usando singleton
        userSessionManager = UserSessionManager.getInstance(requireContext())
        
        setupUI()
        setupClickListeners()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        return dialog
    }

    private fun setupUI() {
        // Informações básicas da mesa
        binding.tvNumeroMesa.text = "Mesa ${mesaComHistorico.numeroMesa}"
        binding.tvTipoMesa.text = "${mesaComHistorico.tipoMesa} - ${mesaComHistorico.tamanhoMesa}"
        
        // Total de reformas
        binding.tvTotalReformas.text = "${mesaComHistorico.totalReformas} reforma(s) realizada(s)"
        
        // Agrupar reformas e manutenções por data
        setupReformasAgrupadas()
    }

    /**
     * Agrupa reformas e manutenções por data e exibe em cards separados
     */
    private fun setupReformasAgrupadas() {
        // Obter nome do usuário logado
        val nomeUsuarioLogado = userSessionManager.getCurrentUserName()
        
        // Agrupar por data (usando apenas a data, sem hora)
        val reformasAgrupadas = mutableListOf<ReformaAgrupada>()
        
        // Criar um mapa de datas para agrupar
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
                // Se já existe uma reforma nesta data, manter a primeira e adicionar manutenções
                val existente = mapaPorData[dataKey]!!
                mapaPorData[dataKey] = existente.copy(reforma = existente.reforma ?: reforma)
            }
        }
        
        // Processar manutenções e agrupar por data
        mesaComHistorico.historicoManutencoes.forEach { manutencao ->
            val dataKey = dateFormat.format(manutencao.dataManutencao)
            if (!mapaPorData.containsKey(dataKey)) {
                // Criar nova entrada apenas com manutenção
                mapaPorData[dataKey] = ReformaAgrupada(
                    dataReforma = manutencao.dataManutencao,
                    reforma = null,
                    manutencoes = listOf(manutencao)
                )
            } else {
                // Adicionar manutenção à entrada existente
                val existente = mapaPorData[dataKey]!!
                mapaPorData[dataKey] = existente.copy(
                    manutencoes = existente.manutencoes + listOf(manutencao)
                )
            }
        }
        
        // Converter para lista e ordenar por data (mais recente primeiro)
        reformasAgrupadas.addAll(mapaPorData.values.sortedByDescending { it.dataReforma.time })
        
        // Configurar RecyclerView
        if (reformasAgrupadas.isEmpty()) {
            binding.rvReformasAgrupadas.visibility = View.GONE
            return
        }
        
        binding.rvReformasAgrupadas.visibility = View.VISIBLE
        binding.rvReformasAgrupadas.layoutManager = LinearLayoutManager(requireContext())
        adapter = ReformaAgrupadaAdapter(reformasAgrupadas, nomeUsuarioLogado)
        binding.rvReformasAgrupadas.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnFechar.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

