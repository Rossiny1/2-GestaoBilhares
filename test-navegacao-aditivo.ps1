# Script para testar navegação do aditivo
Write-Host "=== TESTE DE NAVEGAÇÃO DO ADITIVO ===" -ForegroundColor Green

# Limpar logs anteriores
Write-Host "Limpando logs anteriores..." -ForegroundColor Yellow
adb logcat -c

Write-Host "Iniciando captura de logs do aditivo..." -ForegroundColor Yellow
Write-Host "Execute o fluxo: Cliente -> Aditivo -> Assinar -> Enviar WhatsApp -> Voltar ao app" -ForegroundColor Cyan
Write-Host "Pressione Ctrl+C para parar a captura" -ForegroundColor Red

# Capturar logs específicos do aditivo
adb logcat | findstr /i "AditivoSignatureFragment\|ClientDetailFragment\|Navigation\|navigate"
