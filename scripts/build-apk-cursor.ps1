# Script: build-apk-cursor.ps1
# Descrição: Gera o APK debug do projeto Android mostrando progresso e logs completos no terminal Cursor
# Uso: Execute no terminal do Cursor com: ./build-apk-cursor.ps1

Write-Host "Iniciando build do APK (assembleDebug)..." -ForegroundColor Cyan

# Limpa build antigo
./gradlew clean --console=plain

# Gera o APK debug com logs e progresso
./gradlew assembleDebug --console=plain --stacktrace --warning-mode all

if ($LASTEXITCODE -eq 0) {
    Write-Host "\nBuild finalizado com sucesso! APK gerado em app/build/outputs/apk/debug/app-debug.apk" -ForegroundColor Green
} else {
    Write-Host "\nBuild falhou. Verifique os erros acima." -ForegroundColor Red
} 