# Crash5 - Diagnóstico focado em sincronização de contratos (PULL)
# Baseado no crash3/crash4, compatível com PowerShell 5

param(
    [string]$AdbPath = ""
)

function Resolve-Adb {
    if (-not [string]::IsNullOrWhiteSpace($AdbPath) -and (Test-Path $AdbPath)) { return $AdbPath }
    $cmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($null -ne $cmd) {
        $src = $cmd | Select-Object -ExpandProperty Source
        if (-not [string]::IsNullOrWhiteSpace($src)) { return $src }
    }
    $local = Join-Path $PSScriptRoot "platform-tools\adb.exe"
    if (Test-Path $local) { return $local }
    $envSdk = $env:ANDROID_HOME; if (-not $envSdk) { $envSdk = $env:ANDROID_SDK_ROOT }
    if ($envSdk) {
        $pt = Join-Path $envSdk "platform-tools\adb.exe"
        if (Test-Path $pt) { return $pt }
    }
    throw "ADB não encontrado. Instale o platform-tools e ajuste o PATH."
}

function Require-Device($adb) {
    & $adb start-server | Out-Null
    $out = & $adb devices
    if (-not ($out -match "\tdevice")) {
        Write-Host "Nenhum dispositivo online. Conecte e autorize Depuração USB." -ForegroundColor Yellow
        throw "Sem device"
    }
}

try {
    $adb = Resolve-Adb
    Require-Device $adb
    & $adb logcat -c | Out-Null

    Write-Host "Exibindo logs em tempo real (contratos e fotos)" -ForegroundColor Cyan
    Write-Host "Reproduza: APP vazio → Sincronizar → Abrir Gerenciar Contratos" -ForegroundColor Cyan
    Write-Host "OU: Criar acerto com foto → Sincronizar → Verificar logs de upload" -ForegroundColor Cyan

    # Filtros focados em contratos e fotos
    # Tags: CONTRACT_PULL (nova), SyncManagerV2, AppRepository, ContractManagementViewModel, ContratoLocacaoDao, AndroidRuntime, System.err
    # Tags adicionais para fotos: FirebaseStorageManager, SettlementViewModel, SETTLEMENT, Timber
    $filters = @(
        "AndroidRuntime:E",
        "System.err:W",
        "CONTRACT_PULL:V",
        "SyncManagerV2:V",
        "AppRepository:V",
        "ContractManagementViewModel:V",
        "ContratoLocacaoDao:V",
        "Firestore:V",
        "FirebaseStorageManager:V",
        "SettlementViewModel:V",
        "SETTLEMENT:V",
        "Timber:V"
    )

    $args = @("logcat", "-v", "time", "-s") + $filters

    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $adb
    $psi.Arguments = ($args -join ' ')
    $psi.RedirectStandardOutput = $true
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true

    $p = New-Object System.Diagnostics.Process
    $p.StartInfo = $psi
    [void]$p.Start()

    try {
        while (-not $p.HasExited) {
            $line = $p.StandardOutput.ReadLine()
            if ($null -ne $line) { Write-Host $line }
        }
    } finally {
        if (-not $p.HasExited) { $p.Kill() }
    }
}
catch {
    Write-Host "ERRO: $_" -ForegroundColor Red
}

