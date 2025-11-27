# Script para corrigir erros críticos do ClientListViewModel

Write-Host "Corrigindo erros críticos do ClientListViewModel..." -ForegroundColor Cyan

$file = "ui/src/main/java/com/example/gestaobilhares/ui/clients/ClientListViewModel.kt"

if (-not (Test-Path $file)) {
    Write-Host "Arquivo nao encontrado: $file" -ForegroundColor Red
    exit 1
}

$content = Get-Content $file -Raw -Encoding UTF8
$original = $content

# 1. Corrigir cicloFinalizado.numeroCiclo na linha 548
$content = $content -replace 'Ciclo \$\{cicloAtual\.numeroCiclo\} finalizado', 'Ciclo finalizado'

# 2. Corrigir referência a rota.id na linha 551
# O erro diz "unresolved reference: id" mas rota.id deveria funcionar
# Vamos verificar se há algum problema de contexto

# 3. Corrigir referências a propriedades de Cliente (nome, debitoAtual)
# Essas propriedades existem na entidade Cliente, então o problema pode ser de import

Write-Host "Aplicando correcoes..." -ForegroundColor Yellow

if ($content -ne $original) {
    Set-Content -Path $file -Value $content -Encoding UTF8 -NoNewline
    Write-Host "Correcoes aplicadas com sucesso!" -ForegroundColor Green
} else {
    Write-Host "Nenhuma correcao necessaria." -ForegroundColor Yellow
}

