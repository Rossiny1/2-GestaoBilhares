package com.example.gestaobilhares

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.example.gestaobilhares.databinding.ActivityMainBinding
import com.example.gestaobilhares.utils.DatabasePopulator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
/**
 * MainActivity configurada para Navigation Component e ViewBinding.
 * Usando NoActionBar theme - navegação gerenciada pelos próprios fragments.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Navigation Controller configurado automaticamente - gerenciado pelos fragments
        // REMOVIDO: setupActionBarWithNavController() - incompatível com NoActionBar theme

        // ✅ NOVO: Popular banco de dados com dados de teste
        popularBancoDados()
    }

    /**
     * Popula o banco de dados com dados de teste
     * ✅ NOVO: Sistema de população de dados para testes
     */
    private fun popularBancoDados() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val populator = DatabasePopulator(this@MainActivity)
                populator.popularBancoCompleto()
                Log.d("MainActivity", "🎉 Banco de dados populado com sucesso!")
            } catch (e: Exception) {
                Log.e("MainActivity", "❌ Erro ao popular banco: ${e.message}", e)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        return navHostFragment.navController.navigateUp() || super.onSupportNavigateUp()
    }
} 