package com.example.gestaobilhares.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.gestaobilhares.R

class ConfirmarRetiradaMesaDialogFragment : DialogFragment() {

    interface ConfirmarRetiradaDialogListener {
        fun onDialogPositiveClick(dialog: DialogFragment)
    }

    private var listener: ConfirmarRetiradaDialogListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Buscar o fragment que implementa o listener na hierarquia
        var fragment = parentFragment
        while (fragment != null) {
            if (fragment is ConfirmarRetiradaDialogListener) {
                listener = fragment
                return
            }
            fragment = fragment.parentFragment
        }
        
        // Se não encontrou, tentar na activity
        if (context is ConfirmarRetiradaDialogListener) {
            listener = context
        } else {
            throw ClassCastException("No fragment or activity implements ConfirmarRetiradaDialogListener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            AlertDialog.Builder(it)
                .setTitle("Confirmar Retirada")
                .setMessage("Tem certeza que deseja retirar a mesa deste cliente?")
                .setPositiveButton(R.string.confirmar) { _, _ ->
                    listener?.onDialogPositiveClick(this)
                }
                .setNegativeButton(R.string.cancelar, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    companion object {
        const val TAG = "ConfirmarRetiradaMesaDialog"

        fun newInstance(): ConfirmarRetiradaMesaDialogFragment {
            return ConfirmarRetiradaMesaDialogFragment()
        }
    }
}
