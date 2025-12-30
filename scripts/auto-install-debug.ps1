# Script PowerShell para verificar mudanças remotas e instalar automaticamente o app
# Uso: .\scripts\auto-install-debug.ps1

Write-Host "🔄 Verificando mudanças remotas..." -ForegroundColor Cyan

# Verificar se há mudanças remotas
git fetch origin

$LOCAL = git rev-parse @
$REMOTE = git rev-parse @{u}
$BASE = git merge-base @ @{u}

if ($LOCAL -eq $REMOTE) {
    Write-Host "✅ Repositório local está atualizado. Nenhuma mudança para instalar." -ForegroundColor Green
    exit 0
}
elseif ($LOCAL -eq $BASE) {
    Write-Host "📥 Atualizações disponíveis. Fazendo pull..." -ForegroundColor Yellow
    git pull origin
    
    Write-Host "🔨 Compilando e instalando app..." -ForegroundColor Cyan
    .\gradlew.bat installDebug
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ App instalado com sucesso no dispositivo conectado!" -ForegroundColor Green
    }
    else {
        Write-Host "❌ Erro ao instalar app. Verifique os logs acima." -ForegroundColor Red
        exit 1
    }
}
elseif ($REMOTE -eq $BASE) {
    Write-Host "⚠️  Você tem commits locais não enviados. Faça push primeiro." -ForegroundColor Yellow
    exit 1
}
else {
    Write-Host "⚠️  Divergência detectada. Faça merge manualmente." -ForegroundColor Red
    exit 1
}
