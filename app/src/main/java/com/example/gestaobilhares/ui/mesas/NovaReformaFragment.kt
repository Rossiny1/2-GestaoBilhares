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
import com.example.gestaobilhares.data.entities.TipoMesa
import com.example.gestaobilhares.data.entities.TamanhoMesa
import com.example.gestaobilhares.databinding.FragmentNovaReformaBinding
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
    
    private var mesaSelecionada: Mesa? = null
    private var fotoUri: Uri? = null
    private var fotoPath: String? = null

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

        binding.cbPanos.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutNumeroPanos.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        binding.btnSelecionarPanos.setOnClickListener {
            // TODO: Implementar seletor de panos do estoque
            Toast.makeText(requireContext(), "Seletor de panos será implementado em breve", Toast.LENGTH_SHORT).show()
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

        val numeroPanos = if (panos) {
            binding.etNumeroPanos.text.toString().trim().takeIf { it.isNotEmpty() }
        } else null

        if (panos && numeroPanos.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Informe os números dos panos utilizados", Toast.LENGTH_SHORT).show()
            return
        }

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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
