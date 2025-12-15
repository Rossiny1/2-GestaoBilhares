# ========================================
# SCRIPT DE INSTALAÇÃO APK VIA ADB
# GestaoBilhares - Instalação Automática
# ========================================

param(
    [switch]$Build,
    [switch]$Clean,
    [switch]$Force
)

# Configurações
$ADB_PATH = "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$APK_PATH = "app\build\outputs\apk\debug\app-debug.apk"
$PACKAGE_NAME = "com.example.gestaobilhares"

# Cores para output
$Green = "Green"
$Red = "Red"
$Yellow = "Yellow"
$Cyan = "Cyan"

function Write-ColorOutput {
    param(
        [string]$Message,
        [string]$Color = "White"
    )
    Write-Host $Message -ForegroundColor $Color
}

function Test-ADB {
    if (Test-Path $ADB_PATH) {
        Write-ColorOutput "SUCCESS: ADB encontrado em: $ADB_PATH" $Green
        return $true
    } else {
        Write-ColorOutput "ERROR: ADB nao encontrado em: $ADB_PATH" $Red
        Write-ColorOutput "Verifique se o Android SDK esta instalado corretamente." $Yellow
        return $false
    }
}

function Test-Device {
    Write-ColorOutput "Verificando dispositivos conectados..." $Cyan
    $devices = & $ADB_PATH devices
    $connected = $devices | Where-Object { $_ -match "device$" }
    
    if ($connected.Count -eq 0) {
        Write-ColorOutput "ERROR: Nenhum dispositivo Android conectado!" $Red
        Write-ColorOutput "Conecte um dispositivo via USB e habilite a Depuracao USB." $Yellow
        return $false
    } else {
        Write-ColorOutput "SUCCESS: Dispositivo(s) conectado(s):" $Green
        $connected | ForEach-Object { Write-ColorOutput "  $_" $Cyan }
        return $true
    }
}

function Build-APK {
    if ($Build -or $Force) {
        Write-ColorOutput "Construindo APK..." $Cyan
        
        if ($Clean) {
            Write-ColorOutput "Limpando projeto..." $Yellow
            & .\gradlew clean
        }
        
        Write-ColorOutput "Gerando APK..." $Yellow
        $result = & .\gradlew assembleDebug --no-daemon
        
        if ($LASTEXITCODE -eq 0) {
            Write-ColorOutput "SUCCESS: APK gerado com sucesso!" $Green
            return $true
        } else {
            Write-ColorOutput "ERROR: Erro ao gerar APK!" $Red
            return $false
        }
    }
    return $true
}

function Test-APK {
    if (Test-Path $APK_PATH) {
        $apkInfo = Get-Item $APK_PATH
        $sizeMB = [math]::Round($apkInfo.Length / 1MB, 2)
        Write-ColorOutput "SUCCESS: APK encontrado: $APK_PATH" $Green
        Write-ColorOutput "  Tamanho: $sizeMB MB" $Cyan
        Write-ColorOutput "  Data: $($apkInfo.LastWriteTime)" $Cyan
        return $true
    } else {
        Write-ColorOutput "ERROR: APK nao encontrado: $APK_PATH" $Red
        return $false
    }
}

function Uninstall-App {
    Write-ColorOutput "Desinstalando versao anterior..." $Yellow
    & $ADB_PATH uninstall $PACKAGE_NAME 2>$null
    Write-ColorOutput "SUCCESS: Aplicacao desinstalada (se existia)" $Green
}

function Install-APK {
    Write-ColorOutput "Instalando APK..." $Cyan
    Write-ColorOutput "APK: $APK_PATH" $Yellow
    
    $result = & $ADB_PATH install -r $APK_PATH
    
    if ($LASTEXITCODE -eq 0) {
        Write-ColorOutput "SUCCESS: APK instalado com sucesso!" $Green
        return $true
    } else {
        Write-ColorOutput "ERROR: Erro ao instalar APK!" $Red
        Write-ColorOutput "Resultado: $result" $Red
        return $false
    }
}

function Launch-App {
    Write-ColorOutput "Iniciando aplicacao..." $Cyan
    & $ADB_PATH shell am start -n "$PACKAGE_NAME/.MainActivity"
    
    if ($LASTEXITCODE -eq 0) {
        Write-ColorOutput "SUCCESS: Aplicacao iniciada!" $Green
    } else {
        Write-ColorOutput "WARNING: Aplicacao pode nao ter iniciado automaticamente" $Yellow
        Write-ColorOutput "Abra manualmente o app no dispositivo" $Yellow
    }
}

function Show-APK-Info {
    Write-ColorOutput "`nINFORMACOES DO APK" $Cyan
    Write-ColorOutput "================================" $Cyan
    
    if (Test-Path $APK_PATH) {
        $apkInfo = Get-Item $APK_PATH
        $sizeMB = [math]::Round($apkInfo.Length / 1MB, 2)
        
        Write-ColorOutput "Nome: GestaoBilhares" $White
        Write-ColorOutput "Pacote: $PACKAGE_NAME" $White
        Write-ColorOutput "Tamanho: $sizeMB MB" $White
        Write-ColorOutput "Data: $($apkInfo.LastWriteTime)" $White
        Write-ColorOutput "Caminho: $APK_PATH" $White
    }
}

# ========================================
# EXECUCAO PRINCIPAL
# ========================================

Write-ColorOutput "INSTALADOR APK - GESTAO BILHARES" $Cyan
Write-ColorOutput "========================================" $Cyan

# Verificar ADB
if (-not (Test-ADB)) {
    exit 1
}

# Verificar dispositivo
if (-not (Test-Device)) {
    exit 1
}

# Construir APK se solicitado
if (-not (Build-APK)) {
    exit 1
}

# Verificar se APK existe
if (-not (Test-APK)) {
    Write-ColorOutput "Execute o script com -Build para gerar o APK primeiro." $Yellow
    exit 1
}

# Desinstalar versao anterior
Uninstall-App

# Instalar APK
if (Install-APK) {
    # Iniciar aplicacao
    Launch-App
    
    # Mostrar informacoes
    Show-APK-Info
    
    Write-ColorOutput "`nSUCCESS: INSTALACAO CONCLUIDA COM SUCESSO!" $Green
    Write-ColorOutput "========================================" $Green
    Write-ColorOutput "O app GestaoBilhares esta pronto para uso!" $Green
} else {
    Write-ColorOutput "`nERROR: FALHA NA INSTALACAO!" $Red
    Write-ColorOutput "========================================" $Red
    exit 1
}

Write-ColorOutput "`nCOMANDOS UTEIS:" $Cyan
Write-ColorOutput "adb logcat -s GestaoBilhares    # Ver logs do app" $White
Write-ColorOutput "adb uninstall $PACKAGE_NAME      # Desinstalar app" $White
Write-ColorOutput "adb shell am start -n $PACKAGE_NAME/.MainActivity  # Abrir app" $White 