package com.example.gestaobilhares

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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