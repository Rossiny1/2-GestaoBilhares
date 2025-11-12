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
import com.example.gestaobilhares.databinding.FragmentExpenseTypesBinding
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.entities.CategoriaDespesa
import com.example.gestaobilhares.data.entities.TipoDespesa
import com.example.gestaobilhares.data.entities.TipoDespesaComCategoria
import com.example.gestaobilhares.data.entities.NovoTipoDespesa
import com.example.gestaobilhares.data.entities.EdicaoTipoDespesa
import com.example.gestaobilhares.data.repository.CategoriaDespesaRepository
import com.example.gestaobilhares.data.repository.TipoDespesaRepository
import com.example.gestaobilhares.data.factory.RepositoryFactory
import com.example.gestaobilhares.ui.expenses.adapter.ExpenseTypeAdapter
import com.example.gestaobilhares.ui.expenses.dialog.AddEditTypeDialog
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ExpenseTypesFragment : Fragment() {

    private var _binding: FragmentExpenseTypesBinding? = null
    private val binding get() = _binding!!

    private lateinit var typeAdapter: ExpenseTypeAdapter
    private val types = mutableListOf<TipoDespesaComCategoria>()
    private val categories = mutableListOf<CategoriaDespesa>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpenseTypesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        loadData()
    }

    private fun setupUI() {
        // Configurar botão voltar
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressed()
        }

        // Configurar botão adicionar
        binding.btnAddType.setOnClickListener {
            showAddTypeDialog()
        }

        // Configurar RecyclerView
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        typeAdapter = ExpenseTypeAdapter(
            types = types,
            onEditClick = { type -> showEditTypeDialog(type) },
            onDeleteClick = { type -> deleteType(type) }
        )

        binding.rvTypes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = typeAdapter
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
                val fromPos = viewHolder.bindingAdapterPosition
                val toPos = target.bindingAdapterPosition
                typeAdapter.moveItem(fromPos, toPos)
                return true
            }

            override fun onSwiped(
                viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                direction: Int
            ) {
                val position = viewHolder.bindingAdapterPosition
                val type = types[position]
                deleteType(type)
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.rvTypes)
    }

        private fun loadData() {
        // Carregar dados do banco de dados
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (!isAdded || context == null) return@launch
                
                val database = AppDatabase.getDatabase(requireContext())
                val appRepository = RepositoryFactory.getAppRepository(requireContext())
                val categoriaRepository = CategoriaDespesaRepository(database.categoriaDespesaDao(), appRepository)
                val tipoRepository = TipoDespesaRepository(database.tipoDespesaDao(), appRepository)
                
                // Carregar categorias e tipos em paralelo
                launch {
                    categoriaRepository.buscarAtivas().collect { categorias ->
                        if (!isAdded) return@collect
                        categories.clear()
                        categories.addAll(categorias)
                    }
                }
                
                launch {
                    tipoRepository.buscarAtivosComCategoria().collect { tipos ->
                        if (!isAdded) return@collect
                        types.clear()
                        types.addAll(tipos)
                        updateUI()
                    }
                }
            } catch (e: Exception) {
                Log.e("ExpenseTypesFragment", "Erro ao carregar dados: ${e.message}", e)
                if (isAdded && context != null) {
                    Toast.makeText(requireContext(), "Erro ao carregar dados: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showAddTypeDialog() {
        AddEditTypeDialog.show(
            fragmentManager = parentFragmentManager,
            type = null,
            categories = categories,
            onSave = { name, categoryId ->
                addType(name, categoryId)
            }
        )
    }

    private fun showEditTypeDialog(type: TipoDespesaComCategoria) {
        AddEditTypeDialog.show(
            fragmentManager = parentFragmentManager,
            type = type,
            categories = categories,
            onSave = { name, categoryId ->
                updateType(type, name, categoryId)
            }
        )
    }

    private fun addType(name: String, categoryId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (!isAdded || context == null) return@launch
                
                val database = AppDatabase.getDatabase(requireContext())
                val appRepository = RepositoryFactory.getAppRepository(requireContext())
                val tipoRepository = TipoDespesaRepository(database.tipoDespesaDao(), appRepository)
                
                val novoTipo = NovoTipoDespesa(
                    categoriaId = categoryId,
                    nome = name,
                    descricao = "",
                    criadoPor = ""
                )
                
                @Suppress("UNUSED_VARIABLE")
                val tipoId = tipoRepository.criarTipo(novoTipo)
                
                if (isAdded && context != null) {
                    Snackbar.make(
                        binding.root,
                        "Tipo '$name' adicionado com sucesso!",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    
                    // Recarregar dados
                    loadData()
                }
            } catch (e: Exception) {
                Log.e("ExpenseTypesFragment", "Erro ao adicionar tipo: ${e.message}", e)
                if (isAdded && context != null) {
                    Toast.makeText(requireContext(), "Erro ao adicionar tipo: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateType(type: TipoDespesaComCategoria, newName: String, categoryId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (!isAdded || context == null) return@launch
                
                val database = AppDatabase.getDatabase(requireContext())
                val appRepository = RepositoryFactory.getAppRepository(requireContext())
                val tipoRepository = TipoDespesaRepository(database.tipoDespesaDao(), appRepository)
                
                val edicaoTipo = EdicaoTipoDespesa(
                    id = type.id,
                    categoriaId = categoryId,
                    nome = newName,
                    descricao = type.descricao,
                    ativo = type.ativo
                )
                
                tipoRepository.editarTipo(edicaoTipo)
                
                if (isAdded && context != null) {
                    Snackbar.make(
                        binding.root,
                        "Tipo atualizado para '$newName'!",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    
                    // Recarregar dados
                    loadData()
                }
            } catch (e: Exception) {
                Log.e("ExpenseTypesFragment", "Erro ao atualizar tipo: ${e.message}", e)
                if (isAdded && context != null) {
                    Toast.makeText(requireContext(), "Erro ao atualizar tipo: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deleteType(type: TipoDespesaComCategoria) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (!isAdded || context == null) return@launch
                
                val database = AppDatabase.getDatabase(requireContext())
                val appRepository = RepositoryFactory.getAppRepository(requireContext())
                val tipoRepository = TipoDespesaRepository(database.tipoDespesaDao(), appRepository)
                
                tipoRepository.deletar(type.tipoDespesa)
                
                if (isAdded && context != null) {
                    Snackbar.make(
                        binding.root,
                        "Tipo '${type.nome}' removido!",
                        Snackbar.LENGTH_LONG
                    ).show()
                    
                    // Recarregar dados
                    loadData()
                }
            } catch (e: Exception) {
                Log.e("ExpenseTypesFragment", "Erro ao deletar tipo: ${e.message}", e)
                if (isAdded && context != null) {
                    Toast.makeText(requireContext(), "Erro ao deletar tipo: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateUI() {
        if (types.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvTypes.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvTypes.visibility = View.VISIBLE
            typeAdapter.notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
