# Script de Limpeza e Organização do Projeto
# Autor: Agente AI (Antigravity)
# Data: 17/12/2025

$root = "C:\Users\Rossiny\Desktop\2-GestaoBilhares"
$docDir = "$root\documentation"

Write-Host "INICIANDO LIMPEZA DO PROJETO..." -ForegroundColor Cyan

# 1. Criar pasta de documentacao
if (-not (Test-Path $docDir)) {
    New-Item -ItemType Directory -Path $docDir | Out-Null
    Write-Host "Pasta 'documentation' criada." -ForegroundColor Green
}

# 2. Mover arquivos de documentacao (Markdown)
$docs = Get-ChildItem -Path $root -Filter "*.md" | Where-Object { 
    $_.Name -match "^(ANALISE|PLANO|RELATORIO|RESUMO|STATUS|TAREFAS|DIAGNOSTICO|GUIA|MIGRACAO|MIGRATION|BASELINE|COMPOSE|CORRECAO|EXPLICACAO|FLUXO|INSTALACAO|LER|OTIMIZACAO|SOLUCAO|SISTEMA|VISUALIZACAO|APK).*" 
}

foreach ($doc in $docs) {
    Move-Item -Path $doc.FullName -Destination $docDir
    Write-Host "Movido para docs: $($doc.Name)" -ForegroundColor Gray
}

# 3. Excluir Logs (txt)
$logs = Get-ChildItem -Path $root -Filter "*.txt" | Where-Object { 
    $_.Name -like "logcat*" -or $_.Name -like "build_log*" -or $_.Name -like "logs_ciclo*" -or $_.Name -like "temp_*" 
}

foreach ($log in $logs) {
    Remove-Item -Path $log.FullName -Force
    Write-Host "Excluido log: $($log.Name)" -ForegroundColor Yellow
}

# 4. Excluir arquivos de erro/lixo (nomes estranhos)
$trashFiles = @(
    "ersRossinyDesktop2-GestaoBilhares*", 
    "ervices.json*", 
    "ucesso impressao bitmap*",
    "how *",
    "b" # Pasta b/ se for arquivo ou pasta
)

foreach ($pattern in $trashFiles) {
    try {
        $items = Get-ChildItem -Path $root -Filter $pattern
        foreach ($item in $items) {
            Remove-Item -Path $item.FullName -Recurse -Force -ErrorAction SilentlyContinue
            Write-Host "Excluido lixo: $($item.Name)" -ForegroundColor Red
        }
    } catch {}
}

# 5. Remover pasta b/ (artifact build) explicita se existir
if (Test-Path "$root\b") {
    Remove-Item -Path "$root\b" -Recurse -Force
    Write-Host "Pasta de build 'b/' removida." -ForegroundColor Red
}

Write-Host "LIMPEZA CONCLUIDA COM SUCESSO!" -ForegroundColor Green
