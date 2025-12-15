# ========================================
# ANALISADOR DE LOGS DE CRASH - SIMPLES
# ========================================

param(
    [string]$LogFile = "",
    [string]$PackageName = "com.example.gestaobilhares"
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "ANALISADOR DE LOGS DE CRASH" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Se não especificou arquivo, procurar o mais recente
if ([string]::IsNullOrEmpty($LogFile)) {
    $recentLogs = Get-ChildItem -Path "." -Filter "logcat-crash-*.txt" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if ($recentLogs) {
        $LogFile = $recentLogs.FullName
        Write-Host "Usando arquivo mais recente: $($recentLogs.Name)" -ForegroundColor Yellow
    } else {
        Write-Host "Nenhum arquivo de log encontrado. Execute primeiro: .\capturar-logs-simples.ps1" -ForegroundColor Red
        exit 1
    }
}

# Verificar se arquivo existe
if (-not (Test-Path $LogFile)) {
    Write-Host "Arquivo nao encontrado: $LogFile" -ForegroundColor Red
    exit 1
}

Write-Host "ANALISANDO: $LogFile" -ForegroundColor White
Write-Host ""

# Ler conteúdo do arquivo
$logContent = Get-Content $LogFile -Raw
$lines = Get-Content $LogFile

Write-Host "1. ESTATISTICAS GERAIS:" -ForegroundColor Cyan
Write-Host "   - Total de linhas: $($lines.Count)" -ForegroundColor White
Write-Host "   - Tamanho: $((Get-Item $LogFile).Length) bytes" -ForegroundColor White

# Contar tipos de erro
$fatalCount = ($logContent | Select-String -Pattern "FATAL EXCEPTION" -AllMatches).Matches.Count
$errorCount = ($logContent | Select-String -Pattern "AndroidRuntime.*ERROR" -AllMatches).Matches.Count
$exceptionCount = ($logContent | Select-String -Pattern "Exception" -AllMatches).Matches.Count
$crashCount = ($logContent | Select-String -Pattern "Crash" -AllMatches).Matches.Count

Write-Host "   - Fatal Exceptions: $fatalCount" -ForegroundColor $(if ($fatalCount -gt 0) { "Red" } else { "Green" })
Write-Host "   - Runtime Errors: $errorCount" -ForegroundColor $(if ($errorCount -gt 0) { "Red" } else { "Green" })
Write-Host "   - Exceptions: $exceptionCount" -ForegroundColor $(if ($exceptionCount -gt 0) { "Red" } else { "Green" })
Write-Host "   - Crashes: $crashCount" -ForegroundColor $(if ($crashCount -gt 0) { "Red" } else { "Green" })

Write-Host ""

# Procurar por FATAL EXCEPTION (crash principal)
Write-Host "2. CRASH PRINCIPAL (FATAL EXCEPTION):" -ForegroundColor Cyan
$fatalExceptions = $logContent | Select-String -Pattern "FATAL EXCEPTION.*$PackageName" -AllMatches
if ($fatalExceptions) {
    Write-Host "CRASH ENCONTRADO:" -ForegroundColor Red
    foreach ($fatal in $fatalExceptions) {
        Write-Host "   $($fatal.Line)" -ForegroundColor Red
    }
} else {
    Write-Host "Nenhum crash fatal encontrado" -ForegroundColor Green
}

Write-Host ""

# Procurar por stack traces
Write-Host "3. STACK TRACES:" -ForegroundColor Cyan
$stackTraces = $logContent | Select-String -Pattern "at com\.example\.gestaobilhares" -AllMatches
if ($stackTraces) {
    Write-Host "STACK TRACE DO APP:" -ForegroundColor Yellow
    $stackTraces | ForEach-Object { 
        Write-Host "   $($_.Line)" -ForegroundColor Yellow 
    }
} else {
    Write-Host "Nenhum stack trace do app encontrado" -ForegroundColor Gray
}

Write-Host ""

# Procurar por erros específicos do Firebase/Google Services
Write-Host "4. FIREBASE/GOOGLE SERVICES ERRORS:" -ForegroundColor Cyan
$firebaseErrors = $logContent | Select-String -Pattern "Firebase|Google.*Services|GmsClient|DeadObjectException|TransactionTooLargeException" -AllMatches
if ($firebaseErrors) {
    Write-Host "ERROS FIREBASE/GOOGLE SERVICES:" -ForegroundColor Red
    $firebaseErrors | ForEach-Object { 
        Write-Host "   $($_.Line)" -ForegroundColor Red 
    }
} else {
    Write-Host "Nenhum erro Firebase/Google Services encontrado" -ForegroundColor Green
}

Write-Host ""

# Procurar por erros de banco de dados
Write-Host "5. DATABASE ERRORS:" -ForegroundColor Cyan
$dbErrors = $logContent | Select-String -Pattern "SQLite|Room|Database|AppDatabase|SQLException" -AllMatches
if ($dbErrors) {
    Write-Host "ERROS DE BANCO DE DADOS:" -ForegroundColor Red
    $dbErrors | ForEach-Object { 
        Write-Host "   $($_.Line)" -ForegroundColor Red 
    }
} else {
    Write-Host "Nenhum erro de banco de dados encontrado" -ForegroundColor Green
}

Write-Host ""

# Procurar por erros de autenticação
Write-Host "6. AUTHENTICATION ERRORS:" -ForegroundColor Cyan
$authErrors = $logContent | Select-String -Pattern "AuthViewModel|LoginFragment|Authentication|Login.*Error" -AllMatches
if ($authErrors) {
    Write-Host "ERROS DE AUTENTICACAO:" -ForegroundColor Red
    $authErrors | ForEach-Object { 
        Write-Host "   $($_.Line)" -ForegroundColor Red 
    }
} else {
    Write-Host "Nenhum erro de autenticacao encontrado" -ForegroundColor Green
}

Write-Host ""

# Procurar por erros de memória/performance
Write-Host "7. MEMORY/PERFORMANCE ERRORS:" -ForegroundColor Cyan
$memoryErrors = $logContent | Select-String -Pattern "OutOfMemory|ANR|NotResponding|GC.*freed|Memory.*leak" -AllMatches
if ($memoryErrors) {
    Write-Host "ERROS DE MEMORIA/PERFORMANCE:" -ForegroundColor Red
    $memoryErrors | ForEach-Object { 
        Write-Host "   $($_.Line)" -ForegroundColor Red 
    }
} else {
    Write-Host "Nenhum erro de memoria/performance encontrado" -ForegroundColor Green
}

Write-Host ""

# Procurar por erros de rede
Write-Host "8. NETWORK ERRORS:" -ForegroundColor Cyan
$networkErrors = $logContent | Select-String -Pattern "Network.*Error|Connection.*failed|Socket.*timeout|HTTP.*error" -AllMatches
if ($networkErrors) {
    Write-Host "ERROS DE REDE:" -ForegroundColor Red
    $networkErrors | ForEach-Object { 
        Write-Host "   $($_.Line)" -ForegroundColor Red 
    }
} else {
    Write-Host "Nenhum erro de rede encontrado" -ForegroundColor Green
}

Write-Host ""

# Procurar por erros de permissão
Write-Host "9. PERMISSION ERRORS:" -ForegroundColor Cyan
$permissionErrors = $logContent | Select-String -Pattern "Permission.*denied|Security.*Exception|Access.*denied" -AllMatches
if ($permissionErrors) {
    Write-Host "ERROS DE PERMISSAO:" -ForegroundColor Red
    $permissionErrors | ForEach-Object { 
        Write-Host "   $($_.Line)" -ForegroundColor Red 
    }
} else {
    Write-Host "Nenhum erro de permissao encontrado" -ForegroundColor Green
}

Write-Host ""

# Resumo e recomendações
Write-Host "10. RESUMO E RECOMENDACOES:" -ForegroundColor Cyan

$totalErrors = $fatalCount + $errorCount + $exceptionCount + $crashCount

if ($totalErrors -eq 0) {
    Write-Host "NENHUM ERRO ENCONTRADO - App funcionando corretamente!" -ForegroundColor Green
} elseif ($fatalCount -gt 0) {
    Write-Host "CRASH CRITICO DETECTADO - App nao esta funcionando" -ForegroundColor Red
    Write-Host "   - Verifique o stack trace completo" -ForegroundColor White
    Write-Host "   - Identifique a linha exata do erro" -ForegroundColor White
    Write-Host "   - Corrija o codigo baseado no erro" -ForegroundColor White
} elseif ($errorCount -gt 0) {
    Write-Host "ERROS DETECTADOS - App pode ter problemas" -ForegroundColor Yellow
    Write-Host "   - Verifique os erros especificos acima" -ForegroundColor White
    Write-Host "   - Corrija os problemas identificados" -ForegroundColor White
} else {
    Write-Host "APENAS WARNINGS/INFO - App funcionando com avisos" -ForegroundColor Blue
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "ANALISE CONCLUIDA!" -ForegroundColor Green
Write-Host "Arquivo analisado: $LogFile" -ForegroundColor White
Write-Host "========================================" -ForegroundColor Cyan
