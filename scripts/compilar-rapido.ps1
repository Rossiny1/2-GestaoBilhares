# Compilacao rapida apos correcao do layout correto
$adb = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$packageName = "com.example.gestaobilhares"

Write-Host "=== COMPILACAO RAPIDA ===" -ForegroundColor Cyan
Write-Host ""

Write-Host "[1/4] Limpando cache do modulo UI..." -ForegroundColor Yellow
Remove-Item -Path ".\ui\build\intermediates" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path ".\ui\build\generated" -Recurse -Force -ErrorAction SilentlyContinue
Write-Host "   Cache limpo!" -ForegroundColor Green
Write-Host ""

Write-Host "[2/4] Compilando projeto..." -ForegroundColor Yellow
& .\gradlew.bat :app:assembleDebug --no-daemon 2>&1 | Out-Null
if ($LASTEXITCODE -eq 0) {
    Write-Host "   Build concluido!" -ForegroundColor Green
} else {
    Write-Host "   ERRO no build!" -ForegroundColor Red
    exit 1
}
Write-Host ""

Write-Host "[3/4] Desinstalando app antigo..." -ForegroundColor Yellow
& $adb uninstall $packageName 2>&1 | Out-Null
Write-Host "   App desinstalado!" -ForegroundColor Green
Write-Host ""

Write-Host "[4/4] Instalando novo APK..." -ForegroundColor Yellow
$apkPath = ".\app\build\outputs\apk\debug\app-debug.apk"
& $adb install $apkPath 2>&1 | Out-Null
if ($LASTEXITCODE -eq 0) {
    Write-Host "   App instalado com sucesso!" -ForegroundColor Green
} else {
    Write-Host "   ERRO na instalacao!" -ForegroundColor Red
    exit 1
}
Write-Host ""

Write-Host "=== CONCLUIDO ===" -ForegroundColor Green
Write-Host "Agora o arquivo correto foi compilado. Teste no app!" -ForegroundColor White
Write-Host ""

