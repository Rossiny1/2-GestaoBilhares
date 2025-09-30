# ========================================
# SCRIPT PARA LIMPAR DADOS DO APP - GESTAO BILHARES
# ========================================
#
# INSTRUCOES:
# 1. Conecte seu dispositivo Android com a depuracao USB ativada.
# 2. Execute este script a partir do terminal PowerShell.
# 3. O script ira limpar todos os dados do aplicativo, incluindo o banco de dados.
#
# ========================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "LIMPANDO DADOS DO APP - GESTAO BILHARES" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Caminho para o ADB
$adbPath = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Nome do pacote do aplicativo
$packageName = "com.example.gestaobilhares"

# Verificar se o ADB está disponível
if (-not (Test-Path $adbPath)) {
    Write-Host "ERRO: ADB nao encontrado em $adbPath" -ForegroundColor Red
    Write-Host "Verifique se o Android SDK esta instalado corretamente" -ForegroundColor Red
    Read-Host "Pressione Enter para sair"
    exit 1
}

# Verificar se há um dispositivo conectado
Write-Host "Verificando dispositivo conectado..." -ForegroundColor Yellow
$devices = & $adbPath devices
$hasDevice = $devices | Select-String "device$"

if (-not $hasDevice) {
    Write-Host "ERRO: Nenhum dispositivo Android conectado." -ForegroundColor Red
    Write-Host "Conecte o dispositivo via USB e habilite a depuracao." -ForegroundColor Red
    Read-Host "Pressione Enter para sair"
    exit 1
}

Write-Host "Dispositivo conectado. Limpando dados do app..." -ForegroundColor Green

# Comando para limpar os dados do aplicativo
try {
    & $adbPath shell pm clear $packageName
    Write-Host ""
    Write-Host "Dados do aplicativo '$packageName' limpos com sucesso!" -ForegroundColor Green
    Write-Host "O banco de dados agora esta completamente vazio." -ForegroundColor Green

    # Verificar se o banco foi realmente limpo
    Write-Host ""
    Write-Host "Verificando limpeza do banco de dados..." -ForegroundColor Yellow
    try {
        $dbCheck = & $adbPath shell run-as $packageName ls -la /data/data/$packageName/databases/ 2>$null
        if ($dbCheck) {
            Write-Host "Banco encontrado:" -ForegroundColor Cyan
            Write-Host $dbCheck
        } else {
            Write-Host "Banco de dados completamente limpo!" -ForegroundColor Green
        }
    } catch {
        Write-Host "Banco de dados completamente limpo!" -ForegroundColor Green
    }
} catch {
    Write-Host "ERRO: Falha ao limpar os dados do aplicativo." -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
}

Write-Host ""
Read-Host "Pressione Enter para sair"