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
        fun newInstance(panoGroup: PanoGroup): PanoDetailsDialog {
            return PanoDetailsDialog().apply {
                arguments = Bundle().apply {
                    putString("cor", panoGroup.cor)
                    putString("tamanho", panoGroup.tamanho)
                    putString("material", panoGroup.material)
                    putInt("quantidadeDisponivel", panoGroup.quantidadeDisponivel)
                    putInt("quantidadeTotal", panoGroup.quantidadeTotal)
                    putString("observacoes", panoGroup.observacoes)
                    putString("numeroInicial", panoGroup.numeroInicial)
                    putString("numeroFinal", panoGroup.numeroFinal)
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
        
        binding.rvPanoDetails.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPanoDetails.adapter = adapter
    }

    private fun loadPanoGroup() {
        val cor = arguments?.getString("cor") ?: ""
        val tamanho = arguments?.getString("tamanho") ?: ""
        val material = arguments?.getString("material") ?: ""
        val quantidadeDisponivel = arguments?.getInt("quantidadeDisponivel") ?: 0
        val quantidadeTotal = arguments?.getInt("quantidadeTotal") ?: 0
        val observacoes = arguments?.getString("observacoes")
        val numeroInicial = arguments?.getString("numeroInicial") ?: ""
        val numeroFinal = arguments?.getString("numeroFinal") ?: ""

        // Atualizar header
        binding.tvGroupInfo.text = "Panos $cor - $tamanho - $material"
        binding.tvGroupCount.text = "$quantidadeDisponivel de $quantidadeTotal panos disponíveis"

        // TODO: Carregar panos individuais do banco de dados
        // Por enquanto, criar dados mock baseados no grupo
        val panosMock = createMockPanos(cor, tamanho, material, quantidadeTotal, numeroInicial)
        adapter.submitList(panosMock)
    }

    private fun createMockPanos(cor: String, tamanho: String, material: String, quantidade: Int, numeroInicial: String): List<PanoEstoque> {
        val panos = mutableListOf<PanoEstoque>()
        val numeroBase = numeroInicial.replace("P", "").toIntOrNull() ?: 1
        
        for (i in 0 until quantidade) {
            val numero = numeroBase + i
            val pano = PanoEstoque(
                id = numero.toLong(),
                numero = "P$numero",
                cor = cor,
                tamanho = tamanho,
                material = material,
                disponivel = i < quantidade * 0.8, // 80% disponíveis para simulação
                observacoes = if (i == 2) "Pano com pequeno rasgo" else null
            )
            panos.add(pano)
        }
        
        return panos
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
