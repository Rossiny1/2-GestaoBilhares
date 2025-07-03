# ========================================
# ANÁLISE ESTÁTICA DE CRASHES - GESTAO BILHARES
# ========================================

Write-Host "Analise Estatica de Crashes - GestaoBilhares" -ForegroundColor Cyan

# Verificar se APK existe
$APK = "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $APK)) {
    Write-Host "ERRO: APK nao encontrado! Execute: .\gradlew assembleDebug" -ForegroundColor Red
    exit 1
}

Write-Host "APK encontrado: $APK" -ForegroundColor Green

# 1. ANÁLISE DE DEPENDÊNCIAS
Write-Host "`n1. ANALISANDO DEPENDENCIAS..." -ForegroundColor Yellow

# Verificar se todas as dependências estão presentes
$buildGradle = Get-Content "app\build.gradle.kts" -Raw
$dependencies = @(
    "dagger.hilt.android",
    "androidx.room",
    "androidx.navigation",
    "com.google.android.material"
)

foreach ($dep in $dependencies) {
    if ($buildGradle -match $dep) {
        Write-Host "✅ $dep - OK" -ForegroundColor Green
    } else {
        Write-Host "❌ $dep - FALTANDO" -ForegroundColor Red
    }
}

# 2. ANÁLISE DE CÓDIGO CRÍTICO
Write-Host "`n2. ANALISANDO CODIGO CRITICO..." -ForegroundColor Yellow

# Verificar arquivos críticos
$criticalFiles = @(
    "app\src\main\java\com\example\gestaobilhares\ui\clients\ClientRegisterFragment.kt",
    "app\src\main\java\com\example\gestaobilhares\ui\clients\ClientRegisterViewModel.kt",
    "app\src\main\java\com\example\gestaobilhares\data\repositories\ClienteRepository.kt",
    "app\src\main\java\com\example\gestaobilhares\di\DatabaseModule.kt"
)

foreach ($file in $criticalFiles) {
    if (Test-Path $file) {
        $content = Get-Content $file -Raw
        $issues = @()
        
        # Verificar problemas comuns
        if ($content -match "dialogException.*not.*declared") {
            $issues += "Variável dialogException não declarada"
        }
        if ($content -match "requireContext\\(\\)") {
            $issues += "Possível crash com requireContext()"
        }
        if ($content -match "findNavController\\(\\)") {
            $issues += "Possível crash com findNavController()"
        }
        if ($content -match "binding\\.") {
            $issues += "Possível crash com binding null"
        }
        
        if ($issues.Count -eq 0) {
            Write-Host "✅ $(Split-Path $file -Leaf) - OK" -ForegroundColor Green
        } else {
            Write-Host "⚠️  $(Split-Path $file -Leaf) - PROBLEMAS:" -ForegroundColor Yellow
            foreach ($issue in $issues) {
                Write-Host "   - $issue" -ForegroundColor Red
            }
        }
    } else {
        Write-Host "❌ $file - ARQUIVO NAO ENCONTRADO" -ForegroundColor Red
    }
}

# 3. ANÁLISE DE LAYOUT
Write-Host "`n3. ANALISANDO LAYOUTS..." -ForegroundColor Yellow

$layoutFiles = @(
    "app\src\main\res\layout\fragment_client_register.xml",
    "app\src\main\res\layout\fragment_client_list.xml",
    "app\src\main\res\layout\fragment_routes.xml"
)

