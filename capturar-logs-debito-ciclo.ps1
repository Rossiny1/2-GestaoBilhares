# Script para capturar logs de debito apos finalizar ciclo
# Foco: Debugar problema de debito nao exibido corretamente apos finalizar ciclo

Write-Host "=== CAPTURA DE LOGS DE DEBITO APOS FINALIZAR CICLO ===" -ForegroundColor Yellow
Write-Host "Objetivo: Analisar o fluxo de finalizacao de ciclo e atualizacao de debitos" -ForegroundColor Cyan
Write-Host ""

# Caminho do ADB (mesmo padrao dos outros scripts)
$ADB = "C:\Users\$($env:USERNAME)\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Verificar se o ADB existe
if (!(Test-Path $ADB)) {
    Write-Host "ADB nao encontrado em: $ADB" -ForegroundColor Red
    Write-Host "Certifique-se de que o Android SDK esta instalado corretamente" -ForegroundColor Red
    exit 1
}

# Verificar se ha dispositivo conectado
Write-Host "Verificando dispositivos conectados..." -ForegroundColor Yellow
$devices = & $ADB devices
if ($devices -match "device$") {
    Write-Host "Dispositivo encontrado!" -ForegroundColor Green
} else {
    Write-Host "Nenhum dispositivo conectado!" -ForegroundColor Red
    Write-Host "Conecte um dispositivo USB ou inicie um emulador" -ForegroundColor Yellow
    exit 1
}

# Limpar logs anteriores
Write-Host ""
Write-Host "Limpando logs anteriores..." -ForegroundColor Yellow
& $ADB logcat -c

# Iniciar captura de logs
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  CAPTURANDO LOGS DE DEBITO E CICLO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Filtros ativos:" -ForegroundColor Yellow
Write-Host "  - ClientListViewModel (todos os niveis)" -ForegroundColor White
Write-Host "  - ClientListFragment (todos os niveis)" -ForegroundColor White
Write-Host "  - ClientAdapter (todos os niveis)" -ForegroundColor White
Write-Host "  - ClienteRepository (todos os niveis)" -ForegroundColor White
Write-Host "  - ClienteDao (todos os niveis)" -ForegroundColor White
Write-Host "  - AppRepository (todos os niveis)" -ForegroundColor White
Write-Host "  - CicloAcertoRepository (todos os niveis)" -ForegroundColor White
Write-Host "  - SettlementViewModel (todos os niveis)" -ForegroundColor White
Write-Host ""
Write-Host "Aguardando eventos de finalizacao de ciclo e atualizacao de debitos..." -ForegroundColor Gray
Write-Host "Pressione Ctrl+C para parar a captura" -ForegroundColor Gray
Write-Host ""

