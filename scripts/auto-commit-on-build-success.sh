#!/bin/bash
# Script para commitar e fazer push automaticamente quando build passa
# Este script será chamado automaticamente após build bem-sucedido

set -e

# Verificar se há mudanças para commitar
if git diff --quiet && git diff --cached --quiet; then
    echo "ℹ️  Nenhuma mudança para commitar."
    exit 0
fi

# Criar mensagem de commit automática
COMMIT_MSG="Auto-commit: Correções de build - $(date '+%Y-%m-%d %H:%M:%S')"

echo "📝 Fazendo commit automático das mudanças..."
git add -A
git commit -m "$COMMIT_MSG" || {
    echo "⚠️  Nenhuma mudança para commitar ou commit falhou."
    exit 0
}

echo "📤 Fazendo push para o repositório remoto..."
git push origin HEAD || {
    echo "⚠️  Push falhou. Verifique a conexão ou credenciais."
    exit 1
}

echo "✅ Mudanças commitadas e enviadas com sucesso!"
