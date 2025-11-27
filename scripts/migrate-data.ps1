# Script para migracao do modulo :data
Write-Host "=== MIGRACAO MODULO :data ===" -ForegroundColor Cyan

# Criar estrutura de diretórios
Write-Host "Criando estrutura de diretórios..." -ForegroundColor Yellow
$dataDirs = @(
    "data\src\main\java\com\example\gestaobilhares\data\entities",
    "data\src\main\java\com\example\gestaobilhares\data\dao",
    "data\src\main\java\com\example\gestaobilhares\data\database",
    "data\src\main\java\com\example\gestaobilhares\data\repository\domain",
    "data\src\main\java\com\example\gestaobilhares\data\factory",
    "data\src\main\java\com\example\gestaobilhares\data\model"
)

foreach ($dir in $dataDirs) {
    New-Item -ItemType Directory -Path $dir -Force | Out-Null
}

# Mover entities
Write-Host "Movendo entities..." -ForegroundColor Yellow
$entitiesSource = "app\src\main\java\com\example\gestaobilhares\data\entities"
$entitiesTarget = "data\src\main\java\com\example\gestaobilhares\data\entities"

if (Test-Path $entitiesSource) {
    Get-ChildItem -Path $entitiesSource -Filter "*.kt" | ForEach-Object {
        Copy-Item -Path $_.FullName -Destination (Join-Path $entitiesTarget $_.Name) -Force
        Write-Host "  Movido: $($_.Name)" -ForegroundColor Green
    }
}

# Mover DAOs
Write-Host "Movendo DAOs..." -ForegroundColor Yellow
$daosSource = "app\src\main\java\com\example\gestaobilhares\data\dao"
$daosTarget = "data\src\main\java\com\example\gestaobilhares\data\dao"

if (Test-Path $daosSource) {
    Get-ChildItem -Path $daosSource -Filter "*Dao.kt" | ForEach-Object {
        Copy-Item -Path $_.FullName -Destination (Join-Path $daosTarget $_.Name) -Force
        Write-Host "  Movido: $($_.Name)" -ForegroundColor Green
    }
}

# Mover database
Write-Host "Movendo database..." -ForegroundColor Yellow
$dbSource = "app\src\main\java\com\example\gestaobilhares\data\database"
$dbTarget = "data\src\main\java\com\example\gestaobilhares\data\database"

if (Test-Path $dbSource) {
    Get-ChildItem -Path $dbSource -Filter "*.kt" | ForEach-Object {
        Copy-Item -Path $_.FullName -Destination (Join-Path $dbTarget $_.Name) -Force
        Write-Host "  Movido: $($_.Name)" -ForegroundColor Green
    }
}

# Mover repositories (domain)
Write-Host "Movendo repositories domain..." -ForegroundColor Yellow
$repoDomainSource = "app\src\main\java\com\example\gestaobilhares\data\repository\domain"
$repoDomainTarget = "data\src\main\java\com\example\gestaobilhares\data\repository\domain"

if (Test-Path $repoDomainSource) {
    Get-ChildItem -Path $repoDomainSource -Filter "*.kt" | ForEach-Object {
        Copy-Item -Path $_.FullName -Destination (Join-Path $repoDomainTarget $_.Name) -Force
        Write-Host "  Movido: $($_.Name)" -ForegroundColor Green
    }
}

# Mover repositories principais
Write-Host "Movendo repositories principais..." -ForegroundColor Yellow
$repoSource = "app\src\main\java\com\example\gestaobilhares\data\repository"
$repoTarget = "data\src\main\java\com\example\gestaobilhares\data\repository"

if (Test-Path $repoSource) {
    Get-ChildItem -Path $repoSource -Filter "*.kt" | Where-Object { $_.Name -notlike "*domain*" } | ForEach-Object {
        Copy-Item -Path $_.FullName -Destination (Join-Path $repoTarget $_.Name) -Force
        Write-Host "  Movido: $($_.Name)" -ForegroundColor Green
    }
}

