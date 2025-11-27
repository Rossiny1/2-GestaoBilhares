package com.example.gestaobilhares.ui.cycles
import com.example.gestaobilhares.ui.R

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestaobilhares.ui.databinding.FragmentCycleExpensesBinding
import com.example.gestaobilhares.ui.cycles.adapter.CycleExpensesAdapter
import com.example.gestaobilhares.factory.RepositoryFactory
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.util.Date

/**
 * Fragment para listar despesas do ciclo
 */
class CycleExpensesFragment : Fragment() {

    companion object {
        fun newInstance(cicloId: Long, rotaId: Long, isCicloFinalizado: Boolean): CycleExpensesFragment {
            return CycleExpensesFragment().apply {
                arguments = Bundle().apply {
                    putLong("cicloId", cicloId)
                    putLong("rotaId", rotaId)
                    putBoolean("isCicloFinalizado", isCicloFinalizado)
                }
            }
        }
    }

    private var _binding: FragmentCycleExpensesBinding? = null
    private val binding get() = _binding!!
    
    private var cicloId: Long = 0L
    private var rotaId: Long = 0L
    private var isCicloFinalizado: Boolean = false
    
    private lateinit var viewModel: CycleExpensesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cicloId = arguments?.getLong("cicloId", 0L) ?: 0L
        rotaId = arguments?.getLong("rotaId", 0L) ?: 0L
        isCicloFinalizado = arguments?.getBoolean("isCicloFinalizado", false) ?: false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCycleExpensesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // ✅ MIGRADO: Usa AppRepository centralizado
        val appRepository = RepositoryFactory.getAppRepository(requireContext())
        viewModel = CycleExpensesViewModel(appRepository)
        
        setupRecyclerView()
        setupObservers()
        
