# Script para corrigir imports no modulo :core
Write-Host "=== CORRIGINDO IMPORTS NO MODULO :core ===" -ForegroundColor Cyan

$corePath = "core\src\main\java\com\example\gestaobilhares\utils"
$updatedCount = 0

if (Test-Path $corePath) {
    Get-ChildItem -Path $corePath -Filter "*.kt" -Recurse | ForEach-Object {
        $content = Get-Content $_.FullName -Raw -Encoding UTF8
        $originalContent = $content
        
        # Corrigir imports de utils para core.utils
        $content = $content -replace 'import com\.example\.gestaobilhares\.utils\.', 'import com.example.gestaobilhares.core.utils.'
        
        # Corrigir referencias a utils para core.utils
        $content = $content -replace 'com\.example\.gestaobilhares\.utils\.', 'com.example.gestaobilhares.core.utils.'
        
        # Corrigir import de Timber
        $content = $content -replace 'import timber\.log\.Timber', 'import com.jakewharton.timber.Timber'
        
        # Remover import de R (recursos Android nao existem no core)
        $content = $content -replace 'import com\.example\.gestaobilhares\.R\s*\n', ''
        $content = $content -replace 'import com\.example\.gestaobilhares\.data\.R\s*\n', ''
        
        # Corrigir imports de data.entities (manter como esta)
        # Nao precisa mudar, ja esta correto
        
        if ($content -ne $originalContent) {
            Set-Content -Path $_.FullName -Value $content -Encoding UTF8 -NoNewline
            $updatedCount++
            Write-Host "  Corrigido: $($_.Name)" -ForegroundColor Green
        }
    }
}

Write-Host "Arquivos corrigidos: $updatedCount" -ForegroundColor Cyan
Write-Host "=== IMPORTS DO MODULO :core CORRIGIDOS ===" -ForegroundColor Green

