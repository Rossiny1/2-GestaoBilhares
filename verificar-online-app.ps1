# Verificar funcionamento online do app GestaoBilhares
# Autor: Assistente IA
# Data: 2025-10-24

Write-Host "VERIFICAÇÃO ONLINE - GestaoBilhares" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan

# 1) Resolver ADB (mesmo caminho do script funcional)
$ADB = "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $ADB)) { $ADB = "adb" }
try {
    $null = & $ADB version 2>$null
    if ($LASTEXITCODE -ne 0) { throw "ADB não encontrado" }
    Write-Host "ADB: $ADB" -ForegroundColor Green
} catch {
    Write-Host "Erro: $_" -ForegroundColor Red
    exit 1
}

# 2) Dispositivo
$state = (& $ADB get-state 2>$null).Trim()
if ($state -ne "device") {
    Write-Host "Nenhum dispositivo/emulador em 'device'" -ForegroundColor Red
    & $ADB devices
    exit 1
}
Write-Host "Dispositivo OK" -ForegroundColor Green

# 3) App instalado
$pkg = "com.example.gestaobilhares"
$installed = & $ADB shell pm list packages $pkg
if ($installed -notmatch $pkg) {
    Write-Host "App não instalado: $pkg" -ForegroundColor Red
    exit 1
}
Write-Host "App instalado" -ForegroundColor Green

# 4) Conectividade do dispositivo
Write-Host "Verificando conectividade do dispositivo..." -ForegroundColor Yellow
$conn = & $ADB shell dumpsys connectivity 2>$null
$validated = ($conn | Select-String -Pattern "VALIDATED|Validated|NET_CAPABILITIES_VALIDATED" -SimpleMatch)
if ($validated) {
    Write-Host "Rede VALIDATED: OK" -ForegroundColor Green
} else {
    # Fallback com ping (pode não existir em alguns dispositivos)
    $pingOk = $false
    try {
        $ping = & $ADB shell ping -c 1 8.8.8.8 2>$null
        if ($LASTEXITCODE -eq 0 -or ($ping -match "1 (packets )?received")) { $pingOk = $true }
    } catch {}
    if ($pingOk) { Write-Host "Ping OK" -ForegroundColor Green } else { Write-Host "Rede NÃO validada (pode estar offline)" -ForegroundColor Red }
}

# 5) Abrir o app
Write-Host "Abrindo o app..." -ForegroundColor Yellow
& $ADB shell monkey -p $pkg -c android.intent.category.LAUNCHER 1 > $null 2>&1
Start-Sleep -Seconds 2

# 6) Capturar logs curtos para checar Firebase/Auth/Sync
& $ADB logcat -c 2>$null
$outFile = "online-check-$(Get-Date -Format 'yyyyMMdd-HHmmss').txt"
Write-Host "Coletando logs por 10s..." -ForegroundColor Yellow
$job = Start-Job -ScriptBlock {
    param($adb,$file)
    & $adb logcat -v time -s "FirebaseApp:V" "FirebaseAuth:V" "SyncManagerV2:V" "$using:pkg:V" "*:S" | Out-File -FilePath $file -Encoding UTF8
} -ArgumentList $ADB,$outFile
Start-Sleep -Seconds 10
Stop-Job $job; Remove-Job $job

# 7) Analisar
if (Test-Path $outFile) {
    $log = Get-Content $outFile -Raw
    $firebaseInit = ($log | Select-String -Pattern "Firebase inicializado|FirebaseApp|Initialized" -SimpleMatch)
    $authOk = ($log | Select-String -Pattern "currentUser|signed in|login|autenticado" -SimpleMatch)
    $authWarn = ($log | Select-String -Pattern "não autenticado|not authenticated|No user" -SimpleMatch)
    $syncOps = ($log | Select-String -Pattern "Processando .* operações|Firestore Path|SET executado|DELETE executado" )

    Write-Host "Resultados:" -ForegroundColor Cyan
    Write-Host (" - FirebaseApp: " + ($(if ($firebaseInit) {"OK"} else {"NÃO DETECTADO"}))) -ForegroundColor $(if ($firebaseInit) {"Green"} else {"Red"})
    if ($authOk) { Write-Host " - Auth: OK (usuário detectado nos logs)" -ForegroundColor Green }
    elseif ($authWarn) { Write-Host " - Auth: NÃO autenticado" -ForegroundColor Red }
    else { Write-Host " - Auth: Indeterminado (sem logs)" -ForegroundColor Yellow }
    if ($syncOps) { Write-Host " - Sync: eventos detectados" -ForegroundColor Green } else { Write-Host " - Sync: sem eventos nos 10s" -ForegroundColor Yellow }

    Write-Host "Arquivo de logs: $outFile" -ForegroundColor Gray
} else {
    Write-Host "Falha ao capturar logs" -ForegroundColor Red
}

Write-Host "Concluído." -ForegroundColor Green


