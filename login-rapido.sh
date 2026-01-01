#!/bin/bash
# Login Firebase - Método Rápido

export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin

echo "🔥 Login Firebase - Método Rápido"
echo "=================================="
echo ""

# Executar login e salvar
firebase login --no-localhost 2>&1 | tee /workspace/firebase-url-gerada.txt

echo ""
echo "=================================="
echo "✅ URL salva em: firebase-url-gerada.txt"
echo ""

# Mostrar URL
URL=$(grep -oE "https://[^[:space:]]+" /workspace/firebase-url-gerada.txt | head -1)

if [ ! -z "$URL" ]; then
    echo "🔗 URL:"
    echo "$URL"
    echo ""
    echo "📋 Copie a URL acima e abra no navegador!"
else
    echo "📋 Veja o arquivo: firebase-url-gerada.txt"
fi

echo ""
