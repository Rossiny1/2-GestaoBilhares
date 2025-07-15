package com.example.gestaobilhares.ui.settlement

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
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
import com.example.gestaobilhares.data.repository.MesaRepository
import com.example.gestaobilhares.data.repository.ClienteRepository
import com.example.gestaobilhares.data.repository.AcertoRepository
import com.example.gestaobilhares.data.repository.AcertoMesaRepository
import com.example.gestaobilhares.data.repository.CicloAcertoRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.LinearLayout
import android.widget.Toast
import com.example.gestaobilhares.data.entities.Mesa
import android.util.Log
import com.example.gestaobilhares.ui.settlement.MesaDTO
import com.example.gestaobilhares.ui.clients.AcertoResumo
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Fragment para registrar novos acertos
 * FASE 4A - Implementação crítica do core business
 */
class SettlementFragment : Fragment() {

    private var _binding: FragmentSettlementBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SettlementViewModel
    private val args: SettlementFragmentArgs by navArgs()
    
    private val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private lateinit var mesasAcertoAdapter: MesasAcertoAdapter
    private var paymentValues: MutableMap<String, Double> = mutableMapOf()

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
        
        // Inicializar ViewModel aqui onde o contexto está disponível
        viewModel = SettlementViewModel(
            MesaRepository(AppDatabase.getDatabase(requireContext()).mesaDao()),
            ClienteRepository(AppDatabase.getDatabase(requireContext()).clienteDao()),
            AcertoRepository(AppDatabase.getDatabase(requireContext()).acertoDao()),
            AcertoMesaRepository(AppDatabase.getDatabase(requireContext()).acertoMesaDao()),
            CicloAcertoRepository(AppDatabase.getDatabase(requireContext()).cicloAcertoDao())
        )
        
        Log.d("SettlementFragment", "=== INICIANDO SETTLEMENT FRAGMENT ===")
        Log.d("SettlementFragment", "Cliente ID: ${args.clienteId}")
        
        // Primeiro: verificar permissões
        verificarPermissaoAcerto()
        
        // Segundo: configurar observers
        observeViewModel()
        
        // Terceiro: carregar dados do cliente PRIMEIRO (crítico para comissão)
        carregarDadosClienteESincronizar()
        
        // Quarto: configurar UI básica
        configurarUIBasica()
        
                // Quinto: buscar débito anterior (usado para cálculo do débito atual)
        viewModel.buscarDebitoAnterior(args.clienteId)

        
        
        // Sexto: carregar dados básicos do cliente para header
        viewModel.loadClientForSettlement(args.clienteId)
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
                    
                            // Preparar mesas para acerto
                            val mesasPreparadas = viewModel.prepararMesasParaAcerto(mesasCliente)
                            
