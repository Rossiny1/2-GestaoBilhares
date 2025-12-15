# Script para commit das correções de warnings
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "COMMIT CORRECOES DE WARNINGS" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Verificar se estamos em um repositório Git
if (-not (Test-Path .git)) {
    Write-Host "ERRO: Nao esta em um repositorio Git!" -ForegroundColor Red
    exit 1
}

# Verificar status atual
Write-Host "Verificando status do Git..." -ForegroundColor Yellow
$status = git status --porcelain

if ([string]::IsNullOrWhiteSpace($status)) {
    Write-Host "Nenhuma mudanca para commitar." -ForegroundColor Yellow
    exit 0
}

# Verificar se está em HEAD detached
$branch = git branch --show-current
if ([string]::IsNullOrWhiteSpace($branch)) {
    Write-Host "AVISO: HEAD detached detectado!" -ForegroundColor Yellow
    Write-Host "Criando branch 'fix-warnings'..." -ForegroundColor Yellow
    git checkout -b fix-warnings
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERRO: Falha ao criar branch!" -ForegroundColor Red
        exit 1
    }
}

# Adicionar apenas os arquivos específicos que corrigimos
Write-Host "Adicionando arquivos corrigidos ao staging..." -ForegroundColor Yellow
git add app/src/main/java/com/example/gestaobilhares/ui/expenses/ExpenseRegisterFragment.kt 2>$null
git add app/src/main/java/com/example/gestaobilhares/ui/mesas/GerenciarMesasFragment.kt 2>$null
git add app/src/main/java/com/example/gestaobilhares/ui/mesas/GerenciarMesasViewModel.kt 2>$null

# Adicionar outros arquivos modificados, mas EXCLUINDO build/ e arquivos ignorados
Write-Host "Verificando outros arquivos modificados..." -ForegroundColor Yellow
$allStatus = git status --porcelain
$modifiedFiles = $allStatus | Where-Object { 
    $_ -notmatch "build/" -and 
    $_ -notmatch "\.dex$" -and
    $_ -notmatch "core/build/" -and
    $_ -notmatch "data/build/" -and
    $_ -notmatch "sync/build/" -and
    $_ -notmatch "ui/build/" -and
    ($_ -match "^\s*M\s+" -or $_ -match "^\s*D\s+")
}

if ($modifiedFiles) {
    Write-Host "Adicionando outros arquivos modificados (excluindo build/)..." -ForegroundColor Yellow
    # Adicionar arquivos modificados um por um, excluindo build
    $modifiedFiles | ForEach-Object {
        $file = ($_ -replace "^\s*[MD]\s+", "").Trim()
        if ($file -and $file -notmatch "build/" -and (Test-Path $file -ErrorAction SilentlyContinue)) {
            # Verificar se o arquivo não está ignorado
            $checkIgnore = git check-ignore $file 2>$null
            if (-not $checkIgnore) {
                git add $file 2>$null
            }
        }
    }
}

# Fazer o commit
Write-Host "Realizando commit..." -ForegroundColor Yellow
$commitMessage = @"
fix: Correcoes de warnings de codigo

- Removidas variaveis nao utilizadas em ExpenseRegisterFragment
- Corrigidos parametros nao utilizados em GerenciarMesasFragment
- Removida variavel mesasLocadas nao utilizada em GerenciarMesasViewModel
- Limpeza de warnings concluida
"@

git commit -m $commitMessage

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "=====================================" -ForegroundColor Green
    Write-Host "COMMIT REALIZADO COM SUCESSO!" -ForegroundColor Green
    Write-Host "=====================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Ultimo commit:" -ForegroundColor Cyan
    git log --oneline -1
    Write-Host ""
} else {
    Write-Host ""
    Write-Host "ERRO: Falha ao realizar commit!" -ForegroundColor Red
    exit 1
}

