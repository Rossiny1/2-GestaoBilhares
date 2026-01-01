# Script PowerShell para diagnosticar problemas de build local
# Uso: .\scripts\diagnostico-build-local.ps1

$ErrorActionPreference = "Continue"

Write-Host "🔍 DIAGNÓSTICO DE BUILD LOCAL" -ForegroundColor Cyan
Write-Host "════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

# 1. Verificar Java
Write-Host "1️⃣ Verificando Java..." -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1 | Select-String "version" | Select-Object -First 1
    Write-Host "   ✅ Java encontrado: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "   ❌ Java não encontrado!" -ForegroundColor Red
    Write-Host "   💡 Instale Java 11 ou superior" -ForegroundColor Yellow
}

# 2. Verificar Gradle
Write-Host ""
Write-Host "2️⃣ Verificando Gradle..." -ForegroundColor Yellow
if (Test-Path "gradlew.bat") {
    try {
        $gradleVersion = .\gradlew.bat --version 2>&1 | Select-String "Gradle" | Select-Object -First 1
        Write-Host "   ✅ Gradle encontrado: $gradleVersion" -ForegroundColor Green
    } catch {
        Write-Host "   ⚠️  Erro ao verificar Gradle" -ForegroundColor Yellow
    }
} else {
    Write-Host "   ❌ gradlew.bat não encontrado!" -ForegroundColor Red
}

# 3. Verificar Android SDK
Write-Host ""
Write-Host "3️⃣ Verificando Android SDK..." -ForegroundColor Yellow
if (Test-Path "local.properties") {
    $sdkDir = (Get-Content local.properties | Select-String "sdk.dir").ToString().Split("=")[1]
    if ($sdkDir -and (Test-Path $sdkDir)) {
        Write-Host "   ✅ Android SDK encontrado: $sdkDir" -ForegroundColor Green
    } else {
        Write-Host "   ❌ Android SDK não encontrado em: $sdkDir" -ForegroundColor Red
        Write-Host "   💡 Configure o caminho correto em local.properties" -ForegroundColor Yellow
    }
} else {
    Write-Host "   ❌ local.properties não existe!" -ForegroundColor Red
    Write-Host "   💡 Crie local.properties com: sdk.dir=C:\\caminho\\para\\android-sdk" -ForegroundColor Yellow
}

# 4. Verificar gradle.properties
Write-Host ""
Write-Host "4️⃣ Verificando gradle.properties..." -ForegroundColor Yellow
if (Test-Path "gradle.properties") {
    $jvmArgs = (Get-Content gradle.properties | Select-String "org.gradle.jvmargs").ToString()
    $kotlinArgs = (Get-Content gradle.properties | Select-String "kotlin.daemon.jvmargs").ToString()
    Write-Host "   ✅ gradle.properties encontrado" -ForegroundColor Green
    Write-Host "   Gradle JVM: $jvmArgs" -ForegroundColor Gray
    Write-Host "   Kotlin JVM: $kotlinArgs" -ForegroundColor Gray
} else {
    Write-Host "   ❌ gradle.properties não encontrado!" -ForegroundColor Red
}

# 5. Verificar arquivos ignorados
Write-Host ""
Write-Host "5️⃣ Verificando arquivos ignorados..." -ForegroundColor Yellow
if (Test-Path ".gitignore") {
    $ignored = Get-Content .gitignore | Select-String "local.properties"
    if ($ignored) {
        Write-Host "   ⚠️  local.properties está no .gitignore (normal)" -ForegroundColor Yellow
        Write-Host "   💡 Você precisa criar local.properties localmente" -ForegroundColor Cyan
    }
}

# 6. Tentar build de teste
Write-Host ""
Write-Host "6️⃣ Testando compilação..." -ForegroundColor Yellow
Write-Host "   Executando: .\gradlew.bat compileDebugKotlin --console=plain" -ForegroundColor Gray
$buildOutput = .\gradlew.bat compileDebugKotlin --console=plain 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Host "   ✅ Build passou!" -ForegroundColor Green
} else {
    Write-Host "   ❌ Build falhou!" -ForegroundColor Red
    Write-Host ""
    Write-Host "   📋 Erros encontrados:" -ForegroundColor Yellow
    $buildOutput | Select-String -Pattern "error:|FAILED|Exception" | Select-Object -First 10 | ForEach-Object {
        Write-Host "   $_" -ForegroundColor Red
    }
}

# 7. Comparar com VM
Write-Host ""
Write-Host "════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "📊 RESUMO" -ForegroundColor Cyan
Write-Host ""
Write-Host "💡 Próximos passos:" -ForegroundColor Yellow
Write-Host "   1. Verifique se local.properties existe e está correto"
Write-Host "   2. Verifique se Java está instalado (java -version)"
Write-Host "   3. Compare erros acima com os da VM"
Write-Host "   4. Verifique se todas as dependências estão instaladas"
Write-Host ""
