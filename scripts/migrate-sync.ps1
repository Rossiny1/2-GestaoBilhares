# Script para migracao do modulo :sync
Write-Host "=== MIGRACAO MODULO :sync ===" -ForegroundColor Cyan

# Criar estrutura de diretórios
Write-Host "Criando estrutura de diretórios..." -ForegroundColor Yellow
New-Item -ItemType Directory -Path "sync\src\main\java\com\example\gestaobilhares\sync" -Force | Out-Null
New-Item -ItemType Directory -Path "sync\src\main\java\com\example\gestaobilhares\workers" -Force | Out-Null

# Mover SyncRepository
Write-Host "Movendo SyncRepository..." -ForegroundColor Yellow
$syncRepoSource = "app\src\main\java\com\example\gestaobilhares\data\repository\domain\SyncRepository.kt"
$syncRepoTarget = "sync\src\main\java\com\example\gestaobilhares\sync\SyncRepository.kt"

if (Test-Path $syncRepoSource) {
    Copy-Item -Path $syncRepoSource -Destination $syncRepoTarget -Force
    Write-Host "  Movido: SyncRepository.kt" -ForegroundColor Green
}

# Mover SyncManager (de utils)
Write-Host "Movendo SyncManager..." -ForegroundColor Yellow
$syncManagerSource = "app\src\main\java\com\example\gestaobilhares\utils\SyncManager.kt"
$syncManagerTarget = "sync\src\main\java\com\example\gestaobilhares\sync\SyncManager.kt"

if (Test-Path $syncManagerSource) {
    Copy-Item -Path $syncManagerSource -Destination $syncManagerTarget -Force
    Write-Host "  Movido: SyncManager.kt" -ForegroundColor Green
}

# Mover SyncWorker
Write-Host "Movendo SyncWorker..." -ForegroundColor Yellow
$syncWorkerSource = "app\src\main\java\com\example\gestaobilhares\workers\SyncWorker.kt"
$syncWorkerTarget = "sync\src\main\java\com\example\gestaobilhares\workers\SyncWorker.kt"

if (Test-Path $syncWorkerSource) {
    Copy-Item -Path $syncWorkerSource -Destination $syncWorkerTarget -Force
    Write-Host "  Movido: SyncWorker.kt" -ForegroundColor Green
}

# Atualizar namespaces e imports
Write-Host "Atualizando namespaces e imports..." -ForegroundColor Yellow
$syncFiles = @(
    "sync\src\main\java\com\example\gestaobilhares\sync\SyncRepository.kt",
    "sync\src\main\java\com\example\gestaobilhares\sync\SyncManager.kt",
    "sync\src\main\java\com\example\gestaobilhares\workers\SyncWorker.kt"
)

$updatedCount = 0
foreach ($file in $syncFiles) {
    if (Test-Path $file) {
        $content = Get-Content $file -Raw -Encoding UTF8
        $originalContent = $content
        
        # Atualizar package
        if ($file -like "*\sync\SyncRepository.kt") {
            $content = $content -replace 'package com\.example\.gestaobilhares\.data\.repository\.domain', 'package com.example.gestaobilhares.sync'
        }
        elseif ($file -like "*\sync\SyncManager.kt") {
            $content = $content -replace 'package com\.example\.gestaobilhares\.utils', 'package com.example.gestaobilhares.sync'
        }
        elseif ($file -like "*\workers\SyncWorker.kt") {
            $content = $content -replace 'package com\.example\.gestaobilhares\.workers', 'package com.example.gestaobilhares.workers'
        }
        
        # Atualizar imports
        $content = $content -replace 'import com\.example\.gestaobilhares\.data\.', 'import com.example.gestaobilhares.data.'
        $content = $content -replace 'import com\.example\.gestaobilhares\.utils\.', 'import com.example.gestaobilhares.core.utils.'
        
        if ($content -ne $originalContent) {
            Set-Content -Path $file -Value $content -Encoding UTF8 -NoNewline
            $updatedCount++
            Write-Host "  Atualizado: $(Split-Path $file -Leaf)" -ForegroundColor Green
        }
    }
}

Write-Host "Arquivos atualizados: $updatedCount" -ForegroundColor Cyan
Write-Host "=== MIGRACAO :sync CONCLUIDA ===" -ForegroundColor Green

