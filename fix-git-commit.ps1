# Script para corrigir problema de commit com arquivos de build
Write-Host "Removendo arquivos de build do Git..." -ForegroundColor Yellow

# Remover diretorios de build do cache do Git
git rm -r --cached data/build 2>$null
git rm -r --cached core/build 2>$null
git rm -r --cached app/build 2>$null
git rm -r --cached sync/build 2>$null
git rm -r --cached ui/build 2>$null

Write-Host "Adicionando apenas arquivos de codigo fonte..." -ForegroundColor Yellow

# Adicionar apenas arquivos modificados (não os de build)
git add .gitignore
git add gradle.properties
git add app/build.gradle.kts
git add data/build.gradle.kts
git add core/build.gradle.kts
git add settings.gradle.kts
git add .cursor/rules/
git add core/src/
git add data/src/
git add fix-file-lock.ps1
git add fix-ksp-permissions.ps1
git add migrate-data-module.ps1

# Adicionar arquivos modificados do app (excluindo build)
git add app/src/
git add app/build.gradle.kts

Write-Host "Status do Git:" -ForegroundColor Green
git status --short

Write-Host "`nPronto! Agora execute:" -ForegroundColor Green
Write-Host "git commit -m 'Otimizacoes de build e modularizacao: ajustes em gradle.properties e build.gradle.kts'" -ForegroundColor Cyan

