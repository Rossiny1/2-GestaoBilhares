# Script PowerShell: build-install-apk.ps1
# Builda o APK e instala via ADB em dispositivo USB

Write-Host "Iniciando build do APK..." -ForegroundColor Cyan

# Caminho do projeto (relativo à raiz do workspace)
$projectDir = ".\2-GestaoBilhares"
$apkPath = "$projectDir\app\build\outputs\apk\debug\app-debug.apk"

# Navegar até o diretório do projeto
Set-Location -Path $projectDir

# Build clean + assembleDebug
gradlew.bat clean assembleDebug
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build falhou!" -ForegroundColor Red
    exit 1
}

# Verificar se o APK foi gerado
if (!(Test-Path $apkPath)) {
    Write-Host "APK não encontrado em $apkPath" -ForegroundColor Red
    exit 1
}

Write-Host "Build concluído! APK gerado em $apkPath" -ForegroundColor Green

# Instalar via ADB
$adb = "$env:USERPROFILE\AppData\Local\Android\Sdk\platform-tools\adb.exe"
if (!(Test-Path $adb)) {
    Write-Host "adb.exe não encontrado em $adb" -ForegroundColor Red
    exit 1
}

Write-Host "Instalando APK no dispositivo via ADB..." -ForegroundColor Yellow
& $adb install -r $apkPath
if ($LASTEXITCODE -ne 0) {
    Write-Host "Falha na instalação via ADB!" -ForegroundColor Red
    exit 1
}

Write-Host "APK instalado com sucesso no dispositivo!" -ForegroundColor Green 