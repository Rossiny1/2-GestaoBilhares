package com.example.gestaobilhares.ui.inventory
import com.example.gestaobilhares.ui.R

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class InventorySelectionDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(com.example.gestaobilhares.ui.R.layout.dialog_inventory_selection, null, false)

        view.findViewById<View>(com.example.gestaobilhares.ui.R.id.cardEstoque).setOnClickListener {
            dismiss()
            findNavController().navigate(com.example.gestaobilhares.ui.R.id.stockFragment)
        }
        view.findViewById<View>(com.example.gestaobilhares.ui.R.id.cardVeiculos).setOnClickListener {
            dismiss()
            findNavController().navigate(com.example.gestaobilhares.ui.R.id.vehiclesFragment)
        }
        view.findViewById<View>(com.example.gestaobilhares.ui.R.id.cardEquipamentos).setOnClickListener {
            dismiss()
            findNavController().navigate(com.example.gestaobilhares.ui.R.id.equipmentsFragment)
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .create()
    }
}

