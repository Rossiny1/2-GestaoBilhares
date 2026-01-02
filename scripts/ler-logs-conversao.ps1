# Script PowerShell específico para logs de conversão de Colaborador
# Foca nos logs relacionados ao problema "não foi possível converter"

param(
    [switch]$SalvarArquivo = $false,
    [string]$ArquivoSaida = "logs-conversao-$(Get-Date -Format 'yyyyMMdd-HHmmss').txt"
)

# Caminho do ADB
$adbPath = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe

# Filtros específicos para conversão
$filtros = @(
    "CONVERSÃO",
    "toObject",
    "Gson",
    "Colaborador",
    "getColaboradorByUid",
    "CRIAR_PENDENTE",
    "FIRESTORE.*Colaborador",
    "não foi possível converter",
    "doc.data",
    "doc.getBoolean",
    "dataConvertida"
)

$filtroCombinado = $filtros -join "|"

Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "🔍 LOGS DE CONVERSÃO - Diagnóstico de Colaborador" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

# Limpar buffer
& $adbPath logcat -c | Out-Null

if ($SalvarArquivo) {
    Write-Host "💾 Salvando em: $ArquivoSaida" -ForegroundColor Green
    & $adbPath logcat -v time *:D | 
        Select-String -Pattern $filtroCombinado | 
        Tee-Object -FilePath $ArquivoSaida
} else {
    Write-Host "📺 Modo tempo real (Ctrl+C para parar)" -ForegroundColor Green
    Write-Host ""
    & $adbPath logcat -v time *:D | 
        Select-String -Pattern $filtroCombinado |
        ForEach-Object {
            $linha = $_.Line
            if ($linha -match "❌|ERROR|Falha|null") {
                Write-Host $linha -ForegroundColor Red
            } elseif ($linha -match "✅|SUCCESS|convertido") {
                Write-Host $linha -ForegroundColor Green
            } elseif ($linha -match "📋|DIAGNÓSTICO|doc.data|doc.getBoolean") {
                Write-Host $linha -ForegroundColor Cyan
            } elseif ($linha -match "🔧|CONVERSÃO") {
                Write-Host $linha -ForegroundColor Yellow
            } else {
                Write-Host $linha
            }
        }
}
