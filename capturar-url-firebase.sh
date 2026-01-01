#!/bin/bash
# Script para capturar a URL completa do Firebase login

export PATH=$PATH:/home/ubuntu/.nvm/versions/node/v22.21.1/bin

echo "🔥 Capturando URL de autenticação Firebase..."
echo ""
echo "Este script vai capturar a URL completa e salvar em um arquivo."
echo ""

# Criar arquivo temporário para capturar output
TEMP_FILE="/tmp/firebase-url-$(date +%s).txt"

# Executar firebase login e capturar tudo
firebase login --no-localhost 2>&1 | tee "$TEMP_FILE" &

# Aguardar um pouco para o processo iniciar
sleep 3

# Extrair URL completa do arquivo
if [ -f "$TEMP_FILE" ]; then
    echo ""
    echo "=== URL ENCONTRADA ==="
    # Procurar por URLs que começam com https://
    URL=$(grep -oE "https://[^[:space:]]+" "$TEMP_FILE" | head -1)
    
    if [ ! -z "$URL" ]; then
        echo "$URL"
        echo ""
        echo "✅ URL salva em: $TEMP_FILE"
        echo "📋 Você pode ver o arquivo completo com: cat $TEMP_FILE"
    else
        echo "⚠️ URL não encontrada ainda. Verifique o arquivo: $TEMP_FILE"
    fi
fi
