package com.example.gestaobilhares.ui.mesas

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import timber.log.Timber
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import com.example.gestaobilhares.ui.R
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.entities.MesaReformada
import com.example.gestaobilhares.data.entities.PanoEstoque
import com.example.gestaobilhares.data.entities.TipoMesa
import com.example.gestaobilhares.data.entities.TamanhoMesa
import com.example.gestaobilhares.data.entities.HistoricoManutencaoMesa
import com.example.gestaobilhares.data.entities.TipoManutencao
import com.example.gestaobilhares.ui.databinding.FragmentNovaReformaBinding
import com.example.gestaobilhares.ui.settlement.PanoSelectionDialog
import com.example.gestaobilhares.core.utils.ImageCompressionUtils
import com.example.gestaobilhares.core.utils.FirebaseImageUploader
import com.example.gestaobilhares.core.utils.NetworkUtils
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.repository.AppRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.viewModels
import javax.inject.Inject

/**
 * Fragment para cadastrar uma nova reforma de mesa.
 */
@AndroidEntryPoint
class NovaReformaFragment : Fragment() {

    private var _binding: FragmentNovaReformaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NovaReformaViewModel by viewModels()
    private val historicoViewModel: HistoricoManutencaoMesaViewModel by viewModels()
    
    private var mesaSelecionada: Mesa? = null
    private var fotoUri: Uri? = null
    private var fotoPath: String? = null
    private var numeroPanoSelecionado: String? = null
    private var panoSelecionadoId: Long? = null
    
