# Verificar sincronização de dados (envio e gravação no Firestore)
# Uso básico:
#   .\verificar-sync-dados.ps1 -ProjectId seu_projeto -Collection clientes -DocId 123 (opcional: -BearerToken eyJ...)
# Sem ProjectId/DocId, o script apenas confirma envio via logs (SET/DELETE) após clicar no botão de sincronização.

param(
    [string]$PackageName = "com.example.gestaobilhares",
    [string]$EmpresaId = "empresa_001",
    [string]$ProjectId = "",
    [string]$Collection = "",
    [string]$DocId = "",
    [string]$BearerToken = "",
    [int]$LogSeconds = 15
)

Write-Host "VERIFICAÇÃO DE SINCRONIZAÇÃO (envio e gravação)" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan

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

# 3) App instalado
$installed = & $ADB shell pm list packages $PackageName
if ($installed -notmatch $PackageName) {
    Write-Host "App não instalado: $PackageName" -ForegroundColor Red
    exit 1
}

# 4) Limpar logcat e instruções
& $ADB logcat -c 2>$null
Write-Host "Abra o app, garanta login e CLIQUE no botão de sincronização agora." -ForegroundColor Yellow
Write-Host ("Coletando logs por {0}s..." -f $LogSeconds) -ForegroundColor Yellow

# 5) Capturar logs filtrados (ver envio: SET/DELETE/Firestore Path)
$outFile = "sync-check-$(Get-Date -Format 'yyyyMMdd-HHmmss').txt"
$job = Start-Job -ScriptBlock {
    param($adb,$pkg,$file)
    & $adb logcat -v time -s "SyncManagerV2:V" "FirebaseAuth:V" "$pkg:V" "*:S" | Out-File -FilePath $file -Encoding UTF8
} -ArgumentList $ADB,$PackageName,$outFile
Start-Sleep -Seconds $LogSeconds
Stop-Job $job; Remove-Job $job

if (-not (Test-Path $outFile)) {
    Write-Host "Falha ao capturar logs" -ForegroundColor Red
    exit 1
}

$log = Get-Content $outFile -Raw
$setOk = ($log | Select-String -Pattern "SET executado|Firestore Path|Aplicando operação|CREATE|UPDATE" )
$delOk = ($log | Select-String -Pattern "DELETE executado|DELETE" )
$authWarn = ($log | Select-String -Pattern "não autenticado|not authenticated|No user" -SimpleMatch)

Write-Host "RESULTADOS (via logs):" -ForegroundColor Cyan
if ($setOk) { Write-Host " - Envio: DETECTADO (SET/Path)" -ForegroundColor Green } else { Write-Host " - Envio: NÃO detectado nos logs" -ForegroundColor Yellow }
if ($delOk) { Write-Host " - Deletes: DETECTADOS" -ForegroundColor Green }
if ($authWarn) { Write-Host " - Auth: usuário não autenticado" -ForegroundColor Red }
Write-Host (" - Arquivo de logs: {0}" -f $outFile) -ForegroundColor Gray

# 6) Verificar no Firestore (opcional, se ProjectId/Collection/DocId fornecidos)
if (-not [string]::IsNullOrWhiteSpace($ProjectId) -and -not [string]::IsNullOrWhiteSpace($Collection) -and -not [string]::IsNullOrWhiteSpace($DocId)) {
    $url = "https://firestore.googleapis.com/v1/projects/$ProjectId/databases/(default)/documents/empresas/$EmpresaId/$Collection/$DocId"
    Write-Host "Consultando Firestore: $url" -ForegroundColor Yellow
    try {
        $headers = @{}
        if (-not [string]::IsNullOrWhiteSpace($BearerToken)) { $headers["Authorization"] = "Bearer $BearerToken" }
        $resp = Invoke-RestMethod -Method GET -Uri $url -Headers $headers -ErrorAction Stop
        if ($resp -and $resp.name) {
            Write-Host "GRAVAÇÃO NO FIRESTORE: OK (documento encontrado)" -ForegroundColor Green
        } else {
            Write-Host "GRAVAÇÃO NO FIRESTORE: INDETERMINADA (resposta sem 'name')" -ForegroundColor Yellow
        }
    } catch {
        Write-Host ("Falha ao consultar Firestore: {0}" -f $_.Exception.Message) -ForegroundColor Red
        Write-Host "Possíveis causas: regras exigem auth, token inválido/ausente, ProjectId/paths incorretos." -ForegroundColor Red
    }
} else {
    Write-Host "Dica: Informe -ProjectId, -Collection e -DocId para verificar diretamente no Firestore." -ForegroundColor Gray
}

Write-Host "Concluído." -ForegroundColor Green


