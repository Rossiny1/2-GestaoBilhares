# Script para substituir Log.* por Timber.*
# Este script ajuda a substituir todos os usos de Log por Timber

Write-Host "SUBSTITUINDO Log.* POR Timber.*" -ForegroundColor Cyan
Write-Host "====================================" -ForegroundColor Cyan
Write-Host ""

# Diretorios para processar
$directories = @(
    "app/src/main",
    "ui/src/main",
    "data/src/main",
    "sync/src/main",
    "core/src/main"
)

$totalFiles = 0
$totalReplacements = 0

foreach ($dir in $directories) {
    if (-not (Test-Path $dir)) {
        Write-Host "AVISO: Diretorio nao encontrado: $dir" -ForegroundColor Yellow
        continue
    }
    
    Write-Host "Processando: $dir" -ForegroundColor Gray
    
    # Buscar arquivos Kotlin
    $files = Get-ChildItem -Path $dir -Filter "*.kt" -Recurse
    
    foreach ($file in $files) {
        $content = Get-Content $file.FullName -Raw -Encoding UTF8
        $originalContent = $content
        $fileReplacements = 0
        
        # Substituicoes
        # 1. android.util.Log.d -> Timber.d
        if ($content -match "android\.util\.Log\.d") {
            $content = $content -replace "android\.util\.Log\.d\s*\(\s*([^,]+)\s*,\s*", "Timber.d("
            $fileReplacements++
        }
        
        # 2. Log.d -> Timber.d
        if ($content -match "\bLog\.d\s*\(") {
            $content = $content -replace "\bLog\.d\s*\(\s*([^,]+)\s*,\s*", "Timber.d("
            $fileReplacements++
        }
        
        # 3. android.util.Log.e -> Timber.e
        if ($content -match "android\.util\.Log\.e") {
            $content = $content -replace "android\.util\.Log\.e\s*\(\s*([^,]+)\s*,\s*", "Timber.e("
            $fileReplacements++
        }
        
        # 4. Log.e -> Timber.e (com exception)
        if ($content -match "\bLog\.e\s*\(\s*[^,]+,\s*[^,]+,\s*") {
            $content = $content -replace "\bLog\.e\s*\(\s*([^,]+)\s*,\s*([^,]+)\s*,\s*([^)]+)\)", "Timber.e(`$3, `$2)"
            $fileReplacements++
        }
        
        # 5. Log.e -> Timber.e (sem exception)
        if ($content -match "\bLog\.e\s*\(\s*[^,]+,\s*[^)]+\)") {
            $content = $content -replace "\bLog\.e\s*\(\s*([^,]+)\s*,\s*", "Timber.e("
            $fileReplacements++
        }
        
        # 6. Log.w -> Timber.w
        if ($content -match "\bLog\.w\s*\(") {
            $content = $content -replace "\bLog\.w\s*\(\s*([^,]+)\s*,\s*", "Timber.w("
            $fileReplacements++
        }
        
        # 7. Log.i -> Timber.i
        if ($content -match "\bLog\.i\s*\(") {
            $content = $content -replace "\bLog\.i\s*\(\s*([^,]+)\s*,\s*", "Timber.i("
            $fileReplacements++
        }
        
        # 8. Log.v -> Timber.v
        if ($content -match "\bLog\.v\s*\(") {
            $content = $content -replace "\bLog\.v\s*\(\s*([^,]+)\s*,\s*", "Timber.v("
            $fileReplacements++
        }
        
        # 9. Adicionar import Timber se necessario
        if ($fileReplacements -gt 0 -and $content -notmatch "import\s+timber\.log\.Timber") {
            # Adicionar import apos outros imports
            if ($content -match "(import\s+[^\r\n]+\r?\n)+") {
                $content = $content -replace "(import\s+[^\r\n]+\r?\n)+", "`$0import timber.log.Timber`r`n"
            } else {
                # Adicionar no inicio do arquivo apos package
                if ($content -match "(package\s+[^\r\n]+\r?\n)") {
                    $content = $content -replace "(package\s+[^\r\n]+\r?\n)", "`$1`r`nimport timber.log.Timber`r`n"
                }
            }
        }
        
        # 10. Remover import android.util.Log se nao houver mais usos
        if ($content -notmatch "\bLog\." -and $content -match "import\s+android\.util\.Log") {
            $content = $content -replace "import\s+android\.util\.Log\s*\r?\n", ""
        }
        
        # Salvar se houve mudancas
        if ($content -ne $originalContent) {
            Set-Content -Path $file.FullName -Value $content -Encoding UTF8 -NoNewline
            Write-Host "   OK: $($file.Name): $fileReplacements substituicao(oes)" -ForegroundColor Green
            $totalReplacements += $fileReplacements
            $totalFiles++
        }
    }
}

Write-Host ""
Write-Host "CONCLUIDO!" -ForegroundColor Green
Write-Host "   Arquivos modificados: $totalFiles" -ForegroundColor White
Write-Host "   Total de substituicoes: $totalReplacements" -ForegroundColor White
Write-Host ""
Write-Host "IMPORTANTE: Revise as mudancas manualmente!" -ForegroundColor Yellow
Write-Host "   Algumas substituicoes podem precisar de ajustes manuais" -ForegroundColor Yellow
Write-Host "   Especialmente Log.e() com excecoes (formato diferente)" -ForegroundColor Yellow
