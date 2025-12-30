# Script para gerar APK e preparar para download
# Uso: .\scripts\gerar-e-baixar-apk.ps1

$ErrorActionPreference = "Continue"

Write-Host "📱 Gerando APK para instalação..." -ForegroundColor Cyan
Write-Host ""

# Verificar se gradlew.bat existe
if (-not (Test-Path "gradlew.bat")) {
    Write-Host "❌ gradlew.bat não encontrado!" -ForegroundColor Red
    Write-Host "💡 Execute este script na raiz do projeto." -ForegroundColor Yellow
    exit 1
}

# Gerar APK
Write-Host "🔨 Gerando APK..." -ForegroundColor Yellow
.\gradlew.bat assembleDebug --console=plain 2>&1 | ForEach-Object { Write-Host $_ }

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Erro ao gerar APK!" -ForegroundColor Red
    Write-Host "💡 Verifique os erros acima." -ForegroundColor Yellow
    exit 1
}

# Verificar se APK foi gerado
$apkPath = "app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apkPath) {
    $apkSize = (Get-Item $apkPath).Length / 1MB
    Write-Host ""
    Write-Host "✅ APK gerado com sucesso!" -ForegroundColor Green
    Write-Host "   Local: $apkPath" -ForegroundColor Gray
    Write-Host "   Tamanho: $([math]::Round($apkSize, 2))MB" -ForegroundColor Gray
    Write-Host ""
    
    # Verificar se dispositivo está conectado
    Write-Host "📱 Verificando dispositivos conectados..." -ForegroundColor Yellow
    $devices = adb devices 2>&1 | Select-String -Pattern "device$"
    
    if ($devices) {
        Write-Host "✅ Dispositivo encontrado!" -ForegroundColor Green
        Write-Host ""
        Write-Host "🚀 Instalando APK..." -ForegroundColor Yellow
        
        adb install -r $apkPath 2>&1 | ForEach-Object { Write-Host $_ }
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host ""
            Write-Host "✅ APK instalado com sucesso no dispositivo!" -ForegroundColor Green
        } else {
            Write-Host ""
            Write-Host "⚠️  Erro ao instalar. Tente manualmente:" -ForegroundColor Yellow
            Write-Host "   adb install -r $apkPath" -ForegroundColor Gray
        }
    } else {
        Write-Host "⚠️  Nenhum dispositivo conectado." -ForegroundColor Yellow
        Write-Host ""
        Write-Host "💡 Opções:" -ForegroundColor Cyan
        Write-Host "   1. Conecte o celular via USB e execute:" -ForegroundColor Gray
        Write-Host "      adb install -r $apkPath" -ForegroundColor Gray
        Write-Host ""
        Write-Host "   2. Ou transfira o APK manualmente:" -ForegroundColor Gray
        Write-Host "      Copie: $apkPath" -ForegroundColor Gray
        Write-Host "      Para o celular e instale manualmente" -ForegroundColor Gray
    }
} else {
    Write-Host "❌ APK não foi gerado!" -ForegroundColor Red
    Write-Host "💡 Verifique os erros do build acima." -ForegroundColor Yellow
    exit 1
}

Write-Host ""
