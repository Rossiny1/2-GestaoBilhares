package com.example.gestaobilhares.ui.contracts

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.R
import com.example.gestaobilhares.databinding.FragmentContractManagementBinding
import com.example.gestaobilhares.data.entities.ContratoLocacao
import com.example.gestaobilhares.data.entities.AditivoContrato
import com.example.gestaobilhares.data.entities.Cliente
import com.example.gestaobilhares.data.entities.Rota
import com.example.gestaobilhares.data.entities.Mesa
import com.example.gestaobilhares.ui.contracts.ContractManagementAdapter
import com.example.gestaobilhares.ui.contracts.ContractManagementViewModel
import com.example.gestaobilhares.utils.ContractPdfGenerator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.repository.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment para gerenciamento de contratos
 * Permite visualizar, filtrar e gerenciar todos os contratos de locação
 */
@AndroidEntryPoint
class ContractManagementFragment : Fragment() {

    private var _binding: FragmentContractManagementBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ContractManagementViewModel by viewModels()
    private lateinit var contractAdapter: ContractManagementAdapter

    @Inject
    lateinit var database: AppDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContractManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupClickListeners()
        setupFilters()
        observeViewModel()
        
        // Carregar dados iniciais
        viewModel.loadContractData()
    }

    private fun setupRecyclerView() {
        contractAdapter = ContractManagementAdapter(
            onContractClick = { item ->
                // Abrir opções: contrato, aditivo de inclusão/retirada, distrato
                showContractOrAddendumOptions(item)
            },
            onViewClick = { item ->
                // ✅ NOVO: Mostrar diálogo com lista de documentos
                showDocumentsDialog(item)
            }
        )

        binding.rvContracts.apply {
            adapter = contractAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupClickListeners() {
        // Botão voltar
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Botão buscar
        binding.fabSearch.setOnClickListener {
            showSearchDialog()
        }

        // Botão filtrar
        binding.fabFilter.setOnClickListener {
            showFilterDialog()
        }

        // Botão assinatura do representante legal
        binding.btnAssinaturaRepresentante.setOnClickListener {
            findNavController().navigate(R.id.action_contractManagementFragment_to_representanteLegalSignatureFragment)
        }

    }

    /**
     * ✅ NOVO: Mostra diálogo com lista de documentos (contratos, aditivos, distratos)
     * Ordenados do mais recente para o mais antigo
     */
    private fun showDocumentsDialog(item: ContractManagementViewModel.ContractItem) {
        val contrato = item.contrato ?: return
        val aditivos = item.aditivos
        
        // Criar lista de documentos ordenados por data (mais recente primeiro)
        val documentos = mutableListOf<DocumentoItem>()
        
        // Adicionar contrato principal
        documentos.add(DocumentoItem(
            tipo = "CONTRATO",
            titulo = "Contrato ${contrato.numeroContrato}",
            data = contrato.dataCriacao,
            contrato = contrato,
            aditivo = null
        ))
        
        // Adicionar aditivos ordenados por data
        aditivos.sortedByDescending { it.dataCriacao }.forEach { aditivo ->
            val tipoAditivo = if (aditivo.tipo.equals("RETIRADA", ignoreCase = true)) "ADITIVO (RETIRADA)" else "ADITIVO (INCLUSÃO)"
            documentos.add(DocumentoItem(
                tipo = tipoAditivo,
                titulo = "Aditivo ${aditivo.numeroAditivo}",
                data = aditivo.dataCriacao,
                contrato = contrato,
                aditivo = aditivo
            ))
        }
        
        // Verificar se há distrato (contrato encerrado)
        if (contrato.status == "ENCERRADO_QUITADO" || contrato.status == "RESCINDIDO_COM_DIVIDA") {
            documentos.add(DocumentoItem(
                tipo = "DISTRATO",
                titulo = "Distrato ${contrato.numeroContrato}",
                data = contrato.dataEncerramento ?: contrato.dataAtualizacao,
                contrato = contrato,
                aditivo = null
            ))
        }
        
        // ✅ NOVO: Criar diálogo customizado com RecyclerView
        showCustomDocumentsDialog(item.cliente?.nome ?: "Cliente", documentos)
    }

    /**
     * ✅ NOVO: Mostra diálogo customizado com RecyclerView para documentos
     */
    private fun showCustomDocumentsDialog(clienteNome: String, documentos: List<DocumentoItem>) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_documentos_lista, null)
        
        val recyclerView = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvDocumentos)
        val adapter = DocumentosListaAdapter(documentos) { documento ->
            showDocumentActionsDialog(documento)
        }
        
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Documentos - $clienteNome")
            .setView(dialogView)
            .setNegativeButton("Fechar", null)
            .show()
    }

    /**
     * ✅ NOVO: Mostra diálogo com ações do documento (visualizar/compartilhar)
     */
    private fun showDocumentActionsDialog(documento: DocumentoItem) {
        val actions = arrayOf("Visualizar", "Compartilhar")
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(documento.titulo)
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> { // Visualizar
                        when (documento.tipo) {
                            "CONTRATO" -> viewContract(documento.contrato)
                            "ADITIVO (INCLUSÃO)", "ADITIVO (RETIRADA)" -> {
                                documento.aditivo?.let { aditivo ->
                                    viewAddendum(ContractManagementViewModel.ContractItem(
                                        contrato = documento.contrato,
                                        cliente = null,
                                        rota = null,
                                        mesas = emptyList(),
                                        aditivos = listOf(aditivo),
                                        status = "",
                                        aditivosRetiradaCount = 0,
                                        hasDistrato = false
                                    ))
                                }
                            }
                            "DISTRATO" -> viewDistrato(ContractManagementViewModel.ContractItem(
                                contrato = documento.contrato,
                                cliente = null,
                                rota = null,
                                mesas = emptyList(),
                                aditivos = emptyList(),
                                status = "",
                                aditivosRetiradaCount = 0,
                                hasDistrato = true
                            ))
                        }
                    }
                    1 -> { // Compartilhar
                        when (documento.tipo) {
                            "CONTRATO" -> shareContract(documento.contrato)
                            "ADITIVO (INCLUSÃO)", "ADITIVO (RETIRADA)" -> {
                                documento.aditivo?.let { aditivo ->
                                    shareAddendum(ContractManagementViewModel.ContractItem(
                                        contrato = documento.contrato,
                                        cliente = null,
                                        rota = null,
                                        mesas = emptyList(),
                                        aditivos = listOf(aditivo),
                                        status = "",
                                        aditivosRetiradaCount = 0,
                                        hasDistrato = false
                                    ))
                                }
                            }
                            "DISTRATO" -> shareDistrato(ContractManagementViewModel.ContractItem(
                                contrato = documento.contrato,
                                cliente = null,
                                rota = null,
                                mesas = emptyList(),
                                aditivos = emptyList(),
                                status = "",
                                aditivosRetiradaCount = 0,
                                hasDistrato = true
                            ))
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * ✅ NOVO: Classe para representar um documento
     */
    data class DocumentoItem(
        val tipo: String,
        val titulo: String,
        val data: Date,
        val contrato: ContratoLocacao,
        val aditivo: AditivoContrato?
    )

    /**
     * ✅ NOVO: Adapter para lista de documentos no diálogo
     */
    private inner class DocumentosListaAdapter(
        private val documentos: List<DocumentoItem>,
        private val onItemClick: (DocumentoItem) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<DocumentosListaAdapter.DocumentoViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentoViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_documento_dialog, parent, false)
            return DocumentoViewHolder(view)
        }

        override fun onBindViewHolder(holder: DocumentoViewHolder, position: Int) {
            holder.bind(documentos[position])
        }

        override fun getItemCount() = documentos.size

        inner class DocumentoViewHolder(itemView: android.view.View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
            private val tvTitulo = itemView.findViewById<android.widget.TextView>(R.id.tvDocumentoTitulo)
            private val tvTipo = itemView.findViewById<android.widget.TextView>(R.id.tvDocumentoTipo)
            private val tvStatus = itemView.findViewById<android.widget.TextView>(R.id.tvDocumentoStatus)
            private val tvData = itemView.findViewById<android.widget.TextView>(R.id.tvDocumentoData)

            fun bind(documento: DocumentoItem) {
                // Configurar título
                tvTitulo.text = documento.titulo

                // Configurar tipo com cores diferentes
                tvTipo.text = documento.tipo
                when (documento.tipo) {
                    "CONTRATO" -> {
                        tvTipo.setBackgroundColor(itemView.context.getColor(R.color.blue_600))
                        tvTipo.setTextColor(itemView.context.getColor(R.color.white))
                    }
                    "ADITIVO (INCLUSÃO)" -> {
                        tvTipo.setBackgroundColor(itemView.context.getColor(R.color.green_600))
                        tvTipo.setTextColor(itemView.context.getColor(R.color.white))
                    }
                    "ADITIVO (RETIRADA)" -> {
                        tvTipo.setBackgroundColor(itemView.context.getColor(R.color.orange_600))
                        tvTipo.setTextColor(itemView.context.getColor(R.color.white))
                    }
                    "DISTRATO" -> {
                        tvTipo.setBackgroundColor(itemView.context.getColor(R.color.red_600))
                        tvTipo.setTextColor(itemView.context.getColor(R.color.white))
                    }
                }

                // Configurar status
                val status = when (documento.contrato.status.uppercase()) {
                    "ATIVO" -> if (documento.contrato.assinaturaLocatario != null) "(Assinado)" else "(Gerado)"
                    "ENCERRADO_QUITADO" -> "(Inativo)"
                    "RESCINDIDO_COM_DIVIDA" -> "(Inativo com dívida)"
                    "RESCINDIDO" -> "(Inativo)"
                    else -> "(${documento.contrato.status})"
                }
                tvStatus.text = status

                // Configurar data
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                tvData.text = dateFormat.format(documento.data)

                // Configurar clique
                itemView.setOnClickListener { onItemClick(documento) }
            }
        }
    }


    private fun setupFilters() {
        // Configurar chips de filtro
        binding.chipComContrato.setOnClickListener {
            viewModel.setFilter(ContractManagementViewModel.ContractFilter.WITH_CONTRACT)
            updateFilterChips(ContractManagementViewModel.ContractFilter.WITH_CONTRACT)
        }

        binding.chipSemContrato.setOnClickListener {
            viewModel.setFilter(ContractManagementViewModel.ContractFilter.WITHOUT_CONTRACT)
            updateFilterChips(ContractManagementViewModel.ContractFilter.WITHOUT_CONTRACT)
        }
    }

    private fun updateFilterChips(filter: ContractManagementViewModel.ContractFilter) {
        binding.chipComContrato.isChecked = filter == ContractManagementViewModel.ContractFilter.WITH_CONTRACT
        binding.chipSemContrato.isChecked = filter == ContractManagementViewModel.ContractFilter.WITHOUT_CONTRACT
    }

    private fun observeViewModel() {
        // Observar estatísticas
        viewModel.statistics.observe(viewLifecycleOwner) { stats ->
            binding.tvTotalClientes.text = stats.totalClientes.toString()
            binding.tvContratosGerados.text = stats.contratosGerados.toString()
            binding.tvContratosAssinados.text = stats.contratosAssinados.toString()
        }

        // Observar lista de contratos
        viewModel.contracts.observe(viewLifecycleOwner) { contracts ->
            contractAdapter.submitList(contracts)
            
            // Mostrar/ocultar estado vazio
            if (contracts.isEmpty()) {
                binding.layoutEmpty.visibility = View.VISIBLE
                binding.rvContracts.visibility = View.GONE
            } else {
                binding.layoutEmpty.visibility = View.GONE
                binding.rvContracts.visibility = View.VISIBLE
            }
        }

        // Observar loading
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Implementar loading se necessário
        }
    }

    private fun navigateToContractDetails(contrato: ContratoLocacao?) {
        Toast.makeText(requireContext(), "Detalhes do contrato ${contrato?.numeroContrato}", Toast.LENGTH_SHORT).show()
    }

    private fun showContractOrAddendumOptions(item: ContractManagementViewModel.ContractItem) {
        val hasAddenda = item.aditivos.isNotEmpty()
        val hasRetirada = item.aditivosRetiradaCount > 0
        val hasDistrato = item.hasDistrato
        val options = buildList {
            add("Visualizar Contrato")
            add("Compartilhar Contrato")
            if (hasAddenda) { add("Visualizar Aditivo mais recente"); add("Compartilhar Aditivo mais recente") }
            if (hasRetirada) { add("Visualizar Aditivo de Retirada mais recente"); add("Compartilhar Aditivo de Retirada mais recente") }
            if (hasDistrato) { add("Visualizar Distrato"); add("Compartilhar Distrato") }
        }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Ações")
            .setItems(options) { _, which ->
                var idx = 0
                when (which) {
                    idx++ -> viewContract(item.contrato)
                    idx++ -> shareContract(item.contrato)
                    idx++ -> if (hasAddenda) viewAddendum(item) else return@setItems
                    idx++ -> if (hasAddenda) shareAddendum(item) else return@setItems
                    idx++ -> if (hasRetirada) viewAddendumRetirada(item) else return@setItems
                    idx++ -> if (hasRetirada) shareAddendumRetirada(item) else return@setItems
                    idx -> if (hasDistrato) viewDistrato(item) else return@setItems
                    else -> shareDistrato(item)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun viewContract(contrato: ContratoLocacao?) {
        // Visualizar contrato
        lifecycleScope.launch {
            try {
                if (contrato == null) {
                    Toast.makeText(requireContext(), "Contrato não encontrado", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // Gerar PDF do contrato para visualização
                val contractPdfGenerator = ContractPdfGenerator(requireContext())
                val mesas = contrato?.let { 
                    viewModel.getMesasPorCliente(it.clienteId)
                } ?: emptyList()
                
                // ✅ NOVO: Obter assinatura do representante legal automaticamente
                val assinaturaRepresentante = viewModel.obterAssinaturaRepresentanteLegalAtiva()
                
                val pdfFile = contractPdfGenerator.generateContractPdf(contrato, mesas, assinaturaRepresentante)
                
                if (pdfFile.exists()) {
                    // Tentar abrir PDF com visualizador padrão
                    val uri = FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.fileprovider",
                        pdfFile
                    )
                    
                    // Intent para visualizar PDF
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    
                    // Verificar se há apps que podem abrir PDF
                    val packageManager = requireContext().packageManager
                    val resolveInfo = packageManager.queryIntentActivities(intent, 0)
                    
                    if (resolveInfo.isNotEmpty()) {
                        startActivity(intent)
                    } else {
                        // Se não houver visualizador de PDF, tentar abrir com qualquer app
                        val genericIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "*/*")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        
                        if (genericIntent.resolveActivity(packageManager) != null) {
                            startActivity(genericIntent)
                        } else {
                            // Como último recurso, compartilhar o arquivo
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_SUBJECT, "Contrato ${contrato.numeroContrato}")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            
                            if (shareIntent.resolveActivity(packageManager) != null) {
                                startActivity(Intent.createChooser(shareIntent, "Abrir contrato com"))
                            } else {
                                Toast.makeText(requireContext(), "Nenhum app encontrado para abrir PDF. O arquivo foi salvo em: ${pdfFile.absolutePath}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Erro ao gerar PDF do contrato", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro ao visualizar contrato: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun viewAddendum(item: ContractManagementViewModel.ContractItem) {
        lifecycleScope.launch {
            try {
                val contrato = item.contrato
                if (contrato == null) {
                    Toast.makeText(requireContext(), "Contrato não encontrado", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                if (item.aditivos.isEmpty()) {
                    Toast.makeText(requireContext(), "Nenhum aditivo para este contrato", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val aditivo = item.aditivos.maxByOrNull { it.dataCriacao.time } ?: item.aditivos.last()
                val mesas = viewModel.getMesasPorCliente(contrato.clienteId)
                
                // ✅ NOVO: Obter assinatura do representante legal automaticamente
                val assinaturaRepresentante = viewModel.obterAssinaturaRepresentanteLegalAtiva()
                
                val pdf = com.example.gestaobilhares.utils.AditivoPdfGenerator(requireContext()).generateAditivoPdf(aditivo, contrato, mesas, assinaturaRepresentante)
                if (!pdf.exists()) {
                    Toast.makeText(requireContext(), "Erro ao gerar PDF do aditivo", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", pdf)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val pm = requireContext().packageManager
                if (intent.resolveActivity(pm) != null) startActivity(intent) else Toast.makeText(requireContext(), "Nenhum app para abrir PDF", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro ao visualizar aditivo: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun shareAddendum(item: ContractManagementViewModel.ContractItem) {
        lifecycleScope.launch {
            try {
                val contrato = item.contrato
                if (contrato == null) {
                    Toast.makeText(requireContext(), "Contrato não encontrado", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                if (item.aditivos.isEmpty()) {
                    Toast.makeText(requireContext(), "Nenhum aditivo para este contrato", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val aditivo = item.aditivos.maxByOrNull { it.dataCriacao.time } ?: item.aditivos.last()
                val mesas = viewModel.getMesasPorCliente(contrato.clienteId)
                
                // ✅ NOVO: Obter assinatura do representante legal automaticamente
                val assinaturaRepresentante = viewModel.obterAssinaturaRepresentanteLegalAtiva()
                
                val pdf = com.example.gestaobilhares.utils.AditivoPdfGenerator(requireContext()).generateAditivoPdf(aditivo, contrato, mesas, assinaturaRepresentante)
                if (!pdf.exists()) {
                    Toast.makeText(requireContext(), "Erro ao gerar PDF do aditivo", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", pdf)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Aditivo ${aditivo.numeroAditivo}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Compartilhar aditivo"))
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro ao compartilhar aditivo: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun viewAddendumRetirada(item: ContractManagementViewModel.ContractItem) {
        // Reutiliza o mesmo gerador do aditivo; filtra pelo tipo RETIRADA
        lifecycleScope.launch {
            val contrato = item.contrato ?: return@launch
            val aditivo = item.aditivos.filter { it.tipo.equals("RETIRADA", true) }.maxByOrNull { it.dataCriacao.time } ?: return@launch
            val mesas = viewModel.getMesasPorCliente(contrato.clienteId)
            
            // ✅ NOVO: Obter assinatura do representante legal automaticamente
            val assinaturaRepresentante = viewModel.obterAssinaturaRepresentanteLegalAtiva()
            
            val pdf = com.example.gestaobilhares.utils.AditivoPdfGenerator(requireContext()).generateAditivoPdf(aditivo, contrato, mesas, assinaturaRepresentante)
            if (!pdf.exists()) {
                Toast.makeText(requireContext(), "Erro ao gerar PDF do aditivo de retirada", Toast.LENGTH_SHORT).show(); return@launch
            }
            val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", pdf)
            val intent = Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "application/pdf"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            if (intent.resolveActivity(requireContext().packageManager) != null) startActivity(intent) else Toast.makeText(requireContext(), "Nenhum app para abrir PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareAddendumRetirada(item: ContractManagementViewModel.ContractItem) {
        lifecycleScope.launch {
            val contrato = item.contrato ?: return@launch
            val aditivo = item.aditivos.filter { it.tipo.equals("RETIRADA", true) }.maxByOrNull { it.dataCriacao.time } ?: return@launch
            val mesas = viewModel.getMesasPorCliente(contrato.clienteId)
            
            // ✅ NOVO: Obter assinatura do representante legal automaticamente
            val assinaturaRepresentante = viewModel.obterAssinaturaRepresentanteLegalAtiva()
            
            val pdf = com.example.gestaobilhares.utils.AditivoPdfGenerator(requireContext()).generateAditivoPdf(aditivo, contrato, mesas, assinaturaRepresentante)
            if (!pdf.exists()) { Toast.makeText(requireContext(), "Erro ao gerar PDF", Toast.LENGTH_SHORT).show(); return@launch }
            val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", pdf)
            val shareIntent = Intent(Intent.ACTION_SEND).apply { type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri); putExtra(Intent.EXTRA_SUBJECT, "Aditivo ${aditivo.numeroAditivo}"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            startActivity(Intent.createChooser(shareIntent, "Compartilhar aditivo de retirada"))
        }
    }

    private fun viewDistrato(item: ContractManagementViewModel.ContractItem) {
        lifecycleScope.launch {
            val contrato = item.contrato ?: return@launch
            val mesas = viewModel.getMesasPorCliente(contrato.clienteId)
            // Fechamento mínimo derivado do último acerto
            val db = com.example.gestaobilhares.data.database.AppDatabase.getDatabase(requireContext())
            val repo = com.example.gestaobilhares.data.repository.AppRepository(
                db.clienteDao(), db.acertoDao(), db.mesaDao(), db.rotaDao(), db.despesaDao(),
                db.colaboradorDao(), db.cicloAcertoDao(), db.acertoMesaDao(), db.contratoLocacaoDao(), db.aditivoContratoDao(),
                db.assinaturaRepresentanteLegalDao(), db.logAuditoriaAssinaturaDao(), db.procuraçãoRepresentanteDao()
            )
            val ultimo = repo.buscarUltimoAcertoPorCliente(contrato.clienteId)
            val totalRecebido = ultimo?.valorRecebido ?: 0.0
            val despesasViagem = 0.0
            val subtotal = totalRecebido - despesasViagem
            val comissaoMotorista = subtotal * 0.03
            val comissaoIltair = totalRecebido * 0.02
            val totalGeral = subtotal - comissaoMotorista - comissaoIltair
            val saldo = ultimo?.debitoAtual ?: 0.0
            val fechamento = com.example.gestaobilhares.utils.ContractPdfGenerator.FechamentoResumo(totalRecebido, despesasViagem, subtotal, comissaoMotorista, comissaoIltair, totalGeral, saldo)
            
            // ✅ NOVO: Obter assinatura do representante legal automaticamente
            val assinaturaRepresentante = viewModel.obterAssinaturaRepresentanteLegalAtiva()
            
            val pdf = com.example.gestaobilhares.utils.ContractPdfGenerator(requireContext()).generateDistratoPdf(contrato, mesas, fechamento, if (saldo > 0.0) Pair(saldo, java.util.Date()) else null, assinaturaRepresentante)
            val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", pdf)
            val intent = Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "application/pdf"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            if (intent.resolveActivity(requireContext().packageManager) != null) startActivity(intent) else Toast.makeText(requireContext(), "Nenhum app para abrir PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareDistrato(item: ContractManagementViewModel.ContractItem) {
        lifecycleScope.launch {
            val contrato = item.contrato ?: return@launch
            val mesas = viewModel.getMesasPorCliente(contrato.clienteId)
            val db = com.example.gestaobilhares.data.database.AppDatabase.getDatabase(requireContext())
            val repo = com.example.gestaobilhares.data.repository.AppRepository(
                db.clienteDao(), db.acertoDao(), db.mesaDao(), db.rotaDao(), db.despesaDao(),
                db.colaboradorDao(), db.cicloAcertoDao(), db.acertoMesaDao(), db.contratoLocacaoDao(), db.aditivoContratoDao(),
                db.assinaturaRepresentanteLegalDao(), db.logAuditoriaAssinaturaDao(), db.procuraçãoRepresentanteDao()
            )
            val ultimo = repo.buscarUltimoAcertoPorCliente(contrato.clienteId)
            val totalRecebido = ultimo?.valorRecebido ?: 0.0
            val despesasViagem = 0.0
            val subtotal = totalRecebido - despesasViagem
            val comissaoMotorista = subtotal * 0.03
            val comissaoIltair = totalRecebido * 0.02
            val totalGeral = subtotal - comissaoMotorista - comissaoIltair
            val saldo = ultimo?.debitoAtual ?: 0.0
            val fechamento = com.example.gestaobilhares.utils.ContractPdfGenerator.FechamentoResumo(totalRecebido, despesasViagem, subtotal, comissaoMotorista, comissaoIltair, totalGeral, saldo)
            
            // ✅ NOVO: Obter assinatura do representante legal automaticamente
            val assinaturaRepresentante = viewModel.obterAssinaturaRepresentanteLegalAtiva()
            
            val pdf = com.example.gestaobilhares.utils.ContractPdfGenerator(requireContext()).generateDistratoPdf(contrato, mesas, fechamento, if (saldo > 0.0) Pair(saldo, java.util.Date()) else null, assinaturaRepresentante)
            val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", pdf)
            val shareIntent = Intent(Intent.ACTION_SEND).apply { type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri); putExtra(Intent.EXTRA_SUBJECT, "Distrato ${contrato.numeroContrato}"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            startActivity(Intent.createChooser(shareIntent, "Compartilhar distrato"))
        }
    }

    private fun shareContract(contrato: ContratoLocacao?) {
        // Compartilhar contrato
        lifecycleScope.launch {
            try {
                if (contrato == null) {
                    Toast.makeText(requireContext(), "Contrato não encontrado", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // Gerar PDF do contrato para compartilhamento
                val contractPdfGenerator = ContractPdfGenerator(requireContext())
                val mesas = contrato?.let { 
                    viewModel.getMesasPorCliente(it.clienteId)
                } ?: emptyList()
                
                // ✅ NOVO: Obter assinatura do representante legal automaticamente
                val assinaturaRepresentante = viewModel.obterAssinaturaRepresentanteLegalAtiva()
                
                val pdfFile = contractPdfGenerator.generateContractPdf(contrato, mesas, assinaturaRepresentante)
                
                if (pdfFile.exists()) {
                    // Compartilhar PDF via WhatsApp ou outros apps
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                            requireContext(),
                            "${requireContext().packageName}.fileprovider",
                            pdfFile
                        ))
                        putExtra(Intent.EXTRA_SUBJECT, "Contrato ${contrato.numeroContrato}")
                        putExtra(Intent.EXTRA_TEXT, "Contrato de locação - ${contrato.numeroContrato}")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    
                    val chooserIntent = Intent.createChooser(shareIntent, "Compartilhar contrato")
                    if (shareIntent.resolveActivity(requireContext().packageManager) != null) {
                        startActivity(chooserIntent)
                    } else {
                        Toast.makeText(requireContext(), "Nenhum app de compartilhamento encontrado", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Erro ao gerar PDF do contrato", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro ao compartilhar contrato: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showSearchDialog() {
        // Implementar diálogo de busca
        val editText = TextInputEditText(requireContext()).apply {
            hint = "Digite o nome do cliente ou número do contrato"
            setPadding(50, 30, 50, 30)
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Buscar Contratos")
            .setView(editText)
            .setPositiveButton("Buscar") { _, _ ->
                val searchQuery = editText.text?.toString()?.trim()
                if (!searchQuery.isNullOrEmpty()) {
                    viewModel.searchContracts(searchQuery)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showFilterDialog() {
        // Implementar diálogo de filtros avançados
        val filterOptions = arrayOf("Com Contrato", "Sem Contrato", "Por Rota")
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Filtros Avançados")
            .setItems(filterOptions) { _, which ->
                when (which) {
                    0 -> viewModel.setFilter(ContractManagementViewModel.ContractFilter.WITH_CONTRACT)
                    1 -> viewModel.setFilter(ContractManagementViewModel.ContractFilter.WITHOUT_CONTRACT)
                    2 -> showRouteFilterDialog()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun showRouteFilterDialog() {
        // Implementar filtro por rota
        lifecycleScope.launch {
            try {
                val rotas = viewModel.getAllRoutes()
                val routeNames = rotas.map { it.nome }.toTypedArray()
                
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Filtrar por Rota")
                    .setItems(routeNames) { _, which ->
                        val rotaSelecionada = rotas[which]
                        viewModel.setFilterByRoute(rotaSelecionada.id)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro ao carregar rotas: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
