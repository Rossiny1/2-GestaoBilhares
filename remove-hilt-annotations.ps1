# Script para remover todas as anotações Hilt do projeto
Write-Host "REMOVENDO ANOTACOES HILT DO PROJETO..." -ForegroundColor Yellow

# Lista de arquivos que contêm anotações Hilt
$files = @(
    "app/src/main/java/com/example/gestaobilhares/ui/contracts/ContractManagementFragment.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/expenses/GlobalExpensesViewModel.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/expenses/GlobalExpensesFragment.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/expenses/ExpenseRegisterViewModel.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/mesas/HistoricoMesasVendidasViewModel.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/settlement/SettlementViewModel.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/clients/ClientListViewModel.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/metas/MetasViewModel.kt",
    "app/src/main/java/com/example/gestaobilhares/data/repository/ClienteRepository.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/contracts/AditivoSignatureFragment.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/contracts/SignatureCaptureFragment.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/reports/ClosureReportFragment.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/reports/ClosureReportViewModel.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/metas/MetaCadastroFragment.kt",
    "app/src/main/java/com/example/gestaobilhares/data/repository/TipoDespesaRepository.kt",
    "app/src/main/java/com/example/gestaobilhares/data/repository/DespesaRepository.kt",
    "app/src/main/java/com/example/gestaobilhares/data/repository/CategoriaDespesaRepository.kt",
    "app/src/main/java/com/example/gestaobilhares/data/repository/AcertoRepository.kt",
    "app/src/main/java/com/example/gestaobilhares/data/repository/RotaRepository.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/inventory/equipments/EquipmentsViewModel.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/inventory/others/OthersInventoryViewModel.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/metas/MetasFragment.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/metas/MetaCadastroViewModel.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/inventory/stock/StockViewModel.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/inventory/vehicles/VehicleDetailFragment.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/inventory/vehicles/VehicleDetailViewModel.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/inventory/stock/AddPanosLoteDialog.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/inventory/stock/StockFragment.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/inventory/stock/AddEditStockItemDialog.kt",
    "app/src/main/java/com/example/gestaobilhares/data/repository/StockItemRepository.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/settlement/PanoSelectionDialog.kt",
    "app/src/main/java/com/example/gestaobilhares/data/repository/PanoEstoqueRepository.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/inventory/vehicles/VehiclesFragment.kt",
    "app/src/main/java/com/example/gestaobilhares/data/repository/HistoricoCombustivelVeiculoRepository.kt",
    "app/src/main/java/com/example/gestaobilhares/data/repository/HistoricoManutencaoVeiculoRepository.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/inventory/vehicles/VehiclesViewModel.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/inventory/vehicles/AddEditVehicleDialog.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/inventory/equipments/EquipmentsFragment.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/inventory/others/OthersInventoryFragment.kt",
    "app/src/main/java/com/example/gestaobilhares/data/repository/VeiculoRepository.kt",
    "app/src/main/java/com/example/gestaobilhares/data/repository/HistoricoManutencaoMesaRepository.kt",
    "app/src/main/java/com/example/gestaobilhares/data/repository/MesaReformadaRepository.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/mesas/HistoricoMesasVendidasFragment.kt",
    "app/src/main/java/com/example/gestaobilhares/data/repository/MesaRepository.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/mesas/GerenciarMesasFragment.kt",
    "app/src/main/java/com/example/gestaobilhares/data/repository/MesaVendidaRepository.kt",
    "app/src/main/java/com/example/gestaobilhares/data/repository/CicloAcertoRepository.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/routes/TransferClientViewModel.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/contracts/ContractGenerationViewModel.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/contracts/ContractGenerationFragment.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/contracts/SignatureCaptureViewModel.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/contracts/AditivoSignatureViewModel.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/contracts/RepresentanteLegalSignatureFragment.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/contracts/RepresentanteLegalSignatureViewModel.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/routes/TransferClientDialog.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/routes/ClientSelectionViewModel.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/routes/ClientSelectionDialog.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/contracts/ContractFinalizationDialog.kt",
    "app/src/main/java/com/example/gestaobilhares/utils/LegalLogger.kt",
    "app/src/main/java/com/example/gestaobilhares/utils/SignatureMetadataCollector.kt",
    "app/src/main/java/com/example/gestaobilhares/utils/DocumentIntegrityManager.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/mesas/GerenciarMesasViewModel.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/mesas/RotaMesasViewModel.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/mesas/RotaMesasFragment.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/mesas/EditMesaViewModel.kt",
    "app/src/main/java/com/example/gestaobilhares/data/repository/AcertoMesaRepository.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/dashboard/DashboardFragment.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/dashboard/DashboardViewModel.kt"
)

$removedCount = 0

foreach ($file in $files) {
    if (Test-Path $file) {
        Write-Host "Processando: $file" -ForegroundColor Cyan
        
        # Ler conteúdo do arquivo
        $content = Get-Content $file -Raw -Encoding UTF8
        
        # Remover anotações Hilt
        $originalContent = $content
        
        # Remover @AndroidEntryPoint
        $content = $content -replace '@AndroidEntryPoint\s*\n', ''
        
        # Remover @HiltViewModel
        $content = $content -replace '@HiltViewModel\s*\n', ''
        
        # Remover @Inject
        $content = $content -replace '@Inject\s+', ''
        
        # Remover @Singleton
        $content = $content -replace '@Singleton\s*\n', ''
        
        # Remover imports Hilt
        $content = $content -replace 'import dagger\.hilt\.android\.AndroidEntryPoint\s*\n', ''
        $content = $content -replace 'import dagger\.hilt\.android\.lifecycle\.HiltViewModel\s*\n', ''
        $content = $content -replace 'import javax\.inject\.Inject\s*\n', ''
        $content = $content -replace 'import javax\.inject\.Singleton\s*\n', ''
        
        # Se houve mudanças, salvar o arquivo
        if ($content -ne $originalContent) {
            Set-Content $file -Value $content -Encoding UTF8
            $removedCount++
            Write-Host "SUCCESS: Anotacoes Hilt removidas de: $file" -ForegroundColor Green
        }
    } else {
        Write-Host "WARNING: Arquivo nao encontrado: $file" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "RESUMO:" -ForegroundColor Magenta
Write-Host "Arquivos processados: $($files.Count)" -ForegroundColor Green
Write-Host "Arquivos modificados: $removedCount" -ForegroundColor Green
Write-Host ""
Write-Host "ANOTACOES HILT REMOVIDAS COM SUCESSO!" -ForegroundColor Green
