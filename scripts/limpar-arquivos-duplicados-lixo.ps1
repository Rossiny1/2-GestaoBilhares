# Script para identificar e limpar arquivos duplicados e lixo do projeto
# Executa de forma segura, mostrando o que sera excluido antes de excluir

$ErrorActionPreference = "Continue"
$arquivosParaExcluir = @()
$arquivosDuplicados = @()

Write-Host "=== ANALISE DE ARQUIVOS DUPLICADOS E LIXO ===" -ForegroundColor Cyan
Write-Host ""

# 1. IDENTIFICAR LAYOUTS DUPLICADOS
Write-Host "[1/6] Verificando layouts duplicados..." -ForegroundColor Yellow
$layoutApp = "app\src\main\res\layout\fragment_client_detail.xml"
$layoutUi = "ui\src\main\res\layout\fragment_client_detail.xml"

if (Test-Path $layoutApp) {
    Write-Host "   ENCONTRADO: $layoutApp" -ForegroundColor Yellow
    $arquivosDuplicados += @{
        Tipo = "Layout Duplicado"
        Arquivo = $layoutApp
        Motivo = "Layout duplicado - o correto esta em ui/src/main/res/layout/"
        Seguro = $true
    }
}

if (Test-Path $layoutUi) {
    Write-Host "   ENCONTRADO: $layoutUi (CORRETO - manter)" -ForegroundColor Green
}
Write-Host ""

# 2. IDENTIFICAR NAV_GRAPH DUPLICADOS
Write-Host "[2/6] Verificando nav_graph duplicados..." -ForegroundColor Yellow
$navApp = "app\src\main\res\navigation\nav_graph.xml"
$navUi = "ui\src\main\res\navigation\nav_graph.xml"

if (Test-Path $navApp) {
    Write-Host "   ENCONTRADO: $navApp" -ForegroundColor Yellow
    $arquivosDuplicados += @{
        Tipo = "Navigation Duplicado"
        Arquivo = $navApp
        Motivo = "Navigation graph duplicado - verificar qual esta sendo usado"
        Seguro = $false
    }
}

if (Test-Path $navUi) {
    Write-Host "   ENCONTRADO: $navUi" -ForegroundColor Yellow
}
Write-Host ""

# 3. IDENTIFICAR ARQUIVOS DE LOG ANTIGOS
Write-Host "[3/6] Verificando arquivos de log antigos..." -ForegroundColor Yellow
$logs = Get-ChildItem -Path . -Filter "logcat*.txt" -File | Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-7) }
foreach ($log in $logs) {
    Write-Host "   ENCONTRADO: $($log.Name) (modificado em $($log.LastWriteTime))" -ForegroundColor Yellow
    $arquivosParaExcluir += @{
        Tipo = "Log Antigo"
        Arquivo = $log.FullName
        Motivo = "Log antigo (mais de 7 dias)"
        Seguro = $true
    }
}
Write-Host ""

# 4. IDENTIFICAR ARQUIVOS TEMPORARIOS
Write-Host "[4/6] Verificando arquivos temporarios..." -ForegroundColor Yellow
$tempFiles = @(
    "temp_sync_manager_v2.kt",
    "temp_vehicle_detail_old.kt",
    "limpar_banco.kt"
)
foreach ($temp in $tempFiles) {
    if (Test-Path $temp) {
        Write-Host "   ENCONTRADO: $temp" -ForegroundColor Yellow
        $arquivosParaExcluir += @{
            Tipo = "Arquivo Temporario"
            Arquivo = (Resolve-Path $temp).Path
            Motivo = "Arquivo temporario ou de backup"
            Seguro = $true
        }
    }
}
Write-Host ""

# 5. IDENTIFICAR SCRIPTS DUPLICADOS/OBsoletos
Write-Host "[5/6] Verificando scripts duplicados/obsoletos..." -ForegroundColor Yellow
$scriptsObsoletos = @(
    "build-output-ui.txt",
    "build-output.txt",
    "build-result.txt",
    "erros-build.txt",
    "sembleDebug",
    "tallDebug",
    "tatus",
    "how 712b798settings.gradle.kts",
    "ervices.json"
)
foreach ($script in $scriptsObsoletos) {
    if (Test-Path $script) {
        Write-Host "   ENCONTRADO: $script" -ForegroundColor Yellow
        $arquivosParaExcluir += @{
            Tipo = "Arquivo Obsoleto"
            Arquivo = (Resolve-Path $script).Path
            Motivo = "Arquivo de build/output obsoleto ou corrompido"
            Seguro = $true
        }
    }
}
Write-Host ""

