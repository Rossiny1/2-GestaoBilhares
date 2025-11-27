# Script completo para reinstalar o app com ViewBinding atualizado
$ErrorActionPreference = "Continue"
$adb = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$packageName = "com.example.gestaobilhares"

Write-Host "=== REINSTALACAO COMPLETA DO APP ===" -ForegroundColor Cyan
Write-Host ""

# 1. Verificar dispositivo conectado
Write-Host "[1/6] Verificando dispositivo..." -ForegroundColor Yellow
$deviceCheck = & $adb devices 2>&1 | Select-String "device$"
if ($deviceCheck) {
    Write-Host "   Dispositivo conectado!" -ForegroundColor Green
} else {
    Write-Host "   ERRO: Nenhum dispositivo conectado!" -ForegroundColor Red
    Write-Host "   Conecte o celular via USB e ative a depuracao USB." -ForegroundColor Yellow
    exit 1
}
Write-Host ""

# 2. Desinstalar app antigo
Write-Host "[2/6] Desinstalando app antigo..." -ForegroundColor Yellow
& $adb uninstall $packageName 2>&1 | Out-Null
if ($LASTEXITCODE -eq 0) {
    Write-Host "   App desinstalado com sucesso!" -ForegroundColor Green
} else {
    Write-Host "   App nao estava instalado ou ja foi removido." -ForegroundColor Yellow
}
Write-Host ""

# 3. Limpar cache do ViewBinding
Write-Host "[3/6] Limpando cache do ViewBinding..." -ForegroundColor Yellow
Remove-Item -Path ".\app\build\generated\data_binding_base_class_source_out" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path ".\ui\build\generated\data_binding_base_class_source_out" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path ".\app\build\intermediates" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path ".\ui\build\intermediates" -Recurse -Force -ErrorAction SilentlyContinue
Write-Host "   Cache limpo!" -ForegroundColor Green
Write-Host ""

# 4. Limpar e compilar projeto
Write-Host "[4/6] Limpando projeto..." -ForegroundColor Yellow
& .\gradlew.bat clean --no-daemon 2>&1 | Out-Null
Write-Host "   Clean concluido!" -ForegroundColor Green
Write-Host ""

Write-Host "[5/6] Compilando novo APK (isso pode demorar 2-3 minutos)..." -ForegroundColor Yellow
Write-Host "   Aguarde..." -ForegroundColor Cyan
$buildStart = Get-Date
& .\gradlew.bat :app:assembleDebug --no-daemon 2>&1 | Out-Null
$buildEnd = Get-Date
$buildDuration = ($buildEnd - $buildStart).TotalSeconds

if ($LASTEXITCODE -eq 0) {
    Write-Host "   Build concluido em $([math]::Round($buildDuration, 1)) segundos!" -ForegroundColor Green
} else {
    Write-Host "   ERRO: Build falhou!" -ForegroundColor Red
    Write-Host "   Execute manualmente: .\gradlew.bat :app:assembleDebug" -ForegroundColor Yellow
    exit 1
}
Write-Host ""

# 5. Instalar novo APK
Write-Host "[6/6] Instalando novo APK no celular..." -ForegroundColor Yellow
$apkPath = ".\app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apkPath) {
    $apkInfo = Get-Item $apkPath
    Write-Host "   APK: $($apkInfo.Name) ($([math]::Round($apkInfo.Length / 1MB, 2)) MB)" -ForegroundColor Cyan
    
    & $adb install -r $apkPath 2>&1 | Out-Null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "   App instalado com sucesso!" -ForegroundColor Green
    } else {
        Write-Host "   ERRO: Falha na instalacao!" -ForegroundColor Red
        Write-Host "   Tente instalar manualmente: $apkPath" -ForegroundColor Yellow
        exit 1
    }
} else {
    Write-Host "   ERRO: APK nao encontrado!" -ForegroundColor Red
    exit 1
}
Write-Host ""

Write-Host "=== SUCESSO ===" -ForegroundColor Green
Write-Host "O app foi reinstalado com o ViewBinding atualizado." -ForegroundColor White
Write-Host "Agora voce pode testar clicando em um cliente na tela Rotas." -ForegroundColor White
Write-Host ""

