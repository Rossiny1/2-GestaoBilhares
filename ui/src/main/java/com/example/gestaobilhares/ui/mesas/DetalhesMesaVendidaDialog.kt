package com.example.gestaobilhares.ui.mesas

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.gestaobilhares.data.entities.MesaVendida
import com.example.gestaobilhares.ui.databinding.DialogDetalhesMesaVendidaBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog para exibir detalhes de uma mesa vendida
 * ✅ NOVO: SISTEMA DE VENDA DE MESAS
 */
class DetalhesMesaVendidaDialog : DialogFragment() {

    private var _binding: DialogDetalhesMesaVendidaBinding? = null
    private val binding get() = _binding!!

    private var mesaVendida: MesaVendida? = null

    companion object {
        fun newInstance(mesaVendida: MesaVendida): DetalhesMesaVendidaDialog {
            val dialog = DetalhesMesaVendidaDialog()
            dialog.mesaVendida = mesaVendida
            return dialog
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogDetalhesMesaVendidaBinding.inflate(layoutInflater)
        return Dialog(requireContext()).apply {
            setContentView(binding.root)
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupClickListeners()
        carregarDados()
    }

    private fun setupUI() {
        // Configurar título
        binding.tvTitulo.text = "Detalhes da Mesa Vendida"
    }

    private fun setupClickListeners() {
        binding.btnFechar.setOnClickListener {
            dismiss()
        }
    }

    private fun carregarDados() {
        val mesa = mesaVendida ?: return
        
        // Dados da mesa
        binding.tvNumeroMesa.text = mesa.numeroMesa
        binding.tvTipoMesa.text = getTipoMesaNome(mesa.tipoMesa)
        binding.tvTamanhoMesa.text = getTamanhoMesaNome(mesa.tamanhoMesa)
        binding.tvEstadoConservacao.text = getEstadoConservacaoNome(mesa.estadoConservacao)
        
        // Dados do comprador
        binding.tvNomeComprador.text = mesa.nomeComprador
        binding.tvTelefoneComprador.text = mesa.telefoneComprador ?: "Não informado"
        binding.tvCpfCnpjComprador.text = mesa.cpfCnpjComprador ?: "Não informado"
        binding.tvEnderecoComprador.text = mesa.enderecoComprador ?: "Não informado"
        
        // Dados da venda
        binding.tvValorVenda.text = "R$ ${String.format("%.2f", mesa.valorVenda)}"
        binding.tvDataVenda.text = formatarData(mesa.dataVenda)
        binding.tvObservacoes.text = mesa.observacoes ?: "Nenhuma observação"
        
        // Dados do registro
        binding.tvDataRegistro.text = formatarData(mesa.dataCriacao)
    }

    private fun getTipoMesaNome(tipoMesa: com.example.gestaobilhares.data.entities.TipoMesa): String {
        return when (tipoMesa) {
            com.example.gestaobilhares.data.entities.TipoMesa.SINUCA -> "Sinuca"
            com.example.gestaobilhares.data.entities.TipoMesa.PEMBOLIM -> "Pembolim"
            com.example.gestaobilhares.data.entities.TipoMesa.JUKEBOX -> "Jukebox"
            com.example.gestaobilhares.data.entities.TipoMesa.OUTROS -> "Outros"
        }
    }

    private fun getTamanhoMesaNome(tamanhoMesa: com.example.gestaobilhares.data.entities.TamanhoMesa): String {
        return when (tamanhoMesa) {
            com.example.gestaobilhares.data.entities.TamanhoMesa.PEQUENA -> "Pequena"
            com.example.gestaobilhares.data.entities.TamanhoMesa.MEDIA -> "Média"
            com.example.gestaobilhares.data.entities.TamanhoMesa.GRANDE -> "Grande"
        }
    }

    private fun getEstadoConservacaoNome(estadoConservacao: com.example.gestaobilhares.data.entities.EstadoConservacao): String {
        return when (estadoConservacao) {
            com.example.gestaobilhares.data.entities.EstadoConservacao.OTIMO -> "Ótimo"
            com.example.gestaobilhares.data.entities.EstadoConservacao.BOM -> "Bom"
            com.example.gestaobilhares.data.entities.EstadoConservacao.RUIM -> "Ruim"
        }
    }

    private fun formatarData(data: Date): String {
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
        return formatter.format(data)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

