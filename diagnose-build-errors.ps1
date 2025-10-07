# DIAGNÓSTICO DE ERROS DE BUILD
Write-Host "=== DIAGNÓSTICO DE ERROS DE BUILD ===" -ForegroundColor Yellow

# 1. Verificar se há processos Java em execução
Write-Host "1. Verificando processos Java..." -ForegroundColor Cyan
Get-Process -Name "java" -ErrorAction SilentlyContinue | ForEach-Object {
    Write-Host "   Processo Java encontrado: PID $($_.Id)" -ForegroundColor Red
    Stop-Process -Id $_.Id -Force -ErrorAction SilentlyContinue
    Write-Host "   Processo Java finalizado" -ForegroundColor Green
}

# 2. Verificar se há daemons Gradle em execução
Write-Host "2. Verificando daemons Gradle..." -ForegroundColor Cyan
try {
    .\gradlew --stop
    Write-Host "   Daemons Gradle finalizados" -ForegroundColor Green
} catch {
    Write-Host "   Nenhum daemon Gradle encontrado" -ForegroundColor Yellow
}

# 3. Limpar cache e build
Write-Host "3. Limpando cache e build..." -ForegroundColor Cyan
try {
    .\gradlew clean
    Write-Host "   Cache limpo com sucesso" -ForegroundColor Green
} catch {
    Write-Host "   Erro ao limpar cache: $($_.Exception.Message)" -ForegroundColor Red
}

# 4. Verificar erros de compilação específicos
Write-Host "4. Verificando erros de compilação..." -ForegroundColor Cyan
try {
    .\gradlew compileDebugKotlin --continue --stacktrace
    Write-Host "   Compilação Kotlin concluída" -ForegroundColor Green
} catch {
    Write-Host "   Erros de compilação encontrados:" -ForegroundColor Red
    Write-Host "   $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "=== DIAGNÓSTICO CONCLUÍDO ===" -ForegroundColor Yellow
