# Script para testar o projeto apos modularizacao
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "TESTE DO PROJETO GESTAOBILHARES" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Verificar se estamos no diretorio correto
if (-not (Test-Path "build.gradle.kts") -and -not (Test-Path "build.gradle")) {
    Write-Host "ERRO: Nao esta no diretorio raiz do projeto!" -ForegroundColor Red
    Write-Host "Execute este script na pasta raiz do projeto." -ForegroundColor Yellow
    exit 1
}

# 1. Verificar estrutura dos modulos
Write-Host "Passo 1: Verificando estrutura dos modulos..." -ForegroundColor Yellow
$modulos = @("core", "data", "sync", "ui", "app")
$modulosOk = $true

foreach ($modulo in $modulos) {
    if (Test-Path $modulo) {
        Write-Host "  [OK] Modulo '$modulo' existe" -ForegroundColor Green
    } else {
        Write-Host "  [ERRO] Modulo '$modulo' nao encontrado!" -ForegroundColor Red
        $modulosOk = $false
    }
}

if (-not $modulosOk) {
    Write-Host ""
    Write-Host "ERRO: Alguns modulos estao faltando!" -ForegroundColor Red
    exit 1
}

Write-Host ""

# 2. Verificar arquivos build.gradle.kts dos modulos
Write-Host "Passo 2: Verificando arquivos de configuracao..." -ForegroundColor Yellow
$buildFiles = @(
    "core/build.gradle.kts",
    "data/build.gradle.kts",
    "sync/build.gradle.kts",
    "ui/build.gradle.kts",
    "app/build.gradle.kts"
)

foreach ($buildFile in $buildFiles) {
    if (Test-Path $buildFile) {
        Write-Host "  [OK] $buildFile existe" -ForegroundColor Green
    } else {
        Write-Host "  [AVISO] $buildFile nao encontrado" -ForegroundColor Yellow
    }
}

Write-Host ""

# 3. Limpar build anterior (opcional, mas recomendado)
Write-Host "Passo 3: Limpando builds anteriores..." -ForegroundColor Yellow
Write-Host "  Executando: ./gradlew clean" -ForegroundColor Gray
./gradlew clean --no-daemon 2>&1 | Out-Null

if ($LASTEXITCODE -eq 0) {
    Write-Host "  [OK] Limpeza concluida" -ForegroundColor Green
} else {
    Write-Host "  [AVISO] Limpeza pode ter falhado, continuando..." -ForegroundColor Yellow
}

Write-Host ""

# 4. Verificar sintaxe Kotlin (compilacao rapida)
Write-Host "Passo 4: Verificando sintaxe do codigo..." -ForegroundColor Yellow
Write-Host "  Executando: ./gradlew compileDebugKotlin" -ForegroundColor Gray
./gradlew compileDebugKotlin --no-daemon 2>&1 | Tee-Object -Variable compileOutput

if ($LASTEXITCODE -eq 0) {
    Write-Host "  [OK] Compilacao bem-sucedida!" -ForegroundColor Green
} else {
    Write-Host "  [ERRO] Erros de compilacao encontrados!" -ForegroundColor Red
    Write-Host ""
    Write-Host "=== ERROS DE COMPILACAO ===" -ForegroundColor Red
    $compileOutput | Select-String -Pattern "error:|ERROR:" | Select-Object -First 10
    Write-Host ""
    Write-Host "Para ver todos os erros, execute:" -ForegroundColor Yellow
    Write-Host "  ./gradlew compileDebugKotlin --stacktrace" -ForegroundColor White
    exit 1
}

Write-Host ""

# 5. Gerar APK de debug
Write-Host "Passo 5: Gerando APK de debug..." -ForegroundColor Yellow
Write-Host "  Executando: ./gradlew assembleDebug" -ForegroundColor Gray
./gradlew assembleDebug --no-daemon 2>&1 | Tee-Object -Variable buildOutput

