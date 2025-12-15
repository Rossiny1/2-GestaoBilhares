# Script para instalar APK de diagnóstico de impressora
# Autor: Assistente Android Senior
# Data: 2025-01-06

Write-Host "=== INSTALADOR APK DIAGNÓSTICO IMPRESSORA ===" -ForegroundColor Cyan
Write-Host "Instalando versão com métodos alternativos de bitmap..." -ForegroundColor Yellow

# Caminho do ADB
$ADB = "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verificar se dispositivo está conectado
Write-Host "Verificando dispositivo..." -ForegroundColor Yellow
$devices = & $ADB devices
if ($devices.Count -lt 2) {
    Write-Host "ERRO: Nenhum dispositivo conectado!" -ForegroundColor Red
    Write-Host "Conecte um dispositivo e tente novamente." -ForegroundColor Red
    exit 1
}

Write-Host "SUCCESS: Dispositivo conectado!" -ForegroundColor Green

# Caminho do APK
$apkPath = "app\build\outputs\apk\debug\app-debug.apk"

# Verificar se APK existe
if (-not (Test-Path $apkPath)) {
    Write-Host "ERRO: APK não encontrado em $apkPath" -ForegroundColor Red
    Write-Host "Execute '.\gradlew assembleDebug' primeiro." -ForegroundColor Red
    exit 1
}

Write-Host "APK encontrado: $apkPath" -ForegroundColor Green

# Desinstalar versão anterior (se existir)
Write-Host "Desinstalando versão anterior..." -ForegroundColor Yellow
& $ADB uninstall com.example.gestaobilhares 2>$null

# Instalar nova versão
Write-Host "Instalando nova versão..." -ForegroundColor Green
$result = & $ADB install -r $apkPath

if ($result -match "Success") {
    Write-Host "✅ APK instalado com sucesso!" -ForegroundColor Green
    Write-Host ""
    Write-Host "=== INSTRUÇÕES PARA TESTE ===" -ForegroundColor Yellow
    Write-Host "1. Abra o app GestaoBilhares" -ForegroundColor White
    Write-Host "2. Faça login" -ForegroundColor White
    Write-Host "3. Na tela 'Rotas', clique no ícone 🔧 (chave inglesa)" -ForegroundColor White
    Write-Host "4. Clique em '🚀 Testar Todas as Combinações'" -ForegroundColor White
    Write-Host "5. Observe qual método funciona com sua KP-1025" -ForegroundColor White
    Write-Host ""
    Write-Host "NOVOS MÉTODOS TESTADOS:" -ForegroundColor Cyan
    Write-Host "• Método Alternativo (linha por linha)" -ForegroundColor White
    Write-Host "• Dados Invertidos (formato alternativo)" -ForegroundColor White
    Write-Host "• Modo 24-dot (maior resolução)" -ForegroundColor White
    Write-Host "• Métodos originais (m=0, m=1, etc.)" -ForegroundColor White
} else {
    Write-Host "❌ ERRO na instalação: $result" -ForegroundColor Red
}

Write-Host ""
Write-Host "Pressione ENTER para sair..." -ForegroundColor Gray
Read-Host 