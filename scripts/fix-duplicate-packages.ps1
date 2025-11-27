# Script para corrigir duplicacoes de pacote incorretas
$ErrorActionPreference = "Stop"

$rootPath = Split-Path -Parent $PSScriptRoot
$uiPath = Join-Path $rootPath "ui\src\main\java"

Write-Host "=== CORRIGINDO DUPLICACOES DE PACOTE ===" -ForegroundColor Cyan

$files = Get-ChildItem -Path $uiPath -Filter "*.kt" -Recurse -File
$updatedCount = 0
$totalFiles = $files.Count
$processed = 0

foreach ($file in $files) {
    $processed++
    if ($processed % 50 -eq 0) {
        Write-Host "  Processando: $processed/$totalFiles arquivos..." -ForegroundColor Cyan
    }
    
    try {
        $content = [System.IO.File]::ReadAllText($file.FullName, [System.Text.Encoding]::UTF8)
        $originalContent = $content
        
        # Corrigir duplicacao: com.example.gestaobilhares.ui.com.example.gestaobilhares.ui.R
        $content = $content -replace 'com\.example\.gestaobilhares\.ui\.com\.example\.gestaobilhares\.ui\.R', 'com.example.gestaobilhares.ui.R'
        
        # Corrigir android.com.example.gestaobilhares.ui.R -> android.R
        $content = $content -replace 'android\.com\.example\.gestaobilhares\.ui\.R', 'android.R'
        
        if ($content -ne $originalContent) {
            [System.IO.File]::WriteAllText($file.FullName, $content, [System.Text.Encoding]::UTF8)
            $updatedCount++
        }
    } catch {
        Write-Host "  ERRO ao processar $($file.Name): $_" -ForegroundColor Red
    }
}

Write-Host "`nArquivos atualizados: $updatedCount de $totalFiles" -ForegroundColor Green
Write-Host "=== CORRECAO CONCLUIDA ===" -ForegroundColor Green

