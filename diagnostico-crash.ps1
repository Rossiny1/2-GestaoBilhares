# ========================================
# DIAGNOSTICO DE CRASH - GESTAO BILHARES
# Script principal para analise de problemas
# ========================================

param(
    [string]$PackageName = "com.example.gestaobilhares"
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "DIAGNOSTICO DE CRASH" -ForegroundColor Cyan
Write-Host "GESTAO BILHARES - ANDROID APP" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Menu de opcoes
Write-Host "Escolha uma opcao:" -ForegroundColor White
Write-Host "1. Capturar logs de crash (recomendado)" -ForegroundColor Yellow
Write-Host "2. Analisar logs existentes" -ForegroundColor Yellow
Write-Host "3. Diagnostico completo (capturar + analisar)" -ForegroundColor Yellow
Write-Host ""

$opcao = Read-Host "Digite sua opcao (1-3)"

switch ($opcao) {
    "1" { 
        Write-Host "Executando captura de logs..." -ForegroundColor Green
        & .\capturar-logs-simples.ps1 -PackageName $PackageName
    }
    "2" { 
        Write-Host "Executando analise de logs..." -ForegroundColor Green
        & .\analisar-logs-simples.ps1 -PackageName $PackageName
    }
    "3" { 
        Write-Host "Executando diagnostico completo..." -ForegroundColor Green
        
        # Capturar logs
        Write-Host ""
        Write-Host "PASSO 1: CAPTURANDO LOGS..." -ForegroundColor Cyan
        & .\capturar-logs-simples.ps1 -PackageName $PackageName
        
        # Analisar logs
        Write-Host ""
        Write-Host "PASSO 2: ANALISANDO LOGS..." -ForegroundColor Cyan
        & .\analisar-logs-simples.ps1 -PackageName $PackageName
    }
    default { 
        Write-Host "Opcao invalida" -ForegroundColor Red
        exit 1
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
