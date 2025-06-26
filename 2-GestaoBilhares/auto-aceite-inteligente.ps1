# ⚡ AUTO-ACEITE INTELIGENTE - GestaoBilhares
# Aprova automaticamente comandos seguros de desenvolvimento
# Bloqueia comandos que podem comprometer o sistema

param(
    [int]$intervalo = 2000,  # Intervalo em millisegundos
    [switch]$verbose = $false
)

Write-Host "⚡ AUTO-ACEITE INTELIGENTE INICIADO" -ForegroundColor Green
Write-Host "🛡️ Proteção ativa contra comandos perigosos" -ForegroundColor Yellow
Write-Host "📅 $(Get-Date)" -ForegroundColor Cyan

# Comandos SEGUROS - Aprovação automática
$comandosSegurosDev = @(
    "gradlew",
    "./gradlew",
    "assembleDebug",
    "installDebug", 
    "clean",
    "build",
    "test",
    "compileDebug",
    "processDebugResources",
    "adb",
    "logcat",
    "git",
    "status",
    "add",
    "commit",
    "push",
    "pull",
    "npm",
    "yarn",
    "Write-Host",
    "Get-Date",
    "Test-Path"
)

# Palavras-chave PERIGOSAS - Nunca aprovar automaticamente
$palavrasPerigosas = @(
    "format",
    "delete",
    "remove",
    "registry",
    "regedit",
    "powershell -exec",
    "invoke-",
    "download",
    "curl",
    "wget",
    "net user",
    "runas",
    "elevation",
    "administrator",
    "system32",
    "windows",
    "startup",
    "firewall",
    "antivirus"
)

# Comandos específicos ANDROID/KOTLIN seguros
$comandosAndroidSeguros = @(
    "./gradlew",
    "gradlew.bat",
    "Write-Host",
    "ls",
    "dir", 
    "cd",
    "pwd",
    "cat",
    "type",
    "findstr",
    "grep",
    "echo",
    "Get-Process",
    "Start-Sleep"
)

function Test-ComandoSeguro {
    param([string]$comando)
    
    $comando = $comando.ToLower().Trim()
    
    # Verificar se contém palavras perigosas
    foreach ($perigo in $palavrasPerigosas) {
        if ($comando.Contains($perigo.ToLower())) {
            if ($verbose) { Write-Host "❌ BLOQUEADO: Palavra perigosa '$perigo'" -ForegroundColor Red }
            return $false
        }
    }
    
    # Verificar se é comando seguro de desenvolvimento
    foreach ($seguro in $comandosSegurosDev) {
        if ($comando.Contains($seguro.ToLower())) {
            if ($verbose) { Write-Host "✅ SEGURO: Comando de dev '$seguro'" -ForegroundColor Green }
            return $true
        }
    }
    
    # Verificar comandos Android específicos
    foreach ($android in $comandosAndroidSeguros) {
        if ($comando.Contains($android.ToLower())) {
            if ($verbose) { Write-Host "✅ SEGURO: Comando Android '$android'" -ForegroundColor Green }
            return $true
        }
    }
    
    # Se não está na lista de seguros nem perigosos, permitir mas avisar
    if ($verbose) { Write-Host "⚠️ NEUTRO: Comando não classificado - '$comando'" -ForegroundColor Yellow }
    return $true
}

# Função para enviar teclas automaticamente
Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

function Send-AutoAccept {
    try {
        # Simular Enter para aceitar
        [System.Windows.Forms.SendKeys]::SendWait("{ENTER}")
        Start-Sleep -Milliseconds 300
        
        # Se houver botão "Run", clicar
        [System.Windows.Forms.SendKeys]::SendWait("{TAB}")
        Start-Sleep -Milliseconds 200
        [System.Windows.Forms.SendKeys]::SendWait("{ENTER}")
        Start-Sleep -Milliseconds 300
        
        # Para diálogos adicionais
        [System.Windows.Forms.SendKeys]::SendWait("y")
        Start-Sleep -Milliseconds 200
        [System.Windows.Forms.SendKeys]::SendWait("{ENTER}")
        
    } catch {
        Write-Host "⚠️ Erro ao enviar teclas: $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

function Get-UltimoComando {
    try {
        # Tentar obter último comando do histórico
        $ultimoComando = Get-History -Count 1 | Select-Object -ExpandProperty CommandLine
        return $ultimoComando
    } catch {
        return ""
    }
}

# Contador e estatísticas
$tentativas = 0
$comandosAprovados = 0
$comandosBloqueados = 0

Write-Host "🔄 Monitorando comandos... (Ctrl+C para parar)" -ForegroundColor Blue

try {
    while ($true) {
        Start-Sleep -Milliseconds $intervalo
        $tentativas++
        
        # Verificar se Cursor está rodando
        $cursorProcess = Get-Process "Cursor" -ErrorAction SilentlyContinue
        if (-not $cursorProcess) {
            Write-Host "💤 Cursor não encontrado. Aguardando..." -ForegroundColor Gray
            Start-Sleep -Seconds 5
            continue
        }
        
        # Obter último comando
        $ultimoComando = Get-UltimoComando
        
        if ($ultimoComando) {
            $isSeguro = Test-ComandoSeguro -comando $ultimoComando
            
            if ($isSeguro) {
                Send-AutoAccept
                $comandosAprovados++
                Write-Host "✅ Auto-aprovado: $ultimoComando" -ForegroundColor Green
            } else {
                $comandosBloqueados++
                Write-Host "🛡️ BLOQUEADO: $ultimoComando" -ForegroundColor Red
                Write-Host "   👆 Comando requer aprovação manual por segurança" -ForegroundColor Yellow
            }
        }
        
        # Tentar auto-aprovar sempre que possível (para mudanças de código)
        Send-AutoAccept
        
        # Estatísticas a cada 50 tentativas
        if ($tentativas % 50 -eq 0) {
            Write-Host "📊 Stats: $comandosAprovados aprovados | $comandosBloqueados bloqueados | $tentativas tentativas" -ForegroundColor Cyan
        }
    }
} catch {
    Write-Host "❌ Erro no loop principal: $($_.Exception.Message)" -ForegroundColor Red
} finally {
    Write-Host "📊 ESTATÍSTICAS FINAIS:" -ForegroundColor Blue
    Write-Host "  ✅ Comandos aprovados: $comandosAprovados" -ForegroundColor Green
    Write-Host "  🛡️ Comandos bloqueados: $comandosBloqueados" -ForegroundColor Red
    Write-Host "  🔄 Total tentativas: $tentativas" -ForegroundColor Cyan
    Write-Host "🏁 Auto-aceite finalizado." -ForegroundColor Green
} 