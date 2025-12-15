# Script para limpar build bloqueado
Write-Host "Parando Gradle daemon..." -ForegroundColor Yellow
.\gradlew --stop

Write-Host "Aguardando 3 segundos..." -ForegroundColor Yellow
Start-Sleep -Seconds 3

Write-Host "Finalizando processos Java..." -ForegroundColor Yellow
Get-Process | Where-Object {$_.ProcessName -like "*java*"} | Stop-Process -Force -ErrorAction SilentlyContinue

Write-Host "Aguardando 2 segundos..." -ForegroundColor Yellow
Start-Sleep -Seconds 2

Write-Host "Limpando build..." -ForegroundColor Yellow
.\gradlew clean --no-daemon

Write-Host "Build limpo com sucesso!" -ForegroundColor Green

