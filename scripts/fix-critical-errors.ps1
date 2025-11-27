# Script para corrigir erros críticos do módulo :ui em paralelo

Write-Host "Corrigindo erros críticos do módulo :ui..." -ForegroundColor Cyan

$uiPath = "ui/src/main/java/com/example/gestaobilhares/ui"

# 1. Corrigir AppLogger.log para android.util.Log.d
Write-Host "1. Corrigindo AppLogger.log..." -ForegroundColor Yellow
Get-ChildItem -Path $uiPath -Filter "*.kt" -Recurse | ForEach-Object {
    $content = Get-Content $_.FullName -Raw -Encoding UTF8
    if ($content -match "AppLogger\.log") {
        $content = $content -replace 'AppLogger\.log\("([^"]+)",\s*"([^"]+)"\)', 'android.util.Log.d("$1", "$2")'
        Set-Content -Path $_.FullName -Value $content -Encoding UTF8 -NoNewline
        Write-Host "  Corrigido: $($_.Name)" -ForegroundColor Green
    }
}

# 2. Corrigir referências incorretas a factory
Write-Host "2. Corrigindo referências a factory..." -ForegroundColor Yellow
Get-ChildItem -Path $uiPath -Filter "*.kt" -Recurse | ForEach-Object {
    $content = Get-Content $_.FullName -Raw -Encoding UTF8
    $changed = $false
    
    # Corrigir com.example.gestaobilhares.core.factory para com.example.gestaobilhares.factory
    if ($content -match "com\.example\.gestaobilhares\.core\.factory") {
        $content = $content -replace 'com\.example\.gestaobilhares\.core\.factory', 'com.example.gestaobilhares.factory'
        $changed = $true
    }
    
    if ($changed) {
        Set-Content -Path $_.FullName -Value $content -Encoding UTF8 -NoNewline
        Write-Host "  Corrigido: $($_.Name)" -ForegroundColor Green
    }
}

# 3. Corrigir imports de AppLogger
Write-Host "3. Removendo imports de AppLogger..." -ForegroundColor Yellow
Get-ChildItem -Path $uiPath -Filter "*.kt" -Recurse | ForEach-Object {
    $content = Get-Content $_.FullName -Raw -Encoding UTF8
    if ($content -match "import.*AppLogger") {
        $content = $content -replace '(?m)^import\s+.*AppLogger.*\r?\n', ''
        Set-Content -Path $_.FullName -Value $content -Encoding UTF8 -NoNewline
        Write-Host "  Corrigido: $($_.Name)" -ForegroundColor Green
    }
}

Write-Host "`nCorreções concluídas!" -ForegroundColor Green

