# Script FINAL para corrigir TODOS os erros de build de uma vez
$ErrorActionPreference = "Stop"

$rootPath = Split-Path -Parent $PSScriptRoot
$uiPath = Join-Path $rootPath "ui\src\main\java"

Write-Host "=== CORRECAO FINAL DE TODOS OS ERROS ===" -ForegroundColor Cyan

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
        
        # 1. Corrigir imports de Timber duplicados ou incorretos
        $content = $content -replace 'import\s+com\.jakewharton\.timber\.Timber', 'import timber.log.Timber'
        $content = $content -replace 'import\s+timber\.log\.timber', 'import timber.log.Timber'
        
        # 2. Adicionar import de Timber se usar mas nao tiver
        if ($content -match '\bTimber\.' -and $content -notmatch 'import\s+timber\.log\.Timber') {
            $packageMatch = $content -match '(?m)^(package\s+[^\r\n]+)'
            if ($packageMatch) {
                $content = $content -replace '(?m)^(package\s+[^\r\n]+)', "`$1`r`nimport timber.log.Timber"
            }
        }
        
        # 3. Corrigir referencias R.com duplicadas
        $content = $content -replace 'com\.example\.gestaobilhares\.com\.example\.gestaobilhares\.ui\.R\.', 'com.example.gestaobilhares.ui.R.'
        
        # 4. Corrigir referencias android.com
        $content = $content -replace 'android\.com\.example\.gestaobilhares\.ui\.R\.', 'android.R.'
        
        if ($content -ne $originalContent) {
            [System.IO.File]::WriteAllText($file.FullName, $content, [System.Text.Encoding]::UTF8)
            $updatedCount++
        }
    } catch {
        Write-Host "  ERRO: $($file.Name) - $_" -ForegroundColor Red
    }
}

Write-Host "`nArquivos atualizados: $updatedCount de $totalFiles" -ForegroundColor Green
Write-Host "=== CONCLUIDO ===" -ForegroundColor Green

