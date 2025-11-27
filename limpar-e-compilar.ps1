# Script para limpar cache do ViewBinding e recompilar o projeto
Write-Host "=== LIMPEZA E RECOMPILACAO ===" -ForegroundColor Cyan
Write-Host ""

# 1. Remover cache do ViewBinding
Write-Host "[1/4] Removendo cache do ViewBinding..." -ForegroundColor Yellow
Remove-Item -Path ".\app\build\generated\data_binding_base_class_source_out" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path ".\ui\build\generated\data_binding_base_class_source_out" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path ".\app\build\intermediates\data_binding_layout_info_type_merge" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path ".\ui\build\intermediates\data_binding_layout_info_type_merge" -Recurse -Force -ErrorAction SilentlyContinue
Write-Host "   Cache removido com sucesso!" -ForegroundColor Green
Write-Host ""

# 2. Limpar projeto
Write-Host "[2/4] Executando gradle clean..." -ForegroundColor Yellow
& .\gradlew.bat clean --no-daemon 2>&1 | Out-Null
Write-Host "   Clean concluido!" -ForegroundColor Green
Write-Host ""

# 3. Compilar projeto
Write-Host "[3/4] Compilando projeto (pode demorar)..." -ForegroundColor Yellow
$buildOutput = & .\gradlew.bat :app:assembleDebug --no-daemon 2>&1
$buildSuccess = $LASTEXITCODE -eq 0

if ($buildSuccess) {
    Write-Host "   Build concluido com sucesso!" -ForegroundColor Green
} else {
    Write-Host "   Build falhou!" -ForegroundColor Red
    Write-Host ""
    Write-Host "=== ERRO NO BUILD ===" -ForegroundColor Red
    $buildOutput | Select-Object -Last 30
    exit 1
}
Write-Host ""

# 4. Verificar APK
Write-Host "[4/4] Verificando APK..." -ForegroundColor Yellow
$apkPath = ".\app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apkPath) {
    $apkInfo = Get-Item $apkPath
    Write-Host "   APK gerado: $($apkInfo.Name)" -ForegroundColor Green
    Write-Host "   Tamanho: $([math]::Round($apkInfo.Length / 1MB, 2)) MB" -ForegroundColor Green
    Write-Host "   Local: $apkPath" -ForegroundColor Cyan
} else {
    Write-Host "   APK nao encontrado!" -ForegroundColor Red
    exit 1
}
Write-Host ""

Write-Host "=== SUCESSO ===" -ForegroundColor Green
Write-Host "Agora voce pode instalar o APK no celular." -ForegroundColor White
Write-Host ""

