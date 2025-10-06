# Script para corrigir TODAS as referências problemáticas
Write-Host "🔧 CORRIGINDO TODAS AS REFERÊNCIAS PROBLEMÁTICAS" -ForegroundColor Yellow

# 1. Remover referências ao DAO removido
Write-Host "1. Removendo referências ao DAO removido..." -ForegroundColor Cyan
Get-ChildItem -Path "app\src\main\java" -Recurse -Filter "*.kt" | ForEach-Object {
    $content = Get-Content $_.FullName -Raw -Encoding UTF8
    $originalContent = $content
    
    # Remover referências ao DAO
    $content = $content -replace ', db\.procuraçãoRepresentanteDao\(\)', ''
    $content = $content -replace 'db\.procuraçãoRepresentanteDao\(\),', ''
    $content = $content -replace 'procuraçãoRepresentanteDao,', ''
    $content = $content -replace ', procuraçãoRepresentanteDao', ''
    
    if ($content -ne $originalContent) {
        Set-Content $_.FullName -Value $content -Encoding UTF8
        Write-Host "✅ Corrigido: $($_.Name)" -ForegroundColor Green
    }
}

# 2. Remover imports Dagger
Write-Host "2. Removendo imports Dagger..." -ForegroundColor Cyan
Get-ChildItem -Path "app\src\main\java" -Recurse -Filter "*.kt" | ForEach-Object {
    $content = Get-Content $_.FullName -Raw -Encoding UTF8
    $originalContent = $content
    
    # Remover imports Dagger
    $content = $content -replace 'import dagger\.hilt\.android\.lifecycle\.HiltViewModel', '// import dagger.hilt.android.lifecycle.HiltViewModel // ✅ REMOVIDO: Hilt não é mais usado'
    $content = $content -replace 'import dagger\.hilt\.android\.AndroidEntryPoint', '// import dagger.hilt.android.AndroidEntryPoint // ✅ REMOVIDO: Hilt não é mais usado'
    $content = $content -replace 'import javax\.inject\.Inject', '// import javax.inject.Inject // ✅ REMOVIDO: Hilt não é mais usado'
    $content = $content -replace 'import javax\.inject\.Singleton', '// import javax.inject.Singleton // ✅ REMOVIDO: Hilt não é mais usado'
    
    if ($content -ne $originalContent) {
        Set-Content $_.FullName -Value $content -Encoding UTF8
        Write-Host "✅ Corrigido: $($_.Name)" -ForegroundColor Green
    }
}

Write-Host "✅ CORREÇÃO COMPLETA CONCLUÍDA!" -ForegroundColor Green
Write-Host "Agora execute: ./gradlew assembleDebug --no-daemon" -ForegroundColor Yellow
