# Script para migracao do modulo :ui
Write-Host "=== MIGRACAO MODULO :ui ===" -ForegroundColor Cyan

# Criar estrutura base
Write-Host "Criando estrutura de diretórios..." -ForegroundColor Yellow
$uiBase = "ui\src\main\java\com\example\gestaobilhares\ui"
New-Item -ItemType Directory -Path $uiBase -Force | Out-Null

# Copiar toda a estrutura de diretórios UI
Write-Host "Copiando estrutura de diretórios UI..." -ForegroundColor Yellow
$uiSource = "app\src\main\java\com\example\gestaobilhares\ui"
$uiTarget = "ui\src\main\java\com\example\gestaobilhares\ui"

if (Test-Path $uiSource) {
    # Copiar recursivamente todos os diretórios
    Get-ChildItem -Path $uiSource -Directory | ForEach-Object {
        $targetDir = Join-Path $uiTarget $_.Name
        New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
        
        # Copiar arquivos recursivamente
        Get-ChildItem -Path $_.FullName -Recurse -File | ForEach-Object {
            $relativePath = $_.FullName.Substring($uiSource.Length + 1)
            $targetPath = Join-Path $uiTarget $relativePath
            $targetFileDir = Split-Path $targetPath -Parent
            New-Item -ItemType Directory -Path $targetFileDir -Force | Out-Null
            Copy-Item -Path $_.FullName -Destination $targetPath -Force
        }
    }
    Write-Host "  Estrutura copiada" -ForegroundColor Green
}

# Copiar recursos XML
Write-Host "Copiando recursos XML..." -ForegroundColor Yellow
$resLayoutSource = "app\src\main\res\layout"
$resLayoutTarget = "ui\src\main\res\layout"

if (Test-Path $resLayoutSource) {
    New-Item -ItemType Directory -Path $resLayoutTarget -Force | Out-Null
    Copy-Item -Path "$resLayoutSource\*" -Destination $resLayoutTarget -Recurse -Force
    Write-Host "  Layouts copiados" -ForegroundColor Green
}

$resNavSource = "app\src\main\res\navigation"
$resNavTarget = "ui\src\main\res\navigation"

if (Test-Path $resNavSource) {
    New-Item -ItemType Directory -Path $resNavTarget -Force | Out-Null
    Copy-Item -Path "$resNavSource\*" -Destination $resNavTarget -Recurse -Force
    Write-Host "  Navigation copiado" -ForegroundColor Green
}

# Atualizar namespaces e imports
Write-Host "Atualizando namespaces e imports..." -ForegroundColor Yellow
$updatedCount = 0

Get-ChildItem -Path $uiTarget -Filter "*.kt" -Recurse | ForEach-Object {
    $content = Get-Content $_.FullName -Raw -Encoding UTF8
    $originalContent = $content
    
    # Atualizar package para manter estrutura (com.example.gestaobilhares.ui.*)
    $content = $content -replace 'package com\.example\.gestaobilhares\.ui\.', 'package com.example.gestaobilhares.ui.'
    
    # Atualizar imports de data
    $content = $content -replace 'import com\.example\.gestaobilhares\.data\.', 'import com.example.gestaobilhares.data.'
    
    # Atualizar imports de utils para core.utils
    $content = $content -replace 'import com\.example\.gestaobilhares\.utils\.', 'import com.example.gestaobilhares.core.utils.'
    
    # Atualizar imports de sync
    $content = $content -replace 'import com\.example\.gestaobilhares\.sync\.', 'import com.example.gestaobilhares.sync.'
    $content = $content -replace 'import com\.example\.gestaobilhares\.workers\.', 'import com.example.gestaobilhares.workers.'
    
    if ($content -ne $originalContent) {
        Set-Content -Path $_.FullName -Value $content -Encoding UTF8 -NoNewline
        $updatedCount++
    }
}

Write-Host "Arquivos atualizados: $updatedCount" -ForegroundColor Cyan
Write-Host "=== MIGRACAO :ui CONCLUIDA ===" -ForegroundColor Green
