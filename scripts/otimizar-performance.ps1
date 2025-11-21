# Script para otimizar performance do Gradle e terminal
Write-Host "=== OTIMIZANDO PERFORMANCE DO GRADLE ===" -ForegroundColor Cyan

# 1. Parar todos os daemons Gradle
Write-Host "`n1. Parando daemons Gradle..." -ForegroundColor Yellow
.\gradlew --stop 2>&1 | Out-Null
Start-Sleep -Seconds 2

# 2. Matar processos Java/Gradle órfãos
Write-Host "2. Verificando processos Java/Gradle..." -ForegroundColor Yellow
$processes = Get-Process | Where-Object {$_.ProcessName -like "*java*" -or $_.ProcessName -like "*gradle*"} -ErrorAction SilentlyContinue
if ($processes) {
    Write-Host "   Encontrados $($processes.Count) processos. Matando..." -ForegroundColor Yellow
    $processes | Stop-Process -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
} else {
    Write-Host "   Nenhum processo encontrado." -ForegroundColor Green
}

# 3. Limpar cache do Gradle (opcional - descomente se necessário)
# Write-Host "`n3. Limpando cache do Gradle..." -ForegroundColor Yellow
# Remove-Item -Path "$env:USERPROFILE\.gradle\caches" -Recurse -Force -ErrorAction SilentlyContinue
# Write-Host "   Cache limpo." -ForegroundColor Green

# 4. Verificar configuração de memória
Write-Host "`n4. Verificando configuração de memória..." -ForegroundColor Yellow
$memoria = (Get-CimInstance Win32_ComputerSystem).TotalPhysicalMemory / 1073741824
Write-Host "   RAM total: $([math]::Round($memoria, 2)) GB" -ForegroundColor Cyan
if ($memoria -lt 8) {
    Write-Host "   AVISO: RAM limitada. Considere reduzir -Xmx no gradle.properties" -ForegroundColor Red
}

# 5. Verificar exclusões do Windows Defender
Write-Host "`n5. Verificando exclusões do Windows Defender..." -ForegroundColor Yellow
$projectPath = (Get-Location).Path
$defenderExclusions = Get-MpPreference | Select-Object -ExpandProperty ExclusionPath -ErrorAction SilentlyContinue
if ($defenderExclusions -notcontains $projectPath) {
    Write-Host "   AVISO: Pasta do projeto não está nas exclusões do Windows Defender" -ForegroundColor Yellow
    Write-Host "   Para melhorar performance, adicione esta pasta nas exclusões:" -ForegroundColor Yellow
    Write-Host "   $projectPath" -ForegroundColor Cyan
    Write-Host "`n   Comando para adicionar (execute como Admin):" -ForegroundColor Yellow
    Write-Host "   Add-MpPreference -ExclusionPath '$projectPath'" -ForegroundColor White
} else {
    Write-Host "   Pasta já está nas exclusões." -ForegroundColor Green
}

# 6. Verificar espaço em disco
Write-Host "`n6. Verificando espaço em disco..." -ForegroundColor Yellow
$drive = (Get-Location).Drive.Name
$disk = Get-CimInstance Win32_LogicalDisk -Filter "DeviceID='$drive`:'"
$freeSpaceGB = [math]::Round($disk.FreeSpace / 1073741824, 2)
$totalSpaceGB = [math]::Round($disk.Size / 1073741824, 2)
Write-Host "   Espaço livre: $freeSpaceGB GB de $totalSpaceGB GB" -ForegroundColor Cyan
if ($freeSpaceGB -lt 5) {
    Write-Host "   AVISO: Pouco espaço em disco pode afetar performance" -ForegroundColor Red
}

# 7. Verificar tamanho do cache do Gradle
Write-Host "`n7. Verificando cache do Gradle..." -ForegroundColor Yellow
$gradleCache = "$env:USERPROFILE\.gradle\caches"
if (Test-Path $gradleCache) {
    $cacheSize = (Get-ChildItem -Path $gradleCache -Recurse -ErrorAction SilentlyContinue | Measure-Object -Property Length -Sum).Sum / 1073741824
    Write-Host "   Tamanho do cache: $([math]::Round($cacheSize, 2)) GB" -ForegroundColor Cyan
    if ($cacheSize -gt 10) {
        Write-Host "   AVISO: Cache muito grande. Considere limpar com: .\gradlew cleanBuildCache" -ForegroundColor Yellow
    }
}

Write-Host "`n=== OTIMIZAÇÃO CONCLUÍDA ===" -ForegroundColor Green
Write-Host "`nDicas para melhorar performance:" -ForegroundColor Cyan
Write-Host "1. Adicione a pasta do projeto nas exclusões do Windows Defender (como Admin)" -ForegroundColor White
Write-Host "2. Use --no-daemon apenas quando necessário (daemon é mais rápido)" -ForegroundColor White
Write-Host "3. Evite executar múltiplos builds simultaneamente" -ForegroundColor White
Write-Host "4. Considere usar WSL2 se disponível (mais rápido que PowerShell)" -ForegroundColor White

