# Script para corrigir TODAS as referencias problematicas
Write-Host "CORRIGINDO TODAS AS REFERENCIAS PROBLEMATICAS" -ForegroundColor Yellow

# 1. Remover referencias ao DAO removido
Write-Host "1. Removendo referencias ao DAO removido..." -ForegroundColor Cyan
Get-ChildItem -Path "app\src\main\java" -Recurse -Filter "*.kt" | ForEach-Object {
    $content = Get-Content $_.FullName -Raw -Encoding UTF8
    $originalContent = $content
    
    # Remover referencias ao DAO
    $content = $content -replace ', db\.procuraçãoRepresentanteDao\(\)', ''
    $content = $content -replace 'db\.procuraçãoRepresentanteDao\(\),', ''
    $content = $content -replace 'procuraçãoRepresentanteDao,', ''
    $content = $content -replace ', procuraçãoRepresentanteDao', ''
    
    if ($content -ne $originalContent) {
        Set-Content $_.FullName -Value $content -Encoding UTF8
        Write-Host "Corrigido: $($_.Name)" -ForegroundColor Green
    }
}

# 2. Remover imports Dagger
Write-Host "2. Removendo imports Dagger..." -ForegroundColor Cyan
Get-ChildItem -Path "app\src\main\java" -Recurse -Filter "*.kt" | ForEach-Object {
    $content = Get-Content $_.FullName -Raw -Encoding UTF8
    $originalContent = $content
    
    # Remover imports Dagger
    $content = $content -replace 'import dagger\.hilt\.android\.lifecycle\.HiltViewModel', '// import dagger.hilt.android.lifecycle.HiltViewModel // REMOVIDO: Hilt nao e mais usado'
    $content = $content -replace 'import dagger\.hilt\.android\.AndroidEntryPoint', '// import dagger.hilt.android.AndroidEntryPoint // REMOVIDO: Hilt nao e mais usado'
    $content = $content -replace 'import javax\.inject\.Inject', '// import javax.inject.Inject // REMOVIDO: Hilt nao e mais usado'
    $content = $content -replace 'import javax\.inject\.Singleton', '// import javax.inject.Singleton // REMOVIDO: Hilt nao e mais usado'
    
    if ($content -ne $originalContent) {
        Set-Content $_.FullName -Value $content -Encoding UTF8
        Write-Host "Corrigido: $($_.Name)" -ForegroundColor Green
    }
}

Write-Host "CORRECAO COMPLETA CONCLUIDA!" -ForegroundColor Green
Write-Host "Agora execute: ./gradlew assembleDebug --no-daemon" -ForegroundColor Yellow
