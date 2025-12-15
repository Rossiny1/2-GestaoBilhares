# Script para Commit Automático - Gestão Bilhares
# Autor: Assistente IA
# Data: 2025-01-06
# Descrição: Automatiza o processo de commit com abordagem que funciona

Write-Host "INICIANDO COMMIT AUTOMATICO..." -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Cyan

# 1. Verificar status atual ANTES de adicionar
Write-Host "Verificando status do Git..." -ForegroundColor Yellow
$statusAntes = git status --porcelain
if ([string]::IsNullOrWhiteSpace($statusAntes)) {
    Write-Host "Nenhuma mudanca detectada para commitar!" -ForegroundColor Yellow
    exit 0
}

# Mostrar resumo das mudancas
$modificados = ($statusAntes | Select-String -Pattern "^ M" | Measure-Object).Count
$deletados = ($statusAntes | Select-String -Pattern "^ D" | Measure-Object).Count
$novos = ($statusAntes | Select-String -Pattern "^A " | Measure-Object).Count
$renomeados = ($statusAntes | Select-String -Pattern "^R " | Measure-Object).Count
$untracked = (git status --porcelain | Select-String -Pattern "^??" | Measure-Object).Count

Write-Host "Resumo das mudancas:" -ForegroundColor Cyan
Write-Host "  Modificados: $modificados" -ForegroundColor White
Write-Host "  Deletados: $deletados" -ForegroundColor White
Write-Host "  Novos: $novos" -ForegroundColor White
Write-Host "  Renomeados: $renomeados" -ForegroundColor White
Write-Host "  Nao rastreados: $untracked" -ForegroundColor White

# 2. Limpar arquivos de build que podem causar problemas
Write-Host "Limpando arquivos de build do staging..." -ForegroundColor Yellow
git reset HEAD -- "**/build/" 2>$null
git reset HEAD -- "core/build/" 2>$null
git reset HEAD -- "ui/build/" 2>$null
git reset HEAD -- "data/build/" 2>$null
git reset HEAD -- "sync/build/" 2>$null

# 3. Adicionar arquivos modificados e novos (respeitando .gitignore)
Write-Host "Adicionando arquivos modificados (trackeados) ao staging..." -ForegroundColor Yellow
git add -u

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERRO] Falha ao adicionar arquivos modificados. Tentando novamente..." -ForegroundColor Red
    git add -u
}

Write-Host "Adicionando arquivos novos (untracked) ao staging..." -ForegroundColor Yellow
git add --all

if ($LASTEXITCODE -ne 0) {
    Write-Host "[AVISO] Nao foi possivel adicionar todos os arquivos automaticamente. Tentando caminho alternativo..." -ForegroundColor Yellow
    git ls-files -o --exclude-standard | ForEach-Object { git add $_ }
}

# 4. Verificar se há arquivos no staging para commitar (usando --cached em vez de --staged)
$statusDepois = git diff --cached --name-only
if ([string]::IsNullOrWhiteSpace($statusDepois)) {
    Write-Host "Nenhum arquivo no staging para commitar!" -ForegroundColor Red
    Write-Host "Isso pode acontecer se todos os arquivos ja estao commitados ou se ha um problema com o Git" -ForegroundColor Yellow
    exit 1
}

# 5. Gerar mensagem de commit baseada nos arquivos modificados
Write-Host "Gerando mensagem de commit..." -ForegroundColor Yellow

$commitMessage = "feat: Atualizacao automatica - $(Get-Date -Format 'yyyy-MM-dd HH:mm')"

# Verificar se há arquivos específicos para mensagens personalizadas
if ($deletados -gt 100) {
    $commitMessage = "refactor: Modularizacao do projeto - migracao de arquivos para modulos"
} elseif ($statusAntes -match "colaborador") {
    $commitMessage = "feat: Sistema de gerenciamento de colaboradores implementado"
} elseif ($statusAntes -match "build") {
    $commitMessage = "fix: Correcoes de build e erros de compilacao"
} elseif ($statusAntes -match "crash") {
    $commitMessage = "fix: Correcao de crashes e estabilizacao do app"
} elseif ($statusAntes -match "database") {
    $commitMessage = "fix: Correcoes no banco de dados e migracoes"
} elseif ($statusAntes -match "sync|sincronizacao") {
    $commitMessage = "fix: Melhorias na sincronizacao de fotos e dados"
}

# 6. Realizar o commit
Write-Host "Realizando commit..." -ForegroundColor Yellow
Write-Host "Mensagem: $commitMessage" -ForegroundColor Cyan

git commit -m $commitMessage

# 7. Verificar se o commit foi bem-sucedido
if ($LASTEXITCODE -eq 0) {
    Write-Host "COMMIT REALIZADO COM SUCESSO!" -ForegroundColor Green
    Write-Host "=====================================" -ForegroundColor Cyan
    
    # Mostrar ultimo commit
    Write-Host "Ultimo commit:" -ForegroundColor Yellow
    git log -1 --oneline
    
    # Mostrar status final
    Write-Host "`nStatus apos commit:" -ForegroundColor Yellow
    $statusFinal = git status --short
    if ([string]::IsNullOrWhiteSpace($statusFinal)) {
        Write-Host "  Nenhuma mudanca pendente" -ForegroundColor Green
    } else {
        Write-Host "  Ainda ha mudancas pendentes:" -ForegroundColor Yellow
        git status --short | Select-Object -First 10
    }
    
} else {
    Write-Host "ERRO NO COMMIT!" -ForegroundColor Red
    Write-Host "Codigo de erro: $LASTEXITCODE" -ForegroundColor Red
    
    # Tentar obter mais informacoes sobre o erro
    Write-Host "`nVerificando possiveis problemas..." -ForegroundColor Yellow
    $errorOutput = git commit -m $commitMessage 2>&1
    Write-Host $errorOutput -ForegroundColor Red
    
    Write-Host "`nVerifique se ha conflitos ou problemas no Git" -ForegroundColor Red
    Write-Host "Status atual:" -ForegroundColor Yellow
    git status --short | Select-Object -First 20
    
    exit 1
}

Write-Host "Processo finalizado!" -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Cyan
