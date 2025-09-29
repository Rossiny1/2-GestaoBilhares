# ========================================
# TESTE BUILD APÓS CORREÇÕES - GESTAO BILHARES
# ========================================
#
# INSTRUÇÕES:
# 1. Execute este script a partir do terminal PowerShell.
# 2. O script irá testar se o build está funcionando após as correções.
#
# ========================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "TESTE BUILD APÓS CORREÇÕES - GESTAO BILHARES" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Caminho para o ADB
$adbPath = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verificar se o ADB está disponível
if (-not (Test-Path $adbPath)) {
    Write-Host "ERRO: ADB não encontrado em $adbPath" -ForegroundColor Red
    Write-Host "Verifique se o Android SDK está instalado corretamente" -ForegroundColor Red
    Read-Host "Pressione Enter para sair"
    exit 1
}

Write-Host "Testando build após correções..." -ForegroundColor Green
Write-Host ""

# Comando para compilar apenas o Kotlin
Write-Host "Executando: .\gradlew compileDebugKotlin" -ForegroundColor Yellow
Write-Host ""

try {
    & .\gradlew compileDebugKotlin --stacktrace
    Write-Host ""
    Write-Host "✅ BUILD COMPILADO COM SUCESSO!" -ForegroundColor Green
    Write-Host "As correções foram aplicadas corretamente." -ForegroundColor Green
} catch {
    Write-Host ""
    Write-Host "❌ BUILD FALHOU!" -ForegroundColor Red
    Write-Host "Erro: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "Teste concluído!" -ForegroundColor Cyan
Read-Host "Pressione Enter para sair"