        // Carregar despesas
        viewModel.carregarDespesas(cicloId)
    }

    private fun setupRecyclerView() {
        binding.rvExpenses.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = CycleExpensesAdapter(
                isCicloFinalizado = isCicloFinalizado,
                onExpenseClick = { despesa ->
                    if (!isCicloFinalizado) {
                        mostrarDialogoEditarDespesa(despesa)
                    }
                },
                onExpenseDelete = { despesa ->
                    // ✅ NOVA LÓGICA: Verificar status real do ciclo antes de permitir exclusão
                    verificarEExcluirDespesa(despesa)
                },
                onViewPhoto = { despesa ->
                    // ✅ NOVO: Visualizar foto do comprovante
                    visualizarFotoComprovante(despesa)
                }
            )
        }
    }
    
    /**
     * ✅ NOVA FUNÇÃO: Verifica o status real do ciclo e permite ou nega exclusão
     */
    private fun verificarEExcluirDespesa(despesa: CycleExpenseItem) {
        lifecycleScope.launch {
            try {
                // Buscar o status real do ciclo
                val cicloReal = viewModel.buscarCicloPorId(cicloId)
                
                if (cicloReal?.status == com.example.gestaobilhares.data.entities.StatusCicloAcerto.EM_ANDAMENTO) {
                    // Ciclo em andamento - permitir exclusão
                    mostrarDialogoConfirmarExclusao(despesa)
                } else {
                    // Ciclo finalizado - mostrar diálogo explicativo
                    mostrarDialogoCicloFinalizado()
                }
            } catch (e: Exception) {
                android.util.Log.e("CycleExpensesFragment", "Erro ao verificar status do ciclo: ${e.message}")
                // Em caso de erro, mostrar diálogo explicativo por segurança
                mostrarDialogoCicloFinalizado()
            }
        }
    }
    
    /**
     * ✅ NOVA FUNÇÃO: Mostra diálogo explicativo para ciclos finalizados
     */
    private fun mostrarDialogoCicloFinalizado() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("❌ Exclusão Não Permitida")
            .setMessage(
                "Não é possível excluir despesas de um ciclo finalizado.\n\n" +
                "• Ciclos finalizados são imutáveis para manter a integridade dos dados\n" +
                "• As despesas fazem parte do histórico oficial do acerto\n" +
                "• Para correções, entre em contato com o administrador\n\n" +
                "Status atual: Ciclo Finalizado"
            )
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Entendi", null)
            .show()
    }

    /**
     * Função pública para ser chamada pelo parent fragment
     */
    fun mostrarDialogoDespesaUnificada() {
        mostrarDialogoDespesa()
    }

    /**
     * Mostra diálogo unificado para editar/adicionar despesa
     * ✅ CORREÇÃO: Usar a mesma tela da Nova Despesa
     */
    private fun mostrarDialogoDespesa(despesa: CycleExpenseItem? = null) {
        if (despesa != null) {
            // ✅ NOVO: Navegar para a tela de Nova Despesa com dados pré-preenchidos
            navegarParaNovaDespesaComEdicao(despesa)
        } else {
            // ✅ NOVO: Navegar para a tela de Nova Despesa para adicionar
            navegarParaNovaDespesa()
        }
    }

    /**
     * ✅ NOVO: Navega para a tela de Nova Despesa para adicionar
     */
    private fun navegarParaNovaDespesa() {
        // Obter rotaId do parent fragment
        val rotaId = (parentFragment as? CycleManagementFragment)?.rotaId ?: 0L
        
        if (rotaId > 0) {
            // ✅ CORREÇÃO: Usar a rota correta do nav_graph
            val action = CycleManagementFragmentDirections.actionCycleManagementFragmentToExpenseRegisterFragment(
                rotaId = rotaId,
                despesaId = 0L,
                modoEdicao = false
            )
            findNavController().navigate(action)
        } else {
            // Fallback se não conseguir obter rotaId
            mostrarDialogoDespesaFallback()
        }
    }

    /**
     * ✅ NOVO: Navega para a tela de Nova Despesa com dados pré-preenchidos para edição
     */
    private fun navegarParaNovaDespesaComEdicao(despesa: CycleExpenseItem) {
        // Obter rotaId do parent fragment
        val rotaId = (parentFragment as? CycleManagementFragment)?.rotaId ?: 0L
        
        if (rotaId > 0) {
            // ✅ CORREÇÃO: Usar a rota correta do nav_graph
            val action = CycleManagementFragmentDirections.actionCycleManagementFragmentToExpenseRegisterFragment(
                rotaId = rotaId,
                despesaId = despesa.id,
                modoEdicao = true
            )
            findNavController().navigate(action)
        } else {
            // Fallback se não conseguir obter rotaId
            mostrarDialogoDespesaFallback(despesa)
        }
    }

    /**
     * ✅ NOVO: Diálogo fallback enquanto a navegação não está implementada
     */
    private fun mostrarDialogoDespesaFallback(despesa: CycleExpenseItem? = null) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(com.example.gestaobilhares.ui.R.layout.dialog_edit_expense, null)
        
        // Campos do diálogo
        val etDescricao = dialogView.findViewById<android.widget.EditText>(com.example.gestaobilhares.ui.R.id.etDescricao)
        val etValor = dialogView.findViewById<android.widget.EditText>(com.example.gestaobilhares.ui.R.id.etValor)
        val etCategoria = dialogView.findViewById<android.widget.EditText>(com.example.gestaobilhares.ui.R.id.etCategoria)
        val etObservacoes = dialogView.findViewById<android.widget.EditText>(com.example.gestaobilhares.ui.R.id.etObservacoes)
        
        // Se for edição, preencher com dados existentes
        if (despesa != null) {
            etDescricao.setText(despesa.descricao)
            etValor.setText(String.format("%.2f", despesa.valor))
            etCategoria.setText(despesa.categoria)
            etObservacoes.setText(despesa.observacoes)
        }
        
        val isEdicao = despesa != null
        val titulo = if (isEdicao) "Editar Despesa" else "Adicionar Nova Despesa"
        val botaoPositivo = if (isEdicao) "Salvar" else "Adicionar"
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(titulo)
            .setView(dialogView)
            .setPositiveButton(botaoPositivo) { _, _ ->
                val descricao = etDescricao.text.toString()
                val valor = etValor.text.toString().toDoubleOrNull() ?: 0.0
                val categoria = etCategoria.text.toString()
                val observacoes = etObservacoes.text.toString()
                
                if (descricao.isNotBlank() && valor > 0) {
                    if (isEdicao) {
                        despesa?.let { despesaNotNull ->
                            viewModel.editarDespesa(despesaNotNull.id, descricao, valor, categoria, observacoes)
                            mostrarFeedback("Despesa editada com sucesso!", Snackbar.LENGTH_SHORT)
                        }
                    } else {
                        // TODO: Implementar adição de despesa
                        mostrarFeedback("Funcionalidade de adicionar despesa será implementada em breve", Snackbar.LENGTH_LONG)
                    }
                } else {
                    mostrarFeedback("Preencha todos os campos obrigatórios", Snackbar.LENGTH_SHORT)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Mostra diálogo para editar despesa (usando função unificada)
     */
    private fun mostrarDialogoEditarDespesa(despesa: CycleExpenseItem) {
        mostrarDialogoDespesa(despesa)
    }

    /**
     * Mostra diálogo para confirmar exclusão de despesa
     */
    private fun mostrarDialogoConfirmarExclusao(despesa: CycleExpenseItem) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Exclusão")
            .setMessage("Tem certeza que deseja excluir a despesa '${despesa.descricao}'?")
            .setPositiveButton("Excluir") { _, _ ->
                viewModel.removerDespesa(despesa.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.despesas.collect { despesas ->
                (binding.rvExpenses.adapter as? CycleExpensesAdapter)?.submitList(despesas)
                atualizarEmptyState(despesas.isEmpty())
            }
        }
        
        lifecycleScope.launch {
            viewModel.isLoading.collect { carregando ->
                binding.progressBar.visibility = if (carregando) View.VISIBLE else View.GONE
            }
        }
        
        lifecycleScope.launch {
            viewModel.errorMessage.collect { mensagem ->
                mensagem?.let {
                    mostrarFeedback("Erro: $it", Snackbar.LENGTH_LONG)
                    viewModel.limparErro()
                }
            }
        }

        // ✅ NOVO: Observer para notificar mudanças ao parent fragment
        lifecycleScope.launch {
            viewModel.despesaModificada.collect { modificada ->
                if (modificada) {
                    // Notificar parent fragment para recarregar estatísticas
                    // TODO: Implementar notificação ao parent fragment
                    viewModel.limparNotificacaoMudanca()
                }
            }
        }
    }

    private fun atualizarEmptyState(mostrar: Boolean) {
        binding.apply {
            if (mostrar) {
                emptyStateLayout.visibility = View.VISIBLE
                rvExpenses.visibility = View.GONE
            } else {
                emptyStateLayout.visibility = View.GONE
                rvExpenses.visibility = View.VISIBLE
            }
        }
    }

    private fun mostrarFeedback(mensagem: String, duracao: Int) {
        Snackbar.make(binding.root, mensagem, duracao)
            .setBackgroundTint(requireContext().getColor(com.example.gestaobilhares.ui.R.color.purple_600))
            .setTextColor(requireContext().getColor(com.example.gestaobilhares.ui.R.color.white))
            .show()
    }

    /**
     * ✅ NOVA FUNCIONALIDADE: Visualizar foto do comprovante da despesa
     */
    private fun visualizarFotoComprovante(despesa: CycleExpenseItem) {
        val caminhoFoto = despesa.fotoComprovante ?: return
        
        try {
            android.util.Log.d("CycleExpensesFragment", "=== VISUALIZANDO FOTO DO COMPROVANTE ===")
            android.util.Log.d("CycleExpensesFragment", "Caminho da foto: $caminhoFoto")

            // ✅ CORREÇÃO: Verificar se é URL do Firebase Storage
            val isFirebaseUrl = caminhoFoto.startsWith("https://") && 
                                (caminhoFoto.contains("firebasestorage.googleapis.com") || 
                                 caminhoFoto.contains("firebase"))
            
            // ✅ CORREÇÃO: PRIORIDADE 1 - Verificar se é arquivo local e existe
            if (!isFirebaseUrl && !caminhoFoto.startsWith("content://")) {
                val file = java.io.File(caminhoFoto)
                if (file.exists() && file.isFile) {
                    android.util.Log.d("CycleExpensesFragment", "✅ Carregando foto local: ${file.absolutePath}")
                    mostrarFotoDialog(file, despesa.dataFotoComprovante)
                    return
                } else {
                    android.util.Log.w("CycleExpensesFragment", "⚠️ Arquivo local não existe: ${file.absolutePath}")
                }
            }
            
            // ✅ CORREÇÃO: PRIORIDADE 2 - Se for URI content://, tentar carregar do content provider
            if (caminhoFoto.startsWith("content://")) {
                try {
                    val uri = android.net.Uri.parse(caminhoFoto)
                    val inputStream = requireContext().contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        android.util.Log.d("CycleExpensesFragment", "✅ Carregando foto do content provider")
                        mostrarFotoDialog(inputStream, despesa.dataFotoComprovante)
                        return
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CycleExpensesFragment", "Erro ao carregar do URI: ${e.message}")
                }
                
                // Tentar converter URI para caminho real
                try {
                    val uri = android.net.Uri.parse(caminhoFoto)
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
                                val file = java.io.File(path)
                                if (file.exists()) {
                                    mostrarFotoDialog(file, despesa.dataFotoComprovante)
                                    return
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CycleExpensesFragment", "Erro ao converter URI: ${e.message}")
                }
            }
            
            // ✅ CORREÇÃO: PRIORIDADE 3 - Se for URL do Firebase Storage, fazer download
            if (isFirebaseUrl) {
                android.util.Log.d("CycleExpensesFragment", "Detectada URL do Firebase Storage, fazendo download...")
                lifecycleScope.launch {
                    try {
                        val bitmap = downloadImageFromUrl(caminhoFoto)
                        if (bitmap != null) {
                            mostrarFotoDialog(bitmap, despesa.dataFotoComprovante)
                            android.util.Log.d("CycleExpensesFragment", "✅ Foto carregada do Firebase Storage")
                        } else {
                            android.widget.Toast.makeText(
                                requireContext(),
                                "Erro ao carregar foto do servidor",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("CycleExpensesFragment", "Erro ao fazer download da foto: ${e.message}")
                        android.widget.Toast.makeText(
                            requireContext(),
                            "Erro ao carregar foto: ${e.message}",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
                return
            }
            
            // Se chegou aqui, não conseguiu carregar a foto
            android.util.Log.e("CycleExpensesFragment", "❌ Não foi possível carregar a foto: $caminhoFoto")
            android.widget.Toast.makeText(
                requireContext(),
                "Arquivo de foto não encontrado: $caminhoFoto",
                android.widget.Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            android.util.Log.e("CycleExpensesFragment", "Erro ao visualizar foto: ${e.message}")
            android.widget.Toast.makeText(
                requireContext(),
                "Erro ao visualizar foto: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    /**
     * ✅ NOVO: Faz download de imagem de uma URL
     */
    private suspend fun downloadImageFromUrl(urlString: String): android.graphics.Bitmap? {
        return try {
            val url = java.net.URL(urlString)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.doInput = true
            connection.connect()
            
            val inputStream = connection.inputStream
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            connection.disconnect()
            bitmap
        } catch (e: Exception) {
            android.util.Log.e("CycleExpensesFragment", "Erro ao fazer download da imagem: ${e.message}", e)
            null
        }
    }
    
    /**
     * ✅ NOVO: Método para mostrar o diálogo da foto a partir de um Bitmap
     */
    private fun mostrarFotoDialog(bitmap: android.graphics.Bitmap, dataFoto: Date?) {
        try {
            val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .create()

            val layout = android.widget.LinearLayout(requireContext()).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(32, 32, 32, 32)
            }

            val imageView = android.widget.ImageView(requireContext()).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = true
                maxHeight = 800
            }
            
            imageView.setImageBitmap(bitmap)

            val textView = android.widget.TextView(requireContext()).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 16
                }
                text = "Foto do Comprovante"
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = android.view.Gravity.CENTER
            }

            val dataTextView = android.widget.TextView(requireContext()).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 8
                }
                text = dataFoto?.let {
                    "Capturada em: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("pt", "BR")).format(it)}"
                } ?: "Data não disponível"
                textSize = 12f
                gravity = android.view.Gravity.CENTER
            }

            layout.addView(imageView)
            layout.addView(textView)
            layout.addView(dataTextView)

            dialog.setView(layout)
            dialog.setTitle("Foto do Comprovante")
            dialog.setButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE, "Fechar") { _, _ ->
                dialog.dismiss()
            }
            dialog.show()
        } catch (e: Exception) {
            android.util.Log.e("CycleExpensesFragment", "Erro ao mostrar diálogo da foto: ${e.message}", e)
            android.widget.Toast.makeText(
                requireContext(),
                "Erro ao exibir foto: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    /**
     * ✅ NOVO: Método para mostrar o diálogo da foto a partir de um arquivo
     */
    private fun mostrarFotoDialog(file: java.io.File, dataFoto: Date?) {
        try {
            val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .create()

            val layout = android.widget.LinearLayout(requireContext()).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(32, 32, 32, 32)
            }

            val imageView = android.widget.ImageView(requireContext()).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = true
                maxHeight = 800
            }

            try {
                val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                } else {
                    imageView.setImageResource(com.example.gestaobilhares.ui.R.drawable.ic_camera)
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Erro ao carregar a foto",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("CycleExpensesFragment", "Erro ao decodificar bitmap: ${e.message}")
                imageView.setImageResource(com.example.gestaobilhares.ui.R.drawable.ic_camera)
            }

            layout.addView(imageView)

            // Adicionar data da foto se disponível
            if (dataFoto != null) {
                val dateFormatter = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("pt", "BR"))
                val tvData = android.widget.TextView(requireContext()).apply {
                    text = "Data: ${dateFormatter.format(dataFoto)}"
                    textSize = 12f
                    setPadding(0, 16, 0, 0)
                    setTextColor(requireContext().getColor(com.example.gestaobilhares.ui.R.color.text_secondary))
                }
                layout.addView(tvData)
            }

            dialog.setView(layout)
            dialog.setTitle("Foto do Comprovante")
            dialog.setButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE, "Fechar") { _, _ ->
                dialog.dismiss()
            }
            dialog.show()

        } catch (e: Exception) {
            android.util.Log.e("CycleExpensesFragment", "Erro ao criar diálogo: ${e.message}")
            android.widget.Toast.makeText(
                requireContext(),
                "Erro ao exibir foto: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    /**
     * ✅ NOVO: Método para mostrar o diálogo da foto a partir de um InputStream
     */
    private fun mostrarFotoDialog(inputStream: java.io.InputStream, dataFoto: Date?) {
        try {
            val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .create()

            val layout = android.widget.LinearLayout(requireContext()).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(32, 32, 32, 32)
            }

            val imageView = android.widget.ImageView(requireContext()).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = true
                maxHeight = 800
            }

            try {
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                } else {
                    imageView.setImageResource(com.example.gestaobilhares.ui.R.drawable.ic_camera)
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Erro ao carregar a foto",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("CycleExpensesFragment", "Erro ao decodificar bitmap: ${e.message}")
                imageView.setImageResource(com.example.gestaobilhares.ui.R.drawable.ic_camera)
            } finally {
                inputStream.close()
            }

            layout.addView(imageView)

            // Adicionar data da foto se disponível
            if (dataFoto != null) {
                val dateFormatter = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("pt", "BR"))
                val tvData = android.widget.TextView(requireContext()).apply {
                    text = "Data: ${dateFormatter.format(dataFoto)}"
                    textSize = 12f
                    setPadding(0, 16, 0, 0)
                    setTextColor(requireContext().getColor(com.example.gestaobilhares.ui.R.color.text_secondary))
                }
                layout.addView(tvData)
            }

            dialog.setView(layout)
            dialog.setTitle("Foto do Comprovante")
            dialog.setButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE, "Fechar") { _, _ ->
                dialog.dismiss()
            }
            dialog.show()

        } catch (e: Exception) {
            android.util.Log.e("CycleExpensesFragment", "Erro ao criar diálogo: ${e.message}")
            android.widget.Toast.makeText(
                requireContext(),
                "Erro ao exibir foto: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}

