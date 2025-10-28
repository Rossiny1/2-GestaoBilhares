# Script focado para capturar crash ao selecionar PANO (manutenção e acerto)
# Baseado no crash3.ps1

$ErrorActionPreference = "Stop"

# Caminho fixo do ADB (usar o mesmo padrão do projeto)
$adbPath = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe"

function Assert-Adb {
	if (-not (Test-Path $adbPath)) {
		Write-Host "ADB não encontrado em: $adbPath" -ForegroundColor Red
		Write-Host "Verifique o Android SDK ou ajuste o caminho no script." -ForegroundColor Yellow
		exit 1
	}
}

function Assert-Device {
	$devices = & $adbPath devices | Select-String "\tdevice$"
	if (-not $devices) {
		Write-Host "Nenhum dispositivo/emulador conectado (adb devices)." -ForegroundColor Red
		Write-Host "Conecte um dispositivo com Depuração USB ativa e tente novamente." -ForegroundColor Yellow
		exit 1
	}
}

# Execução
Assert-Adb
Assert-Device

# Preparar arquivo de saída
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$outFile = "logcat-crash-pano-focused-$timestamp.txt"

Write-Host "=== CAPTURA FOCADA DE CRASH - PANO ===" -ForegroundColor Cyan
Write-Host "Arquivo de saída: $outFile" -ForegroundColor Gray
Write-Host "1) Vá para Manutenção de Mesa e clique em 'Selecionar Panos'" -ForegroundColor White
Write-Host "2) Vá para Acerto e clique em 'Trocar Pano'" -ForegroundColor White
Write-Host "3) Reproduza o crash. Pressione Ctrl+C para encerrar a captura." -ForegroundColor Yellow

# Limpar logs anteriores
& $adbPath logcat -c | Out-Null

# Marcar início nos logs
& $adbPath shell log -t PanoSelectionDialog "[PANO] CAPTURA INICIADA ($timestamp)"

# Filtros por TAG (somente os relevantes) + AndroidRuntime para stacktrace
# Observação: usar -s silencia outras TAGs (*:S) mantendo só as listadas
$tags = @(
	"AndroidRuntime:E",
	"System.err:W",
	"PanoSelectionDialog:V",
	"SettlementFragment:V",
	"NovaReformaFragment:V",
	"PanoEstoqueRepository:V",
	"AppRepository:V",
	"StockViewModel:V",
	"StockFragment:V"
)

# Iniciar captura (até Ctrl+C)
& $adbPath logcat -v time -s $tags | Tee-Object -FilePath $outFile
