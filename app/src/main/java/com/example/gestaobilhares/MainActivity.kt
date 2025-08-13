package com.example.gestaobilhares

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.example.gestaobilhares.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
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
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        return navHostFragment.navController.navigateUp() || super.onSupportNavigateUp()
    }
} 