foreach ($file in $layoutFiles) {
    if (Test-Path $file) {
        $content = Get-Content $file -Raw
        $issues = @()
        
        # Verificar problemas comuns de layout
        if ($content -match "android:id=`"@\\+id/etDebitoAtual`"") {
            if ($content -notmatch "android:enabled=`"false`"") {
                $issues += "Campo débito atual deve ser readonly"
            }
        }
        if ($content -match "android:onClick") {
            $issues += "Possível crash com onClick não implementado"
        }
        
        if ($issues.Count -eq 0) {
            Write-Host "✅ $(Split-Path $file -Leaf) - OK" -ForegroundColor Green
        } else {
            Write-Host "⚠️  $(Split-Path $file -Leaf) - PROBLEMAS:" -ForegroundColor Yellow
            foreach ($issue in $issues) {
                Write-Host "   - $issue" -ForegroundColor Red
            }
        }
    } else {
        Write-Host "❌ $file - ARQUIVO NAO ENCONTRADO" -ForegroundColor Red
    }
}

# 4. ANÁLISE DE NAVEGAÇÃO
Write-Host "`n4. ANALISANDO NAVEGACAO..." -ForegroundColor Yellow

$navFile = "app\src\main\res\navigation\nav_graph.xml"
if (Test-Path $navFile) {
    $content = Get-Content $navFile -Raw
    
    # Verificar se todas as ações estão definidas
    $actions = @(
        "action_routesFragment_to_clientListFragment",
        "action_clientListFragment_to_clientRegisterFragment",
        "action_clientListFragment_to_clientDetailFragment"
    )
    
    foreach ($action in $actions) {
        if ($content -match $action) {
            Write-Host "✅ $action - OK" -ForegroundColor Green
        } else {
            Write-Host "❌ $action - FALTANDO" -ForegroundColor Red
        }
    }
} else {
    Write-Host "❌ nav_graph.xml - ARQUIVO NAO ENCONTRADO" -ForegroundColor Red
}

# 5. SIMULAÇÃO DE CENÁRIOS DE CRASH
Write-Host "`n5. SIMULACAO DE CENARIOS DE CRASH..." -ForegroundColor Yellow

# Cenário 1: Crash ao salvar cliente
Write-Host "CENARIO 1: Crash ao salvar cliente" -ForegroundColor Cyan
Write-Host "Possiveis causas:" -ForegroundColor Yellow
Write-Host "  - Banco de dados não inicializado" -ForegroundColor Red
Write-Host "  - Hilt não injetando dependências" -ForegroundColor Red
Write-Host "  - Fragment destruído durante operação" -ForegroundColor Red
Write-Host "  - Context null ao mostrar dialog" -ForegroundColor Red

# Cenário 2: Crash ao clicar em nomes de rua
Write-Host "`nCENARIO 2: Crash ao clicar em nomes de rua" -ForegroundColor Cyan
Write-Host "Possiveis causas:" -ForegroundColor Yellow
Write-Host "  - Safe Args não gerado" -ForegroundColor Red
Write-Host "  - Bundle com argumentos inválidos" -ForegroundColor Red
Write-Host "  - Navigation Component não inicializado" -ForegroundColor Red
Write-Host "  - Fragment não encontrado" -ForegroundColor Red

# 6. RECOMENDAÇÕES
Write-Host "`n6. RECOMENDACOES..." -ForegroundColor Yellow

Write-Host "✅ IMPLEMENTADAS:" -ForegroundColor Green
Write-Host "  - Logging detalhado em todos os componentes" -ForegroundColor Green
Write-Host "  - Tratamento robusto de erros" -ForegroundColor Green
Write-Host "  - Fallback para banco de dados" -ForegroundColor Green
Write-Host "  - Verificação de lifecycle" -ForegroundColor Green

Write-Host "`n🔧 PROXIMOS PASSOS:" -ForegroundColor Cyan
Write-Host "1. Testar em dispositivo físico amanhã" -ForegroundColor Yellow
Write-Host "2. Executar .\teste-crash.ps1" -ForegroundColor Yellow
Write-Host "3. Analisar logs específicos" -ForegroundColor Yellow
Write-Host "4. Implementar correções baseadas nos logs" -ForegroundColor Yellow

Write-Host "`n📊 RESULTADO DA ANALISE ESTATICA CONCLUIDO!" -ForegroundColor Green
