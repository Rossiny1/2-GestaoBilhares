# FASE 12.12: Script para analise de qualidade de codigo
# Uso: .\scripts\ci-analyze-code.ps1

Write-Host "ANALISE DE QUALIDADE DE CODIGO" -ForegroundColor Green
Write-Host "=================================" -ForegroundColor Cyan

# 1. Estatisticas de codigo
Write-Host ""
Write-Host "Estatisticas do Codigo:" -ForegroundColor Yellow

$kotlinFiles = Get-ChildItem -Path "app\src" -Filter "*.kt" -Recurse
$xmlFiles = Get-ChildItem -Path "app\src" -Filter "*.xml" -Recurse

Write-Host "Total de arquivos Kotlin: $($kotlinFiles.Count)" -ForegroundColor Cyan
Write-Host "Total de arquivos XML: $($xmlFiles.Count)" -ForegroundColor Cyan

$totalLines = 0
foreach ($file in $kotlinFiles) {
    $lines = (Get-Content $file.FullName | Measure-Object -Line).Lines
    $totalLines += $lines
}
Write-Host "Total de linhas de codigo Kotlin: $totalLines" -ForegroundColor Cyan

# 2. Verificar arquivos grandes (>500 linhas)
Write-Host ""
Write-Host "Arquivos grandes (>500 linhas):" -ForegroundColor Yellow
$largeFiles = $kotlinFiles | ForEach-Object {
    $lines = (Get-Content $_.FullName | Measure-Object -Line).Lines
    if ($lines -gt 500) {
        [PSCustomObject]@{
            File = $_.FullName.Replace((Get-Location).Path + "\", "")
            Lines = $lines
        }
    }
}

if ($largeFiles) {
    $largeFiles | Format-Table -AutoSize
    Write-Host "AVISO: Considere refatorar arquivos grandes (>500 linhas)" -ForegroundColor Yellow
} else {
    Write-Host "SUCCESS: Nenhum arquivo muito grande encontrado" -ForegroundColor Green
}

# 3. Verificar TODOs e FIXMEs
Write-Host ""
Write-Host "TODOs e FIXMEs encontrados:" -ForegroundColor Yellow
$todos = Select-String -Path "app\src\**\*.kt" -Pattern "TODO|FIXME" | Select-Object -First 20
if ($todos) {
    $todos | ForEach-Object {
        Write-Host "  - $($_.Filename):$($_.LineNumber) - $($_.Line.Trim())" -ForegroundColor Cyan
    }
} else {
    Write-Host "SUCCESS: Nenhum TODO/FIXME encontrado" -ForegroundColor Green
}

# 4. Verificar padroes comuns de problemas
Write-Host ""
Write-Host "Verificando padroes comuns de problemas:" -ForegroundColor Yellow

# Verificar uso de runBlocking (deve ser evitado)
$runBlocking = Select-String -Path "app\src\**\*.kt" -Pattern "runBlocking" | Measure-Object
$runBlockingColor = if ($runBlocking.Count -gt 5) { "Yellow" } else { "Green" }
Write-Host "  - Uso de runBlocking: $($runBlocking.Count) ocorrencias" -ForegroundColor $runBlockingColor

# Verificar uso de Log.d (deve usar AppLogger)
$logD = Select-String -Path "app\src\**\*.kt" -Pattern "Log\.d|android\.util\.Log" | Measure-Object
$logDColor = if ($logD.Count -gt 10) { "Yellow" } else { "Green" }
Write-Host "  - Uso de Log.d direto: $($logD.Count) ocorrencias" -ForegroundColor $logDColor

Write-Host ""
Write-Host "SUCCESS: Analise concluida!" -ForegroundColor Green
