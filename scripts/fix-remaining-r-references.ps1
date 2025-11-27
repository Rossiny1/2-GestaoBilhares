# Script para corrigir referencias R incorretas restantes
$ErrorActionPreference = "Stop"

$rootPath = Split-Path -Parent $PSScriptRoot
$uiPath = Join-Path $rootPath "ui\src\main\java"

Write-Host "=== CORRIGINDO REFERENCIAS R RESTANTES ===" -ForegroundColor Cyan

$files = Get-ChildItem -Path $uiPath -Filter "*.kt" -Recurse -File
$updatedCount = 0

foreach ($file in $files) {
    try {
        $content = [System.IO.File]::ReadAllText($file.FullName, [System.Text.Encoding]::UTF8)
        $originalContent = $content
        
        # Corrigir android.com.example.gestaobilhares.ui.R -> android.R
        $content = $content -replace 'android\.com\.example\.gestaobilhares\.ui\.R\.', 'android.R.'
        
        # Corrigir com.example.gestaobilhares.ui.com.example.gestaobilhares.ui.R -> com.example.gestaobilhares.ui.R
        $content = $content -replace 'com\.example\.gestaobilhares\.ui\.com\.example\.gestaobilhares\.ui\.R\.', 'com.example.gestaobilhares.ui.R.'
        
        # Corrigir referencias R.com que sobraram
        $content = $content -replace '\.R\.com\.example\.gestaobilhares\.ui\.R\.', '.R.'
        $content = $content -replace '\bR\.com\.', 'com.example.gestaobilhares.ui.R.'
        
        if ($content -ne $originalContent) {
            [System.IO.File]::WriteAllText($file.FullName, $content, [System.Text.Encoding]::UTF8)
            $updatedCount++
        }
    } catch {
        Write-Host "  ERRO: $($file.Name)" -ForegroundColor Red
    }
}

Write-Host "Arquivos atualizados: $updatedCount" -ForegroundColor Green
Write-Host "=== CONCLUIDO ===" -ForegroundColor Green

