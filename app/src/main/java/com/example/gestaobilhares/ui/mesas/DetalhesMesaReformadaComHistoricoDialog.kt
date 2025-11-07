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
import com.example.gestaobilhares.R
import com.example.gestaobilhares.data.entities.HistoricoManutencaoMesa
import com.example.gestaobilhares.data.entities.TipoManutencao
import com.example.gestaobilhares.databinding.DialogDetalhesMesaReformadaBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * ✅ NOVO: Dialog para exibir os detalhes de uma mesa reformada com histórico completo de manutenções.
 */
class DetalhesMesaReformadaComHistoricoDialog : DialogFragment() {

    private var _binding: DialogDetalhesMesaReformadaBinding? = null
    private val binding get() = _binding!!

    private lateinit var mesaComHistorico: MesaReformadaComHistorico

    companion object {
        private const val ARG_MESA_COM_HISTORICO = "mesa_com_historico"

        fun newInstance(mesaComHistorico: MesaReformadaComHistorico): DetalhesMesaReformadaComHistoricoDialog {
            val args = Bundle()
            // Serializar os dados necessários
            args.putString("numeroMesa", mesaComHistorico.numeroMesa)
            args.putLong("mesaId", mesaComHistorico.mesaId)
            args.putString("tipoMesa", mesaComHistorico.tipoMesa)
            args.putString("tamanhoMesa", mesaComHistorico.tamanhoMesa)
            args.putInt("totalReformas", mesaComHistorico.totalReformas)
            args.putSerializable("ultimaReforma", mesaComHistorico.reformas.firstOrNull())
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
            val numeroMesa = it.getString("numeroMesa") ?: ""
            val mesaId = it.getLong("mesaId")
            val tipoMesa = it.getString("tipoMesa") ?: ""
            val tamanhoMesa = it.getString("tamanhoMesa") ?: ""
            val ultimaReforma = it.getSerializable("ultimaReforma") as? com.example.gestaobilhares.data.entities.MesaReformada
            val historicoManutencoes = (it.getSerializable("historicoManutencoes") as? ArrayList<*>)?.mapNotNull { 
                it as? HistoricoManutencaoMesa 
            } ?: emptyList()
            
            mesaComHistorico = MesaReformadaComHistorico(
                numeroMesa = numeroMesa,
                mesaId = mesaId,
                tipoMesa = tipoMesa,
                tamanhoMesa = tamanhoMesa,
                reformas = if (ultimaReforma != null) listOf(ultimaReforma) else emptyList(),
                historicoManutencoes = historicoManutencoes
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogDetalhesMesaReformadaBinding.inflate(inflater, container, false)
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
        val ultimaReforma = mesaComHistorico.reformas.firstOrNull()
        
        // Número da mesa
        binding.tvNumeroMesa.text = "Mesa ${mesaComHistorico.numeroMesa}"

        // Tipo da mesa
        binding.tvTipoMesa.text = "${mesaComHistorico.tipoMesa} - ${mesaComHistorico.tamanhoMesa}"

        // Data da última reforma
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        if (mesaComHistorico.dataUltimaReforma != null) {
            binding.tvDataReforma.text = dateFormat.format(mesaComHistorico.dataUltimaReforma)
        } else {
            binding.tvDataReforma.text = "N/A"
        }

        // Itens reformados da última reforma
        if (ultimaReforma != null) {
            val itensReformados = buildString {
                val itens = mutableListOf<String>()
                if (ultimaReforma.pintura) itens.add("Pintura")
                if (ultimaReforma.tabela) itens.add("Tabela")
                if (ultimaReforma.panos) {
                    val panosText = if (ultimaReforma.numeroPanos != null) {
                        "Panos (${ultimaReforma.numeroPanos})"
                    } else {
                        "Panos"
                    }
                    itens.add(panosText)
                }
                if (ultimaReforma.outros) itens.add("Outros")

                append(itens.joinToString(", "))
            }
            binding.tvItensReformados.text = itensReformados

            // Observações
            if (!ultimaReforma.observacoes.isNullOrBlank()) {
                binding.tvObservacoes.text = ultimaReforma.observacoes
                binding.cardObservacoes.visibility = View.VISIBLE
            } else {
                binding.cardObservacoes.visibility = View.GONE
            }

            // Foto da mesa
            if (!ultimaReforma.fotoReforma.isNullOrBlank()) {
                carregarFoto(ultimaReforma.fotoReforma!!)
            } else {
                binding.cardFoto.visibility = View.GONE
            }
        }
        
        // ✅ NOVO: Exibir histórico de manutenções
        exibirHistorico()
    }
    
    /**
     * ✅ NOVO: Exibe histórico de manutenções no card
     */
    private fun exibirHistorico() {
        if (mesaComHistorico.historicoManutencoes.isEmpty()) {
            binding.cardHistorico.visibility = View.GONE
            return
        }
        
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val historicoText = buildString {
            mesaComHistorico.historicoManutencoes.forEachIndexed { index, historico ->
                if (index > 0) append("\n\n")
                append("• ${formatarTipoManutencao(historico.tipoManutencao)}")
                append(" - ${dateFormat.format(historico.dataManutencao)}")
                if (!historico.descricao.isNullOrBlank()) {
                    append("\n  ${historico.descricao}")
                }
                if (!historico.responsavel.isNullOrBlank()) {
                    append("\n  Responsável: ${historico.responsavel}")
                }
                if (historico.custo != null && historico.custo > 0) {
                    append("\n  Custo: R$ ${String.format("%.2f", historico.custo)}")
                }
            }
        }
        
        binding.tvHistoricoManutencoes.text = historicoText
        binding.cardHistorico.visibility = View.VISIBLE
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

    private fun carregarFoto(caminhoFoto: String) {
        try {
            Log.d("DetalhesMesaReformadaDialog", "Carregando foto: $caminhoFoto")
            
            val fotoFile = File(caminhoFoto)
            if (fotoFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(fotoFile.absolutePath)
                if (bitmap != null) {
                    binding.ivFotoMesa.setImageBitmap(bitmap)
                    binding.cardFoto.visibility = View.VISIBLE
                    Log.d("DetalhesMesaReformadaDialog", "Foto carregada com sucesso")
                } else {
                    Log.e("DetalhesMesaReformadaDialog", "Erro ao decodificar bitmap")
                    binding.cardFoto.visibility = View.GONE
                }
            } else {
                Log.e("DetalhesMesaReformadaDialog", "Arquivo de foto não existe: $caminhoFoto")
                binding.cardFoto.visibility = View.GONE
            }
        } catch (e: Exception) {
            Log.e("DetalhesMesaReformadaDialog", "Erro ao carregar foto: ${e.message}", e)
            binding.cardFoto.visibility = View.GONE
            Toast.makeText(requireContext(), "Erro ao carregar foto: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

