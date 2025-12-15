# Script para analisar sincronizacao antes da modularizacao
# Compara commit 7feb452b com implementacao atual

$commitHash = "7feb452b"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  ANALISE DE SINCRONIZACAO" -ForegroundColor Cyan
Write-Host "  Commit: $commitHash (antes modularizacao)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar arquivos de sincronizacao no commit
Write-Host "1. Verificando arquivos de sincronizacao no commit..." -ForegroundColor Yellow
Write-Host ""

$syncFiles = git show $commitHash --name-only 2>$null | Select-String -Pattern "sync|Sync|Sincronizacao|sincronizacao"

if ($syncFiles) {
    Write-Host "Arquivos encontrados:" -ForegroundColor Green
    $syncFiles | ForEach-Object { Write-Host "  - $_" -ForegroundColor Gray }
} else {
    Write-Host "Nenhum arquivo de sincronizacao encontrado no commit" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "2. Verificando RoutesFragment no commit..." -ForegroundColor Yellow
Write-Host ""

$routesFragment = git show "$commitHash`:app/src/main/java/com/example/gestaobilhares/ui/routes/RoutesFragment.kt" 2>$null

if ($routesFragment) {
    $syncButton = $routesFragment | Select-String -Pattern "syncButton|sincronizar|SyncManager" -Context 5,5
    if ($syncButton) {
        Write-Host "Codigo de sincronizacao encontrado:" -ForegroundColor Green
        $syncButton | ForEach-Object { Write-Host $_ -ForegroundColor Gray }
    } else {
        Write-Host "Nenhum codigo de sincronizacao encontrado no RoutesFragment" -ForegroundColor Yellow
    }
} else {
    Write-Host "RoutesFragment nao encontrado no commit" -ForegroundColor Red
}

Write-Host ""
Write-Host "3. Verificando AppRepository no commit..." -ForegroundColor Yellow
Write-Host ""

$appRepo = git show "$commitHash`:app/src/main/java/com/example/gestaobilhares/data/repository/AppRepository.kt" 2>$null | Select-Object -First 100

if ($appRepo) {
    Write-Host "Primeiras linhas do AppRepository:" -ForegroundColor Green
    $appRepo | Select-Object -First 30 | ForEach-Object { Write-Host $_ -ForegroundColor Gray }
} else {
    Write-Host "AppRepository nao encontrado no commit" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  ANALISE CONCLUIDA" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

