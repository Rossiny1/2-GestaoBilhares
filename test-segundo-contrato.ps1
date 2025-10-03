# Script para testar navegação do segundo contrato
Write-Host "=== TESTE NAVEGAÇÃO DO SEGUNDO CONTRATO ===" -ForegroundColor Green

# Limpar logs anteriores
Write-Host "Limpando logs anteriores..." -ForegroundColor Yellow
adb logcat -c

Write-Host "Iniciando captura de logs específicos do segundo contrato..." -ForegroundColor Yellow
Write-Host "Execute o fluxo completo do segundo contrato:" -ForegroundColor Cyan
Write-Host "1. Abra um cliente com contrato ativo" -ForegroundColor White
Write-Host "2. Gere um distrato" -ForegroundColor White
Write-Host "3. Envie o distrato pelo WhatsApp" -ForegroundColor White
Write-Host "4. Volte ao app (deve ir para detalhes do cliente)" -ForegroundColor White
Write-Host "5. Gere um NOVO contrato" -ForegroundColor White
Write-Host "6. Assine o contrato" -ForegroundColor White
Write-Host "7. Envie pelo WhatsApp" -ForegroundColor White
Write-Host "8. Volte ao app e observe onde vai" -ForegroundColor White
Write-Host ""
Write-Host "Pressione Ctrl+C para parar" -ForegroundColor Red

# Capturar logs específicos
adb logcat | findstr /i "SignatureCaptureFragment\|ClientDetailFragment\|Navigation\|navigate\|clienteId\|contrato\|contexto"
