# Script para migracao do modulo :core
Write-Host "=== MIGRACAO MODULO :core ===" -ForegroundColor Cyan

# Criar estrutura de diretórios
Write-Host "Criando estrutura de diretórios..." -ForegroundColor Yellow
New-Item -ItemType Directory -Path "core\src\main\java\com\example\gestaobilhares\utils" -Force | Out-Null

# Lista de arquivos utils para mover
$utilsFiles = @(
    "AppLogger.kt",
    "AuditReportGenerator.kt",
    "ChartGenerator.kt",
    "ContractPdfGenerator.kt",
    "DateUtils.kt",
    "FinancialCalculator.kt",
    "FirebaseImageUploader.kt",
    "NetworkUtils.kt",
    "PdfReportGenerator.kt",
    "ReciboPrinterHelper.kt",
    "SignatureMetadataCollector.kt",
    "SignatureStatistics.kt",
    "StringUtils.kt",
    "MoneyTextWatcher.kt",
    "ImageCompressionUtils.kt",
    "DataValidator.kt",
    "UserSessionManager.kt",
    "DocumentIntegrityManager.kt",
    "LegalLogger.kt",
    "AditivoPdfGenerator.kt",
    "ClosureReportPdfGenerator.kt"
)

$sourceDir = "app\src\main\java\com\example\gestaobilhares\utils"
$targetDir = "core\src\main\java\com\example\gestaobilhares\utils"

# Mover arquivos
Write-Host "Movendo arquivos utils..." -ForegroundColor Yellow
$movedCount = 0
foreach ($file in $utilsFiles) {
    $sourcePath = Join-Path $sourceDir $file
    $targetPath = Join-Path $targetDir $file
    
    if (Test-Path $sourcePath) {
        Copy-Item -Path $sourcePath -Destination $targetPath -Force
        $movedCount++
        Write-Host "  Movido: $file" -ForegroundColor Green
    } else {
        Write-Host "  Nao encontrado: $file" -ForegroundColor Red
    }
}

Write-Host "Arquivos movidos: $movedCount de $($utilsFiles.Count)" -ForegroundColor Cyan

# Atualizar namespaces
Write-Host "Atualizando namespaces..." -ForegroundColor Yellow
$updatedCount = 0
Get-ChildItem -Path $targetDir -Filter "*.kt" | ForEach-Object {
    $content = Get-Content $_.FullName -Raw -Encoding UTF8
    $originalContent = $content
    
    # Substituir package
    $content = $content -replace 'package com\.example\.gestaobilhares\.utils', 'package com.example.gestaobilhares.core.utils'
    
    if ($content -ne $originalContent) {
        Set-Content -Path $_.FullName -Value $content -Encoding UTF8 -NoNewline
        $updatedCount++
        Write-Host "  Namespace atualizado: $($_.Name)" -ForegroundColor Green
    }
}

Write-Host "Namespaces atualizados: $updatedCount arquivos" -ForegroundColor Cyan
Write-Host "=== MIGRACAO :core CONCLUIDA ===" -ForegroundColor Green

