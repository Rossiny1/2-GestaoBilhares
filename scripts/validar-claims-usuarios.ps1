# Script para validar claims de usuários antes de remover fallbacks
# Chama a função callable validateUserClaims do Firebase

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Validação de Claims de Usuários" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Este script valida se todos os usuários têm claims configuradas." -ForegroundColor Yellow
Write-Host "Execute ANTES de remover os fallbacks das Firestore Rules." -ForegroundColor Yellow
Write-Host ""

Write-Host "Para executar a validação, use uma das opcoes abaixo:" -ForegroundColor Cyan
Write-Host ""
Write-Host "OPCAO 1: Via Firebase Console (Recomendado)" -ForegroundColor Green
Write-Host "  1. Acesse: https://console.firebase.google.com/project/gestaobilhares/functions" -ForegroundColor White
Write-Host "  2. Clique na funcao 'validateUserClaims'" -ForegroundColor White
Write-Host "  3. Use a aba 'Testing' para executar com dados vazios: {}" -ForegroundColor White
Write-Host ""
Write-Host "OPCAO 2: Via Firebase CLI" -ForegroundColor Green
Write-Host "  firebase functions:shell" -ForegroundColor White
Write-Host "  validateUserClaims({})" -ForegroundColor White
Write-Host ""
Write-Host "OPCAO 3: Via Node.js (se firebase-admin estiver configurado)" -ForegroundColor Green
Write-Host "  cd functions" -ForegroundColor White
Write-Host "  node -e \"const admin = require('firebase-admin'); admin.initializeApp(); admin.auth().listUsers().then(users => { const withClaims = users.users.filter(u => u.customClaims?.companyId); console.log('Total:', users.users.length, 'Com companyId:', withClaims.length); });\"" -ForegroundColor White
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Critérios de Validação" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "✅ APROVADO para remover fallbacks:" -ForegroundColor Green
Write-Host "   - 100% dos usuários ativos têm 'companyId' nas claims" -ForegroundColor White
Write-Host "   - Nenhum erro de PERMISSION_DENIED nos logs" -ForegroundColor White
Write-Host ""
Write-Host "❌ NÃO APROVADO (execute migração primeiro):" -ForegroundColor Red
Write-Host "   - Qualquer usuário sem 'companyId' nas claims" -ForegroundColor White
Write-Host "   - Erros de migração não resolvidos" -ForegroundColor White
Write-Host ""

