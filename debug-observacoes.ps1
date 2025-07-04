#!/usr/bin/env pwsh
# Script específico para debug das observações em branco
# Criado para rastrear o problema das observações não aparecendo

Write-Host "=== CAPTURANDO LOGS DAS OBSERVAÇÕES ===" -ForegroundColor Cyan
Write-Host "🔍 Filtrando logs específicos de observações..." -ForegroundColor Yellow
Write-Host "Execute um acerto no app e observe os logs:" -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Cyan

# Filtrar apenas logs relacionados às observações
adb logcat -v time | Select-String -Pattern "(OBSERVAÇÕES|observação|observacao|tvObservacaoAcerto|etObservacao|Observação)" 