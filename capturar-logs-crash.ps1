# ========================================
# CAPTURADOR DE LOGS PARA CRASHES
# ========================================

Write-Host "Capturando logs do GestaoBilhares..." -ForegroundColor Cyan

# Caminho do ADB
$ADB = "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Nome do pacote do app
$PACKAGE_NAME = "com.example.gestaobilhares"

# Limpar logs antigos
Write-Host "Limpando logs antigos..." -ForegroundColor Yellow
& $ADB logcat -c

# Capturar logs específicos do app
Write-Host "Capturando logs do app..." -ForegroundColor Green
Write-Host "Pressione Ctrl+C para parar a captura" -ForegroundColor Yellow

# Capturar logs com filtros específicos
& $ADB logcat -s "ClientRegister:*" "ClientRegisterViewModel:*" "ClienteRepository:*" "DatabaseModule:*" "AndroidRuntime:E" "FATAL:*" | Tee-Object -FilePath "logcat-crash-$(Get-Date -Format 'yyyyMMdd-HHmmss').txt" 