# 6. IDENTIFICAR SCRIPTS DE BUILD DUPLICADOS (manter apenas os mais recentes/uteis)
Write-Host "[6/6] Verificando scripts de build duplicados..." -ForegroundColor Yellow
$scriptsBuild = Get-ChildItem -Path . -Filter "build*.ps1" -File | Sort-Object LastWriteTime -Descending
$scriptsBuildUteis = @("build-apk-cursor.ps1", "compilar-rapido.ps1", "limpar-e-compilar.ps1", "reinstalar-app-completo.ps1")
$scriptsBuildRemover = $scriptsBuild | Where-Object { $_.Name -notin $scriptsBuildUteis -and $_.LastWriteTime -lt (Get-Date).AddDays(-30) }

foreach ($script in $scriptsBuildRemover) {
    Write-Host "   ENCONTRADO: $($script.Name) (antigo, pode ser removido)" -ForegroundColor Yellow
    $arquivosParaExcluir += @{
        Tipo = "Script Build Antigo"
        Arquivo = $script.FullName
        Motivo = "Script de build antigo (mais de 30 dias) e nao esta na lista de utéis"
        Seguro = $true
    }
}
Write-Host ""

# RESUMO
Write-Host "=== RESUMO ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "ARQUIVOS DUPLICADOS ENCONTRADOS: $($arquivosDuplicados.Count)" -ForegroundColor Yellow
foreach ($dup in $arquivosDuplicados) {
    $cor = if ($dup.Seguro) { "Green" } else { "Red" }
    Write-Host "  [$($dup.Tipo)] $($dup.Arquivo)" -ForegroundColor $cor
    Write-Host "    Motivo: $($dup.Motivo)" -ForegroundColor Gray
}
Write-Host ""

Write-Host "ARQUIVOS PARA EXCLUIR: $($arquivosParaExcluir.Count)" -ForegroundColor Yellow
foreach ($arquivo in $arquivosParaExcluir) {
    Write-Host "  [$($arquivo.Tipo)] $($arquivo.Arquivo)" -ForegroundColor Green
    Write-Host "    Motivo: $($arquivo.Motivo)" -ForegroundColor Gray
}
Write-Host ""

# CONFIRMACAO
Write-Host "=== CONFIRMACAO ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Deseja excluir os arquivos listados acima? (S/N)" -ForegroundColor Yellow
$confirmacao = Read-Host

if ($confirmacao -eq "S" -or $confirmacao -eq "s") {
    Write-Host ""
    Write-Host "Excluindo arquivos..." -ForegroundColor Yellow
    
    $excluidos = 0
    $erros = 0
    
    # Excluir arquivos seguros
    foreach ($arquivo in $arquivosParaExcluir) {
        try {
            Remove-Item -Path $arquivo.Arquivo -Force -ErrorAction Stop
            Write-Host "  [OK] Excluido: $($arquivo.Arquivo)" -ForegroundColor Green
            $excluidos++
        } catch {
            Write-Host "  [ERRO] Falha ao excluir: $($arquivo.Arquivo) - $($_.Exception.Message)" -ForegroundColor Red
            $erros++
        }
    }
    
    # Excluir duplicados seguros
    foreach ($dup in $arquivosDuplicados | Where-Object { $_.Seguro }) {
        try {
            Remove-Item -Path $dup.Arquivo -Force -ErrorAction Stop
            Write-Host "  [OK] Excluido: $($dup.Arquivo)" -ForegroundColor Green
            $excluidos++
        } catch {
            Write-Host "  [ERRO] Falha ao excluir: $($dup.Arquivo) - $($_.Exception.Message)" -ForegroundColor Red
            $erros++
        }
    }
    
    Write-Host ""
    Write-Host "=== RESULTADO ===" -ForegroundColor Cyan
    Write-Host "  Excluidos: $excluidos" -ForegroundColor Green
    Write-Host "  Erros: $erros" -ForegroundColor $(if ($erros -eq 0) { "Green" } else { "Red" })
    Write-Host ""
    
    if ($arquivosDuplicados | Where-Object { -not $_.Seguro }) {
        Write-Host "ATENCAO: Existem duplicados que precisam de verificacao manual:" -ForegroundColor Yellow
        foreach ($dup in $arquivosDuplicados | Where-Object { -not $_.Seguro }) {
            Write-Host "  - $($dup.Arquivo)" -ForegroundColor Yellow
            Write-Host "    Motivo: $($dup.Motivo)" -ForegroundColor Gray
        }
    }
} else {
    Write-Host ""
    Write-Host "Operacao cancelada. Nenhum arquivo foi excluido." -ForegroundColor Yellow
}

Write-Host ""

