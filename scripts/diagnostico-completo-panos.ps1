# Script completo de diagnóstico para panos no estoque
# Verifica: criação, inserção, notificação Flow, atualização UI
# Versão: 1.0 - Diagnóstico completo

Write-Host "=== DIAGNÓSTICO COMPLETO DE PANOS ===" -ForegroundColor Yellow
Write-Host "Objetivo: Identificar exatamente onde o fluxo de panos está quebrado" -ForegroundColor Cyan
Write-Host ""

# Caminho do ADB
$ADB = "C:\Users\$($env:USERNAME)\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verificar se o ADB existe
if (!(Test-Path $ADB)) {
    Write-Host "❌ ADB não encontrado em: $ADB" -ForegroundColor Red
    exit 1
}

# Verificar dispositivo
$devices = & $ADB devices
if (!($devices -match "device$")) {
    Write-Host "❌ Nenhum dispositivo conectado!" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Dispositivo conectado" -ForegroundColor Green

# Limpar logs
Write-Host "Limpando logs anteriores..." -ForegroundColor Yellow
& $ADB logcat -c

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  INICIANDO DIAGNÓSTICO COMPLETO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "PASSOS PARA TESTAR:" -ForegroundColor Yellow
Write-Host "1. Abra o app" -ForegroundColor White
Write-Host "2. Vá para Estoque > Panos" -ForegroundColor White
Write-Host "3. Clique em 'Adicionar Panos em Lote'" -ForegroundColor White
Write-Host "4. Preencha: Tamanho=Grande, Quantidade=3" -ForegroundColor White
Write-Host "5. Clique em 'Criar Panos'" -ForegroundColor White
Write-Host "6. Observe os logs abaixo" -ForegroundColor White
Write-Host ""

# Capturar logs com análise em tempo real
$logs = @()
& $ADB logcat -v time -s AddPanosLoteDialog:* StockViewModel:* PanoRepository:* PanoEstoqueDao:* StockFragment:* AppRepository:* RoomDatabase:* SQLite:* | ForEach-Object {
    $line = $_
    $logs += $line
    
    # Análise em tempo real
    if ($line -match "AddPanosLoteDialog.*Total de panos criados") {
        Write-Host "✅ ETAPA 1: Panos criados no Dialog" -ForegroundColor Green
        Write-Host $line -ForegroundColor Gray
    }
    elseif ($line -match "AddPanosLoteDialog.*Iniciando criação") {
        Write-Host "🔄 ETAPA 1: Iniciando criação de panos..." -ForegroundColor Cyan
        Write-Host $line -ForegroundColor Gray
    }
    elseif ($line -match "StockViewModel.*=== INÍCIO ADIÇÃO PANOS") {
        Write-Host "✅ ETAPA 2: ViewModel recebeu panos" -ForegroundColor Green
        Write-Host $line -ForegroundColor Gray
    }
    elseif ($line -match "StockViewModel.*Validando duplicidade") {
        Write-Host "🔄 ETAPA 2: Validando duplicidade..." -ForegroundColor Yellow
        Write-Host $line -ForegroundColor Gray
    }
    elseif ($line -match "StockViewModel.*Validação OK") {
        Write-Host "✅ ETAPA 2: Validação concluída" -ForegroundColor Green
        Write-Host $line -ForegroundColor Gray
    }
    elseif ($line -match "StockViewModel.*Inserindo panos individualmente") {
        Write-Host "🔄 ETAPA 3: Inserindo panos individualmente..." -ForegroundColor Cyan
        Write-Host $line -ForegroundColor Gray
    }
    elseif ($line -match "StockViewModel.*Pano.*inserido individualmente") {
        Write-Host "✅ ETAPA 3: Pano inserido individualmente" -ForegroundColor Green
        Write-Host $line -ForegroundColor Gray
    }
    elseif ($line -match "StockViewModel.*=== FIM ADIÇÃO PANOS") {
        Write-Host "✅ ETAPA 3: Inserção concluída" -ForegroundColor Green
        Write-Host $line -ForegroundColor Gray
    }
    elseif ($line -match "StockViewModel.*Agrupando.*panos") {
        Write-Host "🔄 ETAPA 4: Agrupando panos..." -ForegroundColor Cyan
        Write-Host $line -ForegroundColor Gray
    }
    elseif ($line -match "StockViewModel.*Total de grupos criados") {
        Write-Host "✅ ETAPA 4: Agrupamento concluído" -ForegroundColor Green
        Write-Host $line -ForegroundColor Gray
    }
    elseif ($line -match "StockFragment.*Grupos de panos recebidos") {
        Write-Host "✅ ETAPA 5: Fragment recebeu grupos" -ForegroundColor Green
        Write-Host $line -ForegroundColor Gray
    }
    elseif ($line -match "StockFragment.*panoGroupAdapter.submitList") {
        Write-Host "✅ ETAPA 5: Adapter atualizado" -ForegroundColor Green
        Write-Host $line -ForegroundColor Gray
    }
    elseif ($line -match "StockViewModel.*ERRO AO ADICIONAR PANOS") {
        Write-Host "❌ ERRO NA ETAPA 2/3: ViewModel" -ForegroundColor Red
        Write-Host $line -ForegroundColor Gray
    }
    elseif ($line -match "AddPanosLoteDialog.*ERRO") {
        Write-Host "❌ ERRO NA ETAPA 1: Dialog" -ForegroundColor Red
        Write-Host $line -ForegroundColor Gray
    }
    elseif ($line -match "RoomDatabase.*INSERT|SQLite.*INSERT") {
        Write-Host "🔄 BANCO: Inserção SQL detectada" -ForegroundColor DarkCyan
        Write-Host $line -ForegroundColor Gray
    }
    elseif ($line -match "StateFlow.*collect|Flow.*emit") {
        Write-Host "🔄 FLOW: Notificação detectada" -ForegroundColor DarkMagenta
        Write-Host $line -ForegroundColor Gray
    }
    else {
        Write-Host $line
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  ANÁLISE FINAL" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Análise final dos logs capturados
Write-Host "ANÁLISE DAS ETAPAS:" -ForegroundColor Yellow

$etapa1 = $logs -match "AddPanosLoteDialog.*Total de panos criados"
if ($etapa1) {
    Write-Host "✅ ETAPA 1: Dialog criou panos" -ForegroundColor Green
}
else {
    Write-Host "❌ ETAPA 1: Dialog não criou panos" -ForegroundColor Red
}

$etapa2 = $logs -match "StockViewModel.*=== INÍCIO ADIÇÃO PANOS"
if ($etapa2) {
    Write-Host "✅ ETAPA 2: ViewModel recebeu panos" -ForegroundColor Green
}
else {
    Write-Host "❌ ETAPA 2: ViewModel não recebeu panos" -ForegroundColor Red
}

$etapa3 = $logs -match "StockViewModel.*Pano.*inserido individualmente"
if ($etapa3) {
    $count = ($logs -match "StockViewModel.*Pano.*inserido individualmente").Count
    Write-Host "✅ ETAPA 3: $count panos inseridos individualmente" -ForegroundColor Green
}
else {
    Write-Host "❌ ETAPA 3: Nenhum pano inserido" -ForegroundColor Red
}

$etapa4 = $logs -match "StockViewModel.*Total de grupos criados"
if ($etapa4) {
    Write-Host "✅ ETAPA 4: Agrupamento funcionou" -ForegroundColor Green
}
else {
    Write-Host "❌ ETAPA 4: Agrupamento não funcionou" -ForegroundColor Red
}

$etapa5 = $logs -match "StockFragment.*panoGroupAdapter.submitList"
if ($etapa5) {
    Write-Host "✅ ETAPA 5: UI atualizada" -ForegroundColor Green
}
else {
    Write-Host "❌ ETAPA 5: UI não atualizada" -ForegroundColor Red
}

# Verificar se há erros
$erros = $logs -match "ERRO|ERROR|Exception"
if ($erros) {
    Write-Host ""
    Write-Host "⚠️  ERROS ENCONTRADOS:" -ForegroundColor Red
    $erros | ForEach-Object {
        Write-Host $_ -ForegroundColor Red
    }
}

# Verificar inserções SQL
$sql = $logs -match "INSERT.*panos_estoque"
if ($sql) {
    Write-Host ""
    Write-Host "🗄️  OPERAÇÕES SQL DETECTADAS:" -ForegroundColor DarkCyan
    $sql | ForEach-Object {
        Write-Host $_ -ForegroundColor DarkCyan
    }
}

Write-Host ""
Write-Host "DIAGNÓSTICO CONCLUÍDO" -ForegroundColor Yellow
Write-Host "Se alguma etapa falhou, o problema está nessa etapa" -ForegroundColor Cyan
