# Script para salvar o projeto localmente e na nuvem
# Autor: Agente AI (Antigravity)
# Data: 17/12/2025

Clear-Host
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "   SINCRONIZADOR DE PROJETO - GESTAO BILHARES" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host ""

# 1. Verificar Branch
$branch = git branch --show-current
if (-not $branch) {
    Write-Host "ERRO: Nenhuma branch detectada. Verifique o git." -ForegroundColor Red
    Pause
    exit
}
Write-Host "Conectado na branch: " -NoNewline
Write-Host "$branch" -ForegroundColor Yellow
Write-Host ""

# 2. Verificar Status
$status = git status -s
if (-not $status) {
    Write-Host "Nenhuma alteracao pendente para salvar." -ForegroundColor Green
    Write-Host "O projeto ja esta sincronizado!" -ForegroundColor Green
    Write-Host ""
    Read-Host "Pressione ENTER para sair..."
    exit
}

Write-Host "Arquivos alterados:" -ForegroundColor Gray
git status -s
Write-Host ""

# 3. Solicitar mensagem (obrigatório)
do {
    $mensagem = Read-Host "Escreva o que foi alterado (Ex: Corrigi o login)"
    if ([string]::IsNullOrWhiteSpace($mensagem)) {
        Write-Host "Por favor, escreva uma descricao para salvar." -ForegroundColor Red
    }
} while ([string]::IsNullOrWhiteSpace($mensagem))

Write-Host ""
Write-Host "----------------------------------------------" -ForegroundColor Gray

# 4. Executar comandos
try {
    Write-Host "1/3 Preparando arquivos..." -ForegroundColor Cyan
    git add .
    
    Write-Host "2/3 Salvando no computador..." -ForegroundColor Cyan
    git commit -m "$mensagem"
    
    Write-Host "3/3 Enviando para nuvem (GitHub)..." -ForegroundColor Cyan
    git push origin $branch
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "✅ SUCESSO! PROJETO SALVO E ENVIADO." -ForegroundColor Green
        Write-Host "Agora voce pode acessar de outro computador." -ForegroundColor Green
    } else {
        Write-Host ""
        Write-Host "⚠️  ATENCAO: O envio para nuvem pode ter falhado." -ForegroundColor Yellow
        Write-Host "Verifique as mensagens de erro acima." -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ OCORREU UM ERRO INESPERADO: $_" -ForegroundColor Red
}

Write-Host ""
Read-Host "Pressione ENTER para fechar..."
