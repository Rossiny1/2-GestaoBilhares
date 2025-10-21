package com.example.gestaobilhares.ui.expenses

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.gestaobilhares.R
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.factory.RepositoryFactory
import com.example.gestaobilhares.BuildConfig
import com.example.gestaobilhares.utils.DataValidator
import com.example.gestaobilhares.data.entities.CategoriaDespesa
import com.example.gestaobilhares.data.entities.TipoDespesa
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.databinding.FragmentExpenseRegisterBinding
import com.example.gestaobilhares.utils.ImageCompressionUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
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
    
    // ✅ CORREÇÃO: Inicialização segura do ImageCompressionUtils
    private val imageCompressionUtils: ImageCompressionUtils by lazy {
        ImageCompressionUtils(requireContext())
    }

    // Formatador de moeda brasileiro
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    
    // Formatador de data
    private val dateFormatter = DateTimeFormatter.ofPattern("dd 'de' MMM 'de' yyyy", Locale("pt", "BR"))
    
    // ✅ NOVO: Propriedades para captura de foto do comprovante
    private var currentPhotoUri: Uri? = null
    private var fotoComprovantePath: String? = null
    private var dataFotoComprovante: Date? = null
    private var selectedVehicleId: Long? = null
    
    // ✅ NOVO: Launchers para câmera e permissões
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            abrirCamera()
        } else {
            mostrarDialogoExplicacaoPermissao()
        }
    }
    
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            try {
                currentPhotoUri?.let { uri ->
                    Log.d("ExpenseRegisterFragment", "Foto do comprovante capturada com sucesso: $uri")
                    binding.root.post {
                        try {
                            val caminhoReal = obterCaminhoRealFoto(uri)
                            if (caminhoReal != null) {
                                Log.d("ExpenseRegisterFragment", "Caminho real da foto: $caminhoReal")
                                fotoComprovantePath = caminhoReal
                                dataFotoComprovante = Date()
                                mostrarFotoComprovante(caminhoReal, dataFotoComprovante)
                                Toast.makeText(requireContext(), "Foto do comprovante capturada com sucesso!", Toast.LENGTH_SHORT).show()
                            } else {
                                Log.e("ExpenseRegisterFragment", "Não foi possível obter o caminho real da foto")
                                Toast.makeText(requireContext(), "Erro: não foi possível salvar a foto", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Log.e("ExpenseRegisterFragment", "Erro ao processar foto: ${e.message}", e)
                            Toast.makeText(requireContext(), "Erro ao processar foto: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ExpenseRegisterFragment", "Erro crítico após captura de foto: ${e.message}", e)
                Toast.makeText(requireContext(), "Erro ao processar foto capturada", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(requireContext(), "Erro ao capturar foto", Toast.LENGTH_SHORT).show()
        }
    }

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
        val appRepository = RepositoryFactory.getAppRepository(requireContext())
        viewModel = ExpenseRegisterViewModel(appRepository)
        
        setupUI()
        setupClickListeners()
        observeViewModel()
        
        // ✅ NOVO: Verificar se é modo de edição
        if (args.modoEdicao && args.despesaId > 0) {
            carregarDespesaParaEdicao(args.despesaId)
            // ✅ CORREÇÃO: Usar o título correto do toolbar
            binding.toolbar.findViewById<android.widget.TextView>(android.R.id.title)?.text = "Editar Despesa"
        } else {
            // Definir data atual apenas para nova despesa
            updateDateDisplay()
        }
    }

    /**
     * ✅ NOVO: Carrega dados da despesa para edição
     */
    private fun carregarDespesaParaEdicao(despesaId: Long) {
        lifecycleScope.launch {
            try {
                val despesa = viewModel.carregarDespesaParaEdicao(despesaId)
                if (despesa != null) {
                    preencherCamposComDespesa(despesa)
                } else {
                    Toast.makeText(requireContext(), "Despesa não encontrada", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro ao carregar despesa: ${e.message}", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }
    }

    /**
     * ✅ NOVO: Preenche campos com dados da despesa
     */
    private fun preencherCamposComDespesa(despesa: com.example.gestaobilhares.data.entities.Despesa) {
        binding.apply {
            etDescricao.setText(despesa.descricao)
            etValorDespesa.setText(String.format("%.2f", despesa.valor))
            etQuantidade.setText("1") // Por enquanto, sempre 1
            
            // Definir data da despesa
            val dataDespesa = despesa.dataHora
            viewModel.setSelectedDate(dataDespesa)
            updateDateDisplay()
            
            // Definir categoria e tipo
            viewModel.setSelectedCategory(despesa.categoria)
            viewModel.setSelectedType(despesa.tipoDespesa)
            updateCategoryDisplay()
            updateTypeDisplay()
            
            // ✅ NOVO: Carregar foto do comprovante se existir
            if (!despesa.fotoComprovante.isNullOrEmpty()) {
                fotoComprovantePath = despesa.fotoComprovante
                dataFotoComprovante = despesa.dataFotoComprovante
                mostrarFotoComprovante(despesa.fotoComprovante, despesa.dataFotoComprovante)
            }
        }
    }

    private fun setupUI() {
        // Mostrar/ocultar seleção de ciclo para fluxo global (+ despesa do gerenciamento)
        if (args.rotaId == 0L) {
            binding.tvCicloLabel.visibility = View.VISIBLE
            binding.tilCiclo.visibility = View.VISIBLE
            // Carregar ciclos
            viewModel.loadAllCycles()
        } else {
            binding.tvCicloLabel.visibility = View.GONE
            binding.tilCiclo.visibility = View.GONE
        }

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

        // Campo de ciclo (apenas no fluxo global)
        binding.tilCiclo.setEndIconOnClickListener {
            if (args.rotaId == 0L) showCycleSelectionDialog()
        }
        binding.etCiclo.setOnClickListener {
            if (args.rotaId == 0L) showCycleSelectionDialog()
        }

        // Campo de tipo
        binding.tilTipoDespesa.setEndIconOnClickListener {
            showTypeSelectionDialog()
        }
        binding.etTipoDespesa.setOnClickListener {
            showTypeSelectionDialog()
        }

        // ✅ NOVO: Campo de veículo (Viagem)
        binding.tilVeiculo.setEndIconOnClickListener { showVehicleSelectionDialog() }
        binding.etVeiculo.setOnClickListener { showVehicleSelectionDialog() }

        // ✅ NOVO: Botões de foto do comprovante
        binding.btnCameraComprovante.setOnClickListener {
            solicitarCapturaFotoComprovante()
        }
        
        binding.btnRemoverFotoComprovante.setOnClickListener {
            removerFotoComprovante()
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
                // ✅ Mostrar campos de viagem conforme categoria/tipo
                val categoria = viewModel.selectedCategory.value?.nome ?: ""
                val tipoNome = type?.nome ?: ""
                val isCombustivel = tipoNome.equals("Combustível", ignoreCase = true) || tipoNome.equals("Gasolina", ignoreCase = true)
                val isManutencao = tipoNome.equals("Manutenção", ignoreCase = true)
                val isVeiculoRequired = isCombustivel || isManutencao

                // ✅ CORREÇÃO: Mostrar campos baseado no TIPO, não na categoria
                binding.tvViagemHeader.visibility = if (isVeiculoRequired) View.VISIBLE else View.GONE
                binding.tilVeiculo.visibility = if (isVeiculoRequired) View.VISIBLE else View.GONE
                binding.tilKm.visibility = if (isVeiculoRequired) View.VISIBLE else View.GONE
                binding.tilLitros.visibility = if (isCombustivel) View.VISIBLE else View.GONE
            }
        }

        // Atualizar exibição do ciclo selecionado
        lifecycleScope.launch {
            viewModel.selectedCycle.collect { ciclo ->
                val binding = _binding ?: return@collect
                if (args.rotaId == 0L) {
                    val texto = if (ciclo != null) "${ciclo.ano} - ${ciclo.numeroCiclo}º (Rota #${ciclo.rotaId})" else ""
                    binding.etCiclo.setText(texto)
                }
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

    private fun showCycleSelectionDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_select_category, null)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<android.widget.TextView>(R.id.tvTitle).text = "Selecione o Ciclo"

        val recyclerView = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvCategories)
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())

        val adapter = object : androidx.recyclerview.widget.ListAdapter<com.example.gestaobilhares.data.entities.CicloAcertoEntity, androidx.recyclerview.widget.RecyclerView.ViewHolder>(
            object : androidx.recyclerview.widget.DiffUtil.ItemCallback<com.example.gestaobilhares.data.entities.CicloAcertoEntity>() {
                override fun areItemsTheSame(oldItem: com.example.gestaobilhares.data.entities.CicloAcertoEntity, newItem: com.example.gestaobilhares.data.entities.CicloAcertoEntity): Boolean = oldItem.id == newItem.id
                override fun areContentsTheSame(oldItem: com.example.gestaobilhares.data.entities.CicloAcertoEntity, newItem: com.example.gestaobilhares.data.entities.CicloAcertoEntity): Boolean = oldItem == newItem
            }
        ) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_expense_category, parent, false)
                return object : androidx.recyclerview.widget.RecyclerView.ViewHolder(v) {}
            }

            override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
                val ciclo = currentList[position]
                val title = holder.itemView.findViewById<android.widget.TextView>(R.id.tvCategoryName)
                val subtitle = holder.itemView.findViewById<android.widget.TextView>(R.id.tvTypeCount)
                title.text = "${ciclo.ano} - ${ciclo.numeroCiclo}º"
                subtitle.text = "Rota #${ciclo.rotaId} • Status: ${ciclo.status}"
                holder.itemView.setOnClickListener {
                    viewModel.setSelectedCycle(ciclo)
                    dialog.dismiss()
                }
            }
        }

        recyclerView.adapter = adapter

        // Coletar ciclos
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.cycles.collect { lista ->
                adapter.submitList(lista)
            }
        }

        // Pesquisa
        val searchEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSearch)
        searchEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s.toString().lowercase()
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.cycles.collect { lista ->
                        val filtered = lista.filter { (it.ano.toString() + " " + it.numeroCiclo).lowercase().contains(query) }
                        adapter.submitList(filtered)
                    }
                }
            }
        })

        dialogView.findViewById<View>(R.id.btnClear).setOnClickListener {
            viewModel.setSelectedCycle(null)
            dialog.dismiss()
        }
        dialogView.findViewById<View>(R.id.btnDone).setOnClickListener { dialog.dismiss() }

        dialog.show()
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

        // Botão "Novo" removido - funcionalidade movida para menu principal

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

        // Botão "Novo" removido - funcionalidade movida para menu principal

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

    // ✅ NOVO: Diálogo de seleção de veículo
    private fun showVehicleSelectionDialog() {
        if (!isAdded || _binding == null) return
        
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_select_category, null)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<android.widget.TextView>(R.id.tvTitle).text = "Selecione o Veículo"

        val recyclerView = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvCategories)
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())

        val adapter = object : androidx.recyclerview.widget.ListAdapter<com.example.gestaobilhares.data.entities.Veiculo, androidx.recyclerview.widget.RecyclerView.ViewHolder>(
            object : androidx.recyclerview.widget.DiffUtil.ItemCallback<com.example.gestaobilhares.data.entities.Veiculo>() {
                override fun areItemsTheSame(oldItem: com.example.gestaobilhares.data.entities.Veiculo, newItem: com.example.gestaobilhares.data.entities.Veiculo): Boolean = oldItem.id == newItem.id
                override fun areContentsTheSame(oldItem: com.example.gestaobilhares.data.entities.Veiculo, newItem: com.example.gestaobilhares.data.entities.Veiculo): Boolean = oldItem == newItem
            }
        ) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_expense_category, parent, false)
                return object : androidx.recyclerview.widget.RecyclerView.ViewHolder(v) {}
            }

            override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
                val item = currentList[position]
                val title = holder.itemView.findViewById<android.widget.TextView>(R.id.tvCategoryName)
                val subtitle = holder.itemView.findViewById<android.widget.TextView>(R.id.tvTypeCount)
                title.text = "${item.marca} ${item.modelo}"
                subtitle.text = "Ano: ${item.anoModelo} • KM: ${item.kmAtual}"
                holder.itemView.setOnClickListener {
                    selectedVehicleId = item.id
                    if (isAdded && _binding != null) {
                        binding.etVeiculo.setText("${item.marca} ${item.modelo}")
                    }
                    dialog.dismiss()
                }
            }
        }

        recyclerView.adapter = adapter

        // Carregar veículos do banco
        var vehiclesCache: List<com.example.gestaobilhares.data.entities.Veiculo> = emptyList()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // ✅ CORREÇÃO: Verificar se o fragment ainda está ativo antes de usar requireContext()
                if (!isAdded) return@launch
                
                val context = requireContext() // Capturar o contexto uma vez
                val db = AppDatabase.getDatabase(context)
                db.veiculoDao().listar().collect { lista ->
                    if (!isAdded) return@collect // Verificar novamente antes de usar binding
                    vehiclesCache = lista
                    adapter.submitList(lista)
                }
            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Erro ao carregar veículos: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Pesquisa
        val searchEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSearch)
        searchEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s.toString().lowercase()
                val filtered = vehiclesCache.filter { (it.marca + " " + it.modelo).lowercase().contains(query) }
                adapter.submitList(filtered)
            }
        })

        dialogView.findViewById<View>(R.id.btnClear).setOnClickListener {
            selectedVehicleId = null
            if (isAdded && _binding != null) {
                binding.etVeiculo.setText("")
            }
            dialog.dismiss()
        }
        dialogView.findViewById<View>(R.id.btnDone).setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    // Função removida - criação de categoria movida para menu principal

    // Função removida - criação de tipo movida para menu principal

    private fun updateDateDisplay() {
        val currentDate = viewModel.selectedDate.value
        binding.etDataDespesa.setText(currentDate.format(dateFormatter))
    }

    /**
     * ✅ NOVO: Atualiza a exibição da categoria selecionada
     */
    private fun updateCategoryDisplay() {
        val category = viewModel.selectedCategory.value
        binding.etCategoriaDespesa.setText(category?.nome ?: "")
    }

    /**
     * ✅ NOVO: Atualiza a exibição do tipo selecionado
     */
    private fun updateTypeDisplay() {
        val type = viewModel.selectedType.value
        binding.etTipoDespesa.setText(type?.nome ?: "")
    }

    private fun saveExpense() {
        val descricao = binding.etDescricao.text.toString().trim()
        val valorText = binding.etValorDespesa.text.toString().trim()
        val quantidadeText = binding.etQuantidade.text.toString().trim()
        val kmText = binding.etKm.text?.toString()?.trim()
        val litrosText = binding.etLitros.text?.toString()?.trim()

        if (descricao.isEmpty()) {
            binding.tilDescricao.error = "Descrição é obrigatória"
            return
        }

        if (valorText.isEmpty()) {
            binding.tilValorDespesa.error = "Valor é obrigatório"
            return
        }

        // ✅ FASE 2: Usar DataValidator centralizado
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

        val resultadoValidacao = com.example.gestaobilhares.utils.DataValidator.validarDespesa(
            valor = valor,
            quantidade = quantidade,
            categoria = viewModel.selectedCategory.value?.nome,
            tipo = viewModel.selectedType.value?.nome,
            veiculoId = selectedVehicleId
        )

        if (resultadoValidacao.isErro()) {
            val erros = (resultadoValidacao as DataValidator.ResultadoValidacao.Erro).mensagens
            // Mostrar primeiro erro no campo correspondente
            when {
                erros.any { it.contains("Valor") } -> {
                    binding.tilValorDespesa.error = erros.first { it.contains("Valor") }
                }
                erros.any { it.contains("Quantidade") } -> {
                    binding.tilQuantidade.error = erros.first { it.contains("Quantidade") }
                }
                erros.any { it.contains("veículo") } -> {
                    binding.tilVeiculo.error = erros.first { it.contains("veículo") }
                }
            }
            return
        }

        // Regras de validação para Viagem
        val categoria = viewModel.selectedCategory.value?.nome ?: ""
        val tipoNome = viewModel.selectedType.value?.nome ?: ""
        val isCombustivel = tipoNome.equals("Combustível", ignoreCase = true) || tipoNome.equals("Gasolina", ignoreCase = true)
        val isManutencao = tipoNome.equals("Manutenção", ignoreCase = true)
        val isVeiculoRequired = isCombustivel || isManutencao

        var kmValue: Long? = null
        var litrosValue: Double? = null

        // ✅ CORREÇÃO: Validar campos baseado no TIPO, não na categoria
        if (isVeiculoRequired) {
            if (selectedVehicleId == null) {
                binding.tilVeiculo.error = "Selecione um veículo"
                return
            } else {
                binding.tilVeiculo.error = null
            }
            
            // KM é obrigatório para combustível e manutenção
            kmValue = kmText?.toLongOrNull()
            if (kmValue == null || kmValue <= 0) {
                binding.tilKm.error = "Informe o KM"
                return
            } else binding.tilKm.error = null
            
            // Litros é obrigatório apenas para combustível
            if (isCombustivel) {
                litrosValue = litrosText?.toDoubleOrNull()
                if (litrosValue == null || litrosValue <= 0.0) {
                    binding.tilLitros.error = "Informe os litros"
                    return
                } else binding.tilLitros.error = null
            }
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
            observacoes = "", // TODO: Adicionar campo de observações se necessário
            despesaId = args.despesaId,
            modoEdicao = args.modoEdicao,
            fotoComprovante = fotoComprovantePath,
            dataFotoComprovante = dataFotoComprovante,
            veiculoId = if (isVeiculoRequired) selectedVehicleId else null,
            kmRodado = if (isVeiculoRequired) kmValue else null,
            litrosAbastecidos = if (isCombustivel) litrosValue else null
        )
    }
    
    // ✅ NOVO: Métodos para captura de foto do comprovante
    
    private fun solicitarCapturaFotoComprovante() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            abrirCamera()
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }
    
    private fun abrirCamera() {
        try {
            val photoFile = criarArquivoFoto()
            currentPhotoUri = FileProvider.getUriForFile(
                requireContext(),
                "com.example.gestaobilhares.fileprovider",
                photoFile
            )
            cameraLauncher.launch(currentPhotoUri)
        } catch (e: Exception) {
            Log.e("ExpenseRegisterFragment", "Erro ao abrir câmera: ${e.message}", e)
            Toast.makeText(requireContext(), "Erro ao abrir câmera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun criarArquivoFoto(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "COMPROVANTE_${timeStamp}_"
        val storageDir = requireContext().getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }
    
    private fun obterCaminhoRealFoto(uri: Uri): String? {
        return try {
            Log.d("ExpenseRegisterFragment", "Obtendo caminho real para URI: $uri")
            
            // ✅ CORREÇÃO: Tentar comprimir a imagem com fallback seguro
            try {
                val compressedPath = imageCompressionUtils.compressImageFromUri(uri)
                if (compressedPath != null) {
                    Log.d("ExpenseRegisterFragment", "Imagem comprimida com sucesso: $compressedPath")
                    return compressedPath
                }
            } catch (e: Exception) {
                Log.w("ExpenseRegisterFragment", "Compressão falhou, usando método original: ${e.message}")
            }
            
            // Fallback: método original se a compressão falhar
            val cursor = requireContext().contentResolver.query(
                uri, arrayOf(MediaStore.Images.Media.DATA), null, null, null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndex(MediaStore.Images.Media.DATA)
                    if (columnIndex != -1) {
                        val path = it.getString(columnIndex)
                        Log.d("ExpenseRegisterFragment", "Caminho obtido via cursor: $path")
                        if (File(path).exists()) {
                            // ✅ CORREÇÃO: Tentar comprimir com fallback
                            try {
                                val compressedPathFromFile = imageCompressionUtils.compressImageFromPath(path)
                                if (compressedPathFromFile != null) {
                                    Log.d("ExpenseRegisterFragment", "Imagem comprimida do arquivo: $compressedPathFromFile")
                                    return compressedPathFromFile
                                }
                            } catch (e: Exception) {
                                Log.w("ExpenseRegisterFragment", "Compressão do arquivo falhou: ${e.message}")
                            }
                            return path
                        }
                    }
                }
            }
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val tempFile = File.createTempFile("comprovante_foto_", ".jpg", requireContext().cacheDir)
                tempFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                Log.d("ExpenseRegisterFragment", "Arquivo temporário criado: ${tempFile.absolutePath}")
                
                // ✅ CORREÇÃO: Tentar comprimir com fallback
                try {
                    val compressedPath = imageCompressionUtils.compressImageFromPath(tempFile.absolutePath)
                    if (compressedPath != null) {
                        Log.d("ExpenseRegisterFragment", "Arquivo temporário comprimido: $compressedPath")
                        return compressedPath
                    }
                } catch (e: Exception) {
                    Log.w("ExpenseRegisterFragment", "Compressão do arquivo temporário falhou: ${e.message}")
                }
                
                return tempFile.absolutePath
            }
            uri.toString()
        } catch (e: Exception) {
            Log.e("ExpenseRegisterFragment", "Erro ao obter caminho real: ${e.message}", e)
            null
        }
    }
    
    private fun mostrarFotoComprovante(caminhoFoto: String, dataFoto: Date?) {
        try {
            val file = File(caminhoFoto)
            if (file.exists()) {
                val bitmap = android.graphics.BitmapFactory.decodeFile(caminhoFoto)
                binding.ivFotoComprovante.setImageBitmap(bitmap)
                
                // Formatar e exibir data da foto
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
                val dataFormatada = dataFoto?.let { dateFormat.format(it) } ?: "Data não disponível"
                binding.tvDataFotoComprovante.text = "Data: $dataFormatada"
                
                // Mostrar layout da foto
                binding.layoutFotoComprovante.visibility = View.VISIBLE
                
                Log.d("ExpenseRegisterFragment", "✅ Foto do comprovante exibida com sucesso: $caminhoFoto")
            } else {
                Log.e("ExpenseRegisterFragment", "❌ Arquivo de foto não existe: $caminhoFoto")
                Toast.makeText(requireContext(), "Arquivo de foto não encontrado", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("ExpenseRegisterFragment", "Erro ao mostrar foto: ${e.message}", e)
            Toast.makeText(requireContext(), "Erro ao exibir foto: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun removerFotoComprovante() {
        try {
            // Limpar dados da foto
            fotoComprovantePath = null
            dataFotoComprovante = null
            currentPhotoUri = null
            
            // Limpar UI
            binding.ivFotoComprovante.setImageDrawable(null)
            binding.tvDataFotoComprovante.text = "Data: --/--/---- --:--"
            binding.layoutFotoComprovante.visibility = View.GONE
            
            Log.d("ExpenseRegisterFragment", "✅ Foto do comprovante removida com sucesso")
            Toast.makeText(requireContext(), "Foto do comprovante removida", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("ExpenseRegisterFragment", "Erro ao remover foto: ${e.message}", e)
            Toast.makeText(requireContext(), "Erro ao remover foto: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun mostrarDialogoExplicacaoPermissao() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Permissão Necessária")
            .setMessage("Para capturar fotos do comprovante, é necessário permitir o acesso à câmera. Por favor, conceda a permissão nas configurações do aplicativo.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 






