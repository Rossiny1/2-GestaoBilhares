# ========================================
# SCRIPT DE TESTE - CORREÇÃO DO LOGIN
# ========================================
#
# INSTRUCOES IMPORTANTES:
# 1. Execute este script em uma NOVA JANELA do PowerShell
# 2. Para abrir nova janela: Ctrl+Shift+N ou Win+R -> powershell
# 3. Navegue ate a pasta do projeto: cd "C:\Users\Rossiny\Desktop\2-GestaoBilhares"
# 4. Execute: .\test-login-fix.ps1
#
# CAMINHO CORRETO DO ADB:
# C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe
#
# ========================================

Write-Host "========================================" -ForegroundColor Green
Write-Host "TESTE DE CORRECAO DO LOGIN - GESTAO BILHARES" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "INSTRUCOES:" -ForegroundColor Yellow
Write-Host "1. Este script deve ser executado em JANELA SEPARADA" -ForegroundColor Yellow
Write-Host "2. Para abrir nova janela: Ctrl+Shift+N ou Win+R -> powershell" -ForegroundColor Yellow
Write-Host "3. Navegue ate: cd 'C:\Users\Rossiny\Desktop\2-GestaoBilhares'" -ForegroundColor Yellow
Write-Host "4. Execute: .\test-login-fix.ps1" -ForegroundColor Yellow
Write-Host ""

# Definir o caminho do ADB
$adbPath = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verificar se o ADB existe
if (-not (Test-Path $adbPath)) {
    Write-Host "ERRO: ADB nao encontrado em '$adbPath'." -ForegroundColor Red
    Write-Host "Por favor, verifique o caminho ou adicione o ADB ao PATH do sistema." -ForegroundColor Red
    exit 1
}

# Limpar logcat anterior
Write-Host "Limpando logcat anterior..." -ForegroundColor Yellow
& $adbPath logcat -c

Write-Host "Iniciando monitoramento de logs de LOGIN CORRIGIDO..." -ForegroundColor Green
Write-Host "Agora voce pode testar o login no app no dispositivo" -ForegroundColor Green
Write-Host ""
Write-Host "LOGS MONITORADOS (Filtro: LoginFragment|AuthViewModel|AppRepository|Firebase|GoogleSignIn|FATAL|AndroidRuntime|Exception):" -ForegroundColor Cyan
Write-Host ""

# Padrao de filtro para logs de login corrigido
$patternLogin = "LoginFragment|AuthViewModel|AppRepository|Firebase|GoogleSignIn|FATAL|AndroidRuntime|Exception|Caused by|LOGINFRAGMENT ONVIEWCREATED|INICIANDO REPOSITORIO|AppDatabase inicializado|AppRepository inicializado|Google Sign-In configurado|LOGIN HIBRIDO|LOGIN OFFLINE SUCESSO|LOGIN ONLINE SUCESSO"

# Monitorar logcat filtrando logs relevantes para o login corrigido
try {
    & $adbPath logcat -v time | Select-String -Pattern $patternLogin
} catch {
    Write-Host "Erro ao executar logcat: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Verifique se o dispositivo esta conectado e o ADB esta funcionando." -ForegroundColor Red
}
