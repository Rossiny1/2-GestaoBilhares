# Script para verificar erros de compilação
Write-Host "Verificando erros de compilação..." -ForegroundColor Yellow

# Parar processos Java que podem estar bloqueando
Write-Host "Parando processos Java..." -ForegroundColor Cyan
Get-Process | Where-Object {$_.ProcessName -eq 'java'} | Stop-Process -Force -ErrorAction SilentlyContinue

# Limpar cache do Gradle
Write-Host "Limpando cache do Gradle..." -ForegroundColor Cyan
./gradlew --stop
./gradlew clean

# Tentar compilar e capturar erros
Write-Host "Compilando projeto..." -ForegroundColor Cyan
try {
    $output = ./gradlew compileDebugKotlin --stacktrace 2>&1
    $errorCount = ($output | Select-String "error:").Count
    $warningCount = ($output | Select-String "warning:").Count
    
    Write-Host "=== RESUMO DE ERROS ===" -ForegroundColor Red
    Write-Host "Erros encontrados: $errorCount" -ForegroundColor Red
    Write-Host "Warnings encontrados: $warningCount" -ForegroundColor Yellow
    
    if ($errorCount -gt 0) {
        Write-Host "`n=== PRIMEIROS 10 ERROS ===" -ForegroundColor Red
        $output | Select-String "error:" | Select-Object -First 10 | ForEach-Object {
            Write-Host $_.Line -ForegroundColor Red
        }
    }
    
} catch {
    Write-Host "Erro ao executar compilação: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`nScript concluído!" -ForegroundColor Green