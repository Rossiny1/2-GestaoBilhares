# Script para capturar logs do login
Write-Host "=== CAPTURANDO LOGS DO LOGIN ===" -ForegroundColor Green
Write-Host "1. Instale o APK atualizado" -ForegroundColor Yellow
Write-Host "2. Abra o app e tente fazer login" -ForegroundColor Yellow
Write-Host "3. Os logs serão capturados automaticamente" -ForegroundColor Yellow
Write-Host "4. Pressione Ctrl+C para parar a captura" -ForegroundColor Red
Write-Host ""

# Limpar logs antigos
& "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe" logcat -c

# Capturar logs em tempo real
& "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe" logcat | Select-String "AuthViewModel|GestaoBilharesApp|Firebase|login|ERROR|Exception" 