                            // Converter para DTO com dados do cliente já carregados
                            val mesasDTO = mesasPreparadas.map { mesa ->
                                MesaDTO(
                                    id = mesa.id,
                                    numero = mesa.numero,
                                    tipoMesa = mesa.tipoMesa.name,
                                    tamanho = mesa.tamanho.name,
                                    estadoConservacao = mesa.estadoConservacao.name,
                                    fichasInicial = mesa.fichasInicial,
                                    fichasFinal = mesa.fichasFinal,
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
                            Log.w("SettlementFragment", "⚠️ Timeout ou nenhuma mesa encontrada, tentando carregar dados básicos...")
                            // Fallback: tentar carregar mesas diretamente sem aguardar Flow
                            carregarMesasFallback(cliente)
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
            
            // Usar repositório diretamente através do ViewModel
            val mesasCliente = viewModel.carregarMesasClienteDireto(args.clienteId)
            
            if (mesasCliente.isNotEmpty()) {
                Log.d("SettlementFragment", "✅ Fallback: ${mesasCliente.size} mesas carregadas")
                
                val mesasDTO = mesasCliente.map { mesa ->
                    MesaDTO(
                        id = mesa.id,
                        numero = mesa.numero,
                        tipoMesa = mesa.tipoMesa.name,
                        tamanho = mesa.tamanho.name,
                        estadoConservacao = mesa.estadoConservacao.name,
                        fichasInicial = mesa.fichasInicial ?: 0,
                        fichasFinal = mesa.fichasFinal ?: 0,
                        valorFixo = mesa.valorFixo,
                        valorFicha = cliente.valorFicha,
                        comissaoFicha = cliente.comissaoFicha,
                        ativa = mesa.ativa
                    )
                }
                
                setupRecyclerViewComDados(mesasDTO)
            } else {
                Log.e("SettlementFragment", "❌ Fallback: Nenhuma mesa encontrada")
                Toast.makeText(requireContext(), "Cliente não possui mesas para acerto", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("SettlementFragment", "❌ Erro no fallback: ${e.message}", e)
            Toast.makeText(requireContext(), "Erro ao carregar dados: ${e.message}", Toast.LENGTH_LONG).show()
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
        binding.tvRepresentante.text = "Administrador"
        binding.cbPanoTrocado.setOnCheckedChangeListener { _, isChecked ->
            binding.etNumeroPano.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        // ✅ Bloquear edição manual do campo Valor Recebido
        binding.etAmountReceived.isFocusable = false
        binding.etAmountReceived.isClickable = false
        binding.etAmountReceived.isLongClickable = false
        binding.etAmountReceived.keyListener = null
    }
    
    private fun setupRecyclerViewComDados(mesasDTO: List<MesaDTO>) {
        Log.d("SettlementFragment", "=== CONFIGURANDO RECYCLERVIEW COM DADOS COMPLETOS ===")
        Log.d("SettlementFragment", "Total de mesas recebidas: ${mesasDTO.size}")
        
        // ✅ DIAGNÓSTICO: Verificar cada mesa individualmente
        mesasDTO.forEachIndexed { index, mesa ->
            Log.d("SettlementFragment", "Mesa $index: ID=${mesa.id}, Número=${mesa.numero}, Tipo=${mesa.tipoMesa}, Ativa=${mesa.ativa}")
        }
        
        mesasAcertoAdapter = MesasAcertoAdapter(
            onDataChanged = { updateCalculations() },
            onCalcularMedia = { mesaId -> 
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
            }
        )
        
        binding.rvMesasAcerto.adapter = mesasAcertoAdapter
        binding.rvMesasAcerto.layoutManager = LinearLayoutManager(requireContext())
        
        // ✅ CORREÇÃO: Usar as mesas preparadas com relógio inicial correto
        Log.d("SettlementFragment", "Carregando ${mesasDTO.size} mesas preparadas para o acerto")
        mesasDTO.forEach { mesa ->
            Log.d("SettlementFragment", "Mesa ${mesa.numero}: relógio inicial=${mesa.fichasInicial}, relógio final=${mesa.fichasFinal}")
        }
        
        // ✅ DIAGNÓSTICO: Verificar se o adapter está sendo configurado corretamente
        Log.d("SettlementFragment", "Adapter configurado: ${mesasAcertoAdapter.itemCount} itens")
        Log.d("SettlementFragment", "LayoutManager configurado: ${binding.rvMesasAcerto.layoutManager}")
        
        mesasAcertoAdapter.submitList(mesasDTO)
        
        // ✅ DIAGNÓSTICO: Verificar após submitList
        Log.d("SettlementFragment", "Após submitList: ${mesasAcertoAdapter.itemCount} itens no adapter")
        Log.d("SettlementFragment", "RecyclerView visível: ${binding.rvMesasAcerto.visibility}")
        Log.d("SettlementFragment", "RecyclerView altura: ${binding.rvMesasAcerto.height}")
        
        // ✅ NOVO: Forçar atualização do RecyclerView
        binding.rvMesasAcerto.post {
            Log.d("SettlementFragment", "Post executado - RecyclerView atualizado")
            Log.d("SettlementFragment", "ItemCount após post: ${mesasAcertoAdapter.itemCount}")
            binding.rvMesasAcerto.invalidate()
        }
    }
    
    private fun setupCalculationListeners() {
        // ✅ CORREÇÃO CRÍTICA: Listener para desconto
        val descontoWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                Log.d("SettlementFragment", "🔄 Desconto alterado: '${s.toString()}' - recalculando débito atual...")
                updateCalculations()
            }
        }
        
        binding.etDesconto.addTextChangedListener(descontoWatcher)
        
        // ✅ CORREÇÃO CRÍTICA: Listener específico para o campo Valor Recebido
        val valorRecebidoWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                Log.d("SettlementFragment", "🔄 Valor recebido alterado: '${s.toString()}' - recalculando débito atual...")
                updateCalculations()
            }
        }
        
        // Adicionar listener ao campo Valor Recebido
        binding.etAmountReceived.addTextChangedListener(valorRecebidoWatcher)
        
        Log.d("SettlementFragment", "✅ Listeners de cálculo configurados - débito atual será atualizado em tempo real")
    }
    
