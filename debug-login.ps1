# ========================================
# SCRIPT DE DEBUG ESPECÍFICO PARA LOGIN
# Captura logs detalhados do fluxo de login
# ========================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "DEBUG LOGIN - GESTAO BILHARES" -ForegroundColor Cyan
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
Write-Host "2. Os logs serao capturados em tempo real" -ForegroundColor Yellow
Write-Host "3. Pressione Ctrl+C para parar" -ForegroundColor Yellow
Write-Host ""

# Padrao de filtro para login especifico
$pattern = "gestaobilhares|AuthViewModel|LoginFragment|AppRepository|FirebaseAuth|GoogleSignIn|INICIANDO|ERRO|SUCESSO|FALHA|Exception|Error|FATAL|AndroidRuntime|crash"

Write-Host "Monitorando logs de login..." -ForegroundColor Green
Write-Host "Filtros: $pattern" -ForegroundColor Gray
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Monitorar logcat com filtros especificos
try {
    & $adbPath logcat -v time | Select-String -Pattern $pattern
} catch {
    Write-Host "Erro ao executar logcat: $($_.Exception.Message)" -ForegroundColor Red
}
