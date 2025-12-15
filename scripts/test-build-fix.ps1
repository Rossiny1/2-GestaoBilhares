Write-Host "Testando build após correções..." -ForegroundColor Green

# Parar daemons
Write-Host "Parando daemons..." -ForegroundColor Yellow
./gradlew --stop

# Limpar cache
Write-Host "Limpando cache..." -ForegroundColor Yellow
./gradlew clean --no-daemon

# Tentar build
Write-Host "Executando build..." -ForegroundColor Yellow
./gradlew assembleDebug --no-daemon

Write-Host "Build concluído!" -ForegroundColor Green