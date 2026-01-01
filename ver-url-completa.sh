#!/bin/bash
# Script para ver URL completa do Firebase

export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin

echo "🔥 Firebase Login - Capturando URL Completa"
echo "=========================================="
echo ""
echo "Este script vai:"
echo "1. Executar firebase login"
echo "2. Salvar tudo em um arquivo"
echo "3. Mostrar a URL completa"
echo ""
echo "Pressione Enter para continuar..."
read

# Executar e salvar
firebase login --no-localhost 2>&1 | tee /tmp/firebase-login-full.txt

echo ""
echo "=========================================="
echo "✅ Output salvo em: /tmp/firebase-login-full.txt"
echo ""
echo "=== PROCURANDO URL ==="
URL=$(grep -oE "https://[^[:space:]]+" /tmp/firebase-login-full.txt | head -1)

if [ ! -z "$URL" ]; then
    echo ""
    echo "🔗 URL COMPLETA ENCONTRADA:"
    echo ""
    echo "$URL"
    echo ""
    echo "📋 Copie a URL acima e abra no navegador!"
    echo ""
    echo "Para ver todo o conteúdo:"
    echo "  cat /tmp/firebase-login-full.txt"
else
    echo ""
    echo "⚠️ URL não encontrada automaticamente."
    echo "Verifique o arquivo completo:"
    echo "  cat /tmp/firebase-login-full.txt"
    echo ""
    echo "Procure pela linha que começa com: https://accounts.google.com/..."
fi
