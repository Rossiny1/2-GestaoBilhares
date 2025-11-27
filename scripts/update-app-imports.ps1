# Script para atualizar imports no modulo :app
Write-Host "=== ATUALIZANDO IMPORTS NO MODULO :app ===" -ForegroundColor Cyan

$appFiles = @(
    "app\src\main\java\com\example\gestaobilhares\MainActivity.kt",
    "app\src\main\java\com\example\gestaobilhares\GestaoBilharesApplication.kt",
    "app\src\main\java\com\example\gestaobilhares\notification\NotificationService.kt"
)

$updatedCount = 0
foreach ($file in $appFiles) {
    if (Test-Path $file) {
        $content = Get-Content $file -Raw -Encoding UTF8
        $originalContent = $content
        
        # Atualizar imports
        $content = $content -replace 'import com\.example\.gestaobilhares\.data\.', 'import com.example.gestaobilhares.data.'
        $content = $content -replace 'import com\.example\.gestaobilhares\.utils\.', 'import com.example.gestaobilhares.core.utils.'
        $content = $content -replace 'import com\.example\.gestaobilhares\.ui\.', 'import com.example.gestaobilhares.ui.'
        $content = $content -replace 'import com\.example\.gestaobilhares\.sync\.', 'import com.example.gestaobilhares.sync.'
        
        if ($content -ne $originalContent) {
            Set-Content -Path $file -Value $content -Encoding UTF8 -NoNewline
            $updatedCount++
            Write-Host "  Atualizado: $(Split-Path $file -Leaf)" -ForegroundColor Green
        }
    }
}

Write-Host "Arquivos atualizados: $updatedCount" -ForegroundColor Cyan
Write-Host "=== IMPORTS DO MODULO :app ATUALIZADOS ===" -ForegroundColor Green

