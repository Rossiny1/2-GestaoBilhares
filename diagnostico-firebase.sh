#!/bin/bash
# Script de diagnóstico Firebase

echo "🔍 Diagnóstico Firebase CLI"
echo "=========================="
echo ""

# Verificar PATH
export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin

echo "1. Verificando Firebase CLI..."
firebase --version 2>&1 || echo "❌ Firebase CLI não encontrado"

echo ""
echo "2. Verificando Node.js..."
node --version 2>&1 || echo "❌ Node.js não encontrado"

echo ""
echo "3. Verificando PATH..."
echo $PATH | grep -q nvm && echo "✅ PATH contém nvm" || echo "❌ PATH não contém nvm"

echo ""
echo "4. Verificando localização do Firebase..."
which firebase 2>&1 || echo "❌ Firebase não está no PATH"

echo ""
echo "5. Verificando permissões..."
if [ -f "$(which firebase 2>/dev/null)" ]; then
    ls -la $(which firebase) 2>/dev/null
fi

echo ""
echo "6. Verificando configuração Firebase..."
ls -la ~/.config/firebase 2>/dev/null || echo "⚠️ Diretório de configuração não existe"

echo ""
echo "7. Verificando login atual..."
firebase login:list 2>&1

echo ""
echo "=========================="
echo "✅ Diagnóstico completo!"
