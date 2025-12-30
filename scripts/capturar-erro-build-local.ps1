# Script para capturar erro de build local
# Uso: .\scripts\capturar-erro-build-local.ps1

$ErrorActionPreference = "Continue"

Write-Host "🔍 Capturando Erro de Build Local" -ForegroundColor Cyan
Write-Host "════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

# Executar build e capturar erros
Write-Host "Executando: .\gradlew.bat compileDebugKotlin --console=plain" -ForegroundColor Yellow
Write-Host ""

$buildOutput = .\gradlew.bat compileDebugKotlin --console=plain 2>&1 | Out-String

# Salvar em arquivo
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$logFile = "build_local_error_$timestamp.log"
$buildOutput | Out-File -FilePath $logFile -Encoding UTF8

Write-Host "📋 Saída do build salva em: $logFile" -ForegroundColor Green
Write-Host ""

# Mostrar erros
if ($buildOutput -match "BUILD SUCCESS") {
    Write-Host "✅ Build passou!" -ForegroundColor Green
} else {
    Write-Host "❌ Build falhou!" -ForegroundColor Red
    Write-Host ""
    Write-Host "📋 Erros encontrados:" -ForegroundColor Yellow
    
    # Extrair erros
    $errors = $buildOutput | Select-String -Pattern "error:|Error:|ERROR|FAILED|Exception|Unresolved" -Context 2,2
    
    if ($errors) {
        $errors | Select-Object -First 20 | ForEach-Object {
            Write-Host "   $_" -ForegroundColor Red
        }
    } else {
        Write-Host "   (Nenhum padrão de erro encontrado, verifique o arquivo $logFile)" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "   Últimas 30 linhas:" -ForegroundColor Cyan
        $buildOutput -split "`n" | Select-Object -Last 30 | ForEach-Object {
            Write-Host "   $_" -ForegroundColor Gray
        }
    }
}

Write-Host ""
Write-Host "════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "💡 Envie o conteúdo do arquivo $logFile para correção" -ForegroundColor Yellow
Write-Host ""
