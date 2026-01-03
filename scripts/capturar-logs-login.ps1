# Script PowerShell para capturar logs de login do Android
# Uso: .\capturar-logs-login.ps1

Write-Host "=== CAPTURA DE LOGS DE LOGIN ===" -ForegroundColor Cyan
Write-Host ""

# Caminho do ADB (ajuste se necessário)
$adbPath = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verificar se ADB existe
if (-not (Test-Path $adbPath)) {
    Write-Host "❌ ADB não encontrado em: $adbPath" -ForegroundColor Red
    Write-Host "   Verifique o caminho do Android SDK." -ForegroundColor Yellow
    exit 1
}

# Verificar dispositivos conectados
Write-Host "Verificando dispositivos conectados..." -ForegroundColor Yellow
$devices = & $adbPath devices | Select-Object -Skip 1 | Where-Object { $_ -match "device$" }

if ($devices.Count -eq 0) {
    Write-Host "❌ Nenhum dispositivo Android conectado!" -ForegroundColor Red
    Write-Host "   Conecte um dispositivo via USB e habilite a depuração USB." -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ Dispositivo encontrado!" -ForegroundColor Green
Write-Host ""

# Limpar logs anteriores
Write-Host "Limpando logs anteriores..." -ForegroundColor Yellow
& $adbPath logcat -c
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "===== CAPTURANDO LOGS DE LOGIN =====" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Filtros ativos:" -ForegroundColor Yellow
Write-Host "  - AuthViewModel (todos os níveis)" -ForegroundColor White
Write-Host "  - LoginFragment (todos os níveis)" -ForegroundColor White
Write-Host "  - FirebaseAuth (todos os níveis)" -ForegroundColor White
Write-Host "  - LoginDiagnostics (todos os níveis)" -ForegroundColor White
Write-Host ""
Write-Host "Logs específicos capturados:" -ForegroundColor Yellow
Write-Host "  - 🚀 MÉTODO login() FOI CHAMADO" -ForegroundColor White
Write-Host "  - 🟢 DENTRO DO viewModelScope.launch" -ForegroundColor White
Write-Host "  - 🌐 MODO ONLINE - INICIANDO LOGIN" -ForegroundColor White
Write-Host "  - ✅ LOGIN ONLINE SUCESSO" -ForegroundColor White
Write-Host "  - 🔍 Chamando criarOuAtualizarColaboradorOnline" -ForegroundColor White
Write-Host "  - ❌ ERRO FINAL: Colaborador não encontrado" -ForegroundColor White
Write-Host "  - ❌ Colaborador não está aprovado" -ForegroundColor White
Write-Host "  - ❌ Colaborador está inativo" -ForegroundColor White
Write-Host "  - ⚠️ PRIMEIRO ACESSO DETECTADO" -ForegroundColor White
Write-Host ""
Write-Host "Aguardando eventos de login..." -ForegroundColor Yellow
Write-Host "Pressione Ctrl+C para parar a captura" -ForegroundColor Yellow
Write-Host ""

# Capturar logs com filtros específicos
# Filtros:
# - AuthViewModel: todos os níveis (D, I, W, E)
# - LoginFragment: todos os níveis
# - FirebaseAuth: todos os níveis
# - LoginDiagnostics: todos os níveis
# - Buscar por strings específicas que adicionamos

& $adbPath logcat -v time | Select-String -Pattern "AuthViewModel|LoginFragment|FirebaseAuth|LoginDiagnostics|LOGIN_FLOW|BUSCA_NUVEM|MÉTODO login|viewModelScope|signInWithEmailAndPassword|sign-out|signOut" -Context 0,2
