# ========================================
# SCRIPT PARA CRIAR COLABORADOR ADMIN INICIAL
# Cria o colaborador rossinys@gmail.com no banco local
# ========================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CRIANDO COLABORADOR ADMIN INICIAL" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Definir o caminho do ADB
$adbPath = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verificar se o ADB existe
if (-not (Test-Path $adbPath)) {
    Write-Host "ERRO: ADB nao encontrado em '$adbPath'." -ForegroundColor Red
    exit 1
}

Write-Host "INSTRUCOES:" -ForegroundColor Yellow
Write-Host "1. Execute este script ANTES de tentar fazer login" -ForegroundColor Yellow
Write-Host "2. O script vai criar o colaborador admin no banco local" -ForegroundColor Yellow
Write-Host "3. Depois tente fazer login com rossinys@gmail.com" -ForegroundColor Yellow
Write-Host ""

# Limpar logcat anterior
Write-Host "Limpando logcat anterior..." -ForegroundColor Yellow
& $adbPath logcat -c

Write-Host "Aguardando app estar rodando..." -ForegroundColor Green
Write-Host "Abra o app e aguarde a tela de login aparecer" -ForegroundColor Green
Write-Host ""

# Padrao de filtro para capturar logs de criacao de colaborador
$pattern = "gestaobilhares|AuthViewModel|LoginFragment|AppRepository|Colaborador|ADMIN|rossinys|Criando|Criado|Inserindo|Inserido|DB_POPULATION|COLABORADOR|Admin|admin"

Write-Host "Monitorando criacao do colaborador admin..." -ForegroundColor Green
Write-Host "Filtros: $pattern" -ForegroundColor Gray
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Monitorar logcat
try {
    & $adbPath logcat -v time | Select-String -Pattern $pattern
} catch {
    Write-Host "Erro ao executar logcat: $($_.Exception.Message)" -ForegroundColor Red
}
