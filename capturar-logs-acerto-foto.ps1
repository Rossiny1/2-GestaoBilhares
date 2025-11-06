# Script para capturar logs durante criação de acerto com foto
# Filtra apenas logs relacionados a foto e sincronização

$packageName = "com.example.gestaobilhares"
$logFile = "logs-acerto-foto-$(Get-Date -Format 'yyyyMMdd-HHmmss').txt"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CAPTURA DE LOGS - ACERTO COM FOTO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Pacote: $packageName" -ForegroundColor Yellow
Write-Host "Arquivo de log: $logFile" -ForegroundColor Yellow
Write-Host ""
Write-Host "INSTRUCOES:" -ForegroundColor Green
Write-Host "1. Este script vai capturar logs em tempo real" -ForegroundColor White
Write-Host "2. Crie um acerto com foto no app" -ForegroundColor White
Write-Host "3. Apos criar, aguarde 10 segundos e pressione Ctrl+C" -ForegroundColor White
Write-Host ""
Write-Host "Iniciando captura em 3 segundos..." -ForegroundColor Yellow
Start-Sleep -Seconds 3

# Limpar logs antigos
adb logcat -c

# Filtros para capturar apenas logs relevantes
$filters = @(
    "AppRepository:*",
    "SyncManagerV2:*",
    "FirebaseStorageManager:*",
    "SettlementViewModel:*",
    "SettlementFragment:*"
)

Write-Host ""
Write-Host "Capturando logs..." -ForegroundColor Green
Write-Host "Pressione Ctrl+C para parar apos criar o acerto" -ForegroundColor Yellow
Write-Host ""

# Capturar logs e filtrar
adb logcat -v time $filters | Tee-Object -FilePath $logFile | Select-String -Pattern "foto|Foto|FOTO|upload|Upload|UPLOAD|cache|Cache|CACHE|inserirAcertoMesaSync|adicionarAcertoComMesasParaSync|payload|Payload|FirebaseStorage" -Context 0,2

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CAPTURA CONCLUIDA" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Logs salvos em: $logFile" -ForegroundColor Green
Write-Host ""
Write-Host "PROCURAR POR:" -ForegroundColor Yellow
Write-Host "- 'inserirAcertoMesaSync: Resultado upload'" -ForegroundColor White
Write-Host "- 'Cache de URLs do Firebase'" -ForegroundColor White
Write-Host "- 'Mesa X no payload: fotoUrl='" -ForegroundColor White
Write-Host "- 'Upload bem-sucedido'" -ForegroundColor White
Write-Host "- 'ERRO CRITICO'" -ForegroundColor Red

