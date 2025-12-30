#!/bin/bash
# Script para fazer deploy das Firebase Functions (Linux/VM)
# Versão Linux do scripts/deploy-functions.ps1

set -e

echo "========================================"
echo "  Deploy de Firebase Functions"
echo "========================================"
echo ""

# Verificar Firebase CLI
echo "[1/4] Verificando Firebase CLI..."
if ! command -v firebase &> /dev/null; then
    echo "[ERRO] Firebase CLI não encontrado"
    echo "Instale com: npm install -g firebase-tools"
    exit 1
fi

VERSION=$(firebase --version)
echo "[OK] Firebase CLI: $VERSION"
echo ""

# Verificar login
echo "[2/4] Verificando login..."
LOGIN_LIST=$(firebase login:list 2>&1 || true)

if echo "$LOGIN_LIST" | grep -q "No authorized accounts"; then
    echo "[AVISO] Não está logado. Fazendo login..."
    echo ""
    echo "O navegador será aberto para autenticação."
    echo "Aguarde e faça login com sua conta Google do Firebase."
    echo ""
    
    firebase login
    
    if [ $? -ne 0 ]; then
        echo "[ERRO] Falha no login"
        echo "Tente executar manualmente: firebase login"
        exit 1
    fi
    
    echo ""
    echo "[OK] Login realizado com sucesso!"
else
    echo "[OK] Já está logado"
fi

echo ""

# Verificar Node.js e npm
echo "[3/4] Verificando Node.js e npm..."
if ! command -v node &> /dev/null || ! command -v npm &> /dev/null; then
    echo "[ERRO] Node.js ou npm não encontrados"
    exit 1
fi

NODE_VERSION=$(node --version)
NPM_VERSION=$(npm --version)
echo "[OK] Node.js: $NODE_VERSION"
echo "[OK] npm: $NPM_VERSION"
echo ""

# Compilar TypeScript
echo "[4/4] Compilando TypeScript..."
cd functions

echo "Instalando dependências (se necessário)..."
npm install --silent

echo "Compilando TypeScript..."
npm run build

if [ $? -ne 0 ]; then
    echo "[ERRO] Falha na compilação TypeScript"
    cd ..
    exit 1
fi

echo "[OK] Compilação concluída!"
cd ..

echo ""

# Selecionar projeto Firebase
echo "Selecionando projeto Firebase..."
firebase use gestaobilhares --project gestaobilhares 2>&1 | grep -v "Already using" || true
echo "[OK] Projeto 'gestaobilhares' selecionado."
echo ""

# Fazer deploy das functions
echo "========================================"
echo "  Fazendo deploy das Functions..."
echo "========================================"
echo ""
echo "Isso pode levar alguns minutos..."
echo ""

firebase deploy --only functions --project gestaobilhares

if [ $? -eq 0 ]; then
    echo ""
    echo "========================================"
    echo "  Deploy concluído com sucesso!"
    echo "========================================"
    echo ""
    echo "As Firebase Functions foram atualizadas."
    echo ""
    echo "Funções deployadas:"
    echo "  - onUserCreated"
    echo "  - onCollaboratorUpdated"
    echo "  - onColaboradorRotaUpdated"
    echo "  - migrateUserClaims"
    echo "  - validateUserClaims"
    echo ""
else
    echo ""
    echo "[ERRO] Falha no deploy das Functions"
    echo "Verifique se está logado e se o projeto está configurado corretamente"
    exit 1
fi

echo ""
