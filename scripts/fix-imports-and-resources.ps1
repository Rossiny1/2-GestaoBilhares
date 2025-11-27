# Script eficiente para corrigir imports e recursos
$ErrorActionPreference = "Stop"

$rootPath = Split-Path -Parent $PSScriptRoot
$uiPath = Join-Path $rootPath "ui\src\main\java"

Write-Host "=== CORRIGINDO IMPORTS E RECURSOS ===" -ForegroundColor Cyan

# FASE 1: Corrigir imports duplicados e faltantes
$files = Get-ChildItem -Path $uiPath -Filter "*.kt" -Recurse -File
$updatedCount = 0
$processed = 0

foreach ($file in $files) {
    $processed++
    if ($processed % 30 -eq 0) {
        Write-Host "  Processando: $processed/$($files.Count) arquivos..." -ForegroundColor Cyan
    }
    
    try {
        $content = [System.IO.File]::ReadAllText($file.FullName, [System.Text.Encoding]::UTF8)
        $originalContent = $content
        
        # Remover imports duplicados de R - manter apenas um
        $rImports = [regex]::Matches($content, '(?m)^import\s+.*\.R\s*$')
        if ($rImports.Count -gt 1) {
            $firstRImport = $rImports[0].Value
            foreach ($match in $rImports) {
                if ($match.Value -ne $firstRImport) {
                    $content = $content -replace [regex]::Escape($match.Value), ""
                }
            }
            # Garantir que temos o import correto
            if ($firstRImport -notmatch 'com\.example\.gestaobilhares\.ui\.R') {
                $content = $content -replace [regex]::Escape($firstRImport), "import com.example.gestaobilhares.ui.R"
            }
        }
        
        # Remover imports duplicados de Timber
        $timberImports = [regex]::Matches($content, '(?m)^import\s+.*Timber.*$')
        if ($timberImports.Count -gt 1) {
            $firstTimberImport = $timberImports[0].Value
            foreach ($match in $timberImports) {
                if ($match.Value -ne $firstTimberImport) {
                    $content = $content -replace [regex]::Escape($match.Value), ""
                }
            }
            # Garantir import correto
            if ($firstTimberImport -notmatch 'timber\.log\.Timber') {
                $content = $content -replace [regex]::Escape($firstTimberImport), "import timber.log.Timber"
            }
        }
        
        # Adicionar import de R se usar mas nao tiver
        if ($content -match '\bR\.(id|string|color|drawable|layout|plurals|navigation|sync|nav_|style)' -and $content -notmatch '(?m)^import\s+com\.example\.gestaobilhares\.ui\.R\s*$') {
            $packageMatch = [regex]::Match($content, '(?m)^(package\s+[^\r\n]+)')
            if ($packageMatch.Success) {
                $content = $content -replace '(?m)^(package\s+[^\r\n]+)', "`$1`r`nimport com.example.gestaobilhares.ui.R"
            } else {
                $content = "import com.example.gestaobilhares.ui.R`r`n" + $content
            }
        }
        
        # Adicionar import de Timber se usar mas nao tiver
        if ($content -match '\bTimber\.' -and $content -notmatch '(?m)^import\s+timber\.log\.Timber\s*$') {
            $packageMatch = [regex]::Match($content, '(?m)^(package\s+[^\r\n]+)')
            if ($packageMatch.Success) {
                $content = $content -replace '(?m)^(package\s+[^\r\n]+)', "`$1`r`nimport timber.log.Timber"
            } else {
                $content = "import timber.log.Timber`r`n" + $content
            }
        }
        
        # Corrigir referencias R.color, R.drawable sem prefixo completo
        $content = $content -replace '(?<!com\.example\.gestaobilhares\.ui\.)R\.color\.', 'com.example.gestaobilhares.ui.R.color.'
        $content = $content -replace '(?<!com\.example\.gestaobilhares\.ui\.)R\.drawable\.', 'com.example.gestaobilhares.ui.R.drawable.'
        $content = $content -replace '(?<!com\.example\.gestaobilhares\.ui\.)R\.plurals\.', 'com.example.gestaobilhares.ui.R.plurals.'
        $content = $content -replace '(?<!com\.example\.gestaobilhares\.ui\.)R\.sync\.', 'com.example.gestaobilhares.ui.R.sync.'
        $content = $content -replace '(?<!com\.example\.gestaobilhares\.ui\.)R\.style\.', 'com.example.gestaobilhares.ui.R.style.'
        $content = $content -replace '(?<!com\.example\.gestaobilhares\.ui\.)R\.nav_', 'com.example.gestaobilhares.ui.R.nav_'
        
        # Limpar linhas vazias duplicadas
        $content = $content -replace '(?m)^\s*$\r?\n\s*$', "`r`n"
        
        if ($content -ne $originalContent) {
            [System.IO.File]::WriteAllText($file.FullName, $content, [System.Text.Encoding]::UTF8)
            $updatedCount++
        }
    } catch {
        Write-Host "  ERRO: $($file.Name) - $_" -ForegroundColor Red
    }
}

Write-Host "`nArquivos atualizados: $updatedCount" -ForegroundColor Green

# FASE 2: Copiar recursos faltantes do app para ui
Write-Host "`nCopiando recursos faltantes..." -ForegroundColor Yellow

$appResPath = Join-Path $rootPath "app\src\main\res"
$uiResPath = Join-Path $rootPath "ui\src\main\res"

# Copiar colors.xml se nao existir
$uiColorsPath = Join-Path $uiResPath "values\colors.xml"
if (-not (Test-Path $uiColorsPath)) {
    $appColorsPath = Join-Path $appResPath "values\colors.xml"
    if (Test-Path $appColorsPath) {
        Copy-Item -Path $appColorsPath -Destination $uiColorsPath -Force
        Write-Host "  Copiado: colors.xml" -ForegroundColor Green
    }
}

# Copiar drawables se nao existirem
$uiDrawablePath = Join-Path $uiResPath "drawable"
$appDrawablePath = Join-Path $appResPath "drawable"
if (-not (Test-Path $uiDrawablePath) -and (Test-Path $appDrawablePath)) {
    New-Item -ItemType Directory -Path $uiDrawablePath -Force | Out-Null
    Copy-Item -Path "$appDrawablePath\*" -Destination $uiDrawablePath -Recurse -Force
    Write-Host "  Copiado: drawables" -ForegroundColor Green
}

# Copiar navigation se nao existir
$uiNavPath = Join-Path $uiResPath "navigation"
$appNavPath = Join-Path $appResPath "navigation"
if (-not (Test-Path $uiNavPath) -and (Test-Path $appNavPath)) {
    New-Item -ItemType Directory -Path $uiNavPath -Force | Out-Null
    Copy-Item -Path "$appNavPath\*" -Destination $uiNavPath -Recurse -Force
    Write-Host "  Copiado: navigation" -ForegroundColor Green
}

# Copiar plurals.xml se nao existir
$uiPluralsPath = Join-Path $uiResPath "values\plurals.xml"
if (-not (Test-Path $uiPluralsPath)) {
    $appPluralsPath = Join-Path $appResPath "values\plurals.xml"
    if (Test-Path $appPluralsPath) {
        Copy-Item -Path $appPluralsPath -Destination $uiPluralsPath -Force
        Write-Host "  Copiado: plurals.xml" -ForegroundColor Green
    }
}

Write-Host "=== CONCLUIDO ===" -ForegroundColor Green

