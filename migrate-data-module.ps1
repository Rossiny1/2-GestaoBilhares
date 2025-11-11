# Script para migrar arquivos do módulo :data
# Não requer interação do usuário

$ErrorActionPreference = "Stop"

$source = "app\src\main\java\com\example\gestaobilhares\data"
$dest = "data\src\main\java\com\example\gestaobilhares\data"

Write-Host "Migrando arquivos do módulo :data..." -ForegroundColor Cyan

# Criar estrutura de diretórios
$dirs = @(
    "$dest\dao",
    "$dest\database\dao",
    "$dest\entities",
    "$dest\repository\internal",
    "$dest\factory"
)

foreach ($dir in $dirs) {
    if (-not (Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
        Write-Host "Criado: $dir" -ForegroundColor Green
    }
}

# Copiar arquivos recursivamente
Write-Host "Copiando arquivos..." -ForegroundColor Yellow
Get-ChildItem -Path $source -Recurse -File -Filter "*.kt" | ForEach-Object {
    $relativePath = $_.FullName.Substring((Resolve-Path $source).Path.Length + 1)
    $destPath = Join-Path $dest $relativePath
    $destDir = Split-Path $destPath -Parent
    
    if (-not (Test-Path $destDir)) {
        New-Item -ItemType Directory -Path $destDir -Force | Out-Null
    }
    
    Copy-Item -Path $_.FullName -Destination $destPath -Force
    Write-Host "Copiado: $relativePath" -ForegroundColor Gray
}

$count = (Get-ChildItem -Path $dest -Recurse -File -Filter "*.kt").Count
Write-Host "`nMigração concluída! Total de arquivos: $count" -ForegroundColor Green

