# Script para atualizar app/build.gradle.kts
Write-Host "=== ATUALIZANDO app/build.gradle.kts ===" -ForegroundColor Cyan

$buildFile = "app\build.gradle.kts"
$content = Get-Content $buildFile -Raw -Encoding UTF8

# Adicionar dependencias dos modulos apos a linha "dependencies {"
if ($content -notmatch 'implementation\(project\(":core"\)\)') {
    $content = $content -replace '(dependencies \{)', "`$1`n    // Modulos`n    implementation(project("":core""))`n    implementation(project("":data""))`n    implementation(project("":ui""))`n    implementation(project("":sync""))`n"
    Write-Host "Dependencias dos modulos adicionadas" -ForegroundColor Green
}

Set-Content -Path $buildFile -Value $content -Encoding UTF8 -NoNewline
Write-Host "=== app/build.gradle.kts ATUALIZADO ===" -ForegroundColor Green

