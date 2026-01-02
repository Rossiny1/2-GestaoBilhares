# Script PowerShell completo para ler todos os logs do app
# Inclui opções avançadas de filtragem

param(
    [ValidateSet("Debug", "Info", "Warning", "Error", "All")]
    [string]$Nivel = "Debug",
    
    [string]$Tag = "",
    
    [switch]$SalvarArquivo = $false,
    
    [string]$ArquivoSaida = "logs-completo-$(Get-Date -Format 'yyyyMMdd-HHmmss').txt",
    
    [switch]$LimparBuffer = $true,
    
    [int]$LimiteLinhas = 0  # 0 = sem limite
)

# Caminho do ADB
$adbPath = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verificar ADB
if (-not (Test-Path $adbPath)) {
    Write-Host "❌ ADB não encontrado em: $adbPath" -ForegroundColor Red
    exit 1
}

# Verificar dispositivo
$devices = & $adbPath devices
if ($devices.Count -lt 2) {
    Write-Host "❌ Nenhum dispositivo conectado!" -ForegroundColor Red
    exit 1
}

Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "📱 LEITURA COMPLETA DE LOGS - Gestão Bilhares" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "ADB: $adbPath" -ForegroundColor Gray
Write-Host "Nível: $Nivel" -ForegroundColor Gray
if ($Tag) { Write-Host "Tag: $Tag" -ForegroundColor Gray }
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

# Limpar buffer se solicitado
if ($LimparBuffer) {
    Write-Host "🧹 Limpando buffer de logs..." -ForegroundColor Yellow
    & $adbPath logcat -c | Out-Null
}

# Mapear nível
$nivelChar = switch ($Nivel) {
    "Debug" { "D" }
    "Info" { "I" }
    "Warning" { "W" }
    "Error" { "E" }
    "All" { "V" }
    default { "D" }
}

# Construir comando
$comando = "logcat -v time"
if ($nivelChar -ne "V") {
    $comando += " *:$nivelChar"
}

# Executar
if ($SalvarArquivo) {
    Write-Host "💾 Salvando logs em: $ArquivoSaida" -ForegroundColor Green
    Write-Host "Pressione Ctrl+C para parar" -ForegroundColor Yellow
    Write-Host ""
    
    $output = & $adbPath $comando.Split(' ')
    if ($Tag) {
        $output | Select-String -Pattern $Tag | Tee-Object -FilePath $ArquivoSaida
    } else {
        $output | Tee-Object -FilePath $ArquivoSaida
    }
} else {
    Write-Host "📺 Modo tempo real (Ctrl+C para parar)" -ForegroundColor Green
    Write-Host ""
    
    $contador = 0
    & $adbPath $comando.Split(' ') | ForEach-Object {
        if ($LimiteLinhas -gt 0 -and $contador -ge $LimiteLinhas) {
            return
        }
        
        $linha = $_.ToString()
        
        # Filtrar por tag se especificado
        if ($Tag -and $linha -notmatch $Tag) {
            return
        }
        
        # Colorir logs
        if ($linha -match "E/|ERROR|❌|Erro|Falha|Exception") {
            Write-Host $linha -ForegroundColor Red
        } elseif ($linha -match "W/|WARNING|⚠️|Aviso") {
            Write-Host $linha -ForegroundColor Yellow
        } elseif ($linha -match "I/|INFO|✅|Sucesso") {
            Write-Host $linha -ForegroundColor Green
        } elseif ($linha -match "D/AppRepository|D/AuthViewModel|D/LoginFragment") {
            Write-Host $linha -ForegroundColor Cyan
        } else {
            Write-Host $linha
        }
        
        $contador++
    }
}

Write-Host ""
Write-Host "✅ Leitura de logs finalizada" -ForegroundColor Green
