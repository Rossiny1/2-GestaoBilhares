# Script para restaurar todos os layouts do commit 7feb452b
# Autor: Auto
# Data: 2025-01-28

$commitHash = "7feb452b"
$layoutPath = "app/src/main/res/layout"
$errorCount = 0
$successCount = 0

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESTAURACAO DE LAYOUTS DO COMMIT $commitHash" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Lista de layouts conhecidos que podem estar faltando
$layoutsParaRestaurar = @(
    "item_expense_category.xml",
    "item_expense_type.xml",
    "fragment_expense_register.xml",
    "fragment_expense_types.xml",
    "fragment_expense_categories.xml",
    "fragment_closure_report.xml"
)

Write-Host "Verificando layouts no commit $commitHash..." -ForegroundColor Yellow
Write-Host ""

# Primeiro, tenta listar todos os layouts do commit
Write-Host "Buscando todos os layouts XML no commit..." -ForegroundColor Yellow
try {
    $allLayouts = git ls-tree -r --name-only $commitHash -- "$layoutPath/*.xml" 2>$null
    if ($LASTEXITCODE -eq 0 -and $allLayouts) {
        Write-Host "Encontrados $($allLayouts.Count) layouts no commit" -ForegroundColor Green
        $layoutsParaRestaurar = $allLayouts | ForEach-Object { 
            $_.Replace("$layoutPath/", "")
        }
    } else {
        Write-Host "Nao foi possivel listar layouts automaticamente. Usando lista manual." -ForegroundColor Yellow
    }
} catch {
    Write-Host "Erro ao listar layouts: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "Restaurando layouts..." -ForegroundColor Yellow
Write-Host ""

# Garantir que o diretório existe
if (-not (Test-Path $layoutPath)) {
    New-Item -ItemType Directory -Path $layoutPath -Force | Out-Null
    Write-Host "Diretorio $layoutPath criado" -ForegroundColor Green
}

# Restaurar cada layout
foreach ($layout in $layoutsParaRestaurar) {
    $fullPath = Join-Path $layoutPath $layout
    $gitPath = "$layoutPath/$layout"
    
    Write-Host "Restaurando: $layout..." -NoNewline
    
    try {
        # Verificar se o arquivo existe no commit
        $gitRef = "${commitHash}:${gitPath}"
        $exists = git cat-file -e $gitRef 2>$null
        if ($LASTEXITCODE -eq 0) {
            # Restaurar o arquivo
            git show $gitRef > $fullPath 2>$null
            
            if ($LASTEXITCODE -eq 0 -and (Test-Path $fullPath)) {
                $fileSize = (Get-Item $fullPath).Length
                if ($fileSize -gt 0) {
                    Write-Host " OK ($fileSize bytes)" -ForegroundColor Green
                    $successCount++
                } else {
                    Write-Host " ERRO: Arquivo vazio" -ForegroundColor Red
                    $errorCount++
                }
            } else {
                Write-Host " ERRO: Falha ao restaurar" -ForegroundColor Red
                $errorCount++
            }
        } else {
            Write-Host " AVISO: Nao existe no commit" -ForegroundColor Yellow
        }
    } catch {
        Write-Host " ERRO: $_" -ForegroundColor Red
        $errorCount++
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESUMO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Sucesso: $successCount" -ForegroundColor Green
Write-Host "Erros: $errorCount" -ForegroundColor $(if ($errorCount -gt 0) { "Red" } else { "Green" })
Write-Host ""

# Verificar se há outros arquivos que podem estar faltando
Write-Host "Verificando outros arquivos que podem estar faltando..." -ForegroundColor Yellow
Write-Host ""

# Verificar se há referências a bindings que não têm layouts
$bindingReferences = @(
    "FragmentExpenseTypesBinding",
    "FragmentExpenseCategoriesBinding",
    "FragmentExpenseRegisterBinding",
    "ItemExpenseCategoryBinding",
    "ItemExpenseTypeBinding",
    "FragmentClosureReportBinding"
)

$missingLayouts = @()
foreach ($binding in $bindingReferences) {
    # Converter Binding para nome de layout
    $layoutName = $binding -replace "Binding$", ""
    $layoutName = $layoutName -replace "([a-z])([A-Z])", '$1_$2'
    $layoutName = $layoutName.ToLower()
    $layoutFile = "$layoutName.xml"
    $layoutPathFull = Join-Path $layoutPath $layoutFile
    
    if (-not (Test-Path $layoutPathFull)) {
        $missingLayouts += $layoutFile
        Write-Host "FALTANDO: $layoutFile" -ForegroundColor Red
    }
}

if ($missingLayouts.Count -gt 0) {
    Write-Host ""
    Write-Host "Tentando restaurar layouts faltantes do commit..." -ForegroundColor Yellow
    foreach ($missing in $missingLayouts) {
        $gitPath = "$layoutPath/$missing"
        Write-Host "Tentando restaurar: $missing..." -NoNewline
        try {
            $gitRef = "${commitHash}:${gitPath}"
            git show $gitRef > (Join-Path $layoutPath $missing) 2>$null
            if ($LASTEXITCODE -eq 0 -and (Test-Path (Join-Path $layoutPath $missing))) {
                Write-Host " OK" -ForegroundColor Green
                $successCount++
            } else {
                Write-Host " NAO ENCONTRADO NO COMMIT" -ForegroundColor Yellow
            }
        } catch {
            Write-Host " ERRO" -ForegroundColor Red
        }
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESTAURACAO CONCLUIDA" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Total restaurado: $successCount" -ForegroundColor Green
Write-Host "Total de erros: $errorCount" -ForegroundColor $(if ($errorCount -gt 0) { "Red" } else { "Green" })
Write-Host ""

if ($errorCount -eq 0) {
    Write-Host "Todos os layouts foram restaurados com sucesso!" -ForegroundColor Green
    exit 0
} else {
    Write-Host "Alguns layouts nao puderam ser restaurados. Verifique os erros acima." -ForegroundColor Yellow
    exit 1
}

