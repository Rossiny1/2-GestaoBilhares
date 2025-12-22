# Script para substituir android.util.Log por Timber
# Uso: .\scripts\substituir-logs.ps1

Write-Host "🔍 Procurando arquivos com android.util.Log..." -ForegroundColor Cyan

# Encontrar todos os arquivos .kt que contêm android.util.Log
$files = Get-ChildItem -Path . -Recurse -Filter "*.kt" | Where-Object {
    $content = Get-Content $_.FullName -Raw -ErrorAction SilentlyContinue
    $content -match "android\.util\.Log\."
}

Write-Host "📁 Encontrados $($files.Count) arquivos para processar" -ForegroundColor Yellow

$totalReplacements = 0

foreach ($file in $files) {
    $content = Get-Content $_.FullName -Raw
    $originalContent = $content
    
    # Substituir imports
    $content = $content -replace 'import android\.util\.Log', 'import timber.log.Timber'
    
    # Substituir Log.d por Timber.d
    $content = $content -replace 'android\.util\.Log\.d\(', 'Timber.d('
    
    # Substituir Log.e por Timber.e
    $content = $content -replace 'android\.util\.Log\.e\(', 'Timber.e('
    
    # Substituir Log.w por Timber.w
    $content = $content -replace 'android\.util\.Log\.w\(', 'Timber.w('
    
    # Substituir Log.i por Timber.i
    $content = $content -replace 'android\.util\.Log\.i\(', 'Timber.i('
    
    # Substituir Log.v por Timber.v
    $content = $content -replace 'android\.util\.Log\.v\(', 'Timber.v('
    
    if ($content -ne $originalContent) {
        $replacements = ([regex]::Matches($originalContent, 'android\.util\.Log\.')).Count
        $totalReplacements += $replacements
        
        Set-Content -Path $file.FullName -Value $content -NoNewline
        Write-Host "✅ $($file.Name): $replacements substituições" -ForegroundColor Green
    }
}

Write-Host "`n✨ Concluído! Total de substituições: $totalReplacements" -ForegroundColor Cyan
Write-Host "⚠️  IMPORTANTE: Revise os arquivos modificados para garantir que os logs estão corretos!" -ForegroundColor Yellow

