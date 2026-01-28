# Script PowerShell para capturar logs do Gestao Bilhares
Write-Host "🔍 Iniciando captura de logs do app Gestao Bilhares..." -ForegroundColor Green
Write-Host ""

# Caminho do ADB
$adbPath = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verificar dispositivo
Write-Host "📱 Dispositivos conectados:" -ForegroundColor Yellow
& $adbPath devices
Write-Host ""

# Limpar logs antigos
Write-Host "🧹 Limpando logs antigos..." -ForegroundColor Yellow
& $adbPath logcat -c

Write-Host "📊 Capturando logs com tag: SETTLEMENT" -ForegroundColor Green
Write-Host "Pressione Ctrl+C para parar" -ForegroundColor Red
Write-Host ""

# Capturar logs em tempo real
& $adbPath logcat | Select-String "SETTLEMENT"
