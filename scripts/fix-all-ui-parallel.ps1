# Script paralelo para corrigir estrutura e imports do módulo :ui
Write-Host "=== CORREÇÃO PARALELA DO MÓDULO :ui ===" -ForegroundColor Cyan

$rootPath = Split-Path -Parent $PSScriptRoot
$wrongPath = Join-Path $rootPath "ui\src\main\java\com\example\gestaobilhares\ui\c\main\java\com\example\gestaobilhares\ui"
$correctPath = Join-Path $rootPath "ui\src\main\java\com\example\gestaobilhares\ui"

# FASE 1: Mover arquivos em paralelo
if (Test-Path $wrongPath) {
    Write-Host "Fase 1: Movendo arquivos..." -ForegroundColor Yellow
    $files = Get-ChildItem -Path $wrongPath -Recurse -File
    
    $files | ForEach-Object -Parallel {
        $file = $_
        $wrongPath = $using:wrongPath
        $correctPath = $using:correctPath
        
        $relativePath = $file.FullName.Substring($wrongPath.Length + 1)
        $targetPath = Join-Path $correctPath $relativePath
        $targetDir = Split-Path $targetPath -Parent
        
        if (-not (Test-Path $targetDir)) {
            New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
        }
        
        if (-not (Test-Path $targetPath) -or $file.LastWriteTime -gt (Get-Item $targetPath).LastWriteTime) {
            Copy-Item -Path $file.FullName -Destination $targetPath -Force
        }
    } -ThrottleLimit 20
    
    Write-Host "Arquivos movidos. Removendo estrutura duplicada..." -ForegroundColor Green
    Remove-Item -Path (Join-Path $rootPath "ui\src\main\java\com\example\gestaobilhares\ui\c") -Recurse -Force -ErrorAction SilentlyContinue
}

# FASE 2: Corrigir imports e referências em paralelo
Write-Host "`nFase 2: Corrigindo imports e referências em paralelo..." -ForegroundColor Yellow

$uiPath = Join-Path $rootPath "ui\src\main\java"
$files = Get-ChildItem -Path $uiPath -Filter "*.kt" -Recurse

$replacements = @(
    @{Pattern='^import com\.example\.gestaobilhares\.R$'; Replacement='import com.example.gestaobilhares.ui.R'},
    @{Pattern='^import com\.example\.gestaobilhares\.databinding\.'; Replacement='import com.example.gestaobilhares.ui.databinding.'},
    @{Pattern='import com\.jakewharton\.timber\.Timber'; Replacement='import timber.log.Timber'},
    @{Pattern='\bR\.id\.'; Replacement='com.example.gestaobilhares.ui.R.id.'},
    @{Pattern='\bR\.string\.'; Replacement='com.example.gestaobilhares.ui.R.string.'},
    @{Pattern='\bR\.color\.'; Replacement='com.example.gestaobilhares.ui.R.color.'},
    @{Pattern='\bR\.drawable\.'; Replacement='com.example.gestaobilhares.ui.R.drawable.'},
    @{Pattern='\bR\.layout\.'; Replacement='com.example.gestaobilhares.ui.R.layout.'},
    @{Pattern='\bR\.plurals\.'; Replacement='com.example.gestaobilhares.ui.R.plurals.'},
    @{Pattern='\bR\.navigation\.'; Replacement='com.example.gestaobilhares.ui.R.navigation.'},
    @{Pattern='\bR\.sync\.'; Replacement='com.example.gestaobilhares.ui.R.sync.'},
    @{Pattern='\bR\.nav_'; Replacement='com.example.gestaobilhares.ui.R.nav_'},
    @{Pattern='com\.example\.gestaobilhares\.ui\.reports\.ClosureReportPdfGenerator'; Replacement='com.example.gestaobilhares.ui.reports.ClosureReportPdfGenerator'}
)

$updatedCount = 0
$files | ForEach-Object -Parallel {
    $file = $_
    $replacements = $using:replacements
    
    try {
        $content = Get-Content $file.FullName -Raw -Encoding UTF8
        $originalContent = $content
        
        foreach ($rep in $replacements) {
            $content = $content -replace $rep.Pattern, $rep.Replacement
        }
        
        # Adicionar import de R.ui se usar R mas não tiver import
        if ($content -match '\bcom\.example\.gestaobilhares\.ui\.R\.' -and $content -notmatch 'import com\.example\.gestaobilhares\.ui\.R') {
            $importLine = "import com.example.gestaobilhares.ui.R`n"
            if ($content -match '(package\s+[^\n]+\n)') {
                $content = $content -replace '(package\s+[^\n]+\n)', "`$1$importLine"
            } else {
                $content = $importLine + $content
            }
        }
        
        if ($content -ne $originalContent) {
            Set-Content -Path $file.FullName -Value $content -Encoding UTF8 -NoNewline
            return 1
        }
        return 0
    } catch {
        return 0
    }
} -ThrottleLimit 30 | ForEach-Object { $updatedCount += $_ }

Write-Host "`nArquivos atualizados: $updatedCount" -ForegroundColor Green
Write-Host "=== CORREÇÃO CONCLUÍDA ===" -ForegroundColor Green

