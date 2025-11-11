# Script para resolver problemas de lock de arquivo
Write-Host "Parando Gradle daemon..." -ForegroundColor Yellow
.\gradlew --stop

Write-Host "Aguardando 2 segundos..." -ForegroundColor Yellow
Start-Sleep -Seconds 2

Write-Host "Parando processos Java que podem estar bloqueando arquivos..." -ForegroundColor Yellow
Get-Process | Where-Object {$_.ProcessName -like "*java*" -or $_.ProcessName -like "*gradle*"} | Stop-Process -Force -ErrorAction SilentlyContinue

Write-Host "Aguardando 2 segundos..." -ForegroundColor Yellow
Start-Sleep -Seconds 2

Write-Host "Removendo diretorio build do modulo core..." -ForegroundColor Yellow
$coreBuildDir = "core\build"
if (Test-Path $coreBuildDir) {
    try {
        Remove-Item -Path $coreBuildDir -Recurse -Force -ErrorAction Stop
        Write-Host "Diretorio build removido com sucesso" -ForegroundColor Green
    } catch {
        Write-Host "Erro ao remover diretorio: $_" -ForegroundColor Red
        Write-Host "Tente fechar o Android Studio/IDE e executar novamente" -ForegroundColor Yellow
    }
} else {
    Write-Host "Diretorio build nao existe" -ForegroundColor Green
}

Write-Host "Limpando build..." -ForegroundColor Yellow
.\gradlew clean --no-daemon

Write-Host "Pronto! Agora execute: .\gradlew assembleDebug" -ForegroundColor Green

