# Script final para corrigir TODAS as duplicacoes de pacote
$ErrorActionPreference = "Stop"

$rootPath = Split-Path -Parent $PSScriptRoot
$uiPath = Join-Path $rootPath "ui\src\main\java"

Write-Host "=== CORRECAO FINAL DE DUPLICACOES ===" -ForegroundColor Cyan

$files = Get-ChildItem -Path $uiPath -Filter "*.kt" -Recurse -File
$updatedCount = 0

foreach ($file in $files) {
    try {
        $content = [System.IO.File]::ReadAllText($file.FullName, [System.Text.Encoding]::UTF8)
        $originalContent = $content
        
        # Corrigir todas as variacoes de duplicacao
        $content = $content -replace 'com\.example\.gestaobilhares\.ui\.com\.example\.gestaobilhares\.ui', 'com.example.gestaobilhares.ui'
        $content = $content -replace 'com\.example\.gestaobilhares\.ui\.com\.example\.gestaobilhares\.ui\.R', 'com.example.gestaobilhares.ui.R'
        
        if ($content -ne $originalContent) {
            [System.IO.File]::WriteAllText($file.FullName, $content, [System.Text.Encoding]::UTF8)
            $updatedCount++
            Write-Host "  Corrigido: $($file.Name)" -ForegroundColor Green
        }
    } catch {
        Write-Host "  ERRO: $($file.Name) - $_" -ForegroundColor Red
    }
}

Write-Host "`nTotal corrigido: $updatedCount arquivos" -ForegroundColor Green
Write-Host "=== CONCLUIDO ===" -ForegroundColor Green

