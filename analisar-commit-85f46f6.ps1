# Script para analisar o commit 85f46f6
# Verifica se o projeto estava modularizado e com build funcionando

$commitHash = "85f46f6"
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "ANALISE DO COMMIT: $commitHash" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar se o commit existe
Write-Host "[1/8] Verificando se o commit existe..." -ForegroundColor Yellow
$commitExists = git cat-file -e "$commitHash" 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERRO: Commit $commitHash nao encontrado!" -ForegroundColor Red
    exit 1
}
Write-Host "OK: Commit encontrado" -ForegroundColor Green
Write-Host ""

# Informações do commit
Write-Host "[2/8] Informacoes do commit..." -ForegroundColor Yellow
git show --no-patch --format="%H%n%an%n%ae%n%ad%n%s" $commitHash
Write-Host ""

# Verificar módulos
Write-Host "[3/8] Verificando modulos..." -ForegroundColor Yellow
$modulos = @("core", "data", "sync", "ui")
$modulosEncontrados = 0
foreach ($modulo in $modulos) {
    $buildFile = git ls-tree -r --name-only $commitHash | Select-String "^$modulo/build.gradle.kts$"
    if ($buildFile) {
        Write-Host "  OK: $modulo/build.gradle.kts encontrado" -ForegroundColor Green
        $modulosEncontrados++
    } else {
        Write-Host "  FALTANDO: $modulo/build.gradle.kts" -ForegroundColor Red
    }
}
Write-Host "Modulos encontrados: $modulosEncontrados/$($modulos.Count)" -ForegroundColor $(if ($modulosEncontrados -eq $modulos.Count) { "Green" } else { "Yellow" })
Write-Host ""

# Verificar settings.gradle.kts
Write-Host "[4/8] Verificando settings.gradle.kts..." -ForegroundColor Yellow
$settingsContent = git show "${commitHash}:settings.gradle.kts" 2>$null
if ($settingsContent) {
    Write-Host "OK: settings.gradle.kts existe" -ForegroundColor Green
    $includeCore = $settingsContent -match "include\(`"`:core`"\)"
    $includeData = $settingsContent -match "include\(`"`:data`"\)"
    $includeSync = $settingsContent -match "include\(`"`:sync`"\)"
    $includeUi = $settingsContent -match "include\(`"`:ui`"\)"
    
    Write-Host "  include(':core'): $(if ($includeCore) { 'SIM' } else { 'NAO' })" -ForegroundColor $(if ($includeCore) { "Green" } else { "Red" })
    Write-Host "  include(':data'): $(if ($includeData) { 'SIM' } else { 'NAO' })" -ForegroundColor $(if ($includeData) { "Green" } else { "Red" })
    Write-Host "  include(':sync'): $(if ($includeSync) { 'SIM' } else { 'NAO' })" -ForegroundColor $(if ($includeSync) { "Green" } else { "Red" })
    Write-Host "  include(':ui'): $(if ($includeUi) { 'SIM' } else { 'NAO' })" -ForegroundColor $(if ($includeUi) { "Green" } else { "Red" })
} else {
    Write-Host "ERRO: settings.gradle.kts nao encontrado" -ForegroundColor Red
}
Write-Host ""

# Verificar AppRepository
Write-Host "[5/8] Verificando AppRepository..." -ForegroundColor Yellow
$appRepoPath = "app/src/main/java/com/example/gestaobilhares/data/repository/AppRepository.kt"
$appRepoExists = git ls-tree -r --name-only $commitHash | Select-String "^$appRepoPath$"
if ($appRepoExists) {
    Write-Host "OK: AppRepository.kt encontrado" -ForegroundColor Green
    $appRepoContent = git show "${commitHash}:${appRepoPath}" 2>$null
    if ($appRepoContent) {
        $hasCreateMethod = $appRepoContent -match "fun create\(|companion object"
        $hasCategoriaMethods = $appRepoContent -match "buscarCategoriasAtivas|buscarCategoriaPorId"
        $hasTipoMethods = $appRepoContent -match "buscarTiposAtivosComCategoria|buscarTipoPorId"
        
        Write-Host "  Metodo create/companion: $(if ($hasCreateMethod) { 'SIM' } else { 'NAO' })" -ForegroundColor $(if ($hasCreateMethod) { "Green" } else { "Yellow" })
        Write-Host "  Metodos Categoria: $(if ($hasCategoriaMethods) { 'SIM' } else { 'NAO' })" -ForegroundColor $(if ($hasCategoriaMethods) { "Green" } else { "Yellow" })
        Write-Host "  Metodos Tipo: $(if ($hasTipoMethods) { 'SIM' } else { 'NAO' })" -ForegroundColor $(if ($hasTipoMethods) { "Green" } else { "Yellow" })
    }
} else {
    Write-Host "FALTANDO: AppRepository.kt" -ForegroundColor Red
}
Write-Host ""

