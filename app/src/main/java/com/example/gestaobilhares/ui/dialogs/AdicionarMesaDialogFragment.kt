package com.example.gestaobilhares.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.gestaobilhares.R
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.entities.TipoMesa
import com.google.android.material.textfield.TextInputEditText

class AdicionarMesaDialogFragment : DialogFragment() {

    interface AdicionarMesaDialogListener {
        fun onMesaAdicionada(novaMesa: Mesa)
    }

    private var listener: AdicionarMesaDialogListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = parentFragment as AdicionarMesaDialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$parentFragment must implement AdicionarMesaDialogListener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val clientId = arguments?.getLong(ARG_CLIENT_ID) ?: throw IllegalStateException("Client ID is required")

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_mesa, null)
        val etNumeroMesa = dialogView.findViewById<TextInputEditText>(R.id.etNumeroMesa)
        val spinnerTipoMesa = dialogView.findViewById<Spinner>(R.id.spinnerTipoMesa)
        val etRelogioInicial = dialogView.findViewById<TextInputEditText>(R.id.etRelogioInicial)

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.tipos_mesa,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerTipoMesa.adapter = adapter
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("Adicionar Nova Mesa")
            .setView(dialogView)
            .setPositiveButton("Adicionar") { _, _ ->
                val numeroMesa = etNumeroMesa.text.toString()
                val tipoMesaString = spinnerTipoMesa.selectedItem.toString()
                val tipoMesa = TipoMesa.valueOf(tipoMesaString)
                val relogioInicial = etRelogioInicial.text.toString().toIntOrNull() ?: 0

                if (numeroMesa.isNotBlank()) {
                    val novaMesa = Mesa(
                        clienteId = clientId,
                        numero = numeroMesa,
                        tipoMesa = tipoMesa,
                        relogioInicial = relogioInicial
                    )
                    listener?.onMesaAdicionada(novaMesa)
                } else {
                    Toast.makeText(context, "O número da mesa é obrigatório.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }

    companion object {
        const val TAG = "AdicionarMesaDialog"
        private const val ARG_CLIENT_ID = "client_id"

        fun newInstance(clientId: Long): AdicionarMesaDialogFragment {
            val args = Bundle()
            args.putLong(ARG_CLIENT_ID, clientId)
            val fragment = AdicionarMesaDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
