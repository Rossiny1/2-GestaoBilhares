package com.example.gestaobilhares.ui.reports

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import com.example.gestaobilhares.R
import com.example.gestaobilhares.databinding.DialogClosureReportBinding
import com.example.gestaobilhares.utils.ClosureReportPdfGenerator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import java.io.File

/**
 * Diálogo para seleção e geração de relatórios de fechamento
 */
class ClosureReportDialog : DialogFragment() {

    private var _binding: DialogClosureReportBinding? = null
    private val binding get() = _binding!!
    
    private var anoSelecionado: Int = 0
    private var numeroAcerto: Int = 0
    private var resumo: ClosureReportViewModel.Resumo? = null
    private var detalhes: List<ClosureReportViewModel.LinhaDetalhe>? = null
    private var totalMesas: Int = 0
    private var isAnnualReport: Boolean = false

    companion object {
        fun newInstance(
            ano: Int,
            numeroAcerto: Int,
            resumo: ClosureReportViewModel.Resumo,
            detalhes: List<ClosureReportViewModel.LinhaDetalhe>,
            totalMesas: Int,
            isAnnualReport: Boolean = false
        ): ClosureReportDialog {
            val dialog = ClosureReportDialog()
            val args = Bundle()
            args.putInt("ano", ano)
            args.putInt("numeroAcerto", numeroAcerto)
            args.putSerializable("resumo", resumo)
            args.putSerializable("detalhes", ArrayList(detalhes))
            args.putInt("totalMesas", totalMesas)
            args.putBoolean("isAnnualReport", isAnnualReport)
            dialog.arguments = args
            return dialog
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            anoSelecionado = it.getInt("ano")
            numeroAcerto = it.getInt("numeroAcerto")
            resumo = it.getSerializable("resumo") as? ClosureReportViewModel.Resumo
            detalhes = it.getSerializable("detalhes") as? ArrayList<ClosureReportViewModel.LinhaDetalhe>
            totalMesas = it.getInt("totalMesas")
            isAnnualReport = it.getBoolean("isAnnualReport")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogClosureReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        val title = if (isAnnualReport) {
            "Relatório Anual - $anoSelecionado"
        } else {
            "Relatório do ${numeroAcerto}º Acerto"
        }
        
        binding.tvTitle.text = title
        
        // Mostrar informações do resumo
        resumo?.let { resumo ->
            binding.tvFaturamento.text = "Faturamento: ${formatCurrency(resumo.faturamentoTotal)}"
            binding.tvDespesas.text = "Despesas: ${formatCurrency(resumo.despesasTotal)}"
            binding.tvLucro.text = "Lucro: ${formatCurrency(resumo.lucroLiquido)}"
            binding.tvMesas.text = "Mesas: $totalMesas"
        }
    }

    private fun setupClickListeners() {
        binding.btnGeneratePdf.setOnClickListener {
            generatePdfReport()
        }
        
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun generatePdfReport() {
        if (resumo == null || detalhes == null) {
            Toast.makeText(requireContext(), "Erro: Dados não disponíveis", Toast.LENGTH_SHORT).show()
            return
        }

        // Mostrar loading
        binding.btnGeneratePdf.isEnabled = false
        binding.btnGeneratePdf.text = "Gerando..."
        binding.progressIndicator.visibility = View.VISIBLE

        // Gerar PDF em background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val pdfGenerator = ClosureReportPdfGenerator(requireContext())
                val pdfFile = if (isAnnualReport) {
                    pdfGenerator.generateAnnualClosureReport(
                        anoSelecionado,
                        resumo!!,
                        detalhes!!,
                        totalMesas
                    )
                } else {
                    pdfGenerator.generateAcertoClosureReport(
                        anoSelecionado,
                        numeroAcerto,
                        resumo!!,
                        detalhes!!,
                        totalMesas
                    )
                }

                // Voltar para UI thread para compartilhar
                withContext(Dispatchers.Main) {
                    sharePdfFile(pdfFile)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Erro ao gerar PDF: ${e.message}", Toast.LENGTH_LONG).show()
                    resetButton()
                }
            }
        }
    }

    private fun sharePdfFile(pdfFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Relatório de Fechamento")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Tentar abrir WhatsApp primeiro
            val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
                setPackage("com.whatsapp")
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Relatório de Fechamento")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            try {
                startActivity(whatsappIntent)
            } catch (e: Exception) {
                // Se WhatsApp não estiver disponível, usar intent genérico
                startActivity(Intent.createChooser(shareIntent, "Compartilhar Relatório"))
            }

            dismiss()

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Erro ao compartilhar PDF: ${e.message}", Toast.LENGTH_LONG).show()
            resetButton()
        }
    }

    private fun resetButton() {
        binding.btnGeneratePdf.isEnabled = true
        binding.btnGeneratePdf.text = "Gerar PDF"
        binding.progressIndicator.visibility = View.GONE
    }

    private fun formatCurrency(value: Double): String {
        return java.text.NumberFormat.getCurrencyInstance(java.util.Locale("pt", "BR")).format(value)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
