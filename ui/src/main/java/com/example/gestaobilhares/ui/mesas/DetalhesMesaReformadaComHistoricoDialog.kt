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
import com.example.gestaobilhares.ui.R
import com.example.gestaobilhares.data.entities.MesaReformada
import com.example.gestaobilhares.data.entities.HistoricoManutencaoMesa
import com.example.gestaobilhares.data.entities.TipoManutencao
import com.example.gestaobilhares.ui.databinding.DialogDetalhesMesaReformadaComHistoricoBinding
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

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    private val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))

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
        
        // Lista de reformas
        setupListaReformas()
        
        // Histórico de manutenções
        setupHistoricoManutencoes()
    }

    private fun setupListaReformas() {
        if (mesaComHistorico.reformas.isEmpty()) {
            binding.cardReformas.visibility = View.GONE
            return
        }
        
        binding.cardReformas.visibility = View.VISIBLE
        
        val reformasText = buildString {
            mesaComHistorico.reformas.sortedByDescending { it.dataReforma.time }.forEachIndexed { index, reforma ->
                if (index > 0) append("\n\n")
                append("📅 ${dateFormat.format(reforma.dataReforma)}\n")
                
                val itens = mutableListOf<String>()
                if (reforma.pintura) itens.add("Pintura")
                if (reforma.tabela) itens.add("Tabela")
                if (reforma.panos) {
                    val panosText = if (reforma.numeroPanos != null) {
                        "Panos (${reforma.numeroPanos})"
                    } else {
                        "Panos"
                    }
                    itens.add(panosText)
                }
                if (reforma.outros) itens.add("Outros")
                
                append("🔨 ${itens.joinToString(", ")}")
                
                if (!reforma.observacoes.isNullOrBlank()) {
                    append("\n📝 ${reforma.observacoes}")
                }
                
                if (!reforma.fotoReforma.isNullOrBlank()) {
                    append("\n📷 Foto disponível")
                }
            }
        }
        
        binding.tvListaReformas.text = reformasText
    }

    private fun setupHistoricoManutencoes() {
        if (mesaComHistorico.historicoManutencoes.isEmpty()) {
            binding.cardHistoricoManutencoes.visibility = View.GONE
            return
        }
        
        binding.cardHistoricoManutencoes.visibility = View.VISIBLE
        
        val historicoText = buildString {
            mesaComHistorico.historicoManutencoes.sortedByDescending { it.dataManutencao.time }.forEachIndexed { index, manutencao ->
                if (index > 0) append("\n\n")
                append("📅 ${dateFormat.format(manutencao.dataManutencao)}\n")
                append("🔧 ${formatarTipoManutencao(manutencao.tipoManutencao)}\n")
                append("📝 ${manutencao.descricao}")
                
                if (!manutencao.observacoes.isNullOrBlank()) {
                    append("\n💬 ${manutencao.observacoes}")
                }
                
                if (!manutencao.responsavel.isNullOrBlank()) {
                    append("\n👤 Responsável: ${manutencao.responsavel}")
                }
            }
        }
        
        binding.tvHistoricoManutencoes.text = historicoText
    }

    private fun formatarTipoManutencao(tipo: TipoManutencao): String {
        return when (tipo) {
            TipoManutencao.PINTURA -> "Pintura"
            TipoManutencao.TROCA_PANO -> "Troca de Pano"
            TipoManutencao.TROCA_TABELA -> "Troca de Tabela"
            TipoManutencao.REPARO_ESTRUTURAL -> "Reparo Estrutural"
            TipoManutencao.LIMPEZA -> "Limpeza"
            TipoManutencao.OUTROS -> "Outros"
        }
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

