# REMOVER FACTORIES PROBLEMÁTICAS
Write-Host "=== REMOVENDO FACTORIES PROBLEMÁTICAS ===" -ForegroundColor Yellow

# Lista de arquivos Factory problemáticos para remover
$problematicFactories = @(
    "app/src/main/java/com/example/gestaobilhares/ui/expenses/ExpenseRegisterViewModelFactory.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/expenses/GlobalExpensesViewModelFactory.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/inventory/stock/StockViewModelFactory.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/inventory/vehicles/VehicleDetailViewModelFactory.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/inventory/vehicles/VehiclesViewModelFactory.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/mesas/HistoricoMesasVendidasViewModelFactory.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/settlement/SettlementViewModelFactory.kt"
)

foreach ($file in $problematicFactories) {
    if (Test-Path $file) {
        Write-Host "Removendo: $file" -ForegroundColor Cyan
        Remove-Item $file -Force
        Write-Host "   ✅ Removido" -ForegroundColor Green
    } else {
        Write-Host "   ❌ Arquivo não encontrado: $file" -ForegroundColor Red
    }
}

Write-Host "=== REMOÇÃO CONCLUÍDA ===" -ForegroundColor Yellow
