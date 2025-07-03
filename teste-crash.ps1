# ========================================
# TESTE AUTOMATIZADO DE CRASHES
# ========================================

Write-Host "Teste Automatizado de Crashes - GestaoBilhares" -ForegroundColor Cyan

# Caminho do ADB
$ADB = "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$APK = "app\build\outputs\apk\debug\app-debug.apk"

# Verificar se APK existe
if (-not (Test-Path $APK)) {
    Write-Host "ERRO: APK nao encontrado! Execute: .\gradlew assembleDebug" -ForegroundColor Red
    exit 1
}

# Verificar se dispositivo está conectado
Write-Host "Verificando dispositivo..." -ForegroundColor Yellow
$devices = & $ADB devices
if ($devices.Count -lt 2) {
    Write-Host "ERRO: Nenhum dispositivo conectado!" -ForegroundColor Red
    exit 1
}

# Instalar APK
Write-Host "Instalando APK..." -ForegroundColor Yellow
& $ADB install -r $APK

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERRO: Falha na instalacao!" -ForegroundColor Red
    exit 1
}

Write-Host "SUCCESS: APK instalado!" -ForegroundColor Green

# Limpar logs antigos
Write-Host "Limpando logs antigos..." -ForegroundColor Yellow
& $ADB logcat -c

# Iniciar captura de logs em background
Write-Host "Iniciando captura de logs..." -ForegroundColor Green
$logFile = "logcat-crash-$(Get-Date -Format 'yyyyMMdd-HHmmss').txt"
Start-Job -ScriptBlock {
    param($adb, $logFile)
    & $adb logcat -s "ClientRegister:*" "ClientRegisterViewModel:*" "ClienteRepository:*" "DatabaseModule:*" "AndroidRuntime:E" "FATAL:*" | Tee-Object -FilePath $logFile
} -ArgumentList $ADB, $logFile

# Abrir app
Write-Host "Abrindo aplicacao..." -ForegroundColor Cyan
& $ADB shell am start -n "com.example.gestaobilhares/.MainActivity"

Write-Host "App aberto! Agora teste os crashes:" -ForegroundColor Green
Write-Host "1. Tente salvar um novo cliente" -ForegroundColor Yellow
Write-Host "2. Clique em nomes de rua na tela Rota" -ForegroundColor Yellow
Write-Host "3. Aguarde 30 segundos para capturar logs" -ForegroundColor Yellow

Start-Sleep -Seconds 30

# Parar captura de logs
Write-Host "Parando captura de logs..." -ForegroundColor Yellow
& $ADB logcat -d > $logFile

Write-Host "Logs salvos em: $logFile" -ForegroundColor Green
Write-Host "Teste concluido!" -ForegroundColor Cyan 