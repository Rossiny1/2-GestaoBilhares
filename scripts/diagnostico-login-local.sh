#!/bin/bash

# Script de diagnóstico local para testar login sem depender do app
# Usa Firebase CLI e curl para testar diretamente

echo "=== DIAGNÓSTICO LOCAL DE LOGIN ==="
echo ""

# Configurações
PROJECT_ID="gestaobilhares"
EMAIL_TESTE="user@teste.com"  # Substituir pelo email real do User

echo "1. Verificando configuração do Firebase..."
firebase projects:list 2>&1 | grep -q "$PROJECT_ID" && echo "   ✅ Projeto encontrado" || echo "   ❌ Projeto não encontrado"
echo ""

echo "2. Testando busca de colaborador via Firestore REST API..."
echo "   (Simulando query sem autenticação)"

# Nota: Para testar as regras, precisamos usar o Firebase Admin SDK ou
# criar um script que use o Firebase CLI com autenticação

echo "   ⚠️  Para testar as regras, use o script testar-regras-firestore.js"
echo ""

echo "3. Verificando logs do app (se disponíveis)..."
if [ -f "app/build/outputs/logs" ]; then
    echo "   Logs encontrados"
    tail -50 app/build/outputs/logs/*.log 2>/dev/null | grep -i "login\|colaborador\|firestore" | tail -20
else
    echo "   ⚠️  Logs não encontrados localmente"
fi
echo ""

echo "4. Verificando estrutura do código de busca..."
echo "   Arquivo: ui/src/main/java/com/example/gestaobilhares/ui/auth/AuthViewModel.kt"
grep -n "buscarColaboradorNaNuvemPorEmail\|collectionGroup\|whereEqualTo" ui/src/main/java/com/example/gestaobilhares/ui/auth/AuthViewModel.kt | head -10
echo ""

echo "5. Verificando regras do Firestore..."
echo "   Arquivo: firestore.rules"
grep -A 5 "collectionGroup\|colaboradores.*items" firestore.rules | head -30
echo ""

echo "=== PRÓXIMOS PASSOS ==="
echo "1. Execute o script testar-regras-firestore.js com Node.js"
echo "2. Verifique os logs do app no dispositivo"
echo "3. Teste a busca diretamente no Firebase Console"
echo ""
