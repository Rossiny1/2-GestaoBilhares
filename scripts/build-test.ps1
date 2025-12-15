# ========================================
# SCRIPT DE BUILD TESTE - GESTAO BILHARES
# Testa o build após correções de sintaxe
# ========================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "BUILD TESTE - GESTAO BILHARES" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Iniciando build de teste..." -ForegroundColor Green
Write-Host ""

# Executar build
try {
    & .\gradlew assembleDebug --no-daemon
    Write-Host ""
    Write-Host "✅ BUILD CONCLUÍDO COM SUCESSO!" -ForegroundColor Green
} catch {
    Write-Host ""
    Write-Host "❌ BUILD FALHOU: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "Build de teste finalizado" -ForegroundColor Gray
Read-Host "Pressione Enter para sair"
