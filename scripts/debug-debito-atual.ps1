#!/usr/bin/env pwsh
# Script para capturar logs específicos do cálculo de débito atual
# Criado para diagnosticar problema de débito não atualizando

Write-Host "=== CAPTURANDO LOGS DO DÉBITO ATUAL ===" -ForegroundColor Green
Write-Host "Aguardando logs específicos do cálculo de débito..." -ForegroundColor Yellow
Write-Host "Execute o acerto no app e observe os logs abaixo:" -ForegroundColor Yellow
Write-Host "=========================================" -ForegroundColor Green

# Filtrar logs específicos do SettlementFragment e cálculos
adb logcat -v time | Select-String -Pattern "(SettlementFragment|CÁLCULOS|updateCalculations|DÉBITO ATUAL|métodos de pagamento|Valor recebido|showPaymentValuesDialog)"

# Parar com Ctrl+C 