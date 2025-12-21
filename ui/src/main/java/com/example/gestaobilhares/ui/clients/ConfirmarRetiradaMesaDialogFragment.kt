package com.example.gestaobilhares.ui.clients

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * DialogFragment para confirmar a retirada de uma mesa do cliente.
 * 
 * Este dialog é exibido quando o usuário clica no ícone da lixeira na tela de detalhes do cliente.
 * A retirada só é permitida se a mesa foi acertada na data atual.
 * 
 * Após a confirmação:
 * - Se o cliente tem 2+ mesas: gera um ADITIVO de retirada
 * - Se o cliente tem apenas 1 mesa: gera um DISTRATO
 */
class ConfirmarRetiradaMesaDialogFragment : DialogFragment() {

    interface ConfirmarRetiradaDialogListener {
        fun onDialogPositiveClick(dialog: DialogFragment)
    }

    companion object {
        const val TAG = "ConfirmarRetiradaMesaDialogFragment"
        
        fun newInstance(): ConfirmarRetiradaMesaDialogFragment {
            return ConfirmarRetiradaMesaDialogFragment()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirmar Retirada de Mesa")
            .setMessage(
                "Tem certeza que deseja retirar esta mesa do cliente?\n\n" +
                "Após a retirada:\n" +
                "• Se o cliente ainda tiver outras mesas, será gerado um ADITIVO de retirada.\n" +
                "• Se esta for a última mesa, será gerado um DISTRATO.\n\n" +
                "Esta ação não pode ser desfeita."
            )
            .setPositiveButton("Confirmar") { _, _ ->
                // Buscar o listener (parent fragment)
                val listener = parentFragment as? ConfirmarRetiradaDialogListener
                    ?: activity as? ConfirmarRetiradaDialogListener
                
                listener?.onDialogPositiveClick(this)
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }
}

