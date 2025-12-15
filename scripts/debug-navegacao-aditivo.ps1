# Script para debug da navegação do aditivo
Write-Host "=== DEBUG NAVEGAÇÃO DO ADITIVO ===" -ForegroundColor Green

# Limpar logs anteriores
Write-Host "Limpando logs anteriores..." -ForegroundColor Yellow
adb logcat -c

Write-Host "Iniciando captura de logs específicos..." -ForegroundColor Yellow
Write-Host "Execute o fluxo completo do aditivo e observe os logs abaixo:" -ForegroundColor Cyan
Write-Host "1. Abra um cliente" -ForegroundColor White
Write-Host "2. Vá para aditivo" -ForegroundColor White
Write-Host "3. Assine o documento" -ForegroundColor White
Write-Host "4. Envie pelo WhatsApp" -ForegroundColor White
Write-Host "5. Volte ao app" -ForegroundColor White
Write-Host ""
Write-Host "Pressione Ctrl+C para parar" -ForegroundColor Red

# Capturar logs específicos
adb logcat | findstr /i "AditivoSignatureFragment\|ClientDetailFragment\|Navigation\|navigate\|clienteId"
