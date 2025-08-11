# Script para Commit Rápido - Gestão Bilhares
# Uso: .\commit-rapido.ps1 "mensagem do commit"

param(
    [string]$mensagem = "feat: Atualização automática"
)

Write-Host "COMMIT RAPIDO..." -ForegroundColor Green

# Adicionar e commitar em uma linha
git add . && git commit -m $mensagem

if ($LASTEXITCODE -eq 0) {
    Write-Host "Commit realizado: $mensagem" -ForegroundColor Green
    git log -1 --oneline
} else {
    Write-Host "Erro no commit!" -ForegroundColor Red
}
