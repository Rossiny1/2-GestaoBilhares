# Script eficiente para corrigir estrutura e imports do módulo :ui
# Compatível com PowerShell 5.1+
$ErrorActionPreference = "Stop"

$rootPath = Split-Path -Parent $PSScriptRoot
$sourcePath = Join-Path $rootPath "ui\src\main\java\com\example\gestaobilhares\ui\c\main\java\com\example\gestaobilhares\ui"
$targetPath = Join-Path $rootPath "ui\src\main\java\com\example\gestaobilhares\ui"

Write-Host "=== CORRECAO DO MODULO :ui ===" -ForegroundColor Cyan

# FASE 1: Mover arquivos
if (Test-Path $sourcePath) {
    Write-Host "Fase 1: Movendo arquivos..." -ForegroundColor Yellow
    $files = Get-ChildItem -Path $sourcePath -Recurse -File
    
    foreach ($file in $files) {
        $relativePath = $file.FullName.Substring($sourcePath.Length + 1)
        $destFile = Join-Path $targetPath $relativePath
        $destDir = Split-Path $destFile -Parent
        
        if (-not (Test-Path $destDir)) {
            New-Item -ItemType Directory -Path $destDir -Force | Out-Null
        }
        
        Copy-Item -Path $file.FullName -Destination $destFile -Force -ErrorAction SilentlyContinue
    }
    
    Write-Host "Removendo estrutura duplicada..." -ForegroundColor Yellow
    $cPath = Join-Path $rootPath "ui\src\main\java\com\example\gestaobilhares\ui\c"
    if (Test-Path $cPath) {
        Remove-Item -Path $cPath -Recurse -Force -ErrorAction SilentlyContinue
    }
    Write-Host "Arquivos movidos!" -ForegroundColor Green
}

# FASE 2: Corrigir imports e referências
Write-Host "`nFase 2: Corrigindo imports e referências..." -ForegroundColor Yellow

$uiPath = Join-Path $rootPath "ui\src\main\java"
if (-not (Test-Path $uiPath)) {
    Write-Host "ERRO: Diretório não encontrado: $uiPath" -ForegroundColor Red
    exit 1
}

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
        
        # Aplicar substituições
        $content = $content -replace '(?m)^import com\.example\.gestaobilhares\.R$', 'import com.example.gestaobilhares.ui.R'
        $content = $content -replace '(?m)^import com\.example\.gestaobilhares\.databinding\.', 'import com.example.gestaobilhares.ui.databinding.'
        $content = $content -replace 'import com\.jakewharton\.timber\.Timber', 'import timber.log.Timber'
        $content = $content -replace '\bR\.id\.', 'com.example.gestaobilhares.ui.R.id.'
        $content = $content -replace '\bR\.string\.', 'com.example.gestaobilhares.ui.R.string.'
        $content = $content -replace '\bR\.color\.', 'com.example.gestaobilhares.ui.R.color.'
        $content = $content -replace '\bR\.drawable\.', 'com.example.gestaobilhares.ui.R.drawable.'
        $content = $content -replace '\bR\.layout\.', 'com.example.gestaobilhares.ui.R.layout.'
        $content = $content -replace '\bR\.plurals\.', 'com.example.gestaobilhares.ui.R.plurals.'
        $content = $content -replace '\bR\.navigation\.', 'com.example.gestaobilhares.ui.R.navigation.'
        $content = $content -replace '\bR\.sync\.', 'com.example.gestaobilhares.ui.R.sync.'
        $content = $content -replace '\bR\.nav_', 'com.example.gestaobilhares.ui.R.nav_'
        
        # Adicionar import de R.ui se necessário
        if ($content -match '\bcom\.example\.gestaobilhares\.ui\.R\.' -and $content -notmatch '(?m)^import com\.example\.gestaobilhares\.ui\.R$') {
            if ($content -match '(?m)^(package\s+[^\r\n]+)') {
                $content = $content -replace '(?m)^(package\s+[^\r\n]+)', "`$1`r`nimport com.example.gestaobilhares.ui.R"
            } else {
                $content = "import com.example.gestaobilhares.ui.R`r`n" + $content
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

