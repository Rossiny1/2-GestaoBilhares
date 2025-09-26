package com.example.gestaobilhares.ui.inventory.stock

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.R
import com.example.gestaobilhares.data.entities.PanoEstoque
import com.example.gestaobilhares.databinding.DialogPanoDetailsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Dialog para mostrar detalhes individuais dos panos de um grupo
 */
class PanoDetailsDialog : DialogFragment() {

    private var _binding: DialogPanoDetailsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PanoDetailAdapter
    private var panoGroup: PanoGroup? = null

    companion object {
        private const val ARG_PANO_GROUP = "pano_group"
        
        fun newInstance(panoGroup: PanoGroup): PanoDetailsDialog {
            return PanoDetailsDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_PANO_GROUP, panoGroup)
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.d("PanoDetailsDialog", "Criando diálogo de detalhes dos panos")
        
        _binding = DialogPanoDetailsBinding.inflate(LayoutInflater.from(requireContext()))

        setupUI()
        loadPanoGroup()

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Detalhes dos Panos")
            .setView(binding.root)
            .setPositiveButton("Fechar") { _, _ ->
                dismiss()
            }
            .create()
    }

    private fun setupUI() {
        adapter = PanoDetailAdapter { pano ->
            Log.d("PanoDetailsDialog", "Pano clicado: ${pano.numero}")
            // TODO: Implementar ação ao clicar em um pano individual
        }
        
        binding.rvPanoDetails.layoutManager = LinearLayoutManager(requireContext()).apply {
            // Forçar medição correta dos itens
            isItemPrefetchEnabled = false
        }
        binding.rvPanoDetails.adapter = adapter
        binding.rvPanoDetails.setHasFixedSize(true)
        binding.rvPanoDetails.isNestedScrollingEnabled = false
    }

    private fun loadPanoGroup() {
        panoGroup = arguments?.getParcelable(ARG_PANO_GROUP)
        
        panoGroup?.let { group ->
            // Atualizar header
            binding.tvGroupInfo.text = "Panos ${group.cor} - ${group.tamanho} - ${group.material}"
            binding.tvGroupCount.text = "${group.quantidadeDisponivel} de ${group.quantidadeTotal} panos disponíveis"

            // Usar os panos reais do grupo
            adapter.submitList(group.panos.sortedBy { it.numero })
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
