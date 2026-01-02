# Script PowerShell para capturar logs de login do Android (VERSÃO MELHORADA)
# Uso: .\capturar-logs-login-v2.ps1

Write-Host "=== CAPTURA DE LOGS DE LOGIN (VERSÃO MELHORADA) ===" -ForegroundColor Cyan
Write-Host ""

# Verificar se ADB está disponível
$adbPath = Get-Command adb -ErrorAction SilentlyContinue
if (-not $adbPath) {
    Write-Host "❌ ADB não encontrado. Certifique-se de que o Android SDK está instalado e no PATH." -ForegroundColor Red
    exit 1
}

# Verificar dispositivos conectados
Write-Host "Verificando dispositivos conectados..." -ForegroundColor Yellow
$devices = adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "device$" }

if ($devices.Count -eq 0) {
    Write-Host "❌ Nenhum dispositivo Android conectado!" -ForegroundColor Red
    Write-Host "   Conecte um dispositivo via USB e habilite a depuração USB." -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ Dispositivo encontrado!" -ForegroundColor Green
Write-Host ""

# Limpar logs anteriores
Write-Host "Limpando logs anteriores..." -ForegroundColor Yellow
adb logcat -c
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "===== CAPTURANDO LOGS DE LOGIN =====" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Filtros ativos:" -ForegroundColor Yellow
Write-Host "  - AuthViewModel (Log.d e Timber.d)" -ForegroundColor White
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
# Usar filtro mais amplo para capturar tanto Log.d quanto Timber.d
adb logcat -v time *:D | Select-String -Pattern "AuthViewModel|LoginFragment|FirebaseAuth|LoginDiagnostics|LOGIN_FLOW|BUSCA_NUVEM|MÉTODO login|viewModelScope|signInWithEmailAndPassword|sign-out|signOut|DENTRO DO|MODO ONLINE|LOGIN ONLINE|criarOuAtualizarColaboradorOnline|ERRO FINAL|não encontrado|não está aprovado|está inativo|PRIMEIRO ACESSO" -Context 0,1
