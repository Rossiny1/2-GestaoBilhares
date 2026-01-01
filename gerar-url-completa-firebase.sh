#!/bin/bash
# Script para gerar URL completa do Firebase (sem truncar)

export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin

echo "🔥 Gerando URL de autenticação Firebase..."
echo ""

# Criar arquivo para capturar output
OUTPUT_FILE="/workspace/firebase-url-completa.txt"

# Executar login e capturar TUDO
firebase login --no-localhost 2>&1 | tee "$OUTPUT_FILE"

echo ""
echo "=========================================="
echo "✅ Output completo salvo em: firebase-url-completa.txt"
echo ""

# Extrair URL completa
if [ -f "$OUTPUT_FILE" ]; then
    echo "=== URL COMPLETA ==="
    # Procurar por qualquer linha que contenha https://
    grep -i "https://" "$OUTPUT_FILE" | grep -oE "https://[^[:space:]]*" | head -1
    
    echo ""
    echo "📋 Para ver a URL completa, execute:"
    echo "   cat firebase-url-completa.txt"
    echo ""
    echo "Ou abra o arquivo firebase-url-completa.txt no Cursor"
fi
