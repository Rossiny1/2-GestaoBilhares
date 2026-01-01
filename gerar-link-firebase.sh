#!/bin/bash
# Script para gerar link de login do Firebase

export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin

echo "🔥 Gerando link de autenticação Firebase..."
echo ""
echo "Aguarde, o Firebase CLI está gerando a URL..."
echo ""

# Tentar gerar o link (pode falhar se não for interativo, mas vamos tentar capturar a URL)
firebase login --no-localhost 2>&1 | tee /tmp/firebase-login-output.txt

# Extrair URL se estiver no output
if [ -f /tmp/firebase-login-output.txt ]; then
    URL=$(grep -oE "https://[^[:space:]]+" /tmp/firebase-login-output.txt | head -1)
    if [ ! -z "$URL" ]; then
        echo ""
        echo "✅ URL encontrada:"
        echo "$URL"
        echo ""
        echo "📋 Copie a URL acima e abra no seu navegador!"
    fi
fi
