# Crash4 - Diagnóstico focado em sincronização de ciclos (PULL)
# Baseado no crash3, com filtros específicos para CYCLE_PULL e entidades de ciclo/rota

param(
    [string]$AdbPath = ""
)

function Resolve-Adb {
    # 1) Se foi passado pelo parâmetro
    if (-not [string]::IsNullOrWhiteSpace($AdbPath) -and (Test-Path $AdbPath)) { return $AdbPath }
    
    # 2) Usar o 'adb' do PATH (mesma estratégia do crash3)
    $cmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($null -ne $cmd) { return 'adb' }

    # 3) Fallback: platform-tools local
    $local = Join-Path $PSScriptRoot "platform-tools\adb.exe"
    if (Test-Path $local) { return $local }

    # 4) Fallback: ANDROID_HOME/ANDROID_SDK_ROOT
    $sdk = $env:ANDROID_HOME
    if ([string]::IsNullOrWhiteSpace($sdk)) { $sdk = $env:ANDROID_SDK_ROOT }
    if (-not [string]::IsNullOrWhiteSpace($sdk)) {
        $sdkAdb = Join-Path $sdk "platform-tools\adb.exe"
        if (Test-Path $sdkAdb) { return $sdkAdb }
    }

    throw "ADB não encontrado. Adicione ao PATH ou coloque platform-tools/adb.exe na pasta do projeto."
}

function Require-Device($adb) {
    & $adb start-server | Out-Null
    $out = & $adb devices
    if (-not ($out -match "\tdevice")) {
        Write-Host "Nenhum dispositivo online. Conecte e autorize via USB (Depuração USB)." -ForegroundColor Yellow
        Write-Host $out
        exit 1
    }
}

try {
    $adb = Resolve-Adb
    Require-Device $adb
    Write-Host "ADB: $adb" -ForegroundColor Cyan

    Write-Host "Limpando logcat..." -ForegroundColor Yellow
    & $adb logcat -c

    $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $file = Join-Path $PSScriptRoot "logcat-cycle-pull-$stamp.txt"
    Write-Host "Capturando logs para: $file" -ForegroundColor Green

    # Filtros focados em ciclos e alinhamento de rotas
    $filters = @(
        "AndroidRuntime:E",
        "System.err:W",
        "CYCLE_PULL:V",
        "SyncManagerV2:V",
        "AppRepository:V",
        "RoutesViewModel:V",
        "ClientListViewModel:V",
        "RoutesAdapter:V",
        "CicloAcertoDao:V",
        "RotaDao:V"
    )

    # Execução do logcat com -s (somente tags selecionadas) e salvando em arquivo
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

    $fs = New-Object System.IO.FileStream($file, [System.IO.FileMode]::Create, [System.IO.FileAccess]::Write, [System.IO.FileShare]::ReadWrite)
    $sw = New-Object System.IO.StreamWriter($fs)

    Write-Host "Iniciado. Reproduza: Sincronizar (PULL) em app vazio. Pressione Ctrl+C para encerrar." -ForegroundColor Cyan
    try {
        while (-not $p.HasExited) {
            $line = $p.StandardOutput.ReadLine()
            if ($null -ne $line) {
                Write-Host $line
                $sw.WriteLine($line)
                $sw.Flush()
            }
        }
    } finally {
        $sw.Dispose()
        $fs.Dispose()
        if (-not $p.HasExited) { $p.Kill() }
    }

} catch {
    Write-Error $_
    exit 1
}


