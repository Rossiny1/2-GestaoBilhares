# ========================================
# VISUALIZADOR APK NO CURSOR - GESTAO BILHARES
# ========================================
# Script para visualizar telas do APK diretamente no Cursor
# Opções: Emulador, Dispositivo Físico, Screenshots, Layout Inspector

param(
    [switch]$Emulator,
    [switch]$Device,
    [switch]$Screenshot,
    [switch]$LayoutInspector,
    [switch]$Build,
    [switch]$Install
)

# Configurações
$ADB_PATH = "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$EMULATOR_PATH = "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\emulator\emulator.exe"
$APK_PATH = "app\build\outputs\apk\debug\app-debug.apk"
$PACKAGE_NAME = "com.example.gestaobilhares"
$ACTIVITY_NAME = "com.example.gestaobilhares.MainActivity"

# Cores para output
$Green = "Green"
$Red = "Red"
$Yellow = "Yellow"
$Cyan = "Cyan"
$Magenta = "Magenta"

function Write-ColorOutput {
    param([string]$Message, [string]$Color = "White")
    Write-Host $Message -ForegroundColor $Color
}

function Test-Prerequisites {
    Write-ColorOutput "Verificando pré-requisitos..." $Cyan
    
    # Verificar ADB
    if (Test-Path $ADB_PATH) {
        Write-ColorOutput "✅ ADB encontrado" $Green
    } else {
        Write-ColorOutput "❌ ADB não encontrado em: $ADB_PATH" $Red
        return $false
    }
    
    # Verificar Emulador
    if (Test-Path $EMULATOR_PATH) {
        Write-ColorOutput "✅ Emulador Android encontrado" $Green
    } else {
        Write-ColorOutput "❌ Emulador não encontrado em: $EMULATOR_PATH" $Red
        return $false
    }
    
    return $true
}

function Start-Emulator {
    Write-ColorOutput "Iniciando emulador Android..." $Cyan
    
    # Listar AVDs disponíveis
    $avds = & $EMULATOR_PATH -list-avds
    if ($avds.Count -eq 0) {
        Write-ColorOutput "❌ Nenhum AVD (Android Virtual Device) encontrado!" $Red
        Write-ColorOutput "Crie um AVD no Android Studio: Tools > AVD Manager" $Yellow
        return $false
    }
    
    Write-ColorOutput "AVDs disponíveis:" $Yellow
    $avds | ForEach-Object { Write-ColorOutput "  - $_" $Cyan }
    
    # Usar o primeiro AVD disponível
    $selectedAvd = $avds[0]
    Write-ColorOutput "Iniciando AVD: $selectedAvd" $Yellow
    
    # Iniciar emulador em background
    Start-Process -FilePath $EMULATOR_PATH -ArgumentList "-avd", $selectedAvd -WindowStyle Minimized
    
    # Aguardar emulador inicializar
    Write-ColorOutput "Aguardando emulador inicializar (30s)..." $Yellow
    Start-Sleep -Seconds 30
    
    # Verificar se emulador está rodando
    $devices = & $ADB_PATH devices
    if ($devices -match "emulator") {
        Write-ColorOutput "✅ Emulador iniciado com sucesso!" $Green
        return $true
    } else {
        Write-ColorOutput "❌ Emulador não foi detectado" $Red
        return $false
    }
}

function Test-Device {
    Write-ColorOutput "Verificando dispositivos conectados..." $Cyan
    $devices = & $ADB_PATH devices
    $connected = $devices | Where-Object { $_ -match "device$" }
    
    if ($connected.Count -eq 0) {
        Write-ColorOutput "❌ Nenhum dispositivo conectado!" $Red
        Write-ColorOutput "Conecte um dispositivo via USB e habilite Depuração USB" $Yellow
        return $false
    } else {
        Write-ColorOutput "✅ Dispositivo(s) conectado(s):" $Green
        $connected | ForEach-Object { Write-ColorOutput "  $_" $Cyan }
        return $true
    }
}

function Build-APK {
    if ($Build) {
        Write-ColorOutput "Construindo APK..." $Cyan
        
        # Clean build
        Write-ColorOutput "Limpando projeto..." $Yellow
        & .\gradlew clean
        
        # Build APK
        Write-ColorOutput "Gerando APK..." $Yellow
        $result = & .\gradlew assembleDebug --no-daemon
        
        if ($LASTEXITCODE -eq 0) {
            Write-ColorOutput "✅ APK gerado com sucesso!" $Green
            return $true
        } else {
            Write-ColorOutput "❌ Erro ao gerar APK!" $Red
            return $false
        }
    }
    return $true
}

function Install-APK {
    if ($Install -or $Build) {
        Write-ColorOutput "Instalando APK..." $Cyan
        
        # Verificar se APK existe
        if (-not (Test-Path $APK_PATH)) {
            Write-ColorOutput "❌ APK não encontrado: $APK_PATH" $Red
            return $false
        }
        
        # Desinstalar versão anterior
        Write-ColorOutput "Desinstalando versão anterior..." $Yellow
        & $ADB_PATH uninstall $PACKAGE_NAME 2>$null
        
        # Instalar nova versão
        $result = & $ADB_PATH install -r $APK_PATH
        
        if ($LASTEXITCODE -eq 0) {
            Write-ColorOutput "✅ APK instalado com sucesso!" $Green
        } else {
            Write-ColorOutput "❌ Erro ao instalar APK!" $Red
            return $false
        }
    }
    return $true
}

