# 💾 SCRIPT DE BACKUP RÁPIDO - GestaoBilhares
# Autor: AI Assistant
# Uso: .\backup-rapido.ps1 -mensagem "Descrição do estado"

param(
    [string]$mensagem = "Estado atual salvamento automático"
)

Write-Host "💾 BACKUP RÁPIDO INICIANDO..." -ForegroundColor Green
Write-Host "📅 Data/Hora: $(Get-Date)" -ForegroundColor Cyan

# Verificar se estamos em um repositório git
if (-not (Test-Path ".git")) {
    Write-Host "❌ ERRO: Não é um repositório Git!" -ForegroundColor Red
    exit 1
}

# Verificar status do git
Write-Host "🔍 Verificando mudanças..." -ForegroundColor Yellow
$status = git status --porcelain
if (-not $status) {
    Write-Host "✅ Nenhuma mudança para backup." -ForegroundColor Green
    exit 0
}

Write-Host "📁 Mudanças encontradas:" -ForegroundColor Blue
git status --short

# Adicionar todas as mudanças
Write-Host "➕ Adicionando arquivos..." -ForegroundColor Yellow
git add .

# Verificar se houve erro
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ ERRO ao adicionar arquivos!" -ForegroundColor Red
    exit 1
}

# Criar timestamp
$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"

# Criar mensagem de commit estruturada
$commitMessage = @"
💾 BACKUP COMMIT - $mensagem

📅 Timestamp: $timestamp
🔧 Ambiente: $(hostname)
👤 Desenvolvedor: Rossiny

📋 Status: Backup automático de segurança
"@

# Fazer o commit
Write-Host "💾 Criando commit de backup..." -ForegroundColor Yellow
git commit -m $commitMessage

# Verificar se commit foi bem-sucedido
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ BACKUP CRIADO COM SUCESSO!" -ForegroundColor Green
    
    # Mostrar hash do commit
    $commitHash = git rev-parse --short HEAD
    Write-Host "🔑 Hash do commit: $commitHash" -ForegroundColor Cyan
    
    # Mostrar estatísticas
    Write-Host "📊 Estatísticas do backup:" -ForegroundColor Blue
    git show --stat --oneline HEAD
    
    # Sugerir push (opcional)
    Write-Host "💡 Dica: Execute 'git push' para enviar para o repositório remoto" -ForegroundColor Yellow
    
} else {
    Write-Host "❌ ERRO ao criar commit de backup!" -ForegroundColor Red
    exit 1
}

Write-Host "🏁 Backup finalizado com sucesso!" -ForegroundColor Green 