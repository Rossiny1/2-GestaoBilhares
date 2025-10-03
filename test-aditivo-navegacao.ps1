# Script para testar navegação do aditivo
Write-Host "=== TESTE NAVEGAÇÃO DO ADITIVO ===" -ForegroundColor Green

# Limpar logs anteriores
Write-Host "Limpando logs anteriores..." -ForegroundColor Yellow
adb logcat -c

Write-Host "Iniciando captura de logs específicos do aditivo..." -ForegroundColor Yellow
Write-Host "Execute o fluxo completo do aditivo:" -ForegroundColor Cyan
Write-Host "1. Abra um cliente com contrato ativo" -ForegroundColor White
Write-Host "2. Vá para Mesas -> Depositar mesa" -ForegroundColor White
Write-Host "3. Selecione uma mesa e confirme" -ForegroundColor White
Write-Host "4. Gere o aditivo" -ForegroundColor White
Write-Host "5. Assine o documento" -ForegroundColor White
Write-Host "6. Envie pelo WhatsApp" -ForegroundColor White
Write-Host "7. Volte ao app e observe onde vai" -ForegroundColor White
Write-Host ""
Write-Host "Pressione Ctrl+C para parar" -ForegroundColor Red

# Capturar logs específicos
adb logcat | findstr /i "AditivoSignatureFragment\|ClientDetailFragment\|Navigation\|navigate\|clienteId\|contrato"