if ($LASTEXITCODE -eq 0) {
    Write-Host "  [OK] APK gerado com sucesso!" -ForegroundColor Green
    
    # Verificar se o APK foi criado
    $apkPath = "app/build/outputs/apk/debug/app-debug.apk"
    if (Test-Path $apkPath) {
        $apkSize = (Get-Item $apkPath).Length / 1MB
        Write-Host "  [OK] APK encontrado: $apkPath" -ForegroundColor Green
        Write-Host "  [INFO] Tamanho: $([math]::Round($apkSize, 2)) MB" -ForegroundColor Cyan
    } else {
        Write-Host "  [AVISO] APK nao encontrado no caminho esperado" -ForegroundColor Yellow
    }
} else {
    Write-Host "  [ERRO] Falha ao gerar APK!" -ForegroundColor Red
    Write-Host ""
    Write-Host "=== ERROS DE BUILD ===" -ForegroundColor Red
    $buildOutput | Select-String -Pattern "error:|ERROR:|FAILED" | Select-Object -First 10
    Write-Host ""
    Write-Host "Para ver todos os erros, execute:" -ForegroundColor Yellow
    Write-Host "  ./gradlew assembleDebug --stacktrace" -ForegroundColor White
    exit 1
}

Write-Host ""

# 6. Verificar warnings (se houver)
Write-Host "Passo 6: Verificando warnings..." -ForegroundColor Yellow
$warnings = $buildOutput | Select-String -Pattern "warning:|WARNING:" | Measure-Object -Line

if ($warnings.Lines -gt 0) {
    Write-Host "  [AVISO] $($warnings.Lines) warnings encontrados" -ForegroundColor Yellow
    Write-Host "  (Warnings nao impedem o build, mas devem ser revisados)" -ForegroundColor Gray
} else {
    Write-Host "  [OK] Nenhum warning encontrado" -ForegroundColor Green
}

Write-Host ""

# 7. Resumo final
Write-Host "=====================================" -ForegroundColor Green
Write-Host "RESUMO DOS TESTES" -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Green
Write-Host ""
Write-Host "Status: PROJETO FUNCIONANDO!" -ForegroundColor Green
Write-Host ""
Write-Host "Modulos verificados:" -ForegroundColor Cyan
Write-Host "  - core: OK" -ForegroundColor Green
Write-Host "  - data: OK" -ForegroundColor Green
Write-Host "  - sync: OK" -ForegroundColor Green
Write-Host "  - ui: OK" -ForegroundColor Green
Write-Host "  - app: OK" -ForegroundColor Green
Write-Host ""
Write-Host "Compilacao: OK" -ForegroundColor Green
Write-Host "APK gerado: OK" -ForegroundColor Green
Write-Host ""

# Informacoes sobre o APK
if (Test-Path $apkPath) {
    Write-Host "APK disponivel em:" -ForegroundColor Cyan
    Write-Host "  $((Resolve-Path $apkPath).Path)" -ForegroundColor White
    Write-Host ""
    Write-Host "Para instalar no dispositivo:" -ForegroundColor Yellow
    Write-Host "  1. Transfira o APK para o celular" -ForegroundColor White
    Write-Host "  2. Abra o arquivo no celular" -ForegroundColor White
    Write-Host "  3. Permita instalacao de fontes desconhecidas" -ForegroundColor White
    Write-Host "  4. Instale o APK" -ForegroundColor White
    Write-Host ""
}

Write-Host "=====================================" -ForegroundColor Green
Write-Host "TESTES CONCLUIDOS COM SUCESSO!" -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Green
Write-Host ""
Write-Host "PROXIMOS PASSOS:" -ForegroundColor Yellow
Write-Host "  1. Testar o app manualmente no dispositivo" -ForegroundColor White
Write-Host "  2. Verificar funcionalidades principais:" -ForegroundColor White
Write-Host "     - Login" -ForegroundColor Gray
Write-Host "     - Lista de clientes" -ForegroundColor Gray
Write-Host "     - Criar/editar cliente" -ForegroundColor Gray
Write-Host "     - Tela de acerto" -ForegroundColor Gray
Write-Host "     - Sincronizacao" -ForegroundColor Gray
Write-Host "  3. Reportar problemas encontrados" -ForegroundColor White
Write-Host ""

