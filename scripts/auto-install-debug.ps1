# Script PowerShell para verificar mudanças remotas e instalar automaticamente o app
# Uso: .\scripts\auto-install-debug.ps1

$ErrorActionPreference = "Continue"

Write-Host "🔄 Verificando mudanças remotas..." -ForegroundColor Cyan

# Verificar se estamos em um repositório git
if (-not (Test-Path ".git")) {
    Write-Host "❌ Erro: Não é um repositório Git. Execute este script na raiz do projeto." -ForegroundColor Red
    exit 1
}

# Verificar se gradlew.bat existe
if (-not (Test-Path "gradlew.bat")) {
    Write-Host "❌ Erro: gradlew.bat não encontrado. Execute este script na raiz do projeto." -ForegroundColor Red
    exit 1
}

try {
    # Verificar se há mudanças remotas
    git fetch origin 2>&1 | Out-Null
    
    $LOCAL = git rev-parse @ 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "⚠️  Erro ao verificar commit local. Tentando pull direto..." -ForegroundColor Yellow
        git pull origin 2>&1 | ForEach-Object { Write-Host $_ }
        
        Write-Host "🔨 Compilando e instalando app..." -ForegroundColor Cyan
        .\gradlew.bat installDebug
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ App instalado com sucesso no dispositivo conectado!" -ForegroundColor Green
        } else {
            Write-Host "❌ Erro ao instalar app. Verifique os logs acima." -ForegroundColor Red
            exit 1
        }
        exit 0
    }
    
    $REMOTE = git rev-parse @{u} 2>&1
    $BASE = git merge-base @ @{u} 2>&1
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "⚠️  Branch remota não configurada. Tentando pull direto..." -ForegroundColor Yellow
        git pull origin 2>&1 | ForEach-Object { Write-Host $_ }
        
        Write-Host "🔨 Compilando e instalando app..." -ForegroundColor Cyan
        .\gradlew.bat installDebug
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ App instalado com sucesso no dispositivo conectado!" -ForegroundColor Green
        } else {
            Write-Host "❌ Erro ao instalar app. Verifique os logs acima." -ForegroundColor Red
            exit 1
        }
        exit 0
    }
    
    if ($LOCAL -eq $REMOTE) {
        Write-Host "✅ Repositório local está atualizado. Nenhuma mudança para instalar." -ForegroundColor Green
        Write-Host "🔨 Compilando e instalando app mesmo assim..." -ForegroundColor Cyan
        .\gradlew.bat installDebug
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ App instalado com sucesso no dispositivo conectado!" -ForegroundColor Green
        } else {
            Write-Host "❌ Erro ao instalar app. Verifique os logs acima." -ForegroundColor Red
            exit 1
        }
        exit 0
    }
    elseif ($LOCAL -eq $BASE) {
        Write-Host "📥 Atualizações disponíveis. Fazendo pull..." -ForegroundColor Yellow
        git pull origin 2>&1 | ForEach-Object { Write-Host $_ }
        
        if ($LASTEXITCODE -ne 0) {
            Write-Host "⚠️  Erro ao fazer pull, mas continuando com instalação..." -ForegroundColor Yellow
        }
        
        Write-Host "🔨 Compilando e instalando app..." -ForegroundColor Cyan
        .\gradlew.bat installDebug
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ App instalado com sucesso no dispositivo conectado!" -ForegroundColor Green
        } else {
            Write-Host "❌ Erro ao instalar app. Verifique os logs acima." -ForegroundColor Red
            exit 1
        }
    }
    elseif ($REMOTE -eq $BASE) {
        Write-Host "⚠️  Você tem commits locais não enviados. Faça push primeiro." -ForegroundColor Yellow
        Write-Host "💡 Dica: Execute .\scripts\sync-all-changes.ps1 para sincronizar tudo." -ForegroundColor Cyan
        exit 1
    }
    else {
        Write-Host "⚠️  Divergência detectada. Faça merge manualmente." -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ Erro inesperado: $_" -ForegroundColor Red
    Write-Host "💡 Tentando instalação direta..." -ForegroundColor Yellow
    
    .\gradlew.bat installDebug
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ App instalado com sucesso!" -ForegroundColor Green
    } else {
        Write-Host "❌ Erro ao instalar app." -ForegroundColor Red
        exit 1
    }
}
