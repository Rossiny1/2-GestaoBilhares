package com.example.gestaobilhares.ui.settlement

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.gestaobilhares.R
import com.example.gestaobilhares.databinding.FragmentSettlementBinding
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.factory.RepositoryFactory
import com.example.gestaobilhares.BuildConfig
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.data.entities.Acerto
import com.example.gestaobilhares.data.entities.PanoEstoque
import com.example.gestaobilhares.data.entities.StatusAcerto
import com.example.gestaobilhares.ui.settlement.PanoSelectionDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.Toast
import com.example.gestaobilhares.data.entities.Mesa
import android.util.Log
import com.example.gestaobilhares.ui.settlement.MesaDTO
import com.example.gestaobilhares.ui.settlement.MesasAcertoAdapter
import com.example.gestaobilhares.ui.clients.AcertoResumo
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.gestaobilhares.utils.ImageCompressionUtils
import com.example.gestaobilhares.utils.FirebaseImageUploader
import com.example.gestaobilhares.utils.NetworkUtils
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

/**
 * Fragment para registrar novos acertos
 * FASE 4A - Implementação crítica do core business
 */
class SettlementFragment : Fragment() {

    private var _binding: FragmentSettlementBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SettlementViewModel
    private lateinit var appRepository: com.example.gestaobilhares.data.repository.AppRepository
    private val args: SettlementFragmentArgs by navArgs()
    
    private val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private lateinit var mesasAcertoAdapter: MesasAcertoAdapter
    private var paymentValues: MutableMap<String, Double> = mutableMapOf()
    // ✅ NOVO: Flag para controlar modo sem mesas (apenas pagamento de débito)
    private var isDebtOnlyMode: Boolean = false
    // ✅ NOVO: Flag para indicar se houve troca de pano neste acerto
    private var houveTrocaPanoNoAcerto: Boolean = false
    
    // ✅ CORREÇÃO: Inicialização segura do ImageCompressionUtils
    private val imageCompressionUtils: ImageCompressionUtils by lazy {
        ImageCompressionUtils(requireContext())
    }
    
    // ✅ NOVO: Utilitário para upload de imagens ao Firebase Storage
    private val firebaseImageUploader: FirebaseImageUploader by lazy {
        FirebaseImageUploader(requireContext())
    }
    
    // ✅ NOVO: Utilitário para verificar conectividade
    private val networkUtils: NetworkUtils by lazy {
        NetworkUtils(requireContext())
    }
    
    // ✅ NOVO: Variáveis para captura de foto
    private var currentPhotoUri: Uri? = null
    private var currentMesaId: Long = 0L
    
