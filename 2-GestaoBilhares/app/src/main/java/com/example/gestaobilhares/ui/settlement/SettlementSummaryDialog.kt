package com.example.gestaobilhares.ui.settlement

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.gestaobilhares.R
import com.example.gestaobilhares.data.entities.Mesa
import com.google.android.material.button.MaterialButton

class SettlementSummaryDialog : DialogFragment() {
    companion object {
        fun newInstance(
            clienteNome: String,
            mesas: List<Mesa>,
            total: Double,
            metodosPagamento: Map<String, Double>,
            observacao: String?
        ): SettlementSummaryDialog {
            val args = Bundle().apply {
                putString("clienteNome", clienteNome)
                putParcelableArrayList("mesas", ArrayList(mesas))
                putDouble("total", total)
                putSerializable("metodosPagamento", HashMap(metodosPagamento))
                putString("observacao", observacao)
            }
            val fragment = SettlementSummaryDialog()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_settlement_summary, null)
        val clienteNome = arguments?.getString("clienteNome") ?: ""
        val mesas = arguments?.getParcelableArrayList<Mesa>("mesas") ?: emptyList<Mesa>()
        val total = arguments?.getDouble("total") ?: 0.0
        val metodosPagamento = arguments?.getSerializable("metodosPagamento") as? HashMap<String, Double> ?: hashMapOf()
        val observacao = arguments?.getString("observacao") ?: ""

        view.findViewById<TextView>(R.id.tvResumoCliente).text = clienteNome
        view.findViewById<TextView>(R.id.tvResumoMesas).text = mesas.joinToString("\n") { "Mesa: ${it.numero}" }
        view.findViewById<TextView>(R.id.tvResumoTotal).text = "Total: R$ %.2f".format(total)
        view.findViewById<TextView>(R.id.tvResumoPagamentos).text = metodosPagamento.entries.joinToString("\n") { "${it.key}: R$ %.2f".format(it.value) }
        view.findViewById<TextView>(R.id.tvResumoObservacao).text = observacao

        view.findViewById<MaterialButton>(R.id.btnImprimir).setOnClickListener {
            // TODO: Implementar impressão
            dismiss()
        }
        view.findViewById<MaterialButton>(R.id.btnWhatsapp).setOnClickListener {
            val texto = "Acerto de ${clienteNome}\nTotal: R$ %.2f".format(total)
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, texto)
                type = "text/plain"
                setPackage("com.whatsapp")
            }
            startActivity(sendIntent)
            dismiss()
        }
        return android.app.AlertDialog.Builder(requireContext())
            .setView(view)
            .setCancelable(true)
            .create()
    }
} 