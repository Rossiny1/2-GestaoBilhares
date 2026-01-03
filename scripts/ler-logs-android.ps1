# Script PowerShell para ler logs do Android via ADB
# Filtra logs relevantes do app Gestão Bilhares

param(
    [string]$Filtro = "AppRepository|AuthViewModel|LoginFragment",
    [string]$Nivel = "D",  # D=Debug, E=Error, W=Warning, I=Info
    [switch]$SalvarArquivo = $false,
    [string]$ArquivoSaida = "logs-android-$(Get-Date -Format 'yyyyMMdd-HHmmss').txt",
    [switch]$TempoReal = $true
)

# Caminho do ADB
$adbPath = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verificar se o ADB existe
if (-not (Test-Path $adbPath)) {
    Write-Host "❌ ADB não encontrado em: $adbPath" -ForegroundColor Red
    Write-Host "Verifique se o caminho está correto ou instale o Android SDK Platform Tools" -ForegroundColor Yellow
    exit 1
}

# Verificar se há dispositivo conectado
$devices = & $adbPath devices
if ($devices.Count -lt 2) {
    Write-Host "❌ Nenhum dispositivo Android conectado!" -ForegroundColor Red
    Write-Host "Conecte um dispositivo via USB e habilite a depuração USB" -ForegroundColor Yellow
    exit 1
}

Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "📱 LEITURA DE LOGS ANDROID - Gestão Bilhares" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "ADB: $adbPath" -ForegroundColor Gray
Write-Host "Filtro: $Filtro" -ForegroundColor Gray
Write-Host "Nível: $Nivel" -ForegroundColor Gray
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

# Limpar logs antigos (opcional)
Write-Host "🧹 Limpando buffer de logs..." -ForegroundColor Yellow
& $adbPath logcat -c | Out-Null

# Construir comando logcat
$comandoLogcat = "logcat"
$comandoLogcat += " -v time"  # Formato com timestamp
$comandoLogcat += " *:$Nivel"  # Nível mínimo de log

# Se salvar em arquivo
if ($SalvarArquivo) {
    Write-Host "💾 Salvando logs em: $ArquivoSaida" -ForegroundColor Green
    Write-Host "Pressione Ctrl+C para parar e salvar o arquivo" -ForegroundColor Yellow
    Write-Host ""
    
    # Executar e salvar em arquivo
    & $adbPath logcat -v time *:$Nivel | 
        Select-String -Pattern $Filtro | 
        Tee-Object -FilePath $ArquivoSaida
} else {
    Write-Host "📺 Modo tempo real (pressione Ctrl+C para parar)" -ForegroundColor Green
    Write-Host ""
    
    # Executar em tempo real com filtro
    & $adbPath logcat -v time *:$Nivel | 
        Select-String -Pattern $Filtro |
        ForEach-Object {
            $linha = $_.Line
            
            # Colorir por tipo de log
            if ($linha -match "❌|ERROR|Erro|Falha") {
                Write-Host $linha -ForegroundColor Red
            } elseif ($linha -match "⚠️|WARNING|Aviso") {
                Write-Host $linha -ForegroundColor Yellow
            } elseif ($linha -match "✅|SUCCESS|Sucesso") {
                Write-Host $linha -ForegroundColor Green
            } elseif ($linha -match "🔍|🔧|📋|DIAGNÓSTICO|CONVERSÃO") {
                Write-Host $linha -ForegroundColor Cyan
            } elseif ($linha -match "FIRESTORE|Firestore") {
                Write-Host $linha -ForegroundColor Magenta
            } else {
                Write-Host $linha
            }
        }
}
