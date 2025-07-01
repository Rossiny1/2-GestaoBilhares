# ========================================
# CAPTURADOR DE LOGS - HISTÓRICO DE ACERTOS
# ========================================

Write-Host "Capturando logs do histórico de acertos..." -ForegroundColor Cyan

# Caminho do ADB
$ADB = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Limpar logs antigos
Write-Host "Limpando logs antigos..." -ForegroundColor Yellow
& $ADB logcat -c

# Capturar logs específicos
Write-Host "Iniciando captura de logs..." -ForegroundColor Green
Write-Host "Agora teste o app: faça alguns acertos e verifique o histórico" -ForegroundColor Yellow
Write-Host "Pressione Ctrl+C para parar a captura" -ForegroundColor Red

# Capturar logs com filtros específicos
& $ADB logcat -s ClientDetailFragment SettlementHistoryAdapter ClientDetailViewModel SettlementViewModel 