# Capturar logs com filtros especificos (usando o mesmo padrao do script que funcionava)
& $ADB logcat -v time -s ClientListViewModel:* ClientListFragment:* ClientAdapter:* ClienteRepository:* ClienteDao:* AppRepository:* CicloAcertoRepository:* SettlementViewModel:* | ForEach-Object {
    $line = $_
    
    # Logs de finalizacao de ciclo
    if ($line -match "ClientListViewModel.*finalizarRota|ClientListViewModel.*Finalizando|ClientListViewModel.*Iniciando finalizacao") {
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "ClientListViewModel.*Ciclo finalizado|ClientListViewModel.*finalizado com sucesso") {
        Write-Host $line -ForegroundColor Green
    }
    elseif ($line -match "CicloAcertoRepository.*finalizarCiclo|CicloAcertoRepository.*Finalizando ciclo") {
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "CicloAcertoRepository.*Ciclo.*finalizado|CicloAcertoRepository.*debitoTotal") {
        Write-Host $line -ForegroundColor Green
    }
    # Logs de carregamento de clientes
    elseif ($line -match "ClientListViewModel.*carregarClientes|ClientListViewModel.*carregarClientesOtimizado") {
        Write-Host $line -ForegroundColor Yellow
    }
    elseif ($line -match "ClientListViewModel.*Clientes carregados|ClientListViewModel.*Clientes atualizados") {
        Write-Host $line -ForegroundColor Green
    }
    elseif ($line -match "ClientListViewModel.*obterClientesPorRotaComDebitoAtual") {
        Write-Host $line -ForegroundColor Magenta
    }
    # Logs de debito
    elseif ($line -match "ClientAdapter.*debitoAtual|ClientAdapter.*Debito") {
        Write-Host $line -ForegroundColor Yellow
    }
    elseif ($line -match "ClientAdapter.*Sem Debito|ClientAdapter.*exibindo debito") {
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "ClienteRepository.*debitoAtual|ClienteDao.*debitoAtual|ClienteDao.*calcularDebito") {
        Write-Host $line -ForegroundColor Magenta
    }
    elseif ($line -match "SettlementViewModel.*Debito atual|SettlementViewModel.*atualizarDebitoAtual|SettlementViewModel.*debitoAtual atualizado") {
        Write-Host $line -ForegroundColor Green
    }
    # Logs de filtros e abas
    elseif ($line -match "ClientListViewModel.*aplicarFiltrosCombinados|ClientListViewModel.*filtrarClientes") {
        Write-Host $line -ForegroundColor DarkCyan
    }
    elseif ($line -match "ClientListViewModel.*ACERTADOS|ClientListViewModel.*NAO_ACERTADOS|ClientListViewModel.*PENDENCIAS") {
        Write-Host $line -ForegroundColor DarkYellow
    }
    elseif ($line -match "ClientListViewModel.*clienteFoiAcertadoNoCiclo|ClientListViewModel.*Cliente.*acertado") {
        Write-Host $line -ForegroundColor DarkGreen
    }
    # Logs de atualizacao de dados
    elseif ($line -match "AppRepository.*finalizarCicloAtualComDados|AppRepository.*atualizarValoresCiclo") {
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "AppRepository.*atualizarDebitoAtual|AppRepository.*calcularDebitoAtual") {
        Write-Host $line -ForegroundColor Magenta
    }
    # Logs de erro
    elseif ($line -match ".*ERRO.*debito|.*ERROR.*debito|.*Exception.*debito|.*Erro.*debito") {
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "ClientListViewModel.*Erro|ClientListViewModel.*ERROR|ClientListViewModel.*Exception") {
        Write-Host $line -ForegroundColor Red
    }
    elseif ($line -match "ClienteRepository.*Erro|ClienteDao.*Erro|AppRepository.*Erro") {
        Write-Host $line -ForegroundColor Red
    }
    # Logs de valores especificos
    elseif ($line -match ".*R\$.*380|.*380\.00|.*debito.*380") {
        Write-Host $line -ForegroundColor Red -BackgroundColor Yellow
    }
    elseif ($line -match ".*debitoAtual.*=.*R\$|.*debitoAtual.*=.*[0-9]") {
        Write-Host $line -ForegroundColor Yellow
    }
    # Logs de recarregamento apos finalizar
    elseif ($line -match "ClientListViewModel.*Recarregar clientes|ClientListViewModel.*delay.*300") {
        Write-Host $line -ForegroundColor Cyan
    }
    elseif ($line -match "ClientListViewModel.*Status da rota atualizado|ClientListViewModel.*Status.*FINALIZADO") {
        Write-Host $line -ForegroundColor Green
    }
    # Logs de observacao de mudancas
    elseif ($line -match "ClientListViewModel.*obterClientesPorRotaComDebitoAtual") {
        Write-Host $line -ForegroundColor DarkMagenta
    }
    elseif ($line -match "ClientListViewModel.*clientesAtualizados") {
        Write-Host $line -ForegroundColor DarkMagenta
    }
    else {
        Write-Host $line
    }
}
