Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CORRECAO: Problema de permissao KSP" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# [1/4] Parar processos Java/Gradle
Write-Host "`n[1/4] Parando processos Java/Gradle..." -ForegroundColor Yellow
Get-Process | Where-Object {$_.ProcessName -like "*java*" -or $_.ProcessName -like "*gradle*"} | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2
Write-Host "OK: Processos parados" -ForegroundColor Green

# [2/4] Remover diretorio de build do KSP
Write-Host "`n[2/4] Removendo diretorio KSP..." -ForegroundColor Yellow
$kspDir = "app\build\generated\ksp"
if (Test-Path $kspDir) {
    Remove-Item -Path $kspDir -Recurse -Force -ErrorAction SilentlyContinue
    Write-Host "OK: Diretorio KSP removido" -ForegroundColor Green
} else {
    Write-Host "INFO: Diretorio KSP nao existe" -ForegroundColor Yellow
}

# [3/4] Limpar build
Write-Host "`n[3/4] Limpando build..." -ForegroundColor Yellow
.\gradlew clean --no-daemon 2>&1 | Out-Null
Write-Host "OK: Build limpo" -ForegroundColor Green

# [4/4] Parar daemon do Gradle
Write-Host "`n[4/4] Parando daemon do Gradle..." -ForegroundColor Yellow
.\gradlew --stop 2>&1 | Out-Null
Write-Host "OK: Daemon parado" -ForegroundColor Green

Write-Host "`n========================================" -ForegroundColor Green
Write-Host "CORRECAO CONCLUIDA" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host "`nExecute: .\gradlew assembleDebug" -ForegroundColor Yellow

