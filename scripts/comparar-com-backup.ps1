# Script para comparar estado atual com a tag de backup
# Identifica o que foi perdido

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "COMPARACAO: ESTADO ATUAL vs TAG BACKUP" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Pegar a tag mais recente
$tags = git tag -l "*modularizacao*" | Sort-Object -Descending
if (-not $tags) {
    Write-Host "ERRO: Nenhuma tag de modularizacao encontrada!" -ForegroundColor Red
    exit 1
}

$tagMaisRecente = $tags[0]
Write-Host "Tag de referencia: $tagMaisRecente" -ForegroundColor Green
Write-Host ""

# Verificar commits apos a tag
Write-Host "Verificando commits apos a tag..." -ForegroundColor Cyan
$commitsAposTag = git log ${tagMaisRecente}..HEAD --oneline 2>$null
if ($commitsAposTag) {
    Write-Host "Commits apos a tag:" -ForegroundColor Yellow
    $commitsAposTag | Select-Object -First 10 | ForEach-Object { Write-Host "  $_" -ForegroundColor Yellow }
    Write-Host ""
} else {
    Write-Host "Nenhum commit apos a tag (HEAD esta na tag ou antes)" -ForegroundColor Yellow
    Write-Host ""
}

# Verificar estado atual dos modulos
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "ESTADO ATUAL DOS MODULOS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$modulos = @("core", "data", "ui", "sync")
$modulosAtuaisOK = 0
foreach ($modulo in $modulos) {
    $buildFile = "$modulo/build.gradle.kts"
    if (Test-Path $buildFile) {
        Write-Host "  [$modulo] OK - build.gradle.kts existe" -ForegroundColor Green
        $modulosAtuaisOK++
    } else {
        Write-Host "  [$modulo] FALTANDO - build.gradle.kts" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "ESTADO ATUAL DOS LAYOUTS" -ForegroundColor Cyan
Write-Host ""

$layoutsCriticos = @(
    "fragment_expense_register.xml",
    "fragment_expense_types.xml",
    "fragment_expense_categories.xml",
    "item_expense_category.xml",
    "item_expense_type.xml",
    "fragment_closure_report.xml"
)

$layoutsAtuaisOK = 0
foreach ($layout in $layoutsCriticos) {
    $layoutPath = "app/src/main/res/layout/$layout"
    if (Test-Path $layoutPath) {
        Write-Host "  [$layout] OK" -ForegroundColor Green
        $layoutsAtuaisOK++
    } else {
        Write-Host "  [$layout] FALTANDO" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "ESTADO ATUAL DO REPOSITORY" -ForegroundColor Cyan
Write-Host ""

$appRepoPath = "app/src/main/java/com/example/gestaobilhares/data/repository/AppRepository.kt"
if (Test-Path $appRepoPath) {
    Write-Host "  AppRepository.kt existe" -ForegroundColor Green
    $content = Get-Content $appRepoPath -Raw
    if ($content -match "buscarCategoriasAtivas|buscarTiposAtivosComCategoria") {
        Write-Host "  Metodos de categorias/tipos: OK" -ForegroundColor Green
    } else {
        Write-Host "  Metodos de categorias/tipos: FALTANDO" -ForegroundColor Red
    }
} else {
    Write-Host "  AppRepository.kt NAO EXISTE" -ForegroundColor Red
}

$factoryPath = "app/src/main/java/com/example/gestaobilhares/data/factory/RepositoryFactory.kt"
if (Test-Path $factoryPath) {
    Write-Host "  RepositoryFactory.kt existe" -ForegroundColor Green
} else {
    Write-Host "  RepositoryFactory.kt FALTANDO" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESUMO COMPARATIVO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Estado Atual:" -ForegroundColor Yellow
Write-Host "  Modulos OK: $modulosAtuaisOK/4" -ForegroundColor $(if ($modulosAtuaisOK -eq 4) { "Green" } else { "Yellow" })
Write-Host "  Layouts OK: $layoutsAtuaisOK/$($layoutsCriticos.Count)" -ForegroundColor $(if ($layoutsAtuaisOK -eq $layoutsCriticos.Count) { "Green" } else { "Yellow" })
Write-Host ""

# Verificar o que foi deletado apos a tag
Write-Host "Verificando arquivos deletados apos a tag..." -ForegroundColor Cyan
Write-Host ""

$deletedFiles = git diff --name-only --diff-filter=D ${tagMaisRecente}..HEAD 2>$null
if ($deletedFiles) {
    Write-Host "Arquivos deletados apos a tag:" -ForegroundColor Red
    $deletedFiles | Select-Object -First 20 | ForEach-Object { 
        Write-Host "  - $_" -ForegroundColor Red 
    }
    if ($deletedFiles.Count -gt 20) {
        Write-Host "  ... e mais $($deletedFiles.Count - 20) arquivos" -ForegroundColor Yellow
    }
    Write-Host ""
} else {
    Write-Host "Nenhum arquivo deletado apos a tag" -ForegroundColor Green
    Write-Host ""
}

# Verificar arquivos modificados
Write-Host "Verificando arquivos modificados apos a tag..." -ForegroundColor Cyan
$modifiedFiles = git diff --name-only --diff-filter=M ${tagMaisRecente}..HEAD 2>$null
if ($modifiedFiles) {
    Write-Host "Total de arquivos modificados: $($modifiedFiles.Count)" -ForegroundColor Yellow
    Write-Host ""
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CONCLUSAO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

if ($modulosAtuaisOK -eq 4 -and $layoutsAtuaisOK -eq $layoutsCriticos.Count) {
    Write-Host "Estado atual esta MELHOR que a tag!" -ForegroundColor Green
} elseif ($deletedFiles -and ($deletedFiles -match "build.gradle.kts|\.xml$|AppRepository|RepositoryFactory")) {
    Write-Host "ATENCAO: Arquivos importantes foram deletados apos a tag!" -ForegroundColor Red
    Write-Host "Recomendacao: Restaurar da tag ou verificar commits recentes" -ForegroundColor Yellow
} else {
    Write-Host "Estado atual precisa de correcoes" -ForegroundColor Yellow
}

