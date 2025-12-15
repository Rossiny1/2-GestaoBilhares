# 🔄 BUILD GRADUAL - Estratégia de Redução de Complexidade
# Se o build completo falha, vamos compilar por partes

Write-Host "🔄 INICIANDO BUILD GRADUAL..." -ForegroundColor Blue

# 1. APENAS COMPILAR KOTLIN (sem Android)
Write-Host "📝 Compilando apenas Kotlin..." -ForegroundColor Yellow
./gradlew compileDebugKotlin --no-daemon

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Kotlin compilado!" -ForegroundColor Green
    
    # 2. APENAS GERAR R (sem compilar código)
    Write-Host "📱 Gerando recursos..." -ForegroundColor Yellow
    ./gradlew generateDebugResources --no-daemon
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Recursos gerados!" -ForegroundColor Green
        
        # 3. BUILD COMPLETO
        Write-Host "🔨 Build completo..." -ForegroundColor Yellow
        ./gradlew assembleDebug --no-daemon
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ BUILD GRADUAL SUCESSO!" -ForegroundColor Green
        } else {
            Write-Host "❌ Falha no build completo" -ForegroundColor Red
        }
    } else {
        Write-Host "❌ Falha na geração de recursos" -ForegroundColor Red
    }
} else {
    Write-Host "❌ Falha na compilação Kotlin" -ForegroundColor Red
    Write-Host "🔍 Vamos identificar o arquivo problemático..." -ForegroundColor Yellow
    
    # 4. COMPILAR ARQUIVO POR ARQUIVO
    Write-Host "🔍 Compilando arquivo por arquivo..." -ForegroundColor Yellow
    ./gradlew compileDebugKotlin --no-daemon --continue
}
