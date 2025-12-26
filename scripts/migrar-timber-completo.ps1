# Script para migrar TODOS os logs de android.util.Log para Timber
# Uso: .\scripts\migrar-timber-completo.ps1

Write-Host "🔍 Migrando todos os logs para Timber..." -ForegroundColor Cyan

# Encontrar todos os arquivos .kt que contêm android.util.Log
$files = Get-ChildItem -Path . -Recurse -Filter "*.kt" | Where-Object {
    $content = Get-Content $_.FullName -Raw -ErrorAction SilentlyContinue
    $content -match "android\.util\.Log\."
} | Where-Object {
    # Excluir arquivos de teste e documentação
    $_.FullName -notmatch "\\test\\" -and 
    $_.FullName -notmatch "\\documentation\\" -and
    $_.FullName -notmatch "CrashlyticsTree\.kt"  # Este precisa manter Log para constantes
}

Write-Host "📁 Encontrados $($files.Count) arquivos para processar" -ForegroundColor Yellow

$totalReplacements = 0

foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw
    $originalContent = $content
    $fileReplacements = 0
    
    # 1. Substituir import
    if ($content -match "import android\.util\.Log") {
        $content = $content -replace 'import android\.util\.Log', 'import timber.log.Timber'
        $fileReplacements++
    }
    
    # 2. Substituir Log.d(TAG, "message") por Timber.tag(TAG).d("message")
    $matches = [regex]::Matches($content, 'Log\.d\(([^,]+),\s*"([^"]+)"\)')
    foreach ($match in $matches) {
        $tag = $match.Groups[1].Value.Trim()
        $message = $match.Groups[2].Value
        $oldPattern = [regex]::Escape($match.Value)
        $newPattern = "Timber.tag($tag).d(`"$message`")"
        $content = $content -replace $oldPattern, $newPattern
        $fileReplacements++
    }
    
    # 3. Substituir Log.d(TAG, message) por Timber.tag(TAG).d(message) - sem aspas
    $matches = [regex]::Matches($content, 'Log\.d\(([^,]+),\s*([^)]+)\)')
    foreach ($match in $matches) {
        $tag = $match.Groups[1].Value.Trim()
        $message = $match.Groups[2].Value.Trim()
        if ($message -notmatch '^".*"$') {  # Se não está entre aspas
            $oldPattern = [regex]::Escape($match.Value)
            $newPattern = "Timber.tag($tag).d($message)"
            $content = $content -replace $oldPattern, $newPattern
            $fileReplacements++
        }
    }
    
    # 4. Substituir Log.e(TAG, "message", exception) por Timber.tag(TAG).e(exception, "message")
    $matches = [regex]::Matches($content, 'Log\.e\(([^,]+),\s*"([^"]+)",\s*([^)]+)\)')
    foreach ($match in $matches) {
        $tag = $match.Groups[1].Value.Trim()
        $message = $match.Groups[2].Value
        $exception = $match.Groups[3].Value.Trim()
        $oldPattern = [regex]::Escape($match.Value)
        $newPattern = "Timber.tag($tag).e($exception, `"$message`")"
        $content = $content -replace $oldPattern, $newPattern
        $fileReplacements++
    }
    
    # 5. Substituir Log.e(TAG, "message") por Timber.tag(TAG).e("message")
    $matches = [regex]::Matches($content, 'Log\.e\(([^,]+),\s*"([^"]+)"\)')
    foreach ($match in $matches) {
        $tag = $match.Groups[1].Value.Trim()
        $message = $match.Groups[2].Value
        $oldPattern = [regex]::Escape($match.Value)
        $newPattern = "Timber.tag($tag).e(`"$message`")"
        $content = $content -replace $oldPattern, $newPattern
        $fileReplacements++
    }
    
    # 6. Substituir Log.w(TAG, "message") por Timber.tag(TAG).w("message")
    $matches = [regex]::Matches($content, 'Log\.w\(([^,]+),\s*"([^"]+)"\)')
    foreach ($match in $matches) {
        $tag = $match.Groups[1].Value.Trim()
        $message = $match.Groups[2].Value
        $oldPattern = [regex]::Escape($match.Value)
        $newPattern = "Timber.tag($tag).w(`"$message`")"
        $content = $content -replace $oldPattern, $newPattern
        $fileReplacements++
    }
    
    # 7. Substituir Log.w(TAG, message) por Timber.tag(TAG).w(message) - sem aspas
    $matches = [regex]::Matches($content, 'Log\.w\(([^,]+),\s*([^)]+)\)')
    foreach ($match in $matches) {
        $tag = $match.Groups[1].Value.Trim()
        $message = $match.Groups[2].Value.Trim()
        if ($message -notmatch '^".*"$') {
            $oldPattern = [regex]::Escape($match.Value)
            $newPattern = "Timber.tag($tag).w($message)"
            $content = $content -replace $oldPattern, $newPattern
            $fileReplacements++
        }
    }
    
    # 8. Substituir Log.i(TAG, "message") por Timber.tag(TAG).i("message")
    $matches = [regex]::Matches($content, 'Log\.i\(([^,]+),\s*"([^"]+)"\)')
    foreach ($match in $matches) {
        $tag = $match.Groups[1].Value.Trim()
        $message = $match.Groups[2].Value
        $oldPattern = [regex]::Escape($match.Value)
        $newPattern = "Timber.tag($tag).i(`"$message`")"
        $content = $content -replace $oldPattern, $newPattern
        $fileReplacements++
    }
    
    # 9. Substituir Log.v(TAG, "message") por Timber.tag(TAG).v("message")
    $matches = [regex]::Matches($content, 'Log\.v\(([^,]+),\s*"([^"]+)"\)')
    foreach ($match in $matches) {
        $tag = $match.Groups[1].Value.Trim()
        $message = $match.Groups[2].Value
        $oldPattern = [regex]::Escape($match.Value)
        $newPattern = "Timber.tag($tag).v(`"$message`")"
        $content = $content -replace $oldPattern, $newPattern
        $fileReplacements++
    }
    
    # 10. Substituir android.util.Log.d por Timber.tag(TAG).d (caso tenha android.util. explícito)
    $content = $content -replace 'android\.util\.Log\.d\(', 'Timber.tag(TAG).d('
    $content = $content -replace 'android\.util\.Log\.e\(', 'Timber.tag(TAG).e('
    $content = $content -replace 'android\.util\.Log\.w\(', 'Timber.tag(TAG).w('
    $content = $content -replace 'android\.util\.Log\.i\(', 'Timber.tag(TAG).i('
    $content = $content -replace 'android\.util\.Log\.v\(', 'Timber.tag(TAG).v('
    
    if ($content -ne $originalContent) {
        Set-Content -Path $file.FullName -Value $content -NoNewline -Encoding UTF8
        Write-Host "✅ $($file.Name): $fileReplacements substituições" -ForegroundColor Green
        $totalReplacements += $fileReplacements
    }
}

Write-Host "`n✨ Concluído! Total de substituições: $totalReplacements" -ForegroundColor Cyan
Write-Host "⚠️  IMPORTANTE: Revise os arquivos modificados para garantir que os logs estão corretos!" -ForegroundColor Yellow
Write-Host "📝 Alguns logs podem precisar de ajuste manual, especialmente os com interpolação de strings complexas." -ForegroundColor Yellow