# Verificar RepositoryFactory
Write-Host "[6/8] Verificando RepositoryFactory..." -ForegroundColor Yellow
$factoryPath = "app/src/main/java/com/example/gestaobilhares/data/factory/RepositoryFactory.kt"
$factoryExists = git ls-tree -r --name-only $commitHash | Select-String "^$factoryPath$"
if ($factoryExists) {
    Write-Host "OK: RepositoryFactory.kt encontrado" -ForegroundColor Green
} else {
    Write-Host "FALTANDO: RepositoryFactory.kt" -ForegroundColor Yellow
}
Write-Host ""

# Verificar layouts críticos
Write-Host "[7/8] Verificando layouts criticos..." -ForegroundColor Yellow
$layoutsCriticos = @(
    "app/src/main/res/layout/item_expense_category.xml",
    "app/src/main/res/layout/item_expense_type.xml",
    "app/src/main/res/layout/fragment_expense_types.xml",
    "app/src/main/res/layout/fragment_expense_categories.xml",
    "app/src/main/res/layout/fragment_expense_register.xml",
    "app/src/main/res/layout/fragment_closure_report.xml"
)
$layoutsEncontrados = 0
foreach ($layout in $layoutsCriticos) {
    $layoutExists = git ls-tree -r --name-only $commitHash | Select-String "^$layout$"
    if ($layoutExists) {
        Write-Host "  OK: $(Split-Path $layout -Leaf)" -ForegroundColor Green
        $layoutsEncontrados++
    } else {
        Write-Host "  FALTANDO: $(Split-Path $layout -Leaf)" -ForegroundColor Red
    }
}
Write-Host "Layouts encontrados: $layoutsEncontrados/$($layoutsCriticos.Count)" -ForegroundColor $(if ($layoutsEncontrados -eq $layoutsCriticos.Count) { "Green" } else { "Yellow" })
Write-Host ""

# Verificar estrutura de diretórios dos módulos
Write-Host "[8/8] Verificando estrutura de diretorios dos modulos..." -ForegroundColor Yellow
foreach ($modulo in $modulos) {
    $moduloFiles = git ls-tree -r --name-only $commitHash | Select-String "^$modulo/"
    $fileCount = ($moduloFiles | Measure-Object).Count
    if ($fileCount -gt 0) {
        Write-Host "  ${modulo}: $fileCount arquivos" -ForegroundColor Green
    } else {
        Write-Host "  ${modulo}: VAZIO ou nao existe" -ForegroundColor Red
    }
}
Write-Host ""

# Resumo final
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESUMO DA ANALISE" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Commit: $commitHash" -ForegroundColor White
Write-Host "Modulos: $modulosEncontrados/$($modulos.Count)" -ForegroundColor $(if ($modulosEncontrados -eq $modulos.Count) { "Green" } else { "Yellow" })
Write-Host "AppRepository: $(if ($appRepoExists) { 'SIM' } else { 'NAO' })" -ForegroundColor $(if ($appRepoExists) { "Green" } else { "Red" })
Write-Host "RepositoryFactory: $(if ($factoryExists) { 'SIM' } else { 'NAO' })" -ForegroundColor $(if ($factoryExists) { "Green" } else { "Yellow" })
Write-Host "Layouts criticos: $layoutsEncontrados/$($layoutsCriticos.Count)" -ForegroundColor $(if ($layoutsEncontrados -eq $layoutsCriticos.Count) { "Green" } else { "Yellow" })
Write-Host ""

# Verificar se há mensagem de commit indicando build
$commitMessage = git log -1 --format="%s" $commitHash
if ($commitMessage -match "build|modulariz|funcionando|passando") {
    Write-Host "Mensagem do commit indica: $commitMessage" -ForegroundColor Cyan
}
Write-Host ""

