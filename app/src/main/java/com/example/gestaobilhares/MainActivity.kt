package com.example.gestaobilhares

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import com.example.gestaobilhares.ui.databinding.ActivityMainBinding
import com.example.gestaobilhares.core.utils.NetworkUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
// ✅ REMOVIDO: import DatabasePopulator - não é mais necessário
// import dagger.hilt.android.AndroidEntryPoint // REMOVIDO: Hilt nao e mais usado
// ✅ REMOVIDO: imports CoroutineScope - não são mais necessários
/**
 * MainActivity configurada para Navigation Component e ViewBinding.
 * Usando NoActionBar theme - navegação gerenciada pelos próprios fragments.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var isSyncingOnExit = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Navigation Controller configurado automaticamente - gerenciado pelos fragments
        // REMOVIDO: setupActionBarWithNavController() - incompatível com NoActionBar theme

        // ✅ REMOVIDO: Popular banco de dados - agora é feito automaticamente no AppDatabase
        // popularBancoDados()
        
        // ✅ CORREÇÃO: Gerenciar botão voltar globalmente
        // Mostra diálogo de saída apenas na tela de rotas, caso contrário deixa Navigation Component gerenciar
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val navHostFragment = supportFragmentManager.findFragmentById(
                    com.example.gestaobilhares.ui.R.id.nav_host_fragment
                ) as? NavHostFragment ?: run {
                    finish()
                    return
                }
                val navController = navHostFragment.navController
                
                // ✅ Verificar se estamos na tela de rotas
                // Se o destino atual é routesFragment, mostrar diálogo de saída
                // Caso contrário, deixar Navigation Component gerenciar o comportamento padrão
                val currentDestinationId = navController.currentDestination?.id
                val isRoutesScreen = currentDestinationId == com.example.gestaobilhares.ui.R.id.routesFragment
                
                if (isRoutesScreen) {
                    // Estamos na tela de rotas - mostrar diálogo de saída
                    showExitConfirmationDialog()
                } else {
                    // Não estamos na tela de rotas - deixar Navigation Component gerenciar normalmente
                    // O Navigation Component automaticamente volta para a tela anterior no back stack
                    isEnabled = false
                    navController.navigateUp()
                    isEnabled = true
                }
            }
        })
    }
    
    /**
     * ✅ NOVO: Mostra diálogo de confirmação para sair do app
     */
    private fun showExitConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Sair do aplicativo")
            .setMessage("Deseja realmente sair do aplicativo?")
            .setPositiveButton("Sair") { _, _ ->
                // Verificar sincronização antes de fechar
                checkPendingSyncBeforeExit()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }
    
    /**
     * ✅ NOVO: Verifica se há dados pendentes de sincronização antes de fechar o app
     */
    private fun checkPendingSyncBeforeExit() {
        if (isSyncingOnExit) {
            // Já está sincronizando, não fazer nada
            return
        }
        
        lifecycleScope.launch {
            try {
                val appRepository = com.example.gestaobilhares.factory.RepositoryFactory.getAppRepository(this@MainActivity)
                val networkUtils = NetworkUtils(this@MainActivity)
                
                // Verificar se está online
                if (!networkUtils.isConnected()) {
                    Log.d("MainActivity", "App offline - fechando sem verificar sincronização")
                    finish()
                    return@launch
                }
                
                // Verificar pendências de sincronização
                val pending = withContext(Dispatchers.IO) {
                    appRepository.contarOperacoesSyncPendentes()
                }
                
                Log.d("MainActivity", "📡 Pendências de sincronização ao fechar: $pending")
                
                if (pending > 0) {
                    // Mostrar diálogo perguntando se deseja sincronizar
                    MaterialAlertDialogBuilder(this@MainActivity)
                        .setTitle("Sincronização pendente")
                        .setMessage("Você tem $pending operação(ões) pendente(s) de sincronização.\n\nDeseja sincronizar antes de fechar o app?")
                        .setPositiveButton("Sincronizar") { _, _ ->
                            performSyncBeforeExit()
                        }
                        .setNegativeButton("Fechar mesmo assim") { _, _ ->
                            finish()
                        }
                        .setCancelable(false)
                        .show()
                } else {
                    // Sem pendências, fechar normalmente
                    finish()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Erro ao verificar pendências: ${e.message}", e)
                // Em caso de erro, fechar normalmente
                finish()
            }
        }
    }
    
    /**
     * ✅ NOVO: Executa sincronização antes de fechar o app
     */
    private fun performSyncBeforeExit() {
        isSyncingOnExit = true
        
        lifecycleScope.launch {
            try {
                // Criar SyncRepository diretamente (sem usar RepositoryFactory para evitar dependência circular)
                val database = com.example.gestaobilhares.data.database.AppDatabase.getDatabase(this@MainActivity)
                val appRepository = com.example.gestaobilhares.data.repository.AppRepository.create(database)
                val syncRepository = com.example.gestaobilhares.sync.SyncRepository(this@MainActivity, appRepository)
                
                // Mostrar diálogo de progresso
                val progressView = layoutInflater.inflate(com.example.gestaobilhares.ui.R.layout.dialog_sync_progress, null)
                val progressBar = progressView.findViewById<android.widget.ProgressBar>(com.example.gestaobilhares.ui.R.id.syncProgressBar)
                val progressPercent = progressView.findViewById<android.widget.TextView>(com.example.gestaobilhares.ui.R.id.tvSyncProgressPercent)
                val progressStatus = progressView.findViewById<android.widget.TextView>(com.example.gestaobilhares.ui.R.id.tvSyncProgressStatus)
                
                progressBar.progress = 0
                progressPercent.text = "0%"
                progressStatus.text = getString(com.example.gestaobilhares.ui.R.string.sync_status_preparing)
                
                val progressDialog = MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle(com.example.gestaobilhares.ui.R.string.sync_progress_title)
                    .setView(progressView)
                    .setCancelable(false)
                    .create()
                progressDialog.show()
                
                // Executar sincronização
                val result = withContext(Dispatchers.IO) {
                    syncRepository.syncBidirectional { progress ->
                        lifecycleScope.launch {
                            progressBar.progress = progress.percent
                            progressPercent.text = "${progress.percent}%"
                            progressStatus.text = progress.message
                        }
                    }
                }
                
                progressDialog.dismiss()
                
                if (result.isSuccess) {
                    android.widget.Toast.makeText(
                        this@MainActivity,
                        "✅ Sincronização concluída!",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    android.widget.Toast.makeText(
                        this@MainActivity,
                        "⚠️ Sincronização falhou: ${result.exceptionOrNull()?.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
                
                // Fechar app após sincronização
                finish()
            } catch (e: Exception) {
                Log.e("MainActivity", "Erro na sincronização: ${e.message}", e)
                android.widget.Toast.makeText(
                    this@MainActivity,
                    "❌ Erro na sincronização: ${e.message}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                finish()
            } finally {
                isSyncingOnExit = false
            }
        }
    }

    // ✅ REMOVIDO: Função popularBancoDados() - agora é feita automaticamente no AppDatabase

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(com.example.gestaobilhares.ui.R.id.nav_host_fragment) as NavHostFragment
        return navHostFragment.navController.navigateUp() || super.onSupportNavigateUp()
    }

    /**
     * ✅ NOVO: Tratamento de permissões Bluetooth para impressão
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == 1001) { // REQUEST_BLUETOOTH_PERMISSIONS
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Permissões concedidas - notificar que pode tentar imprimir novamente
                Log.d("MainActivity", "Permissões Bluetooth concedidas")
                // O usuário pode tentar imprimir novamente
            } else {
                Log.w("MainActivity", "Permissões Bluetooth negadas")
                // Mostrar mensagem explicativa
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("🔗 Permissões Bluetooth Negadas")
                    .setMessage("Para imprimir recibos, é necessário permitir o acesso ao Bluetooth. Vá em Configurações > Aplicativos > Gestão Bilhares > Permissões e ative o Bluetooth.")
                    .setPositiveButton("Entendi", null)
                    .show()
            }
        }
    }
} 