# Mover factory
Write-Host "Movendo factory..." -ForegroundColor Yellow
$factorySource = "app\src\main\java\com\example\gestaobilhares\data\factory"
$factoryTarget = "data\src\main\java\com\example\gestaobilhares\data\factory"

if (Test-Path $factorySource) {
    Get-ChildItem -Path $factorySource -Filter "*.kt" | ForEach-Object {
        Copy-Item -Path $_.FullName -Destination (Join-Path $factoryTarget $_.Name) -Force
        Write-Host "  Movido: $($_.Name)" -ForegroundColor Green
    }
}

# Mover model
Write-Host "Movendo model..." -ForegroundColor Yellow
$modelSource = "app\src\main\java\com\example\gestaobilhares\data\model"
$modelTarget = "data\src\main\java\com\example\gestaobilhares\data\model"

if (Test-Path $modelSource) {
    Get-ChildItem -Path $modelSource -Filter "*.kt" | ForEach-Object {
        Copy-Item -Path $_.FullName -Destination (Join-Path $modelTarget $_.Name) -Force
        Write-Host "  Movido: $($_.Name)" -ForegroundColor Green
    }
}

# Atualizar namespaces
Write-Host "Atualizando namespaces..." -ForegroundColor Yellow
$dataPaths = @(
    "data\src\main\java\com\example\gestaobilhares\data\entities",
    "data\src\main\java\com\example\gestaobilhares\data\dao",
    "data\src\main\java\com\example\gestaobilhares\data\database",
    "data\src\main\java\com\example\gestaobilhares\data\repository",
    "data\src\main\java\com\example\gestaobilhares\data\factory",
    "data\src\main\java\com\example\gestaobilhares\data\model"
)

$updatedCount = 0
foreach ($path in $dataPaths) {
    if (Test-Path $path) {
        Get-ChildItem -Path $path -Filter "*.kt" -Recurse | ForEach-Object {
            $content = Get-Content $_.FullName -Raw -Encoding UTF8
            $originalContent = $content
            
            # Atualizar package baseado no caminho
            if ($_.FullName -like "*\entities\*") {
                $content = $content -replace 'package com\.example\.gestaobilhares\.data\.entities', 'package com.example.gestaobilhares.data.entities'
            }
            elseif ($_.FullName -like "*\dao\*") {
                $content = $content -replace 'package com\.example\.gestaobilhares\.data\.dao', 'package com.example.gestaobilhares.data.dao'
            }
            elseif ($_.FullName -like "*\database\*") {
                $content = $content -replace 'package com\.example\.gestaobilhares\.data\.database', 'package com.example.gestaobilhares.data.database'
            }
            elseif ($_.FullName -like "*\repository\domain\*") {
                $content = $content -replace 'package com\.example\.gestaobilhares\.data\.repository\.domain', 'package com.example.gestaobilhares.data.repository.domain'
            }
            elseif ($_.FullName -like "*\repository\*") {
                $content = $content -replace 'package com\.example\.gestaobilhares\.data\.repository', 'package com.example.gestaobilhares.data.repository'
            }
            elseif ($_.FullName -like "*\factory\*") {
                $content = $content -replace 'package com\.example\.gestaobilhares\.data\.factory', 'package com.example.gestaobilhares.data.factory'
            }
            elseif ($_.FullName -like "*\model\*") {
                $content = $content -replace 'package com\.example\.gestaobilhares\.data\.model', 'package com.example.gestaobilhares.data.model'
            }
            
            # Atualizar imports de utils para core.utils
            $content = $content -replace 'import com\.example\.gestaobilhares\.utils\.', 'import com.example.gestaobilhares.core.utils.'
            
            if ($content -ne $originalContent) {
                Set-Content -Path $_.FullName -Value $content -Encoding UTF8 -NoNewline
                $updatedCount++
            }
        }
    }
}

Write-Host "Namespaces e imports atualizados: $updatedCount arquivos" -ForegroundColor Cyan
Write-Host "=== MIGRACAO :data CONCLUIDA ===" -ForegroundColor Green

