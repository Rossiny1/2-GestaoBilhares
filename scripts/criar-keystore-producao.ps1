# Script para criar keystore de producao
# Execute este script para criar o keystore necessario para assinar o APK de release

Write-Host "CRIANDO KEYSTORE DE PRODUCAO" -ForegroundColor Cyan
Write-Host "=================================" -ForegroundColor Cyan
Write-Host ""

# Verificar se keytool esta disponivel
$keytoolPath = "keytool"
try {
    $null = Get-Command keytool -ErrorAction Stop
} catch {
    Write-Host "ERRO: keytool nao encontrado!" -ForegroundColor Red
    Write-Host "   Certifique-se de que o JDK esta instalado e no PATH" -ForegroundColor Yellow
    Write-Host "   Ou use o keytool do Android Studio: C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -ForegroundColor Yellow
    exit 1
}

# Solicitar informacoes
Write-Host "Por favor, forneca as seguintes informacoes:" -ForegroundColor Yellow
Write-Host ""

$keystoreName = Read-Host "Nome do keystore (ex: gestaobilhares-release)"
if ([string]::IsNullOrWhiteSpace($keystoreName)) {
    $keystoreName = "gestaobilhares-release"
}

$keystorePath = Read-Host "Caminho completo onde salvar (ex: C:\keystores\gestaobilhares-release.jks)"
if ([string]::IsNullOrWhiteSpace($keystorePath)) {
    $keystorePath = "$PWD\$keystoreName.jks"
}

# Garantir que o caminho termina com .jks
if (-not $keystorePath.EndsWith(".jks")) {
    $keystorePath = "$keystorePath.jks"
}

# Verificar se o diretorio existe
$directory = Split-Path -Parent $keystorePath
if (-not [string]::IsNullOrWhiteSpace($directory) -and -not (Test-Path $directory)) {
    Write-Host "Criando diretorio: $directory" -ForegroundColor Gray
    New-Item -ItemType Directory -Path $directory -Force | Out-Null
}

# Verificar se ja existe
if (Test-Path $keystorePath) {
    Write-Host "AVISO: O arquivo $keystorePath ja existe!" -ForegroundColor Yellow
    $overwrite = Read-Host "Deseja sobrescrever? (s/N)"
    if ($overwrite -ne "s" -and $overwrite -ne "S") {
        Write-Host "Operacao cancelada" -ForegroundColor Red
        exit 0
    }
}

Write-Host ""
Write-Host "Informacoes do certificado:" -ForegroundColor Yellow

$alias = Read-Host "Alias da chave (ex: gestaobilhares)"
if ([string]::IsNullOrWhiteSpace($alias)) {
    $alias = "gestaobilhares"
}

$validity = Read-Host "Validade em dias (padrao: 10000 = ~27 anos)"
if ([string]::IsNullOrWhiteSpace($validity)) {
    $validity = "10000"
}

Write-Host ""
Write-Host "Senhas (serao solicitadas pelo keytool):" -ForegroundColor Yellow
Write-Host "   - Senha do keystore (guarde em local seguro!)" -ForegroundColor Gray
Write-Host "   - Senha da chave (pode ser a mesma)" -ForegroundColor Gray
Write-Host ""

# Comando keytool
$keytoolCommand = "keytool -genkey -v -keystore `"$keystorePath`" -keyalg RSA -keysize 2048 -validity $validity -alias $alias"

Write-Host "Executando comando:" -ForegroundColor Cyan
Write-Host "   $keytoolCommand" -ForegroundColor Gray
Write-Host ""

# Executar keytool
& keytool -genkey -v -keystore $keystorePath -keyalg RSA -keysize 2048 -validity $validity -alias $alias

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "KEYSTORE CRIADO COM SUCESSO!" -ForegroundColor Green
    Write-Host ""
    Write-Host "PROXIMOS PASSOS:" -ForegroundColor Yellow
    Write-Host "   1. Crie o arquivo keystore.properties na raiz do projeto:" -ForegroundColor White
    Write-Host ""
    Write-Host "      storePassword=SUA_SENHA_DO_KEYSTORE" -ForegroundColor Gray
    Write-Host "      keyPassword=SUA_SENHA_DA_CHAVE" -ForegroundColor Gray
    Write-Host "      keyAlias=$alias" -ForegroundColor Gray
    Write-Host "      storeFile=$keystorePath" -ForegroundColor Gray
    Write-Host ""
    Write-Host "   2. IMPORTANTE: Guarde o keystore e as senhas em local SEGURO!" -ForegroundColor Red
    Write-Host "      Sem o keystore, voce NAO podera atualizar o app no Play Store!" -ForegroundColor Red
    Write-Host ""
    Write-Host "   3. O arquivo keystore.properties ja esta no .gitignore (nao sera commitado)" -ForegroundColor Green
    Write-Host ""
} else {
    Write-Host ""
    Write-Host "ERRO ao criar keystore!" -ForegroundColor Red
    Write-Host "   Verifique as mensagens de erro acima" -ForegroundColor Yellow
    exit 1
}
