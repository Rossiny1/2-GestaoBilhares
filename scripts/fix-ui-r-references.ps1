# Script para corrigir referências a R.* no módulo :ui
# De: R.nav_* -> com.example.gestaobilhares.ui.R.nav_*
# De: R.string -> com.example.gestaobilhares.ui.R.string
# De: R.color -> com.example.gestaobilhares.ui.R.color
# De: R.drawable -> com.example.gestaobilhares.ui.R.drawable
# De: R.plurals -> com.example.gestaobilhares.ui.R.plurals

$ErrorActionPreference = "Stop"

Write-Host "=== Corrigindo referências R.* no módulo :ui ===" -ForegroundColor Cyan

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
    
    # Substituir referências a R.nav_*, R.string, R.color, R.drawable, R.plurals
    # Mas não substituir se já tiver o namespace completo
    $content = $content -replace '\bR\.(nav_|string|color|drawable|plurals)', 'com.example.gestaobilhares.ui.R.$1'
    
    # Substituir referências simples a R. (mas não R.id, R.layout que já estão corretos)
    # Apenas se não começar com com.example
    $content = $content -replace '(?<!com\.example\.gestaobilhares\.ui\.)R\.(nav_|string|color|drawable|plurals)', 'com.example.gestaobilhares.ui.R.$1'
    
    if ($content -ne $originalContent) {
        $replacements = ([regex]::Matches($originalContent, '\bR\.(nav_|string|color|drawable|plurals)')).Count
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

