# Script para comentar métodos de veículos que não existem
$files = @(
    "app/src/main/java/com/example/gestaobilhares/ui/inventory/vehicles/VehicleDetailViewModel.kt",
    "app/src/main/java/com/example/gestaobilhares/ui/inventory/vehicles/VehiclesViewModel.kt"
)

foreach ($file in $files) {
    if (Test-Path $file) {
        $content = Get-Content $file -Raw -Encoding UTF8
        
        # Comentar métodos que não existem
        $content = $content -replace 'appRepository\.obterTodosVeiculos\(\)', '// TODO: appRepository.obterTodosVeiculos() // Método não implementado'
        $content = $content -replace 'appRepository\.inserirVeiculo\(', '// TODO: appRepository.inserirVeiculo( // Método não implementado'
        $content = $content -replace 'appRepository\.obterTodosHistoricoCombustivelVeiculo\(\)', '// TODO: appRepository.obterTodosHistoricoCombustivelVeiculo() // Método não implementado'
        $content = $content -replace 'appRepository\.obterTodosHistoricoManutencaoVeiculo\(\)', '// TODO: appRepository.obterTodosHistoricoManutencaoVeiculo() // Método não implementado'
        
        Set-Content $file -Value $content -Encoding UTF8 -NoNewline
        Write-Host "Comentado: $file" -ForegroundColor Yellow
    }
}

Write-Host "`nComentarios adicionados!" -ForegroundColor Cyan
Write-Host "ATENCAO: Esses metodos precisam ser implementados no AppRepository!" -ForegroundColor Red

