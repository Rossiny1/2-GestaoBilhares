# Script para Commit Automático - Gestão Bilhares
# Autor: Assistente IA
# Data: 2025-01-06
# Descrição: Automatiza o processo de commit com abordagem que funciona

Write-Host "INICIANDO COMMIT AUTOMATICO..." -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Cyan

# 1. Verificar status atual
Write-Host "Verificando status do Git..." -ForegroundColor Yellow
git status --porcelain

# 2. Adicionar todos os arquivos
Write-Host "Adicionando arquivos ao staging..." -ForegroundColor Yellow
git add .

# 3. Verificar se há arquivos para commitar
$status = git status --porcelain
if ([string]::IsNullOrEmpty($status)) {
    Write-Host "Nenhum arquivo para commitar!" -ForegroundColor Red
    exit 1
}

# 4. Gerar mensagem de commit baseada nos arquivos modificados
Write-Host "Gerando mensagem de commit..." -ForegroundColor Yellow

$commitMessage = "feat: Atualização automática - $(Get-Date -Format 'yyyy-MM-dd HH:mm')"

# Verificar se há arquivos específicos para mensagens personalizadas
if ($status -match "colaborador") {
    $commitMessage = "feat: Sistema de gerenciamento de colaboradores implementado"
} elseif ($status -match "build") {
    $commitMessage = "fix: Correções de build e erros de compilação"
} elseif ($status -match "crash") {
    $commitMessage = "fix: Correção de crashes e estabilização do app"
} elseif ($status -match "database") {
    $commitMessage = "fix: Correções no banco de dados e migrações"
}

# 5. Realizar o commit
Write-Host "Realizando commit..." -ForegroundColor Yellow
Write-Host "Mensagem: $commitMessage" -ForegroundColor Cyan

git commit -m $commitMessage

# 6. Verificar se o commit foi bem-sucedido
if ($LASTEXITCODE -eq 0) {
    Write-Host "COMMIT REALIZADO COM SUCESSO!" -ForegroundColor Green
    Write-Host "=====================================" -ForegroundColor Cyan
    
    # Mostrar último commit
    Write-Host "Ultimo commit:" -ForegroundColor Yellow
    git log -1 --oneline
    
} else {
    Write-Host "ERRO NO COMMIT!" -ForegroundColor Red
    Write-Host "Verifique se há conflitos ou problemas no Git" -ForegroundColor Red
    exit 1
}

Write-Host "Processo finalizado!" -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Cyan
