# Script para corrigir todos os erros críticos do módulo :ui

Write-Host "Corrigindo erros críticos do módulo :ui..." -ForegroundColor Cyan

$uiPath = "ui/src/main/java/com/example/gestaobilhares/ui"

# 1. Corrigir referências incorretas ao factory
Write-Host "1. Corrigindo referências ao factory..." -ForegroundColor Yellow
$files = @(
    "clients/ClientRegisterFragment.kt",
    "clients/CycleHistoryFragment.kt",
    "colaboradores/ColaboradorManagementFragment.kt",
    "colaboradores/ColaboradorMetasFragment.kt",
    "colaboradores/ColaboradorRegisterFragment.kt"
)

foreach ($file in $files) {
    $fullPath = Join-Path $uiPath $file
    if (Test-Path $fullPath) {
        $content = Get-Content $fullPath -Raw -Encoding UTF8
        $original = $content
        $content = $content -replace 'com\.example\.gestaobilhares\.core\.factory', 'com.example.gestaobilhares.factory'
        if ($content -ne $original) {
            Set-Content -Path $fullPath -Value $content -Encoding UTF8 -NoNewline
            Write-Host "  Corrigido: $file" -ForegroundColor Green
        }
    }
}

# 2. Corrigir imports incorretos
Write-Host "2. Corrigindo imports..." -ForegroundColor Yellow
Get-ChildItem -Path $uiPath -Filter "*.kt" -Recurse | ForEach-Object {
    $content = Get-Content $_.FullName -Raw -Encoding UTF8
    $original = $content
    $content = $content -replace 'import com\.example\.gestaobilhares\.core\.factory\.RepositoryFactory', 'import com.example.gestaobilhares.factory.RepositoryFactory'
    if ($content -ne $original) {
        Set-Content -Path $_.FullName -Value $content -Encoding UTF8 -NoNewline
        Write-Host "  Corrigido: $($_.Name)" -ForegroundColor Green
    }
}

Write-Host "`nCorreções concluídas!" -ForegroundColor Green