    // ✅ NOVO: Inicialização segura do ImageCompressionUtils
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

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            try {
                fotoUri?.let { uri ->
                    Timber.d("NovaReformaFragment", "Foto capturada com sucesso: $uri")
                    
                    binding.root.post {
                        try {
                            val caminhoReal = obterCaminhoRealFoto(uri)
                            if (caminhoReal != null) {
                                Timber.d("NovaReformaFragment", "Caminho real da foto: $caminhoReal")
                                fotoPath = caminhoReal
                                binding.ivFotoMesa.setImageURI(uri)
                                binding.ivFotoMesa.visibility = View.VISIBLE
                                binding.btnRemoverFoto.visibility = View.VISIBLE
                                Toast.makeText(requireContext(), "Foto da mesa reformada capturada com sucesso!", Toast.LENGTH_SHORT).show()
                            } else {
                                Timber.e("NovaReformaFragment", "Não foi possível obter o caminho real da foto")
                                Toast.makeText(requireContext(), "Erro: não foi possível salvar a foto", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Timber.e("NovaReformaFragment", "Erro ao processar foto: ${e.message}", e)
                            Toast.makeText(requireContext(), "Erro ao processar foto: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e("NovaReformaFragment", "Erro crítico após captura de foto: ${e.message}", e)
                Toast.makeText(requireContext(), "Erro ao processar foto capturada", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(requireContext(), "Erro ao capturar foto", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            tirarFoto()
        } else {
            Toast.makeText(requireContext(), "Permissão de câmera necessária para tirar foto", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNovaReformaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSelecionarMesa.setOnClickListener {
            mostrarSeletorMesa()
        }

        // Removido campo de número de pano. Mantemos apenas o botão de seleção.
        binding.cbPanos.setOnCheckedChangeListener { _, isChecked ->
            // Sem campo para mostrar/ocultar. Apenas habilitar o botão quando marcado.
            binding.btnSelecionarPanos.isEnabled = isChecked
            if (!isChecked) {
                numeroPanoSelecionado = null
            }
        }
        // Estado inicial do botão de seleção de pano
        binding.btnSelecionarPanos.isEnabled = binding.cbPanos.isChecked

        binding.btnSelecionarPanos.setOnClickListener {
            mostrarSelecaoPanosReforma()
        }

        binding.btnTirarFoto.setOnClickListener {
            verificarPermissaoCamera()
        }

        binding.btnRemoverFoto.setOnClickListener {
            removerFoto()
        }

        binding.btnSalvarReforma.setOnClickListener {
            salvarReforma()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mesasDisponiveis.collect { mesas ->
                    if (mesas.isNotEmpty()) {
                        val nomesMesas = mesas.map { "Mesa ${it.numero} - ${it.tipoMesa} (${it.tamanho})" }
                        
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Selecionar Mesa")
                            .setItems(nomesMesas.toTypedArray()) { _, which ->
                                mesaSelecionada = mesas[which]
                                binding.tvMesaSelecionada.text = "Mesa ${mesaSelecionada!!.numero} - ${mesaSelecionada!!.tipoMesa} (${mesaSelecionada!!.tamanho})"
                                binding.tvMesaSelecionada.visibility = View.VISIBLE
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                    } else {
                        Toast.makeText(requireContext(), "Nenhuma mesa disponível no depósito", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { _ ->
                    // TODO: Implementar loading state
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorMessage.collect { message ->
                    message?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                        viewModel.clearError()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.successMessage.collect { message ->
                    message?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                        viewModel.clearSuccess()
                        findNavController().navigateUp()
                    }
                }
            }
        }
    }

    private fun mostrarSeletorMesa() {
        viewModel.carregarMesasDisponiveis()
    }

    private fun verificarPermissaoCamera() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                tirarFoto()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun tirarFoto() {
        try {
            val photoFile = criarArquivoFoto()
            fotoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile
            )
            
            takePictureLauncher.launch(fotoUri)
            
        } catch (e: Exception) {
            Timber.e("NovaReformaFragment", "Erro ao abrir câmera: ${e.message}", e)
            Toast.makeText(requireContext(), "Erro ao abrir câmera: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun removerFoto() {
        fotoUri = null
        fotoPath = null
        binding.ivFotoMesa.visibility = View.GONE
        binding.btnRemoverFoto.visibility = View.GONE
        Toast.makeText(requireContext(), "Foto removida", Toast.LENGTH_SHORT).show()
    }

    private fun criarArquivoFoto(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val mesaNumero = mesaSelecionada?.numero ?: "UNKNOWN"
        val imageFileName = "REFORMA_MESA_${mesaNumero}_${timeStamp}"
        val storageDir = requireContext().getExternalFilesDir(null)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    private fun obterCaminhoRealFoto(uri: Uri): String? {
        return try {
            Timber.d("NovaReformaFragment", "Obtendo caminho real para URI: $uri")
            
            // ✅ CORREÇÃO: Tentar comprimir a imagem com fallback seguro
            try {
                val compressedPath = imageCompressionUtils.compressImageFromUri(uri)
                if (compressedPath != null) {
                    Timber.d("NovaReformaFragment", "Imagem comprimida com sucesso: $compressedPath")
                    return compressedPath
                }
            } catch (e: Exception) {
                Timber.w("NovaReformaFragment", "Compressão falhou, usando método original: ${e.message}")
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
                    if (columnIndex >= 0) {
                        val path = it.getString(columnIndex)
                        if (path != null && File(path).exists()) {
                            Timber.d("NovaReformaFragment", "Caminho real encontrado via cursor: $path")
                            
                            // ✅ CORREÇÃO: Tentar comprimir com fallback
                            try {
                                val compressedPath = imageCompressionUtils.compressImageFromPath(path)
                                if (compressedPath != null) {
                                    Timber.d("NovaReformaFragment", "Imagem comprimida do arquivo: $compressedPath")
                                    return compressedPath
                                }
                            } catch (e: Exception) {
                                Timber.w("NovaReformaFragment", "Compressão do arquivo falhou: ${e.message}")
                            }
                            
                            return path
                        }
                    }
                }
            }
            
            // Tentativa 2: Se o cursor não funcionou, criar arquivo temporário
            Timber.d("NovaReformaFragment", "Cursor não funcionou, criando arquivo temporário...")
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val tempFile = File.createTempFile("reforma_foto_", ".jpg", requireContext().cacheDir)
                tempFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                Timber.d("NovaReformaFragment", "Arquivo temporário criado: ${tempFile.absolutePath}")
                
                // ✅ CORREÇÃO: Tentar comprimir com fallback
                try {
                    val compressedPath = imageCompressionUtils.compressImageFromPath(tempFile.absolutePath)
                    if (compressedPath != null) {
                        Timber.d("NovaReformaFragment", "Arquivo temporário comprimido: $compressedPath")
                        return compressedPath
                    }
                } catch (e: Exception) {
                    Timber.w("NovaReformaFragment", "Compressão do arquivo temporário falhou: ${e.message}")
                }
                
                return tempFile.absolutePath
            }
            
            // Tentativa 3: Fallback para URI como string
            Timber.d("NovaReformaFragment", "Usando URI como fallback: $uri")
            uri.toString()
            
        } catch (e: Exception) {
            Timber.e("NovaReformaFragment", "Erro ao obter caminho real da foto: ${e.message}", e)
            null
        }
    }

    private fun salvarReforma() {
        if (mesaSelecionada == null) {
            Toast.makeText(requireContext(), "Selecione uma mesa para reformar", Toast.LENGTH_SHORT).show()
            return
        }

        val pintura = binding.cbPintura.isChecked
        val tabela = binding.cbTabela.isChecked
        val panos = binding.cbPanos.isChecked
        val outros = binding.cbOutros.isChecked

        if (!pintura && !tabela && !panos && !outros) {
            Toast.makeText(requireContext(), "Selecione pelo menos um item reformado", Toast.LENGTH_SHORT).show()
            return
        }

        // ✅ NOVO: Integração com sistema de panos
        val numeroPanos = if (panos) {
            // Se panos foram trocados, exigir seleção prévia; não salvar automaticamente
            if (numeroPanoSelecionado.isNullOrBlank()) {
            mostrarSelecaoPanosReforma()
                Toast.makeText(requireContext(), "Selecione o pano do estoque", Toast.LENGTH_SHORT).show()
                return
            }
            numeroPanoSelecionado
        } else null

        val observacoes = binding.etObservacoes.text.toString().trim().takeIf { it.isNotEmpty() }

        // ✅ CORREÇÃO: Fazer upload da foto para Firebase Storage antes de salvar
        lifecycleScope.launch {
            try {
                var finalFotoPath = fotoPath
                
                // Se há foto e não é URL do Firebase Storage, fazer upload
                if (finalFotoPath != null && !firebaseImageUploader.isFirebaseStorageUrl(finalFotoPath)) {
                    if (networkUtils.isConnected()) {
                        Timber.d("NovaReformaFragment", "Fazendo upload da foto para Firebase Storage...")
                        val uploadedUrl = firebaseImageUploader.uploadMesaReforma(finalFotoPath, mesaSelecionada!!.id)
                        if (uploadedUrl != null) {
                            finalFotoPath = uploadedUrl
                            Timber.d("NovaReformaFragment", "✅ Foto enviada para Firebase Storage: $finalFotoPath")
                        } else {
                            Timber.w("NovaReformaFragment", "⚠️ Falha no upload, usando caminho local")
                        }
                    } else {
                        Timber.d("NovaReformaFragment", "📴 Sem conexão, foto será sincronizada depois")
                    }
                }
                
                val mesaReformada = MesaReformada(
                    mesaId = mesaSelecionada!!.id,
                    numeroMesa = mesaSelecionada!!.numero,
                    tipoMesa = mesaSelecionada!!.tipoMesa,
                    tamanhoMesa = mesaSelecionada!!.tamanho,
                    pintura = pintura,
                    tabela = tabela,
                    panos = panos,
                    numeroPanos = numeroPanos,
                    outros = outros,
                    observacoes = observacoes,
                    fotoReforma = finalFotoPath, // Usar URL do Firebase Storage se disponível
                    dataReforma = System.currentTimeMillis()
                )

                viewModel.salvarReforma(mesaReformada)
                
                // ✅ NOVO: Registrar no histórico de manutenção
                registrarManutencoesNoHistorico(mesaReformada)
            } catch (e: Exception) {
                Timber.e("NovaReformaFragment", "Erro ao salvar reforma: ${e.message}", e)
                Toast.makeText(requireContext(), "Erro ao salvar reforma: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Registra as manutenções realizadas no histórico de manutenção da mesa.
     */
    private fun registrarManutencoesNoHistorico(mesaReformada: MesaReformada) {
        val mesa = mesaSelecionada ?: return
        
        // ✅ NOVO: Usar a foto da reforma como fotoDepois para todas as manutenções
        val fotoReforma = mesaReformada.fotoReforma
        
        // Registrar pintura
        if (mesaReformada.pintura) {
            historicoViewModel.registrarManutencao(
                mesaId = mesa.id,
                numeroMesa = mesa.numero,
                tipoManutencao = TipoManutencao.PINTURA,
                descricao = "Pintura da mesa realizada durante reforma",
                responsavel = "Sistema de Reforma",
                observacoes = mesaReformada.observacoes,
                fotoDepois = fotoReforma
            )
        }
        
        // Registrar troca de pano
        if (mesaReformada.panos) {
            val descricaoPano = if (!mesaReformada.numeroPanos.isNullOrBlank()) {
                "Troca de pano - Números: ${mesaReformada.numeroPanos}"
            } else {
                "Troca de pano realizada durante reforma"
            }

            historicoViewModel.registrarTrocaPanoUnificada(
                mesaId = mesa.id,
                numeroMesa = mesa.numero,
                panoNovoId = panoSelecionadoId,
                descricao = descricaoPano,
                observacao = mesaReformada.observacoes
            )
        }
        
        // Registrar troca de tabela
        if (mesaReformada.tabela) {
            historicoViewModel.registrarManutencao(
                mesaId = mesa.id,
                numeroMesa = mesa.numero,
                tipoManutencao = TipoManutencao.TROCA_TABELA,
                descricao = "Troca de tabela realizada durante reforma",
                responsavel = "Sistema de Reforma",
                observacoes = mesaReformada.observacoes,
                fotoDepois = fotoReforma
            )
        }
        
        // Registrar outras manutenções
        if (mesaReformada.outros) {
            historicoViewModel.registrarManutencao(
                mesaId = mesa.id,
                numeroMesa = mesa.numero,
                tipoManutencao = TipoManutencao.OUTROS,
                descricao = "Outras manutenções realizadas durante reforma",
                responsavel = "Sistema de Reforma",
                observacoes = mesaReformada.observacoes,
                fotoDepois = fotoReforma
            )
        }
    }

    /**
     * ✅ NOVO: Mostra seleção de panos para reforma
     */
    private fun mostrarSelecaoPanosReforma() {
        Timber.d("NovaReformaFragment", "[PANO] Solicitado abrir seleção de panos (manutenção)")
        
        if (mesaSelecionada == null) {
            Toast.makeText(requireContext(), "Selecione uma mesa primeiro", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Selecionar panos do estoque filtrando pelo tamanho da mesa selecionada
        val tamanhoMesaStr = mesaSelecionada?.tamanho?.let { tamanhoEnum ->
            when (tamanhoEnum) {
                com.example.gestaobilhares.data.entities.TamanhoMesa.PEQUENA -> "Pequeno"
                com.example.gestaobilhares.data.entities.TamanhoMesa.MEDIA -> "Médio"
                com.example.gestaobilhares.data.entities.TamanhoMesa.GRANDE -> "Grande"
            }
        }
        
        Timber.d("NovaReformaFragment", "[PANO] Contexto mesa para seleção: tamanho=$tamanhoMesaStr, mesa=${mesaSelecionada?.numero}")
        
        try {
            val dialog = PanoSelectionDialog.newInstance(
                onPanoSelected = { panoSelecionado ->
                    Timber.d("NovaReformaFragment", "Pano selecionado: ${panoSelecionado.numero}")
                    // Registrar uso do pano na reforma
                    registrarPanoReforma(panoSelecionado)
                },
                tamanhoMesa = tamanhoMesaStr
            )
            
            Timber.d("NovaReformaFragment", "[PANO] Abrindo PanoSelectionDialog (manutenção)")
            dialog.show(childFragmentManager, "select_pano_reforma")
            
        } catch (e: Exception) {
            Timber.e("NovaReformaFragment", "Erro ao mostrar diálogo de seleção de panos: ${e.message}", e)
            Toast.makeText(requireContext(), "Erro ao abrir seleção de panos: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * ✅ NOVO: Registra o uso do pano na reforma
     */
    private fun registrarPanoReforma(panoSelecionado: com.example.gestaobilhares.data.entities.PanoEstoque) {
        Timber.d("NovaReformaFragment", "Registrando pano ${panoSelecionado.numero} para reforma")

        // Marcar pano como usado no estoque
        viewModel.marcarPanoComoUsado(panoSelecionado.id, "Usado em reforma da mesa ${mesaSelecionada?.numero}")
        
        // ✅ NOVO: Atualizar a mesa com o pano e data de troca (persistência na entidade Mesa)
        mesaSelecionada?.let { mesa ->
            viewModel.atualizarPanoDaMesaEmReforma(mesa.id, panoSelecionado.id)
            Timber.d("NovaReformaFragment", "Mesa ${mesa.numero} atualizada com pano ${panoSelecionado.numero} (reforma)")
        }
        
        numeroPanoSelecionado = panoSelecionado.numero
        panoSelecionadoId = panoSelecionado.id
        Timber.d("NovaReformaFragment", "Pano selecionado armazenado: $numeroPanoSelecionado")
        Toast.makeText(requireContext(), "Pano ${panoSelecionado.numero} selecionado", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

