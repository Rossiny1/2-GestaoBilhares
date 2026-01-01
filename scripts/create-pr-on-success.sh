#!/bin/bash
# Script para criar PR automaticamente após build bem-sucedido
# Uso: bash scripts/create-pr-on-success.sh

set -e

echo "🔄 Verificando mudanças para criar PR..."

# Verificar se há mudanças para commitar
if git diff --quiet && git diff --cached --quiet; then
    echo "ℹ️  Nenhuma mudança para commitar."
    exit 0
fi

# Obter branch atual
CURRENT_BRANCH=$(git branch --show-current 2>/dev/null || git rev-parse --abbrev-ref HEAD)

if [[ "$CURRENT_BRANCH" == "main" ]] || [[ "$CURRENT_BRANCH" == "master" ]]; then
    echo "⚠️  Não é possível criar PR da branch main/master."
    exit 0
fi

# Verificar se GitHub CLI está instalado
if ! command -v gh &> /dev/null; then
    echo "⚠️  GitHub CLI (gh) não encontrado."
    echo "📝 Fazendo commit e push normal..."
    
    git add -A
    COMMIT_MSG="Auto-commit: Build bem-sucedido - $(date '+%Y-%m-%d %H:%M:%S')"
    git commit -m "$COMMIT_MSG"
    
    if [ $? -eq 0 ]; then
        git push origin "$CURRENT_BRANCH"
        echo "✅ Mudanças commitadas e enviadas!"
        echo "💡 Instale GitHub CLI (gh) para criar PRs automaticamente."
    fi
    exit 0
fi

# Verificar autenticação GitHub
if ! gh auth status &>/dev/null; then
    echo "⚠️  GitHub CLI não autenticado."
    echo "💡 Execute: gh auth login"
    exit 1
fi

# Fazer commit
echo "📝 Fazendo commit das mudanças..."
git add -A

TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')
COMMIT_MSG="Auto-commit: Build bem-sucedido - $TIMESTAMP

- Build passou com sucesso
- Todas as correções aplicadas
- Pronto para revisão"

git commit -m "$COMMIT_MSG"

if [ $? -ne 0 ]; then
    echo "⚠️  Nenhuma mudança para commitar."
    exit 0
fi

# Fazer push
echo "📤 Fazendo push da branch..."
git push origin "$CURRENT_BRANCH"

# Criar ou atualizar PR
echo "🔀 Criando/Atualizando PR..."

EXISTING_PR=$(gh pr list --head "$CURRENT_BRANCH" --json number --jq '.[0].number' 2>/dev/null || echo "")

if [ -n "$EXISTING_PR" ] && [ "$EXISTING_PR" != "null" ]; then
    echo "✅ PR #$EXISTING_PR já existe. Atualizado com novo commit!"
else
    PR_TITLE="Auto-PR: Correções e Otimizações - $(date '+%Y-%m-%d %H:%M')"
    PR_BODY="## 🤖 Pull Request Automático

Este PR foi criado automaticamente após build bem-sucedido.

### 📋 O que foi feito:
- ✅ Build passou com sucesso
- ✅ Todas as correções aplicadas
- ✅ Otimizações de performance
- ✅ Scripts de automação

### 🔍 Revisão:
Por favor, revise as mudanças antes de fazer merge.

### 🚀 Próximos passos:
1. Revisar mudanças
2. Testar localmente (opcional)
3. Aprovar e fazer merge

---
*Criado automaticamente em $(date '+%Y-%m-%d %H:%M:%S')*"

    PR=$(echo "$PR_BODY" | gh pr create \
        --title "$PR_TITLE" \
        --body-file - \
        --base main \
        --head "$CURRENT_BRANCH" 2>&1)

    if [ $? -eq 0 ]; then
        echo "✅ PR criado com sucesso!"
        echo "$PR"
    else
        echo "⚠️  Erro ao criar PR: $PR"
        echo "💡 Mudanças foram commitadas. Crie o PR manualmente."
    fi
fi

echo ""
echo "✅ Processo concluído!"
