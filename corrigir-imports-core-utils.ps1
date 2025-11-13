# Script para corrigir imports de core.utils para utils
$files = @(
    "app/src/main/java/com/example/gestaobilhares/ui/cycles/CycleManagementFragment.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/cycles/CycleManagementViewModel.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/clients/ClientListViewModel.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/clients/SettlementHistoryAdapter.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/metas/MetasViewModel.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/expenses/ExpenseRegisterFragment.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/clients/ClientRegisterFragment.kt"
)

foreach ($file in $files) {
    if (Test-Path $file) {
        $content = Get-Content $file -Raw -Encoding UTF8
        $content = $content -replace 'com\.example\.gestaobilhares\.core\.utils\.', 'com.example.gestaobilhares.utils.'
        Set-Content $file -Value $content -Encoding UTF8 -NoNewline
        Write-Host "Corrigido: $file" -ForegroundColor Green
    }
}

Write-Host "`nCorrecao de imports concluida!" -ForegroundColor Cyan

