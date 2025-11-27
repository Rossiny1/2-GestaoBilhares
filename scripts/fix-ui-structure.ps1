# Script para corrigir estrutura duplicada do módulo :ui
Write-Host "=== CORRIGINDO ESTRUTURA DO MÓDULO :ui ===" -ForegroundColor Cyan

$rootPath = Split-Path -Parent $PSScriptRoot
$wrongPath = Join-Path $rootPath "ui\src\main\java\com\example\gestaobilhares\ui\c\main\java\com\example\gestaobilhares\ui"
$correctPath = Join-Path $rootPath "ui\src\main\java\com\example\gestaobilhares\ui"

if (Test-Path $wrongPath) {
    Write-Host "Movendo arquivos da estrutura incorreta..." -ForegroundColor Yellow
    
    # Mover todos os arquivos recursivamente
    Get-ChildItem -Path $wrongPath -Recurse -File | ForEach-Object {
        $relativePath = $_.FullName.Substring($wrongPath.Length + 1)
        $targetPath = Join-Path $correctPath $relativePath
        $targetDir = Split-Path $targetPath -Parent
        
        if (-not (Test-Path $targetDir)) {
            New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
        }
        
        # Se o arquivo já existe, comparar e manter o mais recente
        if (Test-Path $targetPath) {
            $existingFile = Get-Item $targetPath
            if ($_.LastWriteTime -gt $existingFile.LastWriteTime) {
                Copy-Item -Path $_.FullName -Destination $targetPath -Force
                Write-Host "  Atualizado: $relativePath" -ForegroundColor Green
            }
        } else {
            Copy-Item -Path $_.FullName -Destination $targetPath -Force
            Write-Host "  Movido: $relativePath" -ForegroundColor Green
        }
    }
    
    # Remover estrutura incorreta
    Write-Host "Removendo estrutura duplicada..." -ForegroundColor Yellow
    Remove-Item -Path (Join-Path $rootPath "ui\src\main\java\com\example\gestaobilhares\ui\c") -Recurse -Force
    Write-Host "Estrutura corrigida!" -ForegroundColor Green
} else {
    Write-Host "Estrutura duplicada não encontrada. Verificando estrutura atual..." -ForegroundColor Yellow
}

Write-Host "`n=== CORRIGINDO IMPORTS E REFERÊNCIAS ===" -ForegroundColor Cyan

$uiPath = Join-Path $rootPath "ui\src\main\java"
$files = Get-ChildItem -Path $uiPath -Filter "*.kt" -Recurse

$replacements = @{
    'import com\.example\.gestaobilhares\.R' = 'import com.example.gestaobilhares.ui.R'
    'import com\.example\.gestaobilhares\.databinding\.' = 'import com.example.gestaobilhares.ui.databinding.'
    'import timber\.log\.Timber' = 'import timber.log.Timber'
    'import com\.jakewharton\.timber\.Timber' = 'import timber.log.Timber'
    'R\.id\.' = 'com.example.gestaobilhares.ui.R.id.'
    'R\.string\.' = 'com.example.gestaobilhares.ui.R.string.'
    'R\.color\.' = 'com.example.gestaobilhares.ui.R.color.'
    'R\.drawable\.' = 'com.example.gestaobilhares.ui.R.drawable.'
    'R\.layout\.' = 'com.example.gestaobilhares.ui.R.layout.'
    'R\.plurals\.' = 'com.example.gestaobilhares.ui.R.plurals.'
    'R\.navigation\.' = 'com.example.gestaobilhares.ui.R.navigation.'
    'R\.sync\.' = 'com.example.gestaobilhares.ui.R.sync.'
    'R\.nav_' = 'com.example.gestaobilhares.ui.R.nav_'
}

$updatedCount = 0
foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw -Encoding UTF8
    $originalContent = $content
    $fileUpdated = $false
    
    foreach ($pattern in $replacements.Keys) {
        if ($content -match $pattern) {
            $content = $content -replace $pattern, $replacements[$pattern]
            $fileUpdated = $true
        }
    }
    
    # Corrigir referências diretas a R sem import
    if ($content -match '\bR\.(id|string|color|drawable|layout|plurals|navigation|sync|nav_)\.' -and $content -notmatch 'import.*\.ui\.R') {
        $content = $content -replace '\bR\.(id|string|color|drawable|layout|plurals|navigation|sync|nav_)\.', 'com.example.gestaobilhares.ui.R.$1.'
        $fileUpdated = $true
    }
    
    if ($fileUpdated) {
        Set-Content -Path $file.FullName -Value $content -Encoding UTF8 -NoNewline
        $updatedCount++
    }
}

Write-Host "Arquivos atualizados: $updatedCount" -ForegroundColor Green
Write-Host "`n=== CORREÇÃO CONCLUÍDA ===" -ForegroundColor Green

