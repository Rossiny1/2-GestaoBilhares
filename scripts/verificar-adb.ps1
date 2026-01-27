# Script rápido para verificar ADB e dispositivos
# Versão: 1.0

Write-Host "=== VERIFICAÇÃO ADB ===" -ForegroundColor Yellow

# Caminho do ADB
$ADB = "C:\Users\$($env:USERNAME)\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verificar se ADB existe
Write-Host "Verificando ADB em: $ADB" -ForegroundColor Cyan
if (Test-Path $ADB) {
    Write-Host "✅ ADB encontrado" -ForegroundColor Green
    
    # Verificar versão
    try {
        $version = & $ADB version
        Write-Host "Versão: $version" -ForegroundColor White
    } catch {
        Write-Host "❌ Erro ao verificar versão do ADB" -ForegroundColor Red
    }
    
    # Verificar dispositivos
    Write-Host ""
    Write-Host "Verificando dispositivos..." -ForegroundColor Cyan
    try {
        $devices = & $ADB devices
        Write-Host $devices -ForegroundColor White
        
        if ($devices -match "device$") {
            Write-Host "✅ Dispositivo(s) conectado(s)" -ForegroundColor Green
        } else {
            Write-Host "❌ Nenhum dispositivo conectado" -ForegroundColor Red
            Write-Host "Conecte um dispositivo USB ou inicie um emulador" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "❌ Erro ao verificar dispositivos" -ForegroundColor Red
    }
} else {
    Write-Host "❌ ADB não encontrado" -ForegroundColor Red
    Write-Host "Instale o Android SDK ou verifique o caminho" -ForegroundColor Yellow
    
    # Tentar encontrar ADB em outros locais
    Write-Host ""
    Write-Host "Procurando ADB em outros locais..." -ForegroundColor Cyan
    $paths = @(
        "C:\Program Files\Android\Android Studio\plugins\android\lib\monitor-tools\adb.exe",
        "C:\Program Files (x86)\Android\android-sdk\platform-tools\adb.exe",
        "C:\Android\Sdk\platform-tools\adb.exe"
    )
    
    foreach ($path in $paths) {
        if (Test-Path $path) {
            Write-Host "✅ ADB encontrado em: $path" -ForegroundColor Green
            Write-Host "Atualize o script para usar este caminho" -ForegroundColor Yellow
        }
    }
}

Write-Host ""
Write-Host "Para usar o script de debug:" -ForegroundColor Cyan
Write-Host "1. Conecte um dispositivo ou inicie o emulador" -ForegroundColor White
Write-Host "2. Execute: .\scripts\debug-panos-estoque.ps1" -ForegroundColor White
Write-Host "3. Siga os passos para criar panos" -ForegroundColor White
