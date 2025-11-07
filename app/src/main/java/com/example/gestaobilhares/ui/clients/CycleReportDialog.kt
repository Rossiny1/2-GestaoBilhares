package com.example.gestaobilhares.ui.clients

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AlertDialog
import com.example.gestaobilhares.R
import com.example.gestaobilhares.data.entities.CicloAcertoEntity
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.databinding.DialogCycleReportBinding
import com.example.gestaobilhares.utils.PdfReportGenerator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

/**
 * Diálogo para confirmar geração de relatório PDF do ciclo
 * ✅ FASE 9C: RELATÓRIOS DETALHADOS EM PDF
 */
class CycleReportDialog : DialogFragment() {

    private var _binding: DialogCycleReportBinding? = null
    private val binding get() = _binding!!

    private var ciclo: CicloAcertoEntity? = null
    private var rota: Rota? = null
    private var acertos: List<com.example.gestaobilhares.data.entities.Acerto> = emptyList()
    private var despesas: List<com.example.gestaobilhares.data.entities.Despesa> = emptyList()
    private var clientes: List<com.example.gestaobilhares.data.entities.Cliente> = emptyList()

    companion object {
        fun newInstance(
            ciclo: CicloAcertoEntity,
            rota: Rota,
            acertos: List<com.example.gestaobilhares.data.entities.Acerto>,
            despesas: List<com.example.gestaobilhares.data.entities.Despesa>,
            clientes: List<com.example.gestaobilhares.data.entities.Cliente>
        ): CycleReportDialog {
            return CycleReportDialog().apply {
                this.ciclo = ciclo
                this.rota = rota
                this.acertos = acertos
                this.despesas = despesas
                this.clientes = clientes
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCycleReportBinding.inflate(layoutInflater)
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
            
        // ✅ CORREÇÃO: Configurar largura do diálogo
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(), // 90% da largura da tela
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
            
        // Configurar UI
        setupUI()
        setupClickListeners()
        
        return dialog
    }

    private fun setupUI() {
        ciclo?.let { ciclo ->
            binding.apply {
                tvDialogTitle.text = "Gerar Relatório Detalhado"
                tvDialogMessage.text = "Deseja gerar um relatório PDF detalhado do fechamento da rota?\n\n" +
                        "O relatório incluirá:\n" +
                        "• Cabeçalho com logo da empresa\n" +
                        "• Informações da rota e ciclo\n" +
                        "• Lista completa de recebimentos\n" +
                        "• Resumo financeiro\n" +
                        "• Despesas por categoria\n" +
                        "• Resumo final do fechamento"
                
                // Informações do ciclo
                tvCycleInfo.text = "Ciclo ${ciclo.ano} - #${ciclo.numeroCiclo}"
                tvRouteInfo.text = rota?.nome ?: "Rota não encontrada"
                tvDateInfo.text = "Início: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("pt", "BR")).format(ciclo.dataInicio)}"
            }
        } ?: run {
            // Se não há dados do ciclo, mostrar erro
            binding.apply {
                tvDialogTitle.text = "Erro"
                tvDialogMessage.text = "Não foi possível carregar os dados do ciclo."
                tvCycleInfo.text = "Dados não disponíveis"
                tvRouteInfo.text = "Dados não disponíveis"
                tvDateInfo.text = "Dados não disponíveis"
                btnGenerate.isEnabled = false
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        
        binding.btnGenerate.setOnClickListener {
            generateReport()
        }
    }

    private fun generateReport() {
        val ciclo = this.ciclo
        val rota = this.rota
        
        if (ciclo == null || rota == null) {
            android.widget.Toast.makeText(requireContext(), "Dados do ciclo não encontrados", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        // Mostrar loading
        binding.progressIndicator.visibility = View.VISIBLE
        binding.btnGenerate.isEnabled = false
        binding.btnCancel.isEnabled = false

        // Executar em background
        Thread {
            try {
                val pdfGenerator = PdfReportGenerator(requireContext())
                val pdfFile = pdfGenerator.generateCycleReport(
                    ciclo = ciclo,
                    rota = rota,
                    acertos = acertos,
                    despesas = despesas,
                    clientes = clientes
                )

                // Voltar para UI thread
                requireActivity().runOnUiThread {
                    binding.progressIndicator.visibility = View.GONE
                    binding.btnGenerate.isEnabled = true
                    binding.btnCancel.isEnabled = true
                    
                    // Mostrar sucesso e opções
                    showSuccessDialog(pdfFile)
                }

            } catch (e: Exception) {
                android.util.Log.e("CycleReportDialog", "Erro ao gerar relatório", e)
                
                requireActivity().runOnUiThread {
                    binding.progressIndicator.visibility = View.GONE
                    binding.btnGenerate.isEnabled = true
                    binding.btnCancel.isEnabled = true
                    
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Erro ao gerar relatório: ${e.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun showSuccessDialog(pdfFile: File) {
        val successDialog = AlertDialog.Builder(requireContext())
            .setTitle("✅ Relatório Gerado!")
            .setMessage("O relatório PDF foi gerado com sucesso.\n\nDeseja compartilhar via WhatsApp?")
            .setPositiveButton("Sim, compartilhar") { _, _ ->
                shareViaWhatsApp(pdfFile)
                dismiss()
            }
            .setNegativeButton("Apenas visualizar") { _, _ ->
                openPdfFile(pdfFile)
                dismiss()
            }
            .setNeutralButton("Cancelar") { _, _ ->
                dismiss()
            }
            .setCancelable(false)
            .create()

        successDialog.show()
    }

    private fun shareViaWhatsApp(pdfFile: File) {
        try {
            // Criar URI do arquivo usando FileProvider
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                pdfFile
            )

            // Intent para WhatsApp
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Relatório de Fechamento - ${rota?.nome}")
                putExtra(Intent.EXTRA_TEXT, "Relatório detalhado do fechamento da rota ${rota?.nome} - Ciclo ${ciclo?.ano} #${ciclo?.numeroCiclo}")
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
            } else {
                // Fallback para qualquer app de compartilhamento
                val shareIntent = Intent.createChooser(intent, "Compartilhar relatório via")
                startActivity(shareIntent)
            }

        } catch (e: Exception) {
            android.util.Log.e("CycleReportDialog", "Erro ao compartilhar via WhatsApp", e)
            android.widget.Toast.makeText(
                requireContext(),
                "Erro ao compartilhar: ${e.message}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun openPdfFile(pdfFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                pdfFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
            } else {
                android.widget.Toast.makeText(
                    requireContext(),
                    "Nenhum aplicativo encontrado para visualizar PDF",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }

        } catch (e: Exception) {
            android.util.Log.e("CycleReportDialog", "Erro ao abrir PDF", e)
            android.widget.Toast.makeText(
                requireContext(),
                "Erro ao abrir PDF: ${e.message}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 