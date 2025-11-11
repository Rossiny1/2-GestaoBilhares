# Script para corrigir estrutura do módulo :ui
$ErrorActionPreference = "Stop"

Write-Host "Corrigindo estrutura do módulo :ui..." -ForegroundColor Cyan

$wrongPath = "ui\src\main\java\com\example\gestaobilhares\ui\rc\main\java\com\example\gestaobilhares\ui"
$correctPath = "ui\src\main\java\com\example\gestaobilhares\ui"

if (Test-Path $wrongPath) {
    Write-Host "Movendo arquivos do caminho errado para o correto..." -ForegroundColor Yellow
    
    # Criar diretório correto se não existir
    if (-not (Test-Path $correctPath)) {
        New-Item -ItemType Directory -Path $correctPath -Force | Out-Null
    }
    
    # Mover todos os arquivos e diretórios
    Get-ChildItem -Path $wrongPath -Recurse | ForEach-Object {
        $relativePath = $_.FullName.Substring($wrongPath.Length)
        $destinationPath = Join-Path $correctPath $relativePath
        
        if ($_.PSIsContainer) {
            # É um diretório
            if (-not (Test-Path $destinationPath)) {
                New-Item -ItemType Directory -Path $destinationPath -Force | Out-Null
            }
        } else {
            # É um arquivo
            $fileDir = Split-Path $destinationPath -Parent
            if (-not (Test-Path $fileDir)) {
                New-Item -ItemType Directory -Path $fileDir -Force | Out-Null
            }
            Move-Item -Path $_.FullName -Destination $destinationPath -Force
            Write-Host "Movido: $relativePath" -ForegroundColor DarkGreen
        }
    }
    
    # Remover diretório vazio
    Start-Sleep -Seconds 1
    if (Test-Path $wrongPath) {
        Remove-Item -Path $wrongPath -Recurse -Force -ErrorAction SilentlyContinue
    }
    
    Write-Host "✅ Estrutura corrigida!" -ForegroundColor Green
} else {
    Write-Host "Caminho errado não encontrado. Estrutura pode estar correta." -ForegroundColor Yellow
}

