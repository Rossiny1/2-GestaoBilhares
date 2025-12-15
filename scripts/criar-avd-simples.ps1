# ========================================
# CRIAR AVD SIMPLES - GESTAO BILHARES
# ========================================
# Script para configurar visualização do APK

Write-Host "CONFIGURACAO DE VISUALIZACAO APK - GESTAO BILHARES" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Verificar se há AVDs
Write-Host "Verificando AVDs disponiveis..." -ForegroundColor Yellow
$avds = & "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\emulator\emulator.exe" -list-avds

if ($avds.Count -eq 0) {
    Write-Host "ERROR: Nenhum AVD encontrado!" -ForegroundColor Red
    Write-Host "`nOPCOES DISPONIVEIS:" -ForegroundColor Yellow
    Write-Host "1. Criar AVD via Android Studio (RECOMENDADO)" -ForegroundColor Green
    Write-Host "2. Usar dispositivo fisico" -ForegroundColor Green
    Write-Host "3. Instalar cmdline-tools" -ForegroundColor Green
    
    $choice = Read-Host "`nEscolha uma opcao (1-3)"
    
    switch ($choice) {
        "1" {
            Write-Host "`nCRIAR AVD VIA ANDROID STUDIO:" -ForegroundColor Cyan
            Write-Host "1. Abra o Android Studio" -ForegroundColor White
            Write-Host "2. Va em Tools > AVD Manager" -ForegroundColor White
            Write-Host "3. Clique em 'Create Virtual Device'" -ForegroundColor White
            Write-Host "4. Escolha um dispositivo (ex: Pixel 4)" -ForegroundColor White
            Write-Host "5. Escolha uma imagem do sistema (ex: API 34)" -ForegroundColor White
            Write-Host "6. Clique em 'Finish'" -ForegroundColor White
            Write-Host "`nDepois de criar o AVD, execute novamente este script!" -ForegroundColor Green
        }
        "2" {
            Write-Host "`nUSAR DISPOSITIVO FISICO:" -ForegroundColor Cyan
            Write-Host "1. Conecte seu dispositivo Android via USB" -ForegroundColor White
            Write-Host "2. Habilite 'Depuracao USB' nas opcoes de desenvolvedor" -ForegroundColor White
            Write-Host "3. Aceite a autorizacao no dispositivo" -ForegroundColor White
            Write-Host "4. Execute: .\visualizar-apk-cursor.ps1 -Device" -ForegroundColor Green
        }
        "3" {
            Write-Host "`nINSTALAR CMDLINE-TOOLS:" -ForegroundColor Cyan
            Write-Host "1. Abra o Android Studio" -ForegroundColor White
            Write-Host "2. Va em Tools > SDK Manager" -ForegroundColor White
            Write-Host "3. Na aba 'SDK Tools', marque 'Android SDK Command-line Tools'" -ForegroundColor White
            Write-Host "4. Clique em 'Apply' e aguarde a instalacao" -ForegroundColor White
            Write-Host "5. Execute novamente este script!" -ForegroundColor Green
        }
        default {
            Write-Host "Opcao invalida!" -ForegroundColor Red
        }
    }
} else {
    Write-Host "SUCCESS: AVDs encontrados:" -ForegroundColor Green
    $avds | ForEach-Object { Write-Host "  - $_" -ForegroundColor Cyan }
    
    Write-Host "`nPROXIMOS PASSOS:" -ForegroundColor Yellow
    Write-Host "1. Execute: .\visualizar-apk-cursor.ps1 -Emulator -Build" -ForegroundColor Green
    Write-Host "2. Ou use modo interativo: .\visualizar-apk-cursor.ps1" -ForegroundColor Green
}

Write-Host "`nSUCCESS: Configuracao concluida!" -ForegroundColor Green 