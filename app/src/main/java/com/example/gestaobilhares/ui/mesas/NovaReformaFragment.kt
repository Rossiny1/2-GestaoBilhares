package com.example.gestaobilhares.ui.mesas

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.gestaobilhares.R
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.entities.MesaReformada
import com.example.gestaobilhares.data.entities.PanoEstoque
import com.example.gestaobilhares.data.entities.TipoMesa
import com.example.gestaobilhares.data.entities.TamanhoMesa
import com.example.gestaobilhares.data.entities.HistoricoManutencaoMesa
import com.example.gestaobilhares.data.entities.TipoManutencao
import com.example.gestaobilhares.databinding.FragmentNovaReformaBinding
import com.example.gestaobilhares.ui.settlement.PanoSelectionDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

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

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            try {
                fotoUri?.let { uri ->
                    Log.d("NovaReformaFragment", "Foto capturada com sucesso: $uri")
                    
                    binding.root.post {
                        try {
                            val caminhoReal = obterCaminhoRealFoto(uri)
                            if (caminhoReal != null) {
                                Log.d("NovaReformaFragment", "Caminho real da foto: $caminhoReal")
                                fotoPath = caminhoReal
                                binding.ivFotoMesa.setImageURI(uri)
                                binding.ivFotoMesa.visibility = View.VISIBLE
                                binding.btnRemoverFoto.visibility = View.VISIBLE
                                Toast.makeText(requireContext(), "Foto da mesa reformada capturada com sucesso!", Toast.LENGTH_SHORT).show()
                            } else {
                                Log.e("NovaReformaFragment", "Não foi possível obter o caminho real da foto")
                                Toast.makeText(requireContext(), "Erro: não foi possível salvar a foto", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Log.e("NovaReformaFragment", "Erro ao processar foto: ${e.message}", e)
                            Toast.makeText(requireContext(), "Erro ao processar foto: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("NovaReformaFragment", "Erro crítico após captura de foto: ${e.message}", e)
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
        viewModel.mesasDisponiveis.observe(viewLifecycleOwner) { mesas ->
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

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // TODO: Implementar loading state
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearSuccess()
                findNavController().navigateUp()
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
            Log.e("NovaReformaFragment", "Erro ao abrir câmera: ${e.message}", e)
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
            Log.d("NovaReformaFragment", "Obtendo caminho real para URI: $uri")
            
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
                            Log.d("NovaReformaFragment", "Caminho real encontrado via cursor: $path")
                            return path
                        }
                    }
                }
            }
            
            // Tentativa 2: Se o cursor não funcionou, criar arquivo temporário
            Log.d("NovaReformaFragment", "Cursor não funcionou, criando arquivo temporário...")
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val tempFile = File.createTempFile("reforma_foto_", ".jpg", requireContext().cacheDir)
                tempFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                Log.d("NovaReformaFragment", "Arquivo temporário criado: ${tempFile.absolutePath}")
                return tempFile.absolutePath
            }
            
            // Tentativa 3: Fallback para URI como string
            Log.d("NovaReformaFragment", "Usando URI como fallback: $uri")
            uri.toString()
            
        } catch (e: Exception) {
            Log.e("NovaReformaFragment", "Erro ao obter caminho real da foto: ${e.message}", e)
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
            fotoReforma = fotoPath, // Usar o caminho real da foto
            dataReforma = Date()
        )

        viewModel.salvarReforma(mesaReformada)
        
        // ✅ NOVO: Registrar no histórico de manutenção
        registrarManutencoesNoHistorico(mesaReformada)
    }

    /**
     * Registra as manutenções realizadas no histórico de manutenção da mesa.
     */
    private fun registrarManutencoesNoHistorico(mesaReformada: MesaReformada) {
        val mesa = mesaSelecionada ?: return
        
        // Registrar pintura
        if (mesaReformada.pintura) {
            historicoViewModel.registrarManutencao(
                mesaId = mesa.id,
                numeroMesa = mesa.numero,
                tipoManutencao = TipoManutencao.PINTURA,
                descricao = "Pintura da mesa realizada durante reforma",
                responsavel = "Sistema de Reforma",
                observacoes = mesaReformada.observacoes
            )
        }
        
        // Registrar troca de pano
        if (mesaReformada.panos) {
            val descricaoPano = if (!mesaReformada.numeroPanos.isNullOrBlank()) {
                "Troca de pano - Números: ${mesaReformada.numeroPanos}"
            } else {
                "Troca de pano realizada durante reforma"
            }
            
            historicoViewModel.registrarManutencao(
                mesaId = mesa.id,
                numeroMesa = mesa.numero,
                tipoManutencao = TipoManutencao.TROCA_PANO,
                descricao = descricaoPano,
                responsavel = "Sistema de Reforma",
                observacoes = mesaReformada.observacoes
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
                observacoes = mesaReformada.observacoes
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
                observacoes = mesaReformada.observacoes
            )
        }
    }

    /**
     * ✅ NOVO: Mostra seleção de panos para reforma
     */
    private fun mostrarSelecaoPanosReforma() {
        Log.d("NovaReformaFragment", "Mostrando seleção de panos para reforma")
        
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
        
        Log.d("NovaReformaFragment", "Tamanho da mesa: $tamanhoMesaStr")
        
        try {
            val dialog = PanoSelectionDialog.newInstance(
                onPanoSelected = { panoSelecionado ->
                    Log.d("NovaReformaFragment", "Pano selecionado: ${panoSelecionado.numero}")
                    // Registrar uso do pano na reforma
                    registrarPanoReforma(panoSelecionado)
                },
                tamanhoMesa = tamanhoMesaStr
            )
            
            Log.d("NovaReformaFragment", "Mostrando diálogo de seleção de panos")
            dialog.show(childFragmentManager, "select_pano_reforma")
            
        } catch (e: Exception) {
            Log.e("NovaReformaFragment", "Erro ao mostrar diálogo de seleção de panos: ${e.message}", e)
            Toast.makeText(requireContext(), "Erro ao abrir seleção de panos: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * ✅ NOVO: Registra o uso do pano na reforma
     */
    private fun registrarPanoReforma(panoSelecionado: com.example.gestaobilhares.data.entities.PanoEstoque) {
        Log.d("NovaReformaFragment", "Registrando pano ${panoSelecionado.numero} para reforma")
        
        // Marcar pano como usado no estoque
        viewModel.marcarPanoComoUsado(panoSelecionado.id, "Usado em reforma da mesa ${mesaSelecionada?.numero}")
        
        numeroPanoSelecionado = panoSelecionado.numero
        Log.d("NovaReformaFragment", "Pano selecionado armazenado: $numeroPanoSelecionado")
        Toast.makeText(requireContext(), "Pano ${panoSelecionado.numero} selecionado", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
