#!/bin/bash
# Script para executar login Firebase e capturar URL completa

export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin

echo "🔥 Firebase Login - Capturando URL Completa"
echo "=========================================="
echo ""
echo "Este script vai:"
echo "1. Executar firebase login --no-localhost"
echo "2. Salvar TUDO em um arquivo (incluindo URL completa)"
echo "3. Mostrar a URL completa para você copiar"
echo ""
echo "⚠️ IMPORTANTE: Quando aparecer a URL, copie do arquivo, não do terminal!"
echo ""
echo "Pressione Enter para começar..."
read

# Arquivo para salvar output
OUTPUT_FILE="/workspace/firebase-login-completo.txt"

echo ""
echo "Executando login..."
echo ""

# Executar e salvar TUDO
firebase login --no-localhost 2>&1 | tee "$OUTPUT_FILE"

echo ""
echo "=========================================="
echo "✅ Output completo salvo em: firebase-login-completo.txt"
echo ""

# Tentar extrair URL
if [ -f "$OUTPUT_FILE" ]; then
    echo "=== PROCURANDO URL COMPLETA ==="
    URL=$(grep -oE "https://[^[:space:]]+" "$OUTPUT_FILE" | head -1)
    
    if [ ! -z "$URL" ]; then
        echo ""
        echo "🔗 URL ENCONTRADA:"
        echo ""
        echo "$URL"
        echo ""
        echo "📋 Se a URL acima estiver completa, copie e use!"
        echo "   Se estiver truncada, veja o arquivo completo:"
        echo "   cat firebase-login-completo.txt"
    else
        echo ""
        echo "⚠️ URL não encontrada automaticamente."
        echo ""
        echo "📋 Para ver o conteúdo completo, execute:"
        echo "   cat firebase-login-completo.txt"
        echo ""
        echo "Procure pela linha que começa com: https://accounts.google.com/..."
    fi
fi

echo ""
echo "=========================================="
echo "📝 Próximos passos:"
echo "1. Copie a URL completa (do arquivo se necessário)"
echo "2. Cole no navegador do seu notebook"
echo "3. Faça login com sua conta Google"
echo "4. Copie o código de autorização"
echo "5. Volte ao terminal e cole o código"
echo ""
