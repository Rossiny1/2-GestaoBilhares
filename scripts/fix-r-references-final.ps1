# Script para corrigir referencias R sem prefixo completo
$ErrorActionPreference = "Stop"

$rootPath = Split-Path -Parent $PSScriptRoot
$uiPath = Join-Path $rootPath "ui\src\main\java"

Write-Host "=== CORRIGINDO REFERENCIAS R ===" -ForegroundColor Cyan

$files = Get-ChildItem -Path $uiPath -Filter "*.kt" -Recurse -File
$updatedCount = 0

foreach ($file in $files) {
    try {
        $content = [System.IO.File]::ReadAllText($file.FullName, [System.Text.Encoding]::UTF8)
        $originalContent = $content
        
        # Corrigir referencias R.color, R.drawable, etc que nao tem prefixo completo
        # Mas apenas se nao tiverem o prefixo completo ja
        $content = $content -replace '(?<!com\.example\.gestaobilhares\.ui\.)R\.color\.', 'com.example.gestaobilhares.ui.R.color.'
        $content = $content -replace '(?<!com\.example\.gestaobilhares\.ui\.)R\.drawable\.', 'com.example.gestaobilhares.ui.R.drawable.'
        $content = $content -replace '(?<!com\.example\.gestaobilhares\.ui\.)R\.plurals\.', 'com.example.gestaobilhares.ui.R.plurals.'
        $content = $content -replace '(?<!com\.example\.gestaobilhares\.ui\.)R\.sync\.', 'com.example.gestaobilhares.ui.R.sync.'
        $content = $content -replace '(?<!com\.example\.gestaobilhares\.ui\.)R\.style\.', 'com.example.gestaobilhares.ui.R.style.'
        $content = $content -replace '(?<!com\.example\.gestaobilhares\.ui\.)R\.nav_', 'com.example.gestaobilhares.ui.R.nav_'
        
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

