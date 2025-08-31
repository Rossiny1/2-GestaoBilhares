package com.example.gestaobilhares.ui.expenses

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.databinding.FragmentExpenseCategoriesBinding
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.entities.CategoriaDespesa
import com.example.gestaobilhares.data.entities.NovaCategoriaDespesa
import com.example.gestaobilhares.data.entities.EdicaoCategoriaDespesa
import com.example.gestaobilhares.data.repository.CategoriaDespesaRepository
import com.example.gestaobilhares.ui.expenses.adapter.ExpenseCategoryAdapter
import com.example.gestaobilhares.ui.expenses.dialog.AddEditCategoryDialog
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ExpenseCategoriesFragment : Fragment() {

    private var _binding: FragmentExpenseCategoriesBinding? = null
    private val binding get() = _binding!!

    private lateinit var categoryAdapter: ExpenseCategoryAdapter
    private val categories = mutableListOf<CategoriaDespesa>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpenseCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        loadCategories()
    }

    private fun setupUI() {
        // Configurar botão voltar
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressed()
        }

        // Configurar botão adicionar
        binding.btnAddCategory.setOnClickListener {
            showAddCategoryDialog()
        }

        // Configurar RecyclerView
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        categoryAdapter = ExpenseCategoryAdapter(
            categories = categories,
            onEditClick = { category -> showEditCategoryDialog(category) },
            onDeleteClick = { category -> deleteCategory(category) }
        )

        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
        }

        // Configurar drag and drop para reordenação
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: androidx.recyclerview.widget.RecyclerView,
                viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                target: androidx.recyclerview.widget.RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition
                categoryAdapter.moveItem(fromPos, toPos)
                return true
            }

            override fun onSwiped(
                viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                direction: Int
            ) {
                val position = viewHolder.adapterPosition
                val category = categories[position]
                deleteCategory(category)
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.rvCategories)
    }

    private fun loadCategories() {
        // Carregar categorias do banco de dados
        lifecycleScope.launch {
            try {
                val database = AppDatabase.getDatabase(requireContext())
                val categoriaRepository = CategoriaDespesaRepository(database.categoriaDespesaDao())
                
                categoriaRepository.buscarAtivas().collect { categorias ->
                    categories.clear()
                    categories.addAll(categorias)
                    updateUI()
                }
            } catch (e: Exception) {
                Log.e("ExpenseCategoriesFragment", "Erro ao carregar categorias: ${e.message}", e)
                Toast.makeText(requireContext(), "Erro ao carregar categorias: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddCategoryDialog() {
        AddEditCategoryDialog.show(
            fragmentManager = parentFragmentManager,
            category = null,
            onSave = { name ->
                addCategory(name)
            }
        )
    }

    private fun showEditCategoryDialog(category: CategoriaDespesa) {
        AddEditCategoryDialog.show(
            fragmentManager = parentFragmentManager,
            category = category,
            onSave = { name ->
                updateCategory(category, name)
            }
        )
    }

    private fun addCategory(name: String) {
        lifecycleScope.launch {
            try {
                val database = AppDatabase.getDatabase(requireContext())
                val categoriaRepository = CategoriaDespesaRepository(database.categoriaDespesaDao())
                
                val novaCategoria = NovaCategoriaDespesa(
                    nome = name,
                    descricao = "",
                    criadoPor = ""
                )
                
                val categoriaId = categoriaRepository.criarCategoria(novaCategoria)
                
                Snackbar.make(
                    binding.root,
                    "Categoria '$name' adicionada com sucesso!",
                    Snackbar.LENGTH_SHORT
                ).show()
                
                // Recarregar categorias
                loadCategories()
            } catch (e: Exception) {
                Log.e("ExpenseCategoriesFragment", "Erro ao adicionar categoria: ${e.message}", e)
                Toast.makeText(requireContext(), "Erro ao adicionar categoria: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateCategory(category: CategoriaDespesa, newName: String) {
        lifecycleScope.launch {
            try {
                val database = AppDatabase.getDatabase(requireContext())
                val categoriaRepository = CategoriaDespesaRepository(database.categoriaDespesaDao())
                
                val edicaoCategoria = EdicaoCategoriaDespesa(
                    id = category.id,
                    nome = newName,
                    descricao = category.descricao,
                    ativa = category.ativa
                )
                
                categoriaRepository.editarCategoria(edicaoCategoria)
                
                Snackbar.make(
                    binding.root,
                    "Categoria atualizada para '$newName'!",
                    Snackbar.LENGTH_SHORT
                ).show()
                
                // Recarregar categorias
                loadCategories()
            } catch (e: Exception) {
                Log.e("ExpenseCategoriesFragment", "Erro ao atualizar categoria: ${e.message}", e)
                Toast.makeText(requireContext(), "Erro ao atualizar categoria: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteCategory(category: CategoriaDespesa) {
        lifecycleScope.launch {
            try {
                val database = AppDatabase.getDatabase(requireContext())
                val categoriaRepository = CategoriaDespesaRepository(database.categoriaDespesaDao())
                
                categoriaRepository.deletar(category)
                
                Snackbar.make(
                    binding.root,
                    "Categoria '${category.nome}' removida!",
                    Snackbar.LENGTH_LONG
                ).show()
                
                // Recarregar categorias
                loadCategories()
            } catch (e: Exception) {
                Log.e("ExpenseCategoriesFragment", "Erro ao deletar categoria: ${e.message}", e)
                Toast.makeText(requireContext(), "Erro ao deletar categoria: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI() {
        if (categories.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvCategories.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvCategories.visibility = View.VISIBLE
            categoryAdapter.notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
