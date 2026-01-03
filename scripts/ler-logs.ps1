# Script PowerShell simples e direto para ler logs do Android
# Uso: .\ler-logs.ps1

$adbPath = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verificar ADB
if (-not (Test-Path $adbPath)) {
    Write-Host "❌ ADB não encontrado!" -ForegroundColor Red
    exit 1
}

# Verificar dispositivo
$devices = & $adbPath devices
if ($devices.Count -lt 2) {
    Write-Host "❌ Nenhum dispositivo conectado!" -ForegroundColor Red
    exit 1
}

Write-Host "📱 Lendo logs do Android..." -ForegroundColor Green
Write-Host "Pressione Ctrl+C para parar" -ForegroundColor Yellow
Write-Host ""

# Limpar buffer
& $adbPath logcat -c | Out-Null

# Ler logs com filtro para o app
& $adbPath logcat -v time *:D | 
    Select-String -Pattern "AppRepository|AuthViewModel|LoginFragment|CONVERSÃO|toObject|getColaboradorByUid|não foi possível converter" |
    ForEach-Object {
        $linha = $_.Line
        if ($linha -match "❌|ERROR|Erro|Falha|null|Exception") {
            Write-Host $linha -ForegroundColor Red
        } elseif ($linha -match "✅|SUCCESS|Sucesso|convertido") {
            Write-Host $linha -ForegroundColor Green
        } elseif ($linha -match "🔍|🔧|📋|DIAGNÓSTICO|FIRESTORE") {
            Write-Host $linha -ForegroundColor Cyan
        } elseif ($linha -match "⚠️|WARNING|Aviso") {
            Write-Host $linha -ForegroundColor Yellow
        } else {
            Write-Host $linha
        }
    }