    private fun updateCalculations() {
        try {
            Log.d("SettlementFragment", "=== INICIANDO CÁLCULOS ===")
            
            // Capturar valores dos campos
            val descontoText = binding.etDesconto.text.toString()
            val valorRecebidoText = binding.etAmountReceived.text.toString()
            
            val desconto = descontoText.toDoubleOrNull() ?: 0.0
            val valorRecebido = valorRecebidoText.toDoubleOrNull() ?: 0.0

            Log.d("SettlementFragment", "Texto desconto: '$descontoText' -> R$ $desconto")
            Log.d("SettlementFragment", "Texto valor recebido: '$valorRecebidoText' -> R$ $valorRecebido")
            Log.d("SettlementFragment", "PaymentValues: $paymentValues")
            Log.d("SettlementFragment", "Soma paymentValues: R$ ${paymentValues.values.sum()}")

            // O subtotal agora vem diretamente do adapter, que soma os subtotais de todas as mesas
            val subtotalMesas = mesasAcertoAdapter.getSubtotal()
            
            // Usar o débito anterior carregado do ViewModel
            val debitoAnterior = viewModel.debitoAnterior.value
            val totalComDebito = subtotalMesas + debitoAnterior
            val totalComDesconto = maxOf(0.0, totalComDebito - desconto)
            
            Log.d("SettlementFragment", "=== CÁLCULOS DETALHADOS ===")
            Log.d("SettlementFragment", "Subtotal mesas: R$ $subtotalMesas")
            Log.d("SettlementFragment", "Débito anterior: R$ $debitoAnterior")
            Log.d("SettlementFragment", "Total com débito: R$ $totalComDebito")
            Log.d("SettlementFragment", "Desconto: R$ $desconto")
            Log.d("SettlementFragment", "Total com desconto: R$ $totalComDesconto")
            Log.d("SettlementFragment", "Valor recebido: R$ $valorRecebido")
            
            // ✅ CORREÇÃO CRÍTICA: Calcular débito atual em tempo real
            // Usar diretamente a soma dos paymentValues em vez do campo valor recebido
            val valorRecebidoDosMetodos = paymentValues.values.sum()
            val debitoAtualCalculado = debitoAnterior + subtotalMesas - desconto - valorRecebidoDosMetodos
            
            Log.d("SettlementFragment", "✅ VALOR RECEBIDO DOS MÉTODOS: R$ $valorRecebidoDosMetodos")
            Log.d("SettlementFragment", "✅ PaymentValues detalhado: $paymentValues")
            
            // Atualizar displays dos totais
            binding.tvTableTotal.text = formatter.format(subtotalMesas)
            binding.tvTotalWithDebt.text = formatter.format(totalComDesconto) // Mostrar valor total final
            binding.tvCurrentDebt.text = formatter.format(debitoAtualCalculado) // ✅ DÉBITO ATUAL EM TEMPO REAL
            
            Log.d("SettlementFragment", "✅ DÉBITO ATUAL CALCULADO EM TEMPO REAL: R$ $debitoAtualCalculado")
            Log.d("SettlementFragment", "✅ FÓRMULA: $debitoAnterior + $subtotalMesas - $desconto - $valorRecebidoDosMetodos = $debitoAtualCalculado")
            

            
            Log.d("SettlementFragment", "✅ DISPLAYS ATUALIZADOS")
            Log.d("SettlementFragment", "tvTableTotal: ${binding.tvTableTotal.text}")
            Log.d("SettlementFragment", "tvTotalWithDebt: ${binding.tvTotalWithDebt.text}")
            
        } catch (e: Exception) {
            Log.e("UpdateCalculations", "❌ Erro ao calcular totais", e)
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
        val paymentMethods = arrayOf("Dinheiro", "PIX", "Cartão Débito", "Cartão Crédito", "Transferência")
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
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
        }
        
        // Adicionar título explicativo
        val titleText = TextView(requireContext()).apply {
            text = if (selected.size == 1) {
                "Informe o valor recebido"
            } else {
                "Informe o valor de cada método"
            }
            textSize = 16f
            setTextColor(resources.getColor(com.google.android.material.R.color.material_on_surface_emphasis_high_type, null))
            setPadding(0, 0, 0, 16)
        }
        layout.addView(titleText)
        
        val editTexts = selected.associateWith { metodo ->
            EditText(requireContext()).apply {
                hint = if (selected.size == 1) {
                    "Valor recebido"
                } else {
                    "Valor para $metodo"
                }
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                // Pré-preencher com valor existente se houver
                val valorExistente = paymentValues[metodo]
                if (valorExistente != null && valorExistente > 0) {
                    setText(String.format("%.2f", valorExistente))
                }
                layout.addView(this)
            }
        }
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Métodos de Pagamento")
            .setView(layout)
            .setPositiveButton("OK") { _, _ ->
                Log.d("SettlementFragment", "=== PROCESSANDO MÉTODOS DE PAGAMENTO ===")
                
                paymentValues.clear()
                var totalInformado = 0.0
                var valoresValidos = true
                
                selected.forEach { metodo ->
                    val valorTexto = editTexts[metodo]?.text.toString().trim()
                    val valor = valorTexto.toDoubleOrNull() ?: 0.0
                    
                    Log.d("SettlementFragment", "Método: $metodo - Texto: '$valorTexto' -> Valor: R$ $valor")
                    
                    if (valor < 0) {
                        Log.w("SettlementFragment", "⚠️ Valor negativo detectado para $metodo: R$ $valor")
                        valoresValidos = false
                    }
                    
                    paymentValues[metodo] = valor
                    totalInformado += valor
                }
                
                if (!valoresValidos) {
                    Log.w("SettlementFragment", "⚠️ Alguns valores são inválidos")
                    // Continuar mesmo assim, mas registrar no log
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
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun salvarAcertoComCamposExtras() {
        // Impedir múltiplos cliques
        if (viewModel.isLoading.value) {
            Log.d("SettlementFragment", "Já está salvando, ignorando clique adicional")
            return
        }
        
        // ✅ CORREÇÃO: Validar dados ANTES de desabilitar o botão
        if (!mesasAcertoAdapter.isDataValid()) {
            Toast.makeText(requireContext(), "Verifique os valores das mesas. O relógio final deve ser maior ou igual ao inicial.", Toast.LENGTH_LONG).show()
            return
        }
        
        // Desabilitar botão apenas após validação bem-sucedida
        binding.btnSaveSettlement.isEnabled = false
        viewModel.setLoading(true)

        val valorRecebido = binding.etAmountReceived.text.toString().toDoubleOrNull() ?: 0.0
        val desconto = binding.etDesconto.text.toString().toDoubleOrNull() ?: 0.0
        val observacao = binding.etObservacao.text.toString().trim()
        val panoTrocado = binding.cbPanoTrocado.isChecked
        val numeroPano = if (panoTrocado) binding.etNumeroPano.text.toString() else null
        val tipoAcerto = binding.spTipoAcerto.selectedItem.toString()
        val representante = binding.tvRepresentante.text.toString()

        // ✅ CORREÇÃO: Logs detalhados para debug das observações
        Log.d("SettlementFragment", "=== SALVANDO ACERTO - DEBUG OBSERVAÇÕES ===")
        Log.d("SettlementFragment", "Campo observação (RAW): '${binding.etObservacao.text}'")
        Log.d("SettlementFragment", "Campo observação (TRIM): '$observacao'")
        Log.d("SettlementFragment", "Observação é nula? ${observacao == null}")
        Log.d("SettlementFragment", "Observação é vazia? ${observacao.isEmpty()}")
        Log.d("SettlementFragment", "Observação é blank? ${observacao.isBlank()}")
        Log.d("SettlementFragment", "Tamanho da observação: ${observacao.length}")
        
        // ✅ CORREÇÃO: Garantir que observação não seja nula
        val observacaoFinal = if (observacao.isBlank()) "Acerto realizado via app" else observacao
        Log.d("SettlementFragment", "Observação final que será salva: '$observacaoFinal'")

        // ✅ CORREÇÃO CRÍTICA: Usar dados do adapter como fonte única e confiável
        val mesasDoAcerto = mesasAcertoAdapter.getMesasAcerto().mapIndexed { idx, mesaState ->
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
                fichasInicial = mesaState.relogioInicial,
                fichasFinal = mesaState.relogioFinal,
                valorFixo = mesaOriginal?.valorFixo ?: 0.0,
                tipoMesa = com.example.gestaobilhares.data.entities.TipoMesa.SINUCA,
                comDefeito = mesaState.comDefeito,
                relogioReiniciou = mesaState.relogioReiniciou
            )
        }
        
        Log.d("SettlementFragment", "=== LISTA DE MESAS PARA SALVAR ===")
        Log.d("SettlementFragment", "Total de mesas: ${mesasDoAcerto.size}")
        mesasDoAcerto.forEachIndexed { index, mesa ->
            Log.d("SettlementFragment", "Mesa ${index + 1}: ${mesa.numero} - Valor fixo: R$ ${mesa.valorFixo}")
        }

        val dadosAcerto = SettlementViewModel.DadosAcerto(
            mesas = mesasDoAcerto,
            representante = representante,
            panoTrocado = panoTrocado,
            numeroPano = numeroPano,
            tipoAcerto = tipoAcerto,
            observacao = observacaoFinal, // ✅ CORREÇÃO: Usar observação final
            justificativa = null,
            metodosPagamento = paymentValues
        )

        Log.d("SettlementFragment", "Iniciando salvamento do acerto...")
        Log.d("SettlementFragment", "Desconto aplicado: R$ $desconto")
        Log.d("SettlementFragment", "Observação enviada para ViewModel: '$observacaoFinal'")
        Log.d("SettlementFragment", "Tipo de acerto: $tipoAcerto")
        viewModel.salvarAcerto(
            clienteId = args.clienteId,
            dadosAcerto = dadosAcerto,
            metodosPagamento = paymentValues,
            desconto = desconto
        )
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
                
                Log.d("SettlementFragment", "🔄 Débito anterior atualizado: R$ $debito")
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
                    if (it.isSuccess) {
                        val acertoId = it.getOrNull() ?: return@let
                        Log.d("SettlementFragment", "✅ Acerto salvo com sucesso! ID: $acertoId")
                        

                        
                        mostrarDialogoResumoComAcerto(acertoId)
                    } else {
                        // Em caso de erro, mostrar mensagem
                        val error = it.exceptionOrNull()
                        Log.e("SettlementFragment", "Erro ao salvar acerto: ${error?.message}")
                        Toast.makeText(requireContext(), "Erro ao salvar acerto: ${error?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun mostrarDialogoResumoComAcerto(acertoId: Long) {
        lifecycleScope.launch {
            val acerto = viewModel.buscarAcertoPorId(acertoId)
            if (acerto != null) {
                val mesas = viewModel.buscarMesasDoAcerto(acerto.id)
                val metodosPagamento: Map<String, Double> = acerto.metodosPagamentoJson?.let {
                    Gson().fromJson(it, object : TypeToken<Map<String, Double>>() {}.type)
                } ?: emptyMap()
                
                // ✅ CORREÇÃO: Obter números reais das mesas
                val mesasComNumerosReais = mesas.map { mesaAcerto ->
                    val mesaReal = viewModel.buscarMesaPorId(mesaAcerto.mesaId)
                    Mesa(
                        id = mesaAcerto.mesaId,
                        numero = mesaReal?.numero ?: mesaAcerto.mesaId.toString(),
                        fichasInicial = mesaAcerto.relogioInicial,
                        fichasFinal = mesaAcerto.relogioFinal,
                        valorFixo = mesaAcerto.valorFixo,
                        tipoMesa = com.example.gestaobilhares.data.entities.TipoMesa.SINUCA
                    )
                }
                
                // ✅ NOVO: Obter dados adicionais para o resumo
                val debitoAnterior = viewModel.debitoAnterior.value
                val desconto = binding.etDesconto.text.toString().toDoubleOrNull() ?: 0.0
                
                // ✅ CORREÇÃO: Usar valor total das mesas do banco de dados
                val valorTotalMesas = acerto.valorTotal
                
                val dialog = SettlementSummaryDialog.newInstance(
                    clienteNome = viewModel.clientName.value,
                    mesas = mesasComNumerosReais,
                    total = acerto.valorTotal,
                    metodosPagamento = metodosPagamento,
                    observacao = acerto.observacoes,
                    debitoAtual = acerto.debitoAtual,
                    debitoAnterior = debitoAnterior,
                    desconto = desconto,
                    valorTotalMesas = valorTotalMesas // ✅ CORREÇÃO: Passar valor total das mesas do banco
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
                dialog.show(parentFragmentManager, "SettlementSummaryDialog")
            } else {
                Toast.makeText(requireContext(), "Erro ao carregar acerto salvo", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ✅ REMOVIDO: Função duplicada mostrarDialogoResumo() 
    // Agora usa apenas mostrarDialogoResumoComAcerto() que pega dados reais do banco

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 
