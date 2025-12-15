# ========================================
# DIAGNOSTICO COMPLETO DE CRASH - GESTAO BILHARES
# Script principal para analise de problemas
# ========================================

param(
    [string]$PackageName = "com.example.gestaobilhares",
    [switch]$MonitorTempoReal = $false,
    [switch]$AnalisarLogs = $false,
    [string]$LogFile = ""
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "DIAGNOSTICO COMPLETO DE CRASH" -ForegroundColor Cyan
Write-Host "GESTAO BILHARES - ANDROID APP" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Menu de opcoes
if (-not $MonitorTempoReal -and -not $AnalisarLogs) {
    Write-Host "Escolha uma opcao:" -ForegroundColor White
    Write-Host "1. Capturar logs de crash (recomendado)" -ForegroundColor Yellow
    Write-Host "2. Monitor em tempo real" -ForegroundColor Yellow
    Write-Host "3. Analisar logs existentes" -ForegroundColor Yellow
    Write-Host "4. Diagnostico completo (todas as opcoes)" -ForegroundColor Yellow
    Write-Host ""
    
    $opcao = Read-Host "Digite sua opcao (1-4)"
    
    switch ($opcao) {
        "1" { 
            Write-Host "Executando captura de logs..." -ForegroundColor Green
            & .\capturar-logs-crash-otimizado.ps1 -PackageName $PackageName
        }
        "2" { 
            Write-Host "Executando monitor em tempo real..." -ForegroundColor Green
            & .\monitor-logs-tempo-real.ps1 -PackageName $PackageName
        }
        "3" { 
            Write-Host "Executando analise de logs..." -ForegroundColor Green
            & .\analisar-logs-crash.ps1 -LogFile $LogFile -PackageName $PackageName
        }
        "4" { 
            Write-Host "Executando diagnostico completo..." -ForegroundColor Green
            $MonitorTempoReal = $true
            $AnalisarLogs = $true
        }
        default { 
            Write-Host "Opcao invalida" -ForegroundColor Red
            exit 1
        }
    }
}

# Executar monitor em tempo real
if ($MonitorTempoReal) {
    Write-Host ""
    Write-Host "INICIANDO MONITOR EM TEMPO REAL..." -ForegroundColor Cyan
    Write-Host "   - Abra o app e reproduza o problema" -ForegroundColor White
    Write-Host "   - Pressione Ctrl+C para parar" -ForegroundColor White
    Write-Host ""
    
    try {
        & .\monitor-logs-tempo-real.ps1 -PackageName $PackageName
    } catch {
        Write-Host "Erro no monitor: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Executar analise de logs
if ($AnalisarLogs) {
    Write-Host ""
    Write-Host "ANALISANDO LOGS..." -ForegroundColor Cyan
    
    # Procurar arquivo de log mais recente
    if ([string]::IsNullOrEmpty($LogFile)) {
        $recentLogs = Get-ChildItem -Path "." -Filter "logcat-crash-*.txt" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
        if ($recentLogs) {
            $LogFile = $recentLogs.FullName
        }
    }
    
    if (-not [string]::IsNullOrEmpty($LogFile) -and (Test-Path $LogFile)) {
        try {
            & .\analisar-logs-crash.ps1 -LogFile $LogFile -PackageName $PackageName
        } catch {
            Write-Host "Erro na analise: $($_.Exception.Message)" -ForegroundColor Red
        }
    } else {
        Write-Host "Nenhum arquivo de log encontrado para analise" -ForegroundColor Yellow
        Write-Host "   Execute primeiro: .\capturar-logs-crash-otimizado.ps1" -ForegroundColor White
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "DIAGNOSTICO CONCLUIDO!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "PROXIMOS PASSOS:" -ForegroundColor White
Write-Host "1. Analise os logs capturados" -ForegroundColor White
Write-Host "2. Identifique a causa raiz do crash" -ForegroundColor White
Write-Host "3. Corrija o codigo baseado nos erros encontrados" -ForegroundColor White
Write-Host "4. Teste o app novamente" -ForegroundColor White
Write-Host "5. Repita o processo se necessario" -ForegroundColor White
