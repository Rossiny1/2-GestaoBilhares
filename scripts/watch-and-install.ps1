# Script PowerShell para monitorar mudanças remotas e instalar automaticamente
# Uso: .\scripts\watch-and-install.ps1
# Este script roda em loop verificando mudanças a cada 30 segundos

Write-Host "👀 Monitorando mudanças remotas (Ctrl+C para parar)..." -ForegroundColor Cyan
Write-Host "⏱️  Verificando a cada 30 segundos..." -ForegroundColor Yellow
Write-Host ""

$LAST_COMMIT = ""

while ($true) {
    try {
        # Buscar mudanças remotas silenciosamente
        git fetch origin 2>$null | Out-Null
        
        $CURRENT_COMMIT = git rev-parse origin/HEAD 2>$null
        
        if ($CURRENT_COMMIT -and $CURRENT_COMMIT -ne $LAST_COMMIT) {
            Write-Host ""
            Write-Host "🔄 Mudanças detectadas! Último commit: $CURRENT_COMMIT" -ForegroundColor Yellow
            Write-Host "📥 Fazendo pull..." -ForegroundColor Cyan
            
            git pull origin
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
