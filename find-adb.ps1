# Script para encontrar o caminho correto do ADB
Write-Host "Procurando ADB no sistema..." -ForegroundColor Cyan

# Tentar diferentes caminhos comuns
$adbPaths = @(
    "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe",
    "C:\Android\Sdk\platform-tools\adb.exe", 
    "C:\Program Files\Android\Android Studio\platform-tools\adb.exe",
    "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe",
    "C:\Users\Rossiny\Android\Sdk\platform-tools\adb.exe",
    "adb.exe"
)

Write-Host "`nTestando caminhos:" -ForegroundColor Yellow
foreach ($path in $adbPaths) {
    Write-Host "Testando: $path" -ForegroundColor White
    if (Test-Path $path) {
        Write-Host "  -> ENCONTRADO!" -ForegroundColor Green
        Write-Host "`nCaminho correto do ADB: $path" -ForegroundColor Green
        
        # Testar se o ADB funciona
        try {
            $version = & $path version
            Write-Host "`nVersao do ADB:" -ForegroundColor Cyan
            Write-Host $version -ForegroundColor White
        } catch {
            Write-Host "Erro ao executar ADB: $_" -ForegroundColor Red
        }
        break
    } else {
        Write-Host "  -> Nao encontrado" -ForegroundColor Red
    }
}

# Procurar em outras localizacoes
Write-Host "`nProcurando em outras localizacoes..." -ForegroundColor Yellow
$searchPaths = @(
    "C:\Users\Rossiny\AppData\Local\Android",
    "C:\Android",
    "C:\Program Files\Android"
)

foreach ($searchPath in $searchPaths) {
    if (Test-Path $searchPath) {
        Write-Host "Procurando em: $searchPath" -ForegroundColor White
        $foundAdb = Get-ChildItem -Path $searchPath -Recurse -Name "adb.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($foundAdb) {
            $fullPath = Join-Path $searchPath $foundAdb
            Write-Host "  -> ADB encontrado: $fullPath" -ForegroundColor Green
        }
    }
}

Write-Host "`nBusca concluida!" -ForegroundColor Cyan
