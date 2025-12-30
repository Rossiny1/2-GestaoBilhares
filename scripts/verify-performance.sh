#!/bin/bash
# Script de Verificação de Performance - Ambiente IA
# Deve ser executado por TODOS os agentes antes de iniciar trabalho

set -e

# Cores
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${BLUE}⚡ VERIFICAÇÃO DE PERFORMANCE MÁXIMA${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

ERRORS=0
WARNINGS=0

# 1. Verificar Gradle Workers
echo -e "${BLUE}[1/7] Verificando Gradle Workers...${NC}"
if grep -q "org.gradle.workers.max=4" gradle.properties 2>/dev/null; then
    echo -e "${GREEN}✅ Workers configurado para 4 (otimizado)${NC}"
else
    WORKERS=$(grep "org.gradle.workers.max" gradle.properties 2>/dev/null | cut -d'=' -f2 || echo "não encontrado")
    echo -e "${RED}❌ Workers incorreto: $WORKERS (deve ser 4)${NC}"
    ERRORS=$((ERRORS + 1))
fi
echo ""

# 2. Verificar Gradle Cache
echo -e "${BLUE}[2/7] Verificando Gradle Cache...${NC}"
if grep -q "org.gradle.caching=true" gradle.properties 2>/dev/null; then
    echo -e "${GREEN}✅ Build cache habilitado${NC}"
else
    echo -e "${RED}❌ Build cache desabilitado${NC}"
    ERRORS=$((ERRORS + 1))
fi

if grep -q "org.gradle.configuration-cache=true" gradle.properties 2>/dev/null; then
    echo -e "${GREEN}✅ Configuration cache habilitado${NC}"
else
    echo -e "${YELLOW}⚠️  Configuration cache desabilitado${NC}"
    WARNINGS=$((WARNINGS + 1))
fi
echo ""

# 3. Verificar Compilação Paralela
echo -e "${BLUE}[3/7] Verificando Compilação Paralela...${NC}"
if grep -q "org.gradle.parallel=true" gradle.properties 2>/dev/null; then
    echo -e "${GREEN}✅ Compilação paralela habilitada${NC}"
else
    echo -e "${RED}❌ Compilação paralela desabilitada${NC}"
    ERRORS=$((ERRORS + 1))
fi
echo ""

# 4. Verificar Kotlin Incremental
echo -e "${BLUE}[4/7] Verificando Kotlin Incremental...${NC}"
if grep -q "kotlin.incremental=true" gradle.properties 2>/dev/null; then
    echo -e "${GREEN}✅ Kotlin incremental compilation habilitado${NC}"
else
    echo -e "${YELLOW}⚠️  Kotlin incremental compilation desabilitado${NC}"
    WARNINGS=$((WARNINGS + 1))
fi

if grep -q "ksp.incremental=true" gradle.properties 2>/dev/null; then
    echo -e "${GREEN}✅ KSP incremental habilitado${NC}"
else
    echo -e "${YELLOW}⚠️  KSP incremental desabilitado${NC}"
    WARNINGS=$((WARNINGS + 1))
fi
echo ""

# 5. Verificar Cursor Settings
echo -e "${BLUE}[5/7] Verificando Cursor Settings...${NC}"
if [ -f ".cursor/settings.json" ]; then
    if grep -q '"files.autoSaveDelay": 500' .cursor/settings.json 2>/dev/null; then
        echo -e "${GREEN}✅ Auto-save otimizado (500ms)${NC}"
    else
        echo -e "${YELLOW}⚠️  Auto-save não está em 500ms${NC}"
        WARNINGS=$((WARNINGS + 1))
    fi
    
    if grep -q '"editor.formatOnSave": true' .cursor/settings.json 2>/dev/null; then
        echo -e "${GREEN}✅ Format on save habilitado${NC}"
    else
        echo -e "${YELLOW}⚠️  Format on save desabilitado${NC}"
        WARNINGS=$((WARNINGS + 1))
    fi
else
    echo -e "${YELLOW}⚠️  .cursor/settings.json não encontrado${NC}"
    WARNINGS=$((WARNINGS + 1))
fi
echo ""

# 6. Verificar Cursor Config (Auto-approve)
echo -e "${BLUE}[6/7] Verificando Cursor Auto-approve...${NC}"
if [ -f ".cursor/config.json" ]; then
    if grep -q "autoApprove\|auto-approve" .cursor/config.json 2>/dev/null; then
        echo -e "${GREEN}✅ Auto-approve configurado${NC}"
    else
        echo -e "${YELLOW}⚠️  Auto-approve não configurado${NC}"
        WARNINGS=$((WARNINGS + 1))
    fi
else
    echo -e "${YELLOW}⚠️  .cursor/config.json não encontrado${NC}"
    WARNINGS=$((WARNINGS + 1))
fi
echo ""

# 7. Verificar Firebase CLI
echo -e "${BLUE}[7/7] Verificando Firebase CLI...${NC}"
if command -v firebase &> /dev/null; then
    FIREBASE_VERSION=$(firebase --version 2>/dev/null || echo "erro")
    if [ "$FIREBASE_VERSION" != "erro" ]; then
        echo -e "${GREEN}✅ Firebase CLI instalado: $FIREBASE_VERSION${NC}"
    else
        echo -e "${YELLOW}⚠️  Firebase CLI encontrado mas não funciona${NC}"
        WARNINGS=$((WARNINGS + 1))
    fi
else
    echo -e "${YELLOW}⚠️  Firebase CLI não encontrado no PATH${NC}"
    echo -e "${YELLOW}   (Pode estar instalado localmente no Windows)${NC}"
    WARNINGS=$((WARNINGS + 1))
fi
echo ""

# 8. Verificar Daemons Gradle
echo -e "${BLUE}[EXTRA] Verificando Daemons Gradle...${NC}"
if [ -f "./gradlew" ] && [ -x "./gradlew" ]; then
    GRADLE_STATUS=$(./gradlew --status 2>/dev/null | grep -c "IDLE\|BUSY" || echo "0")
    if [ "$GRADLE_STATUS" -gt 0 ] 2>/dev/null; then
        echo -e "${GREEN}✅ Daemons Gradle ativos${NC}"
        ./gradlew --status 2>/dev/null | head -5
    else
        echo -e "${YELLOW}⚠️  Nenhum daemon Gradle ativo${NC}"
        echo -e "${YELLOW}   (Isso é normal se não houver builds recentes)${NC}"
    fi
else
    echo -e "${YELLOW}⚠️  Gradle wrapper não encontrado ou sem permissão${NC}"
    WARNINGS=$((WARNINGS + 1))
fi
echo ""

# Resumo
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}✅ VERIFICAÇÃO COMPLETA: Tudo otimizado para máxima performance!${NC}"
    echo ""
    echo "Pode iniciar o trabalho com confiança."
    exit 0
elif [ $ERRORS -eq 0 ]; then
    echo -e "${YELLOW}⚠️  VERIFICAÇÃO COMPLETA: $WARNINGS aviso(s) encontrado(s)${NC}"
    echo ""
    echo "Avisos não críticos. Pode continuar, mas considere corrigir."
    exit 0
else
    echo -e "${RED}❌ VERIFICAÇÃO COMPLETA: $ERRORS erro(s) e $WARNINGS aviso(s) encontrado(s)${NC}"
    echo ""
    echo -e "${RED}⚠️  CORRIJA OS ERROS ANTES DE CONTINUAR!${NC}"
    echo ""
    echo "Consulte: .cursor/rules/0-PERFORMANCE-MAXIMA-OBRIGATORIO.md"
    exit 1
fi
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
