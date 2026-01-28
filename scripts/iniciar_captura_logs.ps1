# 📱 Script para Iniciar Captura de Logs do App Android
# Uso: .\scripts\iniciar_captura_logs.ps1

Write-Host "🔧 Preparando captura de logs do app Android..." -ForegroundColor Green

# Caminho do ADB
$adbPath = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verificar se ADB existe
if (-not (Test-Path $adbPath)) {
    Write-Host "❌ ADB não encontrado em: $adbPath" -ForegroundColor Red
    Write-Host "📋 Instale o Android Studio ou verifique o caminho do SDK" -ForegroundColor Yellow
    exit 1
}

# Limpar logs anteriores
Write-Host "🧹 Limpando logs anteriores..." -ForegroundColor Blue
& $adbPath logcat -c

# Iniciar captura de logs
Write-Host "📡 Iniciando captura de logs..." -ForegroundColor Blue
Write-Host "📱 Execute as operações no app Android agora" -ForegroundColor Yellow
Write-Host "⚠️ Mantenha este terminal aberto durante os testes" -ForegroundColor Yellow
Write-Host "🛑 Pressione Ctrl+C para parar a captura" -ForegroundColor Yellow
Write-Host ""

# Capturar logs filtrados
& $adbPath logcat -s FirebaseFirestore:D FirebaseAuth:D GestaoBilhares:D *:E > logs_app_real.txt

Write-Host "✅ Captura de logs finalizada" -ForegroundColor Green
Write-Host "📊 Logs salvos em: logs_app_real.txt" -ForegroundColor Blue

# Extrair erros PERMISSION_DENIED
Write-Host "🔍 Extraindo erros PERMISSION_DENIED..." -ForegroundColor Blue
Get-Content logs_app_real.txt | Select-String -Pattern "PERMISSION_DENIED|Missing|insufficient|Error" -Context 5 > erros_permission_denied.txt

Write-Host "📋 Erros salvos em: erros_permission_denied.txt" -ForegroundColor Green
Write-Host ""
Write-Host "🎯 Próximo passo: Analisar os logs e corrigir Security Rules" -ForegroundColor Cyan
