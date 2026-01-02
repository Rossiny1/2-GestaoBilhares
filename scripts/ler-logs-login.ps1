# Script PowerShell específico para logs de login e autenticação

param(
    [switch]$SalvarArquivo = $false,
    [string]$ArquivoSaida = "logs-login-$(Get-Date -Format 'yyyyMMdd-HHmmss').txt"
)

# Caminho do ADB
$adbPath = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Filtros específicos para login
$filtros = @(
    "LoginFragment",
    "AuthViewModel",
    "login",
    "signInWithEmailAndPassword",
    "FirebaseAuth",
    "LOGIN",
    "AUTH",
    "aprovado",
    "Colaborador encontrado",
    "não foi possível converter",
    "getColaboradorByUid",
    "createPendingColaborador"
)

$filtroCombinado = $filtros -join "|"

Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "🔐 LOGS DE LOGIN - Diagnóstico de Autenticação" -ForegroundColor Cyan
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
            if ($linha -match "❌|ERROR|Falha|Exception") {
                Write-Host $linha -ForegroundColor Red
            } elseif ($linha -match "✅|SUCCESS|Sucesso|LOGIN.*SUCESSO") {
                Write-Host $linha -ForegroundColor Green
            } elseif ($linha -match "🔍|🔧|FIRESTORE|Firestore") {
                Write-Host $linha -ForegroundColor Cyan
            } elseif ($linha -match "aprovado.*false|Aguardando aprovação") {
                Write-Host $linha -ForegroundColor Yellow
            } elseif ($linha -match "aprovado.*true|Aprovado") {
                Write-Host $linha -ForegroundColor Green
            } else {
                Write-Host $linha
            }
        }
}
