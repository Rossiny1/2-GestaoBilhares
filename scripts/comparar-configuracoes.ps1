# Script para comparar configurações entre VM e local
# Uso: .\scripts\comparar-configuracoes.ps1

$ErrorActionPreference = "Continue"

Write-Host "🔍 Comparando Configurações (VM vs Local)" -ForegroundColor Cyan
Write-Host "════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

# 1. Verificar gradle.properties
Write-Host "1️⃣ Verificando gradle.properties..." -ForegroundColor Yellow
if (Test-Path "gradle.properties") {
    Write-Host "   ✅ Arquivo existe" -ForegroundColor Green
    
    # Verificar configurações críticas
    $content = Get-Content gradle.properties -Raw
    
    # Verificar memória Gradle
    if ($content -match "org.gradle.jvmargs=-Xmx(\d+)g") {
        $gradleMem = $matches[1]
        Write-Host "   Gradle memória: ${gradleMem}GB" -ForegroundColor Gray
        if ([int]$gradleMem -gt 6) {
            Write-Host "   ⚠️  Memória muito alta para Windows (pode causar problemas)" -ForegroundColor Yellow
        }
    }
    
    # Verificar memória Kotlin
    if ($content -match "kotlin.daemon.jvmargs=-Xmx(\d+)g") {
        $kotlinMem = $matches[1]
        Write-Host "   Kotlin memória: ${kotlinMem}GB" -ForegroundColor Gray
        if ([int]$kotlinMem -gt 4) {
            Write-Host "   ⚠️  Memória muito alta para Windows (pode causar problemas)" -ForegroundColor Yellow
        }
    }
    
    # Verificar workers
    if ($content -match "org.gradle.workers.max=(\d+)") {
        $workers = $matches[1]
        $cpuCount = (Get-CimInstance Win32_ComputerSystem).NumberOfLogicalProcessors
        Write-Host "   Workers: $workers (CPUs disponíveis: $cpuCount)" -ForegroundColor Gray
        if ([int]$workers -gt $cpuCount) {
            Write-Host "   ⚠️  Workers maior que CPUs (pode causar problemas)" -ForegroundColor Yellow
        }
    }
} else {
    Write-Host "   ❌ Arquivo não existe!" -ForegroundColor Red
}

# 2. Verificar build.gradle.kts
Write-Host ""
Write-Host "2️⃣ Verificando app/build.gradle.kts..." -ForegroundColor Yellow
if (Test-Path "app/build.gradle.kts") {
    Write-Host "   ✅ Arquivo existe" -ForegroundColor Green
    
    # Verificar se há problemas conhecidos
    $buildContent = Get-Content app/build.gradle.kts -Raw
    
    if ($buildContent -match "afterEvaluate") {
        Write-Host "   ✅ Usa afterEvaluate (correto)" -ForegroundColor Green
    }
    
    if ($buildContent -match "ignoreExitValue") {
        Write-Host "   ⚠️  Ainda usa ignoreExitValue (pode causar erro)" -ForegroundColor Yellow
    }
} else {
    Write-Host "   ❌ Arquivo não existe!" -ForegroundColor Red
}

# 3. Verificar recursos do sistema
Write-Host ""
Write-Host "3️⃣ Recursos do Sistema (Windows)..." -ForegroundColor Yellow
$totalRAM = (Get-CimInstance Win32_ComputerSystem).TotalPhysicalMemory / 1GB
$cpuCount = (Get-CimInstance Win32_ComputerSystem).NumberOfLogicalProcessors
Write-Host "   RAM Total: $([math]::Round($totalRAM, 2))GB" -ForegroundColor Gray
Write-Host "   CPUs: $cpuCount" -ForegroundColor Gray

# 4. Verificar se há erros conhecidos
Write-Host ""
Write-Host "4️⃣ Testando compilação rápida..." -ForegroundColor Yellow
try {
    $testOutput = .\gradlew.bat compileDebugKotlin --console=plain 2>&1 | Out-String
    
    if ($testOutput -match "BUILD SUCCESS") {
        Write-Host "   ✅ Build passou!" -ForegroundColor Green
    } elseif ($testOutput -match "error:|Unresolved") {
        Write-Host "   ❌ Erros encontrados:" -ForegroundColor Red
        $testOutput | Select-String -Pattern "error:|Unresolved" | Select-Object -First 5 | ForEach-Object {
            Write-Host "      $_" -ForegroundColor Red
        }
    } else {
        Write-Host "   ⚠️  Build falhou (verifique logs acima)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "   ❌ Erro ao executar build: $_" -ForegroundColor Red
}

# 5. Recomendações
Write-Host ""
Write-Host "════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "💡 Recomendações" -ForegroundColor Cyan
Write-Host ""

if ($totalRAM -lt 16) {
    Write-Host "⚠️  RAM limitada ($([math]::Round($totalRAM, 2))GB)" -ForegroundColor Yellow
    Write-Host "   Considere reduzir memória do Gradle/Kotlin em gradle.properties" -ForegroundColor Gray
}

Write-Host "📋 Se build falhou, me envie:" -ForegroundColor Yellow
Write-Host "   1. Saída completa do build" -ForegroundColor Gray
Write-Host "   2. Erros específicos" -ForegroundColor Gray
Write-Host "   3. Versão do Java (java -version)" -ForegroundColor Gray
Write-Host ""
