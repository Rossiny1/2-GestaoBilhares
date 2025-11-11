# Script para resolver problemas de permissão do KSP
Write-Host "Parando Gradle daemon..." -ForegroundColor Yellow
.\gradlew --stop

Write-Host "Aguardando 2 segundos..." -ForegroundColor Yellow
Start-Sleep -Seconds 2

Write-Host "Removendo diretorio KSP gerado..." -ForegroundColor Yellow
$kspDir = "data\build\generated\ksp"
if (Test-Path $kspDir) {
    try {
        Remove-Item -Path $kspDir -Recurse -Force -ErrorAction Stop
        Write-Host "Diretorio KSP removido com sucesso" -ForegroundColor Green
    } catch {
        Write-Host "Erro ao remover diretorio KSP: $_" -ForegroundColor Red
        Write-Host "Tente fechar o Android Studio/IDE e executar novamente" -ForegroundColor Yellow
    }
} else {
    Write-Host "Diretorio KSP nao existe" -ForegroundColor Green
}

Write-Host "Limpando build..." -ForegroundColor Yellow
.\gradlew clean --no-daemon

Write-Host "Pronto! Agora execute: .\gradlew assembleDebug" -ForegroundColor Green

