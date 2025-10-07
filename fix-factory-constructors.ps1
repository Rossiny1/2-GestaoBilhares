# CORREÇÃO DE CONSTRUTORES DAS FACTORIES
Write-Host "=== CORRIGINDO CONSTRUTORES DAS FACTORIES ===" -ForegroundColor Yellow

# Lista de arquivos Factory que precisam ser corrigidos
$factoryFiles = @(
    "app/src/main/java/com/example/gestaobilhares/ui/expenses/ExpenseRegisterViewModelFactory.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/expenses/GlobalExpensesViewModelFactory.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/inventory/stock/StockViewModelFactory.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/inventory/vehicles/VehicleDetailViewModelFactory.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/inventory/vehicles/VehiclesViewModelFactory.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/mesas/HistoricoMesasVendidasViewModelFactory.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/settlement/SettlementViewModelFactory.kt"
)

foreach ($file in $factoryFiles) {
    if (Test-Path $file) {
        Write-Host "Corrigindo: $file" -ForegroundColor Cyan
        
        # Ler o conteúdo do arquivo
        $content = Get-Content $file -Raw
        
        # Corrigir para usar apenas AppRepository
        $content = $content -replace "return \w+ViewModel\(repository\) as T", "return `$1ViewModel(appRepository) as T"
        $content = $content -replace "private val repository: AppRepository", "private val appRepository: AppRepository"
        
        # Salvar o arquivo corrigido
        Set-Content $file -Value $content -Encoding UTF8
        
        Write-Host "   ✅ Corrigido" -ForegroundColor Green
    } else {
        Write-Host "   ❌ Arquivo não encontrado: $file" -ForegroundColor Red
    }
}

Write-Host "=== CORREÇÃO CONCLUÍDA ===" -ForegroundColor Yellow
