# Script para corrigir SyncManager
Write-Host "=== CORRIGINDO SyncManager ===" -ForegroundColor Cyan

$file = "sync\src\main\java\com\example\gestaobilhares\sync\SyncManager.kt"

if (Test-Path $file) {
    $content = Get-Content $file -Raw -Encoding UTF8
    $originalContent = $content
    
    # Corrigir import de SyncWorker (workers esta dentro de sync agora)
    $content = $content -replace 'import com\.example\.gestaobilhares\.workers\.SyncWorker', 'import com.example.gestaobilhares.workers.SyncWorker'
    
    if ($content -ne $originalContent) {
        Set-Content -Path $file -Value $content -Encoding UTF8 -NoNewline
        Write-Host "SyncManager corrigido" -ForegroundColor Green
    } else {
        Write-Host "SyncManager ja estava correto" -ForegroundColor Yellow
    }
} else {
    Write-Host "SyncManager nao encontrado" -ForegroundColor Red
}

Write-Host "=== SyncManager CORRIGIDO ===" -ForegroundColor Green