    // ✅ NOVO: Launcher para captura de foto
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            abrirCamera()
        } else {
            Toast.makeText(requireContext(), "Permissão de câmera necessária para capturar foto", Toast.LENGTH_LONG).show()
        }
    }
    
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // ✅ CORREÇÃO: Proteção contra crash após captura de foto
            try {
                currentPhotoUri?.let { uri ->
                    Log.d("SettlementFragment", "Foto capturada com sucesso: $uri")
                    
                    // ✅ CORREÇÃO: Usar post para aguardar o layout ser concluído
                    binding.root.post {
                        try {
                            // ✅ CORREÇÃO MELHORADA: Verificar se o arquivo existe e obter caminho real
                            val caminhoReal = obterCaminhoRealFoto(uri)
                            if (caminhoReal != null) {
                                Log.d("SettlementFragment", "Caminho real da foto: $caminhoReal")
                                
                                // ✅ CORREÇÃO: Exibir foto localmente imediatamente
                                mesasAcertoAdapter.setFotoRelogio(currentMesaId, caminhoReal)
                                Toast.makeText(requireContext(), "Foto do relógio capturada com sucesso!", Toast.LENGTH_SHORT).show()
                                
                                // ✅ CORREÇÃO: Fazer upload da foto para Firebase Storage em background
                                lifecycleScope.launch {
                                    try {
                                        // Se não é URL do Firebase Storage, fazer upload
                                        if (!firebaseImageUploader.isFirebaseStorageUrl(caminhoReal)) {
                                            if (networkUtils.isConnected()) {
                                                Log.d("SettlementFragment", "Fazendo upload da foto para Firebase Storage...")
                                                val uploadedUrl = firebaseImageUploader.uploadMesaRelogio(caminhoReal, currentMesaId)
                                                if (uploadedUrl != null) {
                                                    // Salvar URL Firebase separadamente para sincronização
                                                    mesasAcertoAdapter.setFotoRelogioFirebaseUrl(currentMesaId, uploadedUrl)
                                                    Log.d("SettlementFragment", "✅ Foto enviada para Firebase Storage: $uploadedUrl")
                                                } else {
                                                    Log.w("SettlementFragment", "⚠️ Falha no upload, foto será sincronizada depois")
                                                }
                                            } else {
                                                Log.d("SettlementFragment", "📴 Sem conexão, foto será sincronizada depois")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("SettlementFragment", "Erro ao fazer upload da foto: ${e.message}", e)
                                        // Foto local já está sendo exibida, upload pode ser feito depois
                                    }
                                }
                            } else {
                                Log.e("SettlementFragment", "Não foi possível obter o caminho real da foto")
                                Toast.makeText(requireContext(), "Erro: não foi possível salvar a foto", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Log.e("SettlementFragment", "Erro ao processar foto: ${e.message}", e)
                            Toast.makeText(requireContext(), "Erro ao processar foto: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SettlementFragment", "Erro crítico após captura de foto: ${e.message}", e)
                Toast.makeText(requireContext(), "Erro ao processar foto capturada", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(requireContext(), "Erro ao capturar foto", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * ✅ NOVO: Método para obter o caminho real da foto
     */
    private fun obterCaminhoRealFoto(uri: Uri): String? {
        return try {
            Log.d("SettlementFragment", "Obtendo caminho real para URI: $uri")
            
            // ✅ CORREÇÃO: Tentar comprimir a imagem com fallback seguro
            try {
                val compressedPath = imageCompressionUtils.compressImageFromUri(uri)
                if (compressedPath != null) {
                    Log.d("SettlementFragment", "Imagem comprimida com sucesso: $compressedPath")
                    return compressedPath
                }
            } catch (e: Exception) {
                Log.w("SettlementFragment", "Compressão falhou, usando método original: ${e.message}")
            }
            
            // Tentativa 1: Converter URI para caminho real via ContentResolver
            val cursor = requireContext().contentResolver.query(
                uri, 
                arrayOf(android.provider.MediaStore.Images.Media.DATA), 
                null, 
                null, 
                null
            )
            
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndex(android.provider.MediaStore.Images.Media.DATA)
                    if (columnIndex != -1) {
                        val path = it.getString(columnIndex)
                        Log.d("SettlementFragment", "Caminho obtido via cursor: $path")
                        if (java.io.File(path).exists()) {
                            // ✅ CORREÇÃO: Tentar comprimir com fallback
                            try {
                                val compressedPathFromFile = imageCompressionUtils.compressImageFromPath(path)
                                if (compressedPathFromFile != null) {
                                    Log.d("SettlementFragment", "Imagem comprimida do arquivo: $compressedPathFromFile")
                                    return compressedPathFromFile
                                }
                            } catch (e: Exception) {
                                Log.w("SettlementFragment", "Compressão do arquivo falhou: ${e.message}")
                            }
                            return path
                        }
                    }
                }
            }
            
            // Tentativa 2: Se não conseguiu via cursor, tentar copiar para arquivo temporário
            Log.d("SettlementFragment", "Tentando copiar para arquivo temporário")
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val tempFile = java.io.File.createTempFile("relogio_foto_", ".jpg", requireContext().cacheDir)
                tempFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                Log.d("SettlementFragment", "Arquivo temporário criado: ${tempFile.absolutePath}")
                
                // ✅ CORREÇÃO: Tentar comprimir com fallback
                try {
                    val compressedPath = imageCompressionUtils.compressImageFromPath(tempFile.absolutePath)
                    if (compressedPath != null) {
                        Log.d("SettlementFragment", "Arquivo temporário comprimido: $compressedPath")
                        return compressedPath
                    }
                } catch (e: Exception) {
                    Log.w("SettlementFragment", "Compressão do arquivo temporário falhou: ${e.message}")
                }
                
                return tempFile.absolutePath
            }
            
            // Tentativa 3: Se ainda não conseguiu, usar o URI como string
            Log.d("SettlementFragment", "Usando URI como string: $uri")
            uri.toString()
            
        } catch (e: Exception) {
            Log.e("SettlementFragment", "Erro ao obter caminho real: ${e.message}", e)
            null
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettlementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // ✅ LOG CRASH: Início da tela
        Log.d("LOG_CRASH", "SettlementFragment.onViewCreated - INÍCIO")
        
        // Inicializar ViewModel aqui onde o contexto está disponível
        appRepository = RepositoryFactory.getAppRepository(requireContext())
        viewModel = SettlementViewModel(appRepository)
        
        Log.d("SettlementFragment", "=== INICIANDO SETTLEMENT FRAGMENT ===")
        Log.d("SettlementFragment", "Cliente ID: ${args.clienteId}")
        Log.d("SettlementFragment", "Acerto ID para edição: ${args.acertoIdParaEdicao}")
        Log.d("SettlementFragment", "Modo edição: ${args.acertoIdParaEdicao != 0L}")
        
        // Primeiro: verificar permissões
        verificarPermissaoAcerto()
        
        // Segundo: configurar observers
        observeViewModel()
        
        // Terceiro: carregar dados do cliente PRIMEIRO (crítico para comissão)
        carregarDadosClienteESincronizar()
        
        // Quarto: configurar UI básica
        configurarUIBasica()
        
                // Sexto: carregar dados básicos do cliente para header
        viewModel.loadClientForSettlement(args.clienteId)
        
        // ✅ NOVO: Sétimo: carregar dados do acerto se estiver editando
        if (args.acertoIdParaEdicao != 0L) {
            Log.d("SettlementFragment", "🔧 CHAMANDO carregarDadosAcertoParaEdicao() - Acerto ID: ${args.acertoIdParaEdicao}")
            carregarDadosAcertoParaEdicao()
        } else {
            Log.d("SettlementFragment", "🔧 NÃO chamando carregarDadosAcertoParaEdicao() - Acerto ID é 0")
            // ✅ CORREÇÃO: Buscar débito anterior apenas para novos acertos
            viewModel.buscarDebitoAnterior(args.clienteId, null)
        }
    }

    /**
     * ✅ NOVO: Carrega dados do acerto existente para preencher campos na edição
     */
    private fun carregarDadosAcertoParaEdicao() {
        lifecycleScope.launch {
            try {
                Log.d("SettlementFragment", "=== CARREGANDO DADOS DO ACERTO PARA EDIÇÃO ===")
                Log.d("SettlementFragment", "Acerto ID: ${args.acertoIdParaEdicao}")
                
                // ✅ TESTE DIRETO: Buscar acerto diretamente do AppRepository
                Log.d("SettlementFragment", "🔍 TESTE DIRETO: Buscando acerto diretamente do AppRepository...")
                val acertoDireto = appRepository.buscarPorId(args.acertoIdParaEdicao)
                if (acertoDireto != null) {
                    Log.d("SettlementFragment", "✅ TESTE DIRETO: Acerto encontrado diretamente:")
                    Log.d("SettlementFragment", "  - ID: ${acertoDireto.id}")
                    Log.d("SettlementFragment", "  - Débito Anterior: ${acertoDireto.debitoAnterior}")
                    Log.d("SettlementFragment", "  - Débito Atual: ${acertoDireto.debitoAtual}")
                    
                    // ✅ TESTE DIRETO: Definir débito anterior diretamente
                    Log.d("SettlementFragment", "🔍 TESTE DIRETO: Definindo débito anterior diretamente...")
                    viewModel.definirDebitoAnteriorParaEdicao(acertoDireto.debitoAnterior)
                } else {
                    Log.e("SettlementFragment", "❌ TESTE DIRETO: Acerto não encontrado diretamente")
                }
                
                // ✅ CORREÇÃO: Primeiro buscar o débito anterior para edição
                Log.d("SettlementFragment", "🔍 Buscando débito anterior para edição...")
                viewModel.buscarDebitoAnterior(args.clienteId, args.acertoIdParaEdicao)
                
                // Buscar dados do acerto
                val acerto = viewModel.buscarAcertoPorId(args.acertoIdParaEdicao)
                if (acerto != null) {
                    Log.d("SettlementFragment", "Acerto encontrado: ID=${acerto.id}, Valor=${acerto.valorRecebido}")
                    Log.d("SettlementFragment", "🔍 DEBUG ACERTO COMPLETO:")
                    Log.d("SettlementFragment", "  - ID: ${acerto.id}")
                    Log.d("SettlementFragment", "  - Valor Recebido: ${acerto.valorRecebido}")
                    Log.d("SettlementFragment", "  - Desconto: ${acerto.desconto}")
                    Log.d("SettlementFragment", "  - Observações: '${acerto.observacoes}'")
                    Log.d("SettlementFragment", "  - Data: ${acerto.dataAcerto}")
                    Log.d("SettlementFragment", "  - Status: ${acerto.status}")
                    Log.d("SettlementFragment", "  - Débito Anterior: ${acerto.debitoAnterior}")

                    // ✅ VALIDAÇÃO: Verificar se o acerto pode ser editado
                    Log.d("SettlementFragment", "🔍 VALIDAÇÃO: Verificando status do acerto...")
                    Log.d("SettlementFragment", "🔍 Status atual: ${acerto.status}")
                    
                    // ✅ CORREÇÃO: Remover bloqueio de acertos FINALIZADOS
                    // A validação de edição agora é feita pelo AcertoRepository baseada no status do ciclo
                    Log.d("SettlementFragment", "✅ Acerto pode ser editado (Status: ${acerto.status})")

                    // Preencher campos da UI com dados do acerto
                    preencherCamposComDadosAcerto(acerto)
                } else {
                    Log.e("SettlementFragment", "Acerto não encontrado: ${args.acertoIdParaEdicao}")
                }
            } catch (e: Exception) {
                Log.e("SettlementFragment", "Erro ao carregar dados do acerto: ${e.message}", e)
            }
        }
    }
    
    /**
     * ✅ NOVO: Preenche campos da UI com dados do acerto existente
     */
    private fun preencherCamposComDadosAcerto(acerto: com.example.gestaobilhares.data.entities.Acerto) {
        try {
            Log.d("SettlementFragment", "=== PREENCHENDO CAMPOS COM DADOS DO ACERTO ===")
            Log.d("SettlementFragment", "Valor recebido: ${acerto.valorRecebido}")
            Log.d("SettlementFragment", "Desconto: ${acerto.desconto}")
            Log.d("SettlementFragment", "Observações: ${acerto.observacoes}")
            Log.d("SettlementFragment", "Débito anterior: ${acerto.debitoAnterior}")

            // ✅ CORREÇÃO: Débito anterior já foi carregado pelo buscarDebitoAnterior()
            Log.d("SettlementFragment", "🔍 Débito anterior já carregado pelo ViewModel: ${acerto.debitoAnterior}")

            // Preencher valor recebido (sempre, mesmo se for 0)
            Log.d("SettlementFragment", "🔍 Preenchendo valor recebido: ${acerto.valorRecebido}")
            binding.etAmountReceived.setText(acerto.valorRecebido.toString())
            Log.d("SettlementFragment", "✅ Valor recebido preenchido: ${acerto.valorRecebido}")

            // Preencher desconto (sempre, mesmo se for 0)
            Log.d("SettlementFragment", "🔍 Preenchendo desconto: ${acerto.desconto}")
            binding.etDesconto.setText(acerto.desconto.toString())
            Log.d("SettlementFragment", "✅ Desconto preenchido: ${acerto.desconto}")

            // Preencher observações (sempre, mesmo se for vazio)
            Log.d("SettlementFragment", "🔍 Preenchendo observações: '${acerto.observacoes}'")
            binding.etObservacao.setText(acerto.observacoes ?: "")
            Log.d("SettlementFragment", "✅ Observações preenchidas: '${acerto.observacoes ?: ""}'")

            // ✅ NOVO: Preencher relógio final das mesas se houver dados de mesas
            // Aguardar um pouco para garantir que as mesas já foram carregadas no adapter
            Handler(Looper.getMainLooper()).postDelayed({
                Log.d("SettlementFragment", "🔧 Executando preenchimento tardio do relógio final...")
                preencherRelogioFinalMesas(acerto.id)
            }, 1000)

            // Preencher métodos de pagamento (se houver)
            // TODO: Implementar preenchimento dos métodos de pagamento

            Log.d("SettlementFragment", "✅ Campos preenchidos com sucesso!")

        } catch (e: Exception) {
            Log.e("SettlementFragment", "Erro ao preencher campos: ${e.message}", e)
        }
    }

    /**
     * ✅ NOVO: Preenche relógio final das mesas com dados do acerto
     */
    private fun preencherRelogioFinalMesas(acertoId: Long) {
        lifecycleScope.launch {
            try {
                Log.d("SettlementFragment", "🔍 Preenchendo relógio final das mesas para acerto: $acertoId")
                
                // Buscar dados das mesas do acerto
                val acertoMesas = viewModel.buscarAcertoMesasPorAcertoId(acertoId)
                
                Log.d("SettlementFragment", "🔍 AcertoMesas encontradas: ${acertoMesas.size}")
                acertoMesas.forEach { acertoMesa ->
                    Log.d("SettlementFragment", "🔍 AcertoMesa: mesaId=${acertoMesa.mesaId}, relogioInicial=${acertoMesa.relogioInicial}, relogioFinal=${acertoMesa.relogioFinal}")
                }
                
                if (acertoMesas.isNotEmpty()) {
                    Log.d("SettlementFragment", "✅ Encontradas ${acertoMesas.size} mesas para preenchimento")
                    
                    // Verificar se o adapter já foi inicializado
                    if (::mesasAcertoAdapter.isInitialized) {
                        Log.d("SettlementFragment", "✅ Adapter inicializado, atualizando relógio final...")
                        // Atualizar o adapter com os dados das mesas
                        mesasAcertoAdapter.atualizarRelogioFinalMesas(acertoMesas)
                        Log.d("SettlementFragment", "✅ Relógio final das mesas preenchido com sucesso")
                    } else {
                        Log.w("SettlementFragment", "⚠️ Adapter não inicializado ainda")
                    }
                } else {
                    Log.w("SettlementFragment", "⚠️ Nenhuma mesa encontrada para o acerto $acertoId")
                }
                
            } catch (e: Exception) {
                Log.e("SettlementFragment", "Erro ao preencher relógio final das mesas: ${e.message}", e)
            }
        }
    }

    private fun verificarPermissaoAcerto() {
        // TODO: Implementar verificação de status da rota
        // Por enquanto, sempre permitir (será integrado com ClientListViewModel)
        val podeAcertar = true // viewModel.podeRealizarAcerto()
        
        if (!podeAcertar) {
            mostrarAlertaRotaNaoIniciada()
            return
        }
    }

    private fun mostrarAlertaRotaNaoIniciada() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Acerto Não Permitido")
            .setMessage("Para realizar acertos, a rota deve estar com status 'Em Andamento'. Inicie a rota primeiro na tela de clientes.")
            .setPositiveButton("Entendi") { _, _ ->
                findNavController().popBackStack()
            }
            .setCancelable(false)
            .show()
    }

    private fun carregarDadosClienteESincronizar() {
        Log.d("SettlementFragment", "Iniciando carregamento sincronizado dos dados do cliente")
        
        viewModel.carregarDadosCliente(args.clienteId) { cliente ->
            if (cliente != null) {
                Log.d("SettlementFragment", "✅ Cliente carregado: valorFicha=${cliente.valorFicha}, comissaoFicha=${cliente.comissaoFicha}")
                
                // Agora que temos os dados do cliente, preparar as mesas
                lifecycleScope.launch {
                    try {
                        // Carregar mesas do cliente através do ViewModel
                        viewModel.loadMesasCliente(args.clienteId)
                        
                        // ✅ CORREÇÃO: Usar timeout para evitar "job was canceled"
                        val mesasCliente = withTimeoutOrNull(5000) {
                            viewModel.mesasCliente.first { it.isNotEmpty() }
                        }
                        
                        if (mesasCliente != null && mesasCliente.isNotEmpty()) {
                            Log.d("SettlementFragment", "✅ Mesas do cliente carregadas: ${mesasCliente.size}")
                    
                            // ✅ CORREÇÃO: Preparar mesas para acerto com modo de edição
                            val mesasPreparadas = viewModel.prepararMesasParaAcerto(
                                mesasCliente, 
                                args.acertoIdParaEdicao.takeIf { it != 0L }
                            )
                            
                            // Converter para DTO com dados do cliente já carregados
                            val mesasDTO = mesasPreparadas.map { mesa ->
                                MesaDTO(
                                    id = mesa.id,
                                    numero = mesa.numero,
                                    relogioInicial = mesa.relogioInicial,
                                    relogioFinal = mesa.relogioFinal,
                                    tipoMesa = mesa.tipoMesa,
                                    tamanho = mesa.tamanho,
                                    estadoConservacao = mesa.estadoConservacao,
                                    valorFixo = mesa.valorFixo,
                                    valorFicha = cliente.valorFicha,  // ✅ Dados do cliente
                                    comissaoFicha = cliente.comissaoFicha,  // ✅ Dados do cliente
                                    ativa = mesa.ativa
                                )
                            }
                            
                            Log.d("SettlementFragment", "MesasDTO criadas com sucesso: ${mesasDTO.size}")
                            mesasDTO.forEach { mesa ->
                                Log.d("SettlementFragment", "Mesa ${mesa.numero}: valorFicha=${mesa.valorFicha}, comissaoFicha=${mesa.comissaoFicha}")
                            }
                            
                            // Configurar RecyclerView com dados completos
                            setupRecyclerViewComDados(mesasDTO)
                            
                        } else {
                            Log.w("SettlementFragment", "⚠️ Nenhuma mesa encontrada para o cliente.")
                            // Exceção: permitir acerto apenas para pagamento de débito se houver débito
                            val debitoAnterior = viewModel.debitoAnterior.value
                            if (debitoAnterior > 0.0) {
                                Log.i("SettlementFragment", "Modo pagamento de débito sem mesas. Débito anterior: R$ $debitoAnterior")
                                configurarModoPagamentoDebito()
                            } else {
                                Log.w("SettlementFragment", "Cliente sem mesas e sem débito. Encerrando tela de acerto.")
                                Toast.makeText(requireContext(), "Cliente sem mesas e sem débito.", Toast.LENGTH_LONG).show()
                                findNavController().popBackStack()
                            }
                        }
                        
                    } catch (e: Exception) {
                        Log.e("SettlementFragment", "❌ Erro ao carregar mesas: ${e.message}", e)
                        // Fallback em caso de erro
                        carregarMesasFallback(cliente)
                    }
                }
            } else {
                Log.e("SettlementFragment", "❌ Erro: Cliente não encontrado")
                Toast.makeText(requireContext(), "Erro: Cliente não encontrado", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    /**
     * ✅ FUNÇÃO FALLBACK: Carrega mesas quando o Flow falha
     */
    private suspend fun carregarMesasFallback(cliente: com.example.gestaobilhares.data.entities.Cliente) {
        try {
            Log.d("SettlementFragment", "🔄 Executando fallback para carregar mesas...")
            
            // Verificar se o fragment ainda está ativo
            if (!isAdded || context == null) {
                Log.w("SettlementFragment", "Fragment não está mais ativo, cancelando fallback")
                return
            }
            
            // Usar repositório diretamente através do ViewModel
            val mesasCliente = viewModel.carregarMesasClienteDireto(args.clienteId)
            
            if (mesasCliente.isNotEmpty()) {
                Log.d("SettlementFragment", "✅ Fallback: ${mesasCliente.size} mesas carregadas")
                
                // ✅ CORREÇÃO: Preparar mesas para acerto com modo de edição no fallback
                val mesasPreparadas = viewModel.prepararMesasParaAcerto(
                    mesasCliente,
                    args.acertoIdParaEdicao.takeIf { it != 0L }
                )
                
                val mesasDTO = mesasPreparadas.map { mesa ->
                    MesaDTO(
                        id = mesa.id,
                        numero = mesa.numero,
                        relogioInicial = mesa.relogioInicial,
                        relogioFinal = mesa.relogioFinal,
                        tipoMesa = mesa.tipoMesa,
                        tamanho = mesa.tamanho,
                        estadoConservacao = mesa.estadoConservacao,
                        valorFixo = mesa.valorFixo,
                        valorFicha = cliente.valorFicha,
                        comissaoFicha = cliente.comissaoFicha,
                        ativa = mesa.ativa
                    )
                }
                
                setupRecyclerViewComDados(mesasDTO)
            } else {
                Log.w("SettlementFragment", "Fallback: Nenhuma mesa encontrada")
                val debitoAnterior = viewModel.debitoAnterior.value
                if (debitoAnterior > 0.0) {
                    Log.i("SettlementFragment", "Fallback -> Modo pagamento de débito sem mesas. Débito: R$ $debitoAnterior")
                    configurarModoPagamentoDebito()
                } else {
                    Toast.makeText(requireContext(), "Cliente sem mesas e sem débito.", Toast.LENGTH_LONG).show()
                    findNavController().popBackStack()
                }
            }
        } catch (e: Exception) {
            Log.e("SettlementFragment", "❌ Erro no fallback: ${e.message}", e)
            Toast.makeText(requireContext(), "Erro ao carregar dados: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * ✅ NOVO: Configura a tela para o modo "Pagamento de Débito" sem mesas
     * - Esconde RecyclerView de mesas
     * - Zera subtotal de mesas
     * - Mantém métodos de pagamento e desconto para quitar parcial ou totalmente o débito
     */
    private fun configurarModoPagamentoDebito() {
        try {
            isDebtOnlyMode = true
            // Esconder lista de mesas
            binding.rvMesasAcerto.visibility = View.GONE
            // Zerar totais de mesas
            binding.tvTableTotal.text = formatter.format(0.0)
            // Forçar recálculo considerando apenas débito anterior, desconto e pagamentos
            updateCalculations()
            showSnackbar("Modo pagamento de débito habilitado (sem mesas)")
        } catch (e: Exception) {
            Log.e("SettlementFragment", "Erro ao configurar modo pagamento de débito: ${e.message}")
        }
    }
    
    private fun configurarUIBasica() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.btnSaveSettlement.setOnClickListener {
            salvarAcertoComCamposExtras()
        }
        setupPaymentMethod()
        setupCalculationListeners()
        preencherNomeRepresentante()
        // ✅ NOVO: Configurar lógica do pano
        setupPanoLogic()
        // ✅ Bloquear edição manual do campo Valor Recebido
        binding.etAmountReceived.isFocusable = false
        binding.etAmountReceived.isClickable = false
        binding.etAmountReceived.isLongClickable = false
        binding.etAmountReceived.keyListener = null
    }
    
    private fun setupRecyclerViewComDados(mesasDTO: List<MesaDTO>) {
        // ✅ LOG CRASH: Início da configuração do RecyclerView
        Log.d("LOG_CRASH", "SettlementFragment.setupRecyclerViewComDados - INÍCIO")
        
        // Verificar se o fragment ainda está ativo
        if (!isAdded || _binding == null) {
            Log.w("SettlementFragment", "Fragment não está mais ativo ou binding é null, cancelando setupRecyclerViewComDados")
            return
        }
        
        Log.d("SettlementFragment", "=== CONFIGURANDO RECYCLERVIEW COM DADOS COMPLETOS ===")
        Log.d("SettlementFragment", "Total de mesas recebidas: ${mesasDTO.size}")
        
        // ✅ DIAGNÓSTICO: Verificar cada mesa individualmente
        mesasDTO.forEachIndexed { index, mesa ->
            Log.d("SettlementFragment", "Mesa $index: ID=${mesa.id}, Número=${mesa.numero}, Tipo=${mesa.tipoMesa}, Ativa=${mesa.ativa}")
        }
        
        // ✅ LOG CRASH: Configurando adapter
        Log.d("LOG_CRASH", "SettlementFragment.setupRecyclerViewComDados - Configurando adapter")
        
        mesasAcertoAdapter = MesasAcertoAdapter(
            onDataChanged = { 
                Log.d("LOG_CRASH", "SettlementFragment.setupRecyclerViewComDados - Dados alterados, atualizando cálculos")
                updateCalculations() 
            },
            onCalcularMedia = { mesaId -> 
                // ✅ LOG CRASH: Solicitação de cálculo de média
                Log.d("LOG_CRASH", "SettlementFragment.setupRecyclerViewComDados - Solicitando cálculo de média para mesa $mesaId")
                // ✅ NOVO: Calcular média de fichas jogadas dos últimos acertos
                Log.d("SettlementFragment", "Solicitando cálculo de média para mesa $mesaId")
                
                // Iniciar cálculo assíncrono
                lifecycleScope.launch {
                    try {
                        val media = viewModel.calcularMediaFichasJogadas(mesaId, 5)
                        Log.d("SettlementFragment", "Média calculada para mesa $mesaId: $media fichas")
                        
                        // Atualizar o adapter com a média calculada
                        mesasAcertoAdapter.atualizarMediaMesa(mesaId, media)
                        
                        // Recalcular totais após atualizar a média
                        updateCalculations()
                        
                        // Mostrar feedback visual
                        showSnackbar("Média calculada: ${media.toInt()} fichas")
                    } catch (e: Exception) {
                        Log.e("SettlementFragment", "Erro ao calcular média: ${e.message}", e)
                        showSnackbar("Erro ao calcular média: ${e.message}")
                    }
                }
                
                // Retornar 0 temporariamente - será atualizado pelo cálculo assíncrono
                0.0
            },
            onFotoCapturada = { mesaId, caminhoFoto, _ ->
                // ✅ NOVO: Callback quando foto é capturada
                Log.d("SettlementFragment", "Foto capturada para mesa $mesaId: $caminhoFoto")
                // Aqui você pode fazer qualquer processamento adicional se necessário
            },
            onSolicitarCapturaFoto = { mesaId ->
                solicitarCapturaFoto(mesaId)
            }
        )
        
        // ✅ LOG CRASH: Configurando RecyclerView
        Log.d("LOG_CRASH", "SettlementFragment.setupRecyclerViewComDados - Configurando RecyclerView")
        
        binding.rvMesasAcerto.adapter = mesasAcertoAdapter
        binding.rvMesasAcerto.layoutManager = LinearLayoutManager(requireContext())
        
        Log.d("LOG_CRASH", "SettlementFragment.setupRecyclerViewComDados - RecyclerView configurado com sucesso")
        
        // ✅ CORREÇÃO: Usar as mesas preparadas com relógio inicial correto
        Log.d("SettlementFragment", "Carregando ${mesasDTO.size} mesas preparadas para o acerto")
        mesasDTO.forEach { mesa ->
            Log.d("SettlementFragment", "Mesa ${mesa.numero}: relógio inicial=${mesa.relogioInicial}, relógio final=${mesa.relogioFinal}")
            Log.d("SettlementFragment", "🔍 DEBUG MESA ${mesa.numero}:")
            Log.d("SettlementFragment", "  - ID: ${mesa.id}")
            Log.d("SettlementFragment", "  - Número: ${mesa.numero}")
            Log.d("SettlementFragment", "  - Relógio Inicial: ${mesa.relogioInicial}")
            Log.d("SettlementFragment", "  - Relógio Final: ${mesa.relogioFinal}")
            Log.d("SettlementFragment", "  - Valor Fixo: ${mesa.valorFixo}")
        }
        
        // ✅ DIAGNÓSTICO: Verificar se o adapter está sendo configurado corretamente
        Log.d("SettlementFragment", "Adapter configurado: ${mesasAcertoAdapter.itemCount} itens")
        Log.d("SettlementFragment", "LayoutManager configurado: ${binding.rvMesasAcerto.layoutManager}")
        
        // ✅ LOG CRASH: Submetendo lista de mesas
        Log.d("LOG_CRASH", "SettlementFragment.setupRecyclerViewComDados - Submetendo lista de ${mesasDTO.size} mesas")
        
        mesasAcertoAdapter.submitList(mesasDTO)
        
        Log.d("LOG_CRASH", "SettlementFragment.setupRecyclerViewComDados - Lista submetida com sucesso")
        
        // ✅ DIAGNÓSTICO: Verificar após submitList
        Log.d("SettlementFragment", "Após submitList: ${mesasAcertoAdapter.itemCount} itens no adapter")
        Log.d("SettlementFragment", "RecyclerView visível: ${binding.rvMesasAcerto.visibility}")
        Log.d("SettlementFragment", "RecyclerView altura: ${binding.rvMesasAcerto.height}")
        
        // ✅ NOVO: Forçar atualização do RecyclerView
        binding.rvMesasAcerto.post {
            Log.d("SettlementFragment", "Post executado - RecyclerView atualizado")
            Log.d("SettlementFragment", "ItemCount após post: ${mesasAcertoAdapter.itemCount}")
            binding.rvMesasAcerto.invalidate()
            // Atualizar o card com as últimas trocas agora que o adapter está pronto
            carregarUltimasTrocasTodasMesas()
        }
    }
    
    private fun preencherNomeRepresentante() {
        try {
            // ✅ CORREÇÃO: Usar UserSessionManager em vez de SharedPreferences direto
            val userSessionManager = com.example.gestaobilhares.utils.UserSessionManager.getInstance(requireContext())
            val nomeUsuario = userSessionManager.getCurrentUserName()
            
            if (nomeUsuario.isNotEmpty()) {
                binding.tvRepresentante.text = nomeUsuario
                Log.d("SettlementFragment", "✅ Nome do representante preenchido via UserSessionManager: $nomeUsuario")
            } else {
                // Fallback: tentar obter do Firebase Auth
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val nomeFirebase = firebaseUser?.displayName
                
                if (!nomeFirebase.isNullOrEmpty()) {
                    binding.tvRepresentante.text = nomeFirebase
                    Log.d("SettlementFragment", "Nome do representante obtido do Firebase: $nomeFirebase")
                } else {
                    // Último fallback: nome padrão
                    binding.tvRepresentante.text = "Usuário Logado"
                    Log.d("SettlementFragment", "Usando nome padrão para representante")
                }
            }
        } catch (e: Exception) {
            Log.e("SettlementFragment", "Erro ao obter nome do representante: ${e.message}")
            binding.tvRepresentante.text = "Usuário Logado"
        }
    }
    
    private fun setupCalculationListeners() {
        // ✅ CORREÇÃO CRÍTICA: Listener para desconto
        val descontoWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateCalculations()
            }
        }
        
        binding.etDesconto.addTextChangedListener(descontoWatcher)
        
        // ✅ CORREÇÃO CRÍTICA: Listener específico para o campo Valor Recebido
        val valorRecebidoWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateCalculations()
            }
        }
        
        // Adicionar listener ao campo Valor Recebido
        binding.etAmountReceived.addTextChangedListener(valorRecebidoWatcher)
        
        Log.d("SettlementFragment", "✅ Listeners de cálculo configurados - débito atual será atualizado em tempo real")
    }
    
    private fun updateCalculations() {
        try {
            val desconto = binding.etDesconto.text.toString().toDoubleOrNull() ?: 0.0
            val subtotalMesas = if (::mesasAcertoAdapter.isInitialized) mesasAcertoAdapter.getSubtotal() else 0.0
            val debitoAnterior = viewModel.debitoAnterior.value
            val totalComDebito = subtotalMesas + debitoAnterior
            val totalComDesconto = maxOf(0.0, totalComDebito - desconto)
            val valorRecebidoDosMetodos = paymentValues.values.sum()
            val debitoAtualCalculado = debitoAnterior + subtotalMesas - desconto - valorRecebidoDosMetodos

            binding.tvTableTotal.text = formatter.format(subtotalMesas)
            binding.tvTotalWithDebt.text = formatter.format(totalComDesconto)
            binding.tvCurrentDebt.text = formatter.format(debitoAtualCalculado)
        } catch (e: Exception) {
            Log.e("UpdateCalculations", "Erro ao calcular totais", e)
            binding.tvTableTotal.text = formatter.format(0.0)
            binding.tvTotalWithDebt.text = formatter.format(0.0)
        }
    }



    /**
     * Força a atualização dos cálculos com validação extra
     */
    private fun forceUpdateCalculations() {
        try {
            Log.d("SettlementFragment", "🔄 FORÇANDO RECÁLCULO DOS TOTAIS")
            
            // Validar se o adapter está pronto
            if (!::mesasAcertoAdapter.isInitialized) {
                Log.w("SettlementFragment", "⚠️ Adapter ainda não inicializado")
                return
            }
            
            // Verificar se o valor recebido está sincronizado com paymentValues
            val somaPaymentValues = paymentValues.values.sum()
            val valorRecebidoAtual = binding.etAmountReceived.text.toString().toDoubleOrNull() ?: 0.0
            
            if (Math.abs(somaPaymentValues - valorRecebidoAtual) > 0.01) {
                Log.w("SettlementFragment", "⚠️ INCONSISTÊNCIA DETECTADA:")
                Log.w("SettlementFragment", "Soma paymentValues: R$ $somaPaymentValues")
                Log.w("SettlementFragment", "Valor no campo: R$ $valorRecebidoAtual")
                
                // Forçar sincronização
                binding.etAmountReceived.setText(String.format("%.2f", somaPaymentValues))
                Log.d("SettlementFragment", "✅ Campo sincronizado com paymentValues")
            }
            
            // Chamar updateCalculations normal
            updateCalculations()
            
        } catch (e: Exception) {
            Log.e("SettlementFragment", "❌ Erro ao forçar recálculo", e)
            // Fallback para updateCalculations normal
            updateCalculations()
        }
    }
    
    private fun setupPaymentMethod() {
        val paymentMethods = arrayOf("Dinheiro", "PIX", "Cartão Débito", "Cartão Crédito", "Cheque")
        binding.actvPaymentMethod.keyListener = null // Impede digitação manual
        binding.actvPaymentMethod.setOnClickListener {
            showPaymentMethodsDialog(paymentMethods)
        }
    }
    
    /**
     * ✅ NOVO: Mostra um Snackbar com feedback para o usuário
     */
    private fun showSnackbar(message: String) {
        com.google.android.material.snackbar.Snackbar.make(
            binding.root,
            message,
            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun showPaymentMethodsDialog(paymentMethods: Array<String>) {
        val checkedItems = BooleanArray(paymentMethods.size) { false }
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Selecione os métodos de pagamento")
            .setMultiChoiceItems(paymentMethods, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("OK") { _, _ ->
                val selected = paymentMethods.filterIndexed { idx, _ -> checkedItems[idx] }
                if (selected.isNotEmpty()) {
                    // SEMPRE mostrar diálogo de valores, mesmo para um método
                    showPaymentValuesDialog(selected)
                } else {
                    paymentValues.clear()
                    binding.actvPaymentMethod.setText("", false)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showPaymentValuesDialog(selected: List<String>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_payment_values, null)
        val containerInputs = dialogView.findViewById<LinearLayout>(R.id.containerPaymentInputs)
        val tvTotalInformado = dialogView.findViewById<TextView>(R.id.tvTotalInformado)
        val tvDialogSubtitle = dialogView.findViewById<TextView>(R.id.tvDialogSubtitle)
        
        // Atualizar subtitle baseado na quantidade de métodos
        tvDialogSubtitle.text = if (selected.size == 1) {
            "Informe o valor recebido em ${selected[0]}"
        } else {
            "Informe o valor recebido em cada método de pagamento"
        }
        
        val paymentInputs = mutableMapOf<String, com.google.android.material.textfield.TextInputEditText>()
        val moneyWatchers = mutableMapOf<String, com.example.gestaobilhares.utils.MoneyTextWatcher>()
        
        // Criar inputs para cada método de pagamento
        selected.forEach { metodo ->
            val itemView = layoutInflater.inflate(R.layout.item_payment_method_input, containerInputs, false)
            val tvMethodName = itemView.findViewById<TextView>(R.id.tvPaymentMethodName)
            val etPaymentValue = itemView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPaymentValue)
            
            // Configurar nome do método
            tvMethodName.text = metodo
            
            // Configurar formatação monetária
            val moneyWatcher = com.example.gestaobilhares.utils.MoneyTextWatcher(etPaymentValue)
            etPaymentValue.addTextChangedListener(moneyWatcher)
            
            // Pré-preencher com valor existente se houver
            val valorExistente = paymentValues[metodo]
            if (valorExistente != null && valorExistente > 0) {
                moneyWatcher.setValue(valorExistente)
            }
            
            // Listener para atualizar total em tempo real
            etPaymentValue.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    updateTotalDisplay(paymentInputs, moneyWatchers, tvTotalInformado)
                }
            })
            
            paymentInputs[metodo] = etPaymentValue
            moneyWatchers[metodo] = moneyWatcher
            containerInputs.addView(itemView)
        }
        
        // Atualizar total inicial
        updateTotalDisplay(paymentInputs, moneyWatchers, tvTotalInformado)
        
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("✅ Confirmar") { _, _ ->
                Log.d("SettlementFragment", "=== PROCESSANDO MÉTODOS DE PAGAMENTO ===")
                
                paymentValues.clear()
                var totalInformado = 0.0
                var valoresValidos = true
                
                selected.forEach { metodo ->
                    val valor = moneyWatchers[metodo]?.getValue() ?: 0.0
                    
                    Log.d("SettlementFragment", "Método: $metodo -> Valor: R$ $valor")
                    
                    if (valor < 0) {
                        Log.w("SettlementFragment", "⚠️ Valor negativo detectado para $metodo: R$ $valor")
                        valoresValidos = false
                    }
                    
                    paymentValues[metodo] = valor
                    totalInformado += valor
                }
                
                if (!valoresValidos) {
                    Log.w("SettlementFragment", "⚠️ Alguns valores são inválidos")
                }
                
                Log.d("SettlementFragment", "Total informado: R$ $totalInformado")
                
                // Atualizar texto do campo de método de pagamento
                val resumo = if (selected.size == 1) {
                    selected[0]
                } else {
                    paymentValues.entries.joinToString(", ") { "${it.key}: R$ %.2f".format(it.value) }
                }
                binding.actvPaymentMethod.setText(resumo, false)
                
                // Atualiza o campo Valor Recebido com a soma
                binding.etAmountReceived.setText(String.format("%.2f", totalInformado))
                
                Log.d("SettlementFragment", "Campo Valor Recebido atualizado para: '${binding.etAmountReceived.text}'")
                
                // ✅ CORREÇÃO: Forçar recálculo imediato após atualizar métodos de pagamento
                updateCalculations()
                
                // ✅ CORREÇÃO: Forçar recálculo com post para garantir que UI foi atualizada
                binding.etAmountReceived.post {
                    Log.d("SettlementFragment", "Executando recálculo após update UI")
                    // Forçar recálculo imediato
                    forceUpdateCalculations()
                }
                
                Log.d("SettlementFragment", "✅ Métodos de pagamento processados - Total: R$ $totalInformado")
            }
            .setNegativeButton("❌ Cancelar", null)
            .show()
    }
    
    /**
     * Atualiza o display do total em tempo real no diálogo de métodos de pagamento
     */
    private fun updateTotalDisplay(
        @Suppress("UNUSED_PARAMETER") paymentInputs: Map<String, com.google.android.material.textfield.TextInputEditText>,
        moneyWatchers: Map<String, com.example.gestaobilhares.utils.MoneyTextWatcher>,
        tvTotalInformado: TextView
    ) {
        try {
            val total = moneyWatchers.values.sumOf { it.getValue() }
            tvTotalInformado.text = com.example.gestaobilhares.utils.MoneyTextWatcher.formatValue(total)
        } catch (e: Exception) {
            Log.e("SettlementFragment", "Erro ao atualizar total: ${e.message}")
            tvTotalInformado.text = "R$ 0,00"
        }
    }

    private fun salvarAcertoComCamposExtras() {
        // ✅ LOG CRASH: Início do salvamento do acerto
        Log.d("LOG_CRASH", "SettlementFragment.salvarAcertoComCamposExtras - INÍCIO")
        
        // Impedir múltiplos cliques
        if (viewModel.isLoading.value) {
            Log.d("LOG_CRASH", "SettlementFragment.salvarAcertoComCamposExtras - Já está salvando, ignorando clique adicional")
            Log.d("SettlementFragment", "Já está salvando, ignorando clique adicional")
            return
        }
        
        // ✅ LOG CRASH: Validando dados
        Log.d("LOG_CRASH", "SettlementFragment.salvarAcertoComCamposExtras - Validando dados")
        
        // ✅ CORREÇÃO: Validar dados ANTES de desabilitar o botão
        if (!isDebtOnlyMode) {
            Log.d("LOG_CRASH", "SettlementFragment.salvarAcertoComCamposExtras - Modo normal (não débito)")
            if (!::mesasAcertoAdapter.isInitialized || !mesasAcertoAdapter.isDataValid()) {
                val errorMessage = if (::mesasAcertoAdapter.isInitialized) mesasAcertoAdapter.getValidationErrorMessage() else "Dados de mesas não disponíveis"
                Log.e("LOG_CRASH", "SettlementFragment.salvarAcertoComCamposExtras - ERRO de validação: $errorMessage")
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                return
            }
        }
        
        // Desabilitar botão apenas após validação bem-sucedida
        binding.btnSaveSettlement.isEnabled = false
        viewModel.setLoading(true)

        // ✅ CORREÇÃO: valorRecebido não é usado - o valor é calculado a partir dos paymentValues no ViewModel
        val desconto = binding.etDesconto.text.toString().toDoubleOrNull() ?: 0.0
        val observacao = binding.etObservacao.text.toString().trim()
        // Removido: funcionalidade de pano movida para sistema de troca separado
        val numeroPano = null // Não mais usado no acerto principal
        val tipoAcerto = binding.spTipoAcerto.selectedItem.toString()
        val representante = binding.tvRepresentante.text.toString()

        // ✅ CORREÇÃO: Logs detalhados para debug das observações
        Log.d("SettlementFragment", "=== SALVANDO ACERTO - DEBUG OBSERVAÇÕES ===")
        Log.d("SettlementFragment", "Campo observação (RAW): '${binding.etObservacao.text}'")
        Log.d("SettlementFragment", "Campo observação (TRIM): '$observacao'")
        // ✅ CORREÇÃO: observacao é String (não nullable), verificação == null sempre false - removida
        Log.d("SettlementFragment", "Observação é vazia? ${observacao.isEmpty()}")
        Log.d("SettlementFragment", "Observação é blank? ${observacao.isBlank()}")
        Log.d("SettlementFragment", "Tamanho da observação: ${observacao.length}")
        
        // ✅ CORREÇÃO: Observação será apenas manual, sem preenchimento automático
        val observacaoFinal = observacao.trim()
        Log.d("SettlementFragment", "Observação final que será salva: '$observacaoFinal'")

        // ✅ CORREÇÃO CRÍTICA: Usar dados do adapter como fonte única e confiável quando houver mesas
        val mesasDoAcerto = if (!isDebtOnlyMode && ::mesasAcertoAdapter.isInitialized) {
            mesasAcertoAdapter.getMesasAcerto().mapIndexed { idx, mesaState ->
                // Buscar a mesa original no adapter para obter dados completos
                val mesaOriginal = mesasAcertoAdapter.currentList.find { it.id == mesaState.mesaId }
                
                Log.d("SettlementFragment", "=== MONTANDO MESA PARA SALVAR ===")
                Log.d("SettlementFragment", "Mesa ${idx + 1}: ID=${mesaState.mesaId}")
                Log.d("SettlementFragment", "Relógio inicial: ${mesaState.relogioInicial}")
                Log.d("SettlementFragment", "Relógio final: ${mesaState.relogioFinal}")
                Log.d("SettlementFragment", "Valor fixo (mesa original): ${mesaOriginal?.valorFixo ?: 0.0}")
                Log.d("SettlementFragment", "Com defeito: ${mesaState.comDefeito}")
                Log.d("SettlementFragment", "Relógio reiniciou: ${mesaState.relogioReiniciou}")
                
                SettlementViewModel.MesaAcerto(
                    id = mesaState.mesaId,
                    numero = mesaOriginal?.numero ?: (idx + 1).toString(),
                    relogioInicial = mesaState.relogioInicial,
                    relogioFinal = mesaState.relogioFinal,
                    valorFixo = mesaOriginal?.valorFixo ?: 0.0,
                    tipoMesa = com.example.gestaobilhares.data.entities.TipoMesa.SINUCA,
                    comDefeito = mesaState.comDefeito,
                    relogioReiniciou = mesaState.relogioReiniciou,
                    // ✅ NOVO: Incluir dados de foto
                    fotoRelogioFinal = mesaState.fotoRelogioFinal,
                    dataFoto = mesaState.dataFoto
                )
            }
        } else {
            emptyList()
        }
        
        Log.d("SettlementFragment", "=== LISTA DE MESAS PARA SALVAR ===")
        Log.d("SettlementFragment", "Total de mesas: ${mesasDoAcerto.size}")
        mesasDoAcerto.forEachIndexed { index, mesa ->
            Log.d("SettlementFragment", "Mesa ${index + 1}: ${mesa.numero} - Valor fixo: R$ ${mesa.valorFixo}")
        }

        val dadosAcerto = SettlementViewModel.DadosAcerto(
            mesas = mesasDoAcerto,
            representante = representante,
            panoTrocado = houveTrocaPanoNoAcerto,
            numeroPano = numeroPano,
            tipoAcerto = tipoAcerto,
            observacao = observacaoFinal, // ✅ CORREÇÃO: Usar observação final
            justificativa = null,
            metodosPagamento = paymentValues
        )

        // ✅ LOG CRASH: Chamando ViewModel para salvar
        Log.d("LOG_CRASH", "SettlementFragment.salvarAcertoComCamposExtras - Chamando ViewModel para salvar")
        
        Log.d("SettlementFragment", "Iniciando salvamento do acerto...")
        Log.d("SettlementFragment", "Desconto aplicado: R$ $desconto")
        Log.d("SettlementFragment", "Observação enviada para ViewModel: '$observacaoFinal'")
        Log.d("SettlementFragment", "Tipo de acerto: $tipoAcerto")
        
        try {
            viewModel.salvarAcerto(
                clienteId = args.clienteId,
                dadosAcerto = dadosAcerto,
                metodosPagamento = paymentValues,
                desconto = desconto,
                acertoIdParaEdicao = args.acertoIdParaEdicao.takeIf { it != 0L }
            )
            
            Log.d("LOG_CRASH", "SettlementFragment.salvarAcertoComCamposExtras - ViewModel chamado com sucesso")
        } catch (e: Exception) {
            Log.e("LOG_CRASH", "SettlementFragment.salvarAcertoComCamposExtras - ERRO ao chamar ViewModel: ${e.message}", e)
            Toast.makeText(requireContext(), "Erro ao salvar acerto: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun observeViewModel() {
        // Observer para dados do cliente
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.clientName.collect { nome ->
                binding.tvClientName.text = nome
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.clientAddress.collect { endereco ->
                binding.tvClientAddress.text = endereco
            }
        }
        
                // Observer para débito anterior
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.debitoAnterior.collect { debito ->
                val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
                binding.tvPreviousDebt.text = formatter.format(debito)
            }
        }

        // ✅ REMOVIDO: Observer do débito atual do banco (não é necessário)
        // O débito atual será calculado em tempo real na função updateCalculations()

        
        
        // Observer para resultado do salvamento - CRÍTICO PARA O DIÁLOGO
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.resultadoSalvamento.collect { resultado ->
                // ✅ CORREÇÃO: Sempre reabilitar o botão, independente do resultado
                binding.btnSaveSettlement.isEnabled = true
                viewModel.setLoading(false)
                
                resultado?.let {
                    when (it) {
                        is SettlementViewModel.ResultadoSalvamento.Sucesso -> {
                            Log.d("SettlementFragment", "✅ Acerto salvo com sucesso! ID: ${it.acertoId}")
                            
                            // NOVO: Notificar ClientListFragment para atualizar card de progresso
                            findNavController().previousBackStackEntry?.savedStateHandle?.set("acerto_salvo", true)
                            
                            mostrarDialogoResumoComAcerto(it.acertoId)
                        }
                        
                        is SettlementViewModel.ResultadoSalvamento.Erro -> {
                            Log.e("SettlementFragment", "Erro ao salvar acerto: ${it.mensagem}")
                            Toast.makeText(requireContext(), "Erro ao salvar acerto: ${it.mensagem}", Toast.LENGTH_LONG).show()
                        }
                        
                        is SettlementViewModel.ResultadoSalvamento.AcertoJaExiste -> {
                            Log.w("SettlementFragment", "⚠️ Acerto já existe: ID ${it.acertoExistente.id}")
                            mostrarDialogoAcertoJaExiste(it.acertoExistente)
                        }
                    }
                }
            }
        }
    }

    /**
     * ✅ NOVA FUNCIONALIDADE: Mostra diálogo quando já existe acerto no ciclo atual
     */
    private fun mostrarDialogoAcertoJaExiste(acertoExistente: Acerto) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Acerto Já Realizado")
            .setMessage(
                "Este cliente já possui um acerto salvo neste ciclo.\n\n" +
                "📋 Detalhes do acerto existente:\n" +
                "• ID: #${acertoExistente.id.toString().padStart(4, '0')}\n" +
                "• Valor recebido: R$ ${String.format("%.2f", acertoExistente.valorRecebido)}\n" +
                "• Data: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(acertoExistente.dataAcerto)}\n\n" +
                "💡 Para alterar este acerto, vá até o histórico do cliente e selecione o último acerto."
            )
            .setPositiveButton("Ver Histórico") { _, _ ->
                // Voltar para a tela de detalhes do cliente para ver o histórico
                findNavController().popBackStack()
            }
            .setNegativeButton("Entendi", null)
            .setCancelable(false)
            .show()
    }

    private fun mostrarDialogoResumoComAcerto(acertoId: Long) {
        lifecycleScope.launch {
            val acerto = viewModel.buscarAcertoPorId(acertoId)
            if (acerto != null) {
                val mesas = viewModel.buscarMesasDoAcerto(acerto.id)
                val metodosPagamento: Map<String, Double> = acerto.metodosPagamentoJson?.let {
                    Gson().fromJson(it, object : TypeToken<Map<String, Double>>() {}.type)
                } ?: emptyMap()
                
                // ✅ CORREÇÃO: Obter números reais das mesas com tipoMesa correto
                val mesasComNumerosReais = mesas.map { mesaAcerto ->
                    val mesaReal = viewModel.buscarMesaPorId(mesaAcerto.mesaId)
                    Mesa(
                        id = mesaAcerto.mesaId,
                        numero = mesaReal?.numero ?: mesaAcerto.mesaId.toString(),
                        relogioInicial = mesaAcerto.relogioInicial,
                        relogioFinal = mesaAcerto.relogioFinal,
                        valorFixo = mesaAcerto.valorFixo,
                        tipoMesa = mesaReal?.tipoMesa ?: com.example.gestaobilhares.data.entities.TipoMesa.SINUCA // ✅ CORREÇÃO: Usar tipoMesa real
                    )
                }
                
                // ✅ NOVO: Obter dados adicionais para o resumo
                val debitoAnterior = viewModel.debitoAnterior.value
                val desconto = binding.etDesconto.text.toString().toDoubleOrNull() ?: 0.0
                
                // ✅ CORREÇÃO: Usar valor total das mesas do banco de dados
                val valorTotalMesas = acerto.valorTotal
                
                // ✅ NOVO: Carregar dados do cliente para obter o telefone e buscar contrato ativo
                viewModel.carregarDadosCliente(args.clienteId) { cliente ->
                    // Buscar contrato ativo do cliente
                    lifecycleScope.launch {
                        val contratoAtivo = viewModel.buscarContratoAtivoPorCliente(args.clienteId)
                        val numeroContrato = contratoAtivo?.numeroContrato
                        
                        val dialog = SettlementSummaryDialog.newInstance(
                            clienteNome = viewModel.clientName.value,
                            clienteTelefone = cliente?.telefone,
                            clienteCpf = cliente?.cpfCnpj,
                            mesas = mesasComNumerosReais,
                            total = acerto.valorTotal,
                            metodosPagamento = metodosPagamento,
                            observacao = acerto.observacoes,
                            debitoAtual = acerto.debitoAtual,
                            debitoAnterior = debitoAnterior,
                            desconto = desconto,
                            valorTotalMesas = valorTotalMesas,
                            valorFicha = cliente?.valorFicha ?: 0.0,
                            comissaoFicha = cliente?.comissaoFicha ?: 0.0,
                            acertoId = acertoId,
                            numeroContrato = numeroContrato
                        )
                        dialog.acertoCompartilhadoListener = object : SettlementSummaryDialog.OnAcertoCompartilhadoListener {
                            override fun onAcertoCompartilhado() {
                                // ✅ CORREÇÃO: Notificar ClientDetailFragment via cache seguro
                                val sharedPref = requireActivity().getSharedPreferences("acerto_temp", android.content.Context.MODE_PRIVATE)
                                with(sharedPref.edit()) {
                                    putLong("cliente_id", args.clienteId)
                                    putBoolean("acerto_salvo", true)
                                    putLong("novo_acerto_id", acertoId)
                                    apply()
                                }
                                // Voltar para tela Detalhes do Cliente
                                findNavController().popBackStack(R.id.clientDetailFragment, false)
                            }
                        }
                        // ✅ CORREÇÃO: Quando o diálogo é fechado (sem compartilhar), também voltar para ClientDetailFragment
                        dialog.dialog?.setOnDismissListener {
                            // Se o diálogo foi fechado sem compartilhar, voltar para ClientDetailFragment
                            if (findNavController().currentDestination?.id == R.id.settlementFragment) {
                                findNavController().popBackStack(R.id.clientDetailFragment, false)
                            }
                        }
                        dialog.show(parentFragmentManager, "SettlementSummaryDialog")
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Erro ao carregar acerto salvo", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ✅ REMOVIDO: Função duplicada mostrarDialogoResumo() 
    // Agora usa apenas mostrarDialogoResumoComAcerto() que pega dados reais do banco
    
    // ✅ NOVO: Métodos para captura de foto
    
    /**
     * Solicita captura de foto do relógio para uma mesa específica
     */
    fun solicitarCapturaFoto(mesaId: Long) {
        currentMesaId = mesaId
        Log.d("SettlementFragment", "Solicitando captura de foto para mesa ID: $mesaId")
        
        // Verificar permissão de câmera
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                abrirCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                mostrarDialogoExplicacaoPermissao()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    
    /**
     * Abre a câmera para capturar foto
     */
    private fun abrirCamera() {
        try {
            // Criar arquivo temporário para a foto
            val photoFile = criarArquivoFoto()
            currentPhotoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile
            )
            
            // Lançar intent da câmera
            cameraLauncher.launch(currentPhotoUri!!)
            
        } catch (e: Exception) {
            Log.e("SettlementFragment", "Erro ao abrir câmera: ${e.message}", e)
            Toast.makeText(requireContext(), "Erro ao abrir câmera: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Cria um arquivo temporário para a foto
     */
    private fun criarArquivoFoto(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "RELOGIOMESA_${currentMesaId}_${timeStamp}"
        val storageDir = requireContext().getExternalFilesDir(null)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }
    
    /**
     * Mostra diálogo explicando por que a permissão de câmera é necessária
     */
    private fun mostrarDialogoExplicacaoPermissao() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Permissão de Câmera")
            .setMessage("A permissão de câmera é necessária para capturar fotos do relógio final das mesas. Isso ajuda a documentar o estado do equipamento.")
            .setPositiveButton("Permitir") { _, _ ->
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * ✅ NOVO: Configura a lógica do pano
     */
    private fun setupPanoLogic() {
        // Carregar últimas trocas de todas as mesas
        carregarUltimasTrocasTodasMesas()
        
        // Configurar botão Trocar Pano
        binding.btnTrocarPano.setOnClickListener {
            // Se houver mais de uma mesa, primeiro selecionar a mesa
            val mesas = if (::mesasAcertoAdapter.isInitialized) mesasAcertoAdapter.currentList else emptyList()
            if (mesas.size > 1) {
                mostrarSelecaoMesaParaTrocaPano()
            } else if (mesas.size == 1) {
                mostrarSelecaoPano(mesas.first().id)
            } else {
                showSnackbar("Nenhuma mesa disponível")
            }
        }
    }
    
    /**
     * ✅ NOVO: Carrega o pano atual da mesa (simplificado)
     */
    private fun carregarPanoAtual() {
        // Função simplificada - agora usamos o sistema de cards dinâmicos
        android.util.Log.d("SettlementFragment", "carregarPanoAtual: funcionalidade movida para cards dinâmicos")
    }

    private fun carregarUltimasTrocasTodasMesas() {
        if (!::mesasAcertoAdapter.isInitialized) {
            android.util.Log.w("SettlementFragment", "carregarUltimasTrocasTodasMesas: adapter ainda não inicializado")
            mostrarMensagemAguarde()
            return
        }
        val mesas = mesasAcertoAdapter.currentList
        if (mesas.isEmpty()) {
            mostrarMensagemSemMesas()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Limpar container anterior
                binding.llUltimasTrocasPanos.removeAllViews()
                
                for (mesa in mesas) {
                    val pano = viewModel.carregarPanoAtualDaMesa(mesa.id)
                    val mesaFull = viewModel.buscarMesaPorId(mesa.id)
                    val numero = pano?.numero ?: "--"
                    val info = if (pano != null) "${pano.cor} - ${pano.tamanho}" else "Sem pano"
                    val dataStr = mesaFull?.dataUltimaTrocaPano?.let {
                        java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(it)
                    } ?: "N/A"
                    
                    // Criar card para cada mesa
                    val cardView = criarCardUltimaTroca(numero, mesa.numero, info, dataStr)
                    binding.llUltimasTrocasPanos.addView(cardView)
                }
                
                android.util.Log.d("SettlementFragment", "Histórico de panos carregado: ${mesas.size} mesas")
            } catch (e: Exception) {
                android.util.Log.e("SettlementFragment", "Erro ao carregar últimas trocas: ${e.message}", e)
                mostrarMensagemErro()
            }
        }
    }
    
    private fun mostrarMensagemAguarde() {
        binding.llUltimasTrocasPanos.removeAllViews()
        val textView = TextView(requireContext()).apply {
            text = "Aguarde carregamento das mesas..."
            textSize = 14f
            setTextColor(requireContext().getColor(android.R.color.darker_gray))
            setPadding(16, 8, 16, 8)
        }
        binding.llUltimasTrocasPanos.addView(textView)
    }
    
    private fun mostrarMensagemSemMesas() {
        binding.llUltimasTrocasPanos.removeAllViews()
        val textView = TextView(requireContext()).apply {
            text = "Cliente sem mesas"
            textSize = 14f
            setTextColor(requireContext().getColor(android.R.color.darker_gray))
            setPadding(16, 8, 16, 8)
        }
        binding.llUltimasTrocasPanos.addView(textView)
    }
    
    private fun mostrarMensagemErro() {
        binding.llUltimasTrocasPanos.removeAllViews()
        val textView = TextView(requireContext()).apply {
            text = "Erro ao carregar histórico"
            textSize = 14f
            setTextColor(requireContext().getColor(android.R.color.holo_red_dark))
            setPadding(16, 8, 16, 8)
        }
        binding.llUltimasTrocasPanos.addView(textView)
    }
    
    private fun criarCardUltimaTroca(numeroPano: String, numeroMesa: String, infoPano: String, dataTroca: String): View {
        val cardView = com.google.android.material.card.MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8.dpToPx()
            }
            radius = 8f
            elevation = 2f
            strokeWidth = 1
            strokeColor = requireContext().getColor(android.R.color.darker_gray)
        }
        
        val linearLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12.dpToPx(), 8.dpToPx(), 12.dpToPx(), 8.dpToPx())
        }
        
        // Número do pano e mesa
        val tvNumero = TextView(requireContext()).apply {
            text = "$numeroPano - Mesa $numeroMesa"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(requireContext().getColor(android.R.color.white))
        }
        
        // Info do pano
        val tvInfo = TextView(requireContext()).apply {
            text = infoPano
            textSize = 14f
            setTextColor(requireContext().getColor(android.R.color.darker_gray))
        }
        
        // Data da troca
        val tvData = TextView(requireContext()).apply {
            text = "Última troca: $dataTroca"
            textSize = 12f
            setTextColor(requireContext().getColor(android.R.color.darker_gray))
        }
        
        linearLayout.addView(tvNumero)
        linearLayout.addView(tvInfo)
        linearLayout.addView(tvData)
        cardView.addView(linearLayout)
        
        return cardView
    }
    
    private fun Int.dpToPx(): Int {
        return (this * requireContext().resources.displayMetrics.density).toInt()
    }
    
    /**
     * ✅ NOVO: Mostra a seleção de pano
     */
    private fun mostrarSelecaoPano(mesaIdParam: Long? = null) {
        // Obter tamanho da mesa para filtrar panos
        val mesaTarget = if (mesaIdParam != null) {
            mesasAcertoAdapter.currentList.firstOrNull { it.id == mesaIdParam }
        } else {
            mesasAcertoAdapter.currentList.firstOrNull()
        }
        val tamanhoMesa = mesaTarget?.tamanho?.let { tamanhoEnum ->
            when (tamanhoEnum) {
                com.example.gestaobilhares.data.entities.TamanhoMesa.PEQUENA -> "Pequeno"
                com.example.gestaobilhares.data.entities.TamanhoMesa.MEDIA -> "Médio"
                com.example.gestaobilhares.data.entities.TamanhoMesa.GRANDE -> "Grande"
            }
        }
        
        Log.d("SettlementFragment", "[PANO] Abrindo seleção de pano (mesaId=${mesaTarget?.id}, tamanhoMesa=$tamanhoMesa)")
        PanoSelectionDialog.newInstance(
            onPanoSelected = { panoSelecionado ->
                Log.d("SettlementFragment", "Pano selecionado no acerto: ${panoSelecionado.numero}")
                
                // ✅ CORREÇÃO: Marcar pano como usado IMEDIATAMENTE quando selecionado
                lifecycleScope.launch {
                    try {
                        val mesaId = mesaTarget?.id ?: 0L
                        if (mesaId != 0L) {
                            Log.d("SettlementFragment", "Marcando pano ${panoSelecionado.numero} como usado no acerto")
                            viewModel.trocarPanoNaMesa(mesaId, panoSelecionado.numero, "Usado no acerto")
                            // Marcar flag de troca de pano
                            houveTrocaPanoNoAcerto = true
                            
                            // Mostrar confirmação da troca
                            val mesaNumero = mesaTarget?.numero ?: "N/A"
                            Toast.makeText(requireContext(), "Pano ${panoSelecionado.numero} trocado na Mesa $mesaNumero!", Toast.LENGTH_SHORT).show()
                            
                            // Atualizar histórico imediatamente
                            carregarUltimasTrocasTodasMesas()
                            
                            // Mostrar opção de trocar mais panos
                            mostrarOpcaoTrocarMaisPanos()
                        } else {
                            Log.e("SettlementFragment", "Erro: Nenhuma mesa disponível para vincular o pano")
                            Toast.makeText(requireContext(), "Erro: Nenhuma mesa disponível para vincular o pano.", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e("SettlementFragment", "Erro ao marcar pano como usado: ${e.message}", e)
                        Toast.makeText(requireContext(), "Erro ao selecionar pano: ${e.message}", Toast.LENGTH_LONG).show()
                        
                        // ✅ CORREÇÃO: Tentar pelo menos marcar o pano como usado no estoque
                        try {
                            viewModel.marcarPanoComoUsado(panoSelecionado.numero, "Usado no acerto")
                            Toast.makeText(requireContext(), "Pano ${panoSelecionado.numero} marcado como usado no estoque", Toast.LENGTH_SHORT).show()
                        } catch (e2: Exception) {
                            Log.e("SettlementFragment", "Erro crítico ao marcar pano como usado: ${e2.message}", e2)
                            Toast.makeText(requireContext(), "Erro crítico: ${e2.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            tamanhoMesa = tamanhoMesa
        ).show(childFragmentManager, "select_pano")
    }
    
    private fun mostrarOpcaoTrocarMaisPanos() {
        val mesas = if (::mesasAcertoAdapter.isInitialized) mesasAcertoAdapter.currentList else emptyList()
        if (mesas.size > 1) {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Troca de Pano Realizada")
                .setMessage("Deseja trocar pano em outra mesa?")
                .setPositiveButton("Sim") { _, _ ->
                    mostrarSelecaoMesaParaTrocaPano()
                }
                .setNegativeButton("Não") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        } else {
            showSnackbar("Pano trocado com sucesso!")
        }
    }

    private fun mostrarSelecaoMesaParaTrocaPano() {
        val mesas = mesasAcertoAdapter.currentList
        val opcoes = mesas.map { mesa -> "Mesa ${mesa.numero}" }.toTypedArray()
        val ids = mesas.map { it.id }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Selecione a mesa para trocar o pano")
            .setItems(opcoes) { _, which ->
                val mesaId = ids[which]
                mostrarSelecaoPano(mesaId)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    /**
     * ✅ SIMPLIFICADO: Apenas oculta o layout do pano (já foi marcado como usado na seleção)
     */
    private fun trocarPano() {
        Log.d("SettlementFragment", "Finalizando seleção de pano")
        
        // Ocultar layout do pano
        binding.layoutNovoPano.visibility = View.GONE
        // Removido: checkbox não existe mais
        
        // Atualizar pano atual da mesa
        carregarPanoAtual()
        carregarUltimasTrocasTodasMesas()
        
        Toast.makeText(requireContext(), "Pano selecionado com sucesso!", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 









