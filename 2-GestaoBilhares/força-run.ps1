# 🚀 FORÇA RUN - Múltiplas estratégias para executar comandos no Cursor
# Solução robusta para auto-aprovação

Write-Host "🚀 FORÇA RUN ATIVADO - Múltiplas estratégias" -ForegroundColor Green

# Importar bibliotecas
Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

function Force-CursorRun {
    param([int]$tentativas = 5)
    
    Write-Host "🎯 Executando $tentativas estratégias..." -ForegroundColor Yellow
    
    # Estratégia 1: Ctrl+Enter (executar comando)
    Write-Host "  📤 1. Ctrl+Enter..." -ForegroundColor Cyan
    [System.Windows.Forms.SendKeys]::SendWait("^{ENTER}")
    Start-Sleep -Milliseconds 300
    
    # Estratégia 2: Enter simples
    Write-Host "  📤 2. Enter..." -ForegroundColor Cyan
    [System.Windows.Forms.SendKeys]::SendWait("{ENTER}")
    Start-Sleep -Milliseconds 300
    
    # Estratégia 3: Tab até botão + Enter
    Write-Host "  📤 3. Tab+Enter..." -ForegroundColor Cyan
    [System.Windows.Forms.SendKeys]::SendWait("{TAB}{TAB}{ENTER}")
    Start-Sleep -Milliseconds 300
    
    # Estratégia 4: Alt+R (atalho Run)
    Write-Host "  📤 4. Alt+R..." -ForegroundColor Cyan
    [System.Windows.Forms.SendKeys]::SendWait("%r")
    Start-Sleep -Milliseconds 300
    
    # Estratégia 5: Espaço (botão focado)
    Write-Host "  📤 5. Espaço..." -ForegroundColor Cyan
    [System.Windows.Forms.SendKeys]::SendWait(" ")
    Start-Sleep -Milliseconds 300
    
    Write-Host "✅ Todas as estratégias executadas!" -ForegroundColor Green
}

# Loop principal
$contador = 0
try {
    while ($true) {
        $contador++
        
        Write-Host "`n🔄 Ciclo $contador - $(Get-Date -Format 'HH:mm:ss')" -ForegroundColor Blue
        
        # Executar estratégias de força
        Force-CursorRun
        
        # Aguardar próximo ciclo
        Write-Host "⏳ Aguardando 2 segundos..." -ForegroundColor Gray
        Start-Sleep -Seconds 2
        
        # Feedback a cada 10 ciclos
        if ($contador % 10 -eq 0) {
            Write-Host "📊 Completados $contador ciclos de força" -ForegroundColor Magenta
        }
    }
    
} catch [System.Management.Automation.HaltCommandException] {
    Write-Host "`n🛑 Interrompido pelo usuário após $contador ciclos" -ForegroundColor Yellow
} catch {
    Write-Host "`n❌ Erro: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "🏁 Força Run finalizado." -ForegroundColor Green 