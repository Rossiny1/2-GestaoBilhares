# ========================================
# SCRIPT DE DEBUG COMPLETO - CAPTURA CRASHES
# Captura TODOS os logs, incluindo crashes silenciosos
# ========================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "DEBUG CRASH COMPLETO - GESTAO BILHARES" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Definir o caminho do ADB
$adbPath = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verificar se o ADB existe
if (-not (Test-Path $adbPath)) {
    Write-Host "ERRO: ADB nao encontrado em '$adbPath'." -ForegroundColor Red
    exit 1
}

# Limpar logcat anterior
Write-Host "Limpando logcat anterior..." -ForegroundColor Yellow
& $adbPath logcat -c

Write-Host "INSTRUCOES:" -ForegroundColor Yellow
Write-Host "1. Agora tente fazer login no app" -ForegroundColor Yellow
Write-Host "2. Se o app crashar, os logs serao capturados" -ForegroundColor Yellow
Write-Host "3. Pressione Ctrl+C para parar" -ForegroundColor Yellow
Write-Host ""

# Padrao de filtro EXPANDIDO para capturar crashes
$pattern = "gestaobilhares|AuthViewModel|LoginFragment|AppRepository|FirebaseAuth|GoogleSignIn|INICIANDO|ERRO|SUCESSO|FALHA|Exception|Error|FATAL|AndroidRuntime|crash|Killing|appDied|ActivityManager|WindowManager|SurfaceFlinger|BufferQueue|MainActivity|SignInHubActivity|onCreate|onStart|onResume|onPause|onStop|onDestroy|onActivityResult|initializeRepository|login|signInWithGoogle|FirebaseApp|FirebaseAuth|GoogleSignInClient|GoogleSignInAccount|ApiException|NetworkException|SecurityException|IllegalStateException|NullPointerException|UninitializedPropertyAccessException|lateinit|isInitialized|appRepository|networkUtils|syncManager|userSessionManager"

Write-Host "Monitorando TODOS os logs (incluindo crashes)..." -ForegroundColor Green
Write-Host "Filtros: $pattern" -ForegroundColor Gray
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Monitorar logcat com filtros expandidos
try {
    & $adbPath logcat -v time | Select-String -Pattern $pattern
} catch {
    Write-Host "Erro ao executar logcat: $($_.Exception.Message)" -ForegroundColor Red
}
