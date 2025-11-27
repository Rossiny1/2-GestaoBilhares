package com.example.gestaobilhares.ui.clients
import com.example.gestaobilhares.ui.R

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import com.example.gestaobilhares.ui.databinding.DialogAdvancedSearchBinding

/**
 * Diálogo para pesquisa avançada de clientes
 */
class AdvancedSearchDialog(
    context: Context,
    private val onSearch: (SearchType, String) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogAdvancedSearchBinding
    private var selectedSearchType: SearchType = SearchType.NOME_CLIENTE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogAdvancedSearchBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        setupUI()
        setupListeners()
        setupSearchTypeDropdown()
    }

    private fun setupUI() {
        // Configurar largura do diálogo
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupSearchTypeDropdown() {
        val searchTypes = SearchType.values()
        val adapter = ArrayAdapter(
            context,
            android.R.layout.simple_dropdown_item_1line,
            searchTypes.map { it.label }
        )
        
        binding.actvSearchType.setAdapter(adapter)
        binding.actvSearchType.setText(searchTypes[0].label, false)
        updateSearchCriteriaHint()
        
        // Listener para quando o tipo de pesquisa mudar
        binding.actvSearchType.setOnItemClickListener { _, _, position, _ ->
            selectedSearchType = searchTypes[position]
            updateSearchCriteriaHint()
        }
    }

    private fun updateSearchCriteriaHint() {
        binding.tilSearchCriteria.hint = selectedSearchType.hint
    }

    private fun setupListeners() {
        // Botão Cancelar
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        // Botão Pesquisar
        binding.btnSearch.setOnClickListener {
            val criteria = binding.etSearchCriteria.text.toString().trim()
            
            if (criteria.isBlank()) {
                binding.tilSearchCriteria.error = "Digite o critério da pesquisa"
                return@setOnClickListener
            }
            
            binding.tilSearchCriteria.error = null
            onSearch(selectedSearchType, criteria)
            dismiss()
        }
    }

    companion object {
        /**
         * Mostra o diálogo de pesquisa avançada
         */
        fun show(
            context: Context,
            onSearch: (SearchType, String) -> Unit
        ) {
            AdvancedSearchDialog(context, onSearch).show()
        }
    }
}
