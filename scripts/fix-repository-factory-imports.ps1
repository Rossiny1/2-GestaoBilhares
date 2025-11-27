# Script para atualizar imports de RepositoryFactory
# De: com.example.gestaobilhares.data.factory.RepositoryFactory
# Para: com.example.gestaobilhares.core.factory.RepositoryFactory

$ErrorActionPreference = "Stop"

Write-Host "=== Atualizando imports de RepositoryFactory ===" -ForegroundColor Cyan

$rootPath = Split-Path -Parent $PSScriptRoot
$uiPath = Join-Path $rootPath "ui\src\main\java"

if (-not (Test-Path $uiPath)) {
    Write-Host "ERRO: Diretório não encontrado: $uiPath" -ForegroundColor Red
    exit 1
}

# Encontrar todos os arquivos Kotlin no módulo :ui
$files = Get-ChildItem -Path $uiPath -Filter "*.kt" -Recurse

$updatedCount = 0
$totalReplacements = 0

foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw -Encoding UTF8
    $originalContent = $content
    
    # Substituir imports (com e sem data.)
    $content = $content -replace 'import com\.example\.gestaobilhares\.(data\.)?factory\.RepositoryFactory', 'import com.example.gestaobilhares.core.factory.RepositoryFactory'
    
    # Substituir referências completas (com e sem data.)
    $content = $content -replace 'com\.example\.gestaobilhares\.(data\.)?factory\.RepositoryFactory', 'com.example.gestaobilhares.core.factory.RepositoryFactory'
    
    if ($content -ne $originalContent) {
        $replacements = ([regex]::Matches($originalContent, 'com\.example\.gestaobilhares\.(data\.)?factory\.RepositoryFactory')).Count
        $totalReplacements += $replacements
        
        Set-Content -Path $file.FullName -Value $content -Encoding UTF8 -NoNewline
        Write-Host "  Atualizado: $($file.Name) ($replacements substituições)" -ForegroundColor Green
        $updatedCount++
    }
}

Write-Host "`n=== Resumo ===" -ForegroundColor Cyan
Write-Host "Arquivos atualizados: $updatedCount" -ForegroundColor Green
Write-Host "Total de substituições: $totalReplacements" -ForegroundColor Green
Write-Host "`nConcluído!" -ForegroundColor Green

