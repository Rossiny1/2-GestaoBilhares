# Script para corrigir RepositoryFactory
Write-Host "=== CORRIGINDO RepositoryFactory ===" -ForegroundColor Cyan

$file = "data\src\main\java\com\example\gestaobilhares\data\factory\RepositoryFactory.kt"

if (Test-Path $file) {
    $content = Get-Content $file -Raw -Encoding UTF8
    $originalContent = $content
    
    # Corrigir import de SyncRepository
    $content = $content -replace 'import com\.example\.gestaobilhares\.data\.repository\.domain\.SyncRepository', 'import com.example.gestaobilhares.sync.SyncRepository'
    
    if ($content -ne $originalContent) {
        Set-Content -Path $file -Value $content -Encoding UTF8 -NoNewline
        Write-Host "RepositoryFactory corrigido" -ForegroundColor Green
    } else {
        Write-Host "RepositoryFactory ja estava correto" -ForegroundColor Yellow
    }
} else {
    Write-Host "RepositoryFactory nao encontrado" -ForegroundColor Red
}

Write-Host "=== RepositoryFactory CORRIGIDO ===" -ForegroundColor Green

