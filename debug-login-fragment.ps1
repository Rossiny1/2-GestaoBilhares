# ========================================
# SCRIPT DE DEBUG - LOGINFRAGMENT
# ========================================
#
# INSTRUCOES IMPORTANTES:
# 1. Execute este script em uma NOVA JANELA do PowerShell
# 2. Para abrir nova janela: Ctrl+Shift+N ou Win+R -> powershell
# 3. Navegue ate a pasta do projeto: cd "C:\Users\Rossiny\Desktop\2-GestaoBilhares"
# 4. Execute: .\debug-login-fragment.ps1
#
# CAMINHO CORRETO DO ADB:
# C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe
#
# ========================================

Write-Host "========================================" -ForegroundColor Red
Write-Host "DEBUG LOGINFRAGMENT - GESTAO BILHARES" -ForegroundColor Red
Write-Host "========================================" -ForegroundColor Red
Write-Host ""
Write-Host "INSTRUCOES:" -ForegroundColor Yellow
Write-Host "1. Este script deve ser executado em JANELA SEPARADA" -ForegroundColor Yellow
Write-Host "2. Para abrir nova janela: Ctrl+Shift+N ou Win+R -> powershell" -ForegroundColor Yellow
Write-Host "3. Navegue ate: cd 'C:\Users\Rossiny\Desktop\2-GestaoBilhares'" -ForegroundColor Yellow
Write-Host "4. Execute: .\debug-login-fragment.ps1" -ForegroundColor Yellow
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

Write-Host "Iniciando monitoramento de LOGINFRAGMENT..." -ForegroundColor Green
Write-Host "Agora voce pode testar o app no dispositivo" -ForegroundColor Green
Write-Host ""
Write-Host "LOGS MONITORADOS (Filtro: LoginFragment|AuthViewModel|AppRepository|FATAL|AndroidRuntime|Exception):" -ForegroundColor Cyan
Write-Host ""

# Padrao de filtro para logs de LoginFragment
$patternLogin = "LoginFragment|AuthViewModel|AppRepository|FATAL|AndroidRuntime|Exception|Caused by|LOGINFRAGMENT ONCREATE|LOGINFRAGMENT ONCREATEVIEW|LOGINFRAGMENT ONVIEWCREATED|INICIANDO REPOSITORIO|AppDatabase inicializado|AppRepository inicializado"

# Monitorar logcat filtrando logs relevantes para o LoginFragment
try {
    & $adbPath logcat -v time | Select-String -Pattern $patternLogin
} catch {
    Write-Host "Erro ao executar logcat: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Verifique se o dispositivo esta conectado e o ADB esta funcionando." -ForegroundColor Red
}
