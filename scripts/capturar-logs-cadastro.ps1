# Script para capturar logs do cadastro de colaboradores
# Filtra logs do AuthViewModel relacionados ao cadastro e sincronização

# Encontrar o caminho do ADB
$adbPath = $null

# Tentar caminhos comuns do Android SDK
$possiblePaths = @(
    "$env:USERPROFILE\AppData\Local\Android\Sdk\platform-tools\adb.exe",
    "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
    "$env:ANDROID_HOME\platform-tools\adb.exe",
    "$env:ANDROID_SDK_ROOT\platform-tools\adb.exe"
)

# Tentar ler do local.properties se existir
if (Test-Path "local.properties") {
    $localProps = Get-Content "local.properties" | Where-Object { $_ -match "sdk\.dir" }
    if ($localProps) {
        $sdkPath = ($localProps -split "=")[1].Trim()
        # Remover barras duplas escapadas e normalizar caminho
        $sdkPath = $sdkPath -replace "\\\\", "\" -replace "\\:", ":"
        $possiblePaths = @("$sdkPath\platform-tools\adb.exe") + $possiblePaths
    }
}

foreach ($path in $possiblePaths) {
    if (Test-Path $path) {
        $adbPath = $path
        break
    }
}

if (-not $adbPath) {
    Write-Host "❌ ADB não encontrado!" -ForegroundColor Red
    Write-Host "Tentou os seguintes caminhos:" -ForegroundColor Yellow
    foreach ($path in $possiblePaths) {
        Write-Host "  - $path" -ForegroundColor Gray
    }
    Write-Host ""
    Write-Host "Por favor, instale o Android SDK ou configure a variável ANDROID_HOME" -ForegroundColor Yellow
    exit 1
}

# Verificar se há dispositivo conectado
$devices = & $adbPath devices 2>&1
$deviceConnected = $devices | Select-String -Pattern "device$" | Where-Object { $_ -notmatch "List of devices" }

if (-not $deviceConnected) {
    Write-Host "❌ Nenhum dispositivo Android conectado!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Por favor:" -ForegroundColor Yellow
    Write-Host "  1. Conecte seu dispositivo via USB" -ForegroundColor Gray
    Write-Host "  2. Ative a depuração USB no dispositivo" -ForegroundColor Gray
    Write-Host "  3. Aceite a autorização de depuração USB" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Dispositivos detectados:" -ForegroundColor Yellow
    Write-Host $devices
    exit 1
}

Write-Host "=== CAPTURANDO LOGS DE CADASTRO DE COLABORADORES ===" -ForegroundColor Cyan
Write-Host "ADB encontrado em: $adbPath" -ForegroundColor Green
Write-Host "Dispositivo conectado: $($deviceConnected -replace '\s+device$', '')" -ForegroundColor Green
Write-Host ""
Write-Host "Filtros aplicados:" -ForegroundColor Yellow
Write-Host "  - AuthViewModel (todos os logs do ViewModel de autenticação)" -ForegroundColor Gray
Write-Host "  - SINCRONIZANDO (logs de sincronização)" -ForegroundColor Gray
Write-Host "  - PERMISSION_DENIED (erros de permissão)" -ForegroundColor Gray
Write-Host "  - Autenticação anônima (logs de login anônimo)" -ForegroundColor Gray
Write-Host ""
Write-Host "Pressione Ctrl+C para parar a captura" -ForegroundColor Yellow
Write-Host ""

# Comando logcat com filtros
try {
    & $adbPath logcat -c  # Limpar logs anteriores
    Write-Host "Logs anteriores limpos. Iniciando captura..." -ForegroundColor Green
    Write-Host ""
    
    & $adbPath logcat | Select-String -Pattern "AuthViewModel|SINCRONIZANDO|PERMISSION_DENIED|anonymous|signInAnonymously|sincronizarColaborador|cadastro|register" -CaseSensitive:$false
} catch {
    Write-Host "❌ Erro ao executar logcat: $_" -ForegroundColor Red
    exit 1
}

