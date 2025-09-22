package com.example.gestaobilhares.ui.mesas

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import java.util.Date

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

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && fotoUri != null) {
            binding.ivFotoMesa.setImageURI(fotoUri)
            binding.ivFotoMesa.visibility = View.VISIBLE
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
        // TODO: Implementar captura de foto
        Toast.makeText(requireContext(), "Captura de foto será implementada em breve", Toast.LENGTH_SHORT).show()
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
            fotoReforma = fotoUri?.toString(),
            dataReforma = Date()
        )

        viewModel.salvarReforma(mesaReformada)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
