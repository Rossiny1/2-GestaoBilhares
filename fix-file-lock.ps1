# Script para resolver problemas de lock de arquivo
Write-Host "Parando Gradle daemon..." -ForegroundColor Yellow
.\gradlew --stop

Write-Host "Aguardando 2 segundos..." -ForegroundColor Yellow
Start-Sleep -Seconds 2

Write-Host "Parando processos Java que podem estar bloqueando arquivos..." -ForegroundColor Yellow
Get-Process | Where-Object {$_.ProcessName -like "*java*" -or $_.ProcessName -like "*gradle*"} | Stop-Process -Force -ErrorAction SilentlyContinue

Write-Host "Aguardando 2 segundos..." -ForegroundColor Yellow
Start-Sleep -Seconds 2

Write-Host "Removendo diretorios build dos modulos..." -ForegroundColor Yellow
$buildDirs = @("core\build", "data\build", "app\build", "sync\build", "ui\build")
foreach ($buildDir in $buildDirs) {
    if (Test-Path $buildDir) {
        try {
            Remove-Item -Path $buildDir -Recurse -Force -ErrorAction Stop
            Write-Host "Removido: $buildDir" -ForegroundColor Green
        } catch {
            Write-Host "Erro ao remover $buildDir : $_" -ForegroundColor Red
            Write-Host "Tente fechar o Android Studio/IDE e executar novamente" -ForegroundColor Yellow
        }
    } else {
        Write-Host "Nao existe: $buildDir" -ForegroundColor DarkGray
    }
}

Write-Host "Limpando build..." -ForegroundColor Yellow
.\gradlew clean --no-daemon

Write-Host "Pronto! Agora execute: .\gradlew assembleDebug" -ForegroundColor Green

