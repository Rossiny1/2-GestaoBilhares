# ========================================
# SCRIPT DE MONITORAMENTO DE RECIBOS - GESTAO BILHARES
# ========================================
#
# INSTRUÇÕES IMPORTANTES:
# 1. Execute este script em uma NOVA JANELA do PowerShell
# 2. Para abrir nova janela: Ctrl+Shift+N ou Win+R -> powershell
# 3. Navegue até a pasta do projeto: cd "C:\Users\Rossiny\Desktop\2-GestaoBilhares"
# 4. Execute: .\monitor-receipts.ps1
#
# CAMINHO CORRETO DO ADB:
# C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe
#
# ========================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "MONITORAMENTO DE RECIBOS - GESTAO BILHARES" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "INSTRUCOES:" -ForegroundColor Yellow
Write-Host "1. Este script deve ser executado em JANELA SEPARADA" -ForegroundColor Yellow
Write-Host "2. Para abrir nova janela: Ctrl+Shift+N ou Win+R -> powershell" -ForegroundColor Yellow
Write-Host "3. Navegue até: cd 'C:\Users\Rossiny\Desktop\2-GestaoBilhares'" -ForegroundColor Yellow
Write-Host "4. Execute: .\monitor-receipts.ps1" -ForegroundColor Yellow
Write-Host ""
Write-Host "MONITORANDO LOGS DE RECIBOS..." -ForegroundColor Red
Write-Host "Pressione Ctrl+C para parar" -ForegroundColor Gray
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar se o ADB está disponível
$adbPath = "C:\Users\Rossiny\AppData\Local\Android\Sdk\platform-tools\adb.exe"

if (-not (Test-Path $adbPath)) {
    Write-Host "ERRO: ADB não encontrado em $adbPath" -ForegroundColor Red
    Write-Host "Verifique se o Android SDK está instalado corretamente" -ForegroundColor Red
    Read-Host "Pressione Enter para sair"
    exit 1
}

# Verificar se o dispositivo está conectado
Write-Host "Verificando dispositivo conectado..." -ForegroundColor Yellow
$devices = & $adbPath devices

Write-Host "Dispositivos encontrados:" -ForegroundColor Yellow
Write-Host $devices -ForegroundColor Gray

# Corrigir a verificação de dispositivo - verificar se há linha com "device" no final
$hasDevice = $devices | Select-String "device$"

if (-not $hasDevice) {
    Write-Host "ERRO: Nenhum dispositivo Android conectado" -ForegroundColor Red
    Write-Host "Conecte o dispositivo via USB e habilite a depuração" -ForegroundColor Red
    Read-Host "Pressione Enter para sair"
    exit 1
}

Write-Host "Dispositivo conectado: OK" -ForegroundColor Green
Write-Host ""

# Limpar logcat anterior
Write-Host "Limpando logcat anterior..." -ForegroundColor Yellow
& $adbPath logcat -c

Write-Host "Iniciando monitoramento de recibos..." -ForegroundColor Green
Write-Host "Agora você pode testar o app no dispositivo" -ForegroundColor Green
Write-Host ""
Write-Host "LOGS MONITORADOS:" -ForegroundColor Cyan
Write-Host "- SettlementDetailFragment: impressão e WhatsApp de recibos" -ForegroundColor Gray
Write-Host "- SettlementSummaryDialog: impressão e WhatsApp de recibos" -ForegroundColor Gray
Write-Host "- ReciboPrinterHelper: geração de conteúdo dos recibos" -ForegroundColor Gray
Write-Host "- SettlementDetailViewModel: carregamento de dados para recibos" -ForegroundColor Gray
Write-Host "- DADOS DO CLIENTE CARREGADOS: verificação de dados do cliente" -ForegroundColor Gray
Write-Host "- DADOS PARA IMPRESSÃO: verificação de dados antes da impressão" -ForegroundColor Gray
Write-Host "- DADOS PARA WHATSAPP: verificação de dados antes do WhatsApp" -ForegroundColor Gray
Write-Host "- BUSCA CONTRATO: verificação de busca de contrato" -ForegroundColor Gray
Write-Host "- BUSCANDO MESAS COMPLETAS: verificação de busca de mesas" -ForegroundColor Gray
Write-Host ""

# Padrão focado apenas nos logs de recibos (impressão e WhatsApp)
$patternReceipts = "SettlementDetailFragment|SettlementSummaryDialog|ReciboPrinterHelper|SettlementDetailViewModel|DADOS DO CLIENTE CARREGADOS|Cliente encontrado|Nome|ValorFicha|ComissaoFicha|DADOS PARA IMPRESSÃO|Cliente Nome|Cliente CPF|Valor Ficha|Acerto ID|Número Contrato|Mesas Completas|BUSCA CONTRATO|Acerto ID|Cliente ID|Contrato encontrado|Número do contrato|preencherReciboImpressaoCompleto|gerarTextoWhatsApp|compartilharViaWhatsApp|imprimirRecibo|preencherLayoutRecibo|obterMesasCompletas|obterNumeroContrato|obterValorFichaExibir|DADOS DO CLIENTE|Cliente ID|Nome do cliente|Telefone do cliente|CPF do cliente|ValorFicha|ComissaoFicha|DADOS DO ACERTO|Acerto ID|Data|Status|Valor Total|Desconto|Débito Anterior|Débito Atual|Observações|Métodos de Pagamento|Pano trocado|Número do pano|Data finalização|DADOS DO CONTRATO|Contrato ID|Número|Cliente ID|Status|Data criação|Data assinatura|Data encerramento|Representante|Tipo|Observações|DADOS DAS MESAS|Mesa ID|Acerto ID|Relógio inicial|Relógio final|Fichas jogadas|Valor fixo|Subtotal|Com defeito|Relógio reiniciou|DADOS COMPLETOS DAS MESAS|Mesa encontrada|Número da mesa|Tipo da mesa|Status|Data criação|Data atualização|Observações|DADOS DO CONTRATO ATIVO|Contrato ativo encontrado|Número do contrato|Cliente ID|Status|Data criação|Data assinatura|Representante|Tipo|Observações|DADOS PARA IMPRESSÃO|Cliente Nome|Cliente CPF|Valor Ficha|Acerto ID|Número Contrato|Mesas Completas|DADOS PARA WHATSAPP|Cliente Nome|Cliente CPF|Valor Ficha|Acerto ID|Número Contrato|Mesas Completas|BUSCANDO MESAS COMPLETAS|Total acertoMesas|Buscando mesa ID|Mesa encontrada|Mesa adicionada|Mesa não encontrada|Total mesas completas|DADOS RECEBIDOS PARA IMPRESSÃO|Cliente Nome|Cliente CPF|Valor Ficha|Acerto ID|Número Contrato|Mesas Completas|Débito Anterior|Valor Total Mesas|Desconto|Débito Atual|Observações"

# Monitorar logcat filtrando apenas logs de recibos
try {
    Write-Host "Monitorando em tempo real..." -ForegroundColor Green
    Write-Host "Filtros: $patternReceipts" -ForegroundColor Gray
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    
    & $adbPath logcat -v time | Select-String -Pattern $patternReceipts
} catch {
    Write-Host "Erro ao executar logcat: $($_.Exception.Message)" -ForegroundColor Red
    Read-Host "Pressione Enter para sair"
}

Write-Host "" 
Write-Host "Monitoramento finalizado" -ForegroundColor Gray
Read-Host "Pressione Enter para sair"
