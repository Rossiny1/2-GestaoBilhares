# Script para limpar o banco de dados e forçar recriação
Write-Host "=== LIMPANDO BANCO DE DADOS ===" -ForegroundColor Green

# Parar o app se estiver rodando
Write-Host "1. Parando o app..." -ForegroundColor Yellow
& "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe" shell am force-stop com.example.gestaobilhares

# Aguardar um pouco
Start-Sleep -Seconds 2

# Limpar dados do app
Write-Host "2. Limpando dados do app..." -ForegroundColor Yellow
& "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe" shell pm clear com.example.gestaobilhares

# Limpar cache do app
Write-Host "3. Limpando cache..." -ForegroundColor Yellow
& "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe" shell rm -rf /data/data/com.example.gestaobilhares/databases/

Write-Host "✅ Banco de dados limpo com sucesso!" -ForegroundColor Green
Write-Host "Agora você pode reinstalar o APK e o banco será recriado do zero." -ForegroundColor Cyan
