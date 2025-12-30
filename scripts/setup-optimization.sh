#!/bin/bash
# Script de Otimização do Ambiente para IA
# Executa instalações e configurações recomendadas

set -e

echo "🚀 Iniciando otimização do ambiente para máxima eficiência da IA..."
echo ""

# Cores para output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Função para verificar se comando existe
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# 1. Verificar Node.js e npm
echo "📦 Verificando Node.js e npm..."
if command_exists node && command_exists npm; then
    echo -e "${GREEN}✅ Node.js $(node --version) e npm $(npm --version) instalados${NC}"
else
    echo -e "${RED}❌ Node.js ou npm não encontrados. Instale Node.js primeiro.${NC}"
    exit 1
fi

# 2. Instalar Firebase CLI globalmente
echo ""
echo "🔥 Instalando Firebase CLI..."
if command_exists firebase; then
    echo -e "${GREEN}✅ Firebase CLI já instalado: $(firebase --version)${NC}"
else
    echo "Instalando Firebase CLI..."
    npm install -g firebase-tools
    echo -e "${GREEN}✅ Firebase CLI instalado${NC}"
fi

# 3. Verificar autenticação Firebase
echo ""
echo "🔐 Verificando autenticação Firebase..."
if firebase login:list 2>/dev/null | grep -q "rossinys@gmail.com"; then
    echo -e "${GREEN}✅ Firebase autenticado como rossinys@gmail.com${NC}"
else
    echo -e "${YELLOW}⚠️  Firebase não autenticado. Execute: firebase login${NC}"
fi

# 4. Configurar projeto Firebase
echo ""
echo "⚙️  Configurando projeto Firebase..."
firebase use gestaobilhares 2>/dev/null || echo -e "${YELLOW}⚠️  Projeto Firebase não configurado. Execute: firebase use gestaobilhares${NC}"

# 5. Instalar ferramentas úteis
echo ""
echo "🛠️  Instalando ferramentas úteis..."

# Verificar se é Ubuntu/Debian
if command_exists apt-get; then
    echo "Instalando htop, tree, jq..."
    sudo apt-get update -qq
    sudo apt-get install -y htop tree jq 2>/dev/null || echo -e "${YELLOW}⚠️  Algumas ferramentas não puderam ser instaladas (pode precisar de sudo)${NC}"
    echo -e "${GREEN}✅ Ferramentas instaladas${NC}"
else
    echo -e "${YELLOW}⚠️  Sistema não é Ubuntu/Debian. Instale manualmente: htop, tree, jq${NC}"
fi

# 6. Verificar Gradle
echo ""
echo "📦 Verificando Gradle..."
if [ -f "./gradlew" ]; then
    echo -e "${GREEN}✅ Gradle wrapper encontrado${NC}"
    ./gradlew --version | head -1
else
    echo -e "${RED}❌ Gradle wrapper não encontrado${NC}"
fi

# 7. Verificar configurações do Gradle
echo ""
echo "⚙️  Verificando configurações do Gradle..."
if [ -f "gradle.properties" ]; then
    echo -e "${GREEN}✅ gradle.properties encontrado${NC}"
    
    # Verificar configurações importantes
    if grep -q "org.gradle.parallel=true" gradle.properties; then
        echo -e "${GREEN}  ✅ Compilação paralela habilitada${NC}"
    else
        echo -e "${YELLOW}  ⚠️  Compilação paralela não habilitada${NC}"
    fi
    
    if grep -q "org.gradle.caching=true" gradle.properties; then
        echo -e "${GREEN}  ✅ Build cache habilitado${NC}"
    else
        echo -e "${YELLOW}  ⚠️  Build cache não habilitado${NC}"
    fi
    
    if grep -q "kotlin.incremental=true" gradle.properties; then
        echo -e "${GREEN}  ✅ Kotlin incremental compilation habilitado${NC}"
    else
        echo -e "${YELLOW}  ⚠️  Kotlin incremental compilation não habilitado${NC}"
    fi
else
    echo -e "${RED}❌ gradle.properties não encontrado${NC}"
fi

# 8. Criar diretórios de cache se não existirem
echo ""
echo "📁 Criando diretórios de cache..."
mkdir -p .gradle/cache
mkdir -p ~/.gradle/caches
echo -e "${GREEN}✅ Diretórios de cache criados${NC}"

# 9. Verificar configurações do Cursor
echo ""
echo "🎯 Verificando configurações do Cursor..."
if [ -f ".cursor/config.json" ]; then
    echo -e "${GREEN}✅ .cursor/config.json encontrado${NC}"
else
    echo -e "${YELLOW}⚠️  .cursor/config.json não encontrado${NC}"
fi

if [ -f ".cursor/settings.json" ]; then
    echo -e "${GREEN}✅ .cursor/settings.json encontrado${NC}"
else
    echo -e "${YELLOW}⚠️  .cursor/settings.json não encontrado${NC}"
fi

# 10. Verificar MCP Firebase
echo ""
echo "🔌 Verificando MCP Firebase..."
if [ -f "$HOME/.cursor/mcp.json" ] || [ -f "$HOME/.config/cursor/mcp.json" ]; then
    echo -e "${GREEN}✅ Arquivo de configuração MCP encontrado${NC}"
    echo -e "${YELLOW}  ℹ️  Verifique no Cursor: Settings → Tools → Installed MCP Servers${NC}"
else
    echo -e "${YELLOW}⚠️  Arquivo de configuração MCP não encontrado${NC}"
    echo -e "${YELLOW}  ℹ️  Configure em: ~/.cursor/mcp.json ou ~/.config/cursor/mcp.json${NC}"
fi

# 11. Resumo
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${GREEN}✅ Otimização concluída!${NC}"
echo ""
echo "📋 Próximos passos:"
echo "  1. Se Firebase não estiver autenticado: firebase login"
echo "  2. Se projeto não estiver configurado: firebase use gestaobilhares"
echo "  3. Verificar MCP no Cursor: Settings → Tools → Installed MCP Servers"
echo "  4. Testar build: ./gradlew assembleDebug --parallel --build-cache"
echo ""
echo "📚 Documentação completa: documentation/OTIMIZACAO-AMBIENTE-IA.md"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
