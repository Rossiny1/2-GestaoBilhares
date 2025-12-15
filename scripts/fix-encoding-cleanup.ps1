# Script para corrigir problemas de encoding e cache
Write-Host "🧹 LIMPEZA COMPLETA DO PROJETO" -ForegroundColor Yellow

# 1. Parar todos os daemons
Write-Host "1. Parando daemons..." -ForegroundColor Cyan
./gradlew --stop 2>$null
taskkill /f /im java.exe 2>$null

# 2. Limpar cache do Gradle
Write-Host "2. Limpando cache do Gradle..." -ForegroundColor Cyan
Remove-Item -Recurse -Force .gradle -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force build -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force app/build -ErrorAction SilentlyContinue

# 3. Limpar cache do Kotlin
Write-Host "3. Limpando cache do Kotlin..." -ForegroundColor Cyan
Remove-Item -Recurse -Force app/build/kotlin -ErrorAction SilentlyContinue

# 4. Limpar cache do Android Studio
Write-Host "4. Limpando cache do Android Studio..." -ForegroundColor Cyan
Remove-Item -Recurse -Force .idea -ErrorAction SilentlyContinue

Write-Host "✅ LIMPEZA CONCLUÍDA!" -ForegroundColor Green
Write-Host "Agora execute: ./gradlew assembleDebug --no-daemon" -ForegroundColor Yellow
