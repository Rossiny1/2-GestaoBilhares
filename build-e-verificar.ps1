# Script para build e verificar erros
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "BUILD E VERIFICACAO" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Limpar build anterior
Write-Host "Limpando build anterior..." -ForegroundColor Yellow
./gradlew clean --no-daemon 2>&1 | Out-Null

# Executar build
Write-Host "Executando build..." -ForegroundColor Yellow
$buildOutput = ./gradlew assembleDebug --no-daemon 2>&1

# Verificar resultado
if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "=====================================" -ForegroundColor Green
    Write-Host "BUILD PASSOU COM SUCESSO!" -ForegroundColor Green
    Write-Host "=====================================" -ForegroundColor Green
    Write-Host ""
    
    # Verificar APK
    if (Test-Path "app/build/outputs/apk/debug/app-debug.apk") {
        $apk = Get-Item "app/build/outputs/apk/debug/app-debug.apk"
        Write-Host "APK gerado:" -ForegroundColor Cyan
        Write-Host "  Caminho: $($apk.FullName)" -ForegroundColor White
        Write-Host "  Tamanho: $([math]::Round($apk.Length / 1MB, 2)) MB" -ForegroundColor White
        Write-Host "  Data: $($apk.LastWriteTime)" -ForegroundColor White
        Write-Host ""
        Write-Host "APP PRONTO PARA TESTES MANUAIS!" -ForegroundColor Green
    }
} else {
    Write-Host ""
    Write-Host "=====================================" -ForegroundColor Red
    Write-Host "BUILD FALHOU!" -ForegroundColor Red
    Write-Host "=====================================" -ForegroundColor Red
    Write-Host ""
    Write-Host "Erros encontrados:" -ForegroundColor Yellow
    $buildOutput | Select-String -Pattern "(error|ERROR|FAILED)" | Select-Object -First 30
    Write-Host ""
    Write-Host "Para ver o log completo, verifique o output acima." -ForegroundColor Yellow
    exit 1
}

