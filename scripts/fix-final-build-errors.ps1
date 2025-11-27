# Script final para corrigir TODOS os erros de build
$ErrorActionPreference = "Stop"

$rootPath = Split-Path -Parent $PSScriptRoot
$uiPath = Join-Path $rootPath "ui\src\main\java"

Write-Host "=== CORRECAO FINAL DE ERROS DE BUILD ===" -ForegroundColor Cyan

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
        
        # 1. Remover imports duplicados de R - manter apenas um
        $lines = $content -split "`r?`n"
        $newLines = @()
        $hasRImport = $false
        
        foreach ($line in $lines) {
            if ($line -match '^import\s+com\.example\.gestaobilhares\.ui\.R$') {
                if (-not $hasRImport) {
                    $newLines += $line
                    $hasRImport = $true
                }
            } else {
                $newLines += $line
            }
        }
        
        $content = $newLines -join "`r`n"
        
        # 2. Corrigir referencias R.com para R.color, R.drawable, etc
        $content = $content -replace 'com\.example\.gestaobilhares\.ui\.R\.com\.example\.gestaobilhares\.ui\.R\.color\.', 'com.example.gestaobilhares.ui.R.color.'
        $content = $content -replace 'com\.example\.gestaobilhares\.ui\.R\.com\.example\.gestaobilhares\.ui\.R\.drawable\.', 'com.example.gestaobilhares.ui.R.drawable.'
        $content = $content -replace 'com\.example\.gestaobilhares\.ui\.R\.com\.', 'com.example.gestaobilhares.ui.R.'
        
        # 3. Corrigir referencias R.com que sobraram
        $content = $content -replace '\.R\.com\.example\.gestaobilhares\.ui\.R\.', '.R.'
        $content = $content -replace '\bR\.com\.example\.gestaobilhares\.ui\.R\.', 'com.example.gestaobilhares.ui.R.'
        
        # 4. Adicionar import de R se usar R mas nao tiver import
        if (($content -match '\bcom\.example\.gestaobilhares\.ui\.R\.' -or $content -match '\bR\.(id|string|color|drawable|layout|plurals|navigation|sync|nav_|style)\.') -and -not $hasRImport) {
            $packageMatch = $content -match '(?m)^(package\s+[^\r\n]+)'
            if ($packageMatch) {
                $content = $content -replace '(?m)^(package\s+[^\r\n]+)', "`$1`r`nimport com.example.gestaobilhares.ui.R"
            } else {
                $content = "import com.example.gestaobilhares.ui.R`r`n" + $content
            }
        }
        
        # 5. Corrigir imports de Timber
        $content = $content -replace 'import\s+com\.jakewharton\.timber\.Timber', 'import timber.log.Timber'
        if ($content -match '\bTimber\.' -and $content -notmatch 'import\s+timber\.log\.Timber') {
            $packageMatch = $content -match '(?m)^(package\s+[^\r\n]+)'
            if ($packageMatch) {
                $content = $content -replace '(?m)^(package\s+[^\r\n]+)', "`$1`r`nimport timber.log.Timber"
            }
        }
        
        # 6. Corrigir referencias R.color, R.drawable sem prefixo completo
        $content = $content -replace '(?<!com\.example\.gestaobilhares\.ui\.)\bR\.color\.', 'com.example.gestaobilhares.ui.R.color.'
        $content = $content -replace '(?<!com\.example\.gestaobilhares\.ui\.)\bR\.drawable\.', 'com.example.gestaobilhares.ui.R.drawable.'
        $content = $content -replace '(?<!com\.example\.gestaobilhares\.ui\.)\bR\.style\.', 'com.example.gestaobilhares.ui.R.style.'
        $content = $content -replace '(?<!com\.example\.gestaobilhares\.ui\.)\bR\.plurals\.', 'com.example.gestaobilhares.ui.R.plurals.'
        $content = $content -replace '(?<!com\.example\.gestaobilhares\.ui\.)\bR\.sync\.', 'com.example.gestaobilhares.ui.R.sync.'
        $content = $content -replace '(?<!com\.example\.gestaobilhares\.ui\.)\bR\.nav_', 'com.example.gestaobilhares.ui.R.nav_'
        
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

