package com.example.gestaobilhares.ui.colaboradores

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.gestaobilhares.R
import com.example.gestaobilhares.data.entities.Colaborador
import com.example.gestaobilhares.data.entities.NivelAcesso
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.repository.AppRepository
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.databinding.FragmentColaboradorRegisterBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment para cadastro e edição de colaboradores.
 * Permite inserir dados pessoais completos e configurações de acesso.
 */
class ColaboradorRegisterFragment : Fragment() {

    private var _binding: FragmentColaboradorRegisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var appRepository: AppRepository
    private var colaboradorId: Long? = null
    private var dataNascimento: Date? = null
    private val rotasSelecionadas = mutableSetOf<Long>()
    private var todasRotas = listOf<Rota>()

    // Formatadores de data
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    private val displayFormatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentColaboradorRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRepository()
        setupToolbar()
        setupDropdowns()
        setupDatePicker()
        setupClickListeners()
        setupRotasSelector()
        
        // Verificar se é edição
        colaboradorId = arguments?.getLong("colaborador_id", -1L).takeIf { it != -1L }
        if (colaboradorId != null) {
            carregarColaborador(colaboradorId!!)
        }
    }

    private fun setupRepository() {
        val database = AppDatabase.getDatabase(requireContext())
        appRepository =         AppRepository(
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
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        
        // Atualizar título se for edição
        if (colaboradorId != null) {
            binding.toolbar.title = "Editar Colaborador"
        }
    }

    private fun setupDropdowns() {
        // Estados brasileiros
        val estados = arrayOf(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA",
            "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN",
            "RS", "RO", "RR", "SC", "SP", "SE", "TO"
        )
        
        val estadosAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, estados)
        binding.etEstado.setAdapter(estadosAdapter)
        
        // Estados civis
        val estadosCivis = arrayOf("Solteiro", "Casado", "Divorciado", "Viúvo", "União Estável")
        val estadosCivisAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, estadosCivis)
        binding.etEstadoCivil.setAdapter(estadosCivisAdapter)
        
        // Níveis de acesso
        val niveisAcesso = NivelAcesso.values().map { it.name }
        val niveisAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, niveisAcesso)
        binding.etNivelAcesso.setAdapter(niveisAdapter)
    }

    private fun setupDatePicker() {
        binding.etDataNascimento.setOnClickListener {
            val calendar = Calendar.getInstance()
            
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val selectedDate = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth)
                    }.time
                    dataNascimento = selectedDate
                    binding.etDataNascimento.setText(displayFormatter.format(selectedDate))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            
            datePickerDialog.show()
        }
    }

    private fun setupClickListeners() {
        binding.btnCancelar.setOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.btnSalvar.setOnClickListener {
            salvarColaborador()
        }
        
        binding.btnMetas.setOnClickListener {
            if (colaboradorId != null) {
                val bundle = Bundle().apply {
                    putLong("colaborador_id", colaboradorId!!)
                }
                findNavController().navigate(R.id.colaboradorMetasFragment, bundle)
            }
        }
    }

    private fun setupRotasSelector() {
        binding.etRotas.setOnClickListener {
            mostrarDialogoSelecaoRotas()
        }
    }

    private fun mostrarDialogoSelecaoRotas() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Carregar todas as rotas
                todasRotas = withContext(Dispatchers.IO) {
                    appRepository.obterTodasRotas().first()
                }
                
                if (todasRotas.isEmpty()) {
                    Toast.makeText(requireContext(), "Nenhuma rota cadastrada", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                val nomesRotas = todasRotas.map { it.nome }.toTypedArray()
                val rotasSelecionadasArray = BooleanArray(todasRotas.size) { index ->
                    rotasSelecionadas.contains(todasRotas[index].id)
                }
                
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Selecionar Rotas")
                    .setMultiChoiceItems(
                        nomesRotas,
                        rotasSelecionadasArray
                    ) { _, which, isChecked ->
                        val rotaId = todasRotas[which].id
                        if (isChecked) {
                            rotasSelecionadas.add(rotaId)
                        } else {
                            rotasSelecionadas.remove(rotaId)
                        }
                    }
                    .setPositiveButton("Confirmar") { _, _ ->
                        android.util.Log.d("ColaboradorRegister", "🔍 Rotas selecionadas após confirmação: $rotasSelecionadas")
                        atualizarTextoRotasSelecionadas()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
                    
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro ao carregar rotas: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun atualizarTextoRotasSelecionadas() {
        if (rotasSelecionadas.isEmpty()) {
            binding.etRotas.setText("Selecionar Rotas")
        } else {
            val nomesRotas = todasRotas
                .filter { it.id in rotasSelecionadas }
                .map { it.nome }
                .joinToString(", ")
            binding.etRotas.setText("Rotas: $nomesRotas")
        }
    }

    private fun carregarColaborador(id: Long) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val colaborador = withContext(Dispatchers.IO) {
                    appRepository.obterColaboradorPorId(id)
                }
                
                colaborador?.let { col ->
                    // Preencher campos básicos
                    binding.etNome.setText(col.nome)
                    binding.etEmail.setText(col.email)
                    binding.etTelefone.setText(col.telefone ?: "")
                    binding.etCpf.setText(col.cpf ?: "")
                    
                    // Preencher dados pessoais
                    col.dataNascimento?.let { data ->
                        dataNascimento = data
                        binding.etDataNascimento.setText(displayFormatter.format(data))
                    }
                    
                    binding.etEndereco.setText(col.endereco ?: "")
                    binding.etBairro.setText(col.bairro ?: "")
                    binding.etCidade.setText(col.cidade ?: "")
                    binding.etEstado.setText(col.estado ?: "")
                    binding.etCep.setText(col.cep ?: "")
                    
                    // Preencher documentos
                    binding.etRg.setText(col.rg ?: "")
                    binding.etOrgaoEmissor.setText(col.orgaoEmissor ?: "")
                    binding.etEstadoCivil.setText(col.estadoCivil ?: "")
                    binding.etNomeMae.setText(col.nomeMae ?: "")
                    binding.etNomePai.setText(col.nomePai ?: "")
                    
                    // Preencher configurações
                    binding.etNivelAcesso.setText(col.nivelAcesso.name)
                    
                    // ✅ NOVO: Logs para debug do carregamento
                    android.util.Log.d("ColaboradorRegister", "=== CARREGANDO ROTAS VINCULADAS ===")
                    android.util.Log.d("ColaboradorRegister", "ID do colaborador: $id")
                    
                    // Carregar rotas vinculadas
                    val rotasVinculadas = withContext(Dispatchers.IO) {
                        appRepository.obterRotasPorColaborador(id).first()
                    }
                    
                    android.util.Log.d("ColaboradorRegister", "🔍 Rotas vinculadas encontradas: ${rotasVinculadas.size}")
                    rotasVinculadas.forEach { colaboradorRota ->
                        android.util.Log.d("ColaboradorRegister", "   - Rota ID: ${colaboradorRota.rotaId}")
                    }
                    
                    rotasSelecionadas.clear()
                    rotasSelecionadas.addAll(rotasVinculadas.map { it.rotaId })
                    
                    android.util.Log.d("ColaboradorRegister", "🔍 Rotas selecionadas após carregamento: $rotasSelecionadas")
                    
                    // Atualizar texto das rotas
                    setupRotasSelector()
                    atualizarTextoRotasSelecionadas()
                    
                    // Mostrar botão de metas para colaboradores existentes
                    binding.btnMetas.visibility = View.VISIBLE
                    
                } ?: run {
                    Toast.makeText(requireContext(), "Colaborador não encontrado", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro ao carregar colaborador: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun salvarColaborador() {
        val nome = binding.etNome.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val telefone = binding.etTelefone.text.toString().trim()
        val cpf = binding.etCpf.text.toString().trim()
        
        // Validações básicas
        if (nome.isEmpty() || email.isEmpty()) {
            Toast.makeText(requireContext(), "Nome e email são obrigatórios", Toast.LENGTH_SHORT).show()
            return
        }
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val colaborador = Colaborador(
                    id = colaboradorId ?: 0,
                    nome = nome,
                    email = email,
                    telefone = telefone.takeIf { it.isNotEmpty() },
                    cpf = cpf.takeIf { it.isNotEmpty() },
                    dataNascimento = dataNascimento,
                    endereco = binding.etEndereco.text.toString().trim().takeIf { it.isNotEmpty() },
                    bairro = binding.etBairro.text.toString().trim().takeIf { it.isNotEmpty() },
                    cidade = binding.etCidade.text.toString().trim().takeIf { it.isNotEmpty() },
                    estado = binding.etEstado.text.toString().takeIf { it.isNotEmpty() },
                    cep = binding.etCep.text.toString().trim().takeIf { it.isNotEmpty() },
                    rg = binding.etRg.text.toString().trim().takeIf { it.isNotEmpty() },
                    orgaoEmissor = binding.etOrgaoEmissor.text.toString().trim().takeIf { it.isNotEmpty() },
                    estadoCivil = binding.etEstadoCivil.text.toString().takeIf { it.isNotEmpty() },
                    nomeMae = binding.etNomeMae.text.toString().trim().takeIf { it.isNotEmpty() },
                    nomePai = binding.etNomePai.text.toString().trim().takeIf { it.isNotEmpty() },
                    nivelAcesso = NivelAcesso.valueOf(binding.etNivelAcesso.text.toString())
                )
                
                val idSalvo = withContext(Dispatchers.IO) {
                    if (colaboradorId == null) {
                        appRepository.inserirColaborador(colaborador)
                    } else {
                        appRepository.atualizarColaborador(colaborador)
                        colaboradorId!!
                    }
                }
                
                // ✅ NOVO: Logs detalhados para debug
                android.util.Log.d("ColaboradorRegister", "=== SALVANDO VINCULAÇÕES DE ROTAS ===")
                android.util.Log.d("ColaboradorRegister", "ID do colaborador: $idSalvo")
                android.util.Log.d("ColaboradorRegister", "Rotas selecionadas: $rotasSelecionadas")
                
                // Salvar vinculações de rotas
                withContext(Dispatchers.IO) {
                    try {
                        // Remover vinculações antigas
                        android.util.Log.d("ColaboradorRegister", "🔍 Removendo vinculações antigas...")
                        appRepository.removerRotasColaborador(idSalvo)
                        android.util.Log.d("ColaboradorRegister", "✅ Vinculações antigas removidas")
                        
                        // Adicionar novas vinculações
                        android.util.Log.d("ColaboradorRegister", "🔍 Adicionando novas vinculações...")
                        rotasSelecionadas.forEach { rotaId ->
                            android.util.Log.d("ColaboradorRegister", "   Vinculando rota ID: $rotaId")
                            appRepository.vincularColaboradorRota(
                                colaboradorId = idSalvo,
                                rotaId = rotaId,
                                responsavelPrincipal = false,
                                dataVinculacao = Date()
                            )
                            android.util.Log.d("ColaboradorRegister", "   ✅ Rota $rotaId vinculada")
                        }
                        
                        // ✅ NOVO: Verificar se as vinculações foram salvas
                        val rotasVerificadas = appRepository.obterRotasPorColaborador(idSalvo).first()
                        android.util.Log.d("ColaboradorRegister", "🔍 Verificando vinculações salvas...")
                        android.util.Log.d("ColaboradorRegister", "   Total de rotas vinculadas: ${rotasVerificadas.size}")
                        rotasVerificadas.forEach { colaboradorRota ->
                            android.util.Log.d("ColaboradorRegister", "   - Rota ID: ${colaboradorRota.rotaId}")
                        }
                        
                    } catch (e: Exception) {
                        android.util.Log.e("ColaboradorRegister", "❌ Erro ao salvar vinculações: ${e.message}", e)
                        throw e
                    }
                }
                
                Toast.makeText(requireContext(), "Colaborador salvo com sucesso!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
                
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro ao salvar colaborador: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
