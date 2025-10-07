# CORREÇÃO DE PACKAGES NAS FACTORIES
Write-Host "=== CORRIGINDO PACKAGES DAS FACTORIES ===" -ForegroundColor Yellow

# Lista de arquivos Factory que precisam ser corrigidos
$factoryFiles = @(
    "app/src/main/java/com/example/gestaobilhares/ui/inventory/stock/StockViewModelFactory.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/inventory/vehicles/VehicleDetailViewModelFactory.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/inventory/vehicles/VehiclesViewModelFactory.kt"
)

foreach ($file in $factoryFiles) {
    if (Test-Path $file) {
        Write-Host "Corrigindo: $file" -ForegroundColor Cyan
        
        # Ler o conteúdo do arquivo
        $content = Get-Content $file -Raw
        
        # Corrigir package names com "/" para "."
        $content = $content -replace "package com\.example\.gestaobilhares\.ui\.inventory/", "package com.example.gestaobilhares.ui.inventory."
        
        # Salvar o arquivo corrigido
        Set-Content $file -Value $content -Encoding UTF8
        
        Write-Host "   ✅ Corrigido" -ForegroundColor Green
    } else {
        Write-Host "   ❌ Arquivo não encontrado: $file" -ForegroundColor Red
    }
}

Write-Host "=== CORREÇÃO CONCLUÍDA ===" -ForegroundColor Yellow
