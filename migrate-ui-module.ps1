# Script para migrar arquivos do módulo :ui
$ErrorActionPreference = "Stop"

$source = "app\src\main\java\com\example\gestaobilhares\ui"
$dest = "ui\src\main\java\com\example\gestaobilhares\ui"

Write-Host "Migrando arquivos do módulo :ui..." -ForegroundColor Cyan

# Criar estrutura de diretórios base
if (-not (Test-Path $dest)) {
    New-Item -ItemType Directory -Path $dest -Force | Out-Null
}

# Copiar arquivos recursivamente mantendo estrutura
Write-Host "Copiando arquivos UI..." -ForegroundColor Yellow
if (Test-Path $source) {
    Get-ChildItem -Path $source -Recurse -File -Filter "*.kt" | ForEach-Object {
        $relativePath = $_.FullName.Substring($source.Length)
        $destinationPath = Join-Path $dest $relativePath
        
        $fileDir = Split-Path $destinationPath -Parent
        if (-not (Test-Path $fileDir)) {
            New-Item -ItemType Directory -Path $fileDir -Force | Out-Null
        }

        Copy-Item -Path $_.FullName -Destination $destinationPath -Force
        Write-Host "Copiado: $relativePath" -ForegroundColor DarkGreen
    }
}

$copiedFilesCount = if (Test-Path $dest) { (Get-ChildItem -Path $dest -Recurse -File -Filter "*.kt" -ErrorAction SilentlyContinue).Count } else { 0 }
Write-Host "✅ Migração :ui concluída. Total de arquivos .kt copiados: $copiedFilesCount" -ForegroundColor Green

