# backup-rapido.ps1 - Script de backup seguro para o projeto completo

# Caminho do diretório de backup (ajuste se necessário)
$backupDir = "C:\Users\Rossiny\Desktop\2-GestaoBilhares\2-GestaoBilhares\backups"
$dataHora = Get-Date -Format "yyyyMMdd-HHmmss"
$mensagem = $args -join " "
if ([string]::IsNullOrWhiteSpace($mensagem)) {
    $mensagem = "Backup sem mensagem"
}

# Cria o diretório de backup se não existir
if (!(Test-Path $backupDir)) {
    New-Item -ItemType Directory -Path $backupDir | Out-Null
}

# Define o nome do arquivo de backup
$backupFile = "$backupDir\backup-$dataHora.zip"

# Faz o backup do projeto completo, exceto a própria pasta de backups
try {
    $exclude = @("backups", "backups/*")
    $items = Get-ChildItem -Path . -Recurse -File | Where-Object { $_.FullName -notmatch "backups" }
    Compress-Archive -Path $items.FullName -DestinationPath $backupFile -Force
    # Salva a mensagem de backup
    $logFile = "$backupDir\backup-log.txt"
    $logMsg = "$dataHora | $mensagem | Arquivo: $backupFile"
    Add-Content -Path $logFile -Value $logMsg
    Write-Host "Backup do projeto completo finalizado com sucesso!" -ForegroundColor Green
    Write-Host "Arquivo gerado: $backupFile"
    Write-Host "Mensagem: $mensagem"
} catch {
    Write-Host "ERRO: Falha ao executar backup. Detalhes:" -ForegroundColor Red
    Write-Host $_.Exception.Message
} 