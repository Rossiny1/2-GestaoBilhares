# Script para verificar e remover layouts duplicados entre app e ui
Write-Host "=== VERIFICACAO DE LAYOUTS DUPLICADOS ===" -ForegroundColor Cyan
Write-Host ""

$appLayoutPath = "app\src\main\res\layout"
$uiLayoutPath = "ui\src\main\res\layout"

if (-not (Test-Path $appLayoutPath)) {
    Write-Host "[ERRO] Diretorio app\src\main\res\layout nao encontrado" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $uiLayoutPath)) {
    Write-Host "[ERRO] Diretorio ui\src\main\res\layout nao encontrado" -ForegroundColor Red
    exit 1
}

# Obter listas de arquivos
$appLayouts = Get-ChildItem -Path $appLayoutPath -File -ErrorAction SilentlyContinue
$uiLayouts = Get-ChildItem -Path $uiLayoutPath -File -ErrorAction SilentlyContinue

$appLayoutNames = $appLayouts | Select-Object -ExpandProperty Name
$uiLayoutNames = $uiLayouts | Select-Object -ExpandProperty Name

# Encontrar duplicados
$duplicados = $appLayoutNames | Where-Object { $uiLayoutNames -contains $_ }

Write-Host "Total de layouts em app: $($appLayouts.Count)" -ForegroundColor Yellow
Write-Host "Total de layouts em ui: $($uiLayouts.Count)" -ForegroundColor Yellow
Write-Host "Layouts duplicados encontrados: $($duplicados.Count)" -ForegroundColor Cyan
Write-Host ""

if ($duplicados.Count -eq 0) {
    Write-Host "Nenhum layout duplicado encontrado!" -ForegroundColor Green
    exit 0
}

Write-Host "Lista de layouts duplicados:" -ForegroundColor Yellow
$duplicados | ForEach-Object {
    Write-Host "  - $_" -ForegroundColor White
}
Write-Host ""

# Verificar quais sao identicos e quais sao diferentes
$identicos = @()
$diferentes = @()

foreach ($layout in $duplicados) {
    $appFile = Join-Path $appLayoutPath $layout
    $uiFile = Join-Path $uiLayoutPath $layout
    
    if ((Test-Path $appFile) -and (Test-Path $uiFile)) {
        $appHash = (Get-FileHash -Path $appFile -Algorithm MD5).Hash
        $uiHash = (Get-FileHash -Path $uiFile -Algorithm MD5).Hash
        
        if ($appHash -eq $uiHash) {
            $identicos += $layout
        } else {
            $diferentes += $layout
        }
    }
}

Write-Host "Layouts identicos: $($identicos.Count)" -ForegroundColor Green
if ($identicos.Count -gt 0) {
    $identicos | ForEach-Object {
        Write-Host "  [IDENTICO] $_" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "Layouts diferentes: $($diferentes.Count)" -ForegroundColor Yellow
if ($diferentes.Count -gt 0) {
    $diferentes | ForEach-Object {
        Write-Host "  [DIFERENTE] $_" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "=== REMOCAO DE DUPLICADOS ===" -ForegroundColor Cyan
Write-Host ""

# Remover layouts duplicados do app (mantendo apenas no ui)
$removidos = 0
$erros = 0

foreach ($layout in $duplicados) {
    $appFile = Join-Path $appLayoutPath $layout
    
    if (Test-Path $appFile) {
        try {
            Remove-Item -Path $appFile -Force -ErrorAction Stop
            Write-Host "  [OK] Removido: $layout" -ForegroundColor Green
            $removidos++
        } catch {
            Write-Host "  [ERRO] Nao foi possivel remover: $layout - $_" -ForegroundColor Red
            $erros++
        }
    }
}

Write-Host ""
Write-Host "=== RESUMO ===" -ForegroundColor Cyan
Write-Host "  Layouts removidos: $removidos" -ForegroundColor Green
if ($erros -gt 0) {
    Write-Host "  Erros: $erros" -ForegroundColor Red
}
Write-Host ""
Write-Host "Concluido!" -ForegroundColor Green

