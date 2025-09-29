package com.example.gestaobilhares

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.example.gestaobilhares.databinding.ActivityMainBinding
// ✅ REMOVIDO: import DatabasePopulator - não é mais necessário
import dagger.hilt.android.AndroidEntryPoint
// ✅ REMOVIDO: imports CoroutineScope - não são mais necessários
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

        // ✅ REMOVIDO: Popular banco de dados - agora é feito automaticamente no AppDatabase
        // popularBancoDados()
    }

    // ✅ REMOVIDO: Função popularBancoDados() - agora é feita automaticamente no AppDatabase

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        return navHostFragment.navController.navigateUp() || super.onSupportNavigateUp()
    }
} 