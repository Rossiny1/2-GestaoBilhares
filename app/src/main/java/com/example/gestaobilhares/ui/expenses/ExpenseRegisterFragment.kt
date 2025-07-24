package com.example.gestaobilhares.ui.expenses

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.gestaobilhares.R
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.entities.CategoriaDespesa
import com.example.gestaobilhares.data.entities.TipoDespesa
import com.example.gestaobilhares.data.repository.*
import com.example.gestaobilhares.databinding.FragmentExpenseRegisterBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Fragment para cadastro de despesas.
 * Permite criar despesas vinculadas ao ciclo de acertos atual.
 */
class ExpenseRegisterFragment : Fragment() {

    private var _binding: FragmentExpenseRegisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ExpenseRegisterViewModel
    private val args: ExpenseRegisterFragmentArgs by navArgs()

    // Formatador de moeda brasileiro
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    
    // Formatador de data
    private val dateFormatter = DateTimeFormatter.ofPattern("dd 'de' MMM 'de' yyyy", Locale("pt", "BR"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpenseRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Inicializar ViewModel
        val database = AppDatabase.getDatabase(requireContext())
        viewModel = ExpenseRegisterViewModel(
            DespesaRepository(database.despesaDao()),
            CategoriaDespesaRepository(database.categoriaDespesaDao()),
            TipoDespesaRepository(database.tipoDespesaDao()),
            CicloAcertoRepository(
                database.cicloAcertoDao(),
                DespesaRepository(database.despesaDao()),
                AcertoRepository(database.acertoDao(), database.clienteDao()),
                ClienteRepository(database.clienteDao())
            )
        )
        
        setupUI()
        setupClickListeners()
        observeViewModel()
        
        // Definir data atual
        updateDateDisplay()
    }

    private fun setupUI() {
        // Configurar campo de valor
        binding.etValorDespesa.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                s?.toString()?.let { text ->
                    if (text.isNotEmpty()) {
                        try {
                            val value = text.toDouble()
                            binding.tilValorDespesa.prefixText = "R$ "
                        } catch (e: NumberFormatException) {
                            binding.tilValorDespesa.prefixText = ""
                        }
                    } else {
                        binding.tilValorDespesa.prefixText = ""
                    }
                }
            }
        })
    }

    private fun setupClickListeners() {
        // Botão voltar
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Campo de data
        binding.tilDataDespesa.setEndIconOnClickListener {
            showDatePicker()
        }
        binding.etDataDespesa.setOnClickListener {
            showDatePicker()
        }

        // Campo de categoria
        binding.tilCategoriaDespesa.setEndIconOnClickListener {
            showCategorySelectionDialog()
        }
        binding.etCategoriaDespesa.setOnClickListener {
            showCategorySelectionDialog()
        }

        // Campo de tipo
        binding.tilTipoDespesa.setEndIconOnClickListener {
            showTypeSelectionDialog()
        }
        binding.etTipoDespesa.setOnClickListener {
            showTypeSelectionDialog()
        }

        // Botões de ação
        binding.btnCancelar.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnSalvar.setOnClickListener {
            saveExpense()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                val binding = _binding ?: return@collect
                binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.btnSalvar.isEnabled = !isLoading
                binding.btnCancelar.isEnabled = !isLoading
            }
        }

        lifecycleScope.launch {
            viewModel.message.collect { message ->
                val binding = _binding ?: return@collect
                message?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                    viewModel.clearMessage()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.success.collect { success ->
                val binding = _binding ?: return@collect
                if (success) {
                    Toast.makeText(requireContext(), "Despesa salva com sucesso!", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                    viewModel.resetSuccess()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.selectedDate.collect { date ->
                val binding = _binding ?: return@collect
                updateDateDisplay()
            }
        }

        lifecycleScope.launch {
            viewModel.selectedCategory.collect { category ->
                val binding = _binding ?: return@collect
                binding.etCategoriaDespesa.setText(category?.nome ?: "")
                binding.etTipoDespesa.setText("") // Reset tipo
            }
        }

        lifecycleScope.launch {
            viewModel.selectedType.collect { type ->
                val binding = _binding ?: return@collect
                binding.etTipoDespesa.setText(type?.nome ?: "")
            }
        }
    }

    private fun showDatePicker() {
        val currentDate = viewModel.selectedDate.value
        val year = currentDate.year
        val month = currentDate.monthValue - 1
        val day = currentDate.dayOfMonth

        DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val newDate = LocalDateTime.of(selectedYear, selectedMonth + 1, selectedDay, 
                    currentDate.hour, currentDate.minute)
                viewModel.setSelectedDate(newDate)
            },
            year,
            month,
            day
        ).show()
    }

    private fun showCategorySelectionDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_select_category, null)
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Configurar RecyclerView de categorias
        val recyclerView = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvCategories)
        val adapter = CategorySelectionAdapter { category ->
            viewModel.setSelectedCategory(category)
            dialog.dismiss()
        }
        
        recyclerView.adapter = adapter
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())

        // Observar categorias
        lifecycleScope.launch {
            viewModel.categories.collect { categories ->
                adapter.submitList(categories)
            }
        }

        // Configurar campo de pesquisa
        val searchEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSearch)
        searchEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s.toString().lowercase()
                lifecycleScope.launch {
                    viewModel.categories.collect { categories ->
                        val filtered = categories.filter { 
                            it.nome.lowercase().contains(query) 
                        }
                        adapter.submitList(filtered)
                    }
                }
            }
        })

        // Configurar botão "Novo"
        dialogView.findViewById<View>(R.id.btnNewCategory).setOnClickListener {
            dialog.dismiss()
            showCreateCategoryDialog()
        }

        // Configurar botões de ação
        dialogView.findViewById<View>(R.id.btnClear).setOnClickListener {
            viewModel.setSelectedCategory(null)
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.btnDone).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showTypeSelectionDialog() {
        val selectedCategory = viewModel.selectedCategory.value
        if (selectedCategory == null) {
            Toast.makeText(requireContext(), "Selecione uma categoria primeiro", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_select_category, null)
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Configurar título
        dialogView.findViewById<android.widget.TextView>(R.id.tvTitle).text = "Tipo da Despesa"

        // Configurar RecyclerView de tipos
        val recyclerView = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvCategories)
        val adapter = TypeSelectionAdapter { type ->
            if (!isAdded || _binding == null) return@TypeSelectionAdapter
            viewModel.setSelectedType(type)
            dialog.dismiss()
        }
        
        recyclerView.adapter = adapter
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())

        // Observar tipos
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.types.collect { types ->
                adapter.submitList(types)
            }
        }

        // Configurar campo de pesquisa
        val searchEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSearch)
        searchEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s.toString().lowercase()
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.types.collect { types ->
                        val filtered = types.filter { 
                            it.nome.lowercase().contains(query) 
                        }
                        adapter.submitList(filtered)
                    }
                }
            }
        })

        // Configurar botão "Novo"
        dialogView.findViewById<View>(R.id.btnNewCategory).setOnClickListener {
            dialog.dismiss()
            showCreateTypeDialog(selectedCategory.id)
        }

        // Configurar botões de ação
        dialogView.findViewById<View>(R.id.btnClear).setOnClickListener {
            if (!isAdded || _binding == null) return@setOnClickListener
            viewModel.setSelectedType(null)
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.btnDone).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showCreateCategoryDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(android.R.layout.simple_list_item_1, null)
        
        val editText = com.google.android.material.textfield.TextInputEditText(requireContext()).apply {
            hint = "Nome da categoria"
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nova Categoria")
            .setView(editText)
            .setPositiveButton("Criar") { _, _ ->
                val nome = editText.text.toString().trim()
                if (nome.isNotEmpty()) {
                    viewModel.createCategory(nome)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showCreateTypeDialog(categoriaId: Long) {
        val editText = com.google.android.material.textfield.TextInputEditText(requireContext()).apply {
            hint = "Nome do tipo"
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Novo Tipo")
            .setView(editText)
            .setPositiveButton("Criar") { _, _ ->
                val nome = editText.text.toString().trim()
                if (nome.isNotEmpty()) {
                    viewModel.createType(categoriaId, nome)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateDateDisplay() {
        val currentDate = viewModel.selectedDate.value
        binding.etDataDespesa.setText(currentDate.format(dateFormatter))
    }

    private fun saveExpense() {
        val descricao = binding.etDescricao.text.toString().trim()
        val valorText = binding.etValorDespesa.text.toString().trim()
        val quantidadeText = binding.etQuantidade.text.toString().trim()

        if (descricao.isEmpty()) {
            binding.tilDescricao.error = "Descrição é obrigatória"
            return
        }

        if (valorText.isEmpty()) {
            binding.tilValorDespesa.error = "Valor é obrigatório"
            return
        }

        val valor = try {
            valorText.toDouble()
        } catch (e: NumberFormatException) {
            binding.tilValorDespesa.error = "Valor inválido"
            return
        }

        val quantidade = try {
            quantidadeText.toIntOrNull() ?: 1
        } catch (e: NumberFormatException) {
            binding.tilQuantidade.error = "Quantidade inválida"
            return
        }

        if (valor <= 0) {
            binding.tilValorDespesa.error = "Valor deve ser maior que zero"
            return
        }

        if (quantidade <= 0) {
            binding.tilQuantidade.error = "Quantidade deve ser maior que zero"
            return
        }

        // Limpar erros
        binding.tilDescricao.error = null
        binding.tilValorDespesa.error = null
        binding.tilQuantidade.error = null

        // Salvar despesa
        viewModel.saveExpense(
            rotaId = args.rotaId,
            descricao = descricao,
            valor = valor,
            quantidade = quantidade,
            observacoes = "" // TODO: Adicionar campo de observações se necessário
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 