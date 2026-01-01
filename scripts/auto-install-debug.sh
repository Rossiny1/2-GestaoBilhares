#!/bin/bash
# Script para verificar mudanças remotas e instalar automaticamente o app
# Uso: ./scripts/auto-install-debug.sh

set -e

echo "🔄 Verificando mudanças remotas..."

# Verificar se há mudanças remotas
git fetch origin

LOCAL=$(git rev-parse @)
REMOTE=$(git rev-parse @{u})
BASE=$(git merge-base @ @{u})

if [ $LOCAL = $REMOTE ]; then
    echo "✅ Repositório local está atualizado. Nenhuma mudança para instalar."
    exit 0
elif [ $LOCAL = $BASE ]; then
    echo "📥 Atualizações disponíveis. Fazendo pull..."
    git pull origin
    
    echo "🔨 Compilando e instalando app..."
    ./gradlew installDebug
    
    if [ $? -eq 0 ]; then
        echo "✅ App instalado com sucesso no dispositivo conectado!"
    else
        echo "❌ Erro ao instalar app. Verifique os logs acima."
        exit 1
    fi
elif [ $REMOTE = $BASE ]; then
    echo "⚠️  Você tem commits locais não enviados. Faça push primeiro."
    exit 1
else
    echo "⚠️  Divergência detectada. Faça merge manualmente."
    exit 1
fi
