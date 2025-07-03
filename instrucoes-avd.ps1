# ========================================
# INSTRUCOES PARA CRIAR AVD - GESTAO BILHARES
# ========================================

Write-Host "CRIAR AVD VIA ANDROID STUDIO - GESTAO BILHARES" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

Write-Host "`nPASSO A PASSO PARA CRIAR AVD:" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow

Write-Host "1. Abra o Android Studio" -ForegroundColor White
Write-Host "2. Va em Tools > AVD Manager" -ForegroundColor White
Write-Host "3. Clique em 'Create Virtual Device'" -ForegroundColor White
Write-Host "4. Escolha um dispositivo (ex: Pixel 4)" -ForegroundColor White
Write-Host "5. Escolha uma imagem do sistema (ex: API 34)" -ForegroundColor White
Write-Host "6. Clique em 'Finish'" -ForegroundColor White

Write-Host "`nDEPOIS DE CRIAR O AVD:" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host "1. Execute: .\visualizar-apk-cursor.ps1 -Emulator -Build" -ForegroundColor White
Write-Host "2. Ou use modo interativo: .\visualizar-apk-cursor.ps1" -ForegroundColor White

Write-Host "`nALTERNATIVA RAPIDA - DISPOSITIVO FISICO:" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow
Write-Host "Se voce tem um dispositivo Android:" -ForegroundColor White
Write-Host "1. Conecte via USB" -ForegroundColor White
Write-Host "2. Habilite Depuracao USB" -ForegroundColor White
Write-Host "3. Execute: .\visualizar-apk-cursor.ps1 -Device -Build" -ForegroundColor Green

Write-Host "`nSUCCESS: Instrucoes exibidas!" -ForegroundColor Green 