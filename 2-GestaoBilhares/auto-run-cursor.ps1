# ⚡ AUTO-RUN CURSOR - Clica automaticamente no botão "Run"
# Solução específica para comandos gradle no Cursor

param(
    [int]$intervalo = 1000,
    [switch]$verbose = $false
)

Write-Host "⚡ AUTO-RUN CURSOR ATIVADO" -ForegroundColor Green
Write-Host "🎯 Detectando botão 'Run' automaticamente" -ForegroundColor Yellow

# Importar bibliotecas Windows
Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

# Função para encontrar janela do Cursor
Add-Type @"
    using System;
    using System.Runtime.InteropServices;
    using System.Text;
    
    public class WindowAPI {
        [DllImport("user32.dll")]
        public static extern IntPtr FindWindow(string lpClassName, string lpWindowName);
        
        [DllImport("user32.dll")]
        public static extern bool SetForegroundWindow(IntPtr hWnd);
        
        [DllImport("user32.dll")]
        public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
        
        [DllImport("user32.dll")]
        public static extern IntPtr GetForegroundWindow();
        
        [DllImport("user32.dll")]
        public static extern int GetWindowText(IntPtr hWnd, StringBuilder text, int count);
        
        [DllImport("user32.dll")]
        public static extern bool EnumWindows(EnumWindowsProc enumProc, IntPtr lParam);
        
        [DllImport("user32.dll")]
        public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint processId);
        
        public delegate bool EnumWindowsProc(IntPtr hWnd, IntPtr lParam);
    }
"@

function Get-CursorWindows {
    $cursorWindows = @()
    
    $processes = Get-Process "Cursor" -ErrorAction SilentlyContinue
    foreach ($process in $processes) {
        try {
            if ($process.MainWindowHandle -ne [IntPtr]::Zero) {
                $windowTitle = New-Object System.Text.StringBuilder 256
                [WindowAPI]::GetWindowText($process.MainWindowHandle, $windowTitle, 256)
                
                $cursorWindows += @{
                    Handle = $process.MainWindowHandle
                    Title = $windowTitle.ToString()
                    ProcessId = $process.Id
                }
            }
        } catch {
            # Ignorar erros
        }
    }
    
    return $cursorWindows
}

function Send-RunCommand {
    param([IntPtr]$windowHandle)
    
    try {
        # Focar na janela do Cursor
        [WindowAPI]::SetForegroundWindow($windowHandle) | Out-Null
        Start-Sleep -Milliseconds 200
        
        # Tentar múltiplas combinações para "Run"
        # Ctrl+Enter (comum para executar)
        [System.Windows.Forms.SendKeys]::SendWait("^{ENTER}")
        Start-Sleep -Milliseconds 100
        
        # Enter simples
        [System.Windows.Forms.SendKeys]::SendWait("{ENTER}")
        Start-Sleep -Milliseconds 100
        
        # Tab + Enter (navegar para botão Run)
        [System.Windows.Forms.SendKeys]::SendWait("{TAB}{ENTER}")
        Start-Sleep -Milliseconds 100
        
        # Alt+R (atalho para Run)
        [System.Windows.Forms.SendKeys]::SendWait("%r")
        Start-Sleep -Milliseconds 100
        
        # Tecla R (se botão Run estiver focado)
        [System.Windows.Forms.SendKeys]::SendWait("r")
        Start-Sleep -Milliseconds 100
        
        # Espaço (se botão estiver selecionado)
        [System.Windows.Forms.SendKeys]::SendWait(" ")
        Start-Sleep -Milliseconds 100
        
        if ($verbose) {
            Write-Host "✅ Comandos enviados para janela Cursor" -ForegroundColor Green
        }
        
        return $true
        
    } catch {
        if ($verbose) {
            Write-Host "❌ Erro ao enviar comandos: $($_.Exception.Message)" -ForegroundColor Red
        }
        return $false
    }
}

$tentativas = 0
$comandosEnviados = 0

Write-Host "🔄 Monitorando Cursor... (Ctrl+C para parar)" -ForegroundColor Blue
Write-Host "⚡ Intervalo: ${intervalo}ms" -ForegroundColor Cyan

try {
    while ($true) {
        Start-Sleep -Milliseconds $intervalo
        $tentativas++
        
        # Obter janelas do Cursor
        $cursorWindows = Get-CursorWindows
        
        if ($cursorWindows.Count -eq 0) {
            if ($verbose -and ($tentativas % 10 -eq 0)) {
                Write-Host "💤 Cursor não encontrado..." -ForegroundColor Gray
            }
            continue
        }
        
        # Enviar comandos para todas as janelas do Cursor
        foreach ($window in $cursorWindows) {
            $sucesso = Send-RunCommand -windowHandle $window.Handle
            if ($sucesso) {
                $comandosEnviados++
                if ($verbose) {
                    Write-Host "⚡ Comando enviado para: $($window.Title)" -ForegroundColor Green
                }
            }
        }
        
        # Estatísticas a cada 50 tentativas
        if ($tentativas % 50 -eq 0) {
            Write-Host "📊 Tentativas: $tentativas | Comandos enviados: $comandosEnviados" -ForegroundColor Cyan
        }
    }
    
} catch [System.Management.Automation.HaltCommandException] {
    Write-Host "`n🛑 Interrompido pelo usuário" -ForegroundColor Yellow
} catch {
    Write-Host "`n❌ Erro: $($_.Exception.Message)" -ForegroundColor Red
} finally {
    Write-Host "`n📊 ESTATÍSTICAS:" -ForegroundColor Blue
    Write-Host "  🔄 Tentativas: $tentativas" -ForegroundColor White
    Write-Host "  ⚡ Comandos enviados: $comandosEnviados" -ForegroundColor Green
    Write-Host "🏁 Auto-run finalizado." -ForegroundColor Green
} 