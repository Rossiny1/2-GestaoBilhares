# Script para adicionar verificacoes de null safety em ReciboPrinterHelper.kt
Write-Host "=== ADICIONANDO NULL SAFETY EM ReciboPrinterHelper.kt ===" -ForegroundColor Cyan

$file = "core\src\main\java\com\example\gestaobilhares\utils\ReciboPrinterHelper.kt"
$content = Get-Content $file -Raw -Encoding UTF8

# Adicionar ? em todas as referencias a variaveis nullable
$vars = @("txtTitulo", "txtClienteValor", "rowCpfCliente", "txtCpfCliente", "rowNumeroContrato", "txtNumeroContrato", "txtData", "rowValorFicha", "txtValorFicha", "txtMesas", "txtFichasJogadas", "txtDebitoAnterior", "txtSubtotalMesas", "txtTotal", "txtDesconto", "txtValorRecebido", "txtDebitoAtual", "txtPagamentos", "txtObservacoes", "rowNumeroRecibo", "txtNumeroRecibo", "imgLogo")

foreach ($var in $vars) {
    # Substituir var.text por var?.text (mas nao se ja tiver ?)
    $content = $content -replace "(\s+)($var)\.text\s*=", '$1$2?.text ='
    # Substituir var.visibility por var?.visibility
    $content = $content -replace "(\s+)($var)\.visibility\s*=", '$1$2?.visibility ='
    # Substituir var.setTypeface por var?.setTypeface
    $content = $content -replace "(\s+)($var)\.setTypeface", '$1$2?.setTypeface'
}

Set-Content -Path $file -Value $content -Encoding UTF8 -NoNewline
Write-Host "Null safety adicionado em ReciboPrinterHelper.kt" -ForegroundColor Green
Write-Host "=== CORRECAO APLICADA ===" -ForegroundColor Green

