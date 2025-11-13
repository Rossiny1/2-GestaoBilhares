package com.example.gestaobilhares.ui.mesas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.data.entities.TipoMesa
import com.example.gestaobilhares.data.entities.TamanhoMesa
import com.example.gestaobilhares.data.entities.EstadoConservacao
import com.example.gestaobilhares.databinding.FragmentCadastroMesaBinding
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.data.factory.RepositoryFactory
import kotlinx.coroutines.launch
import java.util.*

// Hilt removido - usando instanciação direta
class CadastroMesaFragment : Fragment() {
    private var _binding: FragmentCadastroMesaBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: CadastroMesaViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCadastroMesaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val appRepository = RepositoryFactory.getAppRepository(requireContext())
        viewModel = CadastroMesaViewModel(appRepository)
        setupUI()
    }

    private fun setupUI() {
        // Configurar spinner de tipos
        val tipos = TipoMesa.values().map { formatTipoMesa(it) }
        val tipoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tipos)
        tipoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spTipoMesa.adapter = tipoAdapter

        // ✅ NOVO: Listener para mostrar/ocultar campo de tamanho
        binding.spTipoMesa.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val tipoSelecionado = TipoMesa.values()[position]
                if (tipoSelecionado == TipoMesa.SINUCA) {
                    binding.layoutTamanhoMesa.visibility = android.view.View.VISIBLE
                } else {
                    binding.layoutTamanhoMesa.visibility = android.view.View.GONE
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                binding.layoutTamanhoMesa.visibility = android.view.View.GONE
            }
        }

        // Configurar spinner de tamanhos
        val tamanhos = TamanhoMesa.values().map { formatTamanhoMesa(it) }
        val tamanhoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tamanhos)
        tamanhoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spTamanhoMesa.adapter = tamanhoAdapter

        // Configurar spinner de estado de conservação
        val estados = EstadoConservacao.values().map { formatEstadoConservacao(it) }
        val estadoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, estados)
        estadoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spEstadoConservacao.adapter = estadoAdapter

        binding.btnSalvarMesa.setOnClickListener {
            val numero = binding.etNumeroMesa.text.toString().trim()
            val tipoIndex = binding.spTipoMesa.selectedItemPosition
            val tamanhoIndex = binding.spTamanhoMesa.selectedItemPosition
            val estadoIndex = binding.spEstadoConservacao.selectedItemPosition
            val relogioStr = binding.etRelogio.text?.toString()?.trim()
            
            if (numero.isEmpty() || relogioStr.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Preencha todos os campos obrigatórios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val relogio = relogioStr.toIntOrNull()
            if (relogio == null) {
                binding.tilRelogio.error = "Valor inválido"
                return@setOnClickListener
            } else {
                binding.tilRelogio.error = null
            }

            // ✅ CORREÇÃO: Usar lifecycleScope para evitar crashes
            lifecycleScope.launch {
                try {
                    // ✅ NOVA VALIDAÇÃO: Verificar se número da mesa já existe
                    viewModel.verificarNumeroMesaExistente(numero) { existe ->
                        // ✅ VERIFICAÇÃO DE SEGURANÇA: Verificar se o Fragment ainda está ativo
                        if (!isAdded || _binding == null) {
                            return@verificarNumeroMesaExistente
                        }
                        
                        if (existe) {
                            binding.etNumeroMesa.error = "Número de mesa já existe"
                            return@verificarNumeroMesaExistente
                        }

                        // ✅ NOVA LÓGICA: Definir tamanho padrão para tipos que não são sinuca
                        val tipoSelecionado = TipoMesa.values()[tipoIndex]
                        val tamanhoFinal = if (tipoSelecionado == TipoMesa.SINUCA) {
                            TamanhoMesa.values()[tamanhoIndex]
                        } else {
                            TamanhoMesa.GRANDE // Tamanho padrão para outros tipos
                        }
                        
                        val mesa = Mesa(
                            numero = numero,
                            clienteId = null,
                            // ✅ REMOVIDO: fichasInicial e fichasFinal - usando apenas relogioInicial e relogioFinal
                            relogioInicial = relogio,
                            relogioFinal = relogio,
                            tipoMesa = tipoSelecionado,
                            tamanho = tamanhoFinal,
                            estadoConservacao = EstadoConservacao.values()[estadoIndex],
                            ativa = true,
                            observacoes = binding.etObservacoes.text?.toString()?.takeIf { it.isNotBlank() },
                            dataInstalacao = Date(),
                            dataUltimaLeitura = Date()
                        )
                        
                        viewModel.salvarMesa(mesa)
                        
                        // ✅ VERIFICAÇÃO FINAL: Verificar se ainda está ativo antes de mostrar Toast
                        if (isAdded && _binding != null) {
                            Toast.makeText(requireContext(), "Mesa cadastrada com sucesso!", Toast.LENGTH_SHORT).show()
                            findNavController().popBackStack()
                        }
                    }
                } catch (e: Exception) {
                    // ✅ TRATAMENTO DE ERRO: Verificar se ainda está ativo antes de mostrar erro
                    if (isAdded && _binding != null) {
                        Toast.makeText(requireContext(), "Erro ao salvar mesa: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        
        binding.btnCancelar.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun formatTipoMesa(tipo: TipoMesa): String {
        return when (tipo) {
            TipoMesa.SINUCA -> "Sinuca"
            TipoMesa.JUKEBOX -> "Jukebox"
            TipoMesa.PEMBOLIM -> "Pembolim"
            TipoMesa.OUTROS -> "Outros"
        }
    }

    private fun formatTamanhoMesa(tamanho: TamanhoMesa): String {
        return when (tamanho) {
            TamanhoMesa.PEQUENA -> "Pequena"
            TamanhoMesa.MEDIA -> "Média"
            TamanhoMesa.GRANDE -> "Grande"
        }
    }

    private fun formatEstadoConservacao(estado: EstadoConservacao): String {
        return when (estado) {
            EstadoConservacao.OTIMO -> "Ótimo"
            EstadoConservacao.BOM -> "Bom"
            EstadoConservacao.RUIM -> "Ruim"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 