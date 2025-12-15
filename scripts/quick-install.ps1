# ========================================
# INSTALACAO RAPIDA APK - GESTAO BILHARES
# ========================================

Write-Host "Instalando GestaoBilhares..." -ForegroundColor Cyan

# Caminho do ADB
$ADB = "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$APK = "app\build\outputs\apk\debug\app-debug.apk"

# Verificar se APK existe
if (-not (Test-Path $APK)) {
    Write-Host "ERRO: APK nao encontrado! Execute: .\gradlew assembleDebug" -ForegroundColor Red
    exit 1
}

# Instalar APK
Write-Host "Instalando APK..." -ForegroundColor Yellow
& $ADB install -r $APK

if ($LASTEXITCODE -eq 0) {
    Write-Host "SUCCESS: APK instalado com sucesso!" -ForegroundColor Green
    
    # Abrir app
    Write-Host "Abrindo aplicacao..." -ForegroundColor Cyan
    & $ADB shell am start -n "com.example.gestaobilhares/.MainActivity"
    
    Write-Host "Pronto! App aberto no dispositivo." -ForegroundColor Green
} else {
    Write-Host "ERRO: Falha na instalacao!" -ForegroundColor Red
} 