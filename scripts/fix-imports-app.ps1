# Script para corrigir imports no modulo :app (arquivos que ainda estao la)
Write-Host "=== CORRIGINDO IMPORTS NO MODULO :app ===" -ForegroundColor Cyan

# Atualizar imports em arquivos que ainda estao no app/src
$appPaths = @(
    "app\src\main\java\com\example\gestaobilhares\ui",
    "app\src\main\java\com\example\gestaobilhares\data",
    "app\src\main\java\com\example\gestaobilhares\utils"
)

$updatedCount = 0
foreach ($path in $appPaths) {
    if (Test-Path $path) {
        Get-ChildItem -Path $path -Filter "*.kt" -Recurse | ForEach-Object {
            $content = Get-Content $_.FullName -Raw -Encoding UTF8
            $originalContent = $content
            
            # Atualizar imports de utils para core.utils
            $content = $content -replace 'import com\.example\.gestaobilhares\.utils\.', 'import com.example.gestaobilhares.core.utils.'
            
            # Atualizar imports de data
            $content = $content -replace 'import com\.example\.gestaobilhares\.data\.', 'import com.example.gestaobilhares.data.'
            
            # Atualizar imports de sync
            $content = $content -replace 'import com\.example\.gestaobilhares\.sync\.', 'import com.example.gestaobilhares.sync.'
            $content = $content -replace 'import com\.example\.gestaobilhares\.workers\.', 'import com.example.gestaobilhares.workers.'
            
            if ($content -ne $originalContent) {
                Set-Content -Path $_.FullName -Value $content -Encoding UTF8 -NoNewline
                $updatedCount++
            }
        }
    }
}

Write-Host "Arquivos atualizados: $updatedCount" -ForegroundColor Cyan
Write-Host "=== IMPORTS DO MODULO :app CORRIGIDOS ===" -ForegroundColor Green

