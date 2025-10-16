# Script simples para leitura de logs específicos
# Foca apenas nos logs relacionados ao problema de rotaId

Write-Host "=== LEITURA SIMPLES DE LOGS ===" -ForegroundColor Green
Write-Host "Filtrando logs de ClientDetailFragment, ClientDetailViewModel e AppRepository..." -ForegroundColor Yellow
Write-Host ""

# Verificar ADB
if (!(Get-Command adb -ErrorAction SilentlyContinue)) {
    Write-Host "❌ ADB não encontrado. Instale o Android SDK." -ForegroundColor Red
    exit 1
}

# Limpar logs antigos
Write-Host "🧹 Limpando logs antigos..." -ForegroundColor Yellow
adb logcat -c

Write-Host "📱 Iniciando captura de logs..." -ForegroundColor Cyan
Write-Host "Pressione Ctrl+C para parar" -ForegroundColor Yellow
Write-Host ""

# Capturar logs com filtros específicos
adb logcat -v time | Where-Object {
    $_ -match "ClientDetailFragment|ClientDetailViewModel|AppRepository" -and
    $_ -match "rotaId|clienteId|Cliente|Rota"
} | ForEach-Object {
    # Destacar logs importantes
    if ($_ -match "rotaId.*null|Erro.*rota|não foi possível obter") {
        Write-Host "🚨 " -NoNewline -ForegroundColor Red
    } elseif ($_ -match "Cliente encontrado|RotaId encontrado") {
        Write-Host "✅ " -NoNewline -ForegroundColor Green
    } else {
        Write-Host "ℹ️  " -NoNewline -ForegroundColor Blue
    }
    
    Write-Host $_ -ForegroundColor White
}
