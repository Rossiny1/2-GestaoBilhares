# Script para testar sincronizacao de colaborador USER em app vazio
# Foco: Verificar se rotas sao importadas quando nao existem dados locais

Write-Host "=== TESTANDO SINCRONIZACAO USER EM APP VAZIO ===" -ForegroundColor Yellow
Write-Host "Objetivo: Verificar se colaborador USER consegue importar suas rotas atribuidas" -ForegroundColor Cyan
Write-Host ""

# Caminho do ADB (mesmo padrao dos outros scripts)
$ADB = "C:\Users\$($env:USERNAME)\AppData\Local\Android\Sdk\platform-tools\adb.exe"
Write-Host "ADB path: $ADB" -ForegroundColor Gray

# Verificar se o ADB existe
if (!(Test-Path $ADB)) {
    Write-Host "ERRO: ADB nao encontrado em: $ADB" -ForegroundColor Red
    Write-Host "Certifique-se de que o Android SDK esta instalado corretamente" -ForegroundColor Red
    exit 1
}

Write-Host "OK: ADB encontrado" -ForegroundColor Green

# Verificar se ha dispositivo conectado
Write-Host "Verificando dispositivo..." -ForegroundColor Green
try {
    $adbDevices = & $ADB devices 2>$null
} catch {
    Write-Host "ERRO: Erro ao executar ADB: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Certifique-se de que um dispositivo/emulador esta conectado" -ForegroundColor Red
    exit 1
}

if ($adbDevices -notmatch "device$") {
    Write-Host "ERRO: Nenhum dispositivo conectado via ADB" -ForegroundColor Red
    Write-Host "Conecte um dispositivo ou inicie um emulador" -ForegroundColor Red
    exit 1
}

Write-Host "OK: Dispositivo conectado" -ForegroundColor Green
Write-Host ""

# Limpar logs anteriores
Write-Host "Limpando logs anteriores..." -ForegroundColor Green
try {
    & $ADB logcat -c
} catch {
    Write-Host "AVISO: Nao foi possivel limpar logs anteriores" -ForegroundColor Yellow
}

# Iniciar captura de logs
Write-Host "=== INICIANDO CAPTURA DE LOGS ===" -ForegroundColor Green
Write-Host "Aguarde sincronizacao completar..." -ForegroundColor Yellow
Write-Host "Pressione Ctrl+C quando terminar" -ForegroundColor Yellow
Write-Host ""

# Comando de logcat com filtros relevantes
$logcatCommand = "$ADB logcat -v time -s SyncRepository:W UserSessionManager:W AuthViewModel:W RoutesViewModel:W"

try {
    Invoke-Expression $logcatCommand
} catch {
    Write-Host "ERRO: Erro ao executar logcat: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== RESUMO ESPERADO ===" -ForegroundColor Cyan
Write-Host "1. Bootstrap deve ser ATIVADO para primeiro login"
Write-Host "2. allowRouteBootstrap deve ser 'true'"
Write-Host "3. Queries devem ser executadas SEM filtro de rota"
Write-Host "4. Rotas atribuidas ao colaborador devem ser importadas"
Write-Host "5. Apos importar rotas, filtros devem voltar ao normal"
Write-Host ""

Write-Host "=== PROCURE POR ESTAS LINHAS NOS LOGS ===" -ForegroundColor Yellow
Write-Host "- 'Aplicando bootstrap temporario sem filtro de rota'"
Write-Host "- 'Bootstrap de rotas: baixando todas as rotas temporariamente'"
Write-Host "- 'Pull Rotas: X sincronizadas' (X > 0)"
Write-Host "- 'Pull Colaboradores: Y sincronizados' (Y > 0)"
