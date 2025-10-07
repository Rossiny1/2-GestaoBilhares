# Script simples para testar build
Write-Host "Testando build do projeto..." -ForegroundColor Green

# Parar processos Java se existirem
try {
    taskkill /f /im java.exe 2>$null
    Write-Host "Processos Java finalizados" -ForegroundColor Yellow
} catch {
    Write-Host "Nenhum processo Java encontrado" -ForegroundColor Gray
}

# Limpar cache
Write-Host "Limpando cache..." -ForegroundColor Yellow
.\gradlew clean --no-daemon

# Build
Write-Host "Executando build..." -ForegroundColor Green
.\gradlew assembleDebug --no-daemon

Write-Host "Build concluído!" -ForegroundColor Green
