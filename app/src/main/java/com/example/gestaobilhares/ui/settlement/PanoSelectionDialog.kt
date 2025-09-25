package com.example.gestaobilhares.ui.settlement

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.R
import com.example.gestaobilhares.data.entities.PanoEstoque
import com.example.gestaobilhares.data.repository.PanoEstoqueRepository
import com.example.gestaobilhares.databinding.DialogSelectPanoBinding
import com.example.gestaobilhares.databinding.ItemPanoSelectionBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Dialog para seleção de pano no acerto.
 * Permite escolher um pano disponível do estoque para trocar na mesa.
 */
@AndroidEntryPoint
class PanoSelectionDialog : DialogFragment() {

    private var _binding: DialogSelectPanoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettlementViewModel by viewModels({ requireParentFragment() })
    private lateinit var adapter: PanoSelectionAdapter
    private var selectedPano: PanoEstoque? = null
    private var onPanoSelected: ((PanoEstoque) -> Unit)? = null
    
    @Inject
    lateinit var panoEstoqueRepository: PanoEstoqueRepository

    companion object {
        fun newInstance(onPanoSelected: (PanoEstoque) -> Unit, tamanhoMesa: String? = null): PanoSelectionDialog {
            return PanoSelectionDialog().apply {
                this.onPanoSelected = onPanoSelected
                arguments = Bundle().apply {
                    putString("tamanho_mesa", tamanhoMesa)
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogSelectPanoBinding.inflate(LayoutInflater.from(requireContext()))

        setupUI()
        setupClickListeners()
        loadPanos()

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Selecionar Pano")
            .setView(binding.root)
            .setPositiveButton("Confirmar") { _, _ ->
                selectedPano?.let { pano ->
                    onPanoSelected?.invoke(pano)
                }
            }
            .setNegativeButton("Cancelar") { _, _ ->
                dismiss()
            }
            .create()
    }

    private fun setupUI() {
        adapter = PanoSelectionAdapter { pano ->
            selectedPano = pano
            adapter.notifyDataSetChanged()
            updateConfirmButton()
        }
        
        binding.rvPanos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPanos.adapter = adapter
    }

    private fun setupClickListeners() {
        // Filtros
        binding.etFiltroCor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterPanos()
            }
        })

        binding.etFiltroNumero.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterPanos()
            }
        })
    }

    private fun loadPanos() {
        // Carregar panos disponíveis do banco de dados
        // Usar lifecycleScope do DialogFragment, pois onCreateDialog é chamado antes da View existir
        lifecycleScope.launch {
            try {
                val tamanhoMesa = arguments?.getString("tamanho_mesa")
                
                if (tamanhoMesa != null) {
                    // Filtrar por tamanho da mesa
                    panoEstoqueRepository.buscarDisponiveisPorTamanho(tamanhoMesa).collect { panos ->
                        adapter.submitList(panos)
                    }
                } else {
                    // Carregar todos os panos disponíveis
                    panoEstoqueRepository.listarDisponiveis().collect { panos ->
                        adapter.submitList(panos)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PanoSelectionDialog", "Erro ao carregar panos: ${e.message}", e)
                // Fallback para dados mock em caso de erro
                loadPanosMock()
            }
        }
    }
    
    private fun loadPanosMock() {
        val panosMock = listOf(
            PanoEstoque(
                id = 1L,
                numero = "P001",
                cor = "Azul",
                tamanho = "Grande",
                material = "Veludo",
                disponivel = true,
                observacoes = null
            ),
            PanoEstoque(
                id = 2L,
                numero = "P002",
                cor = "Azul",
                tamanho = "Grande",
                material = "Veludo",
                disponivel = true,
                observacoes = null
            ),
            PanoEstoque(
                id = 3L,
                numero = "P003",
                cor = "Verde",
                tamanho = "Médio",
                material = "Feltro",
                disponivel = true,
                observacoes = null
            )
        )
        
        adapter.submitList(panosMock)
    }

    private fun filterPanos() {
        val corFilter = binding.etFiltroCor.text.toString().lowercase()
        val numeroFilter = binding.etFiltroNumero.text.toString().lowercase()
        
        // TODO: Implementar filtro real
        // Por enquanto, apenas recarregar a lista
        loadPanos()
    }

    private fun updateConfirmButton() {
        binding.btnConfirmar.isEnabled = selectedPano != null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * Adapter para seleção de panos
 */
class PanoSelectionAdapter(
    private val onPanoClick: (PanoEstoque) -> Unit
) : androidx.recyclerview.widget.ListAdapter<PanoEstoque, PanoSelectionAdapter.PanoViewHolder>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<PanoEstoque>() {
        override fun areItemsTheSame(oldItem: PanoEstoque, newItem: PanoEstoque): Boolean = 
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: PanoEstoque, newItem: PanoEstoque): Boolean = 
            oldItem == newItem
    }
) {
    private var selectedPano: PanoEstoque? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PanoViewHolder {
        val binding = ItemPanoSelectionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PanoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PanoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PanoViewHolder(
        private val binding: ItemPanoSelectionBinding
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {
        
        fun bind(pano: PanoEstoque) {
            binding.tvNumeroPano.text = pano.numero
            binding.tvCorPano.text = pano.cor
            binding.tvTamanhoPano.text = pano.tamanho
            binding.tvMaterialPano.text = pano.material
            
            // Mostrar seleção
            binding.ivSelected.visibility = if (selectedPano?.id == pano.id) View.VISIBLE else View.GONE
            
            binding.root.setOnClickListener {
                selectedPano = pano
                onPanoClick(pano)
                notifyDataSetChanged()
            }
        }
    }
}
