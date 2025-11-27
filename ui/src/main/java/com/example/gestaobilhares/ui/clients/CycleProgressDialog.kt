package com.example.gestaobilhares.ui.clients

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import com.example.gestaobilhares.ui.R
import com.example.gestaobilhares.ui.databinding.DialogCycleProgressBinding
import com.example.gestaobilhares.ui.clients.CicloProgressoCard
import com.example.gestaobilhares.data.entities.CicloAcertoEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * ✅ NOVO: Diálogo para exibir informações detalhadas do progresso do ciclo
 */
class CycleProgressDialog(
    context: Context,
    private val cicloProgressoCard: CicloProgressoCard,
    private val cicloAtivo: CicloAcertoEntity?,
    private val rotaNome: String
) : Dialog(context) {

    private lateinit var binding: DialogCycleProgressBinding
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configurar o diálogo
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogCycleProgressBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        
        // Configurar tamanho da janela
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        
        // Configurar margens
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        setupViews()
        populateData()
        setupListeners()
    }

    private fun setupViews() {
        // Configurar animação de entrada
        // window?.attributes?.windowAnimations = com.example.gestaobilhares.ui.R.style.DialogAnimation
    }

    private fun populateData() {
        // Informações do ciclo
        val cicloInfo = if (cicloAtivo != null) {
            "${cicloAtivo.numeroCiclo}º Acerto - $rotaNome"
        } else {
            "Ciclo não iniciado - $rotaNome"
        }
        binding.tvCycleInfo.text = cicloInfo

        // Período do ciclo
        val periodo = if (cicloAtivo != null) {
            if (true) { // cicloAtivo.dataFim is always non-null
                "${dateFormatter.format(cicloAtivo.dataInicio)} - ${dateFormatter.format(cicloAtivo.dataFim)}"
            } else {
                "Iniciado em ${dateFormatter.format(cicloAtivo.dataInicio)}"
            }
        } else {
            "Período não definido"
        }
        binding.tvCyclePeriod.text = periodo

        // Dados financeiros
        binding.tvDialogFaturamento.text = currencyFormatter.format(cicloProgressoCard.receita)
        binding.tvDialogDespesas.text = currencyFormatter.format(cicloProgressoCard.despesas)
        
        // Percentual e clientes
        binding.tvDialogPercentual.text = "${cicloProgressoCard.percentual}%"
        binding.tvDialogTotalClientes.text = "de ${cicloProgressoCard.totalClientes} clientes"
        
        // Pendências
        binding.tvDialogPendencias.text = cicloProgressoCard.pendencias.toString()
        
        // Débito total
        binding.tvDialogDebitoTotal.text = currencyFormatter.format(cicloProgressoCard.debitoTotal)
    }

    private fun setupListeners() {
        binding.btnCloseDialog.setOnClickListener {
            dismiss()
        }
    }
}

