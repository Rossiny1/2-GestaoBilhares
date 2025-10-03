package com.example.gestaobilhares.ui.clients

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.gestaobilhares.databinding.FragmentClientRegisterBinding
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.repository.ClienteRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment para cadastro de novos clientes
 * FASE 4B - Recursos Avançados
 */
class ClientRegisterFragment : Fragment() {

    private var _binding: FragmentClientRegisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ClientRegisterViewModel
    private val args: ClientRegisterFragmentArgs by navArgs()
    
    // ✅ NOVO: Variáveis para Geolocalização
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    private var currentAccuracy: Float? = null
    private var locationCaptureTime: Date? = null
    
    // Launcher para solicitar permissões de localização
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocationGranted || coarseLocationGranted) {
            capturarLocalizacao()
        } else {
            mostrarDialogoPermissaoNegada()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClientRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Inicializar ViewModel aqui onde o contexto está disponível
        val database = AppDatabase.getDatabase(requireContext())
        val appRepository = com.example.gestaobilhares.data.repository.AppRepository(
            database.clienteDao(),
            database.acertoDao(),
            database.mesaDao(),
            database.rotaDao(),
            database.despesaDao(),
            database.colaboradorDao(),
            database.cicloAcertoDao(),
            database.acertoMesaDao(),
            database.contratoLocacaoDao(),
            database.aditivoContratoDao(),
            database.assinaturaRepresentanteLegalDao(),
            database.logAuditoriaAssinaturaDao(),
            database.procuraçãoRepresentanteDao()
        )
        viewModel = ClientRegisterViewModel(ClienteRepository(database.clienteDao(), appRepository))
        
        // ✅ NOVO: Inicializar cliente de localização
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        
        setupUI()
        observeViewModel()
        
        // Carregar dados do cliente se estiver editando
        if (args.clienteId != 0L) {
            Log.d("ClientRegisterFragment", "Modo EDIÇÃO - Carregando cliente ID: ${args.clienteId}")
            carregarDadosClienteParaEdicao()
        } else {
            Log.d("ClientRegisterFragment", "Modo NOVO CADASTRO")
        }
    }

    private fun setupUI() {
        // Botão voltar
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        
        // Botão cancelar
        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }
        
        // Botão salvar
        binding.btnSave.setOnClickListener {
            saveClient()
        }
        
        // ✅ NOVO: End icon do campo endereço aciona captura de localização
        binding.tilAddress.setEndIconOnClickListener {
            solicitarLocalizacao()
        }

        // ✅ NOVO: Definir data de cadastro atual
        val dataAtual = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("pt", "BR"))
            .format(java.util.Date())
        binding.etClienteDesde.setText(dataAtual)

        // ✅ NOVO: Configurar dropdowns de estado e cidade
        setupEstadoCidadeDropdowns()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.novoClienteId.collect { clienteId ->
                if (clienteId != null && clienteId > 0) {
                    try {
                        if (requireActivity().isFinishing || requireActivity().isDestroyed) {
                            return@collect
                        }
                        
                        if (!isAdded) {
                            return@collect
                        }
                        
                        // Ocultar loading
                        binding.progressBar.visibility = View.GONE
                        binding.btnSave.isEnabled = true
                        
                        if (parentFragmentManager.isStateSaved) {
                            return@collect
                        }
                        
                        // Mostrar dialog de sucesso para NOVO cadastro
                        if (isAdded && !requireActivity().isFinishing) {
                            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setTitle("✅ Cliente Cadastrado!")
                                .setMessage("Cliente cadastrado com sucesso!\n\nPróximo passo: Vincular mesas ao cliente.")
                                .setPositiveButton("Adicionar Mesa") { _, _ ->
                                    val bundle = Bundle().apply {
                                        putLong("clienteId", clienteId)
                                        putBoolean("isFromClientRegister", true)
                                        putBoolean("isFromGerenciarMesas", false)
                                    }
                                    findNavController().navigate(com.example.gestaobilhares.R.id.mesasDepositoFragment, bundle)
                                    viewModel.resetNovoClienteId()
                                }
                                .setNegativeButton("Voltar") { _, _ ->
                                    findNavController().popBackStack()
                                    viewModel.resetNovoClienteId()
                                }
                                .show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Usar try-catch para evitar crash
                        try {
                            showErrorDialog("Erro ao exibir confirmação: ${e.localizedMessage}")
                        } catch (dialogException: Exception) {
                            // Se não conseguir mostrar o dialog, pelo menos logar o erro
                            android.util.Log.e("ClientRegister", "Erro ao mostrar dialog: ${dialogException.message}")
                        }
                    }
                }
            }
        }
        
        // ✅ NOVO: Observer para cliente atualizado (edição)
        lifecycleScope.launch {
            viewModel.clienteAtualizado.collect { atualizado ->
                if (atualizado) {
                    try {
                        if (requireActivity().isFinishing || requireActivity().isDestroyed) {
                            return@collect
                        }
                        
                        if (!isAdded) {
                            return@collect
                        }
                        
                        // Ocultar loading
                        binding.progressBar.visibility = View.GONE
                        binding.btnSave.isEnabled = true
                        
                        if (parentFragmentManager.isStateSaved) {
                            return@collect
                        }
                        
                        // Mostrar dialog de sucesso para EDIÇÃO
                        if (isAdded && !requireActivity().isFinishing) {
                            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setTitle("✅ Cliente Atualizado!")
                                .setMessage("Dados do cliente foram atualizados com sucesso!")
                                .setPositiveButton("OK") { _, _ ->
                                    findNavController().popBackStack()
                                    viewModel.resetNovoClienteId()
                                }
                                .show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        try {
                            showErrorDialog("Erro ao exibir confirmação: ${e.localizedMessage}")
                        } catch (dialogException: Exception) {
                            android.util.Log.e("ClientRegister", "Erro ao mostrar dialog: ${dialogException.message}")
                        }
                    }
                }
            }
        }
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.btnSave.isEnabled = !isLoading
            }
        }
        lifecycleScope.launch {
            viewModel.debitoAtual.collect { debito ->
                try {
                    binding.etDebitoAtual.setText(String.format("R$ %.2f", debito))
                } catch (e: Exception) {
                    // Ignorar erro se a view foi destruída
                }
            }
        }
    }

    private fun showErrorDialog(message: String) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Erro")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun saveClient() {
        try {
            android.util.Log.d("ClientRegister", "Iniciando salvamento do cliente")
            
            // Limpar erros anteriores
            clearErrors()
            
            // Obter valores dos campos
            val name = binding.etClientName.text.toString().trim()
            val cpfCnpj = binding.etCpfCnpj.text.toString().trim()
            val address = binding.etAddress.text.toString().trim()
            val bairro = binding.etBairro.text.toString().trim()
            val cidade = binding.actvCidade.text.toString().trim()
            val estado = binding.actvEstado.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val phone2 = binding.etPhone2.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val valorFicha = binding.etValorFicha.text.toString().trim().toDoubleOrNull() ?: 0.0
            val comissaoFicha = binding.etComissaoFicha.text.toString().trim().toDoubleOrNull() ?: 0.0
            val numeroContrato = binding.etNumeroContrato.text.toString().trim()
            val observations = binding.etObservations.text.toString().trim()
            
            android.util.Log.d("ClientRegister", "Valores obtidos: nome=$name, endereco=$address")
            
            // Validações
            if (name.isEmpty()) {
                binding.etClientName.error = "Nome é obrigatório"
                binding.etClientName.requestFocus()
                return
            }
            
            if (name.length < 3) {
                binding.etClientName.error = "Nome deve ter pelo menos 3 caracteres"
                binding.etClientName.requestFocus()
                return
            }
            
            if (address.isEmpty()) {
                binding.etAddress.error = "Endereço é obrigatório"
                binding.etAddress.requestFocus()
                return
            }
            
            if (address.length < 10) {
                binding.etAddress.error = "Endereço deve ser mais detalhado"
                binding.etAddress.requestFocus()
                return
            }

            // ✅ NOVA VALIDAÇÃO: Comissão por ficha é obrigatória
            if (comissaoFicha <= 0) {
                binding.etComissaoFicha.error = "Comissão por ficha é obrigatória"
                binding.etComissaoFicha.requestFocus()
                return
            }
            
            // Validação opcional de email
            if (email.isNotEmpty() && !isValidEmail(email)) {
                binding.etEmail.error = "E-mail inválido"
                binding.etEmail.requestFocus()
                return
            }
            
            // Validação opcional de telefone
            if (phone.isNotEmpty() && phone.length < 10) {
                binding.etPhone.error = "Telefone inválido"
                binding.etPhone.requestFocus()
                return
            }
            
            android.util.Log.d("ClientRegister", "Validações passaram, criando entidade Cliente")
            
            // Mostrar loading
            binding.progressBar.visibility = View.VISIBLE
            binding.btnSave.isEnabled = false
            
            // Criar entidade Cliente
            val cliente = com.example.gestaobilhares.data.entities.Cliente(
                nome = name,
                cpfCnpj = if (cpfCnpj.isNotEmpty()) cpfCnpj else null,
                endereco = address,
                bairro = if (bairro.isNotEmpty()) bairro else null,
                cidade = if (cidade.isNotEmpty()) cidade else null,
                estado = if (estado.isNotEmpty()) estado else null,
                telefone = if (phone.isNotEmpty()) phone else null,
                telefone2 = if (phone2.isNotEmpty()) phone2 else null,
                email = if (email.isNotEmpty()) email else null,
                // ✅ NOVO: Dados de geolocalização
                latitude = currentLatitude,
                longitude = currentLongitude,
                precisaoGps = currentAccuracy,
                dataCapturaGps = locationCaptureTime,
                valorFicha = valorFicha,
                comissaoFicha = comissaoFicha,
                numeroContrato = if (numeroContrato.isNotEmpty()) numeroContrato else null,
                observacoes = if (observations.isNotEmpty()) observations else null,
                rotaId = if (args.rotaId != 0L) args.rotaId else {
                    // ✅ CORREÇÃO: Em modo edição, usar rotaId do cliente existente
                    viewModel.clienteParaEdicao.value?.rotaId ?: 1L
                }
            )
            
            android.util.Log.d("ClientRegister", "Entidade Cliente criada, chamando viewModel.cadastrarCliente")
            viewModel.cadastrarCliente(cliente)
            
        } catch (e: Exception) {
            android.util.Log.e("ClientRegister", "Erro ao salvar cliente: ${e.message}", e)
            // Tratar erro e restaurar UI
            binding.progressBar.visibility = View.GONE
            binding.btnSave.isEnabled = true
            showErrorDialog("Erro ao salvar cliente: ${e.localizedMessage}")
        }
    }
    
    private fun clearErrors() {
        binding.etClientName.error = null
        binding.etCpfCnpj.error = null
        binding.etAddress.error = null
        binding.etBairro.error = null
        binding.actvCidade.error = null
        binding.actvEstado.error = null
        binding.etPhone.error = null
        binding.etPhone2.error = null
        binding.etEmail.error = null
        binding.etValorFicha.error = null
        binding.etComissaoFicha.error = null
        binding.etNumeroContrato.error = null
        binding.etObservations.error = null
    }
    
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun carregarDebitoAtual() {
        // TODO: Implementar busca do débito atual do último acerto
        // Por enquanto, mostrar 0,00
        viewModel.carregarDebitoAtual(null)
    }

    /**
     * ✅ IMPLEMENTADO: Carrega dados do cliente para edição
     */
    private fun carregarDadosClienteParaEdicao() {
        viewModel.carregarClienteParaEdicao(args.clienteId)
        
        // Observer para preencher campos quando dados carregarem
        lifecycleScope.launch {
            viewModel.clienteParaEdicao.collect { cliente ->
                cliente?.let { preencherCamposComDadosCliente(it) }
            }
        }
    }
    
    /**
     * ✅ IMPLEMENTADO: Preenche campos com dados do cliente
     */
    private fun preencherCamposComDadosCliente(cliente: com.example.gestaobilhares.data.entities.Cliente) {
        binding.apply {
            etClientName.setText(cliente.nome)
            etCpfCnpj.setText(cliente.cpfCnpj ?: "")
            etAddress.setText(cliente.endereco ?: "")
            etBairro.setText(cliente.bairro ?: "")
            actvCidade.setText(cliente.cidade ?: "")
            actvEstado.setText(cliente.estado ?: "")
            etPhone.setText(cliente.telefone ?: "")
            etPhone2.setText(cliente.telefone2 ?: "")
            etEmail.setText(cliente.email ?: "")
            etValorFicha.setText(cliente.valorFicha.toString())
            etComissaoFicha.setText(cliente.comissaoFicha.toString())
            etNumeroContrato.setText(cliente.numeroContrato ?: "")
            etObservations.setText(cliente.observacoes ?: "")
            
            // ✅ NOVO: Preencher data de cadastro
            val dataCadastro = if (cliente.dataCadastro != null) {
                java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("pt", "BR"))
                    .format(cliente.dataCadastro)
            } else {
                java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("pt", "BR"))
                    .format(java.util.Date())
            }
            etClienteDesde.setText(dataCadastro)
            
            // ✅ NOVO: Carregar dados de geolocalização se existirem
            if (cliente.latitude != null && cliente.longitude != null) {
                currentLatitude = cliente.latitude
                currentLongitude = cliente.longitude
                currentAccuracy = cliente.precisaoGps
                locationCaptureTime = cliente.dataCapturaGps
                
                // Simular localização para atualizar UI
                val mockLocation = android.location.Location("saved").apply {
                    latitude = cliente.latitude
                    longitude = cliente.longitude
                    if (cliente.precisaoGps != null) {
                        accuracy = cliente.precisaoGps
                    }
                }
                atualizarUILocalizacao(mockLocation)
            }
            
            // Alterar título para modo edição
            // TODO: Adicionar TextView para título no layout se não existir
        }
    }

    /**
     * ✅ NOVO: Configura os dropdowns de estado e cidade
     */
    private fun setupEstadoCidadeDropdowns() {
        try {
            // Carregar dados dos estados e cidades
            val estadosCidades = com.example.gestaobilhares.data.model.EstadosCidades.carregarDados(requireContext())
            
            // Configurar adapter para estados
            val estados = estadosCidades.estados.map { it.nome }.toTypedArray()
            val estadosAdapter = android.widget.ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                estados
            )
            binding.actvEstado.setAdapter(estadosAdapter)
            
            // Listener para atualizar cidades quando estado for selecionado
            binding.actvEstado.setOnItemClickListener { _, _, position, _ ->
                val estadoSelecionado = estadosCidades.estados[position]
                
                // Criar adapter com filtro para as cidades
                val cidadesAdapter = android.widget.ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    estadoSelecionado.cidades.sorted() // Ordenar alfabeticamente
                )
                
                binding.actvCidade.setText("") // Limpar cidade atual
                binding.actvCidade.setAdapter(cidadesAdapter)
                
                // Configurar comportamento de digitação
                binding.actvCidade.threshold = 1 // Mostrar sugestões a partir do primeiro caractere
                binding.actvCidade.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        binding.actvCidade.showDropDown()
                    }
                }
                
                Log.d("ClientRegisterFragment", "Estado selecionado: ${estadoSelecionado.nome}, ${estadoSelecionado.cidades.size} cidades carregadas")
            }
            
            // Configurar adapter inicial para cidades (vazio)
            binding.actvCidade.setAdapter(android.widget.ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                arrayOf<String>()
            ))
            
            // Configurar comportamento do campo de cidade
            binding.actvCidade.setOnClickListener {
                if (binding.actvEstado.text.toString().isNotEmpty()) {
                    binding.actvCidade.showDropDown()
                } else {
                    showErrorDialog("Selecione primeiro um estado")
                }
            }
            
        } catch (e: Exception) {
            Log.e("ClientRegisterFragment", "Erro ao configurar dropdowns: ${e.message}")
            showErrorDialog("Erro ao carregar estados e cidades: ${e.message}")
        }
    }
    
    // ✅ NOVO: Funções de Geolocalização
    
    /**
     * Solicita permissões de localização e inicia captura de GPS
     */
    private fun solicitarLocalizacao() {
        when {
            // Verificar se já temos permissões
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                capturarLocalizacao()
            }
            
            // Mostrar explicação se necessário
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                mostrarDialogoExplicacaoPermissao()
            }
            
            // Solicitar permissões
            else -> {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }
    
    /**
     * Captura a localização atual do dispositivo
     */
    private fun capturarLocalizacao() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        
        // Mostrar loading leve via Toast
        Toast.makeText(requireContext(), "Capturando localização...", Toast.LENGTH_SHORT).show()
        
        try {
            val cancellationToken = CancellationTokenSource()
            
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationToken.token
            ).addOnSuccessListener { location: Location? ->
                if (location != null) {
                    // Salvar coordenadas
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude
                    currentAccuracy = location.accuracy
                    locationCaptureTime = Date()
                    
                    // Atualizar UI
                    // Feedback de sucesso
                    Toast.makeText(requireContext(), "Localização realizada com sucesso", Toast.LENGTH_LONG).show()
                    
                    Log.d("Geolocation", "Localização capturada: ${location.latitude}, ${location.longitude}")
                } else {
                    // Feedback de erro
                    Toast.makeText(
                        requireContext(),
                        "Não foi possível obter sua localização. Verifique se o GPS está ativado.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }.addOnFailureListener { exception ->
                Log.e("Geolocation", "Erro ao capturar localização", exception)
                Toast.makeText(
                    requireContext(),
                    "Erro ao capturar localização: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            Log.e("Geolocation", "Erro inesperado", e)
            Toast.makeText(
                requireContext(),
                "Erro inesperado: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    /**
     * Atualiza a interface com os dados da localização capturada
     */
    private fun atualizarUILocalizacao(location: Location) {
        // Método mantido apenas para futura UI; agora feedback é via Toast e campos internos
    }
    
    /**
     * Mostra diálogo explicando por que precisamos da permissão
     */
    private fun mostrarDialogoExplicacaoPermissao() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Permissão de Localização")
            .setMessage("Para cadastrar a localização exata onde a mesa será instalada, precisamos acessar o GPS do seu dispositivo.\n\nIsso nos ajuda a:")
            .setMessage("• Localizar rapidamente o cliente\n• Otimizar rotas de coleta\n• Melhorar o atendimento\n\nSua localização será usada apenas para este cadastro.")
            .setPositiveButton("Permitir") { _, _ ->
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
            .setNegativeButton("Agora não", null)
            .show()
    }
    
    /**
     * Mostra diálogo quando a permissão é negada
     */
    private fun mostrarDialogoPermissaoNegada() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Permissão Necessária")
            .setMessage("Para capturar a localização, é necessário permitir o acesso ao GPS.\n\nVocê pode ativar nas configurações do aplicativo.")
            .setPositiveButton("Configurações") { _, _ ->
                // Aqui poderia abrir as configurações do app
                Toast.makeText(
                    requireContext(),
                    "Ative a permissão de localização nas configurações",
                    Toast.LENGTH_LONG
                ).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 