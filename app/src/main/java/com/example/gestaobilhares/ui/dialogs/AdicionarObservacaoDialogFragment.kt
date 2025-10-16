package com.example.gestaobilhares.ui.dialogs

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.gestaobilhares.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AdicionarObservacaoDialogFragment : DialogFragment() {

    interface AdicionarObservacaoDialogListener {
        fun onObservacaoAdicionada(textoObservacao: String)
    }

    private var listener: AdicionarObservacaoDialogListener? = null
    private var dataSelecionada: Date? = Calendar.getInstance().time

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = parentFragment as AdicionarObservacaoDialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$parentFragment must implement AdicionarObservacaoDialogListener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_observation, null)
        val etObservacao = dialogView.findViewById<EditText>(R.id.etObservation)
        val btnData = dialogView.findViewById<Button>(R.id.btnDate)
        val tvDataSelecionada = dialogView.findViewById<TextView>(R.id.tvSelectedDate)

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        tvDataSelecionada.text = "Data: ${sdf.format(dataSelecionada!!)}"

        btnData.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    dataSelecionada = calendar.time
                    tvDataSelecionada.text = "Data: ${sdf.format(dataSelecionada!!)}"
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("Adicionar Observação")
            .setView(dialogView)
            .setPositiveButton("Salvar") { _, _ ->
                val textoObservacao = etObservacao.text.toString()
                if (textoObservacao.isNotBlank() && dataSelecionada != null) {
                    listener?.onObservacaoAdicionada(textoObservacao)
                } else {
                    Toast.makeText(context, "Observação e data são obrigatórios.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }

    companion object {
        const val TAG = "AdicionarObservacaoDialog"

        fun newInstance(): AdicionarObservacaoDialogFragment {
            return AdicionarObservacaoDialogFragment()
        }
    }
}
