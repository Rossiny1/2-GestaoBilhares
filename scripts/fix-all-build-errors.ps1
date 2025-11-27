# Script para corrigir TODOS os erros de build restantes
$ErrorActionPreference = "Stop"

$rootPath = Split-Path -Parent $PSScriptRoot
$uiPath = Join-Path $rootPath "ui\src\main\java"

Write-Host "=== CORRIGINDO TODOS OS ERROS DE BUILD ===" -ForegroundColor Cyan

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
        
        # 1. Remover imports duplicados de R (manter apenas um)
        $lines = $content -split "`r?`n"
        $importRCount = 0
        $newLines = @()
        $hasRImport = $false
        
        foreach ($line in $lines) {
            if ($line -match '^import\s+com\.example\.gestaobilhares\.ui\.R$') {
                if (-not $hasRImport) {
                    $newLines += $line
                    $hasRImport = $true
                }
                # Ignorar duplicados
            } else {
                $newLines += $line
            }
        }
        
        $content = $newLines -join "`r`n"
        
        # 2. Adicionar import de R se usar R mas nao tiver import
        if ($content -match '\bcom\.example\.gestaobilhares\.ui\.R\.' -and -not $hasRImport) {
            $packageMatch = $content -match '(?m)^(package\s+[^\r\n]+)'
            if ($packageMatch) {
                $packageLine = $matches[1]
                $content = $content -replace '(?m)^(package\s+[^\r\n]+)', "`$1`r`nimport com.example.gestaobilhares.ui.R"
            } else {
                $content = "import com.example.gestaobilhares.ui.R`r`n" + $content
            }
        }
        
        # 3. Corrigir imports de Timber
        $content = $content -replace 'import\s+com\.jakewharton\.timber\.Timber', 'import timber.log.Timber'
        if ($content -match '\bTimber\.' -and $content -notmatch 'import\s+timber\.log\.Timber') {
            if ($content -match '(?m)^(package\s+[^\r\n]+)') {
                $content = $content -replace '(?m)^(package\s+[^\r\n]+)', "`$1`r`nimport timber.log.Timber"
            }
        }
        
        # 4. Corrigir referencias R.color, R.drawable, etc que estao sem o prefixo completo
        $content = $content -replace '\bR\.color\.', 'com.example.gestaobilhares.ui.R.color.'
        $content = $content -replace '\bR\.drawable\.', 'com.example.gestaobilhares.ui.R.drawable.'
        $content = $content -replace '\bR\.style\.', 'com.example.gestaobilhares.ui.R.style.'
        $content = $content -replace '\bR\.plurals\.', 'com.example.gestaobilhares.ui.R.plurals.'
        $content = $content -replace '\bR\.sync\.', 'com.example.gestaobilhares.ui.R.sync.'
        $content = $content -replace '\bR\.nav_', 'com.example.gestaobilhares.ui.R.nav_'
        
        # 5. Corrigir import de ClosureReportPdfGenerator
        $content = $content -replace 'import\s+com\.example\.gestaobilhares\.ui\.reports\.ClosureReportPdfGenerator', 'import com.example.gestaobilhares.ui.reports.ClosureReportPdfGenerator'
        if ($content -match '\bClosureReportPdfGenerator\b' -and $content -notmatch 'import.*ClosureReportPdfGenerator') {
            if ($content -match '(?m)^(package\s+[^\r\n]+)') {
                $content = $content -replace '(?m)^(package\s+[^\r\n]+)', "`$1`r`nimport com.example.gestaobilhares.ui.reports.ClosureReportPdfGenerator"
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
