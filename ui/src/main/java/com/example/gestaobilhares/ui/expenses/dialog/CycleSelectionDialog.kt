package com.example.gestaobilhares.ui.expenses.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.data.entities.CicloAcertoEntity
import com.example.gestaobilhares.ui.databinding.DialogCycleSelectionBinding
import com.example.gestaobilhares.ui.expenses.adapter.CycleSelectionAdapter

/**
 * Dialog para seleção de ciclo de acerto
 * Usado para filtrar despesas globais por ciclo
 */
class CycleSelectionDialog : DialogFragment() {

    private var _binding: DialogCycleSelectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var cycleAdapter: CycleSelectionAdapter
    private var onCycleSelected: ((CicloAcertoEntity) -> Unit)? = null

    companion object {
        fun show(
            fragmentManager: androidx.fragment.app.FragmentManager,
            cycles: List<CicloAcertoEntity>,
            onCycleSelected: (CicloAcertoEntity) -> Unit
        ) {
            val dialog = CycleSelectionDialog().apply {
                this.onCycleSelected = onCycleSelected
            arguments = Bundle().apply {
                putSerializable("cycles", ArrayList(cycles))
            }
            }
            dialog.show(fragmentManager, "CycleSelectionDialog")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCycleSelectionBinding.inflate(layoutInflater)
        return androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupRecyclerView()
        loadCycles()
    }

    private fun setupUI() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun setupRecyclerView() {
        cycleAdapter = CycleSelectionAdapter { cycle ->
            onCycleSelected?.invoke(cycle)
            dismiss()
        }

        binding.rvCycles.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cycleAdapter
        }
    }

    private fun loadCycles() {
        val cycles = arguments?.getSerializable("cycles") as? ArrayList<CicloAcertoEntity> ?: emptyList()
        cycleAdapter.submitList(cycles)
        
        if (cycles.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvCycles.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvCycles.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

