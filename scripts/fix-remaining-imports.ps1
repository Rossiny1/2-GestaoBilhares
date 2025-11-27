# Script para corrigir imports restantes (databinding e Timber)
$ErrorActionPreference = "Stop"

$rootPath = Split-Path -Parent $PSScriptRoot
$uiPath = Join-Path $rootPath "ui\src\main\java"

Write-Host "=== CORRIGINDO IMPORTS RESTANTES ===" -ForegroundColor Cyan

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
        
        # Corrigir imports de databinding
        $content = $content -replace '(?m)^import com\.example\.gestaobilhares\.databinding\.', 'import com.example.gestaobilhares.ui.databinding.'
        
        # Corrigir imports de Timber
        $content = $content -replace 'import com\.jakewharton\.timber\.Timber', 'import timber.log.Timber'
        
        # Corrigir referencias diretas a databinding sem import correto
        if ($content -match '\bcom\.example\.gestaobilhares\.ui\.databinding\.' -and $content -notmatch '(?m)^import com\.example\.gestaobilhares\.ui\.databinding\.') {
            # Adicionar import se nao existir
            if ($content -match '(?m)^(package\s+[^\r\n]+)') {
                $content = $content -replace '(?m)^(package\s+[^\r\n]+)', "`$1`r`nimport com.example.gestaobilhares.ui.databinding.*"
            }
        }
        
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

