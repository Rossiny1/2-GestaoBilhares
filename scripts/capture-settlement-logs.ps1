# Script para capturar logs do histórico de acertos
# CRIADO: 2025-01-06 20:55
# OBJETIVO: Diagnosticar problema de exibição do histórico

Write-Host "=== CAPTURANDO LOGS DO HISTORICO DE ACERTOS ===" -ForegroundColor Cyan

# Limpar logs anteriores
Write-Host "Limpando logs anteriores..." -ForegroundColor Yellow
adb logcat -c

# Capturar logs específicos do histórico
Write-Host "Capturando logs do histórico de acertos..." -ForegroundColor Green
Write-Host "Pressione Ctrl+C para parar a captura" -ForegroundColor Red

adb logcat | Select-String -Pattern "SettlementHistoryAdapter|ClientDetailFragment|ClientDetailViewModel|rvSettlementHistory|getItemCount|onBindViewHolder|onCreateViewHolder" 