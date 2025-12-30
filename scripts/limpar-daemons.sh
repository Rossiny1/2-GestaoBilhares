#!/bin/bash
# Script para limpar daemons do Gradle/Kotlin e liberar memória
# Uso: bash scripts/limpar-daemons.sh

echo "🧹 Limpando daemons do Gradle e Kotlin..."
echo ""

# Parar todos os daemons Gradle
if command -v gradle &> /dev/null; then
    echo "🛑 Parando daemons Gradle..."
    gradle --stop 2>/dev/null || true
    echo "✅ Daemons Gradle parados"
fi

# Limpar cache do Kotlin daemon
echo "🧹 Limpando cache do Kotlin daemon..."
rm -rf ~/.kotlin/daemon/* 2>/dev/null || true
echo "✅ Cache do Kotlin limpo"

# Matar processos Java órfãos (se houver)
echo "🔍 Verificando processos Java órfãos..."
pkill -9 -f "kotlin-compiler-embeddable" 2>/dev/null && echo "✅ Processos Kotlin limpos" || echo "ℹ️  Nenhum processo Kotlin encontrado"
pkill -9 -f "gradle-daemon" 2>/dev/null && echo "✅ Processos Gradle limpos" || echo "ℹ️  Nenhum processo Gradle encontrado"

echo ""
echo "✅ Limpeza completa!"
echo ""
echo "💡 Memória liberada. Execute './gradlew build' para reiniciar os daemons."
