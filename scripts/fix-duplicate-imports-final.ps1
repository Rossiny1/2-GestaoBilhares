# Script final para remover imports duplicados
$ErrorActionPreference = "Stop"

$rootPath = Split-Path -Parent $PSScriptRoot
$uiPath = Join-Path $rootPath "ui\src\main\java"

Write-Host "=== REMOVENDO IMPORTS DUPLICADOS ===" -ForegroundColor Cyan

$files = Get-ChildItem -Path $uiPath -Filter "*.kt" -Recurse -File
$updatedCount = 0

foreach ($file in $files) {
    try {
        $content = [System.IO.File]::ReadAllText($file.FullName, [System.Text.Encoding]::UTF8)
        $originalContent = $content
        
        $lines = $content -split "`r?`n"
        $newLines = @()
        $seenR = $false
        $seenTimber = $false
        
        foreach ($line in $lines) {
            # Processar imports de R
            if ($line -match '^\s*import\s+.*\.R\s*$') {
                if (-not $seenR) {
                    # Manter apenas o primeiro import correto
                    if ($line -match 'com\.example\.gestaobilhares\.ui\.R') {
                        $newLines += $line
                        $seenR = $true
                    } elseif ($line -match 'import\s+com\.example\.gestaobilhares\.R\s*$') {
                        $newLines += "import com.example.gestaobilhares.ui.R"
                        $seenR = $true
                    }
                }
                continue
            }
            
            # Processar imports de Timber
            if ($line -match '^\s*import\s+.*Timber') {
                if (-not $seenTimber) {
                    if ($line -match 'timber\.log\.Timber') {
                        $newLines += $line
                        $seenTimber = $true
                    } elseif ($line -match 'com\.jakewharton\.timber') {
                        $newLines += "import timber.log.Timber"
                        $seenTimber = $true
                    }
                }
                continue
            }
            
            $newLines += $line
        }
        
        $content = $newLines -join "`r`n"
        
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

