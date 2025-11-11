# Script para migrar arquivos do módulo :sync
$ErrorActionPreference = "Stop"

$source = "app\src\main\java\com\example\gestaobilhares\sync"
$dest = "sync\src\main\java\com\example\gestaobilhares\sync"

Write-Host "Migrando arquivos do módulo :sync..." -ForegroundColor Cyan

# Criar estrutura de diretórios
$dirs = @(
    "$dest\handlers"
)

foreach ($dir in $dirs) {
    if (-not (Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
        Write-Host "Criado: $dir" -ForegroundColor Green
    }
}

# Copiar arquivos recursivamente
Write-Host "Copiando arquivos sync..." -ForegroundColor Yellow
if (Test-Path $source) {
    Get-ChildItem -Path $source -Recurse -File -Filter "*.kt" | ForEach-Object {
        $relativePath = $_.FullName.Substring($source.Length)
        $destinationPath = Join-Path $dest $relativePath
        
        $fileDir = Split-Path $destinationPath -Parent
        if (-not (Test-Path $fileDir)) {
            New-Item -ItemType Directory -Path $fileDir -Force | Out-Null
        }

        Copy-Item -Path $_.FullName -Destination $destinationPath -Force
        Write-Host "Copiado: $($_.Name)" -ForegroundColor DarkGreen
    }
}

# Migrar workers também
$workersSource = "app\src\main\java\com\example\gestaobilhares\workers"
$workersDest = "sync\src\main\java\com\example\gestaobilhares\workers"

if (Test-Path $workersSource) {
    if (-not (Test-Path $workersDest)) {
        New-Item -ItemType Directory -Path $workersDest -Force | Out-Null
    }
    
    Write-Host "Copiando workers..." -ForegroundColor Yellow
    Get-ChildItem -Path $workersSource -File -Filter "*.kt" | ForEach-Object {
        Copy-Item -Path $_.FullName -Destination "$workersDest\$($_.Name)" -Force
        Write-Host "Copiado worker: $($_.Name)" -ForegroundColor DarkGreen
    }
}

$syncCount = if (Test-Path $dest) { (Get-ChildItem -Path $dest -Recurse -File -Filter "*.kt" -ErrorAction SilentlyContinue).Count } else { 0 }
$workersCount = if (Test-Path $workersDest) { (Get-ChildItem -Path $workersDest -Recurse -File -Filter "*.kt" -ErrorAction SilentlyContinue).Count } else { 0 }
$copiedFilesCount = $syncCount + $workersCount

Write-Host "✅ Migração :sync concluída. Total de arquivos .kt copiados: $copiedFilesCount" -ForegroundColor Green

