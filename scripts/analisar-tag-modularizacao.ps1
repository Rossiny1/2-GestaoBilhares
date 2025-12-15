# Script para analisar a tag de backup da modularizacao
# Verifica se a modularizacao esta completa na tag

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "ANALISE DA TAG DE MODULARIZACAO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Pegar a tag mais recente
$tags = git tag -l "*modularizacao*" | Sort-Object -Descending
if (-not $tags) {
    Write-Host "ERRO: Nenhuma tag de modularizacao encontrada!" -ForegroundColor Red
    exit 1
}

$tagMaisRecente = $tags[0]
Write-Host "Analisando tag: $tagMaisRecente" -ForegroundColor Green
Write-Host ""

# Verificar modulos
Write-Host "Verificando modulos:" -ForegroundColor Cyan
$modulos = @("core", "data", "ui", "sync")
$modulosOK = 0
foreach ($modulo in $modulos) {
    $buildFile = "$modulo/build.gradle.kts"
    $gitRef = "${tagMaisRecente}:${buildFile}"
    $exists = git cat-file -e $gitRef 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  [$modulo] OK - build.gradle.kts existe" -ForegroundColor Green
        $modulosOK++
    } else {
        Write-Host "  [$modulo] FALTANDO - build.gradle.kts" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Verificando layouts criticos:" -ForegroundColor Cyan
$layoutsCriticos = @(
    "fragment_expense_register.xml",
    "fragment_expense_types.xml",
    "fragment_expense_categories.xml",
    "item_expense_category.xml",
    "item_expense_type.xml",
    "fragment_closure_report.xml"
)

$layoutsOK = 0
foreach ($layout in $layoutsCriticos) {
    $layoutPath = "app/src/main/res/layout/$layout"
    $gitRef = "${tagMaisRecente}:${layoutPath}"
    $exists = git cat-file -e $gitRef 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  [$layout] OK" -ForegroundColor Green
        $layoutsOK++
    } else {
        Write-Host "  [$layout] FALTANDO" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Verificando AppRepository:" -ForegroundColor Cyan
$appRepoPath = "app/src/main/java/com/example/gestaobilhares/data/repository/AppRepository.kt"
$gitRef = "${tagMaisRecente}:${appRepoPath}"
$exists = git cat-file -e $gitRef 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host "  AppRepository.kt existe" -ForegroundColor Green
    
    # Verificar conteudo
    $content = git show $gitRef 2>$null
    if ($content -match "buscarCategoriasAtivas|buscarTiposAtivosComCategoria") {
        Write-Host "  Metodos de categorias/tipos: OK" -ForegroundColor Green
    } else {
        Write-Host "  Metodos de categorias/tipos: FALTANDO" -ForegroundColor Red
    }
} else {
    Write-Host "  AppRepository.kt NAO EXISTE" -ForegroundColor Red
}

Write-Host ""
Write-Host "Verificando RepositoryFactory:" -ForegroundColor Cyan
$factoryPath = "app/src/main/java/com/example/gestaobilhares/data/factory/RepositoryFactory.kt"
$gitRef = "${tagMaisRecente}:${factoryPath}"
$exists = git cat-file -e $gitRef 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host "  RepositoryFactory.kt existe" -ForegroundColor Green
} else {
    Write-Host "  RepositoryFactory.kt FALTANDO" -ForegroundColor Red
}

Write-Host ""
Write-Host "Verificando settings.gradle.kts:" -ForegroundColor Cyan
$settingsPath = "settings.gradle.kts"
$gitRef = "${tagMaisRecente}:${settingsPath}"
$exists = git cat-file -e $gitRef 2>$null
if ($LASTEXITCODE -eq 0) {
    $content = git show $gitRef 2>$null
    if ($content -match "include.*:core|include.*:data|include.*:ui|include.*:sync") {
        Write-Host "  Modulos incluidos no settings.gradle.kts: OK" -ForegroundColor Green
    } else {
        Write-Host "  Modulos incluidos no settings.gradle.kts: FALTANDO" -ForegroundColor Red
    }
} else {
    Write-Host "  settings.gradle.kts NAO EXISTE" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESUMO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Modulos OK: $modulosOK/4" -ForegroundColor $(if ($modulosOK -eq 4) { "Green" } else { "Yellow" })
Write-Host "Layouts OK: $layoutsOK/$($layoutsCriticos.Count)" -ForegroundColor $(if ($layoutsOK -eq $layoutsCriticos.Count) { "Green" } else { "Yellow" })
Write-Host ""

if ($modulosOK -eq 4 -and $layoutsOK -eq $layoutsCriticos.Count) {
    Write-Host "MODULARIZACAO COMPLETA NA TAG $tagMaisRecente!" -ForegroundColor Green
} else {
    Write-Host "MODULARIZACAO INCOMPLETA NA TAG $tagMaisRecente" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Tags disponiveis:" -ForegroundColor Cyan
    foreach ($tag in $tags) {
        Write-Host "  - $tag" -ForegroundColor Yellow
    }
}

