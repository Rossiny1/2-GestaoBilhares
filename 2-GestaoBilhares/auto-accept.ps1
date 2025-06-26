# 🚀 SCRIPT PARA ACEITE AUTOMÁTICO DE MUDANÇAS NO CURSOR
# Autor: AI Assistant para GestaoBilhares
# Uso: .\auto-accept.ps1

Write-Host "🚀 Iniciando aceite automático de mudanças..." -ForegroundColor Green

# Função para enviar teclas automaticamente
Add-Type -AssemblyName System.Windows.Forms

function Send-KeyStroke {
    param($key)
    [System.Windows.Forms.SendKeys]::SendWait($key)
}

function Accept-Changes {
    Write-Host "✅ Aceitando mudanças..." -ForegroundColor Yellow
    
    # Simular Ctrl+S (salvar)
    Send-KeyStroke "^s"
    Start-Sleep -Milliseconds 500
    
    # Simular Enter (aceitar)
    Send-KeyStroke "{ENTER}"
    Start-Sleep -Milliseconds 300
    
    # Simular Tab + Enter (se houver diálogo)
    Send-KeyStroke "{TAB}"
    Start-Sleep -Milliseconds 200
    Send-KeyStroke "{ENTER}"
}

# Loop principal
Write-Host "🔄 Monitorando por mudanças... (Pressione Ctrl+C para parar)" -ForegroundColor Cyan

$counter = 0
while ($true) {
    Start-Sleep -Seconds 2
    $counter++
    
    # A cada 5 segundos, tentar aceitar mudanças
    if ($counter % 3 -eq 0) {
        Accept-Changes
        Write-Host "⚡ Tentativa de aceite #$($counter/3)" -ForegroundColor Blue
    }
    
    # Verificar se o Cursor está ativo
    $cursorProcess = Get-Process "Cursor" -ErrorAction SilentlyContinue
    if (-not $cursorProcess) {
        Write-Host "❌ Cursor não encontrado. Encerrando..." -ForegroundColor Red
        break
    }
}

Write-Host "🏁 Script finalizado." -ForegroundColor Green 