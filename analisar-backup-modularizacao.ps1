# Script para analisar o backup da modularizacao
# Verifica se a modularizacao esta completa no backup

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "ANALISE DO BACKUP DE MODULARIZACAO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Procurar o backup (excluindo scripts .ps1)
$backupPath = Get-ChildItem -Path . -Filter "*backup*modularizacao*" -Recurse -ErrorAction SilentlyContinue | 
    Where-Object { $_.Extension -ne ".ps1" -and $_.Name -notlike "*analisar*" } | 
    Select-Object -First 1

if (-not $backupPath) {
    Write-Host "Backup nao encontrado na raiz. Procurando em outras localizacoes..." -ForegroundColor Yellow
    if (Test-Path "backups") {
        $backupPath = Get-ChildItem -Path "backups" -Filter "*modularizacao*" -ErrorAction SilentlyContinue | 
            Where-Object { $_.Extension -ne ".ps1" } | 
            Select-Object -First 1
    }
}

if (-not $backupPath) {
    Write-Host "Procurando por tags ou branches de backup..." -ForegroundColor Yellow
    $tags = git tag -l "*modularizacao*" 2>$null
    if ($tags) {
        Write-Host "Tags encontradas: $tags" -ForegroundColor Yellow
    }
}

if (-not $backupPath) {
    Write-Host "ERRO: Backup nao encontrado!" -ForegroundColor Red
    Write-Host "Procurando commits relacionados a modularizacao..." -ForegroundColor Yellow
    Write-Host ""
    
    # Procurar commits relacionados
    Write-Host "Commits relacionados a modularizacao:" -ForegroundColor Yellow
    git log --all --oneline --grep="modularizacao" --grep="modular" -i | Select-Object -First 10
    exit 1
}

Write-Host "Backup encontrado: $($backupPath.FullName)" -ForegroundColor Green
Write-Host ""

# Verificar se e um diretorio ou arquivo
if (Test-Path $backupPath.FullName -PathType Container) {
    $backupDir = $backupPath.FullName
    Write-Host "E um diretorio. Analisando estrutura..." -ForegroundColor Yellow
    Write-Host ""
    
    # Verificar modulos
    Write-Host "Verificando modulos:" -ForegroundColor Cyan
    $modulos = @("core", "data", "ui", "sync")
    foreach ($modulo in $modulos) {
        $moduloPath = Join-Path $backupDir $modulo
        if (Test-Path $moduloPath) {
            $buildFile = Join-Path $moduloPath "build.gradle.kts"
            if (Test-Path $buildFile) {
                Write-Host "  [$modulo] OK - build.gradle.kts existe" -ForegroundColor Green
            } else {
                Write-Host "  [$modulo] FALTANDO - build.gradle.kts" -ForegroundColor Red
            }
        } else {
            Write-Host "  [$modulo] NAO EXISTE" -ForegroundColor Red
        }
    }
    
    Write-Host ""
    Write-Host "Verificando layouts:" -ForegroundColor Cyan
    $layoutPath = Join-Path $backupDir "app/src/main/res/layout"
    if (Test-Path $layoutPath) {
        $layouts = Get-ChildItem -Path $layoutPath -Filter "*.xml" -ErrorAction SilentlyContinue
        Write-Host "  Total de layouts: $($layouts.Count)" -ForegroundColor Green
        
        # Verificar layouts criticos
        $layoutsCriticos = @(
            "fragment_expense_register.xml",
            "fragment_expense_types.xml",
            "fragment_expense_categories.xml",
            "item_expense_category.xml",
            "item_expense_type.xml",
            "fragment_closure_report.xml"
        )
        
        Write-Host ""
        Write-Host "  Layouts criticos:" -ForegroundColor Yellow
        foreach ($layout in $layoutsCriticos) {
            $layoutFile = Join-Path $layoutPath $layout
            if (Test-Path $layoutFile) {
                Write-Host "    [$layout] OK" -ForegroundColor Green
            } else {
                Write-Host "    [$layout] FALTANDO" -ForegroundColor Red
            }
        }
    } else {
        Write-Host "  Diretorio de layouts nao encontrado" -ForegroundColor Red
    }
    
    Write-Host ""
    Write-Host "Verificando AppRepository:" -ForegroundColor Cyan
    $appRepoPath = Join-Path $backupDir "app/src/main/java/com/example/gestaobilhares/data/repository/AppRepository.kt"
    if (Test-Path $appRepoPath) {
        Write-Host "  AppRepository.kt existe" -ForegroundColor Green
        
        # Verificar se tem metodos de categorias e tipos
        $content = Get-Content $appRepoPath -Raw
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
    $factoryPath = Join-Path $backupDir "app/src/main/java/com/example/gestaobilhares/data/factory/RepositoryFactory.kt"
    if (Test-Path $factoryPath) {
        Write-Host "  RepositoryFactory.kt existe" -ForegroundColor Green
    } else {
        Write-Host "  RepositoryFactory.kt FALTANDO" -ForegroundColor Red
    }
    
} else {
    Write-Host "E um arquivo. Verificando tipo..." -ForegroundColor Yellow
    Write-Host "Tipo: $($backupPath.Extension)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "ANALISE CONCLUIDA" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

