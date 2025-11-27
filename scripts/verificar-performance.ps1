# Script rápido para verificar performance
Write-Host "=== DIAGNÓSTICO DE PERFORMANCE ===" -ForegroundColor Cyan

# Processos Java/Gradle
Write-Host "`nProcessos Java/Gradle:" -ForegroundColor Yellow
$procs = Get-Process | Where-Object {$_.ProcessName -like "*java*" -or $_.ProcessName -like "*gradle*"} -ErrorAction SilentlyContinue
if ($procs) {
    $procs | Format-Table ProcessName, Id, CPU, @{Name="Mem(MB)";Expression={$memMB = $_.WorkingSet64 / 1048576; [math]::Round($memMB, 2)}} -AutoSize
} else {
    Write-Host "Nenhum processo encontrado" -ForegroundColor Green
}

# Memória disponível
Write-Host "`nMemória do Sistema:" -ForegroundColor Yellow
$os = Get-CimInstance Win32_OperatingSystem
$totalRAM = [math]::Round($os.TotalVisibleMemorySize / 1024, 2)
$freeRAM = [math]::Round($os.FreePhysicalMemory / 1024, 2)
$usedRAM = $totalRAM - $freeRAM
Write-Host "Total: $totalRAM MB | Usado: $usedRAM MB | Livre: $freeRAM MB" -ForegroundColor Cyan

# Espaço em disco
Write-Host "`nEspaço em Disco:" -ForegroundColor Yellow
$drive = (Get-Location).Drive.Name
$disk = Get-CimInstance Win32_LogicalDisk -Filter "DeviceID='$drive`:'"
$freeGB = [math]::Round($disk.FreeSpace / 1073741824, 2)
Write-Host "Unidade $drive`: $freeGB GB livres" -ForegroundColor Cyan

# Cache Gradle
Write-Host "`nCache Gradle:" -ForegroundColor Yellow
$gradleCache = "$env:USERPROFILE\.gradle"
if (Test-Path $gradleCache) {
    $cacheSize = (Get-ChildItem -Path $gradleCache -Recurse -File -ErrorAction SilentlyContinue | Measure-Object -Property Length -Sum).Sum / 1073741824
    Write-Host "Tamanho: $([math]::Round($cacheSize, 2)) GB" -ForegroundColor Cyan
} else {
    Write-Host "Cache não encontrado" -ForegroundColor Yellow
}

Write-Host "`n=== FIM DO DIAGNÓSTICO ===" -ForegroundColor Green

