# Script para corrigir imports no módulo :ui
# De: com.example.gestaobilhares.R -> com.example.gestaobilhares.ui.R
# De: com.example.gestaobilhares.databinding -> com.example.gestaobilhares.ui.databinding
# De: com.example.gestaobilhares.BuildConfig -> remover (não disponível em módulos de biblioteca)

$ErrorActionPreference = "Stop"

Write-Host "=== Corrigindo imports do módulo :ui ===" -ForegroundColor Cyan

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
    
    # Substituir imports de R
    $content = $content -replace 'import com\.example\.gestaobilhares\.R\b', 'import com.example.gestaobilhares.ui.R'
    
    # Substituir referências completas de R (mas não dentro de strings)
    $content = $content -replace '\bcom\.example\.gestaobilhares\.R\.', 'com.example.gestaobilhares.ui.R.'
    
    # Substituir imports de databinding
    $content = $content -replace 'import com\.example\.gestaobilhares\.databinding\.', 'import com.example.gestaobilhares.ui.databinding.'
    
    # Substituir referências completas de databinding
    $content = $content -replace '\bcom\.example\.gestaobilhares\.databinding\.', 'com.example.gestaobilhares.ui.databinding.'
    
    # Remover imports de BuildConfig
    $content = $content -replace 'import com\.example\.gestaobilhares\.BuildConfig\s*\r?\n', ''
    
    # Substituir referências a BuildConfig por comentário
    $content = $content -replace '\bBuildConfig\.DEBUG\b', 'true // BuildConfig não disponível em módulos de biblioteca'
    
    # Substituir imports de firebase (corrigir namespace)
    $content = $content -replace 'import com\.example\.gestaobilhares\.firebase\.', 'import com.google.firebase.'
    
    if ($content -ne $originalContent) {
        $replacements = ([regex]::Matches($originalContent, 'com\.example\.gestaobilhares\.(R|databinding|BuildConfig)')).Count
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

