# Script para mover arquivos e corrigir referências
$ErrorActionPreference = "Continue"

$rootPath = Split-Path -Parent $PSScriptRoot
$sourcePath = Join-Path $rootPath "ui\src\main\java\com\example\gestaobilhares\ui\c\main\java\com\example\gestaobilhares\ui"
$targetPath = Join-Path $rootPath "ui\src\main\java\com\example\gestaobilhares\ui"

Write-Host "Movendo arquivos..." -ForegroundColor Yellow

if (Test-Path $sourcePath) {
    $files = Get-ChildItem -Path $sourcePath -Recurse -File
    
    foreach ($file in $files) {
        $relativePath = $file.FullName.Substring($sourcePath.Length + 1)
        $destFile = Join-Path $targetPath $relativePath
        $destDir = Split-Path $destFile -Parent
        
        if (-not (Test-Path $destDir)) {
            New-Item -ItemType Directory -Path $destDir -Force | Out-Null
        }
        
        Copy-Item -Path $file.FullName -Destination $destFile -Force
    }
    
    Write-Host "Removendo estrutura duplicada..." -ForegroundColor Yellow
    Remove-Item -Path (Join-Path $rootPath "ui\src\main\java\com\example\gestaobilhares\ui\c") -Recurse -Force -ErrorAction SilentlyContinue
}

Write-Host "Corrigindo referências..." -ForegroundColor Yellow

$uiPath = Join-Path $rootPath "ui\src\main\java"
$files = Get-ChildItem -Path $uiPath -Filter "*.kt" -Recurse

$jobs = @()
foreach ($file in $files) {
    $job = Start-Job -ScriptBlock {
        param($filePath, $replacements)
        
        $content = Get-Content $filePath -Raw -Encoding UTF8
        $original = $content
        
        foreach ($rep in $replacements) {
            $content = $content -replace $rep[0], $rep[1]
        }
        
        if ($content -ne $original) {
            Set-Content -Path $filePath -Value $content -Encoding UTF8 -NoNewline
            return 1
        }
        return 0
    } -ArgumentList $file.FullName, @(
        @('^import com\.example\.gestaobilhares\.R$', 'import com.example.gestaobilhares.ui.R'),
        @('^import com\.example\.gestaobilhares\.databinding\.', 'import com.example.gestaobilhares.ui.databinding.'),
        @('import com\.jakewharton\.timber\.Timber', 'import timber.log.Timber'),
        @('\bR\.id\.', 'com.example.gestaobilhares.ui.R.id.'),
        @('\bR\.string\.', 'com.example.gestaobilhares.ui.R.string.'),
        @('\bR\.color\.', 'com.example.gestaobilhares.ui.R.color.'),
        @('\bR\.drawable\.', 'com.example.gestaobilhares.ui.R.drawable.'),
        @('\bR\.layout\.', 'com.example.gestaobilhares.ui.R.layout.'),
        @('\bR\.plurals\.', 'com.example.gestaobilhares.ui.R.plurals.'),
        @('\bR\.navigation\.', 'com.example.gestaobilhares.ui.R.navigation.'),
        @('\bR\.sync\.', 'com.example.gestaobilhares.ui.R.sync.'),
        @('\bR\.nav_', 'com.example.gestaobilhares.ui.R.nav_')
    )
    
    $jobs += $job
    
    if ($jobs.Count -ge 50) {
        $jobs | Wait-Job | Receive-Job
        $jobs = @()
    }
}

if ($jobs.Count -gt 0) {
    $jobs | Wait-Job | Receive-Job
}

Write-Host "Concluído!" -ForegroundColor Green

