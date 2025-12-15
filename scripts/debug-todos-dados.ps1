#!/usr/bin/env pwsh
# Script abrangente para debug de TODOS os dados sendo perdidos
# Identifica campos em branco: observações, representante, tipo acerto, métodos pagamento, etc.

Write-Host "=== DEBUG ABRANGENTE - TODOS OS DADOS ===" -ForegroundColor Red
Write-Host "🔍 Capturando logs de TODOS os campos que podem estar sendo perdidos:" -ForegroundColor Yellow
Write-Host "- Observações" -ForegroundColor Cyan
Write-Host "- Representante/Colaborador" -ForegroundColor Cyan  
Write-Host "- Tipo de Acerto" -ForegroundColor Cyan
Write-Host "- Pano Trocado" -ForegroundColor Cyan
Write-Host "- Métodos de Pagamento" -ForegroundColor Cyan
Write-Host "- Telefone/Endereço Cliente" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Red

# Filtrar logs de TODOS os campos problemáticos
adb logcat -v time | Select-String -Pattern "(observação|observacao|representante|colaborador|tipoAcerto|panoTrocado|numeroPano|metodosPagamento|telefone|endereco|SALVANDO ACERTO|ClientDetailViewModel|SettlementViewModel)" 