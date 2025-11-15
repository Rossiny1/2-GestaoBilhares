# Script para ler logs de sincronizacao em tempo real
# Filtra logs do SyncRepository, RoutesFragment e relacionados

param(
    [string]$adbPath = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe",
    [switch]$Clear = $false
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  LEITURA DE LOGS DE SINCRONIZACAO" -ForegroundColor Cyan
Write-Host "  (Tempo Real)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar se adb existe
if (-not (Test-Path $adbPath)) {
    Write-Host "[ERRO] ADB nao encontrado em: $adbPath" -ForegroundColor Red
    Write-Host "Verifique o caminho do Android SDK" -ForegroundColor Yellow
    exit 1
}

# Verificar se dispositivo esta conectado
Write-Host "Verificando dispositivo conectado..." -ForegroundColor Yellow
$devices = & $adbPath devices

Write-Host "Dispositivos encontrados:" -ForegroundColor Yellow
Write-Host $devices -ForegroundColor Gray

# Verificar se ha dispositivo conectado
$hasDevice = $devices | Select-String "device$"

if (-not $hasDevice) {
    Write-Host "[ERRO] Nenhum dispositivo Android conectado!" -ForegroundColor Red
    Write-Host "Conecte o dispositivo via USB e habilite USB Debugging" -ForegroundColor Yellow
    Read-Host "Pressione Enter para sair"
    exit 1
}

Write-Host "[OK] Dispositivo conectado" -ForegroundColor Green
Write-Host ""

# Tags para filtrar
$tags = @(
    "SyncRepository",
    "RoutesFragment",
    "SyncWorker",
    "SyncManager",
    "AppRepository"
)

# Criar filtro de tags
$tagFilter = ($tags | ForEach-Object { "$_`:`*" }) -join " "

Write-Host "Filtrando logs por tags:" -ForegroundColor Cyan
foreach ($tag in $tags) {
    Write-Host "  - $tag" -ForegroundColor Gray
}
Write-Host ""

# Cores para diferentes tipos de log
function Write-ColoredLog {
    param([string]$line)
    
    if ($line -match "ERROR|Erro|ERRO") {
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "WARN|Aviso|AVISO") {
        Write-Host $line -ForegroundColor Yellow
    }
    elseif ($line -match "SUCCESS|Sucesso|SUCESSO|INSERIDO|ATUALIZADO|concluido|Concluido") {
        Write-Host $line -ForegroundColor Green
    }
    elseif ($line -match "INFO|Info|INFO|Iniciando|Processando") {
        Write-Host $line -ForegroundColor Cyan
    }
    else {
        Write-Host $line -ForegroundColor White
    }
}

# Limpar logs anteriores se solicitado
if ($Clear) {
    Write-Host "Limpando logs anteriores..." -ForegroundColor Yellow
    & $adbPath logcat -c 2>&1 | Out-Null
    Write-Host "[OK] Logs limpos" -ForegroundColor Green
    Write-Host ""
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  INICIANDO MONITORAMENTO EM TEMPO REAL" -ForegroundColor Cyan
Write-Host "  (Pressione Ctrl+C para parar)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Monitorar logs em tempo real
try {
    & $adbPath logcat -v time $tagFilter 2>&1 | ForEach-Object {
        Write-ColoredLog $_
    }
}
catch {
    Write-Host ""
    Write-Host "[ERRO] Erro ao ler logs: $($_.Exception.Message)" -ForegroundColor Red
    Read-Host "Pressione Enter para sair"
    exit 1
}

