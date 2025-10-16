package com.example.gestaobilhares.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class GerarRelatorioDialogFragment : DialogFragment() {

    interface GerarRelatorioDialogListener {
        fun onGerarRelatorioUltimoAcerto()
        fun onGerarRelatorioAnual()
    }

    private var listener: GerarRelatorioDialogListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Buscar o fragment que implementa o listener na hierarquia
        var fragment = parentFragment
        while (fragment != null) {
            if (fragment is GerarRelatorioDialogListener) {
                listener = fragment
                return
            }
            fragment = fragment.parentFragment
        }
        
        // Se não encontrou, tentar na activity
        if (context is GerarRelatorioDialogListener) {
            listener = context
        } else {
            throw ClassCastException("No fragment or activity implements GerarRelatorioDialogListener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val options = arrayOf("Gerar Relatório do Último Acerto", "Gerar Relatório Anual")
        return AlertDialog.Builder(requireActivity())
            .setTitle("Escolha o Tipo de Relatório")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> listener?.onGerarRelatorioUltimoAcerto()
                    1 -> listener?.onGerarRelatorioAnual()
                }
            }
            .create()
    }

    companion object {
        const val TAG = "GerarRelatorioDialog"

        fun newInstance(): GerarRelatorioDialogFragment {
            return GerarRelatorioDialogFragment()
        }
    }
}
