# Script PowerShell para monitorar mudanças remotas e instalar automaticamente
# Uso: .\scripts\watch-and-install.ps1
# Este script roda em loop verificando mudanças a cada 30 segundos

$ErrorActionPreference = "Continue"

Write-Host "👀 Monitorando mudanças remotas (Ctrl+C para parar)..." -ForegroundColor Cyan
Write-Host "⏱️  Verificando a cada 30 segundos..." -ForegroundColor Yellow
Write-Host ""

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

$LAST_COMMIT = ""

while ($true) {
    try {
        # Buscar mudanças remotas silenciosamente
        git fetch origin 2>$null | Out-Null
        
        $CURRENT_COMMIT = git rev-parse origin/HEAD 2>$null
        
        if ($LASTEXITCODE -ne 0) {
            # Tentar com branch atual
            $currentBranch = git branch --show-current 2>&1
            if (-not $currentBranch) {
                $currentBranch = git rev-parse --abbrev-ref HEAD 2>&1
            }
            if ($currentBranch -and $currentBranch -ne "HEAD") {
                $CURRENT_COMMIT = git rev-parse "origin/$currentBranch" 2>$null
            }
        }
        
        if ($CURRENT_COMMIT -and $CURRENT_COMMIT -ne $LAST_COMMIT) {
            Write-Host ""
            Write-Host "🔄 Mudanças detectadas! Último commit: $CURRENT_COMMIT" -ForegroundColor Yellow
            Write-Host "📥 Fazendo pull..." -ForegroundColor Cyan
            
            $currentBranch = git branch --show-current 2>&1
            if (-not $currentBranch) {
                $currentBranch = git rev-parse --abbrev-ref HEAD 2>&1
            }
            
            if ($currentBranch -and $currentBranch -ne "HEAD") {
                git pull origin $currentBranch 2>&1 | ForEach-Object { Write-Host $_ }
            } else {
                git pull origin 2>&1 | ForEach-Object { Write-Host $_ }
            }
            
            if ($LASTEXITCODE -ne 0) {
                Write-Host "❌ Erro ao fazer pull. Continuando monitoramento..." -ForegroundColor Red
                Start-Sleep -Seconds 30
                continue
            }
            
            Write-Host "🔨 Compilando e instalando app..." -ForegroundColor Cyan
            .\gradlew.bat installDebug
            
            if ($LASTEXITCODE -eq 0) {
                Write-Host "✅ App atualizado e instalado com sucesso!" -ForegroundColor Green
                $LAST_COMMIT = $CURRENT_COMMIT
            }
            else {
                Write-Host "❌ Erro ao instalar. Continuando monitoramento..." -ForegroundColor Red
            }
            
            Write-Host ""
        }
        
        Start-Sleep -Seconds 30
    }
    catch {
        Write-Host "❌ Erro: $_" -ForegroundColor Red
        Start-Sleep -Seconds 30
    }
}
