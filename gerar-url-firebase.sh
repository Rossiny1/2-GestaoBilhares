#!/bin/bash
# Script para gerar URL do Firebase Login
# Execute este script no terminal bash da VM (não PowerShell)

export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin

echo "🔥 Firebase Login - Gerando URL"
echo "================================"
echo ""
echo "Este script vai executar: firebase login --no-localhost"
echo "A URL completa será salva em: firebase-url-gerada.txt"
echo ""
echo "⚠️ IMPORTANTE:"
echo "1. Quando aparecer a pergunta sobre Gemini, digite Y ou N"
echo "2. Quando aparecer a URL, ela será salva automaticamente"
echo "3. Copie a URL do arquivo firebase-url-gerada.txt"
echo ""
echo "Pressione Enter para começar..."
read

# Arquivo para salvar
OUTPUT_FILE="/workspace/firebase-url-gerada.txt"

echo ""
echo "Executando firebase login --no-localhost..."
echo ""

# Executar e salvar TUDO
firebase login --no-localhost 2>&1 | tee "$OUTPUT_FILE"

echo ""
echo "================================"
echo "✅ Output salvo em: firebase-url-gerada.txt"
echo ""

# Extrair URL
if [ -f "$OUTPUT_FILE" ]; then
    echo "=== PROCURANDO URL ==="
    URL=$(grep -oE "https://[^[:space:]]+" "$OUTPUT_FILE" | head -1)
    
    if [ ! -z "$URL" ]; then
        echo ""
        echo "🔗 URL ENCONTRADA:"
        echo ""
        echo "$URL"
        echo ""
        echo "📋 URL também está salva em: firebase-url-gerada.txt"
    else
        echo ""
        echo "⚠️ URL não encontrada automaticamente."
        echo "📋 Veja o arquivo completo:"
        echo "   cat firebase-url-gerada.txt"
    fi
fi

echo ""
echo "================================"
echo "📝 Próximos passos:"
echo "1. Abra o arquivo firebase-url-gerada.txt no Cursor"
echo "2. Copie a URL completa (linha que começa com https://accounts.google.com/...)"
echo "3. Cole no navegador do seu notebook"
echo "4. Faça login com sua conta Google"
echo "5. Copie o código de autorização"
echo "6. Volte ao terminal e cole o código"
echo ""
