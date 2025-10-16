package com.example.gestaobilhares.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.gestaobilhares.R

class RotaNaoIniciadaDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            AlertDialog.Builder(it)
                .setTitle("Novo Acerto Não Permitido")
                .setMessage("Para realizar acertos, a rota deve estar com status 'Em Andamento'. Inicie a rota primeiro na tela de clientes.")
                .setPositiveButton(R.string.entendi, null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setCancelable(true)
                .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    companion object {
        const val TAG = "RotaNaoIniciadaDialog"
    }
}