function Launch-App {
    Write-ColorOutput "Iniciando aplicação..." $Cyan
    & $ADB_PATH shell am start -n "$PACKAGE_NAME/$ACTIVITY_NAME"
    
    if ($LASTEXITCODE -eq 0) {
        Write-ColorOutput "✅ Aplicação iniciada!" $Green
    } else {
        Write-ColorOutput "⚠️ Aplicação pode não ter iniciado automaticamente" $Yellow
    }
}

function Capture-Screenshot {
    Write-ColorOutput "Capturando screenshot..." $Cyan
    
    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $screenshotPath = "screenshot-$timestamp.png"
    
    # Capturar screenshot
    & $ADB_PATH shell screencap -p /sdcard/screenshot.png
    & $ADB_PATH pull /sdcard/screenshot.png $screenshotPath
    
    if (Test-Path $screenshotPath) {
        Write-ColorOutput "✅ Screenshot salvo: $screenshotPath" $Green
        
        # Tentar abrir screenshot
        try {
            Start-Process $screenshotPath
            Write-ColorOutput "Screenshot aberto no visualizador padrão" $Cyan
        } catch {
            Write-ColorOutput "Screenshot salvo em: $screenshotPath" $Yellow
        }
    } else {
        Write-ColorOutput "❌ Erro ao capturar screenshot" $Red
    }
}

function Start-LayoutInspector {
    Write-ColorOutput "Iniciando Layout Inspector..." $Cyan
    Write-ColorOutput "O Layout Inspector será aberto no Android Studio" $Yellow
    
    # Comando para abrir Layout Inspector
    $layoutInspectorCmd = "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\tools\bin\uiautomatorviewer.bat"
    
    if (Test-Path $layoutInspectorCmd) {
        Start-Process $layoutInspectorCmd
        Write-ColorOutput "✅ Layout Inspector iniciado!" $Green
    } else {
        Write-ColorOutput "❌ Layout Inspector não encontrado" $Red
        Write-ColorOutput "Use: Android Studio > View > Tool Windows > Layout Inspector" $Yellow
    }
}

function Show-RealTimeLogs {
    Write-ColorOutput "Iniciando logs em tempo real..." $Cyan
    Write-ColorOutput "Pressione Ctrl+C para parar" $Yellow
    
    # Limpar logs anteriores
    & $ADB_PATH logcat -c
    
    # Capturar logs do app
    & $ADB_PATH logcat -s "GestaoBilhares:*" "AndroidRuntime:E" "FATAL:*"
}

function Show-APK-Info {
    Write-ColorOutput "`nINFORMACOES DO APK" $Magenta
    Write-ColorOutput "================================" $Magenta
    
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

function Show-Menu {
    Write-ColorOutput "`nOPCOES DE VISUALIZACAO" $Magenta
    Write-ColorOutput "================================" $Magenta
    Write-ColorOutput "1. Emulador Android" $Cyan
    Write-ColorOutput "2. Dispositivo Fisico" $Cyan
    Write-ColorOutput "3. Screenshot" $Cyan
    Write-ColorOutput "4. Layout Inspector" $Cyan
    Write-ColorOutput "5. Logs em Tempo Real" $Cyan
    Write-ColorOutput "6. Informacoes do APK" $Cyan
    Write-ColorOutput "7. Sair" $Cyan
}

# ========================================
# EXECUÇÃO PRINCIPAL
# ========================================

Write-ColorOutput "VISUALIZADOR APK NO CURSOR - GESTAO BILHARES" $Magenta
Write-ColorOutput "========================================" $Magenta

# Verificar pré-requisitos
if (-not (Test-Prerequisites)) {
    exit 1
}

# Se parâmetros específicos foram passados
if ($Emulator -or $Device -or $Screenshot -or $LayoutInspector) {
    
    # Construir APK se solicitado
    if (-not (Build-APK)) {
        exit 1
    }
    
    # Instalar APK se solicitado
    if (-not (Install-APK)) {
        exit 1
    }
    
    # Executar opção específica
    if ($Emulator) {
        if (Start-Emulator) {
            Launch-App
            Show-RealTimeLogs
        }
    }
    
    if ($Device) {
        if (Test-Device) {
            Launch-App
            Show-RealTimeLogs
        }
    }
    
    if ($Screenshot) {
        Capture-Screenshot
    }
    
    if ($LayoutInspector) {
        Start-LayoutInspector
    }
    
} else {
    # Modo interativo
    do {
        Show-Menu
        $choice = Read-Host "`nEscolha uma opção (1-7)"
        
        switch ($choice) {
            "1" {
                if (Start-Emulator) {
                    if (Build-APK -and Install-APK) {
                        Launch-App
                        Show-RealTimeLogs
                    }
                }
            }
            "2" {
                if (Test-Device) {
                    if (Build-APK -and Install-APK) {
                        Launch-App
                        Show-RealTimeLogs
                    }
                }
            }
            "3" { Capture-Screenshot }
            "4" { Start-LayoutInspector }
            "5" { Show-RealTimeLogs }
            "6" { Show-APK-Info }
            "7" { 
                Write-ColorOutput "Saindo..." $Green
                break 
            }
            default { Write-ColorOutput "Opção inválida!" $Red }
        }
        
        if ($choice -ne "7") {
            Read-Host "`nPressione Enter para continuar..."
        }
        
    } while ($choice -ne "7")
}

Write-ColorOutput "`nSUCCESS: Visualizador APK finalizado!" $Green
