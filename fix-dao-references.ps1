# Script para remover todas as referências ao DAO removido
Write-Host "🔧 REMOVENDO REFERÊNCIAS AO DAO REMOVIDO" -ForegroundColor Yellow

# Lista de arquivos que precisam ser corrigidos
$files = @(
    "app\src\main\java\com\example\gestaobilhares\ui\contracts\SignatureCaptureFragment.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\contracts\AditivoSignatureFragment.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\contracts\ContractManagementFragment.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\auth\AuthViewModel.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\clients\ClientListFragment.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\clients\ClientDetailFragment.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\clients\ClientRegisterFragment.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\colaboradores\ColaboradorManagementFragment.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\settlement\SettlementDetailFragment.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\cycles\CycleManagementFragment.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\expenses\ExpenseRegisterFragment.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\settlement\SettlementFragment.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\cycles\CycleExpensesFragment.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\cycles\CycleClientsFragment.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\clients\CycleHistoryFragment.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\mesas\MesasDepositoFragment.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\colaboradores\ColaboradorMetasFragment.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\mesas\VendaMesaDialog.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\colaboradores\ColaboradorRegisterFragment.kt"
)

foreach ($file in $files) {
    if (Test-Path $file) {
        Write-Host "Processando: $file" -ForegroundColor Cyan
        
        # Ler o conteúdo do arquivo
        $content = Get-Content $file -Raw -Encoding UTF8
        
        # Remover referências ao DAO
        $content = $content -replace ', procuraçãoRepresentanteDao', ''
        $content = $content -replace 'procuraçãoRepresentanteDao,', ''
        $content = $content -replace 'procuraçãoRepresentanteDao', ''
        
        # Salvar o arquivo
        Set-Content $file -Value $content -Encoding UTF8
        
        Write-Host "✅ Corrigido: $file" -ForegroundColor Green
    } else {
        Write-Host "❌ Arquivo não encontrado: $file" -ForegroundColor Red
    }
}

Write-Host "✅ CORREÇÃO CONCLUÍDA!" -ForegroundColor Green
Write-Host "Agora execute: ./gradlew assembleDebug --no-daemon" -ForegroundColor Yellow
