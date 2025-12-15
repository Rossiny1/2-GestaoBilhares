# Caminho do ADB
$adbPath = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Nome do arquivo de log (com data/hora para não sobrescrever)
$logFile = "logcat-erros-$(Get-Date -Format 'yyyyMMdd-HHmmss').txt"

# Instruções para o usuário
Write-Host "Capturando apenas ERROS do logcat (nível E/)."
Write-Host "Reproduza o erro/crash no app e pressione CTRL+C para parar a captura."
Write-Host "O log será salvo em: $logFile"
Write-Host "Após capturar, analise o arquivo para identificar e corrigir os erros."

# Comando para capturar apenas erros (nível E/) do logcat
& $adbPath logcat *:E | Out-File -Encoding UTF8 $logFile 