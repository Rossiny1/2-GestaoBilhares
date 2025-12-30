#!/bin/bash
# Script para monitorar mudanças remotas e instalar automaticamente
# Uso: ./scripts/watch-and-install.sh
# Este script roda em loop verificando mudanças a cada 30 segundos

set -e

echo "👀 Monitorando mudanças remotas (Ctrl+C para parar)..."
echo "⏱️  Verificando a cada 30 segundos..."

LAST_COMMIT=""

while true; do
    # Buscar mudanças remotas
    git fetch origin --quiet 2>/dev/null || true
    
    CURRENT_COMMIT=$(git rev-parse origin/HEAD 2>/dev/null || echo "")
    
    if [ -n "$CURRENT_COMMIT" ] && [ "$CURRENT_COMMIT" != "$LAST_COMMIT" ]; then
        echo ""
        echo "🔄 Mudanças detectadas! Último commit: $CURRENT_COMMIT"
        echo "📥 Fazendo pull..."
        
        git pull origin || {
            echo "❌ Erro ao fazer pull. Continuando monitoramento..."
            sleep 30
            continue
        }
        
        echo "🔨 Compilando e instalando app..."
        ./gradlew installDebug
        
        if [ $? -eq 0 ]; then
            echo "✅ App atualizado e instalado com sucesso!"
            LAST_COMMIT="$CURRENT_COMMIT"
        else
            echo "❌ Erro ao instalar. Continuando monitoramento..."
        fi
        
        echo ""
    fi
    
    sleep 30
done
