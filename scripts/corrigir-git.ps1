# Script para corrigir problemas do Git
Write-Host "=== CORRECAO DE PROBLEMAS DO GIT ===" -ForegroundColor Cyan
Write-Host ""

$rootPath = $PSScriptRoot

# 1. Remover arquivo de lock se existir
Write-Host "[1/4] Verificando arquivo de lock..." -ForegroundColor Yellow
$lockFile = Join-Path $rootPath ".git\index.lock"
if (Test-Path $lockFile) {
    try {
        Remove-Item -Path $lockFile -Force -ErrorAction Stop
        Write-Host "  [OK] Arquivo index.lock removido" -ForegroundColor Green
    } catch {
        Write-Host "  [ERRO] Falha ao remover lock: ${_}" -ForegroundColor Red
    }
} else {
    Write-Host "  [OK] Nenhum arquivo de lock encontrado" -ForegroundColor Green
}

# 2. Verificar status do repositório
Write-Host ""
Write-Host "[2/4] Verificando status do Git..." -ForegroundColor Yellow
try {
    $status = git status 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  [OK] Git funcionando corretamente" -ForegroundColor Green
        Write-Host $status -ForegroundColor Gray
    } else {
        Write-Host "  [ERRO] Problema ao verificar status" -ForegroundColor Red
        Write-Host $status -ForegroundColor Red
    }
} catch {
    Write-Host "  [ERRO] Falha ao executar git status: ${_}" -ForegroundColor Red
}

# 3. Verificar se está em detached HEAD
Write-Host ""
Write-Host "[3/4] Verificando branch atual..." -ForegroundColor Yellow
try {
    $branch = git branch --show-current 2>&1
    if ($LASTEXITCODE -eq 0 -and $branch) {
        Write-Host "  [OK] Branch atual: $branch" -ForegroundColor Green
    } else {
        Write-Host "  [ATENCAO] Repositorio em detached HEAD" -ForegroundColor Yellow
        Write-Host "  Verificando ultimo commit..." -ForegroundColor Yellow
        
        $lastCommit = git log --oneline -1 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  Ultimo commit: $lastCommit" -ForegroundColor Gray
            
            # Tentar encontrar branch principal
            $branches = git branch -a 2>&1
            if ($branches -match "master|main") {
                $mainBranch = ($branches | Select-String "master|main" | Select-Object -First 1).ToString().Trim()
                Write-Host "  Branch principal encontrado: $mainBranch" -ForegroundColor Cyan
                Write-Host ""
                Write-Host "  Para voltar ao branch principal, execute:" -ForegroundColor Yellow
                Write-Host "    git checkout $($mainBranch.TrimStart('* ').Trim())" -ForegroundColor White
            }
        }
    }
} catch {
    Write-Host "  [ERRO] Falha ao verificar branch: ${_}" -ForegroundColor Red
}

# 4. Verificar referências corrompidas
Write-Host ""
Write-Host "[4/4] Verificando referencias..." -ForegroundColor Yellow
try {
    $refs = git show-ref 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  [OK] Referencias OK" -ForegroundColor Green
    } else {
        Write-Host "  [ATENCAO] Possivel problema com referencias" -ForegroundColor Yellow
        Write-Host "  Tentando corrigir..." -ForegroundColor Yellow
        
        # Tentar reparar referências
        git fsck --full 2>&1 | Out-Null
        Write-Host "  Verificacao de integridade concluida" -ForegroundColor Gray
    }
} catch {
    Write-Host "  [ERRO] Falha ao verificar referencias: ${_}" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== CORRECAO CONCLUIDA ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Se ainda houver problemas, execute manualmente:" -ForegroundColor Yellow
Write-Host "  1. git checkout master (ou main)" -ForegroundColor White
Write-Host "  2. git status" -ForegroundColor White
Write-Host "  3. git log --oneline -5" -ForegroundColor White

